package com.example.admin

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.ByteArrayOutputStream

/**
 * Robust networking services for Soham Tally Academy Admin Panel.
 * Uses Firestore REST API, Telegram SendMessage API, and ImgBB Upload API.
 */
object AdminNetwork {

    private val client = OkHttpClient()
    private const val TAG = "AdminNetwork"

    /**
     * Sends a 6-digit OTP code to the specified Telegram Bot Chat ID.
     */
    suspend fun sendTelegramOtp(
        botToken: String,
        chatId: String,
        otp: String,
        timestampStr: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val textMessage = """
                🔑 <b>Soham Tally Academy Admin OTP</b>
                
                Your 6-digit administrative verification OTP:
                <code>$otp</code>
                
                🕒 Generated at: <b>$timestampStr</b>
                ⌛ This code is valid for <b>5 minutes</b>.
                ⚠️ Do not share this OTP with anyone.
            """.trimIndent()

            val url = "https://api.telegram.org/bot$botToken/sendMessage"
            val jsonBody = JSONObject().apply {
                put("chat_id", chatId)
                put("text", textMessage)
                put("parse_mode", "HTML")
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string()
                Log.d(TAG, "Telegram response: $responseStr")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending Telegram OTP", e)
            return@withContext false
        }
    }

    /**
     * Uploads any selected image (obtained via uri stream in Android) to ImgBB.
     * Converts to Base64 and returns the direct JPG/PNG URL string.
     */
    suspend fun uploadToImgBB(
        imgbbApiKey: String,
        inputStream: InputStream
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Read bytes into Base64
            val byteBuffer = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var len: Int
            while (inputStream.read(buffer).also { len = it } != -1) {
                byteBuffer.write(buffer, 0, len)
            }
            val imageBytes = byteBuffer.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            val url = "https://api.imgbb.com/1/upload?key=$imgbbApiKey"

            // Construct multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", base64Image)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: return@withContext null
                Log.d(TAG, "ImgBB response obtained")
                if (response.isSuccessful) {
                    val json = JSONObject(responseStr)
                    val dataObj = json.getJSONObject("data")
                    return@withContext dataObj.getString("url")
                } else {
                    Log.e(TAG, "ImgBB unsuccessful: $responseStr")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image to ImgBB", e)
        }
        return@withContext null
    }

    /**
     * General function to fetch documents from a Firestore collection.
     */
    suspend fun fetchFirestoreCollection(
        projectId: String,
        apiKey: String,
        collectionName: String
    ): List<FirestoreDoc> = withContext(Dispatchers.IO) {
        val documentsList = mutableListOf<FirestoreDoc>()
        try {
            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collectionName?key=$apiKey"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: return@withContext emptyList()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Fetch unsuccessful for $collectionName: $responseStr")
                    return@withContext emptyList()
                }

                val responseJson = JSONObject(responseStr)
                if (responseJson.has("documents")) {
                    val docsArr = responseJson.getJSONArray("documents")
                    for (i in 0 until docsArr.length()) {
                        val docObj = docsArr.getJSONObject(i)
                        
                        // Parse document ID
                        val fullPathName = docObj.getString("name") // e.g. "projects/../documents/news/ID"
                        val docId = fullPathName.substringAfterLast("/")

                        // Parse fields
                        val fieldsMap = mutableMapOf<String, String>()
                        if (docObj.has("fields")) {
                            val fieldsJson = docObj.getJSONObject("fields")
                            val keys = fieldsJson.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val valObj = fieldsJson.getJSONObject(key)
                                var strVal = ""
                                if (valObj.has("stringValue")) {
                                    strVal = valObj.getString("stringValue")
                                } else if (valObj.has("integerValue")) {
                                    strVal = valObj.getString("integerValue")
                                } else if (valObj.has("doubleValue")) {
                                    strVal = valObj.getString("doubleValue").toString()
                                } else if (valObj.has("booleanValue")) {
                                    strVal = valObj.getBoolean("booleanValue").toString()
                                }
                                fieldsMap[key] = strVal
                            }
                        }
                        documentsList.add(FirestoreDoc(docId, fieldsMap))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching collection $collectionName", e)
        }
        return@withContext documentsList
    }

    /**
     * Publishes a new document to Firestore under key-value pairs formatted as stringValues.
     */
    suspend fun createFirestoreDocument(
        projectId: String,
        apiKey: String,
        collectionName: String,
        fields: Map<String, String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collectionName?key=$apiKey"
            
            val jsonRoot = JSONObject()
            val fieldsJson = JSONObject()
            for ((k, v) in fields) {
                val typeWrapper = JSONObject().apply {
                    put("stringValue", v)
                }
                fieldsJson.put(k, typeWrapper)
            }
            jsonRoot.put("fields", fieldsJson)

            val requestBody = jsonRoot.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string()
                Log.d(TAG, "Create document response: $responseStr")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating firestore document", e)
            return@withContext false
        }
    }

    /**
     * Deletes a document from a Firestore collection given its document ID.
     */
    suspend fun deleteFirestoreDocument(
        projectId: String,
        apiKey: String,
        collectionName: String,
        documentId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collectionName/$documentId?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string()
                Log.d(TAG, "Delete document response: $responseStr")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting firestore document", e)
            return@withContext false
        }
    }
}

/**
 * Data representation for Firestore documents retrieved via REST API.
 */
data class FirestoreDoc(
    val id: String,
    val fields: Map<String, String>
) {
    fun getStr(key: String, default: String = ""): String {
        return fields[key] ?: default
    }
}
