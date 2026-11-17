package com.graffitistudio.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.graffitistudio.core.designsystem.theme.LocalUrbanColors

@Composable
fun UrbanPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = LocalUrbanColors.current.surfacePrimary,
    borderColor: Color = LocalUrbanColors.current.borderSubtle,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        content = content
    )
}

@Composable
fun UrbanPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalUrbanColors.current
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accentLime,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.surfaceHighlight,
            disabledContentColor = colors.textMuted
        ),
        content = content
    )
}

@Composable
fun UrbanIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = LocalUrbanColors.current
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (selected) colors.accentLime else colors.surfaceElevated,
            contentColor = if (selected) colors.onAccent else colors.textPrimary,
            disabledContainerColor = colors.surfacePrimary,
            disabledContentColor = colors.textMuted
        ),
        content = content
    )
}
