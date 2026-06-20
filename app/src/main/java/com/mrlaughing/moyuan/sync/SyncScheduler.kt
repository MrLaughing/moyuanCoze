package com.mrlaughing.moyuan.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mrlaughing.moyuan.util.Constants
import java.util.Calendar

/**
 * 定时同步调度器：使用 AlarmManager 精确定时
 */
object SyncScheduler {

    /**
     * 注册每日同步闹钟
     * @param hour 小时 (0-23)
     * @param minute 分钟 (0-59)
     */
    fun scheduleDailySync(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, SyncReceiver::class.java).apply {
            action = Constants.SYNC_ACTION
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.SYNC_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 如果设定时间已过，则安排到明天
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // 精确闹钟（Android 12+ 需要 SCHEDULE_EXACT_ALARM 权限）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                // 不允许精确闹钟时退化为非精确
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        // 保存同步时间偏好
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("sync_hour", hour)
            .putInt("sync_minute", minute)
            .apply()
    }

    /**
     * 取消同步闹钟
     */
    fun cancelSync(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SyncReceiver::class.java).apply {
            action = Constants.SYNC_ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.SYNC_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    /**
     * 从偏好中读取上次保存的同步时间并重新注册
     */
    fun rescheduleFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val hour = prefs.getInt("sync_hour", Constants.SYNC_DEFAULT_HOUR)
        val minute = prefs.getInt("sync_minute", Constants.SYNC_DEFAULT_MINUTE)
        scheduleDailySync(context, hour, minute)
    }
}

/**
 * 同步闹钟触发的 BroadcastReceiver
 */
class SyncReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.SYNC_ACTION) {
            // 启动 WorkManager 一次性任务执行同步
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
                .build()
            androidx.work.WorkManager.getInstance(context).enqueue(workRequest)

            // 注册下一天的闹钟
            val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            val hour = prefs.getInt("sync_hour", Constants.SYNC_DEFAULT_HOUR)
            val minute = prefs.getInt("sync_minute", Constants.SYNC_DEFAULT_MINUTE)
            SyncScheduler.scheduleDailySync(context, hour, minute)
        }
    }
}

/**
 * 开机重启后重新注册同步闹钟
 */
class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SyncScheduler.rescheduleFromPrefs(context)
        }
    }
}
