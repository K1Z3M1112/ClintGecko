package com.simplefox.browser.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.Xml
import androidx.core.graphics.PathParser
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.Locale
import kotlin.math.max
import kotlin.math.tan

/**
 * Small in-house SVG renderer (shapes, paths, groups, `use`, transforms, solid/linear/radial
 * fills, strokes, simple CSS). Enough for favicons, tile icons and download thumbnails.
 */
class SimpleSvg private constructor(
    private val root: Node,
    private val ids: Map<String, Node>,
    private val css: List<CssRule>,
    val width: Float,
    val height: Float,
    private val viewBox: RectF
) {

    private class Node(val tag: String, val attrs: Map<String, String>, val parent: Node?) {
        val children = ArrayList<Node>()
        var text = ""
    }

    private class CssRule(val selector: String, val props: Map<String, String>)

    private class Style(
        var fill: String = "#000000",
        var stroke: String = "none",
        var strokeWidth: Float = 1f,
        var opacity: Float = 1f,
        var fillOpacity: Float = 1f,
        var strokeOpacity: Float = 1f,
        var evenOdd: Boolean = false,
        var cap: Paint.Cap = Paint.Cap.BUTT,
        var join: Paint.Join = Paint.Join.MITER,
        var miter: Float = 4f,
        var color: String = "#000000",
        var stopColor: String = "#000000",
        var stopOpacity: Float = 1f
    ) {
        fun copy() = Style(
            fill, stroke, strokeWidth, opacity, fillOpacity, strokeOpacity, evenOdd,
            cap, join, miter, color, stopColor, stopOpacity
        )
    }

    /** Draws the image scaled to fit inside [0,destW]x[0,destH], keeping the aspect ratio. */
    fun render(canvas: Canvas, destW: Float, destH: Float) {
        val vw = viewBox.width().takeIf { it > 0f } ?: width
        val vh = viewBox.height().takeIf { it > 0f } ?: height
        if (vw <= 0f || vh <= 0f) return
        val scale = minOf(destW / vw, destH / vh)
        val m = Matrix()
        m.postTranslate(-viewBox.left, -viewBox.top)
        m.postScale(scale, scale)
        m.postTranslate((destW - vw * scale) / 2f, (destH - vh * scale) / 2f)
        canvas.save()
        canvas.clipRect(0f, 0f, destW, destH)
        draw(canvas, root, m, Style(), 0)
        canvas.restore()
    }

    /** Renders into a new bitmap whose longest side is [maxSide] (aspect ratio preserved). */
    fun toBitmap(maxSide: Int): Bitmap {
        val ratio = if (height > 0f) width / height else 1f
        val w = if (ratio >= 1f) maxSide else (maxSide * ratio).toInt()
        val h = if (ratio >= 1f) (maxSide / ratio).toInt() else maxSide
        val bitmap = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        render(Canvas(bitmap), bitmap.width.toFloat(), bitmap.height.toFloat())
        return bitmap
    }

    // ---- drawing ----------------------------------------------------------------------------

    private fun draw(canvas: Canvas, node: Node, parentMatrix: Matrix, parentStyle: Style, depth: Int) {
        if (depth > 24) return
        val attrs = resolvedAttrs(node)
        if (attrs["display"] == "none" || attrs["visibility"] == "hidden") return
        when (node.tag) {
            "defs", "style", "title", "desc", "metadata", "linearGradient", "radialGradient",
            "clipPath", "mask", "pattern", "symbol", "marker", "filter" -> return
        }

        val style = parentStyle.copy().also { it.opacity = 1f; applyStyle(it, attrs) }
        val matrix = Matrix(parentMatrix)
        attrs["transform"]?.let { matrix.preConcat(parseTransform(it)) }

        val opacity = style.opacity
        val needsLayer = opacity < 0.999f && node.tag != "svg"
        if (needsLayer) {
            canvas.saveLayerAlpha(null, (opacity * 255).toInt().coerceIn(0, 255))
            style.opacity = 1f
        }

        when (node.tag) {
            "svg", "g", "a", "switch" -> {
                val childMatrix = if (node.tag == "svg" && node !== root) {
                    Matrix(matrix).also { it.preTranslate(num(attrs["x"], 0f), num(attrs["y"], 0f)) }
                } else matrix
                for (child in node.children) draw(canvas, child, childMatrix, style, depth + 1)
            }
            "use" -> {
                val ref = (attrs["href"] ?: attrs["xlink:href"])?.removePrefix("#")
                val target = ref?.let { ids[it] }
                if (target != null) {
                    val m = Matrix(matrix)
                    m.preTranslate(num(attrs["x"], 0f), num(attrs["y"], 0f))
                    draw(canvas, target, m, style, depth + 1)
                }
            }
            else -> shapePath(node.tag, attrs)?.let { path ->
                path.fillType = if (style.evenOdd) Path.FillType.EVEN_ODD else Path.FillType.WINDING
                path.transform(matrix)
                paintShape(canvas, path, style, matrix)
            }
        }
        if (needsLayer) canvas.restore()
    }

    private fun paintShape(canvas: Canvas, path: Path, st: Style, matrix: Matrix) {
        val bounds = RectF()
        path.computeBounds(bounds, true)
        if (st.fill != "none") {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.FILL }
            if (applyPaint(paint, st.fill, st.fillOpacity, st, bounds)) canvas.drawPath(path, paint)
        }
        if (st.stroke != "none" && st.strokeWidth > 0f) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeCap = st.cap
                strokeJoin = st.join
                strokeMiter = st.miter
                strokeWidth = st.strokeWidth * matrixScale(matrix)
            }
            if (applyPaint(paint, st.stroke, st.strokeOpacity, st, bounds)) canvas.drawPath(path, paint)
        }
    }

    private fun matrixScale(m: Matrix): Float {
        val v = FloatArray(9)
        m.getValues(v)
        val sx = Math.hypot(v[Matrix.MSCALE_X].toDouble(), v[Matrix.MSKEW_Y].toDouble()).toFloat()
        val sy = Math.hypot(v[Matrix.MSKEW_X].toDouble(), v[Matrix.MSCALE_Y].toDouble()).toFloat()
        return (sx + sy) / 2f
    }

    private fun applyPaint(paint: Paint, value: String, alpha: Float, style: Style, bounds: RectF): Boolean {
        val v = value.trim()
        if (v.startsWith("url(")) {
            val id = v.substringAfter("#").substringBefore(")").trim().trim('\'', '"')
            val grad = ids[id] ?: return false
            val shader = gradientShader(grad, bounds, style) ?: return false
            paint.shader = shader
            paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            return true
        }
        val color = parseColor(if (v == "currentColor") style.color else v) ?: return false
        val a = (Color.alpha(color) * alpha).toInt().coerceIn(0, 255)
        paint.color = Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
        return true
    }

    private fun gradientShader(node: Node, bounds: RectF, style: Style): Shader? {
        if (node.tag != "linearGradient" && node.tag != "radialGradient") return null
        var stopsNode = node
        var guard = 0
        while (stopsNode.children.none { it.tag == "stop" } && guard++ < 8) {
            val ref = (stopsNode.attrs["href"] ?: stopsNode.attrs["xlink:href"])?.removePrefix("#") ?: break
            stopsNode = ids[ref] ?: break
        }
        val stops = stopsNode.children.filter { it.tag == "stop" }
        if (stops.isEmpty()) return null
        val colors = IntArray(stops.size)
        val positions = FloatArray(stops.size)
        var last = 0f
        stops.forEachIndexed { i, stop ->
            val a = resolvedAttrs(stop)
            val st = Style().also { it.color = style.color }
            applyStyle(st, a)
            val base = parseColor(if (st.stopColor == "currentColor") st.color else st.stopColor) ?: Color.BLACK
            colors[i] = Color.argb(
                (Color.alpha(base) * st.stopOpacity).toInt().coerceIn(0, 255),
                Color.red(base), Color.green(base), Color.blue(base)
            )
            val raw = a["offset"]?.trim()
            val off = when {
                raw == null -> last
                raw.endsWith("%") -> (raw.dropLast(1).toFloatOrNull() ?: 0f) / 100f
                else -> raw.toFloatOrNull() ?: 0f
            }
            last = max(last, off.coerceIn(0f, 1f))
            positions[i] = last
        }
        val userSpace = node.attrs["gradientUnits"] == "userSpaceOnUse"
        val local = Matrix()
        if (!userSpace) {
            local.postScale(bounds.width().coerceAtLeast(0.0001f), bounds.height().coerceAtLeast(0.0001f))
            local.postTranslate(bounds.left, bounds.top)
        }
        node.attrs["gradientTransform"]?.let { local.preConcat(parseTransform(it)) }
        fun frac(key: String, def: Float): Float {
            val raw = node.attrs[key]?.trim() ?: return def
            return if (raw.endsWith("%")) (raw.dropLast(1).toFloatOrNull() ?: 0f) / 100f * (if (userSpace) viewBox.width() else 1f)
            else num(raw, def)
        }
        val shader: Shader = if (node.tag == "linearGradient") {
            LinearGradient(
                frac("x1", 0f), frac("y1", 0f), frac("x2", 1f), frac("y2", 0f),
                colors, positions, Shader.TileMode.CLAMP
            )
        } else {
            val r = frac("r", 0.5f).coerceAtLeast(0.0001f)
            RadialGradient(frac("cx", 0.5f), frac("cy", 0.5f), r, colors, positions, Shader.TileMode.CLAMP)
        }
        shader.setLocalMatrix(local)
        return shader
    }

    private fun shapePath(tag: String, a: Map<String, String>): Path? {
        val p = Path()
        when (tag) {
            "path" -> return a["d"]?.let { PathParser.createPathFromPathData(it) }
            "rect" -> {
                val x = num(a["x"], 0f); val y = num(a["y"], 0f)
                val w = num(a["width"], 0f); val h = num(a["height"], 0f)
                if (w <= 0f || h <= 0f) return null
                var rx = num(a["rx"], -1f); var ry = num(a["ry"], -1f)
                if (rx < 0f && ry < 0f) { rx = 0f; ry = 0f }
                else if (rx < 0f) rx = ry else if (ry < 0f) ry = rx
                rx = rx.coerceAtMost(w / 2f); ry = ry.coerceAtMost(h / 2f)
                p.addRoundRect(RectF(x, y, x + w, y + h), rx, ry, Path.Direction.CW)
            }
            "circle" -> {
                val r = num(a["r"], 0f)
                if (r <= 0f) return null
                p.addCircle(num(a["cx"], 0f), num(a["cy"], 0f), r, Path.Direction.CW)
            }
            "ellipse" -> {
                val rx = num(a["rx"], 0f); val ry = num(a["ry"], 0f)
                if (rx <= 0f || ry <= 0f) return null
                val cx = num(a["cx"], 0f); val cy = num(a["cy"], 0f)
                p.addOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), Path.Direction.CW)
            }
            "line" -> {
                p.moveTo(num(a["x1"], 0f), num(a["y1"], 0f))
                p.lineTo(num(a["x2"], 0f), num(a["y2"], 0f))
            }
            "polyline", "polygon" -> {
                val pts = numberList(a["points"] ?: return null)
                if (pts.size < 4) return null
                p.moveTo(pts[0], pts[1])
                var i = 2
                while (i + 1 < pts.size) { p.lineTo(pts[i], pts[i + 1]); i += 2 }
                if (tag == "polygon") p.close()
            }
            else -> return null
        }
        return p
    }

    // ---- attributes / style -----------------------------------------------------------------

    private fun resolvedAttrs(node: Node): Map<String, String> {
        val out = HashMap<String, String>(node.attrs)
        val classes = node.attrs["class"]?.split(' ', '\t', '\n')?.filter { it.isNotBlank() }.orEmpty()
        val id = node.attrs["id"]
        for (rule in css) {
            val sel = rule.selector
            val hit = sel == "*" || sel == node.tag ||
                (sel.startsWith(".") && sel.substring(1) in classes) ||
                (sel.startsWith("#") && sel.substring(1) == id) ||
                (sel.contains('.') && !sel.startsWith(".") &&
                    sel.substringBefore('.') == node.tag && sel.substringAfter('.') in classes)
            if (hit) out.putAll(rule.props)
        }
        node.attrs["style"]?.let { out.putAll(parseDeclarations(it)) }
        return out
    }

    private fun applyStyle(s: Style, a: Map<String, String>) {
        a["color"]?.let { s.color = it }
        a["fill"]?.let { s.fill = it }
        a["stroke"]?.let { s.stroke = it }
        a["stroke-width"]?.let { s.strokeWidth = num(it, 1f) }
        a["opacity"]?.let { s.opacity = s.opacity * (it.trim().toFloatOrNull() ?: 1f).coerceIn(0f, 1f) }
        a["fill-opacity"]?.let { s.fillOpacity = (it.trim().toFloatOrNull() ?: 1f).coerceIn(0f, 1f) }
        a["stroke-opacity"]?.let { s.strokeOpacity = (it.trim().toFloatOrNull() ?: 1f).coerceIn(0f, 1f) }
        a["fill-rule"]?.let { s.evenOdd = it.trim() == "evenodd" }
        a["stroke-linecap"]?.let {
            s.cap = when (it.trim()) { "round" -> Paint.Cap.ROUND; "square" -> Paint.Cap.SQUARE; else -> Paint.Cap.BUTT }
        }
        a["stroke-linejoin"]?.let {
            s.join = when (it.trim()) { "round" -> Paint.Join.ROUND; "bevel" -> Paint.Join.BEVEL; else -> Paint.Join.MITER }
        }
        a["stroke-miterlimit"]?.let { s.miter = it.trim().toFloatOrNull() ?: 4f }
        a["stop-color"]?.let { s.stopColor = it }
        a["stop-opacity"]?.let { s.stopOpacity = (it.trim().toFloatOrNull() ?: 1f).coerceIn(0f, 1f) }
    }

    companion object {

        fun parse(text: String): SimpleSvg? = runCatching {
            if (!text.contains("<svg", ignoreCase = true)) return null
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(text))
            var root: Node? = null
            var current: Node? = null
            val ids = HashMap<String, Node>()
            val css = ArrayList<CssRule>()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val attrs = HashMap<String, String>()
                        for (i in 0 until parser.attributeCount) attrs[parser.getAttributeName(i)] = parser.getAttributeValue(i)
                        val node = Node(parser.name.substringAfter(':'), attrs, current)
                        current?.children?.add(node)
                        attrs["id"]?.let { ids[it] = node }
                        if (root == null) root = node
                        current = node
                    }
                    XmlPullParser.TEXT -> current?.let { it.text += parser.text }
                    XmlPullParser.END_TAG -> {
                        val node = current
                        if (node != null && node.tag == "style") css.addAll(parseCss(node.text))
                        current = node?.parent
                    }
                }
                event = parser.next()
            }
            val svg = root?.takeIf { it.tag == "svg" } ?: return null
            val vbParts = svg.attrs["viewBox"]?.let { numberList(it) }
            val vb = if (vbParts != null && vbParts.size == 4 && vbParts[2] > 0f && vbParts[3] > 0f)
                RectF(vbParts[0], vbParts[1], vbParts[0] + vbParts[2], vbParts[1] + vbParts[3]) else RectF()
            val w = svg.attrs["width"]?.takeIf { !it.trim().endsWith("%") }?.let { num(it, 0f) } ?: 0f
            val h = svg.attrs["height"]?.takeIf { !it.trim().endsWith("%") }?.let { num(it, 0f) } ?: 0f
            val width = if (w > 0f) w else if (vb.width() > 0f) vb.width() else 0f
            val height = if (h > 0f) h else if (vb.height() > 0f) vb.height() else 0f
            if (width <= 0f || height <= 0f) return null
            if (vb.width() <= 0f) vb.set(0f, 0f, width, height)
            SimpleSvg(svg, ids, css, width, height, vb)
        }.getOrNull()

        fun toBitmap(text: String, maxSide: Int): Bitmap? = parse(text)?.toBitmap(maxSide)

        private fun num(raw: String?, def: Float): Float {
            if (raw == null) return def
            val s = raw.trim()
            if (s.isEmpty()) return def
            var end = s.length
            while (end > 0 && !(s[end - 1].isDigit() || s[end - 1] == '.')) end--
            val value = s.substring(0, end).toFloatOrNull() ?: return def
            return when (s.substring(end).lowercase(Locale.US)) {
                "pt" -> value * 4f / 3f
                "pc" -> value * 16f
                "mm" -> value * 3.7795276f
                "cm" -> value * 37.795276f
                "in" -> value * 96f
                else -> value
            }
        }

        private fun numberList(raw: String): List<Float> =
            Regex("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?").findAll(raw).mapNotNull { it.value.toFloatOrNull() }.toList()

        private fun parseDeclarations(raw: String): Map<String, String> {
            val out = HashMap<String, String>()
            raw.split(';').forEach { decl ->
                val i = decl.indexOf(':')
                if (i > 0) out[decl.substring(0, i).trim()] = decl.substring(i + 1).replace("!important", "").trim()
            }
            return out
        }

        private fun parseCss(raw: String): List<CssRule> {
            val out = ArrayList<CssRule>()
            val clean = raw.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            Regex("([^{}]+)\\{([^{}]*)\\}").findAll(clean).forEach { m ->
                val props = parseDeclarations(m.groupValues[2])
                m.groupValues[1].split(',').map { it.trim() }.filter { it.isNotEmpty() && !it.contains(' ') && !it.contains('@') }
                    .forEach { out.add(CssRule(it, props)) }
            }
            return out
        }

        private fun parseTransform(raw: String): Matrix {
            val m = Matrix()
            Regex("(\\w+)\\s*\\(([^)]*)\\)").findAll(raw).forEach { match ->
                val v = numberList(match.groupValues[2])
                val t = Matrix()
                when (match.groupValues[1]) {
                    "matrix" -> if (v.size >= 6) t.setValues(
                        floatArrayOf(v[0], v[2], v[4], v[1], v[3], v[5], 0f, 0f, 1f)
                    )
                    "translate" -> t.setTranslate(v.getOrElse(0) { 0f }, v.getOrElse(1) { 0f })
                    "scale" -> t.setScale(v.getOrElse(0) { 1f }, v.getOrElse(1) { v.getOrElse(0) { 1f } })
                    "rotate" -> if (v.size >= 3) t.setRotate(v[0], v[1], v[2]) else t.setRotate(v.getOrElse(0) { 0f })
                    "skewX" -> t.setSkew(tan(Math.toRadians(v.getOrElse(0) { 0f }.toDouble())).toFloat(), 0f)
                    "skewY" -> t.setSkew(0f, tan(Math.toRadians(v.getOrElse(0) { 0f }.toDouble())).toFloat())
                }
                m.preConcat(t)
            }
            return m
        }

        private fun parseColor(raw: String): Int? {
            val s = raw.trim().lowercase(Locale.US)
            if (s.isEmpty() || s == "none" || s == "transparent") return null
            if (s.startsWith("#")) {
                val hex = s.substring(1)
                return runCatching {
                    when (hex.length) {
                        3 -> Color.rgb(
                            hex.substring(0, 1).repeat(2).toInt(16),
                            hex.substring(1, 2).repeat(2).toInt(16),
                            hex.substring(2, 3).repeat(2).toInt(16)
                        )
                        4 -> Color.argb(
                            hex.substring(3, 4).repeat(2).toInt(16),
                            hex.substring(0, 1).repeat(2).toInt(16),
                            hex.substring(1, 2).repeat(2).toInt(16),
                            hex.substring(2, 3).repeat(2).toInt(16)
                        )
                        6 -> Color.parseColor("#$hex")
                        8 -> Color.argb(
                            hex.substring(6, 8).toInt(16), hex.substring(0, 2).toInt(16),
                            hex.substring(2, 4).toInt(16), hex.substring(4, 6).toInt(16)
                        )
                        else -> null
                    }
                }.getOrNull()
            }
            if (s.startsWith("rgb")) {
                val parts = Regex("[-+]?\\d*\\.?\\d+%?").findAll(s.substringAfter('(')).map { it.value }.toList()
                if (parts.size < 3) return null
                fun ch(p: String) = (if (p.endsWith("%")) (p.dropLast(1).toFloat() * 2.55f) else p.toFloat()).toInt().coerceIn(0, 255)
                val a = parts.getOrNull(3)?.let { p ->
                    ((if (p.endsWith("%")) p.dropLast(1).toFloat() / 100f else p.toFloat()) * 255).toInt().coerceIn(0, 255)
                } ?: 255
                return Color.argb(a, ch(parts[0]), ch(parts[1]), ch(parts[2]))
            }
            return runCatching { Color.parseColor(s) }.getOrNull()
        }
    }
}
