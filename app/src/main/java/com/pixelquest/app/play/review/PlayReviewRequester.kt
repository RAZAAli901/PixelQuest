package com.pixelquest.app.play.review

import android.app.Activity

/** Activity-bound bridge for Google Play's review UI. Completion never reveals review submission. */
interface PlayReviewRequester {
    fun requestReview(activity: Activity)
}
