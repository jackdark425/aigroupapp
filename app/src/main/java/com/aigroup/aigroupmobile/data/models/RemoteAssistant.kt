package com.aigroup.aigroupmobile.data.models

import androidx.annotation.DrawableRes
import com.aigroup.aigroupmobile.utils.common.now
import com.aigroup.aigroupmobile.utils.common.simpleDateStr
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class RemoteAssistant(
  val identifier: String,
  val configuration: Configuration,
  val startPrompts: List<String> = emptyList(),
  val metadata: Metadata,
) {

  @Serializable
  data class Configuration(
    val role: String,
    val preferredModelCode: String? = null,
  ) {
    class Builder {
      private var role: String = ""
      private var preferredModelCode: String? = null

      fun role(value: String) = apply { role = value }
      fun preferredModelCode(value: String?) = apply { preferredModelCode = value }

      fun build() = Configuration(role, preferredModelCode)
    }
  }

  @Serializable
  data class Metadata(
    val version: Int,
    val createdAt: String, // TODO: better type
    val author: String,
    val category: String,
    val themeColor: Long?, // TODO: better color
    val tags: List<String>,
    val description: String,
    val title: String,

    // TODO: update to using enum
    @DrawableRes val avatar: Int? = null,
    val avatarLink: String? = null,
    val avatarEmoji: String? = null
  ) {
    class Builder {
      private var version: Int = 1
        get() = field
      // like "2021-09-01"
      private var createdAt: String = LocalDateTime.now.simpleDateStr
        get() = field
      private var author: String = ""
        get() = field
      private var category: String = ""
        get() = field
      private var themeColor: Long? = null
        get() = field
      private var tags: List<String> = emptyList()
        get() = field
      private var description: String = ""
        get() = field
      private var title: String = ""
        get() = field
      private var avatar: Int? = null
        get() = field
      private var avatarLink: String? = null
        get() = field
      private var avatarEmoji: String? = null
        get() = field

      fun version(value: Int) = apply { version = value }
      fun createdAt(value: String) = apply { createdAt = value }
      fun author(value: String) = apply { author = value }
      fun category(value: String) = apply { category = value }
      fun themeColor(value: Long?) = apply { themeColor = value }
      fun tags(value: List<String>) = apply { tags = value }
      fun description(value: String) = apply { description = value }
      fun title(value: String) = apply { title = value }
      fun avatar(value: Int?) = apply { avatar = value }
      fun avatarLink(value: String?) = apply { avatarLink = value }
      fun avatarEmoji(value: String?) = apply { avatarEmoji = value }

      fun build() = Metadata(
        version, createdAt, author, category, themeColor,
        tags, description, title, avatar, avatarLink, avatarEmoji
      )
    }
  }


  class Builder {
    private var identifier: String = ""
    private var configuration: Configuration = Configuration("")
    private var startPrompts: List<String> = emptyList()
    private var metadata: Metadata = Metadata(1, "", "", "", null, emptyList(), "", "")

    fun identifier(value: String) = apply { identifier = value }
    fun configuration(value: Configuration) = apply { configuration = value }
    fun startPrompts(value: List<String>) = apply { startPrompts = value }
    fun metadata(value: Metadata) = apply { metadata = value }

    fun build() = RemoteAssistant(identifier, configuration, startPrompts, metadata)
  }
}
