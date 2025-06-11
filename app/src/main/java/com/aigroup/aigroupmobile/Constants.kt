package com.aigroup.aigroupmobile

// TODO: move this class
data class GuideQuestion(
  val question: String,
  val type: GuideQuestionType = GuideQuestionType.TEXT,
) {
  enum class GuideQuestionType {
    TEXT,
    PREFERENCE
  }
}

// TODO: rename variable
object Constants {
  val UntitledChat: String
    get() = appStringResource(R.string.label_untitled_chat)

  val BotGuideQuestions: List<GuideQuestion>
    get() = listOf(
      GuideQuestion(
        question = appStringResource(R.string.label_guide_question_1),
        type = GuideQuestion.GuideQuestionType.PREFERENCE
      ),
      GuideQuestion(question = appStringResource(R.string.label_guide_question_2)),
      GuideQuestion(question = appStringResource(R.string.label_guide_question_3)),
      GuideQuestion(question = appStringResource(R.string.label_guide_question_4)),
      GuideQuestion(question = appStringResource(R.string.label_guide_question_5)),
      GuideQuestion(question = appStringResource(R.string.label_guide_question_6)),
    )

  val TestTtsText: String
    get() = appStringResource(R.string.label_tts_text_text)

  // TODO: enhance in future
  fun getPreferenceAnswer(question: String): String {
    return appStringResource(R.string.label_guide_question_1_answer)
  }

  fun getQuestionOfMedia(isVideo: Boolean = false): List<String> {
    val typeSpecific = when {
      isVideo -> {
        listOf(
          appStringResource(R.string.label_guess_question_for_video_1),
          appStringResource(R.string.label_guess_question_for_video_2)
        )
      }

      else -> {
        listOf(
          appStringResource(R.string.label_guess_question_for_image_1),
          appStringResource(R.string.label_guess_question_for_image_2)
        )
      }
    }

    val total = typeSpecific + listOf(
      appStringResource(R.string.label_guess_question_for_media_1),
      appStringResource(R.string.label_guess_question_for_media_2),
      appStringResource(R.string.label_guess_question_for_media_3)
    ).sortedBy { it.length }

    val twoShort = total.take(2)
    val twoShuffled = total.subList(2, total.count()).shuffled().take(2)

    return (twoShort + twoShuffled).shuffled()
  }
}
