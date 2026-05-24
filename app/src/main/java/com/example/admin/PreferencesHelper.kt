package com.example.admin

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.regex.Pattern

/**
 * Stores local app states such as OTP values, login status, Telegram coordinates, and Firebase credentials.
 * Also parses the raw web configuration copy-paste block dynamically.
 */
class PreferencesHelper(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("soham_admin_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "PreferencesHelper"
        
        // Defaults from student website HTML
        const val DEFAULT_API_KEY = "AIzaSyDWdkSO7PBQw2rzauDL7nGLv6VS2q8Q5qQ"
        const val DEFAULT_PROJECT_ID = "sohamtally-org"
        const val DEFAULT_AUTH_DOMAIN = "sohamtally-org.firebaseapp.com"
        const val DEFAULT_STORAGE_BUCKET = "sohamtally-org.firebasestorage.app"
        const val DEFAULT_MESSAGING_SENDER_ID = "941657818809"
        const val DEFAULT_APP_ID = "1:941657818809:web:cdc7157d5d6181535cbd68"
        const val DEFAULT_MEASUREMENT_ID = "G-NW6VDE2MK3"

        // Defaults for Telegram Bot
        const val DEFAULT_BOT_TOKEN = "8622712481:AAE79LlwmZau-ElGv6yqEjoz62-jhMNLJRI"
        const val DEFAULT_CHAT_ID = "-1003894592765"
        
        // IMGBB
        const val DEFAULT_IMGBB_KEY = "f4e8c093429f4a9ec2937aef8637484b"
    }

    var firebaseApiKey: String
        get() = prefs.getString("fb_api_key", DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        set(value) = prefs.edit().putString("fb_api_key", value).apply()

    var firebaseProjectId: String
        get() = prefs.getString("fb_project_id", DEFAULT_PROJECT_ID) ?: DEFAULT_PROJECT_ID
        set(value) = prefs.edit().putString("fb_project_id", value).apply()

    var firebaseAuthDomain: String
        get() = prefs.getString("fb_auth_domain", DEFAULT_AUTH_DOMAIN) ?: DEFAULT_AUTH_DOMAIN
        set(value) = prefs.edit().putString("fb_auth_domain", value).apply()

    var firebaseStorageBucket: String
        get() = prefs.getString("fb_storage_bucket", DEFAULT_STORAGE_BUCKET) ?: DEFAULT_STORAGE_BUCKET
        set(value) = prefs.edit().putString("fb_storage_bucket", value).apply()

    var firebaseMessagingSenderId: String
        get() = prefs.getString("fb_messaging_sender_id", DEFAULT_MESSAGING_SENDER_ID) ?: DEFAULT_MESSAGING_SENDER_ID
        set(value) = prefs.edit().putString("fb_messaging_sender_id", value).apply()

    var firebaseAppId: String
        get() = prefs.getString("fb_app_id", DEFAULT_APP_ID) ?: DEFAULT_APP_ID
        set(value) = prefs.edit().putString("fb_app_id", value).apply()

    var firebaseMeasurementId: String
        get() = prefs.getString("fb_measurement_id", DEFAULT_MEASUREMENT_ID) ?: DEFAULT_MEASUREMENT_ID
        set(value) = prefs.edit().putString("fb_measurement_id", value).apply()

    // Telegram Bot Token
    var telegramBotToken: String
        get() = prefs.getString("tg_bot_token", DEFAULT_BOT_TOKEN) ?: DEFAULT_BOT_TOKEN
        set(value) = prefs.edit().putString("tg_bot_token", value).apply()

    // Telegram Chat ID
    var telegramChatId: String
        get() = prefs.getString("tg_chat_id", DEFAULT_CHAT_ID) ?: DEFAULT_CHAT_ID
        set(value) = prefs.edit().putString("tg_chat_id", value).apply()

    // ImgBB API key
    var imgbbApiKey: String
        get() = prefs.getString("imgbb_api_key", DEFAULT_IMGBB_KEY) ?: DEFAULT_IMGBB_KEY
        set(value) = prefs.edit().putString("imgbb_api_key", value).apply()

    // Admin Session Login
    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    var telegramSavedOtp: String
        get() = prefs.getString("tg_saved_otp", "") ?: ""
        set(value) = prefs.edit().putString("tg_saved_otp", value).apply()

    var telegramOtpExpiry: Long
        get() = prefs.getLong("tg_otp_expiry", 0)
        set(value) = prefs.edit().putLong("tg_otp_expiry", value).apply()

    /**
     * Parses the whole JS block dynamically to extract config variables.
     * Returns true if any useful variables are found and saved.
     */
    fun parseAndSaveFirebaseBlock(rawBlockText: String): Boolean {
        var updated = false

        val keysInfo = mapOf(
            "apiKey" to "fb_api_key",
            "projectId" to "fb_project_id",
            "authDomain" to "fb_auth_domain",
            "storageBucket" to "fb_storage_bucket",
            "messagingSenderId" to "fb_messaging_sender_id",
            "appId" to "fb_app_id",
            "measurementId" to "fb_measurement_id"
        )

        for ((jsKey, prefKey) in keysInfo) {
            // Regex to match "apiKey: "VALUE"" or "apiKey": 'VALUE' etc
            val pattern1 = Pattern.compile("[\"']?$jsKey[\"']?\\s*:\\s*[\"']([^\"']+)[\"']")
            val matcher1 = pattern1.matcher(rawBlockText)
            if (matcher1.find()) {
                val value = matcher1.group(1)
                if (value != null && value.isNotBlank()) {
                    prefs.edit().putString(prefKey, value).apply()
                    Log.d(TAG, "Parsed and saved firebase variable: $jsKey = $value")
                    updated = true
                }
            }
        }
        return updated
    }

    /**
     * Resets modern configuration to brand defaults.
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
