package com.aigroup.aigroupmobile.utils.system

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.GenericMediaItem
import com.aigroup.aigroupmobile.data.models.ImageMediaItem
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.VideoMediaItem
import com.aigroup.aigroupmobile.utils.common.FileUtils
import com.aigroup.aigroupmobile.utils.common.filename
import com.aigroup.aigroupmobile.utils.network.createHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.BufferedOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlin.reflect.KClass

private const val AUTHORITY = "com.aigroup.aigroupmobile.fileprovider"

private fun uuidFileName(old: String): String {
  val uuid = UUID.randomUUID().toString()
  val name = old.substringBeforeLast('.')
  if (name == old) {
    return "$name-$uuid"
  } else {
    val ext = old.substringAfterLast('.')
    return "$name-$uuid.$ext"
  }
}


class PathManager @Inject constructor(private val context: Context) {

  data class StorageCopyResponse(
    val storageUri: Uri,
    val originalUri: Uri,
    val fileName: String,
    val mimeType: String
  )

  companion object {
    private const val TAG = "PathManager"
  }

  // see filepaths.xml
  private fun getBaseTempMediaContentFile(): File {
    val file = File(context.cacheDir, "medias")
    if (!file.exists()) {
      file.mkdirs()
    }
    return file
  }

  private fun getGenericCacheDirectory(): File {
    val file = File(context.cacheDir, "others")
    if (!file.exists()) {
      file.mkdirs()
    }
    return file
  }

  @Suppress("UNREACHABLE_CODE")
  private fun <T : MediaItem> createTempContentMediaFile(cls: KClass<T>): File {
    val instant = Clock.System.now().toEpochMilliseconds()
    return when (cls) {
      ImageMediaItem::class -> {
        return File.createTempFile("image_$instant", ".jpg", getBaseTempMediaContentFile())
      }

      VideoMediaItem::class -> {
        return File.createTempFile("video_$instant", ".mp4", getBaseTempMediaContentFile())
      }

      DocumentMediaItem::class -> {
        return File.createTempFile("doc_$instant", ".bin", getBaseTempMediaContentFile())
      }

      GenericMediaItem::class -> {
        return File.createTempFile("other_$instant", ".bin", getBaseTempMediaContentFile())
      }

      else -> {
        throw IllegalArgumentException("Unsupported MediaItem type: $cls")
      }
    }
  }

  fun <T : MediaItem> createTempContentMediaUri(cls: KClass<T>): Uri {
    val file = createTempContentMediaFile(cls)
    return FileProvider.getUriForFile(context, AUTHORITY, file)
  }

  private fun <T : MediaItem> getMediaDirectory(cls: KClass<T>): File {
    val file = when (cls) {
      ImageMediaItem::class -> {
        File(context.filesDir, "images")
      }

      VideoMediaItem::class -> {
        File(context.filesDir, "videos")
      }

      DocumentMediaItem::class -> {
        File(context.filesDir, "docs")
      }

      GenericMediaItem::class -> {
        File(context.filesDir, "others")
      }

      else -> {
        throw IllegalArgumentException("Unsupported MediaItem type: $cls")
      }
    }
    if (!file.exists()) {
      file.mkdirs()
    }
    return file
  }

  suspend fun saveBitmapToStorage(bitmap: Bitmap, baseFilename: String): Uri {
    val filename = uuidFileName(baseFilename) + ".png"
    val target = File(getMediaDirectory(ImageMediaItem::class), filename)
    withContext(Dispatchers.IO) {
      target.outputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
      }
    }
    return target.toUri()
  }

  suspend fun saveResDrawableToStorage(@DrawableRes resId: Int, baseFilename: String): Uri {
    val filename = uuidFileName(baseFilename) + ".png"
    val target = File(getMediaDirectory(ImageMediaItem::class), filename)
    withContext(Dispatchers.IO) {
      val inputStream = context.resources.openRawResource(resId)
      target.outputStream().use { output ->
        inputStream.copyTo(output)
      }
    }
    return target.toUri()
  }

  @Deprecated("Use copyContentMediaToStorage instead")
  suspend fun <T : MediaItem> legacyCopyContentMediaToStorage(uri: Uri, cls: KClass<T>): Uri {
    val last = try {
      val path = FileUtils.getPath(context, uri)
      Uri.parse(path).filename()
    } catch (e: Exception) {
      Log.e(TAG, "copyContentMediaToStorage: error when get path from uri", e)
      uri.filename()
    }

    require(last != null) { "Invalid uri: $uri" }
    val filename = uuidFileName(last)

    val target = File(getMediaDirectory(cls), filename)
    // TODO: log real path if success
    Log.i(TAG, "copyContentMediaToStorage: $uri -> $target")

    withContext(Dispatchers.IO) {
      context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output ->
          input.copyTo(output)
        }
      }
    }

    return target.toUri()
  }

  // TODO: using data structure
  suspend fun <T : MediaItem> copyContentMediaToStorage(uri: Uri, cls: KClass<T>): Pair<Uri, String> {
    val last = try {
      val path = FileUtils.getPath(context, uri)
      Uri.parse(path).filename()
    } catch (e: Exception) {
      Log.w(TAG, "copyContentMediaToStorage: error when get path from uri", e)
      uri.filename()
    }

    require(last != null) { "Invalid uri: $uri" }
    val filename = uuidFileName(last)

    val target = File(getMediaDirectory(cls), filename)
    // TODO: log real path if success
    Log.i(TAG, "copyContentMediaToStorage: $uri -> $target")

    withContext(Dispatchers.IO) {
      context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output ->
          input.copyTo(output)
        }
      }
    }

    return target.toUri() to last
  }

  /**
   * @return Pair of the new Uri and the mimeType of the media
   */
  @Deprecated("Use copyContentMediaToStorage instead")
  suspend fun legacyCopyContentMediaToStorage(uri: Uri): Pair<Uri, String> {
    val mimeType = context.contentResolver.getType(uri)
    val cls = when {
      mimeType?.startsWith("image/", true) == true -> ImageMediaItem::class
      mimeType?.startsWith("video/", true) == true -> VideoMediaItem::class
      mimeType in DocumentMediaItem.DocType.entries.map { it.mimeType } -> DocumentMediaItem::class
      else -> GenericMediaItem::class
    }
    val storageUri = legacyCopyContentMediaToStorage(uri, cls) // TODO: refactor it
    return storageUri to (mimeType ?: "application/octet-stream") // TODO: constant it
  }

  suspend fun copyContentMediaToStorage(uri: Uri): StorageCopyResponse {
    val mimeType = context.contentResolver.getType(uri)
    val cls = when {
      mimeType?.startsWith("image/", true) == true -> ImageMediaItem::class
      mimeType?.startsWith("video/", true) == true -> VideoMediaItem::class
      mimeType in DocumentMediaItem.DocType.entries.map { it.mimeType } -> DocumentMediaItem::class
      else -> GenericMediaItem::class
    }
    val (storageUri, filename) = copyContentMediaToStorage(uri, cls)

    return StorageCopyResponse(
      storageUri,
      uri,
      filename,
      mimeType ?: "application/octet-stream" // TODO: using constants
    )
  }

  suspend fun downloadMediaToStorage(url: Url): Pair<Uri, String> {
    // TODO: using ktor client as global singleton
    val ktor = createHttpClient()
    val response = ktor.get {
      url(url)
      method = HttpMethod.Get
    }

    // get mime type
    val mimeType = response.headers[HttpHeaders.ContentType] ?: "unknown"
    val cls = when {
      mimeType.startsWith("image/", true) -> ImageMediaItem::class
      mimeType.startsWith("video/", true) -> VideoMediaItem::class
      mimeType in DocumentMediaItem.DocType.entries.map { it.mimeType } -> DocumentMediaItem::class
      else -> GenericMediaItem::class
    }

    val target =
      File(getMediaDirectory(cls), uuidFileName(url.pathSegments.lastOrNull() ?: "no-name"))
    response.bodyAsChannel().copyAndClose(target.writeChannel())

    return target.toUri() to mimeType
  }

  suspend fun copyFileToTemp(file: File, fileName: String? = null): Uri {
    val target = withContext(Dispatchers.IO) {
      val target = File(getGenericCacheDirectory(), fileName ?: file.name)
      file.copyTo(target, overwrite = true)
    }
    val uri = FileProvider.getUriForFile(context, AUTHORITY, target)
    Log.i(TAG, "Copied file to temp: $uri")
    return uri
  }

  suspend fun copyFileToTemp(path: String, fileName: String? = null): Uri {
    return copyFileToTemp(File(path), fileName)
  }

  suspend fun zipUserDataDirectory(): Uri {
    return withContext(Dispatchers.IO) {
      val dir = context.filesDir
      val target = File.createTempFile(
        uuidFileName("backup"),
        ".zip",
        getGenericCacheDirectory()
      )

      ZipOutputStream(BufferedOutputStream(target.outputStream())).use { zos ->
        dir.walkTopDown().forEach { file ->
          if (file.isFile) {
            // TODO: 完善 zip 功能和恢复
            val entry = ZipEntry(file.absolutePath)
            zos.putNextEntry(entry)
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
          }
        }
      }

      return@withContext FileProvider.getUriForFile(context, AUTHORITY, target)
    }
  }

  // TODO: 合并以下两个方法

  suspend fun saveImageToGallery(uri: Uri): Uri? {
    return withContext(Dispatchers.IO) {
      val fileName = uri.lastPathSegment ?: "unknown"
      val resolver = context.contentResolver
      val outputFile = OutputFileTaker()

//      val mime = resolver.getType(uri)
//      if (mime == null) {
//        Log.w(TAG, "saveImageToGallery: error: mime == null")
//        return@withContext null
//      }
      val mime = "image/jpeg" // TODO: correct mime type

      // 插入媒体信息
      val imageUri = resolver.insertMediaImage(fileName, mime, outputFileTaker = outputFile)
      if (imageUri == null) {
        Log.w(TAG, "insert: error: uri == null")
        return@withContext null
      }

      resolver.openOutputStream(imageUri).use {
        resolver.openInputStream(uri)?.use { input ->
          input.copyTo(it!!)
        }
      }
      imageUri.finishMediaStoreImagePending(context, resolver, outputFile.file)

      return@withContext imageUri
    }
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  suspend fun saveVideoToGallery(uri: Uri): Uri? {
    return withContext(Dispatchers.IO) {
      val fileName = uri.lastPathSegment ?: "unknown"
      val resolver = context.contentResolver

      val mime = "video/mp4" // TODO: correct mime type

      // 插入媒体信息
      val videoUri = resolver.insertMediaVideo(fileName, mime)
      if (videoUri == null) {
        Log.w(TAG, "insert: error: uri == null")
        return@withContext null
      }

      resolver.openOutputStream(videoUri).use {
        resolver.openInputStream(uri)?.use { input ->
          input.copyTo(it!!)
        }
      }
      videoUri.finishMediaStoreVideoPending(resolver)

      return@withContext videoUri
    }
  }

  suspend fun writeDocsToStorage(name: String, content: ByteArray): Uri {
    val target = File(getMediaDirectory(DocumentMediaItem::class), uuidFileName(name))
    withContext(Dispatchers.IO) {
      target.outputStream().use { output ->
        output.write(content)
      }
    }
    return target.toUri()
  }

  suspend fun writeDocsToStorage(name: String, content: String): Uri {
    return writeDocsToStorage(name, content.toByteArray())
  }

  // share
  suspend fun linkStorageToShare(uri: Uri): Uri {
    val file = File(uri.path ?: error("Invalid uri: $uri"))
    if (!file.exists()) {
      Log.w(TAG, "linkStorageToShare: file not exists: $file")
      return uri
    }
    if (!file.isFile) {
      Log.w(TAG, "linkStorageToShare: not a file: $file")
      return uri
    }

    val target = File(context.cacheDir, "share")
    if (!target.exists()) {
      target.mkdirs()
    }
    val targetFile = File(target, uri.filename()!!)
    if (targetFile.exists()) {
      targetFile.delete()
    }

    // TODO: try link instead of copy and dont copy same file multiple times
    withContext(Dispatchers.IO) {
      file.inputStream().use { input ->
        targetFile.outputStream().use { output ->
          input.copyTo(output)
        }
      }
    }

    return FileProvider.getUriForFile(context, AUTHORITY, targetFile)
  }

}