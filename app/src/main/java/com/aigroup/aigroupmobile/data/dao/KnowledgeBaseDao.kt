package com.aigroup.aigroupmobile.data.dao

import android.util.Log
import com.aigroup.aigroupmobile.data.AppDatabase
import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.MediaItem
import com.aigroup.aigroupmobile.data.models.knowledge.DocumentItem
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeBase
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeChunk
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeDocument
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.notifications.SingleQueryChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.ObjectId
import javax.inject.Inject

class KnowledgeBaseDao @Inject constructor(private val appDatabase: AppDatabase) {

  companion object {
    private const val TAG = "KnowledgeBaseDao"
  }

  fun getKnowledgeBase(id: ObjectId): Flow<KnowledgeBase?> {
    return appDatabase.realm.query<KnowledgeBase>("id = $0", id).first().asFlow().map { it.obj }
  }

  // TODO: support multiple knowledge bases
  fun getChatSessionBase(chatId: String): Flow<SingleQueryChange<KnowledgeBase>> {
    val id = BsonObjectId(chatId)
    return appDatabase.realm.query<KnowledgeBase>("ANY sessions.id = $0", id).first().asFlow()
  }

  // TODO: using ObjectID as param
  fun getDocsFromChat(chatId: String): Flow<ResultsChange<DocumentItem>> {
    return appDatabase.realm.query<DocumentItem>("ANY base.sessions.id = $0", BsonObjectId(chatId)).asFlow()
  }

  // TODO: using ObjectID as param
  fun getDocsFromAssistant(assistantID: String): Flow<ResultsChange<DocumentItem>> {
    return appDatabase.realm.query<DocumentItem>("ANY base.assistants.id = $0", BsonObjectId(assistantID)).asFlow()
  }

  fun getDocsFromBase(base: KnowledgeBase): Flow<ResultsChange<DocumentItem>> {
    return appDatabase.realm.query<DocumentItem>("ANY base.id = $0", base.id).asFlow()
  }

  fun getDocsFromBaseID(id: ObjectId): Flow<ResultsChange<DocumentItem>> {
    return appDatabase.realm.query<DocumentItem>("ANY base.id = $0", id).asFlow()
  }

  suspend fun getTempKnowledgeBase(reason: String): KnowledgeBase {
    // TODO: return directly if exists
    return appDatabase.realm.write {
      val base = copyToRealm(
        KnowledgeBase().apply {
          title = "Knowledge Base"
          description = "Temporary Knowledge Base for : $reason"
        }
      )
      return@write base
    }
  }

  suspend fun ensureKnowledgeBaseOnSession(session: ChatSession): KnowledgeBase {
    // if knowledge base already exists, do nothing
    if (session.knowledgeBases.isNotEmpty()) {
      return session.knowledgeBases.first()
    }

    return appDatabase.realm.write {
      val session = findLatest(session)
      require(session != null) { "ChatSession not found" }

      val base = copyToRealm(
        KnowledgeBase().apply {
          // TODO: detailed title and description dont hard code, every title and description, temp is also
          title = "Knowledge Base"
          description = "Knowledge Base for chatSession ${session.id.toHexString()}"
        }
      )
      session.knowledgeBases.add(base)

      return@write base
    }
  }

  suspend fun attachBaseToSession(base: KnowledgeBase, session: ChatSession) {
    // check already have session
    assert(base.sessions.isEmpty()) { "KnowledgeBase already attached to session" }

    return appDatabase.realm.write {
      val base = findLatest(base)
      require(base != null) { "KnowledgeBase not found" }

      val session = findLatest(session)
      require(session != null) { "ChatSession not found" }

      base.title = "Knowledge Base"
      base.description = "Knowledge Base for chatSession ${session.id.toHexString()}"

      session.knowledgeBases.add(base)
    }
  }

  suspend fun attachBaseToAssistant(base: KnowledgeBase, assistant: BotAssistant) {
    // check already have assistant
    assert(base.assistants.isEmpty()) { "KnowledgeBase already attached to assistant" }

    return appDatabase.realm.write {
      val base = findLatest(base)
      require(base != null) { "KnowledgeBase not found" }

      val assistant = findLatest(assistant)
      require(assistant != null) { "BotAssistant not found" }

      base.title = "Knowledge Base"
      base.description = "Knowledge Base for assistant ${assistant.storeIdentifier}"

      assistant.knowledgeBases.add(base)
      Log.i(TAG, "Attached base $base to assistant $assistant (${assistant.id})")
    }
  }

  suspend fun addKnowledgeDocToBase(base: KnowledgeBase, media: MediaItem): DocumentItem {
    require(media is DocumentMediaItem) { "MediaItem must be DocumentMediaItem" }

    return appDatabase.realm.write {
      val base = findLatest(base)
      require(base != null) { "KnowledgeBase not found" }

      val item = copyToRealm(DocumentItem().apply {
        document = media
      })
      base.docs.add(item)

      return@write item
    }
  }

  suspend fun deleteKnowledgeDocFromBase(base: KnowledgeBase, doc: DocumentItem) {
    // TODO: moving to realm transaction?
    if (doc.knowledgeDocId != null) {
      val box = appDatabase.objectboxStore.boxFor(KnowledgeDocument::class.java)
      val chunkBox = appDatabase.objectboxStore.boxFor(KnowledgeChunk::class.java)

      val knowledgeDoc = box.get(doc.knowledgeDocId!!)
      knowledgeDoc.chunks.forEach {
        chunkBox.remove(it)
      }
      box.remove(knowledgeDoc)

      Log.i(TAG, "Removed related knowledge doc for $doc")
    }

    appDatabase.realm.write {
      val base = findLatest(base)
      require(base != null) { "KnowledgeBase not found" }

      val doc = findLatest(doc)
      require(doc != null) { "DocumentItem not found" }

      base.docs.remove(doc)
      delete(doc)
    }

    Log.i(TAG, "Removed doc $doc from base $base")
  }

  suspend fun deleteKnowledgeBase(base: KnowledgeBase) {
    // delete docs
    base.docs.forEach {
      deleteKnowledgeDocFromBase(base, it)
    }

    appDatabase.realm.write {
      val base = findLatest(base)
      require(base != null) { "KnowledgeBase not found" }
      delete(base)
    }
    Log.i(TAG, "Removed base $base")
  }

  suspend fun deleteKnowledgeBaseByID(id: ObjectId) {
    val base = appDatabase.realm.query<KnowledgeBase>("id = $0", id).first().find()
    if (base != null) {
      deleteKnowledgeBase(base)
    } else {
      Log.w(TAG, "KnowledgeBase not found")
    }
  }

  suspend fun updateDocItem(doc: DocumentItem, updater: DocumentItem.() -> Unit) {
    appDatabase.realm.write {
      val doc = findLatest(doc)
      require(doc != null) { "DocumentItem not found" }
      doc.updater()
    }
  }

  suspend fun cloneKnowledgeBase(base: KnowledgeBase): KnowledgeBase {
    return appDatabase.realm.write {
      val base = findLatest(base)
      require(base != null) { "KnowledgeBase not found" }

      val clone = copyToRealm(
        KnowledgeBase().apply {
          title = base.title
          description = base.description
        }
      )

      base.docs.forEach {
        val doc = copyToRealm(DocumentItem().apply {
          document = copyFromRealm(it.document!!)
          knowledgeDocId = it.knowledgeDocId
          checkSum = it.checkSum
        })
        clone.docs.add(doc)
      }

      return@write clone
    }
  }

}