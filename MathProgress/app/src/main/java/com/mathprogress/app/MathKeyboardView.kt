package com.mathprogress.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

class MathKeyboardView(
    context: Context,
    private val target: EditText,
    private val darkMode: Boolean = false
) : LinearLayout(context) {

    private val accent = Color.rgb(99, 91, 255)
    private val surface = if (darkMode) Color.rgb(31, 32, 38) else Color.WHITE
    private val keySurface = if (darkMode) Color.rgb(42, 43, 51) else Color.rgb(247, 247, 251)
    private val border = if (darkMode) Color.rgb(57, 59, 68) else Color.rgb(225, 226, 233)
    private val fg = if (darkMode) Color.rgb(245, 245, 247) else Color.rgb(29, 30, 35)

    init {
        orientation = VERTICAL
        setPadding(dp(7), dp(7), dp(7), dp(7))
        background = shape(surface, 16, border)
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
                val special = symbol in listOf("+", "−", "×", "÷", "=", "√", "x²", "x³", "xⁿ", "log", "ln")
                row.addView(key(symbol, special) { press(symbol) }, LayoutParams(0, dp(44), 1f).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                })
            }
            addView(row)
        }

        val controls = LinearLayout(context).apply { orientation = HORIZONTAL }
        controls.addView(key("←", false) { move(-1) }, LayoutParams(0, dp(44), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        controls.addView(key("→", false) { move(1) }, LayoutParams(0, dp(44), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        controls.addView(key("↵", false) { insert("\n") }, LayoutParams(0, dp(44), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        controls.addView(key("⌫", false) { backspace() }, LayoutParams(0, dp(44), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        controls.addView(key("Очистить", false) { target.text.clear() }, LayoutParams(0, dp(44), 1.45f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
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

    private fun key(textValue: String, special: Boolean, action: () -> Unit): View = Button(context).apply {
        text = textValue
        textSize = if (textValue.length > 5) 12f else 15f
        typeface = if (special) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        isAllCaps = false
        gravity = Gravity.CENTER
        setTextColor(if (special) accent else fg)
        background = shape(if (special) Color.argb(if (darkMode) 45 else 20, 99, 91, 255) else keySurface, 10, border)
        stateListAnimator = null
        minWidth = 0
        minHeight = 0
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
