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
    val dailyProgress = mutableListOf<DailyProgress>()
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
        profiles.clear(); tasks.clear(); dailyProgress.clear()
        try {
            val pArr = JSONArray(prefs.getString("profiles", "[]"))
            for (i in 0 until pArr.length()) profiles += UserProfile.fromJson(pArr.getJSONObject(i))
            val tArr = JSONArray(prefs.getString("tasks", "[]"))
            for (i in 0 until tArr.length()) tasks += TaskRecord.fromJson(tArr.getJSONObject(i))
            val dArr = JSONArray(prefs.getString("dailyProgress", "[]"))
            for (i in 0 until dArr.length()) dailyProgress += DailyProgress.fromJson(dArr.getJSONObject(i))
            activeProfileId = prefs.getString("activeProfileId", "") ?: ""
            val s = JSONObject(prefs.getString("settings", "{}") ?: "{}")
            val oldPasswordHash = s.optString("passwordHash", "")
            val migratedMethod = if (oldPasswordHash.isNotBlank() && s.optString("securityMethod").isBlank()) "pin" else s.optString("securityMethod", "none")
            settings = AppSettings(
                theme = s.optString("theme", "dark").let { if (it == "system") "dark" else it },
                language = s.optString("language", "ru"),
                securityMethod = migratedMethod,
                pinHash = s.optString("pinHash", oldPasswordHash),
                patternHash = s.optString("patternHash", ""),
                autoLockSeconds = s.optInt("autoLockSeconds", 60).let { if (it in listOf(0, 60, 180, 300)) it else 60 },
                inactivityDays = s.optInt("inactivityDays", 3).coerceIn(2, 7)
            )
        } catch (_: Exception) {
            profiles.clear(); tasks.clear(); dailyProgress.clear(); settings = AppSettings()
        }
        if (profiles.isEmpty()) profiles += UserProfile(name = "Пользователь")
        if (activeProfileId.isBlank() || profiles.none { it.id == activeProfileId }) activeProfileId = profiles.first().id
        purgeTrash(); save()
    }

    fun save() {
        val pa = JSONArray(); profiles.forEach { pa.put(it.toJson()) }
        val ta = JSONArray(); tasks.forEach { ta.put(it.toJson()) }
        val da = JSONArray(); dailyProgress.forEach { da.put(it.toJson()) }
        val s = JSONObject()
            .put("theme", settings.theme).put("language", settings.language)
            .put("securityMethod", settings.securityMethod).put("pinHash", settings.pinHash)
            .put("patternHash", settings.patternHash).put("autoLockSeconds", settings.autoLockSeconds)
            .put("inactivityDays", settings.inactivityDays)
        prefs.edit().putString("profiles", pa.toString()).putString("tasks", ta.toString())
            .putString("dailyProgress", da.toString()).putString("activeProfileId", activeProfileId)
            .putString("settings", s.toString()).apply()
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
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        tasks.removeAll { it.deletedAt in 1 until cutoff }
    }

    fun history(profileId: String = activeProfileId): List<TaskRecord> = tasks.filter { it.profileId == profileId && it.deletedAt == 0L }
    fun trash(profileId: String = activeProfileId): List<TaskRecord> = tasks.filter { it.profileId == profileId && it.deletedAt > 0L }

    fun daily(dateKey: String, profileId: String = activeProfileId): DailyProgress? = dailyProgress.firstOrNull { it.profileId == profileId && it.dateKey == dateKey }
    fun saveDaily(progress: DailyProgress) {
        val i = dailyProgress.indexOfFirst { it.profileId == progress.profileId && it.dateKey == progress.dateKey }
        if (i >= 0) dailyProgress[i] = progress else dailyProgress += progress
        save()
    }
    fun dailyForProfile(profileId: String = activeProfileId): List<DailyProgress> = dailyProgress.filter { it.profileId == profileId }

    fun setDraft(text: String) {
        prefs.edit().putString("draft_$activeProfileId", text).putLong("draft_time_$activeProfileId", System.currentTimeMillis()).apply()
    }
    fun getDraft(): String = prefs.getString("draft_$activeProfileId", "") ?: ""
    fun draftTime(): Long = prefs.getLong("draft_time_$activeProfileId", 0L)
    fun clearDraft() { prefs.edit().remove("draft_$activeProfileId").remove("draft_time_$activeProfileId").apply() }

    fun setLastOpenNow() { prefs.edit().putLong("last_open", System.currentTimeMillis()).apply() }
    fun lastOpen(): Long = prefs.getLong("last_open", System.currentTimeMillis())
    fun setBackgroundAt(time: Long) { prefs.edit().putLong("background_at", time).apply() }
    fun backgroundAt(): Long = prefs.getLong("background_at", 0L)
    fun setLastBackupAt(time: Long) { prefs.edit().putLong("last_backup_at", time).apply() }
    fun lastBackupAt(): Long = prefs.getLong("last_backup_at", 0L)

    fun exportJson(): String {
        save()
        val drafts = JSONObject()
        profiles.forEach { p ->
            drafts.put(p.id, JSONObject()
                .put("text", prefs.getString("draft_${p.id}", "") ?: "")
                .put("time", prefs.getLong("draft_time_${p.id}", 0L)))
        }
        val safeSettings = JSONObject()
            .put("theme", settings.theme).put("language", settings.language)
            .put("autoLockSeconds", settings.autoLockSeconds).put("inactivityDays", settings.inactivityDays)
        return JSONObject()
            .put("version", 2)
            .put("profiles", JSONArray(prefs.getString("profiles", "[]")))
            .put("tasks", JSONArray(prefs.getString("tasks", "[]")))
            .put("dailyProgress", JSONArray(prefs.getString("dailyProgress", "[]")))
            .put("activeProfileId", activeProfileId)
            .put("settings", safeSettings)
            .put("drafts", drafts)
            .toString(2)
    }

    fun importJson(text: String) {
        val root = JSONObject(text)
        val importedSettings = root.optJSONObject("settings") ?: JSONObject()
        val currentSecurity = settings.copy()
        val mergedSettings = JSONObject()
            .put("theme", importedSettings.optString("theme", settings.theme))
            .put("language", importedSettings.optString("language", settings.language))
            .put("securityMethod", currentSecurity.securityMethod)
            .put("pinHash", currentSecurity.pinHash)
            .put("patternHash", currentSecurity.patternHash)
            .put("autoLockSeconds", importedSettings.optInt("autoLockSeconds", currentSecurity.autoLockSeconds))
            .put("inactivityDays", importedSettings.optInt("inactivityDays", currentSecurity.inactivityDays))
        val editor = prefs.edit()
            .putString("profiles", root.optJSONArray("profiles")?.toString() ?: "[]")
            .putString("tasks", root.optJSONArray("tasks")?.toString() ?: "[]")
            .putString("dailyProgress", root.optJSONArray("dailyProgress")?.toString() ?: "[]")
            .putString("activeProfileId", root.optString("activeProfileId", ""))
            .putString("settings", mergedSettings.toString())
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
        editor.apply(); load()
    }

    fun setPin(pin: String) { settings.pinHash = secureHash(pin); settings.securityMethod = "pin"; save() }
    fun checkPin(pin: String): Boolean = verifySecureHash(pin, settings.pinHash)
    fun setPattern(pattern: String) { settings.patternHash = secureHash(pattern); settings.securityMethod = "pattern"; save() }
    fun checkPattern(pattern: String): Boolean = verifySecureHash(pattern, settings.patternHash)
    fun disableSecurity() { settings.securityMethod = "none"; settings.pinHash = ""; settings.patternHash = ""; save() }
    fun securityEnabled(): Boolean = settings.securityMethod != "none"

    fun resetAll() { prefs.edit().clear().apply(); load() }

    private fun secureHash(value: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(value.toCharArray(), salt, 150_000, 256)
        val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash)
    }

    private fun verifySecureHash(value: String, stored: String): Boolean {
        if (stored.isBlank()) return false
        if (!stored.contains(":")) {
            val legacy = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
            return MessageDigest.isEqual(legacy.toByteArray(), stored.toByteArray())
        }
        return try {
            val parts = stored.split(":", limit = 2)
            val salt = Base64.getDecoder().decode(parts[0])
            val expected = Base64.getDecoder().decode(parts[1])
            val spec = PBEKeySpec(value.toCharArray(), salt, 150_000, 256)
            val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            MessageDigest.isEqual(expected, actual)
        } catch (_: Exception) { false }
    }
}
