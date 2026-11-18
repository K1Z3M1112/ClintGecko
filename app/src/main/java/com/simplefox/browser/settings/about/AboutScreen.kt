package com.simplefox.browser.settings.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.simplefox.browser.ui.foundation.Icon
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.simplefox.browser.ui.foundation.HorizontalDivider
import com.simplefox.browser.ui.foundation.Surface
import com.simplefox.browser.ui.foundation.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplefox.browser.R
import com.simplefox.browser.settings.common.SettingsScreenScaffold
import com.simplefox.browser.ui.theme.ClintColors
import com.simplefox.browser.ui.theme.LocalClintColors

private const val AUTHOR_URL = "https://linktr.ee/jhaiian"
private const val KEEP_ANDROID_OPEN_URL = "https://keepandroidopen.org"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
private const val APACHE_2_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0.txt"
private const val GECKOVIEW_URL = "https://mozilla.github.io/geckoview/"
private const val MPL_2_LICENSE_URL = "https://www.mozilla.org/MPL/2.0/"
private const val ANDROIDX_URL = "https://developer.android.com/jetpack/androidx"
private const val OKHTTP_URL = "https://github.com/square/okhttp"
private const val ANDROIDSVG_URL = "https://github.com/BigBadaboom/androidsvg"
private const val COROUTINES_URL = "https://github.com/Kotlin/kotlinx.coroutines"

private class AboutLibrary(
    val label: Int, val license: Int, val usage: Int, val url: String, val licenseUrl: String
)

private val aboutLibraries = listOf(
    AboutLibrary(R.string.about_library_geckoview_label, R.string.about_library_geckoview_license, R.string.about_library_geckoview_usage, GECKOVIEW_URL, MPL_2_LICENSE_URL),
    AboutLibrary(R.string.about_library_androidx_label, R.string.about_library_androidx_license, R.string.about_library_androidx_usage, ANDROIDX_URL, APACHE_2_LICENSE_URL),
    AboutLibrary(R.string.about_library_okhttp_label, R.string.about_library_okhttp_license, R.string.about_library_okhttp_usage, OKHTTP_URL, APACHE_2_LICENSE_URL),
    AboutLibrary(R.string.about_library_androidsvg_label, R.string.about_library_androidsvg_license, R.string.about_library_androidsvg_usage, ANDROIDSVG_URL, APACHE_2_LICENSE_URL),
    AboutLibrary(R.string.about_library_coroutines_label, R.string.about_library_coroutines_license, R.string.about_library_coroutines_usage, COROUTINES_URL, APACHE_2_LICENSE_URL),
)

@Composable
private fun AboutSectionHeader(icon: ImageVector, label: String, colors: ClintColors, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(17.dp))
        }
        Text(
            label,
            color = colors.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.weight(1f).padding(start = 10.dp)
        )
        trailing()
    }
}

@Composable
private fun AboutCard(
    label: String,
    icon: ImageVector,
    colors: ClintColors,
    headerTrailing: @Composable () -> Unit = {},
    onHeaderClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        color = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(Modifier.padding(16.dp).animateContentSize()) {
            Box(if (onHeaderClick != null) Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onHeaderClick) else Modifier) {
                AboutSectionHeader(icon, label, colors, headerTrailing)
            }
            content()
        }
    }
}

@Composable
private fun AboutThinDivider(color: Color) {
    HorizontalDivider(color = color, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
}

@Composable
private fun AboutLinkRow(
    label: String,
    linkText: String,
    colors: ClintColors,
    onClick: () -> Unit,
    labelLineHeight: TextUnit = TextUnit.Unspecified
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = colors.onSurface,
            fontSize = 14.sp,
            lineHeight = labelLineHeight,
            modifier = Modifier.weight(1f).padding(vertical = 6.dp)
        )
        Text(
            linkText.removeSuffix(" \u2197"),
            color = colors.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp)
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AboutStandaloneLink(text: String, colors: ClintColors, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = colors.primary, modifier = Modifier.padding(start = 6.dp).size(14.dp))
    }
}

@Composable
private fun AboutLibraryEntry(
    library: AboutLibrary,
    colors: ClintColors,
    onLinkClick: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(library.label),
                color = colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { onLinkClick(library.url) }.padding(vertical = 4.dp, horizontal = 4.dp)
            )
            Text(
                stringResource(library.license),
                color = colors.secondaryText,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.surfaceVariant)
                    .clickable { onLinkClick(library.licenseUrl) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Text(
            stringResource(library.usage),
            color = colors.secondaryText,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
fun AboutScreen(
    versionInfo: String,
    webViewInfo: String,
    onLinkClick: (String) -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsClick: () -> Unit,
    onAttributionClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    var librariesExpanded by remember { mutableStateOf(false) }

    SettingsScreenScaffold {
        // Hero
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(104.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(R.drawable.ic_setup_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(72.dp)
                )
            }
            Text(
                stringResource(R.string.app_name),
                color = colors.onSurface,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 14.dp)
            )
            Text(
                stringResource(R.string.about_acronym),
                color = colors.secondaryText,
                fontSize = 12.sp,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
            )
            val versionLine = versionInfo.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
            if (versionLine.isNotEmpty()) {
                Text(
                    versionLine,
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        AboutCard(stringResource(R.string.about_section_about), Icons.Filled.Info, colors) {
            Text(stringResource(R.string.about_description), color = colors.onSurface, fontSize = 14.sp, lineHeight = 22.sp)
            AboutThinDivider(colors.surfaceVariant)
            AboutLinkRow(stringResource(R.string.about_created_by), stringResource(R.string.about_linktree_url), colors, { onLinkClick(AUTHOR_URL) })
        }

        AboutCard(
            stringResource(R.string.about_section_version), Icons.Filled.PhoneAndroid, colors,
            headerTrailing = {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.about_copy_info),
                    tint = colors.secondaryText,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("SimpleFox", versionInfo + "\n" + webViewInfo))
                        }
                        .padding(6.dp)
                        .size(18.dp)
                )
            }
        ) {
            Text(versionInfo, color = colors.onSurface, fontSize = 13.sp, lineHeight = 21.sp, fontFamily = FontFamily.Monospace)
            AboutThinDivider(colors.divider)
            Text(
                stringResource(R.string.about_webview_label),
                color = colors.secondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(webViewInfo, color = colors.onSurface, fontSize = 13.sp, lineHeight = 21.sp, fontFamily = FontFamily.Monospace)
        }

        AboutCard(stringResource(R.string.about_section_license), Icons.Filled.Gavel, colors) {
            Text(
                stringResource(R.string.about_license_text),
                color = colors.onSurface,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            AboutStandaloneLink(stringResource(R.string.about_license_link_label).removeSuffix(" \u2197"), colors) { onLinkClick(LICENSE_URL) }
        }

        AboutCard(stringResource(R.string.about_section_keep_android_open), Icons.Filled.LockOpen, colors) {
            Text(
                stringResource(R.string.about_keep_android_open_text),
                color = colors.onSurface,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            AboutStandaloneLink(stringResource(R.string.about_keep_android_open_url).removeSuffix(" \u2197"), colors) { onLinkClick(KEEP_ANDROID_OPEN_URL) }
        }

        AboutCard(stringResource(R.string.about_section_legal), Icons.Filled.Policy, colors) {
            AboutLinkRow(stringResource(R.string.document_viewer_privacy_policy_title), stringResource(R.string.about_legal_view), colors, onPrivacyPolicyClick)
            AboutThinDivider(colors.surfaceVariant)
            AboutLinkRow(stringResource(R.string.document_viewer_terms_title), stringResource(R.string.about_legal_view), colors, onTermsClick)
        }

        AboutCard(
            stringResource(R.string.about_section_libraries), Icons.Filled.Widgets, colors,
            headerTrailing = {
                Text(
                    aboutLibraries.size.toString(),
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Icon(
                    if (librariesExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = colors.secondaryText,
                    modifier = Modifier.size(22.dp)
                )
            },
            onHeaderClick = { librariesExpanded = !librariesExpanded }
        ) {
            if (librariesExpanded) {
                aboutLibraries.forEachIndexed { index, library ->
                    if (index > 0) AboutThinDivider(colors.surfaceVariant)
                    AboutLibraryEntry(library, colors, onLinkClick)
                }
            }
        }

        AboutCard(stringResource(R.string.about_section_attribution), Icons.Filled.Code, colors) {
            AboutLinkRow(
                stringResource(R.string.about_attribution_text),
                stringResource(R.string.about_legal_view),
                colors,
                onAttributionClick,
                labelLineHeight = 20.sp
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
