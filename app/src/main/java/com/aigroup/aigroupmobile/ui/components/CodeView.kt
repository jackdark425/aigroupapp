package com.aigroup.aigroupmobile.ui.components

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import dev.snipme.kodeview.view.CodeTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private fun SyntaxLanguage.Companion.fromStringLang(lang: String): SyntaxLanguage {
  return when (lang.lowercase()) {
    "python" -> SyntaxLanguage.PYTHON
    "java" -> SyntaxLanguage.JAVA
    "kotlin" -> SyntaxLanguage.KOTLIN
    "c" -> SyntaxLanguage.C
    "cpp" -> SyntaxLanguage.CPP
    "rust" -> SyntaxLanguage.RUST
    "csharp" -> SyntaxLanguage.CSHARP
    "coffeescript" -> SyntaxLanguage.COFFEESCRIPT
    "javascript" -> SyntaxLanguage.JAVASCRIPT
    "perl" -> SyntaxLanguage.PERL
    "ruby" -> SyntaxLanguage.RUBY
    "shell" -> SyntaxLanguage.SHELL
    "swift" -> SyntaxLanguage.SWIFT
    else -> SyntaxLanguage.DEFAULT
  }
}

@Composable
private fun CodeViewButton(
  onClick: () -> Unit,
  content: @Composable () -> Unit,
) {
  Box(
    Modifier
      .clip(MaterialTheme.shapes.small)
      .background(MaterialTheme.colorScheme.surfaceContainer)
      .padding(3.dp)
      .size(20.dp)
      .clickable { onClick() }
  ) {
    Box(
      Modifier.align(Alignment.Center)
    ) {
      content()
    }
  }
}

@Composable
fun CodeView(
  code: String,
  language: String,
  modifier: Modifier = Modifier,
) {
  var highlights: Highlights? by remember {
    mutableStateOf(null)
  }

  LaunchedEffect(code) {
    val result = withContext(Dispatchers.IO) { // TODO: should be IO?
      Highlights.Builder()
        .code(code.trim().trimIndent())
        .theme(SyntaxThemes.darcula())
        .language(SyntaxLanguage.fromStringLang(language))
        .build()
    }
    highlights = result
  }

  var collapsed by remember { mutableStateOf(false) }
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  var showCopyStatus by remember { mutableStateOf(false) }

  LaunchedEffect(showCopyStatus) {
    if (showCopyStatus) {
      delay(3000)
      showCopyStatus = false
    }
  }

  fun copyText() {
    // TODO: 重复代码
    val text = code
    clipboardManager.setText(AnnotatedString(text))

    // https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#duplicate-notifications
    // Only show a toast for Android 12 and lower.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
    }
    showCopyStatus = true
  }

  val style = MaterialTheme.typography.bodyLarge.copy(
    fontFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal)),
    fontSize = 15.sp,
    lineHeight = 15.sp * 1.3,
  )

  Surface(
    shape = MaterialTheme.shapes.small,
    contentColor = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = modifier
  ) {
    Column {
      Row(
        Modifier
          .padding(horizontal = 12.dp, vertical = 5.dp)
          .padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CodeViewButton(
          onClick = {
            collapsed = !collapsed
          },
        ) {
          AnimatedContent(collapsed) {
            Icon(
              if (it) {
                Icons.Default.KeyboardArrowDown
              } else {
                Icons.Default.KeyboardArrowRight
              },
              "",
              tint = AppCustomTheme.colorScheme.secondaryLabel,
            )
          }
        }
        Spacer(Modifier.weight(1f))

        Text(
          text = language.uppercase(),
          style = MaterialTheme.typography.bodySmall,
          color = AppCustomTheme.colorScheme.secondaryLabel,
          fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.weight(1f))

        CodeViewButton(
          onClick = {
            copyText()
          },
        ) {

          AnimatedContent(showCopyStatus) {
            Icon(
              if (it) {
                Icons.Default.Check
              } else {
                ImageVector.vectorResource(R.drawable.ic_copy_lite_icon_legacy)
              },
              "",
              tint = AppCustomTheme.colorScheme.secondaryLabel,
              modifier = Modifier.size(30.dp),
            )
          }
        }
      }
      AnimatedVisibility(!collapsed) {
        CompositionLocalProvider(LocalTextStyle provides style) {
          val contentModifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp)
          if (highlights == null) {
            Text(
              code,
              contentModifier
            )
          } else {
            CodeTextView(
              contentModifier,
              highlights!!,
            )
          }
        }
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
fun CodeViewPreview() {
  AIGroupAppTheme {
    LazyColumn(
      Modifier
        .background(Color.Red)
        .fillMaxWidth(),
      contentPadding = PaddingValues(16.dp)
    ) {
      item {
        CodeView(CODE, "python")
      }
    }
  }
}

private const val CODE = """
import pygame
import time
import random

# 初始化 Pygame
pygame.init()

# 定义颜色
white = (255, 255, 255)
yellow = (255, 255, 102)
black = (0, 0, 0)
red = (213, 50, 80)
green = (0, 255, 0)
blue = (50, 153, 213)

# 设置窗口尺寸
width = 600
height = 400

# 创建游戏窗口
game_window = pygame.display.set_mode((width, height))
pygame.display.set_caption('贪吃蛇游戏')

# 游戏时钟
clock = pygame.time.Clock()
snake_block = 10
snake_speed = 15

# 字体样式
font_style = pygame.font.SysFont("bahnschrift", 25)
score_font = pygame.font.SysFont("comicsansms", 35)


def our_snake(snake_block, snake_list):
    for x in snake_list:
        pygame.draw.rect(game_window, black, [x[0], x[1], snake_block, snake_block])


def message(msg, color):
    mesg = font_style.render(msg, True, color)
    game_window.blit(mesg, [width / 6, height / 3])


def gameLoop():  # 创建游戏循环
    game_over = False
    game_close = False

    x1 = width / 2
    y1 = height / 2
    
    x1_change = 0
    y1_change = 0

    snake_List = []
    Length_of_snake = 1

    foodx = round(random.randrange(0, width - snake_block) / 10.0) * 10.0
    foody = round(random.randrange(0, height - snake_block) / 10.0) * 10.0

    while not game_over:

        while game_close == True:
            game_window.fill(blue)
            message("你输了! Q-退出, C-重新开始", red)
            pygame.display.update()

            for event in pygame.event.get():
                if event.type == pygame.KEYDOWN:
                    if event.key == pygame.K_q:
                        game_over = True
                        game_close = False
                    if event.key == pygame.K_c:
                        gameLoop()

        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                game_over = True
            if event.type == pygame.KEYDOWN:
                if event.key == pygame.K_LEFT:
                    x1_change = -snake_block
                    y1_change = 0
                elif event.key == pygame.K_RIGHT:
                    x1_change = snake_block
                    y1_change = 0
                elif event.key == pygame.K_UP:
                    y1_change = -snake_block
                    x1_change = 0
                elif event.key == pygame.K_DOWN:
                    y1_change = snake_block
                    x1_change = 0

        if x1 >= width or x1 < 0 or y1 >= height or y1 < 0:
            game_close = True

        x1 += x1_change
        y1 += y1_change
        game_window.fill(blue)
        pygame.draw.rect(game_window, green, [foodx, foody, snake_block, snake_block])
        snake_Head = []
        snake_Head.append(x1)
        snake_Head.append(y1)
        snake_List.append(snake_Head)
        if len(snake_List) > Length_of_snake:
            del snake_List[0]

        for x in snake_List[:-1]:
            if x == snake_Head:
                game_close = True

        our_snake(snake_block, snake_List)

        pygame.display.update()

        if x1 == foodx and y1 == foody:
            foodx = round(random.randrange(0, width - snake_block) / 10.0) * 10.0
            foody = round(random.randrange(0, height - snake_block) / 10.0) * 10.0
            Length_of_snake += 1

        clock.tick(snake_speed)

    pygame.quit()
    quit()


gameLoop()
"""
