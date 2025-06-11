package com.aigroup.aigroupmobile.ui.pages.chat.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.aigroup.aigroupmobile.ui.components.SectionListItem
import com.aigroup.aigroupmobile.ui.components.SectionListSection
import com.aigroup.aigroupmobile.ui.theme.AIGroupAppTheme
import com.aigroup.aigroupmobile.ui.theme.LocalUiMode
import compose.icons.CssGgIcons
import compose.icons.cssggicons.Collage
import compose.icons.cssggicons.Search

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrainSheet(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
            .padding(16.dp)
    ) {
        LazyColumn {
            item {
                SearchBar(
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("搜索") },
                    leadingIcon = { Icon(CssGgIcons.Search, "") },
                    onQueryChange = { },
                    query = "",
                    onSearch = { },
                    windowInsets = WindowInsets(top = 0.dp),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) { }
                Spacer(modifier = Modifier.padding(16.dp))
            }

            items(3) {
                SectionListSection(
                    sectionHeader = "openai"
                ) {
                    SectionListItem(
                        icon = CssGgIcons.Collage,
                        title = "GPT-3",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceDim
                    )
                    SectionListItem(
                        icon = CssGgIcons.Collage,
                        title = "GPT-4o",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceDim
                    )
                    SectionListItem(
                        icon = CssGgIcons.Collage,
                        title = "GPT-4o",
                    )
                }

                Spacer(modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Preview(device = "id:pixel_6", showBackground = true)
@Composable
fun BrainSheetPreview() {
    AIGroupAppTheme {
        BrainSheet()
    }
}