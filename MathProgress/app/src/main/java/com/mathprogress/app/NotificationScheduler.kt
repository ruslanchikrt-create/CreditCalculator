package com.mathprogress.app

import android.app.*
import android.content.*
import android.os.Build
import java.util.Calendar

object NotificationScheduler {
    const val CHANNEL_SILENT = "study_reminders_silent"
    const val CHANNEL_DAILY = "daily_training"
    const val ACTION_UNFINISHED = "com.mathprogress.app.UNFINISHED"
    const val ACTION_INACTIVE = "com.mathprogress.app.INACTIVE"
    const val ACTION_DAILY = "com.mathprogress.app.DAILY"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL_SILENT, "Напоминания", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Незавершённые задачи и возвращение к занятиям"
                setSound(null, null); enableVibration(false); setShowBadge(true)
            })
            nm.createNotificationChannel(NotificationChannel(CHANNEL_DAILY, "Ежедневная тренировка", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Ежедневное задание после обеда"
                setShowBadge(true)
            })
        }
    }

    fun scheduleAll(context: Context, store: LocalStore) {
        ensureChannels(context)
        scheduleUnfinished(context, store)
        scheduleInactive(context, store)
        scheduleDaily(context)
    }

    fun scheduleUnfinished(context: Context, store: LocalStore) {
        cancel(context, 101, ACTION_UNFINISHED)
        if (store.getDraft().isBlank()) return
        schedule(context, 101, ACTION_UNFINISHED, System.currentTimeMillis() + 6L * 60 * 60 * 1000)
    }

    fun scheduleInactive(context: Context, store: LocalStore) {
        cancel(context, 102, ACTION_INACTIVE)
        val trigger = System.currentTimeMillis() + store.settings.inactivityDays * 24L * 60 * 60 * 1000
        schedule(context, 102, ACTION_INACTIVE, trigger)
    }

    fun scheduleDaily(context: Context) {
        cancel(context, 103, ACTION_DAILY)
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        schedule(context, 103, ACTION_DAILY, target.timeInMillis)
    }

    private fun schedule(context: Context, requestCode: Int, action: String, at: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(context, requestCode, Intent(context, ReminderReceiver::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun cancel(context: Context, requestCode: Int, action: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(context, requestCode, Intent(context, ReminderReceiver::class.java).setAction(action), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) { am.cancel(pi); pi.cancel() }
    }
}

class ReminderReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationScheduler.ensureChannels(context)
        val store = LocalStore(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val action = intent.action
        val builder = when (action) {
            NotificationScheduler.ACTION_UNFINISHED -> {
                if (store.getDraft().isBlank()) return
                Notification.Builder(context, NotificationScheduler.CHANNEL_SILENT)
                    .setContentTitle("Незавершённое решение")
                    .setContentText("Задача ждёт продолжения. Вернитесь к ней, когда будет удобно.")
                    .setSilent(true)
            }
            NotificationScheduler.ACTION_INACTIVE -> Notification.Builder(context, NotificationScheduler.CHANNEL_SILENT)
                .setContentTitle("Небольшая математическая разминка?")
                .setContentText("Пара задач поможет не потерять форму.").setSilent(true)
            NotificationScheduler.ACTION_DAILY -> {
                val dateKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                if (store.daily(dateKey)?.completed == true) { NotificationScheduler.scheduleDaily(context); return }
                Notification.Builder(context, NotificationScheduler.CHANNEL_DAILY)
                    .setContentTitle("Ежедневная тренировка готова")
                    .setContentText("Небольшой набор задач уже ждёт вас. Закройте сегодняшний день ✓")
            }
            else -> return
        }
        val id = when(action) { NotificationScheduler.ACTION_UNFINISHED -> 201; NotificationScheduler.ACTION_INACTIVE -> 202; else -> 203 }
        nm.notify(id, builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(open).setAutoCancel(true).build())
        if (action == NotificationScheduler.ACTION_DAILY) NotificationScheduler.scheduleDaily(context)
    }
}

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) NotificationScheduler.scheduleAll(context, LocalStore(context))
    }
}
