package com.mathprogress.app

import android.app.Notification
import android.content.Context
import android.widget.FrameLayout

/**
 * Небольшие совместимые обёртки для первой APK без сторонних библиотек.
 */
class ScrollView(context: Context) : android.widget.ScrollView(context) {
    class LayoutParams(width: Int, height: Int) : FrameLayout.LayoutParams(width, height)
}

fun Notification.Builder.setSilent(silent: Boolean): Notification.Builder {
    if (silent) setSound(null)
    return this
}
