package com.aigroup.aigroupmobile.utils.system

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File

private const val TAG = "MediaStoreUtils"

// 保存位置，这里使用 Picures，也可以改为 DCIM
private val ALBUM_DIR = Environment.DIRECTORY_PICTURES

/**
 * 用于Q以下系统获取图片文件大小来更新 [MediaStore.Images.Media.SIZE]
 */
internal class OutputFileTaker(var file: File? = null)

/**
 * 插入视频到媒体库
 * TODO: 合并方法
 */
// only android q and above
@RequiresApi(Build.VERSION_CODES.Q)
internal fun ContentResolver.insertMediaVideo(
  fileName: String,
  mimeType: String?,
  relativePath: String? = null,
): Uri? {
  // 视频信息
  val videoValues = ContentValues().apply {
    if (mimeType != null) {
      put(MediaStore.Video.Media.MIME_TYPE, mimeType)
    }
    // 插入时间
    val date = System.currentTimeMillis() / 1000
    put(MediaStore.Video.Media.DATE_ADDED, date)
    put(MediaStore.Video.Media.DATE_MODIFIED, date)
  }
  // 保存的位置
  val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
  // Android 10 及其以上
  val path = if (relativePath != null) "$ALBUM_DIR/${relativePath}" else ALBUM_DIR
  videoValues.apply {
    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
    put(MediaStore.Video.Media.RELATIVE_PATH, path)
    put(MediaStore.Video.Media.IS_PENDING, 1)
  }
  // 插入视频信息
  return this.insert(collection, videoValues)
}


/**
 * 插入图片到媒体库
 */
internal fun ContentResolver.insertMediaImage(
  fileName: String,
  mimeType: String?,
  relativePath: String? = null,
  outputFileTaker: OutputFileTaker? = null
): Uri? {
  // 图片信息
  val imageValues = ContentValues().apply {
    if (mimeType != null) {
      put(MediaStore.Images.Media.MIME_TYPE, mimeType)
    }
    // 插入时间
    val date = System.currentTimeMillis() / 1000
    put(MediaStore.Images.Media.DATE_ADDED, date)
    put(MediaStore.Images.Media.DATE_MODIFIED, date)
  }
  // 保存的位置
  val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    // Android 10 及其以上
    val path = if (relativePath != null) "$ALBUM_DIR/${relativePath}" else ALBUM_DIR
    imageValues.apply {
      put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
      put(MediaStore.Images.Media.RELATIVE_PATH, path)
      put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    // 高版本不用查重直接插入，会自动重命名
  } else {
    // Android 9 及其以下
    val pictures = Environment.getExternalStoragePublicDirectory(ALBUM_DIR)
    val saveDir = if (relativePath != null) File(pictures, relativePath) else pictures

    if (!saveDir.exists() && !saveDir.mkdirs()) {
      Log.e(TAG, "save: error: can't create Pictures directory")
      return null
    }

    // 文件路径查重，重复的话在文件名后拼接数字
    var imageFile = File(saveDir, fileName)
    val fileNameWithoutExtension = imageFile.nameWithoutExtension
    val fileExtension = imageFile.extension

    // 查询文件是否已经存在
    var queryUri = this.queryMediaImage28(imageFile.absolutePath)
    var suffix = 1
    while (queryUri != null) {
      // 存在的话重命名，路径后面拼接 fileNameWithoutExtension(数字).png
      val newName = fileNameWithoutExtension + "(${suffix++})." + fileExtension
      imageFile = File(saveDir, newName)
      queryUri = this.queryMediaImage28(imageFile.absolutePath)
    }

    imageValues.apply {
      put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.name)
      // 保存路径
      val imagePath = imageFile.absolutePath
      Log.i(TAG, "save file: $imagePath")
      put(MediaStore.Images.Media.DATA, imagePath)
    }
    outputFileTaker?.file = imageFile// 回传文件路径，用于设置文件大小
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
  }
  // 插入图片信息
  return this.insert(collection, imageValues)
}

/**
 * Android Q以下版本，查询媒体库中当前路径是否存在
 * @return Uri 返回null时说明不存在，可以进行图片插入逻辑
 */
private fun ContentResolver.queryMediaImage28(imagePath: String): Uri? {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null

  val imageFile = File(imagePath)
  if (imageFile.canRead() && imageFile.exists()) {
    Log.v(TAG, "query: path: $imagePath exists")
    // 文件已存在，返回一个file://xxx的uri
    // 这个逻辑也可以不要，但是为了减少媒体库查询次数，可以直接判断文件是否存在
    return Uri.fromFile(imageFile)
  }
  // 保存的位置
  val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

  // 查询是否已经存在相同图片
  val query = this.query(
    collection,
    arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA),
    "${MediaStore.Images.Media.DATA} == ?",
    arrayOf(imagePath), null
  )
  query?.use {
    while (it.moveToNext()) {
      val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
      val id = it.getLong(idColumn)
      val existsUri = ContentUris.withAppendedId(collection, id)
      Log.v(TAG, "query: path: $imagePath exists uri: $existsUri")
      return existsUri
    }
  }
  return null
}

// TODO: 合并以下两个方法

@RequiresApi(Build.VERSION_CODES.Q)
internal fun Uri.finishMediaStoreVideoPending(
  resolver: ContentResolver,
) {
  val videoValues = ContentValues()
  videoValues.put(MediaStore.Video.Media.IS_PENDING, 0)
  resolver.update(this, videoValues, null, null)
}

internal fun Uri.finishMediaStoreImagePending(
  context: Context,
  resolver: ContentResolver,
  outputFile: File?
) {
  val imageValues = ContentValues()
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
    if (outputFile != null) {
      // Android 10 以下需要更新文件大小字段，否则部分设备的图库里照片大小显示为0
      imageValues.put(MediaStore.Images.Media.SIZE, outputFile.length())
    }
    resolver.update(this, imageValues, null, null)
    // 通知媒体库更新，部分设备不更新 图库看不到 ？？？
    val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, this)
    context.sendBroadcast(intent)
  } else {
    // Android Q添加了IS_PENDING状态，为0时其他应用才可见
    imageValues.put(MediaStore.Images.Media.IS_PENDING, 0)
    resolver.update(this, imageValues, null, null)
  }
}
