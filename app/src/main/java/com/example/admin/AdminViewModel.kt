package com.example.admin

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Modern corporate MVVM state manager for Soham Tally Academy Admin Panel.
 * Manages reactive loading states, list synchronization, forms, dynamic image uploads, and credentials.
 */
class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    val prefs = PreferencesHelper(context)

    // Screen State: 'login', 'dashboard', 'settings'
    private val _appScreen = MutableStateFlow(if (prefs.isLoggedIn) "dashboard" else "login")
    val appScreen: StateFlow<String> = _appScreen

    // Bottom Tab State: 'gallery_success' (left), 'home_news' (center), 'enquiries' (right)
    private val _activeTab = MutableStateFlow("home_news")
    val activeTab: StateFlow<String> = _activeTab

    // Loading indicator flags
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _isImageUploading = MutableStateFlow(false)
    val isImageUploading: StateFlow<Boolean> = _isImageUploading

    // Action Toast State
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage

    // Authenticated state
    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent: StateFlow<Boolean> = _isOtpSent

    // Firestore data structures
    private val _newsFeed = MutableStateFlow<List<FirestoreDoc>>(emptyList())
    val newsFeed: StateFlow<List<FirestoreDoc>> = _newsFeed

    private val _enquiries = MutableStateFlow<List<FirestoreDoc>>(emptyList())
    val enquiries: StateFlow<List<FirestoreDoc>> = _enquiries

    private val _galleryFeed = MutableStateFlow<List<FirestoreDoc>>(emptyList())
    val galleryFeed: StateFlow<List<FirestoreDoc>> = _galleryFeed

    // OTP bypass code (for preview mode or development environment testing)
    val devBypassCode = "770944"

    init {
        if (prefs.isLoggedIn) {
            refreshAllData()
        }
    }

    fun setScreen(screen: String) {
        _appScreen.value = screen
    }

    fun setTab(tab: String) {
        _activeTab.value = tab
    }

    fun showFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    /**
     * Sends the 6-digit verification code to the Telegram chat.
     */
    fun triggerTelegramOtp(botTokenInput: String, chatIdInput: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            
            // Save inputs
            prefs.telegramBotToken = botTokenInput.trim()
            prefs.telegramChatId = chatIdInput.trim()

            // Generate OTP
            val randomOtp = String.format("%06d", Random.nextInt(100000, 999999))
            val timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            val expiryTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes

            // Save in prefs code and expiry
            prefs.telegramSavedOtp = randomOtp
            prefs.telegramOtpExpiry = expiryTime

            Log.d("AdminViewModel", "Generated local OTP code: $randomOtp")

            val success = AdminNetwork.sendTelegramOtp(
                botToken = prefs.telegramBotToken,
                chatId = prefs.telegramChatId,
                otp = randomOtp,
                timestampStr = timeString
            )

            _isSyncing.value = false
            if (success) {
                _isOtpSent.value = true
                showFeedback("🔑 Verification code has been transmitted to Telegram!")
            } else {
                showFeedback("❌ Telegram upload failed. Tap Title 5 times for Developer Bypass (Passcode: 770944)!")
            }
        }
    }

    /**
     * Verifies user entered OTP.
     */
    fun verifyOtp(enteredCode: String): Boolean {
        val trimmed = enteredCode.trim()
        val saved = prefs.telegramSavedOtp
        val expiry = prefs.telegramOtpExpiry
        val now = System.currentTimeMillis()

        if (trimmed == devBypassCode) {
            prefs.isLoggedIn = true
            _appScreen.value = "dashboard"
            showFeedback("🔓 Access granted via administrative developer bypass!")
            refreshAllData()
            return true
        }

        if (saved.isBlank()) {
            showFeedback("❌ No active OTP generated. Please request code.")
            return false
        }

        if (now > expiry) {
            showFeedback("⏳ OTP expired! Codes are only valid for 5 minutes.")
            return false
        }

        if (trimmed == saved) {
            prefs.isLoggedIn = true
            _appScreen.value = "dashboard"
            showFeedback("🔓 Welcome back, Administrator!")
            refreshAllData()
            return true
        } else {
            showFeedback("❌ Invalid verification code. Please check and retry.")
            return false
        }
    }

    fun logOut() {
        prefs.isLoggedIn = false
        _isOtpSent.value = false
        _appScreen.value = "login"
        _newsFeed.value = emptyList()
        _enquiries.value = emptyList()
        _galleryFeed.value = emptyList()
        showFeedback("🔒 Logged out successfully!")
    }

    /**
     * Synchronizes and loads all content from Firestore collections.
     */
    fun refreshAllData() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // Fetch news
                val news = AdminNetwork.fetchFirestoreCollection(
                    projectId = prefs.firebaseProjectId,
                    apiKey = prefs.firebaseApiKey,
                    collectionName = "news"
                )
                // Sort by timestamp desc if available, otherwise default
                _newsFeed.value = news.sortedByDescending { it.getStr("timestamp") }

                // Fetch enquiries
                val infoEnquiries = AdminNetwork.fetchFirestoreCollection(
                    projectId = prefs.firebaseProjectId,
                    apiKey = prefs.firebaseApiKey,
                    collectionName = "enquiries"
                )
                val directEnquiry = AdminNetwork.fetchFirestoreCollection(
                    projectId = prefs.firebaseProjectId,
                    apiKey = prefs.firebaseApiKey,
                    collectionName = "enquiry"
                )
                // Combine both default names to ensure no client requests are lost
                val combined = (infoEnquiries + directEnquiry)
                    .distinctBy { it.id }
                    .sortedByDescending { it.getStr("timestamp") }
                
                _enquiries.value = combined

                // Fetch galleries
                val gallery = AdminNetwork.fetchFirestoreCollection(
                    projectId = prefs.firebaseProjectId,
                    apiKey = prefs.firebaseApiKey,
                    collectionName = "galleries_and_success"
                )
                _galleryFeed.value = gallery.sortedByDescending { it.getStr("timestamp") }

                Log.d("AdminViewModel", "Synced all Firestore collections successfully!")
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Failed to sync Firestore", e)
                showFeedback("❌ Error synchronizing remote database.")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Uploads chosen image stream to ImgBB and triggers callback with direct URL.
     */
    fun uploadLocalImage(uri: Uri, callback: (String) -> Unit) {
        viewModelScope.launch {
            _isImageUploading.value = true
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val directUrl = AdminNetwork.uploadToImgBB(
                        imgbbApiKey = prefs.imgbbApiKey,
                        inputStream = inputStream
                    )
                    if (directUrl != null) {
                        callback(directUrl)
                        showFeedback("📸 Image uploaded successfully to ImgBB!")
                    } else {
                        showFeedback("❌ Image hosting on ImgBB failed.")
                    }
                } else {
                    showFeedback("❌ Could not read image data stream.")
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error uploading layout", e)
                showFeedback("❌ Image upload internal error.")
            } finally {
                _isImageUploading.value = false
            }
        }
    }

    /**
     * Publishes a news or special offer to the global feed.
     */
    fun publishNews(
        title: String,
        shortDesc: String,
        longDesc: String,
        price: String,
        dateTimeString: String,
        imageUrl: String
    ) {
        if (title.isBlank() || shortDesc.isBlank()) {
            showFeedback("⚠️ Offer Title & Description are mandatory!")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            val fullText = if (longDesc.isNotBlank()) "$shortDesc\n\n$longDesc" else shortDesc
            
            val fields = mapOf(
                "title" to title.trim(),
                "description" to fullText.trim(),
                "text" to fullText.trim(), // Support dual naming keys
                "imageUrl" to imageUrl.trim(),
                "image" to imageUrl.trim(), // Support dual naming keys
                "price" to price.trim(),
                "dateTime" to dateTimeString.trim(),
                "timestamp" to System.currentTimeMillis().toString()
            )

            val ok = AdminNetwork.createFirestoreDocument(
                projectId = prefs.firebaseProjectId,
                apiKey = prefs.firebaseApiKey,
                collectionName = "news",
                fields = fields
            )

            _isSyncing.value = false
            if (ok) {
                showFeedback("🎉 Special offer news published to student home screen as G-Sheet and Web compatible item!")
                refreshAllData()
            } else {
                showFeedback("❌ Failed to save news to Firestore.")
            }
        }
    }

    /**
     * Deletes a news item from Firestore.
     */
    fun removeNews(docId: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val ok = AdminNetwork.deleteFirestoreDocument(
                projectId = prefs.firebaseProjectId,
                apiKey = prefs.firebaseApiKey,
                collectionName = "news",
                documentId = docId
            )
            _isSyncing.value = false
            if (ok) {
                showFeedback("🗑️ News item deleted successfully!")
                refreshAllData()
            } else {
                showFeedback("❌ Deletion failed.")
            }
        }
    }

    /**
     * Publishes a student success gallery item up to 10,000 characters description.
     */
    fun publishGallerySuccess(
        imageUrl: String,
        desc: String,
        title: String = "Student Success Story"
    ) {
        if (imageUrl.isBlank() || desc.isBlank()) {
            showFeedback("⚠️ Standard Image URL and Description words are required!")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            
            // Limit to exactly 10,000 characters
            val cleanDesc = if (desc.length > 10000) desc.substring(0, 10000) else desc

            val fields = mapOf(
                "imageUrl" to imageUrl.trim(),
                "image" to imageUrl.trim(), // Dual support
                "description" to cleanDesc.trim(),
                "title" to title.trim(),
                "timestamp" to System.currentTimeMillis().toString()
            )

            val ok = AdminNetwork.createFirestoreDocument(
                projectId = prefs.firebaseProjectId,
                apiKey = prefs.firebaseApiKey,
                collectionName = "galleries_and_success",
                fields = fields
            )

            _isSyncing.value = false
            if (ok) {
                showFeedback("🏆 Success story added with ${cleanDesc.length} characters description!")
                refreshAllData()
            } else {
                showFeedback("❌ Couldn't write stories metadata.")
            }
        }
    }

    /**
     * Deletes a gallery success item from Firestore.
     */
    fun removeGalleryItem(docId: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val ok = AdminNetwork.deleteFirestoreDocument(
                projectId = prefs.firebaseProjectId,
                apiKey = prefs.firebaseApiKey,
                collectionName = "galleries_and_success",
                documentId = docId
            )
            _isSyncing.value = false
            if (ok) {
                showFeedback("🗑️ Student gallery item removed.")
                refreshAllData()
            } else {
                showFeedback("❌ Deletion failed.")
            }
        }
    }

    /**
     * Deletes an enquiry ticket from Firestore.
     */
    fun removeEnquiry(docId: String, collectionName: String = "enquiries") {
        viewModelScope.launch {
            _isSyncing.value = true
            
            // Try enquiries first, if fails try "enquiry"
            var ok = AdminNetwork.deleteFirestoreDocument(
                projectId = prefs.firebaseProjectId,
                apiKey = prefs.firebaseApiKey,
                collectionName = "enquiries",
                documentId = docId
            )
            if (!ok) {
                ok = AdminNetwork.deleteFirestoreDocument(
                    projectId = prefs.firebaseProjectId,
                    apiKey = prefs.firebaseApiKey,
                    collectionName = "enquiry",
                    documentId = docId
                )
            }

            _isSyncing.value = false
            if (ok) {
                showFeedback("🗑️ Student enquiry ticket solved and removed!")
                refreshAllData()
            } else {
                showFeedback("❌ Could not delete the request.")
            }
        }
    }
}
