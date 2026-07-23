package com.mrlaughing.moyuan.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Reliable sync scheduling backed by WorkManager. */
object SyncScheduler {

    const val UNIQUE_DAILY_SYNC = "moyuan_daily_sync"
    const val UNIQUE_IMMEDIATE_SYNC = "moyuan_immediate_sync"

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Schedules one sync per day near the user-selected local time. */
    fun scheduleDailySync(context: Context, hour: Int, minute: Int) {
        val now = ZonedDateTime.now()
        var nextRun = now
            .withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!nextRun.isAfter(now)) nextRun = nextRun.plusDays(1)

        val request = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, nextRun))
            .setConstraints(networkConstraints)
            .addTag(UNIQUE_DAILY_SYNC)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_DAILY_SYNC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /** Enqueues a single foreground-triggered sync and prevents concurrent duplicates. */
    fun enqueueImmediateSync(context: Context, replaceRunning: Boolean = false): UUID {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(UNIQUE_IMMEDIATE_SYNC)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_SYNC,
            if (replaceRunning) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
        return request.id
    }

    fun cancelDailySync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_DAILY_SYNC)
    }
}
