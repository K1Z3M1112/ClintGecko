package com.simplefox.browser.util

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan

/**
 * Minimal Markdown-to-Spannable renderer for GitHub-flavoured README / release-note text.
 * Handles headings, emphasis, strikethrough, inline and fenced code, links, images (as alt text),
 * block quotes, bullet/numbered/task lists, rules and bare URLs. Raw HTML tags are dropped.
 */
object SimpleMarkdown {

    private const val CODE_BG = 0x22888888
    private val HEADING_SCALE = floatArrayOf(1.6f, 1.4f, 1.25f, 1.1f, 1.0f, 0.9f)

    private val headingRe = Regex("^\\s{0,3}(#{1,6})\\s+(.*?)\\s*#*\\s*$")
    private val ruleRe = Regex("^\\s{0,3}([-*_])(\\s*\\1){2,}\\s*$")
    private val bulletRe = Regex("^(\\s*)[-*+]\\s+(\\[[ xX]]\\s+)?(.*)$")
    private val orderedRe = Regex("^(\\s*)(\\d{1,9})[.)]\\s+(.*)$")
    private val quoteRe = Regex("^\\s{0,3}>\\s?(.*)$")
    private val fenceRe = Regex("^\\s{0,3}(```|~~~)")
    private val tagRe = Regex("</?(?![a-zA-Z][a-zA-Z0-9+.-]*:)[a-zA-Z][^>]*>|<!--.*?-->")

    private val inlineRe = Regex(
        "(`+)(.+?)\\1" +                          // 1,2 inline code
            "|!\\[([^\\]]*)]\\(([^)\\s]*)[^)]*\\)" +   // 3,4 image
            "|\\[([^\\]]+)]\\(([^)\\s]*)[^)]*\\)" +    // 5,6 link
            "|(\\*\\*|__)(?=\\S)(.+?)(?<=\\S)\\7" +     // 7,8 bold
            "|(~~)(?=\\S)(.+?)(?<=\\S)\\9" +            // 9,10 strike
            "|(\\*|(?<![A-Za-z0-9])_)(?=\\S)(.+?)(?<=\\S)\\11" + // 11,12 italic
            "|(https?://[^\\s<>()\\[\\]]+)"             // 13 bare url
    )

    fun render(markdown: String, linkColor: Int? = null): CharSequence {
        val out = SpannableStringBuilder()
        val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        var i = 0
        var fenceMarker: String? = null
        var codeStart = 0

        fun newParagraphBreak() {
            if (out.isNotEmpty() && !out.endsWith("\n\n")) {
                if (!out.endsWith("\n")) out.append('\n')
                out.append('\n')
            }
        }

        while (i < lines.size) {
            val line = lines[i]
            val fence = fenceRe.find(line)

            if (fenceMarker != null) {
                if (fence != null && fence.groupValues[1] == fenceMarker) {
                    out.setSpan(TypefaceSpan("monospace"), codeStart, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    out.setSpan(BackgroundColorSpan(CODE_BG), codeStart, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    out.append("\n\n")
                    fenceMarker = null
                } else {
                    out.append(line).append('\n')
                }
                i++
                continue
            }
            if (fence != null) {
                newParagraphBreak()
                fenceMarker = fence.groupValues[1]
                codeStart = out.length
                i++
                continue
            }

            if (line.isBlank()) {
                newParagraphBreak()
                i++
                continue
            }

            val heading = headingRe.find(line)
            if (heading != null) {
                newParagraphBreak()
                val start = out.length
                appendInline(out, heading.groupValues[2], linkColor)
                val level = heading.groupValues[1].length
                out.setSpan(StyleSpan(Typeface.BOLD), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.setSpan(RelativeSizeSpan(HEADING_SCALE[level - 1]), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.append("\n\n")
                i++
                continue
            }

            if (ruleRe.matches(line)) {
                newParagraphBreak()
                val start = out.length
                out.append("────────────")
                out.setSpan(ForegroundColorSpan(0x88888888.toInt()), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.append("\n\n")
                i++
                continue
            }

            val quote = quoteRe.find(line)
            if (quote != null) {
                val start = out.length
                appendInline(out, quote.groupValues[1], linkColor)
                out.append('\n')
                out.setSpan(LeadingMarginSpan.Standard(24), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.setSpan(StyleSpan(Typeface.ITALIC), start, out.length - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                i++
                continue
            }

            val bullet = bulletRe.find(line)
            val ordered = if (bullet == null) orderedRe.find(line) else null
            if (bullet != null || ordered != null) {
                val indentText = (bullet ?: ordered)!!.groupValues[1]
                val depth = indentText.replace("\t", "    ").length / 2
                val start = out.length
                if (bullet != null) {
                    val task = bullet.groupValues[2].trim()
                    if (task.isNotEmpty()) out.append(if (task.contains('x', true)) "☑ " else "☐ ")
                    appendInline(out, bullet.groupValues[3], linkColor)
                    out.append('\n')
                    out.setSpan(BulletSpan(16 + depth * 20), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else {
                    out.append(ordered!!.groupValues[2]).append(". ")
                    appendInline(out, ordered.groupValues[3], linkColor)
                    out.append('\n')
                    out.setSpan(LeadingMarginSpan.Standard(16 + depth * 20, 40 + depth * 20), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                i++
                continue
            }

            // Paragraph: soft-wrapped lines are joined with a space, a trailing double space or backslash breaks.
            appendInline(out, line.trim(), linkColor)
            val hardBreak = line.endsWith("  ") || line.endsWith("\\")
            val next = lines.getOrNull(i + 1)
            if (hardBreak || next == null || next.isBlank() || isBlockStart(next)) out.append('\n') else out.append(' ')
            i++
        }

        if (fenceMarker != null) {
            out.setSpan(TypefaceSpan("monospace"), codeStart, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            out.setSpan(BackgroundColorSpan(CODE_BG), codeStart, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        var end = out.length
        while (end > 0 && out[end - 1] == '\n') end--
        return out.subSequence(0, end)
    }

    private fun isBlockStart(line: String): Boolean =
        headingRe.containsMatchIn(line) || ruleRe.matches(line) || bulletRe.matches(line) ||
            orderedRe.matches(line) || quoteRe.containsMatchIn(line) || fenceRe.containsMatchIn(line)

    private fun appendInline(out: SpannableStringBuilder, raw: String, linkColor: Int?) {
        val text = tagRe.replace(raw, "")
        var pos = 0
        for (m in inlineRe.findAll(text)) {
            if (m.range.first < pos) continue
            out.append(text, pos, m.range.first)
            val g = m.groupValues
            when {
                g[2].isNotEmpty() -> span(out, g[2].trim(), TypefaceSpan("monospace"), BackgroundColorSpan(CODE_BG))
                m.groups[4] != null -> {
                    val alt = g[3]
                    if (alt.isNotEmpty()) out.append(alt)
                }
                m.groups[6] != null -> {
                    val start = out.length
                    appendInline(out, g[5], linkColor)
                    if (g[6].isNotEmpty()) {
                        out.setSpan(URLSpan(g[6]), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        if (linkColor != null) out.setSpan(ForegroundColorSpan(linkColor), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                g[8].isNotEmpty() -> {
                    val start = out.length
                    appendInline(out, g[8], linkColor)
                    out.setSpan(StyleSpan(Typeface.BOLD), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                g[10].isNotEmpty() -> {
                    val start = out.length
                    appendInline(out, g[10], linkColor)
                    out.setSpan(StrikethroughSpan(), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                g[12].isNotEmpty() -> {
                    val start = out.length
                    appendInline(out, g[12], linkColor)
                    out.setSpan(StyleSpan(Typeface.ITALIC), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                g[13].isNotEmpty() -> {
                    val url = g[13].trimEnd('.', ',', ';', ':', '!', '?')
                    val start = out.length
                    out.append(url)
                    out.setSpan(URLSpan(url), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    if (linkColor != null) out.setSpan(ForegroundColorSpan(linkColor), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    pos = m.range.first + url.length
                    continue
                }
            }
            pos = m.range.last + 1
        }
        out.append(text, pos, text.length)
    }

    private fun span(out: SpannableStringBuilder, text: String, vararg spans: Any) {
        val start = out.length
        out.append(text)
        for (s in spans) out.setSpan(s, start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
