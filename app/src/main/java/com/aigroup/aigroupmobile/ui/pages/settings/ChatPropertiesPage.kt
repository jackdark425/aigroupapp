@file:OptIn(ExperimentalMaterial3Api::class)

package com.aigroup.aigroupmobile.ui.pages.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import com.aigroup.aigroupmobile.ui.components.ActionButton
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.map

private enum class PropItem(
  @StringRes val labelRes: Int,
  val step: Int,
  val range: ClosedFloatingPointRange<Float>,
  @StringRes val descriptionRes: Int
) {
  TEMPERATURE(R.string.label_llm_prop_temp, 10, 0f..2f, (R.string.label_llm_prop_desc_temp)),
  TOP_P(R.string.label_llm_prop_top_p, 5, 0f..1f, (R.string.label_llm_prop_desc_top_p)),
  PRESENCE_PENALTY(
    R.string.label_llm_prop_presence_penalty, 10, -2f..2f,
    (R.string.label_llm_prop_desc_presence_penalty)
  ),
  FREQUENCY_PENALTY(
    R.string.label_llm_prop_frequency_penalty, 10, -2f..2f,
    (R.string.label_llm_prop_desc_frequency_penalty)
  );

  val label: String
    @Composable
    get() = stringResource(labelRes)

  val description: String
    @Composable
    get() = stringResource(descriptionRes)
}

@Composable
fun ChatPropertiesPage(
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.background,
  showTitle: Boolean = true,
  onBack: (() -> Unit)? = null,
  expand: Boolean = true,

  properties: AppPreferences.LongBotProperties,
  onChange: (AppPreferences.LongBotProperties) -> Unit = {},
  onReset: () -> Unit = {},
) {
  var selectedProp by remember { mutableStateOf(PropItem.TEMPERATURE) }
  val haptic = LocalHapticFeedback.current

  Scaffold(
    modifier = modifier,
    containerColor = containerColor,
    topBar = {
      if (showTitle) {
        TopAppBar(
          title = { Text(stringResource(R.string.label_llm_model_props_setting), fontWeight = FontWeight.SemiBold) },
          // back button
          navigationIcon = {
            if (onBack != null) {
              IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "", Modifier.size(20.dp))
              }
            }
          },
          actions = {
            TextButton(
              onClick = onReset,
              colors = ButtonDefaults.textButtonColors(
                contentColor = AppCustomTheme.colorScheme.primaryAction
              )
            ) {
              Text(stringResource(R.string.label_button_reset_llm_props))
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
          )
        )
      }
    },
  ) { innerPadding ->
    Box(
      Modifier
        .padding(if (showTitle) innerPadding else PaddingValues())
        .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(8.dp)
      ) {
        if (expand) {
          Column(
            modifier = Modifier.clip(MaterialTheme.shapes.large),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            PropertyItem(
              item = PropItem.TEMPERATURE,
              sliderValue = properties.temperature,
              onChange = { onChange(properties.toBuilder().setTemperature(it).build()) },
              icon = {
                Icon(
                  ImageVector.vectorResource(R.drawable.ic_temp_icon),
                  contentDescription = stringResource(R.string.label_llm_prop_temp),
                  modifier = Modifier.size(23.dp)
                )
              }
            )

            HorizontalDivider(
              color = MaterialTheme.colorScheme.surfaceDim,
              thickness = 0.5.dp
            )

            PropertyItem(
              item = PropItem.TOP_P,
              sliderValue = properties.topP,
              onChange = { onChange(properties.toBuilder().setTopP(it).build()) },
              icon = {
                Icon(
                  ImageVector.vectorResource(R.drawable.ic_dig_icon),
                  contentDescription = stringResource(R.string.label_llm_prop_top_p),
                  modifier = Modifier.size(23.dp)
                )
              }
            )
            HorizontalDivider(
              color = MaterialTheme.colorScheme.surfaceDim,
              thickness = 0.5.dp
            )

            PropertyItem(
              item = PropItem.PRESENCE_PENALTY,
              sliderValue = properties.presencePenalty,
              onChange = { onChange(properties.toBuilder().setPresencePenalty(it).build()) },
              icon = {
                Icon(
                  ImageVector.vectorResource(R.drawable.ic_spark_icon),
                  contentDescription = stringResource(R.string.label_llm_prop_presence_penalty),
                  modifier = Modifier.size(23.dp)
                )
              }
            )

            HorizontalDivider(
              color = MaterialTheme.colorScheme.surfaceDim,
              thickness = 0.5.dp
            )

            PropertyItem(
              item = PropItem.FREQUENCY_PENALTY,
              sliderValue = properties.frequencyPenalty,
              onChange = { onChange(properties.toBuilder().setFrequencyPenalty(it).build()) },
              icon = {
                Icon(
                  ImageVector.vectorResource(R.drawable.ic_no_repeat_icon),
                  contentDescription = stringResource(R.string.label_llm_prop_frequency_penalty),
                  modifier = Modifier.size(23.dp)
                )
              }
            )
          }

          if (!showTitle) {
            Spacer(modifier = Modifier.size(8.dp))
            TextButton(
              onClick = { onReset() },
              shape = MaterialTheme.shapes.medium,
            ) {
              Text(stringResource(R.string.label_button_reset_llm_prop_default_value))
            }
          }
        } else {
          Column {
            Box(
              modifier = Modifier.clip(MaterialTheme.shapes.large),
            ) {
              when (selectedProp) {
                PropItem.TEMPERATURE -> PropertyItem(
                  item = PropItem.TEMPERATURE,
                  sliderValue = properties.temperature,
                  onChange = { onChange(properties.toBuilder().setTemperature(it).build()) }
                )

                PropItem.TOP_P -> PropertyItem(
                  item = PropItem.TOP_P,
                  sliderValue = properties.topP,
                  onChange = { onChange(properties.toBuilder().setTopP(it).build()) }
                )

                PropItem.PRESENCE_PENALTY -> PropertyItem(
                  item = PropItem.PRESENCE_PENALTY,
                  sliderValue = properties.presencePenalty,
                  onChange = { onChange(properties.toBuilder().setPresencePenalty(it).build()) }
                )

                PropItem.FREQUENCY_PENALTY -> PropertyItem(
                  item = PropItem.FREQUENCY_PENALTY,
                  sliderValue = properties.frequencyPenalty,
                  onChange = { onChange(properties.toBuilder().setFrequencyPenalty(it).build()) }
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TODO: using entires of enum
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
              ActionButton(
                stringResource(R.string.label_llm_prop_temp), ImageVector.vectorResource(R.drawable.ic_temp_icon),
                toggle = selectedProp == PropItem.TEMPERATURE,
                titleFontSize = 12.sp
              ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selectedProp = PropItem.TEMPERATURE
              }

              ActionButton(
                stringResource(R.string.label_llm_prop_top_p), ImageVector.vectorResource(R.drawable.ic_dig_icon),
                toggle = selectedProp == PropItem.TOP_P,
                titleFontSize = 12.sp
              ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selectedProp = PropItem.TOP_P
              }

              ActionButton(
                // TODO: 优化写法，看在英文下的效果，单词长，容易拉长按钮总体长度
                stringResource(R.string.label_llm_prop_presence_penalty).replace(" ", "\n"),
                ImageVector.vectorResource(R.drawable.ic_spark_icon),
                toggle = selectedProp == PropItem.PRESENCE_PENALTY,
                titleFontSize = 12.sp
              ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selectedProp = PropItem.PRESENCE_PENALTY
              }

              ActionButton(
                stringResource(R.string.label_llm_prop_frequency_penalty).replace(" ", "\n"),
                ImageVector.vectorResource(R.drawable.ic_no_repeat_icon),
                toggle = selectedProp == PropItem.FREQUENCY_PENALTY,
                titleFontSize = 12.sp
              ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selectedProp = PropItem.FREQUENCY_PENALTY
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PropertyItem(
  item: PropItem,
  sliderValue: Double,
  icon: (@Composable () -> Unit)? = null,
  onChange: (Double) -> Unit,
) {
  var value by remember { mutableStateOf(sliderValue) }

  LaunchedEffect(sliderValue) {
    value = sliderValue
  }

  Box(
    Modifier
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .fillMaxWidth()
  ) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
          icon()
          Spacer(modifier = Modifier.size(3.dp))
        }
        Text(item.label, color = AppCustomTheme.colorScheme.primaryLabel)
        Spacer(modifier = Modifier.weight(1f))
        Text(
          "%.2f".format(value),
          style = MaterialTheme.typography.bodySmall,
          color = AppCustomTheme.colorScheme.secondaryLabel
        )
      }
      Spacer(modifier = Modifier.size(8.dp))

      Slider(
        value = value.toFloat(),
        onValueChange = {
          value = it.toDouble()
        },
        onValueChangeFinished = {
          onChange(value)
        },
        thumb = {
          Box(
            Modifier
              .size(width = 10.dp, height = 30.dp)
              .clip(MaterialTheme.shapes.small)
              .background(AppCustomTheme.colorScheme.primaryAction)
          )
        },
        colors = SliderDefaults.colors(
          thumbColor = AppCustomTheme.colorScheme.primaryAction,
          activeTrackColor = AppCustomTheme.colorScheme.primaryAction,
          inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainer,
          inactiveTickColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        steps = item.step,
        valueRange = item.range
      )

      if (item.description != null) {
        Spacer(modifier = Modifier.size(8.dp))
        Text(
          item.description,
          style = MaterialTheme.typography.bodySmall,
          color = AppCustomTheme.colorScheme.secondaryLabel
        )
      }
    }
  }
}

@Composable
fun ChatPropertiesSettingsPage(
  viewModel: SettingsViewModel = hiltViewModel(),
  onBack: (() -> Unit)? = null,
) {
  val properties by viewModel.preferences.map { it.defaultModelProperties }
    .collectAsStateWithLifecycle(
      AppPreferencesDefaults.defaultLongBotProperties
    )

  ChatPropertiesPage(
    showTitle = true,
    properties = properties,
    containerColor = AppCustomTheme.colorScheme.groupedBackground,
    onBack = onBack,
    onChange = {
      viewModel.updateLongBotProperties(it)
    },
    onReset = { viewModel.resetLongBotProperties() }
  )
}

@Preview(showSystemUi = true)
@Composable
private fun ChatPropertiesPagePreview() {
  var properties by remember {
    mutableStateOf(AppPreferences.LongBotProperties.getDefaultInstance())
  }

  AIGroupAppTheme {
    ChatPropertiesPage(
      showTitle = true,
      onBack = { },
      properties = properties,
      onChange = { properties = it },
      onReset = { properties = AppPreferences.LongBotProperties.getDefaultInstance() }
    )
  }
}

@Preview(showSystemUi = true)
@Composable
private fun ChatPropertiesPageModalPreview() {
  var properties by remember {
    mutableStateOf(AppPreferences.LongBotProperties.getDefaultInstance())
  }

  ModalBottomSheet(
    onDismissRequest = {},
    containerColor = MaterialTheme.colorScheme.background,
    sheetState = if (LocalInspectionMode.current) rememberStandardBottomSheetState() else rememberModalBottomSheetState()
  ) {
    ChatPropertiesPage(
      showTitle = false,
      onBack = { },
      expand = false,
      properties = properties,
      onChange = { properties = it },
      onReset = { properties = AppPreferences.LongBotProperties.getDefaultInstance() }
    )
  }
}

private fun AppPreferences.LongBotProperties.getValue(type: PropItem): Double = when (type) {
  PropItem.TEMPERATURE -> temperature
  PropItem.TOP_P -> topP
  PropItem.PRESENCE_PENALTY -> presencePenalty
  PropItem.FREQUENCY_PENALTY -> frequencyPenalty
}