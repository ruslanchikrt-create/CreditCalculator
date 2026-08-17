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
    companion object { fun fromJson(o: JSONObject) = UserProfile(o.getString("id"), o.optString("name", "Пользователь"), o.optString("avatarUri", "")) }
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
    var source: String = "solver"
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("profileId", profileId).put("input", input).put("type", type)
        .put("answer", answer).put("steps", JSONArray(steps)).put("createdAt", createdAt).put("updatedAt", updatedAt)
        .put("deletedAt", deletedAt).put("selfSolved", selfSolved).put("checked", checked).put("correct", correct)
        .put("grade", grade).put("source", source)

    companion object {
        fun fromJson(o: JSONObject): TaskRecord {
            val a = o.optJSONArray("steps") ?: JSONArray()
            val steps = (0 until a.length()).map { a.optString(it) }
            return TaskRecord(
                id = o.getString("id"), profileId = o.getString("profileId"), input = o.optString("input"),
                type = o.optString("type"), answer = o.optString("answer"), steps = steps,
                createdAt = o.optLong("createdAt", System.currentTimeMillis()), updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                deletedAt = o.optLong("deletedAt", 0L), selfSolved = o.optBoolean("selfSolved", false),
                checked = o.optBoolean("checked", false), correct = o.optBoolean("correct", false),
                grade = o.optInt("grade", 0), source = o.optString("source", "solver")
            )
        }
    }
}

data class AppSettings(
    var theme: String = "system",
    var language: String = "ru",
    var notificationsEnabled: Boolean = true,
    var unfinishedNotifications: Boolean = true,
    var inactivityNotifications: Boolean = true,
    var inactivityDays: Int = 3,
    var passwordHash: String = ""
)
