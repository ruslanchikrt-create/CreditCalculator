package com.mathprogress.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.hypot

class DailyJourneyView(
    context: Context,
    private val year: Int,
    private val month: Int,
    private val statuses: Map<Int, Int>,
    private val enabledUntilDay: Int,
    private val darkMode: Boolean,
    private val onDayClick: (Int) -> Unit
) : View(context) {

    private val daysInMonth = Calendar.getInstance().apply { set(year, month, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
    private val columns = 5
    private val nodeRadius = dp(21f)
    private val rowHeight = dp(70f)
    private val topPad = dp(26f)
    private val bottomPad = dp(18f)
    private val sidePad = nodeRadius + dp(8f)
    private val centers = mutableMapOf<Int, Pair<Float, Float>>()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = dp(4f); strokeCap = Paint.Cap.ROUND }
    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; textSize = sp(15f) }
    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; strokeWidth = dp(3f); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(2.5f); color = Color.rgb(120,113,255) }
    private val todayDay: Int = Calendar.getInstance().let { now -> if (now.get(Calendar.YEAR)==year && now.get(Calendar.MONTH)==month) now.get(Calendar.DAY_OF_MONTH) else -1 }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rows = ceil(daysInMonth / columns.toDouble()).toInt()
        val height = (topPad + rows * rowHeight + bottomPad).toInt()
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        centers.clear()
        val w = width.toFloat()
        val stepX = (w - 2 * sidePad) / (columns - 1)
        val centersList = mutableListOf<Pair<Float, Float>>()
        for (day in 1..daysInMonth) {
            val idx = day - 1
            val row = idx / columns
            val pos = idx % columns
            val snakePos = if (row % 2 == 0) pos else columns - 1 - pos
            val x = sidePad + snakePos * stepX
            val y = topPad + row * rowHeight + nodeRadius
            centers[day] = x to y
            centersList += x to y
        }

        for (i in 0 until centersList.lastIndex) {
            val day1 = i + 1
            val day2 = i + 2
            linePaint.color = when {
                statuses[day1] == 2 && statuses[day2] == 2 -> Color.rgb(38, 174, 99)
                statuses[day1] == 2 -> Color.rgb(99, 91, 255)
                statuses[day1] == 1 -> Color.rgb(242, 158, 45)
                else -> if (darkMode) Color.rgb(67, 69, 79) else Color.rgb(218, 220, 229)
            }
            val (x1,y1)=centersList[i]; val (x2,y2)=centersList[i+1]
            canvas.drawLine(x1,y1,x2,y2,linePaint)
        }

        for (day in 1..daysInMonth) {
            val (x,y)=centers[day] ?: continue
            val status = statuses[day] ?: 0
            val enabled = day <= enabledUntilDay
            nodePaint.style = Paint.Style.FILL
            nodePaint.color = when {
                !enabled -> if (darkMode) Color.rgb(42,43,50) else Color.rgb(239,240,245)
                status == 2 -> Color.rgb(38, 174, 99)
                status == 1 -> Color.rgb(242, 158, 45)
                else -> if (darkMode) Color.rgb(48,49,58) else Color.WHITE
            }
            canvas.drawCircle(x,y,nodeRadius,nodePaint)
            if (enabled && status == 0) {
                nodePaint.style = Paint.Style.STROKE; nodePaint.strokeWidth = dp(2f); nodePaint.color = Color.rgb(99,91,255)
                canvas.drawCircle(x,y,nodeRadius,nodePaint)
            }
            if (day == todayDay) canvas.drawCircle(x,y,nodeRadius+dp(5f),todayPaint)
            if (status == 2) {
                val p = android.graphics.Path(); p.moveTo(x-dp(8f), y); p.lineTo(x-dp(2f), y+dp(6f)); p.lineTo(x+dp(9f), y-dp(7f)); canvas.drawPath(p, checkPaint)
            } else {
                textPaint.color = when {
                    !enabled -> if (darkMode) Color.rgb(112,114,125) else Color.rgb(164,166,176)
                    status == 1 -> Color.WHITE
                    else -> if (darkMode) Color.WHITE else Color.rgb(30,31,37)
                }
                val baseline = y - (textPaint.ascent()+textPaint.descent())/2
                canvas.drawText(day.toString(),x,baseline,textPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val hit = centers.entries.minByOrNull { (_,p) -> hypot(event.x-p.first,event.y-p.second) }
            if (hit != null && hit.key <= enabledUntilDay && hypot(event.x-hit.value.first,event.y-hit.value.second) <= nodeRadius*1.45f) {
                performClick(); onDayClick(hit.key)
            }
            return true
        }
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }
    private fun dp(v:Float)=v*resources.displayMetrics.density
    private fun sp(v:Float)=v*resources.displayMetrics.scaledDensity
}
