package com.pixelquest.app.domain.namaz

/**
 * Every Namaz XP reward, in one place (docs/namaz-design.md, XP). Change a value here and the
 * calculator, the screen and the tests follow.
 */
object NamazXpConfig {

    /** A prayer marked prayed within its window, before the next prayer starts. */
    const val PER_PRAYER = 10

    /** A prayer marked after its window passed: half. */
    const val PER_PRAYER_LATE = 5

    /** All five prayed on one prayer day. */
    const val ALL_FIVE_BONUS = 30

    /** Each time a run of days with all five prayed reaches [WEEK_STREAK_DAYS] (7, 14, 21 ...). */
    const val WEEK_STREAK_BONUS = 100
    const val WEEK_STREAK_DAYS = 7

    /**
     * The most a prayer day's prayers and its all-five bonus can earn together: 5 × 10 + 30. The
     * week's streak bonus comes on top: under the cap it could never be given, since a day with
     * all five on time already reaches it.
     */
    const val DAILY_CAP = 80
}
