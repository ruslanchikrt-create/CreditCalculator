package com.mathprogress.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class MainActivity : Activity() {
    private lateinit var store: LocalStore
    private lateinit var root: FrameLayout
    private lateinit var content: FrameLayout
    private lateinit var drawer: FrameLayout
    private lateinit var drawerList: LinearLayout
    private var current = "results"
    private var period = StatsPeriod.WEEK
    private var historyDate: Calendar? = null
    private var historyNewest = true
    private var avatarProfileId: String? = null

    private val accent = Color.rgb(99, 91, 255)
    private val green = Color.rgb(29, 164, 94)
    private val red = Color.rgb(210, 58, 65)

    companion object {
        const val REQ_AVATAR = 101
        const val REQ_BACKUP_SAVE = 102
        const val REQ_BACKUP_OPEN = 103
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LocalStore(this)
        NotificationScheduler.ensureChannel(this)
        requestNotificationPermission()
        buildShell()
        showResults()
        if (store.hasPassword()) showUnlock()
        store.setLastOpenNow()
        NotificationScheduler.scheduleInactive(this, store)
    }

    override fun onPause() {
        NotificationScheduler.scheduleUnfinished(this, store)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) {
            store.setLastOpenNow()
            NotificationScheduler.scheduleInactive(this, store)
        }
    }

    private fun buildShell() {
        root = FrameLayout(this).apply { setBackgroundColor(bg()) }
        val vertical = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg()) }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(10), dp(6))
            setBackgroundColor(cardColor())
            elevation = dp(3).toFloat()
        }
        top.addView(button("☰") { openDrawer() }, LinearLayout.LayoutParams(dp(48), dp(44)))
        top.addView(text(L("Математика — Прогресс", "MathProgress"), 18f, true), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(8), 0, 0, 0) })
        top.addView(button("●") { showProfiles() }, LinearLayout.LayoutParams(dp(44), dp(44)))
        vertical.addView(top, LinearLayout.LayoutParams(-1, dp(58)))

        content = FrameLayout(this)
        vertical.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        val ad = TextView(this).apply {
            text = L("Место для рекламного баннера", "Advertising banner area")
            gravity = Gravity.CENTER
            textSize = 11f
            setTextColor(muted())
            setBackgroundColor(if (dark()) Color.rgb(25, 26, 31) else Color.rgb(238, 239, 244))
        }
        vertical.addView(ad, LinearLayout.LayoutParams(-1, dp(52)))
        root.addView(vertical, FrameLayout.LayoutParams(-1, -1))

        drawer = FrameLayout(this).apply {
            visibility = View.GONE
            isClickable = true
            setOnClickListener { closeDrawer() }
        }
        drawerList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(28), dp(14), dp(20))
            setBackgroundColor(cardColor())
            isClickable = true
            setOnClickListener { }
            elevation = dp(18).toFloat()
        }
        drawer.addView(drawerList, FrameLayout.LayoutParams(dp(300), -1, Gravity.START))
        root.addView(drawer, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        rebuildDrawer()
    }

    private fun rebuildDrawer() {
        drawerList.removeAllViews()
        val profile = store.activeProfile()
        drawerList.addView(text(profile.name, 20f, true))
        drawerList.addView(text(L("Активный профиль", "Active profile"), 12f, false, muted()))
        drawerList.addView(space(16))
        val items = listOf(
            Triple("results", "⌂", L("Результаты", "Results")),
            Triple("solve", "∑", L("Решить задачу", "Solve")),
            Triple("practice", "✓", L("Проверка знаний", "Practice")),
            Triple("history", "↺", L("История", "History")),
            Triple("mistakes", "!", L("Мои ошибки", "My mistakes")),
            Triple("trash", "♲", L("Корзина", "Trash")),
            Triple("profiles", "●", L("Профили", "Profiles")),
            Triple("settings", "⚙", L("Настройки", "Settings")),
            Triple("guide", "ⓘ", L("Инструкция", "Guide"))
        )
        items.forEach { (key, icon, title) ->
            val b = Button(this).apply {
                text = "$icon   $title"
                textSize = 14f
                isAllCaps = false
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setPadding(dp(12), 0, 0, 0)
                setTextColor(if (current == key) accent else fg())
                background = round(if (current == key) Color.argb(28, 99, 91, 255) else Color.TRANSPARENT, 12)
                stateListAnimator = null
                setOnClickListener {
                    closeDrawer()
                    when (key) {
                        "results" -> showResults()
                        "solve" -> showSolve()
                        "practice" -> showPractice()
                        "history" -> showHistory()
                        "mistakes" -> showMistakes()
                        "trash" -> showTrash()
                        "profiles" -> showProfiles()
                        "settings" -> showSettings()
                        "guide" -> showGuide()
                    }
                }
            }
            drawerList.addView(b, LinearLayout.LayoutParams(-1, dp(46)))
        }
    }

    private fun openDrawer() { rebuildDrawer(); drawer.setBackgroundColor(Color.argb(115, 0, 0, 0)); drawer.visibility = View.VISIBLE }
    private fun closeDrawer() { drawer.visibility = View.GONE }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (drawer.visibility == View.VISIBLE) closeDrawer() else super.onBackPressed()
    }

    private fun switchScreen(key: String, view: View) {
        current = key
        content.removeAllViews()
        content.addView(view, FrameLayout.LayoutParams(-1, -1))
        rebuildDrawer()
    }

    private fun showResults() {
        val (scroll, box) = page()
        box.addView(text(L("Результаты", "Results"), 28f, true))
        box.addView(text(store.activeProfile().name, 14f, false, muted()))
        box.addView(space(12))

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            StatsPeriod.WEEK to L("Неделя", "Week"),
            StatsPeriod.MONTH to L("Месяц", "Month"),
            StatsPeriod.YEAR to L("Год", "Year"),
            StatsPeriod.ALL to L("Всё", "All")
        ).forEach { (p, title) ->
            tabs.addView(button(title, period == p) { period = p; showResults() }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        box.addView(tabs)
        box.addView(space(12))

        val s = StatsEngine.snapshot(store.history(), period)
        val hero = card()
        hero.addView(text(s.message, 20f, true))
        if (s.comparison.isNotBlank()) hero.addView(text(s.comparison, 14f, true, green).apply { setPadding(0, dp(7), 0, 0) })
        hero.addView(space(14))
        val metrics = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        metrics.addView(metric(L("Средняя", "Average"), s.averageGrade?.let { String.format(Locale.US, "%.1f", it) } ?: "—"), LinearLayout.LayoutParams(0, -2, 1f))
        metrics.addView(metric(L("Точность", "Accuracy"), s.accuracy?.let { "$it%" } ?: "—"), LinearLayout.LayoutParams(0, -2, 1f))
        metrics.addView(metric(L("Решено", "Solved"), s.solved.toString()), LinearLayout.LayoutParams(0, -2, 1f))
        hero.addView(metrics)
        box.addView(hero)
        box.addView(space(10))

        val topics = card()
        topics.addView(text(L("Сильная тема: ", "Strong topic: ") + (s.bestTopic ?: "—"), 14f, true, if (s.bestTopic == null) muted() else green))
        topics.addView(text(L("Стоит повторить: ", "Review: ") + (s.weakTopic ?: "—"), 14f, true, if (s.weakTopic == null) muted() else red))
        box.addView(topics)

        val draft = store.getDraft()
        if (draft.isNotBlank()) {
            box.addView(space(10))
            val c = card()
            c.addView(text(L("Незавершённая задача", "Unfinished task"), 17f, true))
            c.addView(text(draft.take(110), 14f, false, muted()))
            c.addView(space(8))
            c.addView(primary(L("Продолжить", "Continue")) { showSolve(draft) })
            box.addView(c)
        }
        box.addView(space(12))
        box.addView(primary(L("Решить задачу", "Solve a problem")) { showSolve() })
        box.addView(space(8))
        box.addView(outline(L("Проверить знания", "Practice")) { showPractice() })
        box.addView(space(24))
        switchScreen("results", scroll)
    }

    private fun showSolve(prefill: String = "") {
        val (scroll, box) = page()
        box.addView(text(L("Решить задачу", "Solve a problem"), 27f, true))
        box.addView(text(L("Введите выражение и получите полное пошаговое решение.", "Enter an expression for a step-by-step solution."), 14f, false, muted()))
        box.addView(space(10))

        val types = arrayOf("Авто", "Линейное", "Квадратное", "Кубическое", "Система", "Дроби", "Корни", "Показательное", "Логарифмическое")
        val spinner = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, types) }
        box.addView(spinner, LinearLayout.LayoutParams(-1, dp(50)))

        val input = EditText(this).apply {
            textSize = 19f
            setTextColor(fg())
            setHintTextColor(muted())
            hint = "x^2 - 5x + 6 = 0"
            gravity = Gravity.TOP
            minLines = 4
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = round(cardColor(), 14, border())
            showSoftInputOnFocus = false
            setText(if (prefill.isNotBlank()) prefill else store.getDraft())
        }
        input.addTextChangedListener(SimpleTextWatcher { store.setDraft(it) })
        box.addView(input, LinearLayout.LayoutParams(-1, dp(140)))
        box.addView(space(8))
        box.addView(MathKeyboardView(this, input))
        box.addView(space(8))
        box.addView(outline("ⓘ  " + L("Примеры ввода", "Examples")) { showExamples() })
        box.addView(space(10))

        val resultBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(primary(L("Решить подробно", "Solve step by step")) {
            val forced = when (spinner.selectedItemPosition) {
                1 -> "linear"; 2 -> "quadratic"; 3 -> "cubic"; 4 -> "system"
                5 -> "fraction"; 6 -> "root"; 7 -> "exponential"; 8 -> "log"; else -> "auto"
            }
            val result = MathEngine.solve(input.text.toString(), forced)
            resultBox.removeAllViews()
            if (!result.success) {
                resultBox.addView(messageCard(result.error ?: L("Не удалось решить", "Could not solve"), red))
            } else {
                resultBox.addView(solutionCard(result))
                resultBox.addView(space(8))
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                row.addView(outline(L("Поделиться", "Share")) { shareResult(result) }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(4), 0) })
                row.addView(outline(L("Проверить себя", "Check myself")) { selfCheck(result) }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(4), 0, 0, 0) })
                resultBox.addView(row)
                store.addOrUpdateTask(TaskRecord(profileId = store.activeProfileId, input = result.input, type = result.type, answer = result.answer, steps = result.steps))
                store.clearDraft()
                NotificationScheduler.scheduleUnfinished(this, store)
            }
        })
        box.addView(space(10))
        box.addView(resultBox)
        box.addView(space(24))
        switchScreen("solve", scroll)
    }

    private fun selfCheck(result: SolveResult) {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(12), 0) }
        val answer = EditText(this).apply {
            hint = L("Ваш ответ", "Your answer")
            textSize = 18f
            setTextColor(fg())
            setHintTextColor(muted())
            showSoftInputOnFocus = false
        }
        wrap.addView(answer, LinearLayout.LayoutParams(-1, dp(55)))
        wrap.addView(MathKeyboardView(this, answer))
        val dialog = AlertDialog.Builder(this).setTitle(L("Решите самостоятельно", "Solve yourself")).setView(wrap)
            .setNegativeButton(L("Отмена", "Cancel"), null).setPositiveButton(L("Проверить", "Check"), null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val ok = if (result.numericAnswers.isNotEmpty()) PracticeEngine.check(answer.text.toString(), result.numericAnswers) else answer.text.toString().trim() == result.answer.trim()
                store.addOrUpdateTask(TaskRecord(profileId = store.activeProfileId, input = result.input, type = result.type, answer = result.answer, steps = result.steps, selfSolved = true, checked = true, correct = ok))
                AlertDialog.Builder(this).setTitle(if (ok) L("Верно!", "Correct!") else L("Есть ошибка", "There is a mistake"))
                    .setMessage(if (ok) L("Отлично! Решение правильное.", "Great! Your answer is correct.") else L("Правильный ответ: ", "Correct answer: ") + result.answer)
                    .setPositiveButton("OK", null).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showPractice() {
        val (scroll, box) = page()
        box.addView(text(L("Проверка знаний", "Practice"), 27f, true))
        box.addView(text(L("Приложение случайно создаёт задания и ставит оценку.", "Random tasks with automatic grading."), 14f, false, muted()))
        box.addView(space(10))
        val topic = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, PracticeEngine.topics()) }
        box.addView(topic, LinearLayout.LayoutParams(-1, dp(50)))
        val count = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("5 заданий", "10 заданий", "15 заданий")) }
        box.addView(count, LinearLayout.LayoutParams(-1, dp(50)))
        box.addView(space(10))
        box.addView(primary(L("Начать проверку", "Start practice")) {
            val total = when (count.selectedItemPosition) { 1 -> 10; 2 -> 15; else -> 5 }
            practiceQuestion(topic.selectedItem.toString(), total, 1, 0)
        })
        box.addView(space(24))
        switchScreen("practice", scroll)
    }

    private fun practiceQuestion(topic: String, total: Int, index: Int, correct: Int) {
        if (index > total) {
            val grade = PracticeEngine.grade(correct, total)
            val percent = if (total == 0) 0 else correct * 100 / total
            val title = when { percent == 100 -> L("Без ошибок! Блестяще!", "Perfect! Brilliant!"); percent >= 90 -> L("Отличный результат!", "Excellent result!"); percent >= 75 -> L("Очень хорошо!", "Very good!"); else -> L("Тренировка завершена", "Practice complete") }
            AlertDialog.Builder(this).setTitle(title).setMessage(L("Правильно: $correct из $total\nТочность: $percent%\nОценка: $grade", "Correct: $correct of $total\nAccuracy: $percent%\nGrade: $grade"))
                .setPositiveButton("OK") { _, _ -> showResults() }.show()
            return
        }
        val q = PracticeEngine.generate(topic)
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(5), dp(14), 0) }
        wrap.addView(text(L("Задание $index из $total", "Task $index of $total"), 13f, true, accent))
        wrap.addView(text(q.input, 22f, true).apply { setPadding(0, dp(8), 0, dp(8)) })
        val answer = EditText(this).apply { hint = L("Ваш ответ", "Your answer"); textSize = 18f; setTextColor(fg()); setHintTextColor(muted()); showSoftInputOnFocus = false }
        wrap.addView(answer, LinearLayout.LayoutParams(-1, dp(55)))
        wrap.addView(MathKeyboardView(this, answer))
        val dialog = AlertDialog.Builder(this).setTitle(L("Проверка знаний", "Practice")).setView(wrap).setNegativeButton("ⓘ") { _, _ -> showHint(q) }
            .setPositiveButton(L("Ответить", "Answer"), null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val ok = PracticeEngine.check(answer.text.toString(), q.expected)
                store.addOrUpdateTask(TaskRecord(profileId = store.activeProfileId, input = q.input, type = q.type, answer = q.answerText, steps = MathEngine.solve(q.input).steps, selfSolved = true, checked = true, correct = ok, source = "practice"))
                dialog.dismiss()
                AlertDialog.Builder(this).setTitle(if (ok) L("Верно!", "Correct!") else L("Ошибка", "Incorrect"))
                    .setMessage(if (ok) L("Так держать!", "Keep it up!") else L("Правильный ответ: ", "Correct answer: ") + q.answerText)
                    .setPositiveButton(L("Дальше", "Next")) { _, _ -> practiceQuestion(topic, total, index + 1, correct + if (ok) 1 else 0) }.show()
            }
        }
        dialog.show()
    }

    private fun showHistory() {
        val (scroll, box) = page()
        box.addView(text(L("История", "History"), 27f, true))
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(outline(L("Выбрать дату", "Choose date")) { chooseHistoryDate() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(4), 0) })
        controls.addView(outline(if (historyNewest) L("Новые ↓", "Newest ↓") else L("Старые ↑", "Oldest ↑")) { historyNewest = !historyNewest; showHistory() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(4), 0, 0, 0) })
        box.addView(controls)
        if (historyDate != null) {
            box.addView(space(6)); box.addView(outline(L("Сбросить дату", "Clear date")) { historyDate = null; showHistory() })
        }
        box.addView(space(10))
        var list = store.history()
        historyDate?.let { day -> list = list.filter { sameDay(it.createdAt, day) } }
        list = if (historyNewest) list.sortedByDescending { it.createdAt } else list.sortedBy { it.createdAt }
        if (list.isEmpty()) box.addView(messageCard(L("История пока пуста", "History is empty"), muted()))
        else list.forEach { task -> box.addView(taskCard(task)); box.addView(space(8)) }
        box.addView(space(24))
        switchScreen("history", scroll)
    }

    private fun taskCard(task: TaskRecord): View = card().apply {
        isClickable = true
        setOnClickListener { showTask(task) }
        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(task.createdAt))
        addView(text(task.type, 13f, true, accent))
        addView(text(date, 11f, false, muted()))
        addView(text(task.input, 18f, true).apply { setPadding(0, dp(5), 0, 0) })
        if (task.answer.isNotBlank()) addView(text(task.answer, 14f, false, green))
        if (task.checked) addView(text(if (task.correct) "✓ " + L("Верно", "Correct") else "! " + L("Ошибка", "Incorrect"), 12f, true, if (task.correct) green else red))
    }

    private fun showTask(task: TaskRecord) {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
        wrap.addView(text(task.type, 14f, true, accent))
        wrap.addView(text(task.input, 21f, true))
        wrap.addView(space(8))
        task.steps.forEachIndexed { i, step -> wrap.addView(text("${i + 1}. $step", 14f).apply { setPadding(0, dp(3), 0, dp(3)) }) }
        wrap.addView(text(L("Ответ: ", "Answer: ") + task.answer, 17f, true, green).apply { setPadding(0, dp(8), 0, 0) })
        val dialog = AlertDialog.Builder(this).setView(ScrollView(this).apply { addView(wrap) })
            .setNegativeButton(L("Закрыть", "Close"), null).setNeutralButton(L("Удалить", "Delete"), null).setPositiveButton(L("Поделиться", "Share"), null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { shareTask(task) }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { store.deleteToTrash(task.id); dialog.dismiss(); showHistory() }
        }
        dialog.show()
    }

    private fun showMistakes() {
        val (scroll, box) = page()
        box.addView(text(L("Мои ошибки", "My mistakes"), 27f, true))
        box.addView(space(10))
        val list = store.history().filter { it.checked && !it.correct }.sortedByDescending { it.createdAt }
        if (list.isEmpty()) box.addView(messageCard(L("Ошибок нет — отлично!", "No mistakes — great!"), green)) else list.forEach { box.addView(taskCard(it)); box.addView(space(8)) }
        switchScreen("mistakes", scroll)
    }

    private fun showTrash() {
        val (scroll, box) = page()
        box.addView(text(L("Корзина", "Trash"), 27f, true))
        box.addView(text(L("Удалённые решения хранятся 30 дней.", "Deleted solutions are kept for 30 days."), 14f, false, muted()))
        box.addView(space(10))
        val list = store.trash().sortedByDescending { it.deletedAt }
        if (list.isEmpty()) box.addView(messageCard(L("Корзина пуста", "Trash is empty"), muted()))
        list.forEach { task ->
            val days = max(0, 30 - ((System.currentTimeMillis() - task.deletedAt) / 86_400_000L).toInt())
            val c = card(); c.addView(text(task.input, 17f, true)); c.addView(text(L("Будет удалено через $days дн.", "Deleted in $days days"), 12f, false, muted()))
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(outline(L("Восстановить", "Restore")) { store.restore(task.id); showTrash() }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, dp(6), dp(4), 0) })
            row.addView(outline(L("Навсегда", "Forever"), red) { store.deleteForever(task.id); showTrash() }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(4), dp(6), 0, 0) })
            c.addView(row); box.addView(c); box.addView(space(8))
        }
        switchScreen("trash", scroll)
    }

    private fun showProfiles() {
        val (scroll, box) = page()
        box.addView(text(L("Профили", "Profiles"), 27f, true)); box.addView(space(10))
        store.profiles.forEach { p ->
            val c = card(); c.addView(text((if (p.id == store.activeProfileId) "✓ " else "") + p.name, 18f, true))
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(outline(L("Выбрать", "Select")) { store.activeProfileId = p.id; store.save(); showProfiles() }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, dp(6), dp(3), 0) })
            row.addView(outline(L("Имя", "Name")) { editProfile(p) }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), dp(6), dp(3), 0) })
            row.addView(outline(L("Аватар", "Avatar")) { pickAvatar(p) }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), dp(6), 0, 0) })
            c.addView(row); box.addView(c); box.addView(space(8))
        }
        box.addView(primary(L("Добавить пользователя", "Add user")) { addProfile() })
        switchScreen("profiles", scroll)
    }

    private fun showSettings() {
        val (scroll, box) = page()
        box.addView(text(L("Настройки", "Settings"), 27f, true)); box.addView(space(10))
        box.addView(setting(L("Режим", "Appearance"), store.settings.theme) { chooseTheme() })
        box.addView(setting(L("Язык", "Language"), if (store.settings.language == "ru") "Русский" else "English") { chooseLanguage() })
        box.addView(setting(L("Пароль", "Password"), if (store.hasPassword()) L("Установлен", "Set") else L("Не установлен", "Not set")) { setPasswordDialog() })
        box.addView(setting(L("Уведомления", "Notifications"), L("Незавершённые и после перерыва", "Unfinished and inactivity")) { notificationSettings() })
        box.addView(setting(L("Резервная копия", "Backup"), L("Сохранить / восстановить", "Save / restore")) { backupMenu() })
        box.addView(setting(L("Ошибка или пожелание", "Feedback"), L("Выбрать приложение для отправки", "Choose an app to send")) { feedback() })
        box.addView(setting(L("Сброс данных", "Reset data"), L("Только через пароль", "Password required"), red) { resetData() })
        switchScreen("settings", scroll)
    }

    private fun setting(title: String, subtitle: String, color: Int = fg(), action: () -> Unit): View = card().apply {
        setOnClickListener { action() }
        addView(text(title, 16f, true, color)); addView(text(subtitle, 12f, false, muted()))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(8)) }
    }

    private fun addProfile() {
        val e = EditText(this).apply { hint = L("Имя", "Name") }
        AlertDialog.Builder(this).setTitle(L("Новый пользователь", "New user")).setView(e).setNegativeButton(L("Отмена", "Cancel"), null)
            .setPositiveButton(L("Добавить", "Add")) { _, _ -> if (e.text.isNotBlank()) { val p = UserProfile(name = e.text.toString().trim()); store.profiles += p; store.activeProfileId = p.id; store.save(); showProfiles() } }.show()
    }

    private fun editProfile(profile: UserProfile) {
        val e = EditText(this).apply { setText(profile.name); selectAll() }
        AlertDialog.Builder(this).setTitle(L("Изменить имя", "Edit name")).setView(e).setNegativeButton(L("Отмена", "Cancel"), null)
            .setPositiveButton(L("Сохранить", "Save")) { _, _ -> if (e.text.isNotBlank()) { profile.name = e.text.toString().trim(); store.save(); showProfiles() } }.show()
    }

    private fun pickAvatar(profile: UserProfile) {
        avatarProfileId = profile.id
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }, REQ_AVATAR)
    }

    private fun chooseTheme() {
        val values = arrayOf(L("Как в системе", "System"), L("Светлый", "Light"), L("Тёмный", "Dark"))
        AlertDialog.Builder(this).setTitle(L("Режим оформления", "Appearance")).setItems(values) { _, which -> store.settings.theme = listOf("system", "light", "dark")[which]; store.save(); recreate() }.show()
    }

    private fun chooseLanguage() {
        AlertDialog.Builder(this).setTitle(L("Язык", "Language")).setItems(arrayOf("Русский", "English")) { _, which -> store.settings.language = if (which == 0) "ru" else "en"; store.save(); recreate() }.show()
    }

    private fun setPasswordDialog() {
        val e = EditText(this).apply { hint = L("Новый пароль", "New password"); inputType = 0x00000081 }
        AlertDialog.Builder(this).setTitle(L("Установить / сменить пароль", "Set / change password")).setView(e).setNegativeButton(L("Отмена", "Cancel"), null)
            .setPositiveButton(L("Сохранить", "Save")) { _, _ -> if (e.text.length >= 4) { store.setPassword(e.text.toString()); toast(L("Пароль сохранён", "Password saved")) } else toast(L("Минимум 4 символа", "At least 4 characters")) }.show()
    }

    private fun showUnlock() {
        val e = EditText(this).apply { hint = L("Пароль", "Password"); inputType = 0x00000081 }
        val d = AlertDialog.Builder(this).setTitle(L("Введите пароль", "Enter password")).setView(e).setCancelable(false).setPositiveButton(L("Открыть", "Unlock"), null).create()
        d.setOnShowListener { d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { if (store.checkPassword(e.text.toString())) d.dismiss() else e.error = L("Неверный пароль", "Wrong password") } }
        d.show()
    }

    private fun notificationSettings() {
        val labels = arrayOf(L("Незавершённые решения — без звука", "Unfinished solutions — silent"), L("Напоминать после нескольких дней", "Remind after inactivity"))
        val checked = booleanArrayOf(store.settings.unfinishedNotifications, store.settings.inactivityNotifications)
        AlertDialog.Builder(this).setTitle(L("Уведомления", "Notifications")).setMultiChoiceItems(labels, checked) { _, which, value -> checked[which] = value }
            .setPositiveButton(L("Сохранить", "Save")) { _, _ -> store.settings.notificationsEnabled = checked.any { it }; store.settings.unfinishedNotifications = checked[0]; store.settings.inactivityNotifications = checked[1]; store.save(); NotificationScheduler.scheduleUnfinished(this, store); NotificationScheduler.scheduleInactive(this, store) }.show()
    }

    private fun backupMenu() {
        AlertDialog.Builder(this).setTitle(L("Резервная копия", "Backup")).setItems(arrayOf(L("Создать копию", "Create backup"), L("Восстановить", "Restore"))) { _, which ->
            if (which == 0) startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "application/json"; putExtra(Intent.EXTRA_TITLE, "MathProgress-backup.json") }, REQ_BACKUP_SAVE)
            else startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "application/json"; addCategory(Intent.CATEGORY_OPENABLE) }, REQ_BACKUP_OPEN)
        }.show()
    }

    private fun feedback() {
        val e = EditText(this).apply { hint = L("Опишите ошибку или предложение", "Describe an issue or suggestion"); minLines = 5; gravity = Gravity.TOP }
        AlertDialog.Builder(this).setTitle(L("Написать разработчику", "Send feedback")).setView(e).setNegativeButton(L("Отмена", "Cancel"), null)
            .setPositiveButton(L("Выбрать приложение", "Choose app")) { _, _ -> if (e.text.isNotBlank()) shareText(L("Отзыв о MathProgress", "MathProgress feedback"), e.text.toString()) }.show()
    }

    private fun resetData() {
        if (!store.hasPassword()) { toast(L("Сначала установите пароль", "Set a password first")); return }
        val e = EditText(this).apply { hint = L("Пароль", "Password"); inputType = 0x00000081 }
        AlertDialog.Builder(this).setTitle(L("Подтвердите сброс", "Confirm reset")).setView(e).setNegativeButton(L("Отмена", "Cancel"), null)
            .setPositiveButton(L("Сбросить", "Reset")) { _, _ -> if (store.checkPassword(e.text.toString())) { store.resetAll(); recreate() } else toast(L("Неверный пароль", "Wrong password")) }.show()
    }

    private fun showGuide() {
        AlertDialog.Builder(this).setTitle(L("Инструкция", "Guide")).setMessage(
            L(
                "Главная страница показывает результаты за неделю, месяц, год и всё время. Меню открывается кнопкой ☰. В разделе решения используйте специальную математическую клавиатуру. Каждое решение сохраняется в историю. Удалённые задачи остаются в корзине 30 дней. В проверке знаний приложение создаёт случайные задания и ставит оценку. Настройки содержат профили, язык, режим, пароль, резервную копию и тихие уведомления.",
                "The home page shows results by week, month, year and all time. Use ☰ for navigation. Solve with the math keyboard, review history, practice random tasks, manage profiles, backups and silent reminders."
            )
        ).setPositiveButton("OK", null).show()
    }

    private fun showExamples() {
        AlertDialog.Builder(this).setTitle(L("Примеры ввода", "Examples")).setMessage(
            "3x+7=22\n\nx^2-5x+6=0\n\nx^3-6x^2+11x-6=0\n\n2x+y=5; x-y=1\n\n2x+y-z=1; x-y+2z=3; 3x+y+z=7\n\n(x+1)/(x-2)=3\n\nsqrt(2x+3)=5\n\n2^(3x-1)=16\n\nlog2(x+1)=3"
        ).setPositiveButton("OK", null).show()
    }

    private fun showHint(q: PracticeQuestion) {
        val hint = when {
            q.type.contains("Квадрат") -> L("Найдите дискриминант D = b² − 4ac.", "Find D = b² − 4ac.")
            q.type.contains("Система") -> L("Попробуйте метод сложения или подстановки.", "Try elimination or substitution.")
            q.type.contains("дроб", true) -> L("Сначала выпишите ОДЗ.", "Start with the domain restrictions.")
            q.type.contains("корн", true) -> L("Учтите ОДЗ и возведите обе части в квадрат.", "Use the domain and square both sides.")
            q.type.contains("Показ", true) -> L("Приведите к одному основанию или логарифмируйте.", "Use a common base or logarithms.")
            q.type.contains("Лог", true) -> L("Сначала выпишите ОДЗ.", "Start with the logarithm domain.")
            else -> L("Перенесите неизвестные в одну часть, числа — в другую.", "Move variables to one side and constants to the other.")
        }
        AlertDialog.Builder(this).setTitle("Подсказка ⓘ").setMessage(hint).setPositiveButton("OK", null).show()
    }

    private fun chooseHistoryDate() {
        val c = historyDate ?: Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d -> historyDate = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }; showHistory() }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun sameDay(time: Long, day: Calendar): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        return c.get(Calendar.YEAR) == day.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
    }

    private fun shareResult(r: SolveResult) = shareText(r.type, buildString { appendLine(r.type); appendLine(r.input); appendLine(); r.steps.forEachIndexed { i, s -> appendLine("${i + 1}. $s") }; appendLine(); append(L("Ответ: ", "Answer: ") + r.answer) })
    private fun shareTask(t: TaskRecord) = shareText(t.type, buildString { appendLine(t.type); appendLine(t.input); appendLine(); t.steps.forEachIndexed { i, s -> appendLine("${i + 1}. $s") }; appendLine(); append(L("Ответ: ", "Answer: ") + t.answer) })
    private fun shareText(title: String, body: String) {
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, title); putExtra(Intent.EXTRA_TEXT, body) }
        startActivity(Intent.createChooser(send, L("Через какое приложение отправить?", "Choose an app to share")))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!
        try {
            when (requestCode) {
                REQ_AVATAR -> {
                    try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
                    store.profiles.firstOrNull { it.id == avatarProfileId }?.avatarUri = uri.toString(); store.save(); showProfiles()
                }
                REQ_BACKUP_SAVE -> { contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(store.exportJson()) }; toast(L("Копия сохранена", "Backup saved")) }
                REQ_BACKUP_OPEN -> { val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return; store.importJson(text); recreate() }
            }
        } catch (e: Exception) { toast(e.message ?: L("Ошибка", "Error")) }
    }

    private fun solutionCard(r: SolveResult): View = card().apply {
        addView(text(r.type, 14f, true, accent)); addView(text(r.input, 20f, true).apply { setPadding(0, dp(5), 0, dp(8)) })
        addView(text(L("Полное решение", "Detailed solution"), 17f, true))
        r.steps.forEachIndexed { i, step -> addView(text("${i + 1}. $step", 14f).apply { setPadding(0, dp(3), 0, dp(3)) }) }
        addView(text(L("Ответ: ", "Answer: ") + r.answer, 18f, true, green).apply { setPadding(0, dp(8), 0, 0) })
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 900)
    }

    private fun page(): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(20), dp(18), dp(20)) }
        scroll.addView(box, ScrollView.LayoutParams(-1, -2))
        return scroll to box
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = round(cardColor(), 16, border())
    }

    private fun metric(title: String, value: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        addView(text(value, 24f, true)); addView(text(title, 12f, false, muted()))
    }

    private fun text(value: String, size: Float = 15f, bold: Boolean = false, color: Int = fg()): TextView = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT; setLineSpacing(0f, 1.06f)
    }

    private fun button(title: String, selected: Boolean = false, action: () -> Unit): Button = Button(this).apply {
        text = title; textSize = 13f; isAllCaps = false; setTextColor(if (selected) Color.WHITE else fg()); background = round(if (selected) accent else cardColor(), 12, if (selected) null else border()); stateListAnimator = null; setOnClickListener { action() }
    }

    private fun primary(title: String, action: () -> Unit): Button = Button(this).apply {
        text = title; textSize = 14f; isAllCaps = false; setTextColor(Color.WHITE); background = round(accent, 13); stateListAnimator = null; setOnClickListener { action() }; layoutParams = LinearLayout.LayoutParams(-1, dp(50))
    }

    private fun outline(title: String, color: Int = fg(), action: () -> Unit): Button = Button(this).apply {
        text = title; textSize = 13f; isAllCaps = false; setTextColor(color); background = round(cardColor(), 12, border()); stateListAnimator = null; setOnClickListener { action() }
    }

    private fun messageCard(message: String, color: Int): View = card().apply { addView(text(message, 16f, true, color)) }
    private fun space(height: Int): View = Space(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }

    private fun round(fill: Int, radius: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(radius).toFloat(); setColor(fill); if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun dark(): Boolean = when (store.settings.theme) {
        "dark" -> true; "light" -> false
        else -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
    private fun bg() = if (dark()) Color.rgb(18, 19, 23) else Color.rgb(246, 247, 251)
    private fun cardColor() = if (dark()) Color.rgb(31, 32, 38) else Color.WHITE
    private fun fg() = if (dark()) Color.rgb(244, 244, 246) else Color.rgb(27, 28, 33)
    private fun muted() = if (dark()) Color.rgb(165, 167, 177) else Color.rgb(108, 111, 123)
    private fun border() = if (dark()) Color.rgb(52, 54, 63) else Color.rgb(228, 229, 235)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun L(ru: String, en: String) = if (store.settings.language == "en") en else ru
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
}

class SimpleTextWatcher(private val callback: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { callback(s?.toString().orEmpty()) }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
