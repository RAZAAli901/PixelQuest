package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.PqSwitch
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * Settings in v2's language (docs/ui-v2.md): its top bar, then each section as a label over a card
 * of rows, switches and links ([SettingSwitch], [SettingLink]).
 */
@Composable
fun SettingsScreenScaffold(
    onNavigateBack: (() -> Unit)? = null,
    accountSection: @Composable ColumnScope.() -> Unit = {},
    notificationsSection: @Composable ColumnScope.() -> Unit = {},
    appearanceSection: @Composable ColumnScope.() -> Unit = {},
    aiSection: @Composable ColumnScope.() -> Unit = {},
    islamicSection: (@Composable ColumnScope.() -> Unit)? = null,
    simpleModeSection: @Composable ColumnScope.() -> Unit = {},
    dataSection: @Composable ColumnScope.() -> Unit = {},
    dangerZoneSection: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 40.dp)
    ) {
        PqTopBar(stringResource(R.string.pq_hero_settings), onBack = onNavigateBack)
        SettingsSection("Account", accountSection)
        SettingsSection("Notifications", notificationsSection)
        SettingsSection("Look & sound", appearanceSection)
        SettingsSection("AI coach", aiSection)
        if (islamicSection != null) SettingsSection(stringResource(R.string.settings_islamic_title), islamicSection)
        SettingsSection("Simple Mode", simpleModeSection)
        SettingsSection("Data & backup", dataSection)
        SettingsSection("Danger zone", dangerZoneSection)
    }
}

/** A section: its name over a card of its rows. */
@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    PqGroupLabel(title, Modifier.padding(top = 8.dp).semantics { heading() })
    PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

/** A setting that's on or off: its name, a line about it, and a switch; the whole row toggles it. */
@Composable
fun SettingSwitch(title: String, body: String?, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true, tag: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = PqType.BodyStrong)
            if (body != null) Text(text = body, style = PqType.Meta)
        }
        PqSwitch(checked = checked, onCheckedChange = null)
    }
}

/** A way somewhere else: its icon, its name, what it's set to now, and an arrow. */
@Composable
fun SettingLink(icon: ImageVector, title: String, onClick: () -> Unit, value: String? = null, enabled: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        Icon(icon, contentDescription = null, tint = PqColors.Ink, modifier = Modifier.size(22.dp))
        Text(text = title, style = PqType.BodyStrong, modifier = Modifier.weight(1f))
        if (value != null) Text(text = value, style = PqType.Meta)
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = PqColors.Muted, modifier = Modifier.size(22.dp))
    }
}

/** A line explaining a setting. */
@Composable
fun SettingNote(text: String) {
    Text(text = text, style = PqType.Meta.copy(color = PqColors.Ink2))
}
