package com.pixelquest.app.domain.namaz

import org.junit.Assert.assertEquals
import org.junit.Test

/** Every Namaz XP rule (NamazXpConfig): per prayer, late, all five, the week's run, and the cap. */
class NamazXpCalculatorTest {

    private fun award(onTime: Boolean = true, prayedBefore: Int = 0, earnedToday: Int = 0, run: Int = 1) =
        NamazXpCalculator.award(onTime, prayedBefore, earnedToday, run)

    @Test
    fun aPrayerInItsWindow_earns10() {
        assertEquals(NamazXpAward(10), award())
    }

    @Test
    fun aPrayerMarkedLate_earnsHalf() {
        assertEquals(NamazXpAward(5), award(onTime = false))
    }

    @Test
    fun theFifthPrayer_addsTheAllFiveBonus() {
        val fifth = award(prayedBefore = 4, earnedToday = 40)
        assertEquals(NamazXpAward(prayer = 10, allFiveBonus = 30), fifth)
        assertEquals(30, fifth.bonus)
        assertEquals(40, fifth.total)
    }

    @Test
    fun theFourthPrayer_hasNoBonus() {
        assertEquals(NamazXpAward(10), award(prayedBefore = 3, earnedToday = 30))
    }

    @Test
    fun aDayCompletingASevenDayRun_addsTheWeeksBonus_andSoDoes14() {
        assertEquals(NamazXpAward(10, 30, 100), award(prayedBefore = 4, earnedToday = 40, run = 7))
        assertEquals(100, award(prayedBefore = 4, earnedToday = 40, run = 14).weekBonus)
        assertEquals(0, award(prayedBefore = 4, earnedToday = 40, run = 6).weekBonus)
        assertEquals(0, award(prayedBefore = 4, earnedToday = 40, run = 8).weekBonus)
    }

    @Test
    fun theWeeksBonus_comesOnlyWithTheFifthPrayer() {
        assertEquals(0, award(prayedBefore = 2, earnedToday = 20, run = 7).weekBonus)
    }

    @Test
    fun aFullDayOnTime_isExactlyTheCap() {
        var earned = 0
        repeat(5) { before ->
            earned += award(prayedBefore = before, earnedToday = earned).let { it.prayer + it.allFiveBonus }
        }
        assertEquals(NamazXpConfig.DAILY_CAP, earned)
    }

    @Test
    fun theCap_trimsTheBonusFirst_thenThePrayer() {
        // 75 already: the fifth's 10 and 30 only have 5 left between them.
        assertEquals(NamazXpAward(prayer = 5, allFiveBonus = 0), award(prayedBefore = 4, earnedToday = 75))
        // 72: the prayer's 8 of 10 room, then nothing for the bonus... the prayer takes 8.
        assertEquals(NamazXpAward(prayer = 8, allFiveBonus = 0), award(prayedBefore = 4, earnedToday = 72))
        // 45: 10 for the prayer, 25 of the bonus.
        assertEquals(NamazXpAward(prayer = 10, allFiveBonus = 25), award(prayedBefore = 4, earnedToday = 45))
    }

    @Test
    fun atTheCap_aPrayerEarnsNothing_butTheWeeksBonusStillComes() {
        assertEquals(NamazXpAward(0, 0, 100), award(prayedBefore = 4, earnedToday = 80, run = 7))
        assertEquals(NamazXpAward.NONE, award(prayedBefore = 1, earnedToday = 95))
    }
}
