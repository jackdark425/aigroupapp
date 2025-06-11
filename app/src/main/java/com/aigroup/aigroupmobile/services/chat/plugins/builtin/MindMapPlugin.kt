package com.aigroup.aigroupmobile.services.chat.plugins.builtin

import android.graphics.Point
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.aallam.openai.api.chat.ToolBuilder
import com.aallam.openai.api.chat.chatCompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.DocumentMediaItem
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageDocItem
import com.aigroup.aigroupmobile.data.models.MessageTextItem
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPlugin
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginDescription
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginRunScope
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginUpdateScope
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.utils.common.MimeTypes
import com.composables.materialcolors.MaterialColors
import com.composables.materialcolors.get
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.parse
import org.redundent.kotlin.xml.xml
import java.io.File
import kotlin.math.max

private const val SYSTEM_PROMPT = """
You are an expert in visualizing complex matters with mind maps. You describe the mind maps you create with a dedicated Markdown syntax.

Here is an example:

Input Text:
A mind map is a visual tool for organizing information, starting from a central idea or "root" and branching out
into related topics and subtopics. Mind maps have a long history, with early forms of visual idea representation
seen in various cultures over centuries. The popularization of mind maps as we know them today is largely attributed to Tony Buzan,
a British author who wrote extensively on popular psychology. Buzan's work helped bring mind maps into mainstream use as a method
for improving learning, creativity, and productivity. Research on mind maps has delved into their effectiveness and characteristics,
demonstrating how they can aid in understanding, memory retention, and information recall. There has also been interest in the automatic
creation of mind maps, where technology is used to generate them based on given data. These maps have diverse applications, including
creative techniques like brainstorming, strategic planning, and argument mapping. Various tools can be used to create mind maps.
Traditionally, people have relied on pen and paper, allowing for a more personal, hands-on approach. In more recent times,
digital tools like Mermaid—a scripting language for generating diagrams—have emerged, enabling users to create mind maps
efficiently and automatically.

Output mind map:
# Origins
## Long history
## Popularization
### British popular psychology author Tony Buzan
# Research
## On effectiveness and features
## On Automatic creation
### Uses
#### Creative techniques
#### Strategic planning
#### Argument mapping
# Tools
## Pen and paper
## Mermaid

You process every Input Text entered by a user in the same way.

Generate new mindmap in Chinese and only response markdown mindmap.
"""

/**
 * Using https://freemind.sourceforge.io/wiki/index.php/File_format as mindmap file format
 */
class MindMapPlugin : ChatPlugin() {
  companion object : ChatPluginDescription<MindMapPlugin> {
    const val TAG = "MindMapPlugin"

    override val name: String = "mind-map"
    override val displayName: String
      @Composable
      get() = "思维导图"
    override val icon = @Composable { ImageVector.vectorResource(R.drawable.ic_map_icon) }
    override val tintColor: Color = MaterialColors.Green[100]
    override val iconColor: Color = MaterialColors.Green[900]

    override val builder: ToolBuilder.() -> Unit = {
      function(
        name = name,
        description = "Generate a mind map outline based on the given topic",
      ) {
        put("type", "object")
        putJsonObject("properties") {
          putJsonObject("topic") {
            put("type", "string")
            put("description", "The main topic of the mind map")
          }
        }
        putJsonArray("required") {
          add("topic")
        }
      }
    }

    override fun create(): MindMapPlugin {
      return MindMapPlugin()
    }
  }

  private suspend fun ChatPluginRunScope.generateMindMapOutline(topic: String): String? {
    val prompt = """
      [TOPIC_TEXT]: $topic
    """.trimIndent()

    val request = chatCompletionRequest {
      temperature = 0.1
      model = ModelId(userModel.code)
      messages {
        system { content = SYSTEM_PROMPT }
        user { content = prompt }
      }
    }
    val response = userAI.chatCompletion(request).choices.first().message.content

    return response
  }

  private fun convertOutlineToMindMap(source: String): String {
    val lines = source.split("\n")
    val root = MindMapNode(0, "Root")
    val stack = mutableListOf(root)

    for (line in lines) {
      val line = line.trim()
      if (line.isEmpty()) {
        continue
      }

      val level = if (line.startsWith("#")) {
        line.count { it == '#' }
      } else {
        stack.last().level + 1
      }

      val text = if (line.startsWith("#")) {
        line.substringAfterLast("#").trim()
      } else {
        line.trim('-').trim()
      }

      val node = MindMapNode(level, text)
      while (level <= stack.last().level) {
        stack.removeLast()
      }
      stack.last().children += node
      stack += node
    }

    val mm = xml("map") {
      attribute("version", "1.0.1")
      fun Node.buildNode(node: MindMapNode) {
        element("node") {
          attributes(
//            "POSITION" to "right,
            "TEXT" to node.text
          )
          for (child in node.children) {
            buildNode(child)
          }
        }
      }
      buildNode(root)
    }

    return mm.toString()
  }

  override suspend fun execute(
    args: JsonObject,
    botMessage: MessageChat,
    run: suspend (suspend ChatPluginRunScope.() -> Unit) -> Unit,
    updater: suspend (suspend ChatPluginUpdateScope.() -> Unit) -> Unit
  ) {
    val topic = args.getValue("topic").jsonPrimitive.content
    Log.d(TAG, "Generating mind map for topic: $topic")

    run {
      val outline = generateMindMapOutline(topic) ?: error("Failed to generate mind map outline")
      val mindMap = convertOutlineToMindMap(outline)

      Log.i(TAG, "Generated mind map outline: $mindMap")

      updater {
        val filename = "${topic}的思维导图.mm"
        val uri = pathManager.writeDocsToStorage(filename, mindMap)

        chatDao.appendParts(botMessage, listOf(
          MessageDocItem().apply {
            document = DocumentMediaItem(
              url = uri.toString(),
              mimeType = MimeTypes.Application.FREEMIND.mimeType,
              title = filename,
            )
          }
        ))

        chatDao.appendParts(
          botMessage,
          listOf(
            MessageTextItem().apply { this.text = "这是我生成的关于 “$topic” 的思维导图" }
          ),
          simulateTextAnimation = true
        )
      }
    }
  }
}

data class MindMapNode(
  val level: Int,
  val text: String,
  val children: MutableList<MindMapNode> = mutableListOf() // TODO: is this right?
) {
  companion object {
    fun fromFreemindFile(file: File): MindMapNode {
      val parsed = parse(file.inputStream())
      if (parsed.nodeName == "map") {
        // TODO: avoid using "Root" as root node, same in convertOutlineToMindMap
        val rootNode =
          parsed.children.firstOrNull { it is Node && it.nodeName == "node" && it.get<String>("TEXT") == "Root" }
        if (rootNode == null) {
          error("Invalid mind map format")
        }

        return parseXmlNode(rootNode as Node)
      } else {
        error("Invalid mind map format")
      }
    }

    private fun parseXmlNode(node: Node, level: Int = 0): MindMapNode {
      val text = node.get<String>("TEXT")!!
      val mindNode = MindMapNode(level, text)

      for (child in node.children) {
        if (child is Node && child.nodeName == "node") {
          mindNode.children += parseXmlNode(child, level + 1)
        }
      }

      return mindNode
    }
  }
}

private data class MindMapNodeRender(
  val node: MindMapNode,
  val x: Float,
  val y: Float,
  val level: Int,
  val text: TextLayoutResult
)

private data class MindMapConnectionRender(
  val start: Point? = null,
  val end: Point? = null,
  val underline: Float,
  val color: Color,
  val thickness: Float = 3f,
  val isLeaf: Boolean = false
)

@Composable
fun MindMapRenderer(mindMap: MindMapNode, modifier: Modifier = Modifier) {
  val textMeasurer = rememberTextMeasurer()

  val padding = 10.0f
  val horizontalSpacing = 120.0f
  val verticalSpacing = 20f
  val underlineSpacing = 5f
  val backColor = MaterialTheme.colorScheme.background
  val rootThickness = 6f
  val strokeColors = listOf(
    MaterialColors.Blue[400],
    MaterialColors.Green[400],
    MaterialColors.Orange[400],
    MaterialColors.Purple[400],
    MaterialColors.Red[400],
    MaterialColors.Teal[400],
    MaterialColors.Yellow[400],
  )

  var canvasHeight by remember(mindMap) { mutableStateOf(0f) }
  var canvasWidth by remember(mindMap) { mutableStateOf(0f) }

  Layout(
    content = {
      Spacer(
        modifier = Modifier
          .drawWithCache {
            val renderList = mutableListOf<MindMapNodeRender>()
            val connectionList = mutableListOf<MindMapConnectionRender>()

            fun drawMindMapNode(
              node: MindMapNode,
              x: Float = padding,
              y: Float = padding,
              level: Int = 0,
              strokeColor: Color = strokeColors[0]
            ): Pair<Size, TextLayoutResult> {
              val textMeasurement = textMeasurer.measure(
                AnnotatedString(node.text),
                style = TextStyle(
                  fontFamily = FontFamily(Font(R.font.jetbrains_mono)),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium,
                  letterSpacing = 0.1.sp
                )
              )
              val textElement = MindMapNodeRender(node, x, y, level, textMeasurement)

              var relativeY = 0f
              var width = 0f
              var lineColor = strokeColor

              if (node.children.size > 0) {
                val childrenX = x + textMeasurement.multiParagraph.width + horizontalSpacing
                val lineEndPoints = mutableListOf<MindMapConnectionRender>()
                val childrenWidth = mutableListOf<Float>()

                for ((i, child) in node.children.withIndex()) {
                  val (itemSize, childText) = drawMindMapNode(child, childrenX, y + relativeY, level + 1, lineColor)
                  val itemHeight = itemSize.height

                  val textHeight = childText.multiParagraph.height
                  lineEndPoints.add(
                    MindMapConnectionRender(
                      end = Point(
                        (childrenX + 10).toInt(),
                        (y + relativeY + itemHeight / 2 + textHeight / 2 + underlineSpacing).toInt()
                      ),
                      color = lineColor,
                      underline = childText.multiParagraph.width,
                      thickness = max(rootThickness - level - 1, 2f),
                      isLeaf = child.children.isEmpty()
                    )
                  )

                  relativeY += itemHeight
                  childrenWidth.add(itemSize.width)

                  // append spacing
                  if (i < node.children.size - 1) {
                    relativeY += verticalSpacing

                    if (level == 0) {
                      lineColor = strokeColors[(i + 1) % strokeColors.size]
                    }
                  }
                }

                // fix text element
                val centerY = relativeY / 2
                val fixedY = y + centerY - textMeasurement.multiParagraph.height / 2
                renderList.add(textElement.copy(y = fixedY))

                // draw the lines
                for (point in lineEndPoints) {
                  val startX = x + textMeasurement.multiParagraph.width + 10
                  val startY = y + centerY + underlineSpacing + textMeasurement.multiParagraph.height / 2

                  connectionList.add(point.copy(start = Point(startX.toInt(), startY.toInt())))
                }

                // modify width
                width += childrenWidth.maxOrNull() ?: 0f
                width += horizontalSpacing
                width += textMeasurement.multiParagraph.width
              } else {
                renderList.add(textElement)
              }

              val itemHeight = max(textMeasurement.multiParagraph.height, relativeY)
              val itemWidth = max(textMeasurement.multiParagraph.width + 10, width)

              if (level == 0) {
                connectionList.add(
                  MindMapConnectionRender(
                    end = Point(
                      x.toInt(),
                      (y + itemHeight / 2 + textMeasurement.multiParagraph.height / 2 + underlineSpacing).toInt()
                    ),
                    underline = textMeasurement.multiParagraph.width,
                    color = strokeColor,
                    thickness = rootThickness
                  )
                )
              }

              return Size(itemWidth, itemHeight) to textMeasurement
            }

            val canvasSize = drawMindMapNode(mindMap).first
            canvasHeight = canvasSize.height
            canvasWidth = canvasSize.width

            onDrawBehind {
              for (render in renderList) {
                drawText(render.text, topLeft = Offset(render.x, render.y))
              }
              for (connection in connectionList) {
                if (connection.start != null && connection.end != null) {
                  val control1X = connection.start.x + 50
                  val control2X = connection.end.x - 80
                  val path = Path()
                  path.moveTo(connection.start.x.toFloat(), connection.start.y.toFloat())
                  path.cubicTo(
                    control1X.toFloat(), connection.start.y.toFloat(),
                    control2X.toFloat(), connection.end.y.toFloat(),
                    connection.end.x.toFloat(), connection.end.y.toFloat()
                  )
                  drawPath(
                    path, color = connection.color,
                    style = Stroke(connection.thickness, cap = StrokeCap.Round)
                  )
                }

                if (connection.end != null) {
                  drawLine(
                    color = connection.color,
                    start = Offset(connection.end.x.toFloat(), connection.end.y.toFloat()),
                    end = Offset(connection.end.x + connection.underline, connection.end.y.toFloat()),
                    strokeWidth = connection.thickness
                  )

                  if (!connection.isLeaf) {
                    val circleRadius = 9f
                    drawCircle(
                      color = connection.color,
                      center = Offset(connection.end.x.toFloat() + connection.underline + circleRadius / 2, connection.end.y.toFloat()),
                      radius = circleRadius + 3,
                      style = Fill
                    )
                    drawCircle(
                      color = backColor,
                      center = Offset(connection.end.x.toFloat() + connection.underline + circleRadius / 2, connection.end.y.toFloat()),
                      radius = circleRadius,
                      style = Fill
                    )
                  }
                }
              }
            }
          }
      )
    },
    modifier = modifier,
    measurePolicy = { measurables, constraints ->
      val canvasPlaceable = measurables[0].measure(constraints)

      layout(
        (canvasWidth + padding * 2).toInt(),
        (canvasHeight + padding * 2).toInt()
      ) {
        canvasPlaceable.place(0, 0)
      }
    }
  )
}

@Preview
@Composable
private fun MindMapRendererPreview() {
  val node = remember { buildTestMindNode().children.first() }
  AIGroupAppTheme {
    Box(
      Modifier
        .background(MaterialTheme.colorScheme.background)) {
      MindMapRenderer(node, modifier = Modifier)
    }
  }
}

// TODO: Should private
fun buildTestMindNode(): MindMapNode {
  val outline = """
# 环境保护
## 重要性
### 生态平衡
### 人类健康

## 挑战
### 气候变化
### 污染
#### 空气污染
#### 水污染
#### 土壤污染
## 措施
### 可再生能源
### 节能减排
### 绿色技术
## 法律法规
### 国际协议
#### 巴黎协定
### 国家政策
## 社会参与
### 公民意识
### 环保组织
## 教育
### 环保教育
### 可持续发展教育
""".trimIndent()

  val lines = outline.split("\n")
  val root = MindMapNode(0, "Root")
  val stack = mutableListOf(root)

  for (line in lines) {
    val line = line.trim()
    if (line.isEmpty()) {
      continue
    }

    val level = if (line.startsWith("#")) {
      line.count { it == '#' }
    } else {
      stack.last().level + 1
    }

    val text = if (line.startsWith("#")) {
      line.substringAfterLast("#").trim()
    } else {
      line.trim('-').trim()
    }

    val node = MindMapNode(level, text)
    while (level <= stack.last().level) {
      stack.removeLast()
    }
    stack.last().children += node
    stack += node
  }

  return root
}