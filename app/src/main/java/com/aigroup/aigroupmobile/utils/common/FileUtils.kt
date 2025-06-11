package com.aigroup.aigroupmobile.utils.common

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID

object FileUtils {
    private const val DOCUMENTS_DIR = "documents"

    private const val AUTHORITY = "YOUR_AUTHORITY.provider" // TODO: fix

    /**
     * TAG for log messages.
     */
    private const val TAG = "FileUtils"
    private const val DEBUG = false // Set to true to enable logging

    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     *
     * @param uri
     * @return Extension including the dot("."); "" if there is no extension;
     * null if uri was null.
     */
    private fun getExtension(uri: String?): String? {
        if (uri == null) {
            return null
        }
        val dot = uri.lastIndexOf(".")
        return if (dot >= 0) {
            uri.substring(dot)
        } else {
            // No extension.
            ""
        }
    }

    /**
     * @return Whether the URI is a local one.
     */
    private fun isLocal(url: String?): Boolean {
        return url != null && !url.startsWith("http://") && !url.startsWith("https://")
    }

    /**
     * @return True if Uri is a MediaStore Uri.
     * @author paulburke
     */
    fun isMediaUri(uri: Uri): Boolean {
        return "media".equals(uri.authority, ignoreCase = true)
    }

    /**
     * Convert File into Uri.
     *
     * @param file
     * @return uri
     */
    fun getUri(file: File?): Uri? {
        return if (file != null) Uri.fromFile(file) else null
    }

    /**
     * Returns the path only (without file name).
     *
     * @param file
     * @return
     */
    fun getPathWithoutFilename(file: File?): File? {
        return if (file != null) {
            if (file.isDirectory) {
                // no file to be split off. Return everything
                file
            } else {
                val filename = file.name
                val filepath = file.absolutePath

                // Construct path without file name.
                var pathwithoutname = filepath.substring(
                    0,
                    filepath.length - filename.length
                )
                if (pathwithoutname.endsWith("/")) {
                    pathwithoutname = pathwithoutname.substring(0, pathwithoutname.length - 1)
                }
                File(pathwithoutname)
            }
        } else null
    }

    /**
     * @return The MIME type for the given file.
     */
    private fun getMimeType(file: File): String? {
        val extension = getExtension(file.name)
        return if (extension!!.length > 0) MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            extension.substring(1)
        ) else "application/octet-stream"
    }

    /**
     * @return The MIME type for the give Uri.
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        val file = getPath(context, uri)?.let { File(it) }
        return file?.let { getMimeType(it) }
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is local.
     */
    private fun isLocalStorageDocument(uri: Uri): Boolean {
        return AUTHORITY == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    // TODO: using suspend in query? (https://developer.android.com/about/versions/14/changes/partial-photo-video-access?hl=zh-cn)
    private fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = MediaStore.Files.FileColumns.DATA
        val projection = arrayOf(
            column,
            MediaStore.Video.Media.DISPLAY_NAME
        )
        try {
            cursor = context.contentResolver.query(
                uri!!, projection, selection, selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                if (DEBUG) DatabaseUtils.dumpCursor(cursor)
                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br></br>
     * <br></br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @see .isLocal
     * @see .getFile
     */
    fun getPath(context: Context, uri: Uri): String? {
        if (DEBUG) Log.d(
            "$TAG File -",
            "Authority: " + uri.authority +
                    ", Fragment: " + uri.fragment +
                    ", Port: " + uri.port +
                    ", Query: " + uri.query +
                    ", Scheme: " + uri.scheme +
                    ", Host: " + uri.host +
                    ", Segments: " + uri.pathSegments.toString()
        )

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // LocalStorageProvider
            if (isLocalStorageDocument(uri)) {
                // The path is the id
                return DocumentsContract.getDocumentId(uri)
            } else if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                var id = DocumentsContract.getDocumentId(uri)
                if (id != null && id.startsWith("raw:")) {
                    return id.substring(4)
                }
                if (id != null && id.startsWith("msf:")) {
                    id = id.substring(4)
                }

                // TODO: msf uri search by following is not work
                val contentUriPrefixesToTry = arrayOf(
                    "content://downloads/public_downloads",
                    "content://downloads/my_downloads"
                )
                for (contentUriPrefix in contentUriPrefixesToTry) {
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse(contentUriPrefix),
                        java.lang.Long.valueOf(id)
                    )
                    try {
                        val path = getDataColumn(context, contentUri, null, null)
                        if (path != null) {
                            return path
                        }
                    } catch (_: Exception) {
                    }
                }

                val fileName = getFileName(context, uri)
                val cacheDir = getDocumentCacheDir(context)
                val file = generateFileName(fileName, cacheDir)
                var destinationPath: String? = null
                if (file != null) {
                    destinationPath = file.absolutePath
                    saveFileFromUri(context, uri, destinationPath)
                }
                return destinationPath
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                val contentUri: Uri? = when (type) {
                  "image" -> {
                      MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                  }
                  "video" -> {
                      MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                  }
                  "audio" -> {
                      MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                  }
                  else -> {
                      null
                  }
                }

                if (contentUri == null) {
                    val fileName = getFileName(context, uri)
                    val cacheDir = getDocumentCacheDir(context)
                    val file = generateFileName(fileName, cacheDir)
                    var destinationPath: String? = null
                    if (file != null) {
                        destinationPath = file.absolutePath
                        saveFileFromUri(context, uri, destinationPath)
                    }
                    return destinationPath
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(
                    split[1]
                )
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) {
                uri.lastPathSegment
            } else getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * Convert Uri into File, if possible.
     *
     * @return file A local file that the Uri was pointing to, or null if the
     * Uri is unsupported or pointed to a remote resource.
     * @author paulburke
     * @see .getPath
     */
    fun getFile(context: Context, uri: Uri?): File? {
        if (uri != null) {
            val path = getPath(context, uri)
            if (path != null && isLocal(path)) {
                return File(path)
            }
        }
        return null
    }

    fun getDocumentCacheDir(context: Context): File {
        val dir = File(context.cacheDir, DOCUMENTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        logDir(context.cacheDir)
        logDir(dir)
        return dir
    }

    private fun logDir(dir: File) {
        if (!DEBUG) return
        Log.d(TAG, "Dir=$dir")
        dir.listFiles()?.let { files ->
            for (file in files) {
                Log.d(TAG, "File=" + file.path)
            }
        }
    }

    private fun generateFileName(name: String?, directory: File): File? {
        name ?: return null

        val middle = UUID.randomUUID().toString()
        val middleDir = File(directory, middle)
        middleDir.mkdirs()

        val file = File(middleDir, name)
        try {
            if (!file.createNewFile()) {
                return null
            }
        } catch (e: IOException) {
            Log.w(TAG, e)
            return null
        }
        logDir(directory)
        return file
    }

    private fun saveFileFromUri(context: Context, uri: Uri, destinationPath: String?) {
        var `is`: InputStream? = null
        var bos: BufferedOutputStream? = null
        try {
            `is` = context.contentResolver.openInputStream(uri)
            bos = BufferedOutputStream(FileOutputStream(destinationPath, false))
            val buf = ByteArray(1024)
            `is`!!.read(buf)
            do {
                bos.write(buf)
            } while (`is`.read(buf) != -1)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`?.close()
                bos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        var filename: String? = null
        if (mimeType == null) {
            val path = getPath(context, uri)
            filename = if (path == null) {
                getName(uri.toString())
            } else {
                val file = File(path)
                file.name
            }
        } else {
            val returnCursor = context.contentResolver.query(
                uri, null,
                null, null, null
            )
            if (returnCursor != null) {
                val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                returnCursor.moveToFirst()
                filename = returnCursor.getString(nameIndex)
                returnCursor.close()
            }
        }
        return filename
    }

    private fun getName(filename: String?): String? {
        if (filename == null) {
            return null
        }
        val index = filename.lastIndexOf('/')
        return filename.substring(index + 1)
    }

}