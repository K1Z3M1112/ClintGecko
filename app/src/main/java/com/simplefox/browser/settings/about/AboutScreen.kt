package com.simplefox.browser.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
private const val CONTACT_EMAIL_URL = "mailto:jhaiian@proton.me"
private const val APACHE_2_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0.txt"
private const val GECKOVIEW_URL = "https://mozilla.github.io/geckoview/"
private const val MPL_2_LICENSE_URL = "https://www.mozilla.org/MPL/2.0/"
private const val MARKWON_URL = "https://github.com/noties/Markwon"
private const val ANDROIDX_URL = "https://developer.android.com/jetpack/androidx"
private const val OKHTTP_URL = "https://github.com/square/okhttp"
private const val SIMPLEMAGIC_URL = "https://github.com/j256/simplemagic"
private const val SIMPLEMAGIC_LICENSE_URL = "https://opensource.org/licenses/ISC"
private const val ANDROIDSVG_URL = "https://github.com/BigBadaboom/androidsvg"
private const val COROUTINES_URL = "https://github.com/Kotlin/kotlinx.coroutines"
private const val BOUNCYCASTLE_URL = "https://www.bouncycastle.org"
private const val BOUNCYCASTLE_LICENSE_URL = "https://www.bouncycastle.org/licence.html"
private const val MEDIA3_URL = "https://developer.android.com/guide/topics/media/media3"

@Composable
private fun AboutCard(label: String, colors: ClintColors, content: @Composable () -> Unit) {
    Surface(
        color = colors.cardBackground,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                color = colors.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            content()
        }
    }
}

@Composable
private fun AboutThinDivider(color: Color) {
    HorizontalDivider(color = color, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun AboutLinkRow(
    label: String,
    linkText: String,
    colors: ClintColors,
    onClick: () -> Unit,
    labelLineHeight: TextUnit = TextUnit.Unspecified
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = colors.secondaryText,
            fontSize = 13.sp,
            lineHeight = labelLineHeight,
            modifier = Modifier.weight(1f)
        )
        Text(
            linkText,
            color = colors.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(start = 12.dp)
                .clickable(onClick = onClick)
                .padding(4.dp)
        )
    }
}

@Composable
private fun AboutStandaloneLink(text: String, colors: ClintColors, onClick: () -> Unit) {
    Text(
        text,
        color = colors.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
    )
}

@Composable
private fun AboutLibraryEntry(
    linkLabel: String,
    licenseLabel: String,
    usageText: String,
    colors: ClintColors,
    onLinkClick: () -> Unit,
    onLicenseClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            linkLabel,
            color = colors.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f).clickable(onClick = onLinkClick).padding(4.dp)
        )
        Text(
            licenseLabel,
            color = colors.secondaryText,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onLicenseClick).padding(4.dp)
        )
    }
    Text(
        usageText,
        color = colors.secondaryText,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
    )
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

    SettingsScreenScaffold {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(R.drawable.ic_setup_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(80.dp)
            )
            Column(Modifier.padding(start = 16.dp)) {
                Text(stringResource(R.string.app_name), color = colors.onSurface, fontSize = 22.sp)
                Text(
                    stringResource(R.string.about_acronym),
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        AboutCard(stringResource(R.string.about_section_about), colors) {
            Text(stringResource(R.string.about_description), color = colors.onSurface, fontSize = 14.sp, lineHeight = 21.sp)
            AboutThinDivider(colors.surfaceVariant)
            AboutLinkRow(stringResource(R.string.about_created_by), stringResource(R.string.about_linktree_url), colors, { onLinkClick(AUTHOR_URL) })
        }

        AboutCard(stringResource(R.string.about_section_version), colors) {
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

        AboutCard(stringResource(R.string.about_section_license), colors) {
            Text(
                stringResource(R.string.about_license_text),
                color = colors.onSurface,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            AboutStandaloneLink(stringResource(R.string.about_license_link_label), colors) { onLinkClick(LICENSE_URL) }
        }

        AboutCard(stringResource(R.string.about_section_keep_android_open), colors) {
            Text(
                stringResource(R.string.about_keep_android_open_text),
                color = colors.onSurface,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            AboutStandaloneLink(stringResource(R.string.about_keep_android_open_url), colors) { onLinkClick(KEEP_ANDROID_OPEN_URL) }
        }

        AboutCard(stringResource(R.string.about_section_legal), colors) {
            AboutLinkRow(stringResource(R.string.document_viewer_privacy_policy_title), stringResource(R.string.about_legal_view), colors, onPrivacyPolicyClick)
            AboutThinDivider(colors.surfaceVariant)
            AboutLinkRow(stringResource(R.string.document_viewer_terms_title), stringResource(R.string.about_legal_view), colors, onTermsClick)
        }

        AboutCard(stringResource(R.string.about_section_contact), colors) {
            AboutLinkRow(stringResource(R.string.about_contact_email_label), stringResource(R.string.about_contact_email), colors, { onLinkClick(CONTACT_EMAIL_URL) })
        }

        AboutCard(stringResource(R.string.about_section_libraries), colors) {
            AboutLibraryEntry(
                stringResource(R.string.about_library_geckoview_label), stringResource(R.string.about_library_geckoview_license),
                stringResource(R.string.about_library_geckoview_usage), colors,
                { onLinkClick(GECKOVIEW_URL) }, { onLinkClick(MPL_2_LICENSE_URL) }
            )
            AboutThinDivider(colors.surfaceVariant)
            AboutLibraryEntry(
                stringResource(R.string.about_library_androidx_label), stringResource(R.string.about_library_androidx_license),
                stringResource(R.string.about_library_androidx_usage), colors,
                { onLinkClick(ANDROIDX_URL) }, { onLinkClick(APACHE_2_LICENSE_URL) }
            )
            AboutThinDivider(colors.surfaceVariant)
            AboutLibraryEntry(
                stringResource(R.string.about_library_markwon_label), stringResource(R.string.about_library_markwon_license),
                stringResource(R.string.about_library_markwon_usage), colors,
                { onLinkClick(MARKWON_URL) }, { onLinkClick(APACHE_2_LICENSE_URL) }
            )
            AboutThinDivider(colors.surfaceVariant)
            AboutLibraryEntry(
                stringResource(R.string.about_library_okhttp_label), stringResource(R.string.about_library_okhttp_license),
                stringResource(R.string.about_library_okhttp_usage), colors,
                { onLinkClick(OKHTTP_URL) }, { onLinkClick(APACHE_2_LICENSE_URL) }
            )
            AboutThinDivider(colors.surfaceVariant)
            AboutLibraryEntry(
                stringResource(R.string.about_library_simplemagic_label), stringResource(R.string.about_library_simplemagic_license),
                stringResource(R.string.about_library_simplemagic_usage), colors,
                { onLinkClick(SIMPLEMAGIC_URL) }, { onLinkClick(SIMPLEMAGIC_LICENSE_URL) }
            )
            AboutThinDivider(colors.surfaceVariant)
            AboutLibraryEntry(
                stringResource(R.string.about_library_androidsvg_label), stringResource(R.string.about_library_androidsvg_license),
                stringResource(R.string.about_library_androidsvg_usage), colors,
                { onLinkClick(ANDROIDSVG_URL) }, { onLinkClick(APACHE_2_LICENSE_URL) }
            )
            AboutThinDivider(colors.surfaceVariant)
            AboutLibraryEntry(
                stringResource(R.string.about_library_coroutines_label), stringResource(R.string.about_library_coroutines_license),
                stringResource(R.string.about_library_coroutines_usage), colors,
                { onLinkClick(COROUTINES_URL) }, { onLinkClick(APACHE_2_LICENSE_URL) }
            )
            AboutThinDivider(colors.surfaceVariant)
            AboutLibraryEntry(
                stringResource(R.string.about_library_bouncycastle_label), stringResource(R.string.about_library_bouncycastle_license),
                stringResource(R.string.about_library_bouncycastle_usage), colors,
                { onLinkClick(BOUNCYCASTLE_URL) }, { onLinkClick(BOUNCYCASTLE_LICENSE_URL) }
            )
            AboutThinDivider(colors.surfaceVariant)
            AboutLibraryEntry(
                stringResource(R.string.about_library_media3_label), stringResource(R.string.about_library_media3_license),
                stringResource(R.string.about_library_media3_usage), colors,
                { onLinkClick(MEDIA3_URL) }, { onLinkClick(APACHE_2_LICENSE_URL) }
            )
        }

        AboutCard(stringResource(R.string.about_section_attribution), colors) {
            AboutLinkRow(
                stringResource(R.string.about_attribution_text),
                stringResource(R.string.about_legal_view),
                colors,
                onAttributionClick,
                labelLineHeight = 19.sp
            )
        }
    }
}
