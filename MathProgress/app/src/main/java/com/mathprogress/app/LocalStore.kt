package com.mathprogress.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class LocalStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("math_progress_store", Context.MODE_PRIVATE)
    val profiles = mutableListOf<UserProfile>()
    val tasks = mutableListOf<TaskRecord>()
    var settings = AppSettings()
    var activeProfileId: String = ""

    init { load() }

    fun activeProfile(): UserProfile {
        var p = profiles.firstOrNull { it.id == activeProfileId }
        if (p == null) {
            if (profiles.isEmpty()) profiles += UserProfile(name = "Пользователь")
            p = profiles.first()
            activeProfileId = p.id
            save()
        }
        return p
    }

    fun load() {
        profiles.clear(); tasks.clear()
        try {
            val pArr = JSONArray(prefs.getString("profiles", "[]"))
            for (i in 0 until pArr.length()) profiles += UserProfile.fromJson(pArr.getJSONObject(i))
            val tArr = JSONArray(prefs.getString("tasks", "[]"))
            for (i in 0 until tArr.length()) tasks += TaskRecord.fromJson(tArr.getJSONObject(i))
            activeProfileId = prefs.getString("activeProfileId", "") ?: ""
            val s = JSONObject(prefs.getString("settings", "{}") ?: "{}")
            settings = AppSettings(
                theme = s.optString("theme", "system"), language = s.optString("language", "ru"),
                notificationsEnabled = s.optBoolean("notificationsEnabled", true),
                unfinishedNotifications = s.optBoolean("unfinishedNotifications", true),
                inactivityNotifications = s.optBoolean("inactivityNotifications", true),
                inactivityDays = s.optInt("inactivityDays", 3).coerceIn(2,7),
                passwordHash = s.optString("passwordHash", "")
            )
        } catch (_: Exception) { profiles.clear(); tasks.clear(); settings = AppSettings() }
        if (profiles.isEmpty()) profiles += UserProfile(name = "Пользователь")
        if (activeProfileId.isBlank() || profiles.none { it.id == activeProfileId }) activeProfileId = profiles.first().id
        purgeTrash(); save()
    }

    fun save() {
        val pa = JSONArray(); profiles.forEach { pa.put(it.toJson()) }
        val ta = JSONArray(); tasks.forEach { ta.put(it.toJson()) }
        val s = JSONObject()
            .put("theme", settings.theme).put("language", settings.language)
            .put("notificationsEnabled", settings.notificationsEnabled)
            .put("unfinishedNotifications", settings.unfinishedNotifications)
            .put("inactivityNotifications", settings.inactivityNotifications)
            .put("inactivityDays", settings.inactivityDays).put("passwordHash", settings.passwordHash)
        prefs.edit().putString("profiles", pa.toString()).putString("tasks", ta.toString())
            .putString("activeProfileId", activeProfileId).putString("settings", s.toString()).apply()
    }

    fun addOrUpdateTask(task: TaskRecord) {
        task.updatedAt = System.currentTimeMillis()
        val i = tasks.indexOfFirst { it.id == task.id }
        if (i >= 0) tasks[i] = task else tasks += task
        save()
    }

    fun deleteToTrash(id: String) { tasks.firstOrNull { it.id == id }?.let { it.deletedAt = System.currentTimeMillis(); save() } }
    fun restore(id: String) { tasks.firstOrNull { it.id == id }?.let { it.deletedAt = 0L; save() } }
    fun deleteForever(id: String) { tasks.removeAll { it.id == id }; save() }
    fun purgeTrash() {
        val cutoff = System.currentTimeMillis() - 30L*24*60*60*1000
        tasks.removeAll { it.deletedAt in 1 until cutoff }
    }

    fun history(profileId: String = activeProfileId): List<TaskRecord> = tasks.filter { it.profileId == profileId && it.deletedAt == 0L }
    fun trash(profileId: String = activeProfileId): List<TaskRecord> = tasks.filter { it.profileId == profileId && it.deletedAt > 0L }

    fun setDraft(text: String) {
        prefs.edit().putString("draft_$activeProfileId", text).putLong("draft_time_$activeProfileId", System.currentTimeMillis()).apply()
    }
    fun getDraft(): String = prefs.getString("draft_$activeProfileId", "") ?: ""
    fun draftTime(): Long = prefs.getLong("draft_time_$activeProfileId", 0L)
    fun clearDraft() { prefs.edit().remove("draft_$activeProfileId").remove("draft_time_$activeProfileId").apply() }

    fun setLastOpenNow() { prefs.edit().putLong("last_open", System.currentTimeMillis()).apply() }
    fun lastOpen(): Long = prefs.getLong("last_open", System.currentTimeMillis())

    fun exportJson(): String {
        save()
        val drafts = JSONObject()
        profiles.forEach { p ->
            drafts.put(p.id, JSONObject()
                .put("text", prefs.getString("draft_${p.id}", "") ?: "")
                .put("time", prefs.getLong("draft_time_${p.id}", 0L)))
        }
        return JSONObject()
            .put("version", 1)
            .put("profiles", JSONArray(prefs.getString("profiles", "[]")))
            .put("tasks", JSONArray(prefs.getString("tasks", "[]")))
            .put("activeProfileId", activeProfileId)
            .put("settings", JSONObject(prefs.getString("settings", "{}") ?: "{}"))
            .put("drafts", drafts)
            .toString(2)
    }

    fun importJson(text: String) {
        val root = JSONObject(text)
        val editor = prefs.edit()
            .putString("profiles", root.optJSONArray("profiles")?.toString() ?: "[]")
            .putString("tasks", root.optJSONArray("tasks")?.toString() ?: "[]")
            .putString("activeProfileId", root.optString("activeProfileId", ""))
            .putString("settings", root.optJSONObject("settings")?.toString() ?: "{}")
        val drafts = root.optJSONObject("drafts")
        if (drafts != null) {
            val keys = drafts.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val d = drafts.optJSONObject(id) ?: continue
                editor.putString("draft_$id", d.optString("text", ""))
                editor.putLong("draft_time_$id", d.optLong("time", 0L))
            }
        }
        editor.apply()
        load()
    }

    fun setPassword(password: String) { settings.passwordHash = secureHash(password); save() }
    fun checkPassword(password: String): Boolean {
        val stored = settings.passwordHash
        if (stored.isBlank()) return true
        if (!stored.contains(":")) return stored == legacyHash(password)
        val parts = stored.split(":", limit = 2)
        return try {
            val salt = Base64.getDecoder().decode(parts[0])
            val expected = Base64.getDecoder().decode(parts[1])
            val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
            val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            MessageDigest.isEqual(expected, actual)
        } catch (_: Exception) { false }
    }
    fun hasPassword(): Boolean = settings.passwordHash.isNotBlank()

    fun resetAll() {
        prefs.edit().clear().apply(); load()
    }

    private fun secureHash(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash)
    }
    private fun legacyHash(s: String): String = MessageDigest.getInstance("SHA-256")
        .digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
