package com.aigroup.aigroupmobile.ui.pages.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// TODO: 分享用户卡片

@Composable
fun UserCard(modifier: Modifier = Modifier) {
  ElevatedCard(
    modifier = modifier,
    elevation = CardDefaults.cardElevation(
      defaultElevation = 6.dp
    ),
  ) {
    Text(
      text = "Elevated",
      modifier = Modifier
        .padding(16.dp),
      textAlign = TextAlign.Center,
    )
  }
}

@Preview(showBackground = true)
@Composable
fun UserCardPreview() {
  Box(modifier = Modifier.width(300.dp).height(200.dp).padding(16.dp)) {
    UserCard(Modifier.fillMaxSize())
  }
}