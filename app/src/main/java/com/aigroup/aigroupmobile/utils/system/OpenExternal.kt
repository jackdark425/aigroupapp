package com.aigroup.aigroupmobile.utils.system

import android.content.Context
import android.content.Intent
import android.util.Log
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem

object OpenExternal {

  const val TAG = "OpenExternal"

  suspend fun openDocMediaItemExternal(context: Context, media: DocumentMediaItem) {
    val uri = media.uri
    val mime = media.mimeType

    // TODO: avoid this and using path manager by Injector
    val shareUri = PathManager(context).linkStorageToShare(uri)
    Log.i(TAG, "openDocMediaItemExternal share other app with: $shareUri")

    val intent = Intent(Intent.ACTION_VIEW)
    intent.action = Intent.ACTION_VIEW
    intent.setDataAndType(shareUri, mime)
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    context.startActivity(intent)
  }

}