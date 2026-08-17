package com.mathprogress.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

class MathKeyboardView(
    context: Context,
    private val target: EditText
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setPadding(dp(5), dp(5), dp(5), dp(5))
        background = shape(Color.WHITE, 14, Color.rgb(225, 226, 234))
        target.showSoftInputOnFocus = false

        val rows = listOf(
            listOf("7", "8", "9", "+", "−"),
            listOf("4", "5", "6", "×", "÷"),
            listOf("1", "2", "3", "(", ")"),
            listOf("0", ".", "=", "x", "y"),
            listOf("z", "x²", "x³", "xⁿ", "√"),
            listOf("a⁄b", "|x|", "log", "ln", "π"),
            listOf("<", ">", "≤", "≥", "≠")
        )
        rows.forEach { symbols ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            symbols.forEach { symbol ->
                row.addView(key(symbol) { press(symbol) }, LayoutParams(0, dp(42), 1f).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                })
            }
            addView(row)
        }

        val controls = LinearLayout(context).apply { orientation = HORIZONTAL }
        controls.addView(key("←") { move(-1) }, LayoutParams(0, dp(42), 1f))
        controls.addView(key("→") { move(1) }, LayoutParams(0, dp(42), 1f))
        controls.addView(key("↵") { insert("\n") }, LayoutParams(0, dp(42), 1f))
        controls.addView(key("⌫") { backspace() }, LayoutParams(0, dp(42), 1f))
        controls.addView(key("Очистить") { target.text.clear() }, LayoutParams(0, dp(42), 1.35f))
        addView(controls)
    }

    private fun press(key: String) {
        when (key) {
            "−" -> insert("-")
            "×" -> insert("*")
            "÷" -> insert("/")
            "x²" -> insert("^2")
            "x³" -> insert("^3")
            "xⁿ" -> template("^()", 2)
            "√" -> template("sqrt()", 5)
            "a⁄b" -> template("()/()", 1)
            "|x|" -> template("||", 1)
            "log" -> template("log2()", 5)
            "ln" -> template("ln()", 3)
            else -> insert(key)
        }
    }

    private fun insert(value: String) {
        val start = target.selectionStart.coerceAtLeast(0)
        val end = target.selectionEnd.coerceAtLeast(start)
        target.text.replace(start, end, value)
        target.setSelection((start + value.length).coerceAtMost(target.text.length))
        target.requestFocus()
    }

    private fun template(value: String, cursorOffset: Int) {
        val start = target.selectionStart.coerceAtLeast(0)
        val end = target.selectionEnd.coerceAtLeast(start)
        target.text.replace(start, end, value)
        target.setSelection((start + cursorOffset).coerceIn(0, target.text.length))
        target.requestFocus()
    }

    private fun move(delta: Int) {
        target.setSelection((target.selectionStart + delta).coerceIn(0, target.text.length))
        target.requestFocus()
    }

    private fun backspace() {
        val start = target.selectionStart.coerceAtLeast(0)
        val end = target.selectionEnd.coerceAtLeast(start)
        if (end > start) target.text.delete(start, end)
        else if (start > 0) target.text.delete(start - 1, start)
        target.requestFocus()
    }

    private fun key(textValue: String, action: () -> Unit): View = Button(context).apply {
        text = textValue
        textSize = 13f
        isAllCaps = false
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(30, 31, 37))
        background = shape(Color.rgb(247, 247, 251), 9, Color.rgb(226, 227, 233))
        stateListAnimator = null
        setPadding(0, 0, 0, 0)
        setOnClickListener { action() }
    }

    private fun shape(fill: Int, radius: Int, stroke: Int): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(radius).toFloat()
        setColor(fill)
        setStroke(dp(1), stroke)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
