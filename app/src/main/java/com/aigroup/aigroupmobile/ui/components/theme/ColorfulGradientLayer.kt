package com.aigroup.aigroupmobile.ui.components.theme

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.ui.theme.LocalUiMode
import com.aigroup.aigroupmobile.ui.theme.UiMode
import com.skydoves.cloudy.cloudy
import org.intellij.lang.annotations.Language

private val Color1 = Color(0xFFFFA5C3)
private val Color2 = Color(0xFFFFC393)
private val Color3 = Color(0xFFCFAFF7)

private val Color.aColor: android.graphics.Color
  get() = android.graphics.Color.valueOf(red, green, blue, alpha)

@Language("AGSL")
val CUSTOM_SHADER = """
    uniform float2 resolution;
    
    const float fadeHeight = 300;
    
    uniform int fadeOut;
    layout(color) uniform half4 color;
    layout(color) uniform half4 color2;
    layout(color) uniform half4 color3;
    uniform float iTime; // Shader playback time (s)
    
    const float h = 500;
    const float k = 0.85 - 0.25;
    const float k_2 = 0.2 - (-0.1);
    const float PI = 3.14159;
    float a = (-k) / (500 * 500);
    
    float colorFadeOut(float2 fragCoord) {
        float y = 1 - ((resolution.y - fragCoord.y) / fadeHeight);
        float a = 0.5;
        return -y * y + 1;
    }
    
    float tintColorX() {
        // from 0.25 to 0.85 and back
        return 0.3 * (1 + sin(0.002 * PI * (iTime - 250))) + 0.25;
    }
    
    float tintColorY() {
       // from -0.1 to 0.2 and back
        return 0.15 * (1 + sin(0.002 * PI * (iTime - 250))) - 0.1;
    }

    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord/resolution.xy;
        float x = fragCoord.x / resolution.x;

        half4 target = mix(color2, color, distance(uv, vec2(x, 0.8)));
        target = mix(color3, target, distance(uv, vec2(tintColorX(), tintColorY())));
        
        if (fragCoord.y <= resolution.y - fadeHeight) {
            return target;
        }
        
        if (fadeOut > 0) {
          float alpha = colorFadeOut(fragCoord);
          return half4(target.xyz * alpha, alpha);
        }
       
        return target;
    }
""".trimIndent()

@Composable
private fun produceDrawLoopCounter(durationSeconds: Float = 1f): State<Float> {
  return produceState(0f) {
    while (true) {
      withInfiniteAnimationFrameMillis {
        value = (it % (durationSeconds * 1000f)) / durationSeconds
      }
    }
  }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun DynamicGradientLayer(modifier: Modifier = Modifier, animation: Boolean, fadeOut: Boolean) {
  val animationTime by produceDrawLoopCounter(5f)

  Box(
    modifier
      .drawWithCache {
        val shader = RuntimeShader(CUSTOM_SHADER)
        val shaderBrush = ShaderBrush(shader)
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("iTime", animationTime * if (animation) 1f else 0f)
        onDrawBehind {
          shader.setColorUniform(
            "color",
            Color1.aColor
          )
          shader.setColorUniform(
            "color2",
            Color2.aColor
          )
          shader.setColorUniform(
            "color3",
            Color3.aColor
          )
          shader.setIntUniform("fadeOut", if (fadeOut) 1 else 0)
          drawRect(shaderBrush)
        }
      }
      .fillMaxWidth()
      .height(300.dp)
  )
}

@Composable
private fun FallbackColorfulGradientLayer(modifier: Modifier = Modifier) {
  val gradientColors = listOf(
    Color1,
    Color2,
    Color3,
    Color1
  )
  
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(300.dp)
      .background(
        brush = Brush.radialGradient(
          colors = gradientColors,
          radius = 800f
        )
      )
  )
}

@Composable
fun ColorfulGradientLayer(modifier: Modifier = Modifier, animation: Boolean = true, fadeOut: Boolean = true) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // android 13
    DynamicGradientLayer(modifier, animation, fadeOut)
  } else {
    // 为低版本 Android 提供备用的彩色渐变实现
    FallbackColorfulGradientLayer(modifier)
  }
}


@Preview
@Composable
fun ColorfulGradientLayerPreview() {
  AIGroupAppTheme {
    ColorfulGradientLayer(
      Modifier
    )
  }
}