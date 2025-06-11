package com.aigroup.aigroupmobile.data.models.knowledge

import io.objectbox.annotation.Backlink
import io.objectbox.relation.ToMany
import io.objectbox.annotation.Entity as ObjectBoxEntity
import io.objectbox.annotation.Id as ObjectBoxId

@ObjectBoxEntity
data class KnowledgeDocument(
  @ObjectBoxId var id: Long = 0,
  var title: String = "",
  var metadata: MutableMap<String, String> = mutableMapOf(),
) {
  @Backlink(to = "document")
  lateinit var chunks: ToMany<KnowledgeChunk>
}