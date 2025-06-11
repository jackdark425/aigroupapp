package com.aigroup.aigroupmobile.ui.utils


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// TODO: support rtl

fun PaddingValues.withoutTop() = PaddingValues(
    start = this.calculateStartPadding(LayoutDirection.Ltr),
    top = 0.dp,
    end = this.calculateEndPadding(LayoutDirection.Ltr),
    bottom = this.calculateBottomPadding()
)

fun PaddingValues.withoutBottom() = PaddingValues(
    start = this.calculateStartPadding(LayoutDirection.Ltr),
    top = this.calculateTopPadding(),
    end = this.calculateEndPadding(LayoutDirection.Ltr),
    bottom = 0.dp
)