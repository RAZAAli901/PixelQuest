package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.comicBorder
import java.time.DayOfWeek

/**
 * Step 23: Comic dropdown / selector matching PixelDropdown/PixelSelector role.
 * Includes ComicDropdown menu and ComicDaySelector chip row.
 */
@Composable
fun <T> ComicDropdown(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String = { it.toString() },
    modifier: Modifier = Modifier,
    label: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                fontFamily = BangersFontFamily,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp,
                color = ComicTokens.SolidBlack,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        ComicPanel(
            variant = ComicPanelVariant.SURFACE,
            contentPadding = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = itemLabel(selectedItem),
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComicTokens.SolidBlack
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = ComicTokens.SolidBlack
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(ComicTokens.PanelSurface)
                .comicBorder(ComicShapeTokens.BorderWidthDefault, ComicTokens.SolidBlack, RoundedCornerShape(8.dp))
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = itemLabel(item),
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            fontWeight = if (item == selectedItem) FontWeight.Black else FontWeight.Normal,
                            color = ComicTokens.SolidBlack
                        )
                    },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Step 23: ComicDaySelector matching PixelDaySelector role.
 * Features 48dp tactile day chips, Sky Blue fill for selected state,
 * and solid black ink borders.
 */
@Composable
fun ComicDaySelector(
    selectedDays: Set<DayOfWeek>,
    onDayToggled: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "SELECT DAYS",
    errorText: String? = null
) {
    val days = listOf(
        DayOfWeek.MONDAY to "M",
        DayOfWeek.TUESDAY to "T",
        DayOfWeek.WEDNESDAY to "W",
        DayOfWeek.THURSDAY to "T",
        DayOfWeek.FRIDAY to "F",
        DayOfWeek.SATURDAY to "S",
        DayOfWeek.SUNDAY to "S"
    )

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            fontFamily = BangersFontFamily,
            fontSize = 15.sp,
            letterSpacing = 0.5.sp,
            color = ComicTokens.SolidBlack,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            days.forEach { (day, shortName) ->
                val isSelected = selectedDays.contains(day)
                val panelVariant = if (isSelected) ComicPanelVariant.SKY_BLUE else ComicPanelVariant.SURFACE

                ComicPanel(
                    variant = panelVariant,
                    contentPadding = 0.dp,
                    cornerRadius = 8.dp,
                    shadowOffset = 3.dp,
                    borderWidth = 2.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onDayToggled(day) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shortName,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = ComicTokens.SolidBlack
                        )
                    }
                }
            }
        }

        if (errorText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ComicTokens.CoralRed,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
