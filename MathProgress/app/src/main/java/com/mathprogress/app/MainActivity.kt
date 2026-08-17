package com.mathprogress.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
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
    private lateinit var navButton: ImageButton
    private var current = "results"
    private var backAction: (() -> Unit)? = null
    private var lockOverlay: FrameLayout? = null
    private var firstResume = true
    private var deviceAuthPurpose = ""
    private var pendingSecurityAction: (() -> Unit)? = null
    private var avatarProfileId: String? = null

    private var period = StatsPeriod.WEEK
    private val historyFilter = HistoryFilterState()

    private var practiceTopic = "Линейные"
    private var practiceTotal = 5
    private var practiceIndex = 0
    private var practiceCorrect = 0
    private var practiceQuestion: PracticeQuestion? = null
    private var practiceAnswered = false
    private var practiceLastCorrect = false
    private var practiceStartedAt = 0L

    private val calendarCursor: Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private var dailyDateKey = ""
    private var dailyQuestions: List<PracticeQuestion> = emptyList()
    private var dailyIndex = 0
    private var dailyCorrect = 0
    private var dailyAttempt = 1
    private var dailyAnswered = false
    private var dailyLastCorrect = false

    private val accent = Color.rgb(99, 91, 255)
    private val green = Color.rgb(37, 174, 99)
    private val red = Color.rgb(215, 63, 70)
    private val amber = Color.rgb(242, 158, 45)

    companion object {
        const val REQ_AVATAR = 101
        const val REQ_BACKUP_SAVE = 102
        const val REQ_BACKUP_OPEN = 103
        const val REQ_DEVICE_AUTH = 104
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LocalStore(this)
        NotificationScheduler.ensureChannels(this)
        requestNotificationPermission()
        buildShell()
        showResults()
        NotificationScheduler.scheduleAll(this, store)
        store.setLastOpenNow()
        if (store.securityEnabled()) lockNow()
    }

    override fun onResume() {
        super.onResume()
        if (!::store.isInitialized) return
        store.setLastOpenNow()
        NotificationScheduler.scheduleInactive(this, store)
        if (firstResume) { firstResume = false; return }
        if (store.securityEnabled() && lockOverlay == null) {
            val elapsed = System.currentTimeMillis() - store.backgroundAt()
            val timeout = store.settings.autoLockSeconds * 1000L
            if (store.backgroundAt() > 0 && (timeout == 0L || elapsed >= timeout)) lockNow()
        }
    }

    override fun onPause() {
        if (::store.isInitialized) {
            store.setBackgroundAt(System.currentTimeMillis())
            NotificationScheduler.scheduleUnfinished(this, store)
        }
        super.onPause()
    }

    private fun buildShell() {
        applySystemBars()
        root = FrameLayout(this).apply { setBackgroundColor(bg()) }
        val vertical = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg()) }
        root.setOnApplyWindowInsetsListener { _, insets ->
            @Suppress("DEPRECATION")
            vertical.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            insets
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(5), dp(14), dp(5)); setBackgroundColor(cardColor()); elevation = dp(2).toFloat()
        }
        navButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_menu); imageTintList = ColorStateList.valueOf(fg()); background = round(Color.TRANSPARENT, 12)
            contentDescription = "Меню"; setPadding(dp(11), dp(11), dp(11), dp(11)); setOnClickListener { openDrawer() }
        }
        top.addView(navButton, LinearLayout.LayoutParams(dp(46), dp(46)))
        top.addView(text("Математика — Прогресс", 19f, true), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(10),0,0,0) })
        vertical.addView(top, LinearLayout.LayoutParams(-1, dp(60)))

        content = FrameLayout(this)
        vertical.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        vertical.addView(Space(this), LinearLayout.LayoutParams(-1, dp(50)))
        root.addView(vertical, FrameLayout.LayoutParams(-1,-1))

        drawer = FrameLayout(this).apply {
            visibility = View.GONE; isClickable = true; setOnClickListener { closeDrawer() }
        }
        drawerList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(16), dp(14), dp(14)); setBackgroundColor(cardColor()); elevation = dp(20).toFloat()
            isClickable = true; setOnClickListener { }
        }
        drawer.addView(drawerList, FrameLayout.LayoutParams(dp(310), -1, Gravity.START))
        root.addView(drawer, FrameLayout.LayoutParams(-1,-1))
        setContentView(root)
        root.requestApplyInsets()
        rebuildDrawer()
    }

    private fun applySystemBars() {
        window.statusBarColor = bg()
        window.navigationBarColor = bg()
        if (Build.VERSION.SDK_INT >= 30) {
            val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (dark()) 0 else mask, mask)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (dark()) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun rebuildDrawer() {
        drawerList.removeAllViews()
        val p = store.activeProfile()
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(8), dp(6), dp(12)); background = round(surface2(), 16)
            setOnClickListener { closeDrawer(); showProfile() }
        }
        header.addView(avatarView(p, 54), LinearLayout.LayoutParams(dp(54),dp(54)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12),0,0,0) }
        info.addView(text(p.name,18f,true)); info.addView(text("Профиль и смена пользователя",12f,false,muted()))
        header.addView(info, LinearLayout.LayoutParams(0,-2,1f))
        header.addView(text("›",28f,true,muted()))
        drawerList.addView(header, LinearLayout.LayoutParams(-1, dp(78)))
        drawerList.addView(space(12))

        val items = listOf(
            DrawerItem("results", R.drawable.ic_home, "Результаты") { showResults() },
            DrawerItem("solve", R.drawable.ic_calculate, "Решить задачу") { showSolve() },
            DrawerItem("practice", R.drawable.ic_check, "Проверка знаний") { showPracticeSetup() },
            DrawerItem("daily", R.drawable.ic_daily, "Ежедневная тренировка") { showDaily() },
            DrawerItem("history", R.drawable.ic_history, "История") { showHistory() },
            DrawerItem("mistakes", R.drawable.ic_info, "Мои ошибки") { showMistakes() },
            DrawerItem("trash", R.drawable.ic_delete, "Корзина") { showTrash() },
            DrawerItem("settings", R.drawable.ic_settings, "Настройки") { showSettings() },
            DrawerItem("guide", R.drawable.ic_book, "Инструкция") { showGuide() }
        )
        items.forEach { drawerList.addView(drawerItemView(it), LinearLayout.LayoutParams(-1, dp(47))) }
        drawerList.addView(Space(this), LinearLayout.LayoutParams(1,0,1f))
        drawerList.addView(divider())
        drawerList.addView(drawerItemView(DrawerItem("about",R.drawable.ic_about,"О приложении"){showAbout()}), LinearLayout.LayoutParams(-1,dp(47)))
        drawerList.addView(drawerItemView(DrawerItem("exit",R.drawable.ic_exit,"Выход"){finishAndRemoveTask()}, danger=true), LinearLayout.LayoutParams(-1,dp(47)))
    }

    private data class DrawerItem(val key:String,val icon:Int,val title:String,val action:()->Unit)
    private fun drawerItemView(item:DrawerItem,danger:Boolean=false):View = LinearLayout(this).apply {
        orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),0,dp(8),0)
        background=round(if(current==item.key)Color.argb(if(dark())45 else 25,99,91,255) else Color.TRANSPARENT,12)
        val iv=ImageView(this@MainActivity).apply{setImageResource(item.icon);imageTintList=ColorStateList.valueOf(if(danger)red else if(current==item.key)accent else muted());setPadding(dp(2),dp(2),dp(2),dp(2))}
        addView(iv,LinearLayout.LayoutParams(dp(24),dp(24)))
        addView(text(item.title,14.5f,current==item.key,if(danger)red else if(current==item.key)accent else fg()),LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(dp(14),0,0,0)})
        setOnClickListener{closeDrawer();item.action()}
    }

    private fun openDrawer(){ rebuildDrawer(); drawer.setBackgroundColor(Color.argb(120,0,0,0)); drawer.visibility=View.VISIBLE }
    private fun closeDrawer(){drawer.visibility=View.GONE}

    private fun setScreen(key:String,view:View,back:(()->Unit)?=null){
        current=key;backAction=back;content.removeAllViews();content.addView(view,FrameLayout.LayoutParams(-1,-1));
        navButton.setImageResource(if(back==null)R.drawable.ic_menu else R.drawable.ic_back)
        navButton.contentDescription=if(back==null)"Меню" else "Назад"
        navButton.setOnClickListener{if(back==null)openDrawer() else back()}
        rebuildDrawer()
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed(){
        when { drawer.visibility==View.VISIBLE->closeDrawer(); lockOverlay!=null->{}; backAction!=null->backAction?.invoke(); current!="results"->showResults(); else->super.onBackPressed() }
    }

    // ---------- RESULTS ----------
    private fun showResults(){
        val (scroll,box)=page()
        box.addView(screenTitle("Результаты","Ваш прогресс и успеваемость"))
        val tabs=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        listOf(StatsPeriod.WEEK to "Неделя",StatsPeriod.MONTH to "Месяц",StatsPeriod.YEAR to "Год",StatsPeriod.ALL to "Всё время").forEach{(p,t)->
            tabs.addView(segment(t,period==p){period=p;showResults()},LinearLayout.LayoutParams(0,dp(42),1f).apply{setMargins(dp(2),0,dp(2),0)})
        }
        box.addView(tabs);box.addView(space(12))
        val s=StatsEngine.snapshot(store.history(),period)
        val hero=card()
        hero.addView(text(s.message,20f,true))
        if(s.comparison.isNotBlank())hero.addView(text(s.comparison,14f,true,green).apply{setPadding(0,dp(7),0,0)})
        hero.addView(space(14))
        val metrics=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        metrics.addView(metric("Средняя",s.averageGrade?.let{String.format(Locale.US,"%.1f",it)}?:"—",R.drawable.ic_check),LinearLayout.LayoutParams(0,-2,1f))
        metrics.addView(metric("Точность",s.accuracy?.let{"$it%"}?:"—",R.drawable.ic_trophy),LinearLayout.LayoutParams(0,-2,1f))
        metrics.addView(metric("Решено",s.solved.toString(),R.drawable.ic_calculate),LinearLayout.LayoutParams(0,-2,1f))
        hero.addView(metrics);box.addView(hero);box.addView(space(10))
        val topics=card();topics.addView(iconLine(R.drawable.ic_check,"Сильная тема: ${s.bestTopic?:"—"}",if(s.bestTopic==null)muted() else green));topics.addView(space(7));topics.addView(iconLine(R.drawable.ic_info,"Стоит повторить: ${s.weakTopic?:"—"}",if(s.weakTopic==null)muted() else amber));box.addView(topics)
        box.addView(space(10))
        val today=dateKey(System.currentTimeMillis());val daily=store.daily(today)
        val dc=card();dc.addView(iconLine(R.drawable.ic_daily,"Ежедневная тренировка",accent,true));dc.addView(space(6))
        dc.addView(text(when{daily?.completed==true->"Сегодня выполнено ✓";(daily?.attempts?:0)>0->"Сегодня можно исправить результат";else->"5 коротких заданий на сегодня"},14f,false,muted()))
        dc.addView(space(9));dc.addView(if(daily?.completed==true)outline("Открыть календарь",R.drawable.ic_daily){showDaily()} else primary("Начать ежедневные задания",R.drawable.ic_daily){startDaily(today)})
        box.addView(dc)
        val draft=store.getDraft();if(draft.isNotBlank()){box.addView(space(10));val c=card();c.addView(iconLine(R.drawable.ic_calculate,"Незавершённая задача",accent,true));c.addView(text(draft.take(110),14f,false,muted()).apply{setPadding(0,dp(5),0,dp(8))});c.addView(outline("Продолжить",R.drawable.ic_calculate){showSolve(draft)});box.addView(c)}
        box.addView(space(12));box.addView(primary("Решить задачу",R.drawable.ic_calculate){showSolve()});box.addView(space(8));box.addView(outline("Проверить знания",R.drawable.ic_check){showPracticeSetup()});box.addView(space(24))
        setScreen("results",scroll)
    }

    // ---------- SOLVER ----------
    private fun showSolve(prefill:String=""){
        val (scroll,box)=page();box.addView(screenTitle("Решить задачу","Полное пошаговое решение"))
        val types=arrayOf("Авто","Линейное","Квадратное","Кубическое","Система","Дроби","Корни","Показательное","Логарифмическое")
        val spinner=styledSpinner(types.toList());box.addView(labeled("Тип задачи",spinner));box.addView(space(10))
        val input=EditText(this).apply{textSize=20f;setTextColor(fg());setHintTextColor(muted());hint="Например: x² − 5x + 6 = 0";gravity=Gravity.TOP;minLines=4;setPadding(dp(14),dp(14),dp(14),dp(14));background=round(cardColor(),16,border());showSoftInputOnFocus=false;setText(if(prefill.isNotBlank())prefill else store.getDraft())}
        input.addTextChangedListener(SimpleTextWatcher{store.setDraft(it)})
        box.addView(input,LinearLayout.LayoutParams(-1,dp(142)));box.addView(space(8));box.addView(MathKeyboardView(this,input,dark()));box.addView(space(8));box.addView(outline("Примеры ввода",R.drawable.ic_info){showInputExamplesPage()});box.addView(space(10))
        val resultBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        box.addView(primary("Решить подробно",R.drawable.ic_calculate){
            val forced=when(spinner.selectedItemPosition){1->"linear";2->"quadratic";3->"cubic";4->"system";5->"fraction";6->"root";7->"exponential";8->"log";else->"auto"}
            val result=MathEngine.solve(input.text.toString(),forced);resultBox.removeAllViews()
            if(!result.success)resultBox.addView(messageCard(result.error?:"Не удалось решить",red)) else{
                resultBox.addView(solutionCard(result));resultBox.addView(space(8))
                val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};row.addView(outline("Поделиться",R.drawable.ic_about){shareResult(result)},LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(0,0,dp(4),0)});row.addView(outline("Решить самому",R.drawable.ic_check){showSelfCheck(result)},LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(dp(4),0,0,0)});resultBox.addView(row)
                store.addOrUpdateTask(TaskRecord(profileId=store.activeProfileId,input=result.input,type=result.type,answer=result.answer,steps=result.steps));store.clearDraft();NotificationScheduler.scheduleUnfinished(this,store)
            }
        });box.addView(space(10));box.addView(resultBox);box.addView(space(24));setScreen("solve",scroll)
    }

    private fun showSelfCheck(result:SolveResult){
        val (scroll,box)=page();box.addView(screenTitle("Решить самостоятельно","Введите свой ответ — приложение проверит"));val c=card();c.addView(text(result.input,23f,true));c.addView(text(result.type,13f,false,accent));box.addView(c);box.addView(space(10))
        val answer=EditText(this).apply{hint="Ваш ответ";textSize=19f;setTextColor(fg());setHintTextColor(muted());showSoftInputOnFocus=false;setPadding(dp(12),dp(10),dp(12),dp(10));background=round(cardColor(),14,border())};box.addView(answer,LinearLayout.LayoutParams(-1,dp(58)));box.addView(space(8));box.addView(MathKeyboardView(this,answer,dark()));box.addView(space(10))
        val feedback=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};box.addView(primary("Проверить",R.drawable.ic_check){val ok=if(result.numericAnswers.isNotEmpty())PracticeEngine.check(answer.text.toString(),result.numericAnswers) else answer.text.toString().trim()==result.answer.trim();store.addOrUpdateTask(TaskRecord(profileId=store.activeProfileId,input=result.input,type=result.type,answer=result.answer,steps=result.steps,selfSolved=true,checked=true,correct=ok,grade=if(ok)5 else 2));feedback.removeAllViews();feedback.addView(messageCard(if(ok)"Верно! Отличная работа." else "Есть ошибка. Правильный ответ: ${result.answer}",if(ok)green else amber))});box.addView(space(10));box.addView(feedback);setScreen("solve",scroll){showSolve(result.input)}
    }

    private fun showInputExamplesPage(){val (s,b)=page();b.addView(screenTitle("Примеры ввода","Как правильно записывать разные задачи"));listOf("Вычисление" to "(25)/(5)=","Линейное" to "3x+7=22","Квадратное" to "x^2-5x+6=0","Кубическое" to "x^3-6x^2+11x-6=0","Система 2×2" to "2x+y=5; x-y=1","Система 3×3" to "2x+y-z=1; x-y+2z=3; 3x+y+z=7","Дроби" to "(x+1)/(x-2)=3","Корень" to "sqrt(2x+3)=5","Показательное" to "2^(3x-1)=16","Логарифм" to "log2(x+1)=3").forEach{(t,e)->val c=card();c.addView(text(t,14f,true,accent));c.addView(text(e,18f,true));b.addView(c);b.addView(space(7))};setScreen("solve",s){showSolve()}}

    // ---------- PRACTICE ----------
    private fun showPracticeSetup(){
        val (scroll,box)=page();box.addView(screenTitle("Проверка знаний","Отдельная тренировка с итоговой оценкой"));val c=card();c.addView(iconLine(R.drawable.ic_check,"Выберите тему и количество заданий",accent,true));c.addView(text("Каждый ответ сохраняется. После теста вы увидите оценку, точность и сможете разобрать ошибки.",14f,false,muted()).apply{setPadding(0,dp(7),0,0)});box.addView(c);box.addView(space(10))
        val topic=styledSpinner(PracticeEngine.topics());box.addView(labeled("Тема",topic));box.addView(space(8));val counts=listOf("5 заданий","10 заданий","15 заданий");val count=styledSpinner(counts);box.addView(labeled("Количество",count));box.addView(space(12));box.addView(primary("Начать проверку",R.drawable.ic_check){practiceTopic=topic.selectedItem.toString();practiceTotal=listOf(5,10,15)[count.selectedItemPosition];practiceIndex=0;practiceCorrect=0;practiceStartedAt=System.currentTimeMillis();nextPracticeQuestion()});setScreen("practice",scroll)
    }

    private fun nextPracticeQuestion(){if(practiceIndex>=practiceTotal){showPracticeResult();return};practiceQuestion=PracticeEngine.generate(practiceTopic);practiceAnswered=false;showPracticeQuestion()}
    private fun showPracticeQuestion(){
        val q=practiceQuestion?:return;val (scroll,box)=page();box.addView(screenTitle("Проверка знаний","Задание ${practiceIndex+1} из $practiceTotal"));box.addView(progressStrip(practiceIndex+1,practiceTotal));box.addView(space(10));val qc=card();qc.addView(text(q.type,13f,true,accent));qc.addView(text(q.input,24f,true).apply{setPadding(0,dp(8),0,0)});box.addView(qc);box.addView(space(10))
        if(!practiceAnswered){val input=EditText(this).apply{hint="Ваш ответ";textSize=19f;setTextColor(fg());setHintTextColor(muted());showSoftInputOnFocus=false;setPadding(dp(12),dp(10),dp(12),dp(10));background=round(cardColor(),14,border())};box.addView(input,LinearLayout.LayoutParams(-1,dp(58)));box.addView(space(8));box.addView(MathKeyboardView(this,input,dark()));box.addView(space(8));box.addView(outline("Подсказка",R.drawable.ic_info){showInlineHint(box,q)});box.addView(space(8));box.addView(primary("Проверить ответ",R.drawable.ic_check){val ok=PracticeEngine.check(input.text.toString(),q.expected);practiceLastCorrect=ok;practiceCorrect+=if(ok)1 else 0;practiceAnswered=true;val solution=MathEngine.solve(q.input);store.addOrUpdateTask(TaskRecord(profileId=store.activeProfileId,input=q.input,type=q.type,answer=q.answerText,steps=solution.steps,selfSolved=true,checked=true,correct=ok,grade=if(ok)5 else 2,source="practice"));showPracticeQuestion()})}
        else{box.addView(messageCard(if(practiceLastCorrect)"Верно! Так держать." else "Ошибка. Правильный ответ: ${q.answerText}",if(practiceLastCorrect)green else amber));box.addView(space(10));box.addView(primary(if(practiceIndex+1==practiceTotal)"Показать результат" else "Следующее задание",R.drawable.ic_check){practiceIndex++;nextPracticeQuestion()})}
        setScreen("practice",scroll){showPracticeSetup()}
    }

    private fun showPracticeResult(){val grade=PracticeEngine.grade(practiceCorrect,practiceTotal);val percent=if(practiceTotal==0)0 else practiceCorrect*100/practiceTotal;val mins=max(1,((System.currentTimeMillis()-practiceStartedAt)/60000L).toInt());val (s,b)=page();b.addView(screenTitle("Результат проверки","Тест завершён"));val hero=card();hero.gravity=Gravity.CENTER;hero.addView(text(motivation(percent),22f,true,if(percent>=75)green else fg()).apply{gravity=Gravity.CENTER});hero.addView(text("$percent%",48f,true,accent).apply{gravity=Gravity.CENTER;setPadding(0,dp(8),0,0)});hero.addView(text("Оценка: $grade",18f,true).apply{gravity=Gravity.CENTER});b.addView(hero);b.addView(space(10));val c=card();c.addView(text("Правильно: $practiceCorrect из $practiceTotal",15f,true));c.addView(text("Время: $mins мин.",14f,false,muted()));b.addView(c);b.addView(space(10));b.addView(primary("Разобрать ошибки",R.drawable.ic_info){showMistakes()});b.addView(space(8));b.addView(outline("Вернуться к результатам",R.drawable.ic_home){showResults()});setScreen("practice",s){showPracticeSetup()}}

    private fun showInlineHint(box:LinearLayout,q:PracticeQuestion){val hint=when{q.type.contains("Квадрат")->"Начните с дискриминанта D = b² − 4ac.";q.type.contains("Система")->"Попробуйте сложение или подстановку.";q.type.contains("дроб",true)->"Сначала запишите ОДЗ — знаменатель не равен нулю.";q.type.contains("корн",true)->"Проверьте ОДЗ и возведите обе части в квадрат.";q.type.contains("Показ",true)->"Попробуйте привести степени к одному основанию.";q.type.contains("Лог",true)->"Начните с ОДЗ аргумента логарифма.";else->"Перенесите неизвестные в одну часть, числа — в другую."};box.addView(messageCard(hint,accent),2)}

    // ---------- DAILY ----------
    private fun showDaily(){
        val (scroll,box)=page();box.addView(screenTitle("Ежедневная тренировка","Закрывайте дни и собирайте полный месяц"));val monthLocale=if(store.settings.language=="en")Locale.ENGLISH else Locale("ru");val monthTitle=SimpleDateFormat("LLLL yyyy",monthLocale).format(calendarCursor.time).replaceFirstChar{if(it.isLowerCase())it.titlecase(monthLocale) else it.toString()}
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL};nav.addView(iconOnly(R.drawable.ic_back){calendarCursor.add(Calendar.MONTH,-1);showDaily()},LinearLayout.LayoutParams(dp(44),dp(44)));nav.addView(text(monthTitle,20f,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,-2,1f));nav.addView(textButton("›"){calendarCursor.add(Calendar.MONTH,1);showDaily()},LinearLayout.LayoutParams(dp(44),dp(44)));box.addView(nav);box.addView(space(8))
        val prefix=SimpleDateFormat("yyyy-MM",Locale.US).format(calendarCursor.time);val progress=store.dailyForProfile().filter{it.dateKey.startsWith(prefix)};val status=mutableMapOf<Int,Int>();progress.forEach{val d=it.dateKey.takeLast(2).toIntOrNull()?:return@forEach;status[d]=if(it.completed)2 else if(it.attempts>0)1 else 0}
        val monthDays=calendarCursor.getActualMaximum(Calendar.DAY_OF_MONTH);val now=Calendar.getInstance();val selIndex=calendarCursor.get(Calendar.YEAR)*12+calendarCursor.get(Calendar.MONTH);val nowIndex=now.get(Calendar.YEAR)*12+now.get(Calendar.MONTH);val enabled=when{selIndex<nowIndex->monthDays;selIndex==nowIndex->now.get(Calendar.DAY_OF_MONTH);else->0};val complete=(1..monthDays).all{status[it]==2}
        if(complete){val win=card();win.addView(iconLine(R.drawable.ic_trophy,"Месяц пройден полностью!",amber,true));win.addView(text("Все ежедневные тренировки закрыты. Этот месяц отмечен кубком.",14f,false,muted()));box.addView(win);box.addView(space(8))}
        val journey=DailyJourneyView(this,calendarCursor.get(Calendar.YEAR),calendarCursor.get(Calendar.MONTH),status,enabled,dark()){day->val c=Calendar.getInstance().apply{set(calendarCursor.get(Calendar.YEAR),calendarCursor.get(Calendar.MONTH),day,12,0,0);set(Calendar.MILLISECOND,0)};startDaily(dateKey(c.timeInMillis))};box.addView(journey,LinearLayout.LayoutParams(-1,-2));box.addView(space(8));val legend=card();legend.addView(text("● Фиолетовый — можно пройти   ● Оранжевый — нужно исправить   ✓ Зелёный — день закрыт",12.5f,false,muted()));legend.addView(text("Пропущенный день можно закрыть позже. При повторной попытке задания будут другими.",12.5f,false,muted()).apply{setPadding(0,dp(7),0,0)});box.addView(legend);box.addView(space(24));setScreen("daily",scroll)
    }

    private fun startDaily(date:String){val p=store.daily(date)?:DailyProgress(store.activeProfileId,date);dailyDateKey=date;dailyAttempt=p.attempts+1;dailyQuestions=PracticeEngine.dailyQuestions(date,store.activeProfileId,dailyAttempt,5);dailyIndex=0;dailyCorrect=0;dailyAnswered=false;showDailyQuestion()}
    private fun showDailyQuestion(){
        if(dailyIndex>=dailyQuestions.size){finishDailyAttempt();return};val q=dailyQuestions[dailyIndex];val (s,b)=page();b.addView(screenTitle("Ежедневная тренировка",formatDateKey(dailyDateKey)));b.addView(progressStrip(dailyIndex+1,dailyQuestions.size));b.addView(text("Попытка $dailyAttempt",12f,true,accent).apply{setPadding(0,dp(7),0,dp(7))});val qc=card();qc.addView(text(q.type,13f,true,accent));qc.addView(text(q.input,24f,true).apply{setPadding(0,dp(7),0,0)});b.addView(qc);b.addView(space(10))
        if(!dailyAnswered){val input=EditText(this).apply{hint="Ваш ответ";textSize=19f;setTextColor(fg());setHintTextColor(muted());showSoftInputOnFocus=false;setPadding(dp(12),dp(10),dp(12),dp(10));background=round(cardColor(),14,border())};b.addView(input,LinearLayout.LayoutParams(-1,dp(58)));b.addView(space(8));b.addView(MathKeyboardView(this,input,dark()));b.addView(space(8));b.addView(primary("Проверить",R.drawable.ic_check){val ok=PracticeEngine.check(input.text.toString(),q.expected);dailyLastCorrect=ok;dailyCorrect+=if(ok)1 else 0;dailyAnswered=true;val sol=MathEngine.solve(q.input);store.addOrUpdateTask(TaskRecord(profileId=store.activeProfileId,input=q.input,type=q.type,answer=q.answerText,steps=sol.steps,selfSolved=true,checked=true,correct=ok,grade=if(ok)5 else 2,source="daily:$dailyDateKey"));showDailyQuestion()})} else {b.addView(messageCard(if(dailyLastCorrect)"Верно ✓" else "Нужно повторить. Ответ: ${q.answerText}",if(dailyLastCorrect)green else amber));b.addView(space(8));b.addView(primary(if(dailyIndex+1==dailyQuestions.size)"Завершить" else "Дальше",R.drawable.ic_check){dailyIndex++;dailyAnswered=false;showDailyQuestion()})}
        setScreen("daily",s){showDaily()}
    }

    private fun finishDailyAttempt(){val p=store.daily(dailyDateKey)?:DailyProgress(store.activeProfileId,dailyDateKey);p.attempts=max(p.attempts,dailyAttempt);p.bestCorrect=max(p.bestCorrect,dailyCorrect);if(dailyCorrect==dailyQuestions.size){p.completed=true;p.completedAt=System.currentTimeMillis()};store.saveDaily(p);showDailyResult(p)}
    private fun showDailyResult(p:DailyProgress){val (s,b)=page();b.addView(screenTitle("Итог дня",formatDateKey(dailyDateKey)));val hero=card();hero.gravity=Gravity.CENTER;if(p.completed){hero.addView(text("✓",54f,true,green).apply{gravity=Gravity.CENTER});hero.addView(text("День закрыт!",24f,true,green).apply{gravity=Gravity.CENTER});hero.addView(text("Все задания решены правильно.",14f,false,muted()).apply{gravity=Gravity.CENTER})}else{hero.addView(text("$dailyCorrect / ${dailyQuestions.size}",42f,true,amber).apply{gravity=Gravity.CENTER});hero.addView(text("Можно исправиться",22f,true).apply{gravity=Gravity.CENTER});hero.addView(text("Мы дадим другие уравнения. Галочка появится, когда весь набор будет решён правильно.",14f,false,muted()).apply{gravity=Gravity.CENTER})};b.addView(hero);b.addView(space(10));if(!p.completed)b.addView(primary("Исправить результат — новые задания",R.drawable.ic_daily){startDaily(dailyDateKey)});b.addView(space(8));b.addView(outline("Вернуться к календарю",R.drawable.ic_daily){showDaily()});setScreen("daily",s){showDaily()}}

    // ---------- HISTORY ----------
    private fun showHistory(){val (s,b)=page();b.addView(screenTitle("История","По умолчанию показаны все сохранённые задачи"));val filtered=historyFilter.apply(store.history());val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};top.addView(outline("Фильтры и сортировка",R.drawable.ic_filter){showHistoryFilters()},LinearLayout.LayoutParams(0,dp(48),1f));b.addView(top);if(!historyFilter.isDefault()){b.addView(space(7));b.addView(text(filterSummary(),12.5f,true,accent));b.addView(space(5));b.addView(textButton("Сбросить все фильтры"){resetHistoryFilter();showHistory()})};b.addView(space(10));b.addView(text("Найдено: ${filtered.size}",13f,true,muted()));b.addView(space(7));if(filtered.isEmpty())b.addView(messageCard("По выбранным условиям задач нет.",muted())) else filtered.forEach{b.addView(taskCard(it));b.addView(space(8))};b.addView(space(24));setScreen("history",s)}

    private fun showHistoryFilters(){
        val (s,b)=page();b.addView(screenTitle("Фильтры истории","Можно комбинировать несколько условий одновременно"));
        val search=EditText(this).apply{hint="Поиск по условию, ответу или теме";setText(historyFilter.search);textSize=16f;setTextColor(fg());setHintTextColor(muted());setPadding(dp(12),0,dp(12),0);background=round(cardColor(),13,border())};b.addView(search,LinearLayout.LayoutParams(-1,dp(52)));b.addView(space(9))
        val periodNames=listOf("Всё время","Сегодня","Вчера","Эта неделя","Этот месяц","Этот год","Конкретная дата","Диапазон дат");val periodKeys=listOf("all","today","yesterday","week","month","year","specific","custom");val ps=styledSpinner(periodNames);ps.setSelection(periodKeys.indexOf(historyFilter.period).coerceAtLeast(0));b.addView(labeled("Период",ps));b.addView(space(8))
        val dateRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};val fromBtn=outline(dateLabel(historyFilter.startAt,"Начало"),R.drawable.ic_daily){pickDate(false){time->historyFilter.startAt=startOfDay(time);if(historyFilter.period=="specific")historyFilter.endAt=endOfDay(time);showHistoryFilters()}};val toBtn=outline(dateLabel(if(historyFilter.endAt==Long.MAX_VALUE)0 else historyFilter.endAt,"Конец"),R.drawable.ic_daily){pickDate(true){time->historyFilter.endAt=endOfDay(time);showHistoryFilters()}};dateRow.addView(fromBtn,LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(0,0,dp(4),0)});dateRow.addView(toBtn,LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(dp(4),0,0,0)});b.addView(dateRow);b.addView(text("Для «Конкретной даты» достаточно выбрать начало. Для диапазона — начало и конец.",11.5f,false,muted()).apply{setPadding(0,dp(5),0,dp(8))})
        val sourceNames=listOf("Все источники","Обычные решения","Проверка знаний","Ежедневные задания");val sourceKeys=listOf("all","solver","practice","daily");val ss=styledSpinner(sourceNames);ss.setSelection(sourceKeys.indexOf(historyFilter.source).coerceAtLeast(0));b.addView(labeled("Источник",ss));b.addView(space(8))
        val resultNames=listOf("Любой результат","Правильно","С ошибкой","Без проверки");val resultKeys=listOf("all","correct","wrong","unchecked");val rs=styledSpinner(resultNames);rs.setSelection(resultKeys.indexOf(historyFilter.result).coerceAtLeast(0));b.addView(labeled("Результат",rs));b.addView(space(8))
        val modeNames=listOf("Любой режим","Решал самостоятельно","Решило приложение");val modeKeys=listOf("all","self","auto");val ms=styledSpinner(modeNames);ms.setSelection(modeKeys.indexOf(historyFilter.mode).coerceAtLeast(0));b.addView(labeled("Режим",ms));b.addView(space(8))
        val types=listOf("Все типы")+store.history().map{it.type}.filter{it.isNotBlank()}.distinct().sorted();val ts=styledSpinner(types);ts.setSelection(if(historyFilter.type=="all")0 else types.indexOf(historyFilter.type).coerceAtLeast(0));b.addView(labeled("Тип задачи",ts));b.addView(space(8))
        val sortNames=listOf("Сначала новые","Сначала старые","Оценка: высокая → низкая","Оценка: низкая → высокая","По типу задачи","По источнику");val sortKeys=listOf("newest","oldest","gradeHigh","gradeLow","type","source");val os=styledSpinner(sortNames);os.setSelection(sortKeys.indexOf(historyFilter.sort).coerceAtLeast(0));b.addView(labeled("Сортировка",os));b.addView(space(12))
        b.addView(primary("Применить",R.drawable.ic_filter){historyFilter.search=search.text.toString();historyFilter.period=periodKeys[ps.selectedItemPosition];historyFilter.source=sourceKeys[ss.selectedItemPosition];historyFilter.result=resultKeys[rs.selectedItemPosition];historyFilter.mode=modeKeys[ms.selectedItemPosition];historyFilter.type=if(ts.selectedItemPosition==0)"all" else ts.selectedItem.toString();historyFilter.sort=sortKeys[os.selectedItemPosition];if(historyFilter.period=="specific"&&historyFilter.startAt>0)historyFilter.endAt=endOfDay(historyFilter.startAt);showHistory()});b.addView(space(8));b.addView(outline("Сбросить всё",R.drawable.ic_delete){resetHistoryFilter();showHistoryFilters()});b.addView(space(24));setScreen("history",s){showHistory()}
    }

    private fun resetHistoryFilter(){historyFilter.period="all";historyFilter.source="all";historyFilter.result="all";historyFilter.mode="all";historyFilter.type="all";historyFilter.sort="newest";historyFilter.search="";historyFilter.startAt=0;historyFilter.endAt=Long.MAX_VALUE}
    private fun filterSummary():String{val parts=mutableListOf<String>();if(historyFilter.search.isNotBlank())parts+="поиск: «${historyFilter.search}»";if(historyFilter.period!="all")parts+="период";if(historyFilter.source!="all")parts+="источник";if(historyFilter.result!="all")parts+="результат";if(historyFilter.mode!="all")parts+="режим";if(historyFilter.type!="all")parts+=historyFilter.type;if(historyFilter.sort!="newest")parts+="особая сортировка";return "Активно: "+parts.joinToString(" • ")}

    private fun taskCard(t:TaskRecord):View=card().apply{setOnClickListener{showTask(t)};val date=SimpleDateFormat("dd.MM.yyyy  HH:mm",Locale.getDefault()).format(Date(t.createdAt));addView(text(t.type,13f,true,accent));addView(text(date,11.5f,false,muted()));addView(text(t.input,18f,true).apply{setPadding(0,dp(6),0,dp(4))});if(t.answer.isNotBlank())addView(text(t.answer,14f,false,green));if(t.checked)addView(text(if(t.correct)"✓ Верно" else "! Ошибка",12f,true,if(t.correct)green else amber))}
    private fun showTask(t:TaskRecord){val (s,b)=page();b.addView(screenTitle(t.type,"Подробности сохранённой задачи"));val c=card();c.addView(text(t.input,22f,true));c.addView(space(10));c.addView(text("Полное решение",17f,true));t.steps.forEachIndexed{i,st->c.addView(text("${i+1}. $st",14f).apply{setPadding(0,dp(3),0,dp(3))})};c.addView(text("Ответ: ${t.answer}",17f,true,green).apply{setPadding(0,dp(10),0,0)});b.addView(c);b.addView(space(10));b.addView(primary("Поделиться",R.drawable.ic_about){shareTask(t)});b.addView(space(8));b.addView(outline("Редактировать и пересчитать",R.drawable.ic_edit){showEditTask(t)});b.addView(space(8));b.addView(dangerButton("Удалить в корзину",R.drawable.ic_delete){store.deleteToTrash(t.id);showHistory()});setScreen("history",s){showHistory()}}
    private fun showEditTask(t:TaskRecord){val (s,b)=page();b.addView(screenTitle("Редактирование","Измените условие и пересчитайте решение"));val e=EditText(this).apply{setText(t.input);textSize=19f;setTextColor(fg());gravity=Gravity.TOP;showSoftInputOnFocus=false;setPadding(dp(12),dp(12),dp(12),dp(12));background=round(cardColor(),14,border())};b.addView(e,LinearLayout.LayoutParams(-1,dp(130)));b.addView(space(8));b.addView(MathKeyboardView(this,e,dark()));b.addView(space(10));val msg=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};b.addView(primary("Пересчитать и сохранить",R.drawable.ic_check){val r=MathEngine.solve(e.text.toString());if(!r.success){msg.removeAllViews();msg.addView(messageCard(r.error?:"Ошибка",red))}else{t.input=r.input;t.type=r.type;t.answer=r.answer;t.steps=r.steps;store.addOrUpdateTask(t);showTask(t)}});b.addView(space(8));b.addView(msg);setScreen("history",s){showTask(t)}}

    // ---------- MISTAKES / TRASH ----------
    private fun showMistakes(){val (s,b)=page();b.addView(screenTitle("Мои ошибки","Задачи, где ответ был неверным"));val list=store.history().filter{it.checked&&!it.correct}.sortedByDescending{it.createdAt};if(list.isEmpty())b.addView(messageCard("Ошибок нет — отличный результат!",green))else list.forEach{b.addView(taskCard(it));b.addView(space(8))};setScreen("mistakes",s)}
    private fun showTrash(){val (s,b)=page();b.addView(screenTitle("Корзина","Удалённые решения хранятся 30 дней"));val list=store.trash().sortedByDescending{it.deletedAt};if(list.isEmpty())b.addView(messageCard("Корзина пуста",muted()));list.forEach{t->val days=max(0,30-((System.currentTimeMillis()-t.deletedAt)/86_400_000L).toInt());val c=card();c.addView(text(t.input,17f,true));c.addView(text("Окончательное удаление через $days дн.",12f,false,muted()));val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};row.addView(outline("Восстановить",R.drawable.ic_history){store.restore(t.id);showTrash()},LinearLayout.LayoutParams(0,dp(46),1f).apply{setMargins(0,dp(6),dp(4),0)});row.addView(dangerButton("Удалить",R.drawable.ic_delete){store.deleteForever(t.id);showTrash()},LinearLayout.LayoutParams(0,dp(46),1f).apply{setMargins(dp(4),dp(6),0,0)});c.addView(row);b.addView(c);b.addView(space(8))};setScreen("trash",s)}

    // ---------- PROFILE ----------
    private fun showProfile(){val p=store.activeProfile();val (s,b)=page();b.addView(screenTitle("Профиль","Управление текущим пользователем"));val hero=card();hero.gravity=Gravity.CENTER;hero.addView(avatarView(p,88),LinearLayout.LayoutParams(dp(88),dp(88)).apply{gravity=Gravity.CENTER});hero.addView(text(p.name,24f,true).apply{gravity=Gravity.CENTER;setPadding(0,dp(10),0,0)});val stats=StatsEngine.snapshot(store.history(),StatsPeriod.ALL);hero.addView(text("Решено: ${stats.solved}  •  Точность: ${stats.accuracy?.let{"$it%"}?:"—"}",13f,false,muted()).apply{gravity=Gravity.CENTER;setPadding(0,dp(5),0,0)});b.addView(hero);b.addView(space(10));b.addView(primary("Редактировать профиль",R.drawable.ic_edit){showEditProfile()});b.addView(space(8));b.addView(outline("Сменить пользователя",R.drawable.ic_user){showUserSwitcher()});setScreen("profile",s){showResults()}}
    private fun showEditProfile(){val p=store.activeProfile();val (s,b)=page();b.addView(screenTitle("Редактировать профиль","Имя и аватар"));val preview=avatarView(p,100);val wrap=LinearLayout(this).apply{gravity=Gravity.CENTER};wrap.addView(preview,LinearLayout.LayoutParams(dp(100),dp(100)));b.addView(wrap);b.addView(space(10));b.addView(outline("Сменить аватар",R.drawable.ic_camera){pickAvatar(p)});b.addView(space(10));val name=EditText(this).apply{setText(p.name);textSize=18f;setTextColor(fg());setPadding(dp(12),0,dp(12),0);background=round(cardColor(),13,border())};b.addView(labeled("Имя",name,dp(54)));b.addView(space(12));b.addView(primary("Сохранить",R.drawable.ic_check){if(name.text.isNotBlank()){p.name=name.text.toString().trim();store.save();showProfile()}});if(store.profiles.size>1){b.addView(space(16));b.addView(dangerButton("Удалить этот профиль",R.drawable.ic_delete){store.profiles.removeAll{it.id==p.id};store.activeProfileId=store.profiles.first().id;store.save();showProfile()})};setScreen("profile",s){showProfile()}}
    private fun showUserSwitcher(){val (s,b)=page();b.addView(screenTitle("Сменить пользователя","История и результаты у каждого профиля свои"));store.profiles.forEach{p->val c=card();val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL};row.addView(avatarView(p,48),LinearLayout.LayoutParams(dp(48),dp(48)));row.addView(text(p.name,17f,true),LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(dp(12),0,0,0)});if(p.id==store.activeProfileId)row.addView(text("✓",22f,true,green));c.addView(row);c.setOnClickListener{store.activeProfileId=p.id;store.save();showProfile()};b.addView(c);b.addView(space(8))};b.addView(primary("Добавить пользователя",R.drawable.ic_user){showAddUser()});setScreen("profile",s){showProfile()}}
    private fun showAddUser(){val (s,b)=page();b.addView(screenTitle("Новый пользователь","Создайте отдельный профиль"));val name=EditText(this).apply{hint="Имя";textSize=18f;setTextColor(fg());setHintTextColor(muted());setPadding(dp(12),0,dp(12),0);background=round(cardColor(),13,border())};b.addView(name,LinearLayout.LayoutParams(-1,dp(55)));b.addView(space(12));b.addView(primary("Создать профиль",R.drawable.ic_check){if(name.text.isNotBlank()){val p=UserProfile(name=name.text.toString().trim());store.profiles+=p;store.activeProfileId=p.id;store.save();showEditProfile()}});setScreen("profile",s){showUserSwitcher()}}
    private fun pickAvatar(p:UserProfile){avatarProfileId=p.id;startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)},REQ_AVATAR)}

    // ---------- SETTINGS ----------
    private fun showSettings(){val (s,b)=page();b.addView(screenTitle("Настройки","Основные параметры приложения"));val themeCard=card();themeCard.addView(iconLine(if(dark())R.drawable.ic_moon else R.drawable.ic_sun,"Оформление",accent,true));val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(9),0,0)};row.addView(text("☀  Светлая",14f,true),LinearLayout.LayoutParams(0,-2,1f));val sw=Switch(this).apply{isChecked=dark();buttonTintList=null;thumbTintList=ColorStateList.valueOf(accent);trackTintList=ColorStateList.valueOf(Color.argb(80,99,91,255));setOnCheckedChangeListener{_,checked->store.settings.theme=if(checked)"dark" else "light";store.save();recreate()}};row.addView(sw);row.addView(text("Тёмная  ☾",14f,true),LinearLayout.LayoutParams(0,-2,1f));themeCard.addView(row);b.addView(themeCard);b.addView(space(8));b.addView(settingsCard(R.drawable.ic_book,"Язык",if(store.settings.language=="en")"English" else "Русский"){showLanguagePage()});b.addView(space(8));b.addView(settingsCard(R.drawable.ic_security,"Безопасность",securityDescription()){showSecurity()});b.addView(space(8));b.addView(settingsCard(R.drawable.ic_backup,"Резервная копия","Сохранение и восстановление данных"){showBackup()});b.addView(space(18));b.addView(dangerButton("Сбросить данные приложения",R.drawable.ic_delete){showResetPage()});setScreen("settings",s)}
    private fun showLanguagePage(){val (s,b)=page();b.addView(screenTitle("Язык","Выберите язык интерфейса"));b.addView(selectCard("Русский",store.settings.language=="ru"){store.settings.language="ru";store.save();recreate()});b.addView(space(8));b.addView(selectCard("English",store.settings.language=="en"){store.settings.language="en";store.save();recreate()});setScreen("settings",s){showSettings()}}

    // ---------- SECURITY ----------
    private fun showSecurity(){val (s,b)=page();b.addView(screenTitle("Безопасность","Защита входа в приложение"));val status=card();status.addView(iconLine(R.drawable.ic_security,"Текущая защита",accent,true));status.addView(text(securityDescription(),15f,true).apply{setPadding(0,dp(8),0,0)});status.addView(text("При смене способа защиты приложение обязательно запросит текущую защиту.",12.5f,false,muted()).apply{setPadding(0,dp(5),0,0)});b.addView(status);b.addView(space(10));b.addView(text("СПОСОБ ЗАЩИТЫ",12f,true,muted()));b.addView(space(6));b.addView(settingsCard(R.drawable.ic_security,"PIN-код","4 цифры"){changeSecurityTo("pin")});b.addView(space(7));b.addView(settingsCard(R.drawable.ic_user,"Защита телефона","Биометрия / PIN / пароль Android"){changeSecurityTo("device")});b.addView(space(7));b.addView(settingsCard(R.drawable.ic_check,"Графический ключ","Рисунок по сетке 3×3"){changeSecurityTo("pattern")});if(store.securityEnabled()){b.addView(space(10));b.addView(dangerButton("Отключить защиту",R.drawable.ic_security){confirmCurrentSecurity("Отключение защиты"){store.disableSecurity();showSecurity()}})};b.addView(space(18));b.addView(text("АВТОБЛОКИРОВКА",12f,true,muted()));b.addView(text("По умолчанию — через 1 минуту после выхода из приложения.",12.5f,false,muted()).apply{setPadding(0,dp(5),0,dp(8))});val times=listOf(0 to "Сразу",60 to "1 мин",180 to "3 мин",300 to "5 мин");val tr=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};times.forEach{(sec,label)->tr.addView(segment(label,store.settings.autoLockSeconds==sec){store.settings.autoLockSeconds=sec;store.save();showSecurity()},LinearLayout.LayoutParams(0,dp(42),1f).apply{setMargins(dp(2),0,dp(2),0)})};b.addView(tr);setScreen("security",s){showSettings()}}
    private fun securityDescription():String=when(store.settings.securityMethod){"pin"->"PIN-код включён";"device"->"Используется защита телефона";"pattern"->"Графический ключ включён";else->"Защита не включена"}
    private fun changeSecurityTo(method:String){if(store.securityEnabled())confirmCurrentSecurity("Подтвердите текущую защиту"){proceedSecurity(method)} else proceedSecurity(method)}
    private fun proceedSecurity(method:String){when(method){"pin"->showSetPinPage();"pattern"->showSetPatternPage();"device"->{val km=getSystemService(KEYGUARD_SERVICE) as KeyguardManager;if(!km.isDeviceSecure){showSecurityMessage("На телефоне сначала нужно настроить PIN, пароль, графический ключ или биометрию Android.");return};deviceAuthPurpose="enable";launchDeviceAuth()}}}
    private fun showSetPinPage(){val (s,b)=page();b.addView(screenTitle("Новый PIN-код","Введите 4 цифры два раза"));val p1=pinEdit("Новый PIN");val p2=pinEdit("Повторите PIN");b.addView(labeled("PIN-код",p1,dp(54)));b.addView(space(8));b.addView(labeled("Повтор",p2,dp(54)));b.addView(space(12));val msg=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};b.addView(primary("Сохранить PIN",R.drawable.ic_security){val a=p1.text.toString();val c=p2.text.toString();msg.removeAllViews();when{a.length!=4||!a.all{it.isDigit()}->msg.addView(messageCard("PIN должен состоять ровно из 4 цифр.",amber));a!=c->msg.addView(messageCard("PIN-коды не совпадают.",amber));else->{store.setPin(a);showSecurity()}}});b.addView(space(8));b.addView(msg);setScreen("security",s){showSecurity()}}
    private fun showSetPatternPage(){val (s,b)=page();b.addView(screenTitle("Новый графический ключ","Соедините минимум 4 точки"));val instruction=text("Нарисуйте новый ключ",16f,true,accent);b.addView(instruction);var first:String?=null;lateinit var view:PatternLockView;view=PatternLockView(this,dark()){pattern->if(first==null){first=pattern;instruction.text="Повторите тот же рисунок";view.postDelayed({view.reset()},350)}else if(first==pattern){store.setPattern(pattern);showSecurity()}else{instruction.text="Рисунки не совпали. Начните заново.";instruction.setTextColor(red);first=null;view.showError()}};b.addView(view,LinearLayout.LayoutParams(-1,-2));b.addView(messageCard("Не используйте слишком простой рисунок. Для смены ключа позже потребуется подтвердить текущий.",muted()));setScreen("security",s){showSecurity()}}
    private fun confirmCurrentSecurity(title:String,onSuccess:()->Unit){pendingSecurityAction=onSuccess;when(store.settings.securityMethod){"pin"->showPinConfirmPage(title);"pattern"->showPatternConfirmPage(title);"device"->{deviceAuthPurpose="confirm";launchDeviceAuth()};else->{pendingSecurityAction=null;onSuccess()}}}
    private fun showPinConfirmPage(title:String){val (s,b)=page();b.addView(screenTitle(title,"Введите текущий PIN-код"));val pin=pinEdit("Текущий PIN");b.addView(pin,LinearLayout.LayoutParams(-1,dp(56)));b.addView(space(12));val msg=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};b.addView(primary("Подтвердить",R.drawable.ic_security){if(store.checkPin(pin.text.toString())){val a=pendingSecurityAction;pendingSecurityAction=null;a?.invoke()}else{msg.removeAllViews();msg.addView(messageCard("Неверный PIN-код.",red))}});b.addView(space(8));b.addView(msg);setScreen("security",s){pendingSecurityAction=null;showSecurity()}}
    private fun showPatternConfirmPage(title:String){val (s,b)=page();b.addView(screenTitle(title,"Нарисуйте текущий графический ключ"));lateinit var v:PatternLockView;v=PatternLockView(this,dark()){pattern->if(store.checkPattern(pattern)){val a=pendingSecurityAction;pendingSecurityAction=null;a?.invoke()}else v.showError()};b.addView(v,LinearLayout.LayoutParams(-1,-2));setScreen("security",s){pendingSecurityAction=null;showSecurity()}}
    private fun showSecurityMessage(message:String){val (s,b)=page();b.addView(screenTitle("Безопасность","Требуется действие"));b.addView(messageCard(message,amber));b.addView(space(10));b.addView(primary("Вернуться",R.drawable.ic_back){showSecurity()});setScreen("security",s){showSecurity()}}

    private fun launchDeviceAuth(){
        if(Build.VERSION.SDK_INT>=30){
            val prompt=android.hardware.biometrics.BiometricPrompt.Builder(this).setTitle("Подтвердите личность").setSubtitle("Используйте защиту телефона").setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL).build()
            prompt.authenticate(CancellationSignal(),mainExecutor,object:android.hardware.biometrics.BiometricPrompt.AuthenticationCallback(){override fun onAuthenticationSucceeded(result:android.hardware.biometrics.BiometricPrompt.AuthenticationResult?){super.onAuthenticationSucceeded(result);onDeviceAuthSuccess()}})
        } else {
            val km=getSystemService(KEYGUARD_SERVICE) as KeyguardManager;val intent=km.createConfirmDeviceCredentialIntent("Подтвердите личность","Используйте защиту телефона")
            if(intent!=null)startActivityForResult(intent,REQ_DEVICE_AUTH) else showSecurityMessage("На устройстве не настроена системная защита.")
        }
    }
    private fun onDeviceAuthSuccess(){when(deviceAuthPurpose){"enable"->{store.settings.securityMethod="device";store.settings.pinHash="";store.settings.patternHash="";store.save();showSecurity()};"confirm"->{val a=pendingSecurityAction;pendingSecurityAction=null;a?.invoke()};"unlock"->unlockOverlay()};deviceAuthPurpose=""}

    // ---------- BACKUP ----------
    private fun showBackup(){val (s,b)=page();b.addView(screenTitle("Резервная копия","Полное сохранение учебных данных"));val last=store.lastBackupAt();val info=card();info.addView(iconLine(R.drawable.ic_backup,"Что сохраняется",accent,true));listOf("Профили и аватары (ссылки на локальные изображения)","Вся история решений и корзина","Результаты проверок знаний","Прогресс ежедневных заданий и календаря","Незавершённые задачи","Язык и оформление").forEach{info.addView(text("✓  $it",13.5f,false,fg()).apply{setPadding(0,dp(4),0,0)})};info.addView(text("PIN, графический ключ и системная защита не экспортируются — это сделано ради безопасности.",12f,false,muted()).apply{setPadding(0,dp(9),0,0)});b.addView(info);b.addView(space(10));b.addView(messageCard(if(last>0)"Последняя копия: ${SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(Date(last))}" else "Резервная копия ещё не создавалась.",muted()));b.addView(space(10));b.addView(primary("Создать резервную копию",R.drawable.ic_backup){startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type="application/json";putExtra(Intent.EXTRA_TITLE,"MathProgress-backup-${SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())}.json")},REQ_BACKUP_SAVE)});b.addView(space(8));b.addView(outline("Восстановить из копии",R.drawable.ic_history){showRestorePage()});setScreen("backup",s){showSettings()}}
    private fun showRestorePage(){val (s,b)=page();b.addView(screenTitle("Восстановление","Проверьте предупреждение перед продолжением"));val warn=card();warn.addView(text("Важно",18f,true,amber));warn.addView(text("Профили, история, результаты и ежедневный прогресс будут заменены данными из выбранной копии. Текущая защита приложения останется без изменений.",14f,false,fg()).apply{setPadding(0,dp(7),0,0)});b.addView(warn);b.addView(space(12));b.addView(primary("Выбрать файл резервной копии",R.drawable.ic_backup){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/json";addCategory(Intent.CATEGORY_OPENABLE)},REQ_BACKUP_OPEN)});setScreen("backup",s){showBackup()}}

    // ---------- GUIDE / ABOUT ----------
    private fun showGuide(){val (s,b)=page();b.addView(screenTitle("Инструкция","Подробное руководство по приложению"));guideSection(b,R.drawable.ic_home,"1. Результаты","Главная страница показывает среднюю оценку, точность, количество решений, сильные и слабые темы. Переключайте неделю, месяц, год или всё время. Если показатели растут, приложение отдельно покажет прогресс.");guideSection(b,R.drawable.ic_calculate,"2. Решение задач","Откройте «Решить задачу», выберите тип или оставьте «Авто», введите условие математической клавиатурой и нажмите «Решить подробно». Решение автоматически сохранится в историю.");guideSection(b,R.drawable.ic_check,"3. Решить самостоятельно","После получения решения можно перейти в режим самостоятельной проверки. Введите свой ответ — приложение сравнит результат и сохранит успех или ошибку.");guideSection(b,R.drawable.ic_check,"4. Проверка знаний","Выберите тему и число заданий. Все задания проходят на отдельных страницах. В конце появятся оценка, процент правильных ответов и переход к разбору ошибок.");guideSection(b,R.drawable.ic_daily,"5. Ежедневная тренировка","После обеда приложение напоминает о ежедневном наборе. День закрывается зелёной галочкой только после полностью правильной попытки. Если были ошибки — нажмите «Исправить», и приложение даст другой набор. Месяц, закрытый полностью, отмечается кубком.");guideSection(b,R.drawable.ic_history,"6. История","По умолчанию отображается вся история. В фильтрах можно выбрать период, конкретную дату или диапазон, источник, результат, режим, тип задачи, поиск и порядок сортировки. Запись можно открыть, отредактировать, пересчитать, поделиться или удалить.");guideSection(b,R.drawable.ic_delete,"7. Корзина","Удалённые решения хранятся 30 дней. Их можно восстановить или удалить окончательно раньше срока.");guideSection(b,R.drawable.ic_user,"8. Профили","Текущий пользователь находится вверху боковой панели. Нажмите на него, чтобы изменить имя или аватар, создать новый профиль либо переключиться на другого пользователя.");guideSection(b,R.drawable.ic_security,"9. Безопасность","В настройках можно выбрать PIN, графический ключ или системную защиту телефона. При смене защиты требуется текущий способ. Автоблокировка доступна сразу, через 1, 3 или 5 минут.");guideSection(b,R.drawable.ic_backup,"10. Резервная копия","Создайте файл резервной копии и сохраните его в удобное место. Для восстановления выберите этот файл. Защитные коды намеренно не переносятся между устройствами.");guideSection(b,R.drawable.ic_info,"11. Подсказки и связь","Значок информации открывает подсказки там, где они нужны. Ошибки и предложения отправляются через раздел «О приложении» с выбором установленного приложения для отправки.");b.addView(space(20));setScreen("guide",s)}
    private fun guideSection(box:LinearLayout,icon:Int,title:String,body:String){val c=card();c.addView(iconLine(icon,title,accent,true));c.addView(text(body,14f,false,fg()).apply{setPadding(0,dp(8),0,0);setLineSpacing(dp(2).toFloat(),1.08f)});box.addView(c);box.addView(space(8))}
    private fun showAbout(){val (s,b)=page();b.addView(screenTitle("О приложении","Математика — Прогресс"));val c=card();c.addView(text("Математика — Прогресс",22f,true));c.addView(text("Версия 0.3.0",13f,false,muted()).apply{setPadding(0,dp(4),0,dp(8))});c.addView(text("Решение уравнений, обучение, проверка знаний, ежедневные тренировки и личная статистика в одном приложении.",14f,false,fg()));b.addView(c);b.addView(space(10));b.addView(settingsCard(R.drawable.ic_info,"Сообщить об ошибке","Опишите проблему и выберите приложение для отправки"){showFeedbackPage(true)});b.addView(space(8));b.addView(settingsCard(R.drawable.ic_edit,"Предложить улучшение","Расскажите, чего не хватает приложению"){showFeedbackPage(false)});b.addView(space(8));b.addView(settingsCard(R.drawable.ic_trophy,"Оценить приложение","Открыть страницу приложения"){rateApp()});b.addView(space(8));b.addView(settingsCard(R.drawable.ic_book,"Инструкция","Открыть подробное руководство"){showGuide()});setScreen("about",s)}
    private fun showFeedbackPage(error:Boolean){val (s,b)=page();val title=if(error)"Сообщить об ошибке" else "Предложить улучшение";b.addView(screenTitle(title,"Сообщение отправится через выбранное вами приложение"));val e=EditText(this).apply{hint=if(error)"Что произошло? Что вы делали перед ошибкой?" else "Что стоит добавить или изменить?";minLines=7;gravity=Gravity.TOP;textSize=16f;setTextColor(fg());setHintTextColor(muted());setPadding(dp(12),dp(12),dp(12),dp(12));background=round(cardColor(),14,border())};b.addView(e,LinearLayout.LayoutParams(-1,dp(190)));b.addView(space(12));b.addView(primary("Выбрать приложение и отправить",R.drawable.ic_about){if(e.text.isNotBlank())shareText(title,e.text.toString())});setScreen("about",s){showAbout()}}

    // ---------- RESET ----------
    private fun showResetPage(){val (s,b)=page();b.addView(screenTitle("Сброс данных","Необратимое действие"));b.addView(messageCard("Будут удалены профили, история, статистика, ежедневный прогресс, корзина и настройки. Резервная копия на устройстве не удаляется.",red));b.addView(space(10));if(!store.securityEnabled())b.addView(messageCard("Для безопасного сброса сначала включите PIN, графический ключ или защиту телефона.",amber))else b.addView(dangerButton("Подтвердить защиту и сбросить",R.drawable.ic_delete){confirmCurrentSecurity("Подтверждение сброса"){store.resetAll();recreate()}});setScreen("settings",s){showSettings()}}

    // ---------- APP LOCK ----------
    private fun lockNow(){if(lockOverlay!=null||!store.securityEnabled())return;val overlay=FrameLayout(this).apply{setBackgroundColor(bg());elevation=dp(50).toFloat()};val sc=android.widget.ScrollView(this);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(28),dp(56),dp(28),dp(30))};box.addView(iconImage(R.drawable.ic_security,70,accent));box.addView(text("Приложение заблокировано",24f,true).apply{gravity=Gravity.CENTER;setPadding(0,dp(16),0,dp(5))});box.addView(text(when(store.settings.securityMethod){"pin"->"Введите PIN-код";"pattern"->"Нарисуйте графический ключ";else->"Подтвердите личность защитой телефона"},14f,false,muted()).apply{gravity=Gravity.CENTER});box.addView(space(22));when(store.settings.securityMethod){"pin"->buildPinUnlock(box);"pattern"->{lateinit var v:PatternLockView;v=PatternLockView(this,dark()){p->if(store.checkPattern(p))unlockOverlay()else v.showError()};box.addView(v,LinearLayout.LayoutParams(-1,-2))};"device"->{box.addView(primary("Разблокировать",R.drawable.ic_security){deviceAuthPurpose="unlock";launchDeviceAuth()})}};box.addView(space(18));box.addView(textButton("Выйти из приложения"){finishAndRemoveTask()});sc.addView(box);overlay.addView(sc,FrameLayout.LayoutParams(-1,-1));root.addView(overlay,FrameLayout.LayoutParams(-1,-1));lockOverlay=overlay}
    private fun buildPinUnlock(box:LinearLayout){val dots=text("○  ○  ○  ○",30f,true,accent).apply{gravity=Gravity.CENTER};val error=text("",13f,true,red).apply{gravity=Gravity.CENTER};box.addView(dots);box.addView(error);box.addView(space(14));var value="";fun update(){dots.text=(0 until 4).joinToString("  "){if(it<value.length)"●" else "○"};if(value.length==4){if(store.checkPin(value))unlockOverlay()else{error.text="Неверный PIN";value="";dots.postDelayed({update()},250)}}};val nums=listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"),listOf("","0","⌫"));nums.forEach{r->val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER};r.forEach{n->if(n.isBlank())row.addView(Space(this),LinearLayout.LayoutParams(dp(72),dp(62)).apply{setMargins(dp(4),dp(4),dp(4),dp(4))})else row.addView(textButton(n){if(n=="⌫")value=value.dropLast(1)else if(value.length<4)value+=n;error.text="";update()},LinearLayout.LayoutParams(dp(72),dp(62)).apply{setMargins(dp(4),dp(4),dp(4),dp(4))})};box.addView(row)}}
    private fun unlockOverlay(){lockOverlay?.let{root.removeView(it)};lockOverlay=null;store.setBackgroundAt(System.currentTimeMillis())}

    // ---------- ACTIVITY RESULTS ----------
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==REQ_DEVICE_AUTH){if(resultCode==RESULT_OK)onDeviceAuthSuccess();return};if(resultCode!=RESULT_OK||data?.data==null)return;val uri=data.data!!;try{when(requestCode){REQ_AVATAR->{try{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){};store.profiles.firstOrNull{it.id==avatarProfileId}?.avatarUri=uri.toString();store.save();showEditProfile()};REQ_BACKUP_SAVE->{contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(store.exportJson())};store.setLastBackupAt(System.currentTimeMillis());showBackup()};REQ_BACKUP_OPEN->{val text=contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:return;store.importJson(text);recreate()}}}catch(e:Exception){toast(e.message?:"Ошибка")}}

    // ---------- SHARE ----------
    private fun shareResult(r:SolveResult)=shareText(r.type,buildString{appendLine(r.type);appendLine(r.input);appendLine();r.steps.forEachIndexed{i,st->appendLine("${i+1}. $st")};appendLine();append("Ответ: ${r.answer}")})
    private fun shareTask(t:TaskRecord)=shareText(t.type,buildString{appendLine(t.type);appendLine(t.input);appendLine();t.steps.forEachIndexed{i,st->appendLine("${i+1}. $st")};appendLine();append("Ответ: ${t.answer}")})
    private fun shareText(title:String,body:String){startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_SUBJECT,title);putExtra(Intent.EXTRA_TEXT,body)},"Через какое приложение отправить?"))}
    private fun rateApp(){val id=packageName;try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=$id")))}catch(_:Exception){startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=$id")))}}

    // ---------- HELPERS ----------
    private fun page():Pair<android.widget.ScrollView,LinearLayout>{val scroll=android.widget.ScrollView(this).apply{isFillViewport=true};val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(20),dp(18),dp(24))};scroll.addView(box,android.widget.FrameLayout.LayoutParams(-1,-2));return scroll to box}
    private fun screenTitle(title:String,subtitle:String):View=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;addView(text(title,29f,true));if(subtitle.isNotBlank())addView(text(subtitle,13.5f,false,muted()).apply{setPadding(0,dp(3),0,dp(14))})}
    private fun card():LinearLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(15),dp(14),dp(15),dp(14));background=round(cardColor(),17,border())}
    private fun messageCard(message:String,color:Int):View=card().apply{addView(text(message,15f,true,color))}
    private fun settingsCard(icon:Int,title:String,sub:String,action:()->Unit):View=card().apply{val row=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL};row.addView(iconImage(icon,28,accent));val texts=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),0,0,0);addView(text(title,16f,true));addView(text(sub,12.5f,false,muted()))};row.addView(texts,LinearLayout.LayoutParams(0,-2,1f));row.addView(text("›",26f,true,muted()));addView(row);setOnClickListener{action()}}
    private fun selectCard(title:String,selected:Boolean,action:()->Unit):View=card().apply{val row=LinearLayout(this@MainActivity).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL};row.addView(text(title,17f,true),LinearLayout.LayoutParams(0,-2,1f));if(selected)row.addView(text("✓",22f,true,green));addView(row);setOnClickListener{action()}}
    private fun solutionCard(r:SolveResult):View=card().apply{addView(text(r.type,13f,true,accent));addView(text(r.input,21f,true).apply{setPadding(0,dp(5),0,dp(9))});addView(text("Полное решение",17f,true));r.steps.forEachIndexed{i,st->addView(text("${i+1}. $st",14f).apply{setPadding(0,dp(3),0,dp(3))})};addView(text("Ответ: ${r.answer}",18f,true,green).apply{setPadding(0,dp(9),0,0)})}
    private fun metric(label:String,value:String,icon:Int):View=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;addView(iconImage(icon,21,accent),LinearLayout.LayoutParams(dp(21),dp(21)).apply{gravity=Gravity.CENTER});addView(text(value,24f,true).apply{gravity=Gravity.CENTER;setPadding(0,dp(5),0,0)});addView(text(label,11.5f,false,muted()).apply{gravity=Gravity.CENTER})}
    private fun progressStrip(index:Int,total:Int):View=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;val p=ProgressBar(this@MainActivity,null,android.R.attr.progressBarStyleHorizontal).apply{max=total;progress=index;progressTintList=ColorStateList.valueOf(accent);progressBackgroundTintList=ColorStateList.valueOf(border())};addView(p,LinearLayout.LayoutParams(-1,dp(6)));addView(text("$index / $total",11.5f,true,muted()).apply{gravity=Gravity.END;setPadding(0,dp(3),0,0)})}
    private fun labeled(label:String,view:View,height:Int=-2):View=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;addView(text(label,12f,true,muted()).apply{setPadding(dp(2),0,0,dp(4))});addView(view,LinearLayout.LayoutParams(-1,height))}
    private fun styledSpinner(items:List<String>):Spinner=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,items);background=round(cardColor(),13,border());setPadding(dp(10),0,dp(8),0)}
    private fun primary(title:String,icon:Int?=null,action:()->Unit):Button=Button(this).apply{text=(if(icon!=null)"  " else "")+title;textSize=14.5f;isAllCaps=false;setTextColor(Color.WHITE);background=round(accent,13);stateListAnimator=null;setOnClickListener{action()};if(icon!=null){setCompoundDrawablesWithIntrinsicBounds(icon,0,0,0);compoundDrawableTintList=ColorStateList.valueOf(Color.WHITE);compoundDrawablePadding=dp(8)};layoutParams=LinearLayout.LayoutParams(-1,dp(50))}
    private fun outline(title:String,icon:Int?=null,color:Int=fg(),action:()->Unit):Button=Button(this).apply{text=title;textSize=14f;isAllCaps=false;setTextColor(color);background=round(cardColor(),13,border());stateListAnimator=null;setOnClickListener{action()};if(icon!=null){setCompoundDrawablesWithIntrinsicBounds(icon,0,0,0);compoundDrawableTintList=ColorStateList.valueOf(color);compoundDrawablePadding=dp(7)}}
    private fun dangerButton(title:String,icon:Int,action:()->Unit):Button=outline(title,icon,red,action)
    private fun segment(title:String,selected:Boolean,action:()->Unit):Button=Button(this).apply{text=title;textSize=12.5f;isAllCaps=false;setTextColor(if(selected)Color.WHITE else fg());background=round(if(selected)accent else surface2(),12,if(selected)null else border());stateListAnimator=null;setPadding(dp(4),0,dp(4),0);setOnClickListener{action()}}
    private fun textButton(title:String,action:()->Unit):Button=Button(this).apply{text=title;textSize=14f;isAllCaps=false;setTextColor(accent);background=round(Color.TRANSPARENT,12);stateListAnimator=null;setOnClickListener{action()}}
    private fun iconOnly(icon:Int,action:()->Unit):ImageButton=ImageButton(this).apply{setImageResource(icon);imageTintList=ColorStateList.valueOf(fg());background=round(surface2(),12,border());setPadding(dp(11),dp(11),dp(11),dp(11));setOnClickListener{action()}}
    private fun iconLine(icon:Int,value:String,color:Int=fg(),bold:Boolean=false):View=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;addView(iconImage(icon,22,color));addView(text(value,14.5f,bold,color),LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(dp(10),0,0,0)})}
    private fun iconImage(icon:Int,size:Int,tint:Int):ImageView=ImageView(this).apply{setImageResource(icon);imageTintList=ColorStateList.valueOf(tint);setPadding(dp(1),dp(1),dp(1),dp(1))}.also{it.layoutParams=LinearLayout.LayoutParams(dp(size),dp(size))}
    private fun avatarView(p:UserProfile,size:Int):ImageView=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP;background=oval(surface2());clipToOutline=true;if(p.avatarUri.isNotBlank()){try{setImageURI(Uri.parse(p.avatarUri))}catch(_:Exception){setImageResource(R.drawable.ic_user);imageTintList=ColorStateList.valueOf(accent)}}else{setImageResource(R.drawable.ic_user);imageTintList=ColorStateList.valueOf(accent);setPadding(dp(size/5),dp(size/5),dp(size/5),dp(size/5))}}
    private fun divider():View=View(this).apply{setBackgroundColor(border());layoutParams=LinearLayout.LayoutParams(-1,dp(1)).apply{setMargins(dp(4),dp(6),dp(4),dp(6))}}
    private fun text(value:String,size:Float=15f,bold:Boolean=false,color:Int=fg()):TextView=TextView(this).apply{text=value;textSize=size;setTextColor(color);typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;setLineSpacing(0f,1.06f)}
    private fun space(h:Int):View=Space(this).apply{layoutParams=LinearLayout.LayoutParams(1,dp(h))}
    private fun round(fill:Int,radius:Int,stroke:Int?=null)=GradientDrawable().apply{cornerRadius=dp(radius).toFloat();setColor(fill);if(stroke!=null)setStroke(dp(1),stroke)}
    private fun oval(fill:Int)=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(fill);setStroke(dp(1),border())}
    private fun pinEdit(hintText:String)=EditText(this).apply{hint=hintText;inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD;maxLines=1;textSize=20f;letterSpacing=.18f;setTextColor(fg());setHintTextColor(muted());setPadding(dp(12),0,dp(12),0);background=round(cardColor(),13,border())}
    private fun pickDate(end:Boolean,onPicked:(Long)->Unit){val base=Calendar.getInstance().apply{if(end&&historyFilter.endAt!=Long.MAX_VALUE&&historyFilter.endAt>0)timeInMillis=historyFilter.endAt else if(historyFilter.startAt>0)timeInMillis=historyFilter.startAt};DatePickerDialog(this,{_,y,m,d->val c=Calendar.getInstance().apply{set(y,m,d,12,0,0);set(Calendar.MILLISECOND,0)};onPicked(c.timeInMillis)},base.get(Calendar.YEAR),base.get(Calendar.MONTH),base.get(Calendar.DAY_OF_MONTH)).show()}
    private fun dateLabel(time:Long,fallback:String)=if(time<=0)fallback else SimpleDateFormat("dd.MM.yyyy",Locale.getDefault()).format(Date(time))
    private fun startOfDay(time:Long)=Calendar.getInstance().apply{timeInMillis=time;set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}.timeInMillis
    private fun endOfDay(time:Long)=startOfDay(time)+86_399_999L
    private fun dateKey(time:Long)=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date(time))
    private fun formatDateKey(key:String):String=try{val d=SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(key)!!;SimpleDateFormat("d MMMM yyyy",Locale("ru")).format(d)}catch(_:Exception){key}
    private fun motivation(p:Int)=when{p==100->listOf("Без ошибок! Блестяще!","Идеальный результат!","Великолепно!").random();p>=90->listOf("Отличный результат!","Очень сильный результат!","Супер! Так держать!").random();p>=75->"Очень хорошо!";p>=60->"Есть хорошая база";else->"Разберём ошибки и станем сильнее"}
    private fun requestNotificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),900)}
    private fun dark()=store.settings.theme!="light"
    private fun bg()=if(dark())Color.rgb(17,18,22) else Color.rgb(246,247,251)
    private fun cardColor()=if(dark())Color.rgb(30,31,37) else Color.WHITE
    private fun surface2()=if(dark())Color.rgb(38,39,46) else Color.rgb(244,245,249)
    private fun fg()=if(dark())Color.rgb(245,245,247) else Color.rgb(28,29,34)
    private fun muted()=if(dark())Color.rgb(164,166,177) else Color.rgb(108,111,123)
    private fun border()=if(dark())Color.rgb(54,56,65) else Color.rgb(224,226,233)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun toast(v:String)=Toast.makeText(this,v,Toast.LENGTH_SHORT).show()
}

class SimpleTextWatcher(private val callback:(String)->Unit):android.text.TextWatcher{
    override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
    override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){callback(s?.toString().orEmpty())}
    override fun afterTextChanged(s:android.text.Editable?){}
}
