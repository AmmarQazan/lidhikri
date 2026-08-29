package com.greendome.adhkar.review

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object InAppReviewPrompt {
    private const val TAG = "InAppReview"

    @Volatile
    private var inFlight = false

    fun onMainScreenReady(activity: ComponentActivity) {
        InAppReviewTracker(activity).recordUsageToday()
        activity.lifecycleScope.launch {
            tryLaunch(activity, InAppReviewTrigger.APP_OPEN)
        }
    }

    fun considerLaunch(activity: ComponentActivity) {
        activity.lifecycleScope.launch {
            tryLaunch(activity, InAppReviewTrigger.TASBIH)
        }
    }

    private suspend fun tryLaunch(activity: Activity, trigger: InAppReviewTrigger) {
        val tracker = InAppReviewTracker(activity)
        if (!tracker.isEligible(trigger) || tracker.shouldSkipCooldown()) return
        if (inFlight) return
        inFlight = true
        try {
            val manager = ReviewManagerFactory.create(activity)
            val reviewInfo = manager.requestReviewFlow().await()
            if (activity.isFinishing || activity.isDestroyed) return
            manager.launchReviewFlow(activity, reviewInfo).await()
            tracker.markRequested()
        } catch (e: Exception) {
            Log.w(TAG, "In-app review unavailable", e)
            tracker.markAttemptFailed()
        } finally {
            inFlight = false
        }
    }
}
