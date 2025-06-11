package com.aigroup.aigroupmobile.utils.system

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.aigroup.aigroupmobile.utils.common.FileUtils

object VideoUtils {

  // TODO: 适配 suspend
  suspend fun getFrame(context: Context, uri: Uri): Bitmap? {
    val file = FileUtils.getFile(context, uri)
    require(file != null && file.exists()) { "File not found" }

    val retriever = MediaMetadataRetriever().also {
      it.setDataSource(file.absolutePath)
    }

    return retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
  }

}