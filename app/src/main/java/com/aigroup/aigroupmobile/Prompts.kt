package com.aigroup.aigroupmobile

import com.aigroup.aigroupmobile.data.models.ChatSummary
import com.aigroup.aigroupmobile.data.models.MessageChat
import com.aigroup.aigroupmobile.data.models.MessageChatData
import com.aigroup.aigroupmobile.data.models.MessageTextItem
import com.aigroup.aigroupmobile.data.models.readableText
import java.util.Locale

// TODO: system prompt

object Prompts {

  fun imagePlaceholder(helpText: String): String {
    return """
      **REMARK FOR CHATBOT**: This is originally a picture media content, for some reason
      it cannot be placed here directly, but the description related to the picture is:
      
      $helpText
      
      (Please do not disclose any content related to REMARK FOR CHATBOT to users)
    """.trimIndent()
  }

  fun videoTextPlaceholder(helpText: String): String {
    return """
      **REMARK FOR CHATBOT**: This is originally a video media content, for some reason
      it cannot be placed here directly, but the description related to the video is:
      
      $helpText
      
      (Please do not disclose any content related to REMARK FOR CHATBOT to users)
    """.trimIndent()
  }

  fun videoImagePlaceholder(helpText: String): String {
    return """
      **REMARK FOR CHATBOT**: This is originally a video media content, for some reason
      it cannot be placed here directly, but the description related to the video is:
      
      $helpText.
      
      (Please do not disclose any content related to REMARK FOR CHATBOT to users)
      And i will provide the first frame of the video as an image:
    """.trimIndent()
  }

  fun summarySession(oldSummary: String?, newMessages: List<MessageChat>): String {
    // generate an english prompt to summarize the user chat session by llm like (gpt-4o)
    // the old summary is the previous summary of the chat session
    // the new messages are the new messages that the user has sent after the old summary generation

    val newMessagesText = newMessages.joinToString("\n") {
      val role = it.sender!!.role
      val text = it.readableText
      "$role: $text"
    }

    return """
      **OLD-SUMMARY**: 
      ${oldSummary ?: "No summary available"}
      
      **NEW-MESSAGES**:
      $newMessagesText
      
      **INSTRUCTIONS**:
      `reset`
      `no quotes`
      `no explanations`
      `no prompt`
      `no self-reference`
      `no apologies`
      `no filler`
      `just answer`
      
      Summarizing the important parts of the **NEW-MESSAGES** using CHINESE concisely. 
      You can try to refer the **OLD-SUMMARY**. Track the topic of chat session and ignore the detail of chat.  
     
      Your first aim is make text concise and short, The text you provide should be short so it can be placed as
      a title on ios NavigationBar. For that, You can follow the latest news first or only. And don't add tailing punctuation.
    """.trimIndent()
  }

  fun translateMessage(part: MessageTextItem, locale: Locale): String {
    val targetLang = locale.toLanguageTag()
    return """
      **REMARK FOR CHATBOT**: This is a message that needs to be translated to $targetLang.
      
      **MESSAGE**: 
      ${part.text}
      
      **INSTRUCTIONS**:
      `translate`
      `no quotes`
      `no explanations`
      `no prompt`
      `no self-reference`
      `no apologies`
      `no filler`
      `just answer`
      
      Translate the message to $targetLang. 
      You can use the latest news or only. And don't add tailing punctuation.
    """.trimIndent()
  }

  fun avatarImagePrompt(info: String): String {
    return """
      Generate an CHINESE prompt for the avatar image generation.
      You can plan the composition of the picture yourself, but it must be suitable for use as an avatar.
       
      Refer to the following help information:
      $info
    """.trimIndent()
  }

  fun systemPromptGenerator(): String {
    return """
      Given a user customize assistant bot information or existing prompt, produce a detailed system prompt to guide a language model in completing the task effectively.

      # Guidelines
      - Understand the role of assistant bot: Grasp the main responsibility of the assistant bot, produce a brief self-introduction information to system prompt.
      - Understand the Task: Grasp the main objective, goals, requirements, constraints, and expected output.
      - Minimal Changes: If an existing prompt is provided, improve it only if it's simple. For complex prompts, enhance clarity and add missing elements without altering the original structure.
      - Reasoning Before Conclusions**: Encourage reasoning steps before any conclusions are reached. ATTENTION! If the user provides examples where the reasoning happens afterward, REVERSE the order! NEVER START EXAMPLES WITH CONCLUSIONS!
          - Reasoning Order: Call out reasoning portions of the prompt and conclusion parts (specific fields by name). For each, determine the ORDER in which this is done, and whether it needs to be reversed.
          - Conclusion, classifications, or results should ALWAYS appear last.
      - Examples: Include high-quality examples if helpful, using placeholders [in brackets] for complex elements.
         - What kinds of examples may need to be included, how many, and whether they are complex enough to benefit from placeholders.
      - Clarity and Conciseness: Use clear, specific language. Avoid unnecessary instructions or bland statements.
      - Formatting: Use markdown features for readability. DO NOT USE ``` CODE BLOCKS UNLESS SPECIFICALLY REQUESTED.
      - Constants: DO include constants in the prompt, as they are not susceptible to prompt injection. Such as guides, rubrics, and examples.
      - Output Format: Explicitly the most appropriate output format, in detail. This should include length and syntax (e.g. short sentence, paragraph, JSON, etc.)
          - For tasks outputting well-defined or structured data (classification, JSON, etc.) bias toward outputting a JSON.
          - JSON should never be wrapped in code blocks (```) unless explicitly requested.

      The final prompt you output should adhere to the following structure below. Do not include any additional commentary, only output the completed system prompt. SPECIFICALLY, do not include any additional messages at the start or end of the prompt. (e.g. no "---")

      [Concise instruction describing the task - this should be the first line in the prompt, no section header]

      [Additional details as needed.]

      [Optional sections with headings or bullet points for detailed steps.]

      # Steps [optional]

      [optional: a detailed breakdown of the steps necessary to accomplish the task]

      # Output Format

      [Specifically call out how the output should be formatted, be it response length, structure e.g. JSON, markdown, etc]

      # Examples [optional]

      [Optional: 1-3 well-defined examples with placeholders if necessary. Clearly mark where examples start and end, and what the input and output are. User placeholders as necessary.]
      [If the examples are shorter than what a realistic example is expected to be, make a reference with () explaining how real examples should be longer / shorter / different. AND USE PLACEHOLDERS! ]

      # Notes [optional]

      [optional: edge cases, details, and an area to call or repeat out specific important considerations]
    """.trimIndent()
  }
}