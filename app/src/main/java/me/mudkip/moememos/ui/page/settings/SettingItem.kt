package me.mudkip.moememos.ui.page.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics

@Composable
fun SettingItem(
    icon: ImageVector,
    text: String,
    subtitle: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    SettingRow(icon, text, subtitle, trailingIcon, enabled,
        Modifier.clickable(enabled = enabled, onClick = onClick))
}

@Composable
fun SettingSwitchItem(
    icon: ImageVector,
    text: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingRow(
        icon, text, subtitle,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                modifier = Modifier.clearAndSetSemantics {},
            )
        },
        enabled = enabled,
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    text: String,
    subtitle: String?,
    trailingContent: @Composable (() -> Unit)?,
    enabled: Boolean,
    modifier: Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    ListItem(
        modifier = modifier,
        headlineContent = { Text(text) },
        supportingContent = subtitle?.takeIf { it.isNotBlank() }?.let {
            { Text(it) }
        },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(
            headlineColor = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
            supportingColor = if (enabled) secondaryColor else contentColor.copy(alpha = 0.38f),
            leadingIconColor = if (enabled) secondaryColor else contentColor.copy(alpha = 0.38f),
        ),
    )
}
