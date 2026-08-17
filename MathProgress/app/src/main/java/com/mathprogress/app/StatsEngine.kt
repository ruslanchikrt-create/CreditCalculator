package com.mathprogress.app

import java.util.Calendar
import kotlin.math.roundToInt

enum class StatsPeriod { WEEK, MONTH, YEAR, ALL }

data class StatsSnapshot(
    val solved: Int,
    val checked: Int,
    val correct: Int,
    val accuracy: Int?,
    val averageGrade: Double?,
    val selfSolved: Int,
    val bestTopic: String?,
    val weakTopic: String?,
    val message: String,
    val comparison: String,
    val difficultyCounts: Map<Int,Int>,
    val difficultyAccuracy: Map<Int,Int>,
    val currentDifficulty: Int,
    val maxSuccessfulDifficulty: Int
)

object StatsEngine {
    fun snapshot(tasks: List<TaskRecord>, period: StatsPeriod, now: Long = System.currentTimeMillis()): StatsSnapshot {
        val (start, previousStart, previousEnd) = bounds(period, now)
        val current = tasks.filter { it.deletedAt == 0L && it.createdAt >= start && it.createdAt <= now }
        val prev = if (period == StatsPeriod.ALL) emptyList() else tasks.filter { it.deletedAt == 0L && it.createdAt >= previousStart && it.createdAt <= previousEnd }
        val checked = current.filter { it.checked }
        val correct = checked.count { it.correct }
        val accuracy = if (checked.isEmpty()) null else (correct * 100.0 / checked.size).roundToInt()
        val grades = current.mapNotNull { if (it.grade in 1..5) it.grade else null }
        val avg = grades.takeIf { it.isNotEmpty() }?.average()
        val topicRates = checked.groupBy { it.type }.mapValues { (_, list) -> list.count { it.correct }.toDouble() / list.size }
        val best = topicRates.maxByOrNull { it.value }?.key
        val weak = topicRates.minByOrNull { it.value }?.key

        val prevChecked = prev.filter { it.checked }
        val prevAcc = if (prevChecked.isEmpty()) null else (prevChecked.count { it.correct } * 100.0 / prevChecked.size).roundToInt()
        val comparison = when {
            accuracy != null && prevAcc != null && accuracy > prevAcc -> "Уже лучше! Точность выросла с $prevAcc% до $accuracy%."
            accuracy != null && prevAcc != null && accuracy < prevAcc -> "Сейчас $accuracy%. Разберите ошибки — результат можно быстро вернуть вверх."
            accuracy != null && prevAcc != null -> "Результат стабильный: $accuracy%."
            else -> ""
        }
        val counts = (1..4).associateWith { level -> current.count { it.difficulty == level } }
        val byLevel = (1..4).associateWith { level ->
            val items = checked.filter { it.difficulty == level }
            if (items.isEmpty()) 0 else (items.count { it.correct } * 100.0 / items.size).roundToInt()
        }
        val suggested = PracticeEngine.suggestedDifficulty(current.ifEmpty { tasks })
        val maxSuccessful = checked.filter { it.correct }.maxOfOrNull { it.difficulty } ?: 0
        val message = motivational(accuracy, avg, current.size)
        return StatsSnapshot(current.size, checked.size, correct, accuracy, avg, current.count { it.selfSolved }, best, weak, message, comparison, counts, byLevel, suggested, maxSuccessful)
    }

    private fun motivational(accuracy: Int?, avg: Double?, solved: Int): String {
        if (solved == 0) return "Начните с одной задачи — прогресс появится здесь."
        return when {
            accuracy != null && accuracy == 100 -> listOf("Без ошибок! Блестяще!", "Идеальный результат!", "Великолепно — 100%!").random()
            accuracy != null && accuracy >= 95 -> listOf("Очень сильный результат!", "Блестяще!", "Отличная форма!").random()
            accuracy != null && accuracy >= 90 -> listOf("Отличный результат!", "Супер! Так держать!", "Почти без ошибок!").random()
            avg != null && avg >= 4.5 -> "Отличная средняя оценка — продолжайте в том же духе!"
            accuracy != null && accuracy >= 75 -> "Хороший результат. Ещё немного практики — и будет отлично."
            accuracy != null && accuracy >= 60 -> "Есть хорошая база. Разбор ошибок поможет поднять результат."
            accuracy != null -> "Продолжайте тренироваться: приложение покажет, где именно теряются баллы."
            else -> "Решения сохраняются. Для оценки знаний пройдите небольшую проверку."
        }
    }

    private fun bounds(period: StatsPeriod, now: Long): Triple<Long,Long,Long> {
        if (period == StatsPeriod.ALL) return Triple(0L,0L,0L)
        val currentStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0)
            when(period) {
                StatsPeriod.WEEK -> { val daysFromMonday = (get(Calendar.DAY_OF_WEEK) + 5) % 7; add(Calendar.DAY_OF_YEAR, -daysFromMonday) }
                StatsPeriod.MONTH -> set(Calendar.DAY_OF_MONTH,1)
                StatsPeriod.YEAR -> { set(Calendar.MONTH,Calendar.JANUARY); set(Calendar.DAY_OF_MONTH,1) }
                StatsPeriod.ALL -> {}
            }
        }
        val start = currentStart.timeInMillis
        val elapsed = now - start
        val previousStart = Calendar.getInstance().apply {
            timeInMillis = start
            when(period) {
                StatsPeriod.WEEK -> add(Calendar.DAY_OF_YEAR,-7)
                StatsPeriod.MONTH -> add(Calendar.MONTH,-1)
                StatsPeriod.YEAR -> add(Calendar.YEAR,-1)
                StatsPeriod.ALL -> {}
            }
        }.timeInMillis
        val previousEnd = kotlin.math.min(start-1, previousStart + elapsed)
        return Triple(start,previousStart,previousEnd)
    }
}
