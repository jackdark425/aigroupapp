package com.aigroup.aigroupmobile.connect.utils

import android.util.Log
import com.aigroup.aigroupmobile.utils.network.createHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.quote
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileInputStream

@Serializable
private data class UploadInfo(
  val policy: String,
  val signature: String,
  @SerialName("upload_dir")
  val uploadDir: String,
  @SerialName("upload_host")
  val uploadHost: String,
  @SerialName("expire_in_seconds")
  val expireInSeconds: Int,
  @SerialName("max_file_size_mb")
  val maxFileSizeMb: Int,
  @SerialName("capacity_limit_mb")
  val capacityLimitMb: Int,
  @SerialName("oss_access_key_id")
  val ossAccessKeyId: String,
  @SerialName("x_oss_object_acl")
  val xOssObjectAcl: String,
  @SerialName("x_oss_forbid_overwrite")
  val xOssForbidOverwrite: String
)

@Serializable
private data class UploadResponse(
  @SerialName("request_id")
  val requestId: String,
  val data: UploadInfo
)


object DashScopeUploadUtil {

  private const val TAG = "DashScopeUploadUtil"

  private val client = createHttpClient()

  suspend fun uploadLocalFile(path: String, model: String, apiKey: String): String {
    Log.i(TAG, "Uploading file: $path ($model) ($apiKey)")

    // check if file exists
    val file = File(path)
    if (!file.exists() || !file.isFile) {
      throw Exception("File does not exist")
    }

    val contents = withContext(Dispatchers.IO) {
      FileInputStream(file).use { it.readBytes() }
    }

    val uploadInfo = getUploadInfo(model, apiKey)

    val key = uploadInfo.uploadDir + "/" + file.name
    val data = mapOf(
      "OSSAccessKeyId" to uploadInfo.ossAccessKeyId,
      "Signature" to uploadInfo.signature,
      "policy" to uploadInfo.policy,
      "key" to key,
      "x-oss-object-acl" to uploadInfo.xOssObjectAcl,
      "x-oss-forbid-overwrite" to uploadInfo.xOssForbidOverwrite,
      "success_action_status" to "200",
      "x-oss-content-type" to "video/mp4"
    )
    val url = uploadInfo.uploadHost

    val response = client.submitFormWithBinaryData(
      url = url,
      formData = formData {
        data.forEach { (key, value) ->
          append(key.quote(), value)
        }
        append(
          "file".quote(),
          InputProvider(file.length()) { file.inputStream().asInput() },
          Headers.build {
            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
            append(HttpHeaders.ContentType, "video/mp4")
          }
        )
      }
    ) {
      header(HttpHeaders.UserAgent, userAgent)
      header(HttpHeaders.Accept, "application/json")
      header(HttpHeaders.Date, formatDateTime(System.currentTimeMillis()))
    }

    if (response.status == HttpStatusCode.OK) {
      return "oss://$key"
    } else {
      throw Exception("Failed to upload file")
    }
  }

  private suspend fun getUploadInfo(model: String, apiKey: String): UploadInfo {
    // TODO: using constants
    val baseUrl = "https://dashscope.aliyuncs.com/api/v1"
    val uploadInfoUrl = "$baseUrl/uploads"

    val params = mapOf(
      "action" to "getPolicy",
      "model" to model
    )

    val response = client.get(uploadInfoUrl) {
      headers {
        defaultHeaders(apiKey).forEach { (key, value) ->
          append(key, value)
        }
      }
      params.forEach { (key, value) ->
        parameter(key, value)
      }
    }

    if (response.status == HttpStatusCode.OK) {
      val info = response.body<UploadResponse>()
      return info.data
    } else {
      throw Exception("Failed to get upload info")
    }
  }

  private val userAgent = "dashscope/1.20.8; python/3.12.5; platform/macOS-14.1-x86_64-i386-64bit; processor/i386"

  private fun defaultHeaders(apiKey: String): Map<String, String> {
    val headers = mutableMapOf(
      HttpHeaders.UserAgent to userAgent,
      HttpHeaders.Authorization to "Bearer $apiKey",
      HttpHeaders.Accept to "application/json"
    )
    return headers
  }

  private fun formatDateTime(timeMillis: Long): String {
    val date = java.util.Date(timeMillis)
    val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US)
    format.timeZone = java.util.TimeZone.getTimeZone("GMT")
    return format.format(date)
  }
}