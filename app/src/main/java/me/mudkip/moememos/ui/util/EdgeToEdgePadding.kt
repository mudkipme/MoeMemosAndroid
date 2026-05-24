package me.mudkip.moememos.ui.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun edgeToEdgeContentPadding(
    contentPadding: PaddingValues,
    additionalBottomPadding: Dp = 0.dp,
): PaddingValues {
    return PaddingValues(
        start = contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
        top = contentPadding.calculateTopPadding(),
        end = contentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
        bottom = contentPadding.calculateBottomPadding() + additionalBottomPadding,
    )
}
