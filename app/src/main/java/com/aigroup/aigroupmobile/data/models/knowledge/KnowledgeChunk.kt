package com.aigroup.aigroupmobile.data.models.knowledge

import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.VectorDistanceType
import io.objectbox.relation.ToOne
import io.objectbox.annotation.Entity as ObjectBoxEntity
import io.objectbox.annotation.Id as ObjectBoxId

@ObjectBoxEntity
data class KnowledgeChunk(
  @ObjectBoxId var id: Long = 0,

  var textContent: String = "",

  // TODO: how to dynamic dimensions?
  @HnswIndex(dimensions = 3072, distanceType = VectorDistanceType.EUCLIDEAN)
  var embedding: FloatArray = FloatArray(3072),

  var metadata: MutableMap<String, String> = mutableMapOf(),
) {
  lateinit var document: ToOne<KnowledgeDocument>

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as KnowledgeChunk

    if (id != other.id) return false
    if (textContent != other.textContent) return false
    if (metadata != other.metadata) return false
    if (document != other.document) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + textContent.hashCode()
    result = 31 * result + metadata.hashCode()
    result = 31 * result + document.hashCode()
    return result
  }
}
