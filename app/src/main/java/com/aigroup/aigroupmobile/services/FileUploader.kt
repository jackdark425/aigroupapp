package com.aigroup.aigroupmobile.services

import android.util.Log
import androidx.datastore.core.DataStore
import com.aallam.openai.api.core.RequestOptions
import com.aigroup.aigroupmobile.connect.utils.DashScopeUploadUtil
import com.aigroup.aigroupmobile.data.models.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 负责与 AI 交互过程中以及文件管理中的文件上传
 */
class FileUploader @Inject constructor(
  private val dataStore: DataStore<AppPreferences>
) {

  companion object {
    private const val TAG = "FileUploader"
    const val VIDEO_MODEL = "qwen-vl-max-0809"
  }

  private val preferences = dataStore.data

  private suspend fun uploadFileToDashscope(path: String) = withContext(Dispatchers.IO) {
    // NOTE: must ensure vlm key exists in UX
    // TODO: not only dashscope
    val videoApiKey = preferences.map { it.token.dashscope }.first()
    val videoOnlineLink = DashScopeUploadUtil.uploadLocalFile(
      path,
      VIDEO_MODEL,
      videoApiKey
    )
    Log.i(TAG, "Video uploaded: $videoOnlineLink")

    val aiRequestOptions = RequestOptions(
      headers = mapOf("X-DashScope-OssResourceResolve" to "enable")
    )

    return@withContext videoOnlineLink to aiRequestOptions
  }

  suspend fun uploadFile(path: String): Pair<String, RequestOptions> {
    return uploadFileToDashscope(path)
  }

  fun getBaseRequestOptions(): RequestOptions {
    return RequestOptions(
      headers = mapOf("X-DashScope-OssResourceResolve" to "enable")
    )
  }

}