package com.pixelquest.app.play.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import javax.inject.Inject
import javax.inject.Singleton

/** Google Play errors and quota decisions deliberately leave app UI unchanged. */
@Singleton
class GooglePlayReviewRequester @Inject constructor() : PlayReviewRequester {
    override fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) manager.launchReviewFlow(activity, request.result)
        }
    }
}
