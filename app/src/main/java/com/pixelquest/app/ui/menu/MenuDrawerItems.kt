package com.pixelquest.app.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.comicDropShadow

/** Everything inside the side menu: the header, a divider, and the entries ([MenuItems.all]). */
@Composable
fun MenuDrawerContent(
    header: MenuHeaderState,
    context: MenuContext,
    selected: MenuItemId?,
    onItemClick: (MenuItem) -> Unit
) {
    MenuDrawerHeader(header)
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = MenuSpacing.Edge, vertical = 8.dp),
        color = PixelTheme.colors.onSurface.copy(alpha = 0.15f)
    )
    MenuDrawerItems(items = MenuItems.all, context = context, selected = selected, onItemClick = onItemClick)
}

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
                status = item.status(context)?.let { stringResource(it) },
                isSelected = item.id == selected,
                isSimpleMode = context.isSimpleMode,
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
    modifier: Modifier = Modifier,
    isSimpleMode: Boolean = false,
    status: String? = null
) {
    val colors = PixelTheme.colors
    val style = menuStyle(isSimpleMode)
    val shape = style.itemShape
    val selectedBorder = style.selectedBorder
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MenuSpacing.ItemHeight)
            .then(
                if (isSelected && style.selectedShadow) {
                    Modifier
                        .padding(end = 3.dp, bottom = 3.dp)
                        .comicDropShadow(offsetX = 3.dp, offsetY = 3.dp, shape = shape)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(if (isSelected) style.selectedFill else colors.background, shape)
            .then(if (isSelected && selectedBorder != null) Modifier.border(selectedBorder, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(horizontal = 16.dp)
    ) {
        // The label says it all: TalkBack would otherwise read the emoji's name first.
        Text(text = icon, fontSize = 20.sp, modifier = Modifier.clearAndSetSemantics {})
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = if (style.uppercaseLabels) label.uppercase() else label,
            style = style.labelStyle,
            color = style.ink,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (status != null) {
            Text(
                text = if (style.uppercaseLabels) status.uppercase() else status,
                style = MaterialTheme.typography.labelSmall,
                color = style.ink.copy(alpha = 0.7f)
            )
        }
    }
}
