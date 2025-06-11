package com.aigroup.aigroupmobile.data.models

import android.net.Uri
import com.aigroup.aigroupmobile.utils.encrypt.sha256
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.annotations.Ignore
import java.io.File

sealed interface MediaItem {
    /**
     * The unique identifier of the media item
     */
    // TODO: using url as identifier is good?
    val identifier: String

    /**
     * The string representation of Android Uri
     */
    val url: String
    val mimeType: String
    var onlineLink: String?
    var knowledgeDocId: Long?

    val uri: Uri
        get() = Uri.parse(url)

    val file: File
        get() = File(uri.path!!)
}

// TODO: 考虑唯一 id 方式
// TODO: 不要用多态， 使用 data class 作为辅助即可

/**
 * The document media type file, like PDF, DOCX, etc.
 */
class DocumentMediaItem(
    override var url: String,
    override var mimeType: String,

    /**
     * 文件名称，用于显示，默认是原始的文件名
     * TODO: 扩展到 MediaItem, better doc comment here
     */
    var title: String
): EmbeddedRealmObject, MediaItem {
    enum class DocType(val mimeType: String) {
        Pdf("application/pdf"),
        World("application/msword"),
        // docx
        WorldXml("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        Txt("text/plain"),
        // TODO: more
        Markdown("text/markdown"),
        Html("text/html"),
    }

    override val identifier: String
        get() = sha256("#document:$url") // TODO: make all id to sha256

    override var onlineLink: String? = null
    override var knowledgeDocId: Long? = null

    constructor(): this("", "", "")
}

// TODO: add a uri constructor
class ImageMediaItem(override var url: String): EmbeddedRealmObject, MediaItem {
    override val identifier: String
        get() = "#image:$url"

    @Ignore
    override var mimeType: String = "image/png" // TODO: not only png?, see [ChatViewModel]

    override var onlineLink: String? = null
    override var knowledgeDocId: Long? = null

    constructor(): this("")
}

class VideoMediaItem(override var url: String, var snapshot: ImageMediaItem?): EmbeddedRealmObject, MediaItem {
    override val identifier: String
        get() = "#video:$url"

    @Ignore
    override var mimeType: String = "video/mp4" // TODO: not only mp4?

    override var onlineLink: String? = null
    override var knowledgeDocId: Long? = null

    constructor(): this("", null)
}

class GenericMediaItem(
    override var url: String,
    override var mimeType: String
): EmbeddedRealmObject, MediaItem {
    override val identifier: String
        get() = "#generic:$url"

    override var onlineLink: String? = null
    override var knowledgeDocId: Long? = null

    constructor(): this("", "")
}
