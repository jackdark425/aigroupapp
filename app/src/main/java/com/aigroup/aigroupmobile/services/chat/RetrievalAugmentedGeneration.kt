package com.aigroup.aigroupmobile.services.chat

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import com.aallam.openai.api.embedding.embeddingRequest
import com.aallam.openai.api.model.ModelId
import com.aigroup.aigroupmobile.connect.chat.officialAI
import com.aigroup.aigroupmobile.data.AppDatabase
import com.aigroup.aigroupmobile.data.dao.KnowledgeBaseDao
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeBase
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeChunk
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeChunk_
import com.aigroup.aigroupmobile.data.models.knowledge.KnowledgeDocument
import com.aigroup.aigroupmobile.services.documents.DocumentParseComponent
import com.aigroup.aigroupmobile.utils.common.filename
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.document.parser.TextDocumentParser
import dev.langchain4j.data.document.splitter.DocumentSplitters
import io.objectbox.kotlin.awaitCallInTx
import io.objectbox.kotlin.query
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RetrievalAugmentedGeneration @Inject constructor(
  @ApplicationContext private val context: Context,
  private val database: AppDatabase,
  private val dataStore: DataStore<AppPreferences>,
  private val dao: KnowledgeBaseDao,
) {

  companion object {
    private const val TAG = "RetrievalAugmentedGeneration"
  }

  suspend fun indexDocument(doc: DocumentMediaItem): KnowledgeDocument {
    val documentComponent = DocumentParseComponent.component(context, doc.mimeType)
    val parser = documentComponent.createParser(doc)

    Log.i(TAG, "Indexing document: $doc")

    val document = FileSystemDocumentLoader.loadDocument(doc.uri.path, parser)
    val splitter = DocumentSplitters.recursive(512, 64)
    val textSegments = splitter.split(document)
    Log.i(TAG, "Document split to ${textSegments.size} chunks")

    // TODO: support config model and service provider and extra settings like base api url
    val embeddingModel = AppPreferencesDefaults.defaultEmbeddingModel
    val ai = dataStore.data.map { it.officialAI() }.first()

    // doing embedding
    Log.i(TAG, "Embedding document: $doc")
    val request = embeddingRequest {
      model = ModelId(embeddingModel.code)
      input = textSegments.map { it.text() }
    }
    val embeddings = ai.embeddings(request).embeddings.mapIndexed { index, embedding ->
      textSegments[index] to embedding
    }
    Log.i(TAG, "Document embedded: $doc")

    val filename = doc.uri.filename()!!
    val docTitle = filename.substringBeforeLast(".")
    val ext = filename.substringAfterLast(".")

    // prepare document first
    val documentsBox = database.objectboxStore.boxFor(KnowledgeDocument::class.java)
    val chunksBox = database.objectboxStore.boxFor(KnowledgeChunk::class.java)

    val knowledgeDoc = KnowledgeDocument().apply {
      title = docTitle
      metadata["filename"] = filename
      metadata["ext"] = ext
    }
    documentsBox.store.awaitCallInTx {
      documentsBox.put(knowledgeDoc)
      Log.i(TAG, "put document: $knowledgeDoc")

      chunksBox.store.runInTx {
        for ((segment, textEmbedding) in embeddings) {
          val metadataEntries = segment.metadata().toMap().filter {
            it.value is String
          }.map {
            it.key to it.value.toString()
          }
          val chunkMetadata = mutableMapOf(*metadataEntries.toTypedArray())
          val chunk = KnowledgeChunk().apply {
            textContent = segment.text()
            metadata = chunkMetadata
            embedding = textEmbedding.embedding.map { it.toFloat() }.toFloatArray()
          }
          chunksBox.put(chunk)

          knowledgeDoc.chunks.add(chunk)
          documentsBox.put(knowledgeDoc)
        }
      }
    }
    Log.i(TAG, "Document indexed: $doc")

    return knowledgeDoc
  }

  suspend fun retrieveRelatedChunks(base: KnowledgeBase, prompt: String, topP: Double = 0.2, topK: Int = 5): Map<KnowledgeChunk, Double> {
    val docIds = dao.getDocsFromBase(base).map { it.list.toList() }.first().mapNotNull { it.knowledgeDocId }

    // doing embedding
    // TODO: support config model and service provider and extra settings like base api url
    val embeddingModel = AppPreferencesDefaults.defaultEmbeddingModel
    val ai = dataStore.data.map { it.officialAI() }.first()

    Log.i(TAG, "Embedding prompt: $prompt")
    val promptEmbedding = ai.embeddings(embeddingRequest {
      model = ModelId(embeddingModel.code)
      input = listOf(prompt)
    }).embeddings.first().embedding.map { it.toFloat() }.toFloatArray()
    Log.i(TAG, "Prompt embedded: $prompt")

    val chunkBox = database.objectboxStore.boxFor(KnowledgeChunk::class.java)
    // TODO: using https://docs.objectbox.io/kotlin-support#flow ?
    val chunks = chunkBox.query {
      nearestNeighbors(KnowledgeChunk_.embedding, promptEmbedding, topK)
      `in`(KnowledgeChunk_.documentId, docIds.toLongArray())
    }.findWithScores()

    // TODO: 排序
    return chunks.filter {
      it.score >= (1.0 - topP)
    }.associate {
      it.get() to it.score
    }
  }

}