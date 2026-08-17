package com.mathprogress.app

import android.app.*
import android.content.*
import android.os.Build

object NotificationScheduler {
    const val CHANNEL = "study_reminders_silent"
    const val ACTION_UNFINISHED = "com.mathprogress.app.UNFINISHED"
    const val ACTION_INACTIVE = "com.mathprogress.app.INACTIVE"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL, "Тихие напоминания", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Незавершённые задачи и мягкие напоминания о занятиях"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(true)
            }
            nm.createNotificationChannel(ch)
        }
    }

    fun scheduleUnfinished(context: Context, store: LocalStore) {
        cancel(context, 101, ACTION_UNFINISHED)
        if (!store.settings.notificationsEnabled || !store.settings.unfinishedNotifications || store.getDraft().isBlank()) return
        val trigger = System.currentTimeMillis() + 6L*60*60*1000
        schedule(context, 101, ACTION_UNFINISHED, trigger)
    }

    fun scheduleInactive(context: Context, store: LocalStore) {
        cancel(context, 102, ACTION_INACTIVE)
        if (!store.settings.notificationsEnabled || !store.settings.inactivityNotifications) return
        val trigger = System.currentTimeMillis() + store.settings.inactivityDays*24L*60*60*1000
        schedule(context, 102, ACTION_INACTIVE, trigger)
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
        NotificationScheduler.ensureChannel(context)
        val store = LocalStore(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val (id,title,text) = when(intent.action) {
            NotificationScheduler.ACTION_UNFINISHED -> {
                if (store.getDraft().isBlank() || !store.settings.unfinishedNotifications) return
                Triple(201, "Незавершённая задача", "Вы начали решение и не закончили. Продолжить?")
            }
            NotificationScheduler.ACTION_INACTIVE -> {
                if (!store.settings.inactivityNotifications) return
                Triple(202, "Небольшая тренировка?", "Вы несколько дней не заходили. Одна короткая задача поможет сохранить форму.")
            }
            else -> return
        }
        val n = Notification.Builder(context, NotificationScheduler.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(text).setContentIntent(open).setAutoCancel(true)
            .setSilent(true).build()
        nm.notify(id,n)
    }
}

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val store = LocalStore(context)
            NotificationScheduler.scheduleUnfinished(context, store)
            NotificationScheduler.scheduleInactive(context, store)
        }
    }
}
