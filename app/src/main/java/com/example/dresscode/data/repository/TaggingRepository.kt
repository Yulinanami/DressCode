package com.example.dresscode.data.repository

import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class TaggingResult(
    val originalName: String,
    val suggestedName: String?,
    val tagsJson: String
)

class TaggingRepository @Inject constructor(
    private val client: OkHttpClient,
    private val baseUrl: String
) {

    suspend fun uploadForTags(fileName: String, bytes: ByteArray): TaggingResult {
        val contentType = "image/*".toMediaTypeOrNull()
        val requestBody = bytes.toRequestBody(contentType)
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, requestBody)
            .build()
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/tag-and-suggest-name")
            .post(multipartBody)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("打标签失败：HTTP ${response.code}")
                }
                val payload = response.body?.string() ?: throw IOException("响应为空")
                val json = JSONObject(payload)
                val tagsObject = json.optJSONObject("tags")
                val tagsJson = tagsObject?.toString(2)
                    ?: json.opt("tags")?.toString()
                    ?: "{}"
                val originalName = json.optString("original_filename")
                    .ifBlank { json.optString("filename", fileName) }
                    .ifBlank { fileName }
                val suggestedName = json.optString("suggested_name").takeIf { it.isNotBlank() }
                TaggingResult(
                    originalName = originalName,
                    suggestedName = suggestedName,
                    tagsJson = tagsJson
                )
            }
        }
    }
}
