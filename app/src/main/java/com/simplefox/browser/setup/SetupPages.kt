package com.simplefox.browser.setup

import com.simplefox.browser.ui.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.simplefox.browser.ui.foundation.Card
import com.simplefox.browser.ui.foundation.CardDefaults
import com.simplefox.browser.ui.foundation.Checkbox
import com.simplefox.browser.ui.foundation.CheckboxDefaults
import com.simplefox.browser.ui.foundation.Icon
import com.simplefox.browser.ui.ClintRadioButton
import com.simplefox.browser.ui.foundation.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplefox.browser.R
import com.simplefox.browser.settings.lookandfeel.LanguageSelectorDialog
import com.simplefox.browser.settings.lookandfeel.languageSummaryText
import com.simplefox.browser.ui.theme.LocalClintColors

@Composable
fun SetupWelcomePage(
    language: String,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onLanguageSelected: (String) -> Unit,
    consentChecked: Boolean,
    onConsentCheckedChange: (Boolean) -> Unit,
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit
) {
    val colors = LocalClintColors.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    if (showLanguageDialog) {
        LanguageSelectorDialog(
            current = language,
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onSelect = { showLanguageDialog = false; onLanguageSelected(it) },
            onDismiss = { showLanguageDialog = false }
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_setup_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.padding(top = 40.dp).size(104.dp)
        )
        Text(
            stringResource(R.string.setup_welcome_title),
            color = colors.onSurface,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        )
        Text(
            stringResource(R.string.setup_welcome_subtitle),
            color = colors.secondaryText,
            fontSize = 14.sp,
            lineHeight = 19.6.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 32.dp)
        )
        Card(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { showLanguageDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(com.simplefox.browser.ui.icons.Icons.Filled.Language, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.pref_language_title),
                    color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
                Text(
                    languageSummaryText(language),
                    color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
            }
        }
        Card(
            Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(com.simplefox.browser.ui.icons.Icons.Filled.Shield, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.document_viewer_privacy_policy_title),
                    color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onPrivacyClick)
                        .padding(4.dp)
                )
                Text(
                    "\u2022",
                    color = colors.secondaryText, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    stringResource(R.string.document_viewer_terms_title),
                    color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onTermsClick)
                        .padding(4.dp)
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onConsentCheckedChange(!consentChecked) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = consentChecked,
                onCheckedChange = onConsentCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = colors.primary)
            )
            Text(
                stringResource(R.string.setup_terms_agree),
                color = colors.secondaryText, fontSize = 13.sp, lineHeight = 18.2.sp,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }
    }
}

private class ThemeOption(val key: String, val titleRes: Int, val descRes: Int)

private val ThemeOptions = listOf(
    ThemeOption("system", R.string.theme_system, R.string.theme_system_desc),
    ThemeOption("dark", R.string.theme_dark, R.string.theme_dark_desc),
    ThemeOption("light", R.string.theme_light, R.string.theme_light_desc)
)

/**
 * Light / dark / system choice. The choice only changes setup state, which re-themes this screen in place;
 * nothing is written to preferences or recreated until setup finishes.
 */
@Composable
fun SetupThemePage(
    theme: String,
    onThemeSelected: (String) -> Unit
) {
    val colors = LocalClintColors.current

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp)
    ) {
        item(key = "header") {
            SetupPageHeader(
                title = stringResource(R.string.setup_theme_title),
                subtitle = stringResource(R.string.setup_theme_subtitle)
            )
        }
        item(key = "appearance") {
            Column(Modifier.fillMaxWidth()) {
                SetupSectionHeading(
                    com.simplefox.browser.ui.icons.Icons.Filled.BrightnessMedium,
                    stringResource(R.string.pref_category_appearance).uppercase(),
                    colors.primary, Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeOptions.forEach { option ->
                        OptionTile(
                            selected = theme == option.key,
                            onClick = { onThemeSelected(option.key) },
                            label = stringResource(option.titleRes),
                            cardBackground = colors.cardBackground,
                            primary = colors.primary,
                            onPrimary = colors.onPrimary
                        ) {
                            ThemeModeSwatch(option.key, Modifier.size(40.dp))
                        }
                    }
                }
                Crossfade(targetState = theme, label = "themeCaption") { key ->
                    ThemeOptions.firstOrNull { it.key == key }?.let { SetupCaption(stringResource(it.descRes)) }
                }
            }
        }
        item(key = "bottomSpace") { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun SetupEnginePage(
    engine: String,
    customName: String,
    customUrl: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onEngineSelected: (String) -> Unit,
    onCustomEngineSaved: (name: String, url: String) -> Unit
) {
    val colors = LocalClintColors.current
    var customEditorOpen by remember { mutableStateOf(false) }
    val hasCustomEngine = customName.isNotBlank() && customUrl.isNotBlank()

    if (customEditorOpen) {
        com.simplefox.browser.ui.CustomSearchEngineDialog(
            initialName = customName,
            initialUrl = customUrl,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onConfirm = { name, url ->
                onCustomEngineSaved(name, url)
                customEditorOpen = false
            },
            onDismiss = { customEditorOpen = false }
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SetupPageHeader(
            title = stringResource(R.string.choose_search_engine),
            subtitle = stringResource(R.string.setup_subtitle)
        )

        data class EngineOption(val key: String, val titleRes: Int, val descRes: Int)
        listOf(
            EngineOption("duckduckgo", R.string.engine_duckduckgo, R.string.engine_duckduckgo_desc),
            EngineOption("brave", R.string.engine_brave, R.string.engine_brave_desc),
            EngineOption("ecosia", R.string.engine_ecosia, R.string.engine_ecosia_desc),
            EngineOption("google", R.string.engine_google, R.string.engine_google_desc)
        ).forEach { option ->
            val sel = engine == option.key
            SelectableCard(selected = sel, onClick = { onEngineSelected(option.key) }, cardBackground = colors.cardBackground, primary = colors.primary) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        val customSel = engine == "custom"
        SelectableCard(
            selected = customSel,
            onClick = { if (hasCustomEngine) onEngineSelected("custom") else customEditorOpen = true },
            cardBackground = colors.cardBackground, primary = colors.primary
        ) {
            ClintRadioButton(selected = customSel)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    if (hasCustomEngine) customName else stringResource(R.string.engine_custom),
                    color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium
                )
                Text(
                    if (hasCustomEngine) customUrl else stringResource(R.string.engine_custom_desc),
                    color = colors.secondaryText, fontSize = 13.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            com.simplefox.browser.ui.foundation.IconButton(onClick = { customEditorOpen = true }) {
                com.simplefox.browser.ui.foundation.Icon(
                    com.simplefox.browser.ui.icons.Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                    tint = colors.iconTint
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
