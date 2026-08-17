package com.mathprogress.app

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var avatarUri: String = ""
) {
    fun toJson() = JSONObject().put("id", id).put("name", name).put("avatarUri", avatarUri)
    companion object {
        fun fromJson(o: JSONObject) = UserProfile(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name", "Пользователь"),
            avatarUri = o.optString("avatarUri", "")
        )
    }
}

data class TaskRecord(
    val id: String = UUID.randomUUID().toString(),
    var profileId: String,
    var input: String,
    var type: String,
    var answer: String,
    var steps: List<String>,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var deletedAt: Long = 0L,
    var selfSolved: Boolean = false,
    var checked: Boolean = false,
    var correct: Boolean = false,
    var grade: Int = 0,
    var source: String = "solver",
    var difficulty: Int = 1,
    var userAnswer: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("profileId", profileId).put("input", input).put("type", type)
        .put("answer", answer).put("steps", JSONArray(steps)).put("createdAt", createdAt).put("updatedAt", updatedAt)
        .put("deletedAt", deletedAt).put("selfSolved", selfSolved).put("checked", checked).put("correct", correct)
        .put("grade", grade).put("source", source).put("difficulty", difficulty).put("userAnswer", userAnswer)

    companion object {
        fun fromJson(o: JSONObject): TaskRecord {
            val a = o.optJSONArray("steps") ?: JSONArray()
            val steps = (0 until a.length()).map { a.optString(it) }
            return TaskRecord(
                id = o.optString("id", UUID.randomUUID().toString()), profileId = o.optString("profileId"), input = o.optString("input"),
                type = o.optString("type"), answer = o.optString("answer"), steps = steps,
                createdAt = o.optLong("createdAt", System.currentTimeMillis()), updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                deletedAt = o.optLong("deletedAt", 0L), selfSolved = o.optBoolean("selfSolved", false),
                checked = o.optBoolean("checked", false), correct = o.optBoolean("correct", false),
                grade = o.optInt("grade", 0), source = o.optString("source", "solver"),
                difficulty = o.optInt("difficulty", 1).coerceIn(1,4), userAnswer = o.optString("userAnswer", "")
            )
        }
    }
}

data class DailyProgress(
    val profileId: String,
    val dateKey: String,
    var attempts: Int = 0,
    var bestCorrect: Int = 0,
    var completed: Boolean = false,
    var completedAt: Long = 0L
) {
    fun toJson() = JSONObject()
        .put("profileId", profileId).put("dateKey", dateKey).put("attempts", attempts)
        .put("bestCorrect", bestCorrect).put("completed", completed).put("completedAt", completedAt)

    companion object {
        fun fromJson(o: JSONObject) = DailyProgress(
            profileId = o.optString("profileId"), dateKey = o.optString("dateKey"),
            attempts = o.optInt("attempts", 0), bestCorrect = o.optInt("bestCorrect", 0),
            completed = o.optBoolean("completed", false), completedAt = o.optLong("completedAt", 0L)
        )
    }
}

data class AppSettings(
    var theme: String = "dark",
    var language: String = "ru",
    var securityMethod: String = "none",
    var pinHash: String = "",
    var patternHash: String = "",
    var autoLockSeconds: Int = 60,
    var inactivityDays: Int = 3,
    var onboardingComplete: Boolean = false,
    var disclaimerAccepted: Boolean = false
)
