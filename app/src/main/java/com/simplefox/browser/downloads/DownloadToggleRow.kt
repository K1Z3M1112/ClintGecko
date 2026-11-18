package com.simplefox.browser.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.simplefox.browser.ui.foundation.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplefox.browser.ui.ClintSwitch
import com.simplefox.browser.ui.theme.LocalClintColors

/** Title + summary + switch row shared by the download dialogs. The whole row and the switch both toggle. */
@Composable
internal fun DownloadToggleRow(
    title: String,
    summary: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(summary, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        ClintSwitch(checked = checked, onCheckedChange = { onToggle() })
    }
}
