package com.example.proyectoparcial2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private val GridColor get() = if (Palette.light) Color(0xFFE5E5EA) else Color(0xFF26262A)
private val AxisColor get() = if (Palette.light) Color(0xFF8E8E93) else Color(0xFF636366)

private fun fmt(v: Double) = CalculatorEngine.formatResult(v)
private fun snap(v: Double) = if (abs(v) < 1e-9) 0.0 else v

private fun niceStep(raw: Double): Double {
    val p = 10.0.pow(floor(log10(raw)))
    val m = raw / p
    return p * when { m < 1.5 -> 1.0; m < 3.5 -> 2.0; m < 7.5 -> 5.0; else -> 10.0 }
}

/** Turns "sin(x)" into a function of x, or null when blank/invalid. */
private fun compile(text: String): ((Double) -> Double)? {
    val e = text.trim().removePrefix("y=").removePrefix("f(x)=").removePrefix("g(x)=").trim()
    if (e.isBlank()) return null
    val syntaxError = runCatching { CalculatorEngine.evaluate(e, AngleMode.RAD, 0.731) }
        .exceptionOrNull() is IllegalArgumentException
    if (syntaxError) return null
    return { x -> try { CalculatorEngine.evaluate(e, AngleMode.RAD, x) } catch (_: Exception) { Double.NaN } }
}

@Composable
internal fun ColumnScope.GraphPanel(accent: Color, onInsert: (String) -> Unit) {
    var fx by rememberSaveable { mutableStateOf("sin(x)") }
    var gx by rememberSaveable { mutableStateOf("") }
    var showArea by rememberSaveable { mutableStateOf(false) }
    var aText by rememberSaveable { mutableStateOf("0") }
    var bText by rememberSaveable { mutableStateOf("π") }
    var cx by remember { mutableDoubleStateOf(0.0) }
    var cy by remember { mutableDoubleStateOf(0.0) }
    var spanX by remember { mutableDoubleStateOf(20.0) }
    var traceX by remember { mutableStateOf<Double?>(null) }

    val gColor = if (accent == Color(0xFF64D2FF)) Color(0xFFFF375F) else Color(0xFF64D2FF)
    val f = remember(fx) { compile(fx) }
    val g = remember(gx) { compile(gx) }
    val left = cx - spanX / 2
    val right = cx + spanX / 2
    val roots = remember(f, cx, spanX) { f?.let { fn -> findRoots(fn, left, right).map(::snap) } ?: emptyList() }
    val crossings = remember(f, g, cx, spanX) {
        if (f == null || g == null) emptyList() else findRoots({ f(it) - g(it) }, left, right).map(::snap)
    }
    val a = evalField(aText)
    val b = evalField(bText)
    val area = if (showArea && f != null && a != null && b != null) integrate(f, a, b) else null

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = TextSecondary, fontSize = 10.sp)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalcField(fx, { fx = it; traceX = null }, "f(x) =", accent, Modifier.weight(1f), KeyboardType.Ascii)
        CalcField(gx, { gx = it }, "g(x) = (opcional)", gColor, Modifier.weight(1f), KeyboardType.Ascii)
    }
    PillRow {
        listOf("sin(x)", "cos(x)", "tan(x)", "x^2", "x^3−3x", "1/x", "e^x", "ln(x)", "√(x)", "sin(x)/x")
            .forEach { p -> Pill(p, p == fx, accent) { fx = p; traceX = null } }
    }
    Box(
        Modifier.fillMaxWidth().weight(1f).padding(top = 2.dp)
            .clip(RoundedCornerShape(16.dp)).background(GraphBg)
    ) {
        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val upp = spanX / size.width
                        cx -= pan.x * upp
                        cy += pan.y * upp
                        spanX = (spanX / zoom).coerceIn(1e-3, 1e5)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { o -> traceX = cx + (o.x - size.width / 2.0) * spanX / size.width }
                }
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas
            val upp = spanX / w
            fun sx(x: Double) = ((x - cx) / upp + w / 2).toFloat()
            fun sy(y: Double) = (h / 2 - (y - cy) / upp).toFloat().coerceIn(-h * 3, h * 4)

            // Grid + tick labels (drawText needs non-negative room to the right/bottom)
            val step = niceStep(spanX / 8)
            val labelRow = sy(0.0).coerceIn(0f, h - 30f)
            val labelCol = sx(0.0).coerceIn(0f, w - 60f)
            var t = floor(left / step) * step
            while (t <= right) {
                val p = sx(t)
                drawLine(GridColor, Offset(p, 0f), Offset(p, h), 1f)
                if (abs(t) > step / 2 && p >= 0f && p < w - 60f)
                    drawText(textMeasurer, fmt(t), Offset(p + 3f, labelRow + 2f), labelStyle)
                t += step
            }
            val halfY = upp * h / 2
            t = floor((cy - halfY) / step) * step
            while (t <= cy + halfY) {
                val p = sy(t)
                drawLine(GridColor, Offset(0f, p), Offset(w, p), 1f)
                if (abs(t) > step / 2 && p >= 0f && p < h - 30f)
                    drawText(textMeasurer, fmt(t), Offset(labelCol + 3f, p + 2f), labelStyle)
                t += step
            }
            drawLine(AxisColor, Offset(0f, sy(0.0)), Offset(w, sy(0.0)), 2f)
            drawLine(AxisColor, Offset(sx(0.0), 0f), Offset(sx(0.0), h), 2f)

            // Shaded integral area
            if (area != null && f != null && a != null && b != null) {
                val lo = min(a, b); val hi = max(a, b)
                val path = Path().apply { moveTo(sx(lo), sy(0.0)) }
                for (k in 0..240) {
                    val x = lo + (hi - lo) * k / 240
                    val y = f(x)
                    path.lineTo(sx(x), if (y.isFinite()) sy(y) else sy(0.0))
                }
                path.lineTo(sx(hi), sy(0.0)); path.close()
                drawPath(path, accent.copy(alpha = 0.25f))
            }

            g?.let { drawCurve(it, gColor, cx, upp, ::sy) }
            f?.let { drawCurve(it, accent, cx, upp, ::sy) }

            roots.forEach { r -> drawMark(Offset(sx(r), sy(0.0)), accent) }
            if (g != null) crossings.forEach { x -> drawMark(Offset(sx(x), sy(g(x))), gColor) }

            val tx = traceX
            if (tx != null && f != null) {
                val tpx = sx(tx)
                drawLine(TextSecondary, Offset(tpx, 0f), Offset(tpx, h), 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                val ty = f(tx)
                if (ty.isFinite()) {
                    // Tangent line y = f(a) + f'(a)(x − a)
                    val d = derivative(f, tx)
                    if (d.isFinite()) drawLine(
                        TextPrimary.copy(alpha = 0.6f),
                        Offset(0f, sy(ty + d * (left - tx))), Offset(w, sy(ty + d * (right - tx))), 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    )
                    drawMark(Offset(tpx, sy(ty)), accent, big = true)
                }
                g?.invoke(tx)?.takeIf { it.isFinite() }?.let { drawMark(Offset(tpx, sy(it)), gColor, big = true) }
            }
        }
        if (fx.isNotBlank() && f == null) Text("f(x) no es válida", color = ErrorColor, modifier = Modifier.align(Alignment.Center))
        else if (gx.isNotBlank() && g == null) Text("g(x) no es válida", color = ErrorColor, modifier = Modifier.align(Alignment.Center))
    }

    PillRow {
        Pill("−", false, accent) { spanX = (spanX * 1.5).coerceAtMost(1e5) }
        Pill("+", false, accent) { spanX = (spanX / 1.5).coerceAtLeast(1e-3) }
        Pill("⌂ Centrar", false, accent) { cx = 0.0; cy = 0.0; spanX = 20.0; traceX = null }
        Pill("∫ Área", showArea, accent) { showArea = !showArea }
        Pill("Quitar punto", false, accent) { traceX = null }
    }
    if (showArea) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CalcField(aText, { aText = it }, "desde a", accent, Modifier.weight(1f), KeyboardType.Ascii)
            CalcField(bText, { bText = it }, "hasta b", accent, Modifier.weight(1f), KeyboardType.Ascii)
        }
        Spacer(Modifier.height(6.dp))
        ValueRow(
            "∫ f(x) dx  de a a b", if (area != null && area.isFinite()) fmt(area) else "—", accent,
            onClick = area?.takeIf { it.isFinite() }?.let { v -> { onInsert(fmt(v)) } }
        )
    }
    val tx = traceX
    if (tx != null && f != null) {
        val ty = f(tx)
        val d = derivative(f, tx)
        val parts = mutableListOf("x = ${fmt(tx)}", "f(x) = ${if (ty.isFinite()) fmt(ty) else "indefinido"}")
        if (d.isFinite() && ty.isFinite()) parts += "f′(x) = ${fmt(d)}"
        g?.invoke(tx)?.let { parts += "g(x) = ${if (it.isFinite()) fmt(it) else "indefinido"}" }
        Text(parts.joinToString("   "), color = TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    } else {
        Hint("Arrastra para mover · pellizca para zoom · toca para ver el punto, la derivada y la tangente")
    }
    if (roots.isNotEmpty() || crossings.isNotEmpty()) {
        PillRow {
            roots.take(8).forEach { r -> Pill("raíz x = ${fmt(r)}", false, accent) { onInsert(fmt(r)) } }
            crossings.take(6).forEach { x -> Pill("∩ x = ${fmt(x)}", false, accent) { onInsert(fmt(x)) } }
        }
    }
}

private fun DrawScope.drawCurve(f: (Double) -> Double, color: Color, cx: Double, upp: Double, sy: (Double) -> Float) {
    val w = size.width
    val h = size.height
    val path = Path()
    var penUp = true
    var lastY = 0f
    var px = 0f
    // Lift the pen on undefined points and on asymptote jumps (tan, 1/x)
    while (px <= w) {
        val y = f(cx + (px - w / 2) * upp)
        if (!y.isFinite()) penUp = true
        else {
            val py = sy(y)
            if (penUp || abs(py - lastY) > h * 2) path.moveTo(px, py) else path.lineTo(px, py)
            penUp = false
            lastY = py
        }
        px += 2f
    }
    drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawMark(p: Offset, color: Color, big: Boolean = false) {
    drawCircle(Color.White, (if (big) 7 else 6).dp.toPx(), p)
    drawCircle(color, (if (big) 5 else 4).dp.toPx(), p)
}
