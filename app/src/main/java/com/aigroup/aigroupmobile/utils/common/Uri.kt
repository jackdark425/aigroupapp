package com.aigroup.aigroupmobile.utils.common

import android.net.Uri

/**
 * Get the filename from a Uri. It's useful for partially encoded URLs.
 */
fun Uri.filename(): String? {
  if (lastPathSegment == null) {
    return null
  }
  // handle url encoded characters
  return Uri.decode(toString()).substringAfterLast("/") // TODO: more delimiter?
}