package com.hotelski.waterme.notifications

import java.util.concurrent.atomic.AtomicInteger

object AppForegroundState {
    private val startedActivities = AtomicInteger(0)

    val isInForeground: Boolean
        get() = startedActivities.get() > 0

    fun onActivityStarted() {
        startedActivities.incrementAndGet()
    }

    fun onActivityStopped() {
        startedActivities.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
    }
}
