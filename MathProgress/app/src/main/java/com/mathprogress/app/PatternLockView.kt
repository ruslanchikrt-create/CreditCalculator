package com.mathprogress.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

class PatternLockView(
    context: Context,
    private val darkMode: Boolean,
    private val onComplete: (String) -> Unit
) : View(context) {
    private val selected = mutableListOf<Int>()
    private val points = mutableListOf<Pair<Float,Float>>()
    private var currentX = 0f
    private var currentY = 0f
    private var drawing = false
    private var error = false
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = dp(5f); strokeCap = Paint.Cap.ROUND }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, w.coerceAtMost(dp(330f).toInt()))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        buildPoints()
        line.color = if (error) Color.rgb(215,63,70) else Color.rgb(99,91,255)
        if (selected.size > 1) for (i in 0 until selected.lastIndex) {
            val a=points[selected[i]]; val b=points[selected[i+1]]; canvas.drawLine(a.first,a.second,b.first,b.second,line)
        }
        if (drawing && selected.isNotEmpty()) {
            val a=points[selected.last()]; canvas.drawLine(a.first,a.second,currentX,currentY,line)
        }
        for (i in points.indices) {
            val (x,y)=points[i]
            dot.style=Paint.Style.FILL
            dot.color = if (i in selected) line.color else if (darkMode) Color.rgb(66,68,78) else Color.rgb(222,224,232)
            canvas.drawCircle(x,y,dp(if(i in selected)13f else 10f),dot)
            if (i !in selected) {
                dot.style=Paint.Style.STROKE; dot.strokeWidth=dp(2f); dot.color=if(darkMode)Color.rgb(112,115,128) else Color.rgb(170,173,184)
                canvas.drawCircle(x,y,dp(18f),dot)
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when(e.action) {
            MotionEvent.ACTION_DOWN -> { selected.clear(); error=false; drawing=true; currentX=e.x;currentY=e.y;addHit(e.x,e.y);invalidate();return true }
            MotionEvent.ACTION_MOVE -> { currentX=e.x;currentY=e.y;addHit(e.x,e.y);invalidate();return true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                drawing=false; currentX=e.x;currentY=e.y; invalidate()
                if(selected.size>=4) onComplete(selected.joinToString("-")) else { error=true; invalidate(); postDelayed({reset()},650) }
                return true
            }
        }
        return true
    }

    fun showError() { error=true; invalidate(); postDelayed({reset()},650) }
    fun reset() { selected.clear();error=false;drawing=false;invalidate() }

    private fun buildPoints(){
        if(points.isNotEmpty())return
        val usableW=width.toFloat(); val usableH=height.toFloat(); val xs=listOf(usableW*.2f,usableW*.5f,usableW*.8f); val ys=listOf(usableH*.18f,usableH*.5f,usableH*.82f)
        for(y in ys)for(x in xs)points+=x to y
    }
    private fun addHit(x:Float,y:Float){
        buildPoints(); val idx=points.indices.firstOrNull{ i->i !in selected && hypot(x-points[i].first,y-points[i].second)<dp(30f)}
        if(idx!=null)selected+=idx
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    private fun dp(v:Float)=v*resources.displayMetrics.density
}
