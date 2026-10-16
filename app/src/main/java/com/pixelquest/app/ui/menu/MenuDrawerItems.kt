package com.pixelquest.app.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * The side menu's entries, one row each: its icon and label, the current one highlighted. Rows
 * are tall and evenly spaced, so the list is easy to read and to tap.
 */
@Composable
fun MenuDrawerItems(
    items: List<MenuItem>,
    context: MenuContext,
    selected: MenuItemId?,
    onItemClick: (MenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MenuSpacing.ItemInset),
        verticalArrangement = Arrangement.spacedBy(MenuSpacing.ItemGap)
    ) {
        items.visibleIn(context).forEach { item ->
            MenuDrawerItemRow(
                icon = item.icon,
                label = stringResource(item.labelFor(context)),
                isSelected = item.id == selected,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun MenuDrawerItemRow(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MenuSpacing.ItemHeight)
            .clip(shape)
            .background(if (isSelected) colors.primary.copy(alpha = 0.16f) else colors.background, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(horizontal = 16.dp)
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = colors.primaryText,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
