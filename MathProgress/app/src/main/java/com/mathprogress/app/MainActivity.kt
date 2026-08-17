package com.mathprogress.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
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
    private val green = Color.rgb(31, 171, 96)
    private val red = Color.rgb(214, 64, 69)
    private val amber = Color.rgb(230, 154, 40)

    companion object {
        const val REQ_AVATAR = 101
        const val REQ_BACKUP_SAVE = 102
        const val REQ_BACKUP_OPEN = 103
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LocalStore(this)
        configureSystemBars()
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

    private fun configureSystemBars() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = bg()
        var flags = window.decorView.systemUiVisibility
        flags = if (!dark()) flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        if (Build.VERSION.SDK_INT >= 26) flags = if (!dark()) flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        window.decorView.systemUiVisibility = flags
    }

    private fun buildShell() {
        root = FrameLayout(this).apply { setBackgroundColor(bg()) }
        val vertical = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg()) }
        val statusSpacer = Space(this)
        vertical.addView(statusSpacer, LinearLayout.LayoutParams(1, 0))

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setBackgroundColor(cardColor())
            elevation = dp(2).toFloat()
        }
        top.addView(iconButton(R.drawable.ic_menu) { openDrawer() }, LinearLayout.LayoutParams(dp(44), dp(44)))
        top.addView(text(L("Математика", "MathProgress"), 19f, true), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(12), 0, dp(8), 0) })
        val topAvatar = avatar(store.activeProfile(), 40).apply { setOnClickListener { profileActions(store.activeProfile()) } }
        top.addView(topAvatar, LinearLayout.LayoutParams(dp(40), dp(40)))
        vertical.addView(top, LinearLayout.LayoutParams(-1, dp(60)))

        content = FrameLayout(this)
        vertical.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        // Пустая зона для будущего рекламного баннера. Никаких надписей пользователю.
        val adSlot = FrameLayout(this).apply { setBackgroundColor(bg()) }
        vertical.addView(adSlot, LinearLayout.LayoutParams(-1, dp(50)))
        val bottomSpacer = Space(this)
        vertical.addView(bottomSpacer, LinearLayout.LayoutParams(1, 0))
        root.addView(vertical, FrameLayout.LayoutParams(-1, -1))

        drawer = FrameLayout(this).apply {
            visibility = View.GONE
            isClickable = true
            setOnClickListener { closeDrawer() }
        }
        drawerList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(18), dp(14), dp(18))
            setBackgroundColor(cardColor())
            isClickable = true
            setOnClickListener { }
            elevation = dp(16).toFloat()
        }
        drawer.addView(drawerList, FrameLayout.LayoutParams(dp(310), -1, Gravity.START))
        root.addView(drawer, FrameLayout.LayoutParams(-1, -1))

        root.setOnApplyWindowInsetsListener { _, insets ->
            val topInset: Int
            val bottomInset: Int
            if (Build.VERSION.SDK_INT >= 30) {
                topInset = insets.getInsets(WindowInsets.Type.statusBars()).top
                bottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                topInset = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottomInset = insets.systemWindowInsetBottom
            }
            statusSpacer.layoutParams = LinearLayout.LayoutParams(1, topInset)
            bottomSpacer.layoutParams = LinearLayout.LayoutParams(1, bottomInset)
            drawerList.setPadding(dp(14), topInset + dp(16), dp(14), dp(18))
            insets
        }
        setContentView(root)
        root.requestApplyInsets()
        rebuildDrawer()
    }

    private fun rebuildDrawer() {
        drawerList.removeAllViews()
        val profile = store.activeProfile()

        val profileCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(8), dp(10))
            background = round(softAccent(), 16)
            setOnClickListener { showProfileChooser() }
        }
        profileCard.addView(avatar(profile, 54), LinearLayout.LayoutParams(dp(54), dp(54)))
        val profileTexts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(4), 0) }
        profileTexts.addView(text(profile.name, 17f, true))
        profileTexts.addView(text(L("Активный профиль · нажмите для смены", "Active profile · tap to switch"), 11f, false, muted()))
        profileCard.addView(profileTexts, LinearLayout.LayoutParams(0, -2, 1f))
        profileCard.addView(iconButton(R.drawable.ic_edit, softAccent()) { profileActions(profile) }, LinearLayout.LayoutParams(dp(40), dp(40)))
        drawerList.addView(profileCard, LinearLayout.LayoutParams(-1, -2))
        drawerList.addView(space(16))

        val items = listOf(
            DrawerItem("results", R.drawable.ic_home, L("Результаты", "Results")) { showResults() },
            DrawerItem("solve", R.drawable.ic_calculate, L("Решить задачу", "Solve")) { showSolve() },
            DrawerItem("practice", R.drawable.ic_school, L("Проверка знаний", "Practice")) { showPractice() },
            DrawerItem("history", R.drawable.ic_history, L("История", "History")) { showHistory() },
            DrawerItem("mistakes", R.drawable.ic_warning, L("Мои ошибки", "My mistakes")) { showMistakes() },
            DrawerItem("trash", R.drawable.ic_delete, L("Корзина", "Trash")) { showTrash() },
            DrawerItem("profiles", R.drawable.ic_users, L("Пользователи", "Users")) { showProfiles() },
            DrawerItem("settings", R.drawable.ic_settings, L("Настройки", "Settings")) { showSettings() },
            DrawerItem("guide", R.drawable.ic_info, L("Инструкция", "Guide")) { showGuide() }
        )
        items.forEach { item -> drawerList.addView(drawerItem(item), LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, dp(2), 0, dp(2)) }) }

        drawerList.addView(space(10))
        drawerList.addView(themeQuickRow())
    }

    private data class DrawerItem(val key: String, val icon: Int, val title: String, val action: () -> Unit)

    private fun drawerItem(item: DrawerItem): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), 0, dp(10), 0)
        val selected = current == item.key
        background = round(if (selected) softAccent() else Color.TRANSPARENT, 13)
        addView(icon(item.icon, 22, if (selected) accent else muted()), LinearLayout.LayoutParams(dp(30), dp(30)))
        addView(text(item.title, 14f, selected, if (selected) accent else fg()), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(8), 0, 0, 0) })
        setOnClickListener { closeDrawer(); item.action() }
    }

    private fun themeQuickRow(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(8), dp(12), dp(8))
        background = round(surface2(), 14)
        addView(icon(R.drawable.ic_sun, 21, if (!dark()) amber else muted()), LinearLayout.LayoutParams(dp(30), dp(30)))
        val sw = Switch(this@MainActivity).apply {
            isChecked = dark()
            buttonTintList = null
            setOnCheckedChangeListener { _, checked ->
                store.settings.theme = if (checked) "dark" else "light"
                store.save(); recreate()
            }
        }
        addView(sw, LinearLayout.LayoutParams(0, -2, 1f))
        addView(icon(R.drawable.ic_moon, 21, if (dark()) accent else muted()), LinearLayout.LayoutParams(dp(30), dp(30)))
    }

    private fun openDrawer() { rebuildDrawer(); drawer.setBackgroundColor(Color.argb(120, 0, 0, 0)); drawer.visibility = View.VISIBLE }
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
        box.addView(sectionTitle(R.drawable.ic_home, L("Результаты", "Results"), L("Ваша успеваемость и прогресс", "Your learning progress")))
        box.addView(space(16))

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; background = round(surface2(), 14); setPadding(dp(4), dp(4), dp(4), dp(4)) }
        listOf(
            StatsPeriod.WEEK to L("Неделя", "Week"), StatsPeriod.MONTH to L("Месяц", "Month"),
            StatsPeriod.YEAR to L("Год", "Year"), StatsPeriod.ALL to L("Всё", "All")
        ).forEach { (p, title) ->
            tabs.addView(segment(title, period == p) { period = p; showResults() }, LinearLayout.LayoutParams(0, dp(42), 1f))
        }
        box.addView(tabs)
        box.addView(space(14))

        val s = StatsEngine.snapshot(store.history(), period)
        val hero = card(if (s.solved == 0) cardColor() else softAccent())
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(icon(R.drawable.ic_trophy, 27, if (s.solved == 0) muted() else accent), LinearLayout.LayoutParams(dp(40), dp(40)))
        head.addView(text(s.message, 19f, true), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(8), 0, 0, 0) })
        hero.addView(head)
        if (s.comparison.isNotBlank()) hero.addView(text(s.comparison, 14f, true, green).apply { setPadding(dp(48), dp(4), 0, 0) })
        box.addView(hero)
        box.addView(space(10))

        val metrics = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        metrics.addView(metricCard(R.drawable.ic_school, L("Средняя", "Average"), s.averageGrade?.let { String.format(Locale.US, "%.1f", it) } ?: "—", accent), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, 0, dp(4), 0) })
        metrics.addView(metricCard(R.drawable.ic_check, L("Точность", "Accuracy"), s.accuracy?.let { "$it%" } ?: "—", green), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        metrics.addView(metricCard(R.drawable.ic_calculate, L("Решено", "Solved"), s.solved.toString(), amber), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, 0, 0) })
        box.addView(metrics)
        box.addView(space(10))

        val topics = card()
        topics.addView(infoLine(R.drawable.ic_trophy, L("Сильная тема", "Strong topic"), s.bestTopic ?: "—", if (s.bestTopic == null) muted() else green))
        topics.addView(divider())
        topics.addView(infoLine(R.drawable.ic_warning, L("Стоит повторить", "Review"), s.weakTopic ?: "—", if (s.weakTopic == null) muted() else red))
        box.addView(topics)

        val draft = store.getDraft()
        if (draft.isNotBlank()) {
            box.addView(space(10))
            val c = card()
            c.addView(infoLine(R.drawable.ic_history, L("Незавершённая задача", "Unfinished task"), draft.take(80), accent))
            c.addView(space(8))
            c.addView(actionButton(L("Продолжить", "Continue"), R.drawable.ic_edit, true) { showSolve(draft) })
            box.addView(c)
        }

        box.addView(space(14))
        box.addView(actionButton(L("Решить задачу", "Solve a problem"), R.drawable.ic_calculate, true) { showSolve() })
        box.addView(space(8))
        box.addView(actionButton(L("Проверить знания", "Practice"), R.drawable.ic_school, false) { showPractice() })
        box.addView(space(20))
        switchScreen("results", scroll)
    }

    private fun showSolve(prefill: String = "") {
        val (scroll, box) = page()
        box.addView(sectionTitle(R.drawable.ic_calculate, L("Решить задачу", "Solve"), L("Уравнения, системы и обычные вычисления", "Equations, systems and calculations")))
        box.addView(space(14))

        val types = arrayOf("Авто", "Линейное", "Квадратное", "Кубическое", "Система", "Дроби", "Корни", "Показательное", "Логарифмическое")
        val spinner = styledSpinner(types.toList())
        box.addView(card().apply { addView(spinner, LinearLayout.LayoutParams(-1, dp(48))) })
        box.addView(space(10))

        val input = EditText(this).apply {
            textSize = 19f; setTextColor(fg()); setHintTextColor(muted())
            hint = L("Например: x² − 5x + 6 = 0", "Example: x² − 5x + 6 = 0")
            gravity = Gravity.TOP; minLines = 4
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = round(cardColor(), 16, border())
            showSoftInputOnFocus = false
            setText(if (prefill.isNotBlank()) prefill else store.getDraft())
        }
        input.addTextChangedListener(SimpleTextWatcher { store.setDraft(it) })
        box.addView(input, LinearLayout.LayoutParams(-1, dp(136)))
        box.addView(space(8))
        box.addView(MathKeyboardView(this, input, dark()))
        box.addView(space(8))
        box.addView(actionButton(L("Примеры ввода", "Input examples"), R.drawable.ic_info, false) { showExamples() })
        box.addView(space(10))

        val resultBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(actionButton(L("Решить подробно", "Solve step by step"), R.drawable.ic_check, true) {
            val forced = when (spinner.selectedItemPosition) {
                1 -> "linear"; 2 -> "quadratic"; 3 -> "cubic"; 4 -> "system"; 5 -> "fraction"; 6 -> "root"; 7 -> "exponential"; 8 -> "log"; else -> "auto"
            }
            val result = MathEngine.solve(input.text.toString(), forced)
            resultBox.removeAllViews()
            if (!result.success) resultBox.addView(messageCard(result.error ?: L("Не удалось решить", "Could not solve"), red, R.drawable.ic_warning))
            else {
                resultBox.addView(solutionCard(result))
                resultBox.addView(space(8))
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                row.addView(actionButton(L("Поделиться", "Share"), R.drawable.ic_share, false) { shareResult(result) }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(0, 0, dp(4), 0) })
                row.addView(actionButton(L("Проверить себя", "Check myself"), R.drawable.ic_school, false) { selfCheck(result) }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(4), 0, 0, 0) })
                resultBox.addView(row)
                store.addOrUpdateTask(TaskRecord(profileId = store.activeProfileId, input = result.input, type = result.type, answer = result.answer, steps = result.steps))
                store.clearDraft(); NotificationScheduler.scheduleUnfinished(this, store)
            }
        })
        box.addView(space(10)); box.addView(resultBox); box.addView(space(20))
        switchScreen("solve", scroll)
    }

    private fun selfCheck(result: SolveResult) {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(12), 0) }
        wrap.addView(text(result.input, 18f, true).apply { setPadding(0, 0, 0, dp(8)) })
        val answer = EditText(this).apply { hint = L("Ваш ответ", "Your answer"); textSize = 18f; setTextColor(fg()); setHintTextColor(muted()); showSoftInputOnFocus = false }
        wrap.addView(answer, LinearLayout.LayoutParams(-1, dp(55))); wrap.addView(MathKeyboardView(this, answer, dark()))
        val dialog = D().setTitle(L("Решите самостоятельно", "Solve yourself")).setView(wrap).setNegativeButton(L("Отмена", "Cancel"), null).setPositiveButton(L("Проверить", "Check"), null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val ok = if (result.numericAnswers.isNotEmpty()) PracticeEngine.check(answer.text.toString(), result.numericAnswers) else answer.text.toString().trim() == result.answer.trim()
                store.addOrUpdateTask(TaskRecord(profileId = store.activeProfileId, input = result.input, type = result.type, answer = result.answer, steps = result.steps, selfSolved = true, checked = true, correct = ok))
                D().setTitle(if (ok) L("Верно!", "Correct!") else L("Есть ошибка", "There is a mistake")).setMessage(if (ok) L("Отлично! Решение правильное.", "Great! Your answer is correct.") else L("Правильный ответ: ", "Correct answer: ") + result.answer).setPositiveButton("OK", null).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showPractice() {
        val (scroll, box) = page()
        box.addView(sectionTitle(R.drawable.ic_school, L("Проверка знаний", "Practice"), L("Случайные задания с оценкой результата", "Random tasks with automatic grading")))
        box.addView(space(14))
        val topic = styledSpinner(PracticeEngine.topics())
        val count = styledSpinner(listOf("5 заданий", "10 заданий", "15 заданий"))
        val options = card(); options.addView(label(L("Тема", "Topic"))); options.addView(topic, LinearLayout.LayoutParams(-1, dp(48))); options.addView(space(8)); options.addView(label(L("Количество", "Count"))); options.addView(count, LinearLayout.LayoutParams(-1, dp(48)))
        box.addView(options); box.addView(space(12))
        box.addView(actionButton(L("Начать проверку", "Start practice"), R.drawable.ic_school, true) {
            val total = when (count.selectedItemPosition) { 1 -> 10; 2 -> 15; else -> 5 }
            practiceQuestion(topic.selectedItem.toString(), total, 1, 0)
        })
        box.addView(space(20)); switchScreen("practice", scroll)
    }

    private fun practiceQuestion(topic: String, total: Int, index: Int, correct: Int) {
        if (index > total) {
            val grade = PracticeEngine.grade(correct, total); val percent = if (total == 0) 0 else correct * 100 / total
            val title = when { percent == 100 -> L("Без ошибок! Блестяще!", "Perfect! Brilliant!"); percent >= 90 -> L("Отличный результат!", "Excellent result!"); percent >= 75 -> L("Очень хорошо!", "Very good!"); else -> L("Тренировка завершена", "Practice complete") }
            D().setTitle(title).setMessage(L("Правильно: $correct из $total\nТочность: $percent%\nОценка: $grade", "Correct: $correct of $total\nAccuracy: $percent%\nGrade: $grade")).setPositiveButton("OK") { _, _ -> showResults() }.show(); return
        }
        val q = PracticeEngine.generate(topic)
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(5), dp(14), 0) }
        wrap.addView(text(L("Задание $index из $total", "Task $index of $total"), 13f, true, accent)); wrap.addView(text(q.input, 22f, true).apply { setPadding(0, dp(8), 0, dp(8)) })
        val answer = EditText(this).apply { hint = L("Ваш ответ", "Your answer"); textSize = 18f; setTextColor(fg()); setHintTextColor(muted()); showSoftInputOnFocus = false }
        wrap.addView(answer, LinearLayout.LayoutParams(-1, dp(55))); wrap.addView(MathKeyboardView(this, answer, dark()))
        val dialog = D().setTitle(L("Проверка знаний", "Practice")).setView(wrap).setNegativeButton(L("Подсказка", "Hint")) { _, _ -> showHint(q) }.setPositiveButton(L("Ответить", "Answer"), null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val ok = PracticeEngine.check(answer.text.toString(), q.expected)
                store.addOrUpdateTask(TaskRecord(profileId = store.activeProfileId, input = q.input, type = q.type, answer = q.answerText, steps = MathEngine.solve(q.input).steps, selfSolved = true, checked = true, correct = ok, source = "practice"))
                dialog.dismiss()
                D().setTitle(if (ok) L("Верно!", "Correct!") else L("Ошибка", "Incorrect")).setMessage(if (ok) L("Так держать!", "Keep it up!") else L("Правильный ответ: ", "Correct answer: ") + q.answerText).setPositiveButton(L("Дальше", "Next")) { _, _ -> practiceQuestion(topic, total, index + 1, correct + if (ok) 1 else 0) }.show()
            }
        }
        dialog.show()
    }

    private fun showHistory() {
        val (scroll, box) = page()
        box.addView(sectionTitle(R.drawable.ic_history, L("История", "History"), L("Все решения сохраняются по датам", "Solutions saved by date")))
        box.addView(space(14))
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(actionButton(L("Дата", "Date"), R.drawable.ic_history, false) { chooseHistoryDate() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(4), 0) })
        controls.addView(actionButton(if (historyNewest) L("Новые сначала", "Newest") else L("Старые сначала", "Oldest"), R.drawable.ic_history, false) { historyNewest = !historyNewest; showHistory() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(4), 0, 0, 0) })
        box.addView(controls)
        if (historyDate != null) { box.addView(space(6)); box.addView(actionButton(L("Сбросить выбранную дату", "Clear date"), R.drawable.ic_delete, false) { historyDate = null; showHistory() }) }
        box.addView(space(10))
        var list = store.history(); historyDate?.let { day -> list = list.filter { sameDay(it.createdAt, day) } }; list = if (historyNewest) list.sortedByDescending { it.createdAt } else list.sortedBy { it.createdAt }
        if (list.isEmpty()) box.addView(messageCard(L("История пока пуста", "History is empty"), muted(), R.drawable.ic_history))
        else list.forEach { task -> box.addView(taskCard(task)); box.addView(space(8)) }
        box.addView(space(20)); switchScreen("history", scroll)
    }

    private fun taskCard(task: TaskRecord): View = card().apply {
        isClickable = true; setOnClickListener { showTask(task) }
        val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(icon(if (task.correct) R.drawable.ic_check else R.drawable.ic_calculate, 22, if (task.correct) green else accent), LinearLayout.LayoutParams(dp(34), dp(34)))
        val col = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        col.addView(text(task.type, 13f, true, accent)); col.addView(text(SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.getDefault()).format(Date(task.createdAt)), 11f, false, muted()))
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f)); addView(row)
        addView(text(task.input, 17f, true).apply { setPadding(0, dp(8), 0, dp(4)) })
        if (task.answer.isNotBlank()) addView(text(task.answer, 14f, false, green))
    }

    private fun showTask(task: TaskRecord) {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
        wrap.addView(text(task.type, 14f, true, accent)); wrap.addView(text(task.input, 20f, true)); wrap.addView(space(8))
        task.steps.forEachIndexed { i, step -> wrap.addView(text("${i + 1}. $step", 14f).apply { setPadding(0, dp(3), 0, dp(3)) }) }
        wrap.addView(text(L("Ответ: ", "Answer: ") + task.answer, 17f, true, green).apply { setPadding(0, dp(8), 0, dp(8)) })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(iconOnlyButton(R.drawable.ic_edit) { editTask(task) }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(4), 0) })
        actions.addView(iconOnlyButton(R.drawable.ic_share) { shareTask(task) }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        actions.addView(iconOnlyButton(R.drawable.ic_delete, red) { store.deleteToTrash(task.id); toast(L("Перемещено в корзину", "Moved to trash")); showHistory() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(4), 0, 0, 0) })
        wrap.addView(actions)
        D().setView(ScrollView(this).apply { addView(wrap) }).setNegativeButton(L("Закрыть", "Close"), null).show()
    }

    private fun editTask(task: TaskRecord) {
        val e = EditText(this).apply { setText(task.input); textSize = 18f; setTextColor(fg()); setHintTextColor(muted()); minLines = 3; gravity = Gravity.TOP }
        D().setTitle(L("Редактировать задачу", "Edit task")).setView(e).setNegativeButton(L("Отмена", "Cancel"), null).setPositiveButton(L("Пересчитать", "Recalculate")) { _, _ ->
            val r = MathEngine.solve(e.text.toString())
            if (r.success) { task.input = r.input; task.type = r.type; task.answer = r.answer; task.steps = r.steps; store.addOrUpdateTask(task); showHistory() } else toast(r.error ?: L("Ошибка", "Error"))
        }.show()
    }

    private fun showMistakes() {
        val (scroll, box) = page(); box.addView(sectionTitle(R.drawable.ic_warning, L("Мои ошибки", "My mistakes"), L("Задания, которые стоит повторить", "Tasks worth reviewing"))); box.addView(space(14))
        val list = store.history().filter { it.checked && !it.correct }.sortedByDescending { it.createdAt }
        if (list.isEmpty()) box.addView(messageCard(L("Ошибок нет — отличный результат!", "No mistakes — great!"), green, R.drawable.ic_check)) else list.forEach { box.addView(taskCard(it)); box.addView(space(8)) }
        box.addView(space(20)); switchScreen("mistakes", scroll)
    }

    private fun showTrash() {
        val (scroll, box) = page(); box.addView(sectionTitle(R.drawable.ic_delete, L("Корзина", "Trash"), L("Удалённые решения хранятся 30 дней", "Deleted solutions are kept for 30 days"))); box.addView(space(14))
        val list = store.trash().sortedByDescending { it.deletedAt }
        if (list.isEmpty()) box.addView(messageCard(L("Корзина пуста", "Trash is empty"), muted(), R.drawable.ic_delete))
        list.forEach { task ->
            val days = max(0, 30 - ((System.currentTimeMillis() - task.deletedAt) / 86_400_000L).toInt())
            val c = card(); c.addView(text(task.input, 17f, true)); c.addView(text(L("Окончательное удаление через $days дн.", "Deleted permanently in $days days"), 12f, false, muted()))
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(actionButton(L("Восстановить", "Restore"), R.drawable.ic_history, false) { store.restore(task.id); showTrash() }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, dp(8), dp(4), 0) })
            row.addView(actionButton(L("Удалить", "Delete"), R.drawable.ic_delete, false, red) { store.deleteForever(task.id); showTrash() }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(4), dp(8), 0, 0) })
            c.addView(row); box.addView(c); box.addView(space(8))
        }
        box.addView(space(20)); switchScreen("trash", scroll)
    }

    private fun showProfiles() {
        val (scroll, box) = page(); box.addView(sectionTitle(R.drawable.ic_users, L("Пользователи", "Users"), L("Отдельная история и статистика для каждого", "Separate history and stats for each user"))); box.addView(space(14))
        store.profiles.forEach { p ->
            val c = card(); val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(avatar(p, 56), LinearLayout.LayoutParams(dp(56), dp(56)))
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(4), 0) }
            info.addView(text(p.name, 18f, true)); if (p.id == store.activeProfileId) info.addView(text(L("Активный профиль", "Active profile"), 12f, true, green))
            row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(iconButton(R.drawable.ic_edit, cardColor()) { profileActions(p) }, LinearLayout.LayoutParams(dp(44), dp(44)))
            c.addView(row)
            if (p.id != store.activeProfileId) { c.addView(space(8)); c.addView(actionButton(L("Выбрать", "Select"), R.drawable.ic_check, false) { store.activeProfileId = p.id; store.save(); showProfiles() }) }
            box.addView(c); box.addView(space(8))
        }
        box.addView(actionButton(L("Добавить пользователя", "Add user"), R.drawable.ic_users, true) { addProfile() }); box.addView(space(20)); switchScreen("profiles", scroll)
    }

    private fun showSettings() {
        val (scroll, box) = page(); box.addView(sectionTitle(R.drawable.ic_settings, L("Настройки", "Settings"), L("Оформление, безопасность и данные", "Appearance, security and data"))); box.addView(space(14))

        val theme = card(); theme.addView(text(L("Оформление", "Appearance"), 16f, true)); theme.addView(space(8))
        val themeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(6), 0, dp(6), 0) }
        themeRow.addView(icon(R.drawable.ic_sun, 24, if (!dark()) amber else muted()), LinearLayout.LayoutParams(dp(40), dp(40)))
        themeRow.addView(text(L("Светлый", "Light"), 13f, !dark(), if (!dark()) fg() else muted()), LinearLayout.LayoutParams(0, -2, 1f))
        val sw = Switch(this).apply { isChecked = dark() }
        themeRow.addView(sw, LinearLayout.LayoutParams(-2, -2))
        themeRow.addView(text(L("Тёмный", "Dark"), 13f, dark(), if (dark()) fg() else muted()), LinearLayout.LayoutParams(0, -2, 1f).apply { gravity = Gravity.END })
        themeRow.addView(icon(R.drawable.ic_moon, 24, if (dark()) accent else muted()), LinearLayout.LayoutParams(dp(40), dp(40)))
        sw.setOnCheckedChangeListener { _, checked -> store.settings.theme = if (checked) "dark" else "light"; store.save(); recreate() }
        theme.addView(themeRow); box.addView(theme); box.addView(space(8))

        box.addView(settingRow(R.drawable.ic_info, L("Язык", "Language"), if (store.settings.language == "ru") "Русский" else "English") { chooseLanguage() }); box.addView(space(8))
        box.addView(settingRow(R.drawable.ic_person, L("Пароль", "Password"), if (store.hasPassword()) L("Установлен", "Set") else L("Не установлен", "Not set")) { setPasswordDialog() }); box.addView(space(8))
        box.addView(settingRow(R.drawable.ic_history, L("Уведомления", "Notifications"), L("Тихие напоминания", "Silent reminders")) { notificationSettings() }); box.addView(space(8))
        box.addView(settingRow(R.drawable.ic_share, L("Резервная копия", "Backup"), L("Сохранить или восстановить", "Save or restore")) { backupMenu() }); box.addView(space(8))
        box.addView(settingRow(R.drawable.ic_edit, L("Ошибка или пожелание", "Feedback"), L("Написать разработчику", "Send feedback")) { feedback() }); box.addView(space(8))
        box.addView(settingRow(R.drawable.ic_delete, L("Сброс данных", "Reset data"), L("Только после ввода пароля", "Password required"), red) { resetData() })
        box.addView(space(20)); switchScreen("settings", scroll)
    }

    private fun settingRow(iconRes: Int, title: String, subtitle: String, color: Int = fg(), action: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(12), dp(10), dp(12)); background = round(cardColor(), 16, border()); setOnClickListener { action() }
        addView(icon(iconRes, 23, if (color == red) red else accent), LinearLayout.LayoutParams(dp(40), dp(40)))
        val col = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, 0, 0) }
        col.addView(text(title, 16f, true, color)); col.addView(text(subtitle, 12f, false, muted()))
        addView(col, LinearLayout.LayoutParams(0, -2, 1f)); addView(text("›", 28f, false, muted()))
    }

    private fun profileActions(profile: UserProfile) {
        D().setTitle(profile.name).setItems(arrayOf(L("Изменить имя", "Edit name"), L("Изменить аватар", "Change avatar"))) { _, which -> if (which == 0) editProfileName(profile) else pickAvatar(profile) }.show()
    }

    private fun showProfileChooser() {
        val names = store.profiles.map { if (it.id == store.activeProfileId) "✓  ${it.name}" else it.name }.toMutableList(); names += L("＋ Добавить пользователя", "+ Add user")
        D().setTitle(L("Сменить пользователя", "Switch user")).setItems(names.toTypedArray()) { _, which ->
            if (which == store.profiles.size) addProfile() else { store.activeProfileId = store.profiles[which].id; store.save(); recreate() }
        }.show()
    }

    private fun addProfile() {
        val e = EditText(this).apply { hint = L("Имя", "Name"); setTextColor(fg()); setHintTextColor(muted()) }
        D().setTitle(L("Новый пользователь", "New user")).setView(e).setNegativeButton(L("Отмена", "Cancel"), null).setPositiveButton(L("Добавить", "Add")) { _, _ ->
            if (e.text.isNotBlank()) { val p = UserProfile(name = e.text.toString().trim()); store.profiles += p; store.activeProfileId = p.id; store.save(); recreate() }
        }.show()
    }

    private fun editProfileName(profile: UserProfile) {
        val e = EditText(this).apply { setText(profile.name); selectAll(); setTextColor(fg()); setHintTextColor(muted()) }
        D().setTitle(L("Изменить имя", "Edit name")).setView(e).setNegativeButton(L("Отмена", "Cancel"), null).setPositiveButton(L("Сохранить", "Save")) { _, _ ->
            if (e.text.isNotBlank()) { profile.name = e.text.toString().trim(); store.save(); recreate() }
        }.show()
    }

    private fun pickAvatar(profile: UserProfile) {
        avatarProfileId = profile.id
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }, REQ_AVATAR)
    }

    private fun chooseLanguage() {
        D().setTitle(L("Язык", "Language")).setItems(arrayOf("Русский", "English")) { _, which -> store.settings.language = if (which == 0) "ru" else "en"; store.save(); recreate() }.show()
    }

    private fun setPasswordDialog() {
        val e = EditText(this).apply { hint = L("Новый пароль", "New password"); inputType = 0x00000081; setTextColor(fg()); setHintTextColor(muted()) }
        D().setTitle(L("Установить / сменить пароль", "Set / change password")).setView(e).setNegativeButton(L("Отмена", "Cancel"), null).setPositiveButton(L("Сохранить", "Save")) { _, _ ->
            if (e.text.length >= 4) { store.setPassword(e.text.toString()); toast(L("Пароль сохранён", "Password saved")) } else toast(L("Минимум 4 символа", "At least 4 characters"))
        }.show()
    }

    private fun showUnlock() {
        val e = EditText(this).apply { hint = L("Пароль", "Password"); inputType = 0x00000081; setTextColor(fg()); setHintTextColor(muted()) }
        val d = D().setTitle(L("Введите пароль", "Enter password")).setView(e).setCancelable(false).setPositiveButton(L("Открыть", "Unlock"), null).create()
        d.setOnShowListener { d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { if (store.checkPassword(e.text.toString())) d.dismiss() else e.error = L("Неверный пароль", "Wrong password") } }; d.show()
    }

    private fun notificationSettings() {
        val labels = arrayOf(L("Незавершённые решения — без звука", "Unfinished solutions — silent"), L("Напоминать после нескольких дней", "Remind after inactivity"))
        val checked = booleanArrayOf(store.settings.unfinishedNotifications, store.settings.inactivityNotifications)
        D().setTitle(L("Уведомления", "Notifications")).setMultiChoiceItems(labels, checked) { _, which, value -> checked[which] = value }.setPositiveButton(L("Сохранить", "Save")) { _, _ ->
            store.settings.notificationsEnabled = checked.any { it }; store.settings.unfinishedNotifications = checked[0]; store.settings.inactivityNotifications = checked[1]; store.save(); NotificationScheduler.scheduleUnfinished(this, store); NotificationScheduler.scheduleInactive(this, store)
        }.setNeutralButton(L("Через сколько дней", "Inactivity days")) { _, _ -> chooseInactivityDays() }.show()
    }

    private fun chooseInactivityDays() {
        val values = intArrayOf(2, 3, 5, 7); D().setTitle(L("Напомнить после", "Remind after")).setItems(values.map { L("$it дня", "$it days") }.toTypedArray()) { _, which -> store.settings.inactivityDays = values[which]; store.save(); NotificationScheduler.scheduleInactive(this, store) }.show()
    }

    private fun backupMenu() {
        D().setTitle(L("Резервная копия", "Backup")).setItems(arrayOf(L("Создать копию", "Create backup"), L("Восстановить", "Restore"))) { _, which ->
            if (which == 0) startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "application/json"; putExtra(Intent.EXTRA_TITLE, "MathProgress-backup.json") }, REQ_BACKUP_SAVE)
            else startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "application/json"; addCategory(Intent.CATEGORY_OPENABLE) }, REQ_BACKUP_OPEN)
        }.show()
    }

    private fun feedback() {
        val e = EditText(this).apply { hint = L("Опишите ошибку или предложение", "Describe an issue or suggestion"); minLines = 5; gravity = Gravity.TOP; setTextColor(fg()); setHintTextColor(muted()) }
        D().setTitle(L("Написать разработчику", "Send feedback")).setView(e).setNegativeButton(L("Отмена", "Cancel"), null).setPositiveButton(L("Выбрать приложение", "Choose app")) { _, _ -> if (e.text.isNotBlank()) shareText(L("Отзыв о MathProgress", "MathProgress feedback"), e.text.toString()) }.show()
    }

    private fun resetData() {
        if (!store.hasPassword()) { toast(L("Сначала установите пароль", "Set a password first")); return }
        val e = EditText(this).apply { hint = L("Пароль", "Password"); inputType = 0x00000081; setTextColor(fg()); setHintTextColor(muted()) }
        D().setTitle(L("Подтвердите сброс", "Confirm reset")).setView(e).setNegativeButton(L("Отмена", "Cancel"), null).setPositiveButton(L("Сбросить", "Reset")) { _, _ -> if (store.checkPassword(e.text.toString())) { store.resetAll(); recreate() } else toast(L("Неверный пароль", "Wrong password")) }.show()
    }

    private fun showGuide() {
        D().setTitle(L("Инструкция", "Guide")).setMessage(L(
            "Главная страница показывает результаты за неделю, месяц, год и всё время. Меню открывается кнопкой в левом верхнем углу. Активный пользователь находится вверху боковой панели — там его можно сменить или отредактировать. В разделе решения используйте специальную математическую клавиатуру. Обычные выражения тоже можно считать: например, (25)/(5)= даст ответ 5. Каждое решение сохраняется в историю. Удалённые задачи остаются в корзине 30 дней. В проверке знаний приложение создаёт случайные задания и ставит оценку.",
            "The home page shows results by week, month, year and all time. The active user is at the top of the side menu. Use the math keyboard to solve equations or calculate expressions. History, trash, practice and backups are available from the menu."
        )).setPositiveButton("OK", null).show()
    }

    private fun showExamples() {
        D().setTitle(L("Примеры ввода", "Examples")).setMessage("(25)/(5)=\n\n3x+7=22\n\nx^2-5x+6=0\n\nx^3-6x^2+11x-6=0\n\n2x+y=5; x-y=1\n\n2x+y-z=1; x-y+2z=3; 3x+y+z=7\n\n(x+1)/(x-2)=3\n\nsqrt(2x+3)=5\n\n2^(3x-1)=16\n\nlog2(x+1)=3").setPositiveButton("OK", null).show()
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
        D().setTitle(L("Подсказка", "Hint")).setMessage(hint).setPositiveButton("OK", null).show()
    }

    private fun chooseHistoryDate() {
        val c = historyDate ?: Calendar.getInstance(); DatePickerDialog(this, { _, y, m, d -> historyDate = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }; showHistory() }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun sameDay(time: Long, day: Calendar): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = time }; return c.get(Calendar.YEAR) == day.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
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
                    store.profiles.firstOrNull { it.id == avatarProfileId }?.avatarUri = uri.toString(); store.save(); recreate()
                }
                REQ_BACKUP_SAVE -> { contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(store.exportJson()) }; toast(L("Копия сохранена", "Backup saved")) }
                REQ_BACKUP_OPEN -> { val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return; store.importJson(text); recreate() }
            }
        } catch (e: Exception) { toast(e.message ?: L("Ошибка", "Error")) }
    }

    private fun solutionCard(r: SolveResult): View = card().apply {
        val head = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(icon(R.drawable.ic_check, 24, green), LinearLayout.LayoutParams(dp(36), dp(36))); head.addView(text(r.type, 14f, true, accent), LinearLayout.LayoutParams(0, -2, 1f)); addView(head)
        addView(text(r.input, 20f, true).apply { setPadding(0, dp(6), 0, dp(10)) }); addView(text(L("Полное решение", "Detailed solution"), 16f, true))
        r.steps.forEachIndexed { i, step -> addView(text("${i + 1}. $step", 14f).apply { setPadding(dp(2), dp(4), 0, dp(4)) }) }
        val ans = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10)); background = round(Color.argb(if (dark()) 45 else 22, 31, 171, 96), 12) }
        ans.addView(icon(R.drawable.ic_check, 22, green), LinearLayout.LayoutParams(dp(34), dp(34))); ans.addView(text(L("Ответ: ", "Answer: ") + r.answer, 17f, true, green), LinearLayout.LayoutParams(0, -2, 1f))
        addView(space(8)); addView(ans)
    }

    private fun sectionTitle(iconRes: Int, title: String, subtitle: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(iconBubble(iconRes, accent), LinearLayout.LayoutParams(dp(48), dp(48)))
        val col = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
        col.addView(text(title, 27f, true)); col.addView(text(subtitle, 13f, false, muted()))
        addView(col, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun metricCard(iconRes: Int, title: String, value: String, color: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(7), dp(12), dp(7), dp(12)); background = round(cardColor(), 15, border())
        addView(icon(iconRes, 22, color), LinearLayout.LayoutParams(dp(30), dp(30))); addView(text(value, 22f, true).apply { gravity = Gravity.CENTER }); addView(text(title, 11f, false, muted()).apply { gravity = Gravity.CENTER })
    }

    private fun infoLine(iconRes: Int, title: String, value: String, valueColor: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)); addView(icon(iconRes, 22, valueColor), LinearLayout.LayoutParams(dp(36), dp(36)))
        val col = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }; col.addView(text(title, 12f, false, muted())); col.addView(text(value, 15f, true, valueColor)); addView(col, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun card(fill: Int = cardColor()): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14)); background = round(fill, 17, border()) }
    private fun divider(): View = View(this).apply { setBackgroundColor(border()); layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply { setMargins(dp(36), dp(4), 0, dp(4)) } }
    private fun label(value: String) = text(value, 12f, true, muted()).apply { setPadding(dp(4), 0, 0, dp(3)) }

    private fun actionButton(title: String, iconRes: Int, primary: Boolean, textColor: Int = if (primary) Color.WHITE else fg(), action: () -> Unit): Button = Button(this).apply {
        text = title; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; isAllCaps = false; gravity = Gravity.CENTER; minHeight = 0; minWidth = 0
        setTextColor(textColor); background = round(if (primary) accent else cardColor(), 14, if (primary) null else border()); stateListAnimator = null
        setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0); compoundDrawablePadding = dp(8); compoundDrawableTintList = ColorStateList.valueOf(textColor); setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(50))
    }

    private fun iconOnlyButton(iconRes: Int, tint: Int = fg(), action: () -> Unit): ImageButton = iconButton(iconRes, cardColor(), tint, action)

    private fun segment(title: String, selected: Boolean, action: () -> Unit): Button = Button(this).apply {
        text = title; textSize = 13f; typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT; isAllCaps = false; setTextColor(if (selected) Color.WHITE else muted()); background = round(if (selected) accent else Color.TRANSPARENT, 11); stateListAnimator = null; minHeight = 0; minWidth = 0; setOnClickListener { action() }
    }

    private fun messageCard(message: String, color: Int, iconRes: Int): View = card().apply { val r = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }; r.addView(icon(iconRes, 24, color), LinearLayout.LayoutParams(dp(38), dp(38))); r.addView(text(message, 15f, true, color), LinearLayout.LayoutParams(0, -2, 1f)); addView(r) }

    private fun iconButton(iconRes: Int, fill: Int = surface2(), tint: Int = fg(), action: () -> Unit): ImageButton = ImageButton(this).apply { setImageResource(iconRes); imageTintList = ColorStateList.valueOf(tint); background = round(fill, 13, if (fill == Color.TRANSPARENT) null else border()); setPadding(dp(10), dp(10), dp(10), dp(10)); scaleType = ImageView.ScaleType.CENTER_INSIDE; setOnClickListener { action() } }

    private fun iconBubble(iconRes: Int, color: Int): View = FrameLayout(this).apply { background = round(softAccent(), 14); addView(icon(iconRes, 25, color), FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER)) }

    private fun icon(iconRes: Int, size: Int, tint: Int): ImageView = ImageView(this).apply { setImageResource(iconRes); imageTintList = ColorStateList.valueOf(tint); scaleType = ImageView.ScaleType.CENTER_INSIDE; setPadding(dp(3), dp(3), dp(3), dp(3)); layoutParams = ViewGroup.LayoutParams(dp(size), dp(size)) }

    private fun avatar(profile: UserProfile, size: Int): ImageView = ImageView(this).apply {
        background = round(softAccent(), size / 2); clipToOutline = true; scaleType = ImageView.ScaleType.CENTER_CROP
        val uri = profile.avatarUri.takeIf { it.isNotBlank() }
        if (uri != null) try { setImageURI(Uri.parse(uri)); setPadding(0, 0, 0, 0) } catch (_: Exception) { setImageResource(R.drawable.ic_person); imageTintList = ColorStateList.valueOf(accent); setPadding(dp(10), dp(10), dp(10), dp(10)) }
        else { setImageResource(R.drawable.ic_person); imageTintList = ColorStateList.valueOf(accent); setPadding(dp(10), dp(10), dp(10), dp(10)) }
    }

    private fun styledSpinner(items: List<String>): Spinner {
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = (super.getView(position, convertView, parent) as TextView).apply { setTextColor(fg()); textSize = 15f; setPadding(dp(8), 0, dp(8), 0) }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = (super.getDropDownView(position, convertView, parent) as TextView).apply { setTextColor(fg()); setBackgroundColor(cardColor()); textSize = 15f; setPadding(dp(14), dp(12), dp(14), dp(12)) }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return Spinner(this).apply { this.adapter = adapter }
    }

    private fun page(): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(bg()) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(20), dp(18), dp(20)) }
        scroll.addView(box, ScrollView.LayoutParams(-1, -2)); return scroll to box
    }

    private fun text(value: String, size: Float = 15f, bold: Boolean = false, color: Int = fg()): TextView = TextView(this).apply { text = value; textSize = size; setTextColor(color); typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT; setLineSpacing(0f, 1.06f) }
    private fun space(height: Int): View = Space(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }

    private fun round(fill: Int, radius: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply { cornerRadius = dp(radius).toFloat(); setColor(fill); if (stroke != null) setStroke(dp(1), stroke) }

    private fun D(): AlertDialog.Builder = AlertDialog.Builder(this, if (dark()) android.R.style.Theme_Material_Dialog_Alert else android.R.style.Theme_Material_Light_Dialog_Alert)
    private fun softAccent() = Color.argb(if (dark()) 50 else 22, 99, 91, 255)
    private fun surface2() = if (dark()) Color.rgb(38, 39, 46) else Color.rgb(244, 245, 249)
    private fun dark(): Boolean = when (store.settings.theme) { "dark" -> true; "light" -> false; else -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES }
    private fun bg() = if (dark()) Color.rgb(17, 18, 22) else Color.rgb(247, 248, 252)
    private fun cardColor() = if (dark()) Color.rgb(30, 31, 37) else Color.WHITE
    private fun fg() = if (dark()) Color.rgb(245, 245, 247) else Color.rgb(28, 29, 34)
    private fun muted() = if (dark()) Color.rgb(166, 168, 178) else Color.rgb(103, 106, 119)
    private fun border() = if (dark()) Color.rgb(51, 53, 62) else Color.rgb(226, 228, 235)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun L(ru: String, en: String) = if (store.settings.language == "en") en else ru
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 900)
    }
}

class SimpleTextWatcher(private val callback: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { callback(s?.toString().orEmpty()) }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
