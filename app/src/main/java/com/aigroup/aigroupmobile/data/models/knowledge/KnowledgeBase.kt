package com.aigroup.aigroupmobile.data.models.knowledge

import com.aigroup.aigroupmobile.data.models.BotAssistant
import com.aigroup.aigroupmobile.data.models.ChatSession
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import io.realm.kotlin.ext.backlinks
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PersistedName
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class KnowledgeBase: RealmObject {
  @PrimaryKey
  @PersistedName("_id")
  var id = ObjectId()

  var title: String = ""
  var description: String = ""

  var docs: RealmList<DocumentItem> = realmListOf()

  // back reference to ChatSession
  val sessions: RealmResults<ChatSession> by backlinks(ChatSession::knowledgeBases)

  // back reference to BotAssistant
  val assistants: RealmResults<BotAssistant> by backlinks(BotAssistant::knowledgeBases)
}

// TODO: migrate to generic MediaDocs
class DocumentItem: RealmObject {
  @PrimaryKey
  @PersistedName("_id")
  var id = ObjectId()

  // only support text document here
  var document: DocumentMediaItem? = null

  // maps to ObjectBox
  var knowledgeDocId: Long? = null

  var checkSum: String = ""

  // back link
  val base: RealmResults<KnowledgeBase> by backlinks(KnowledgeBase::docs)
}