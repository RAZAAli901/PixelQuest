package com.pixelquest.app.darood.sync

import android.content.Context
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.domain.GoalRules
import com.pixelquest.app.darood.domain.PartyGoal
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** What a party's goals have been told as on this phone: the latest goal's number, and whether it was reached. */
interface GoalToldStore {
    fun told(partyId: String): Pair<Int, Boolean>?
    fun setTold(partyId: String, sequence: Int, reached: Boolean)
}

@Singleton
class PrefsGoalToldStore @Inject constructor(@ApplicationContext context: Context) : GoalToldStore {
    private val prefs = context.getSharedPreferences("pixelquest_darood_goals", Context.MODE_PRIVATE)

    override fun told(partyId: String): Pair<Int, Boolean>? =
        prefs.getString(partyId, null)?.split(':')?.takeIf { it.size == 2 }?.let { (n, r) -> (n.toIntOrNull() ?: return null) to (r == "1") }

    override fun setTold(partyId: String, sequence: Int, reached: Boolean) {
        prefs.edit().putString(partyId, "$sequence:${if (reached) 1 else 0}").apply()
    }
}

/** A goal's news, as it's posted (on the Darood channel). */
sealed interface GoalNews {
    val party: Party
    val goal: PartyGoal

    data class Reached(override val party: Party, override val goal: PartyGoal) : GoalNews
    data class NewGoal(override val party: Party, override val goal: PartyGoal) : GoalNews
}

fun interface GoalNotifier {
    fun post(news: GoalNews)
}

/**
 * Goal reached, and New goal set (J089), for every member of each Darood Party the player is
 * active in, in the worker's rounds: what changed since this phone last looked is told once. The
 * first look at a party only notes where it stands (nothing old is announced), and the leader isn't
 * told of a goal they set themselves.
 */
class GoalNotifyRun(
    private val repository: DaroodPartyRepository,
    private val me: () -> String?,
    private val told: GoalToldStore,
    private val notifier: GoalNotifier
) {
    /** How many notifications it posted. */
    suspend fun run(): Int {
        val account = me() ?: return 0
        val parties = repository.observeMyParties(PartyType.DAROOD).first().filter { it.myStatus == MemberStatus.ACTIVE }
        var posted = 0
        for (party in parties) {
            val goals = (repository.fetchGoals(party.id) as? SupabaseResult.Success)?.data ?: continue
            val current = GoalRules.current(goals) ?: continue
            val before = told.told(party.id)
            told.setTold(party.id, current.sequence, current.reached)
            if (before == null) continue
            val (toldSequence, toldReached) = before
            // A goal reached since: the one told as open, or any later one.
            goals.filter { it.reached && (it.sequence > toldSequence || (it.sequence == toldSequence && !toldReached)) }.forEach {
                notifier.post(GoalNews.Reached(party, it)); posted++
            }
            goals.filter { it.sequence > toldSequence && it.setBy != account && !it.reached }.forEach {
                notifier.post(GoalNews.NewGoal(party, it)); posted++
            }
        }
        return posted
    }
}
