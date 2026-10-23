package com.example.proyectoparcial2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

internal enum class Tool(val chip: String, val title: String) {
    CONST("π  Constantes", "Constantes"),
    UNITS("⇄  Unidades", "Conversor de unidades"),
    STATS("σ  Estadística", "Estadística"),
    BASES("#  Bases", "Conversor de bases"),
    GRAPH("∿  Gráfica", "Graficadora"),
    EQ("x²  Ecuaciones", "Ecuaciones"),
    SETTINGS("⚙  Ajustes", "Ajustes")
}

private fun fmt(v: Double) = CalculatorEngine.formatResult(v)

/** Field values accept expressions too ("2/3", "pi", "1,5"). */
internal fun evalField(s: String): Double? =
    if (s.isBlank()) null
    else runCatching { CalculatorEngine.evaluate(s.replace(',', '.'), AngleMode.DEG) }.getOrNull()

// ── Shared building blocks ────────────────────────────────────────────────────

@Composable
internal fun PanelScaffold(title: String, accent: Color, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(BgColor).systemBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Text("←", color = accent, fontSize = 24.sp)
            }
            Text(title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp))
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(accent.copy(alpha = 0.5f)))
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp), content = content)
    }
}

@Composable
internal fun Pill(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Text(
        text, color = if (selected) onAccentOf(accent) else TextPrimary, fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (selected) accent else FuncColor)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
internal fun PillRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
internal fun CalcField(
    value: String, onValueChange: (String) -> Unit, label: String, accent: Color,
    modifier: Modifier = Modifier.fillMaxWidth(), keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value, onValueChange, modifier,
        label = { Text(label) }, singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = false),
        textStyle = TextStyle(fontSize = 18.sp, color = TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent, unfocusedBorderColor = ClearColor,
            focusedLabelColor = accent, unfocusedLabelColor = TextSecondary,
            cursorColor = accent, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
        )
    )
}

@Composable
internal fun ValueRow(label: String, value: String, accent: Color, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FuncColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
        if (onClick != null) Text("  ›", color = accent, fontSize = 18.sp)
    }
}

@Composable
private fun SectionTitle(text: String, accent: Color) {
    Text(text, color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
}

// ── Constants ─────────────────────────────────────────────────────────────────

@Composable
internal fun ColumnScope.ConstantsPanel(accent: Color, onInsert: (String) -> Unit, onCopy: (String) -> Unit) {
    Hint("Toca para insertar en la calculadora · mantén presionado para copiar")
    LazyColumn(
        Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        CONSTANT_GROUPS.forEach { (group, list) ->
            item(key = group) { SectionTitle(group, accent) }
            items(list, key = { it.name }) { c ->
                val insertText = if (c.symbol in setOf("π", "e", "φ")) c.symbol else plainNumber(c.value)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FuncColor)
                        .combinedClickable(onClick = { onInsert(insertText) }, onLongClick = { onCopy(plainNumber(c.value)) })
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Text(c.symbol, color = accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(c.name, color = TextPrimary, fontSize = 15.sp)
                        if (c.unit.isNotEmpty()) Text(c.unit, color = TextSecondary, fontSize = 12.sp)
                    }
                    Text(fmt(c.value), color = TextPrimary, fontSize = 14.sp)
                }
            }
        }
    }
}

// ── Unit converter ────────────────────────────────────────────────────────────

@Composable
internal fun ColumnScope.UnitsPanel(accent: Color, initial: Double?, onInsert: (String) -> Unit) {
    var cat by rememberSaveable { mutableIntStateOf(0) }
    var from by rememberSaveable { mutableIntStateOf(0) }
    var input by rememberSaveable { mutableStateOf(initial?.let { plainNumber(it) } ?: "1") }
    val category = UNIT_CATEGORIES[cat]
    val fromUnit = category.units[from.coerceIn(0, category.units.lastIndex)]
    val v = evalField(input)

    PillRow { UNIT_CATEGORIES.forEachIndexed { i, c -> Pill(c.name, i == cat, accent) { cat = i; from = 0 } } }
    CalcField(input, { input = it }, "Valor en ${fromUnit.name} (${fromUnit.symbol})", accent,
        keyboardType = KeyboardType.Decimal)
    Hint("Convertir desde:")
    PillRow { category.units.forEachIndexed { i, u -> Pill(u.symbol, i == from, accent) { from = i } } }
    LazyColumn(
        Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (v == null) item { Hint("Ingresa un valor válido") }
        else items(category.units.filter { it !== fromUnit }) { u ->
            val r = fmt(convertUnit(v, fromUnit, u))
            ValueRow(u.name, "$r ${u.symbol}", accent) { onInsert(r) }
        }
    }
}

// ── Statistics ────────────────────────────────────────────────────────────────

@Composable
internal fun ColumnScope.StatsPanel(accent: Color, onInsert: (String) -> Unit) {
    var data by rememberSaveable { mutableStateOf("") }
    val st = computeStats(parseNumberList(data))

    CalcField(data, { data = it }, "Datos (separados por comas o espacios)", accent, singleLine = false)
    PillRow {
        Pill("Ejemplo", false, accent) { data = "4, 8, 15, 16, 23, 42" }
        Pill("Limpiar", false, accent) { data = "" }
    }
    LazyColumn(
        Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (st == null) item { Hint("Escribe números para calcular sus estadísticas. Toca un resultado para insertarlo.") }
        else {
            val rows = listOf(
                "Cantidad (n)" to st.n.toDouble(),
                "Suma Σx" to st.sum,
                "Suma de cuadrados Σx²" to st.sumSq,
                "Media x̄" to st.mean,
                "Mediana" to st.median,
                "Mínimo" to st.min,
                "Máximo" to st.max,
                "Rango" to st.max - st.min,
                "Varianza σ² (población)" to st.varPop,
                "Varianza s² (muestra)" to st.varSample,
                "Desv. estándar σ (población)" to st.sdPop,
                "Desv. estándar s (muestra)" to st.sdSample
            )
            items(rows) { (label, value) ->
                ValueRow(label, if (value.isFinite()) fmt(value) else "—", accent,
                    onClick = { onInsert(fmt(value)) }.takeIf { value.isFinite() })
            }
            item {
                ValueRow("Moda", if (st.modes.isEmpty()) "Sin moda" else st.modes.joinToString(", ") { fmt(it) }, accent)
            }
        }
    }
}

// ── Number bases ──────────────────────────────────────────────────────────────

private val RADIXES = listOf(Triple("DEC", 10, "Decimal"), Triple("HEX", 16, "Hexadecimal"),
    Triple("OCT", 8, "Octal"), Triple("BIN", 2, "Binario"))
private const val DIGITS = "0123456789ABCDEF"

private fun groupBits(s: String): String {
    val neg = s.startsWith("-")
    val grouped = s.removePrefix("-").reversed().chunked(4).joinToString(" ").reversed()
    return if (neg) "-$grouped" else grouped
}

@Composable
internal fun ColumnScope.BasesPanel(accent: Color, initial: Long?, onInsert: (String) -> Unit, onCopy: (String) -> Unit) {
    var radixIdx by rememberSaveable { mutableIntStateOf(0) }
    var input by rememberSaveable { mutableStateOf(initial?.toString() ?: "255") }
    val radix = RADIXES[radixIdx].second
    val value = input.toLongOrNull(radix)

    PillRow {
        RADIXES.forEachIndexed { i, (code, r, _) ->
            Pill(code, i == radixIdx, accent) {
                input = value?.let { java.lang.Long.toString(it, r).uppercase() } ?: ""
                radixIdx = i
            }
        }
    }
    CalcField(
        input,
        { s ->
            val allowed = DIGITS.take(radix)
            input = s.uppercase().filterIndexed { idx, c -> c in allowed || (idx == 0 && c == '-') }
        },
        "Número en ${RADIXES[radixIdx].third}", accent,
        keyboardType = if (radix == 16) KeyboardType.Ascii else KeyboardType.Number
    )
    Column(
        Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (value == null) Hint("Ingresa un número entero válido")
        else {
            Hint("Toca un valor para copiarlo")
            RADIXES.forEach { (code, r, name) ->
                val s = java.lang.Long.toString(value, r).uppercase().let { if (r == 2) groupBits(it) else it }
                ValueRow("$name ($code)", s, accent) { onCopy(s.replace(" ", "")) }
            }
            val bits = if (value == 0L) 1 else 64 - java.lang.Long.numberOfLeadingZeros(abs(value))
            ValueRow("Bits necesarios", "$bits", accent)
            Pill("Insertar decimal en la calculadora", false, accent) { onInsert(value.toString()) }
        }
    }
}

// ── Graph ─────────────────────────────────────────────────────────────────────

private val GridColor = Color(0xFF26262A)
private val AxisColor = Color(0xFF636366)

private fun niceStep(raw: Double): Double {
    val p = 10.0.pow(floor(log10(raw)))
    val m = raw / p
    return p * when { m < 1.5 -> 1.0; m < 3.5 -> 2.0; m < 7.5 -> 5.0; else -> 10.0 }
}

@Composable
internal fun ColumnScope.GraphPanel(accent: Color, onInsert: (String) -> Unit) {
    var fx by rememberSaveable { mutableStateOf("sin(x)") }
    var cx by remember { mutableDoubleStateOf(0.0) }
    var cy by remember { mutableDoubleStateOf(0.0) }
    var spanX by remember { mutableDoubleStateOf(20.0) }
    var traceX by remember { mutableStateOf<Double?>(null) }

    val expr = fx.trim().removePrefix("y=").removePrefix("f(x)=").trim()
    val invalid = remember(expr) {
        expr.isBlank() || runCatching { CalculatorEngine.evaluate(expr, AngleMode.RAD, 0.731) }
            .exceptionOrNull() is IllegalArgumentException
    }
    val f: (Double) -> Double = remember(expr) {
        { x -> try { CalculatorEngine.evaluate(expr, AngleMode.RAD, x) } catch (_: Exception) { Double.NaN } }
    }
    val roots = remember(expr, invalid, cx, spanX) {
        if (invalid) emptyList()
        else findRoots(f, cx - spanX / 2, cx + spanX / 2).map { if (abs(it) < 1e-9) 0.0 else it }
    }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = TextSecondary, fontSize = 10.sp)

    CalcField(fx, { fx = it; traceX = null }, "f(x) =", accent, keyboardType = KeyboardType.Ascii)
    PillRow {
        listOf("sin(x)", "cos(x)", "tan(x)", "x^2", "x^3−3x", "1/x", "e^x", "ln(x)", "√(x)", "sin(x)/x")
            .forEach { p -> Pill(p, p == fx, accent) { fx = p; traceX = null } }
    }
    Box(
        Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp)
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFF111113))
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
            fun sy(y: Double) = (h / 2 - (y - cy) / upp).toFloat()

            // Grid + tick labels (drawText needs non-negative room to the right/bottom)
            val step = niceStep(spanX / 8)
            val labelRow = sy(0.0).coerceIn(0f, h - 30f)
            val labelCol = sx(0.0).coerceIn(0f, w - 60f)
            var g = floor((cx - spanX / 2) / step) * step
            while (g <= cx + spanX / 2) {
                val p = sx(g)
                drawLine(GridColor, Offset(p, 0f), Offset(p, h), 1f)
                if (abs(g) > step / 2 && p >= 0f && p < w - 60f)
                    drawText(textMeasurer, fmt(g), Offset(p + 3f, labelRow + 2f), labelStyle)
                g += step
            }
            val halfY = upp * h / 2
            g = floor((cy - halfY) / step) * step
            while (g <= cy + halfY) {
                val p = sy(g)
                drawLine(GridColor, Offset(0f, p), Offset(w, p), 1f)
                if (abs(g) > step / 2 && p >= 0f && p < h - 30f)
                    drawText(textMeasurer, fmt(g), Offset(labelCol + 3f, p + 2f), labelStyle)
                g += step
            }
            drawLine(AxisColor, Offset(0f, sy(0.0)), Offset(w, sy(0.0)), 2f)
            drawLine(AxisColor, Offset(sx(0.0), 0f), Offset(sx(0.0), h), 2f)
            if (invalid) return@Canvas

            // Curve: lift the pen on undefined points and on asymptote jumps
            val path = Path()
            var penUp = true
            var lastY = 0f
            var px = 0f
            while (px <= w) {
                val y = f(cx + (px - w / 2) * upp)
                if (!y.isFinite()) penUp = true
                else {
                    val py = sy(y).coerceIn(-h * 3, h * 4)
                    if (penUp || abs(py - lastY) > h * 2) path.moveTo(px, py) else path.lineTo(px, py)
                    penUp = false
                    lastY = py
                }
                px += 2f
            }
            drawPath(path, accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            roots.forEach { r ->
                val p = Offset(sx(r), sy(0.0))
                drawCircle(Color.White, 6.dp.toPx(), p)
                drawCircle(accent, 4.dp.toPx(), p)
            }
            traceX?.let { tx ->
                val tpx = sx(tx)
                drawLine(accent.copy(alpha = 0.5f), Offset(tpx, 0f), Offset(tpx, h), 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                val ty = f(tx)
                if (ty.isFinite()) {
                    drawCircle(Color.White, 7.dp.toPx(), Offset(tpx, sy(ty)))
                    drawCircle(accent, 5.dp.toPx(), Offset(tpx, sy(ty)))
                }
            }
        }
        if (invalid) Text("Escribe una función de x válida", color = ErrorColor,
            modifier = Modifier.align(Alignment.Center))
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("−  Alejar", false, accent) { spanX = (spanX * 1.5).coerceAtMost(1e5) }
        Pill("+  Acercar", false, accent) { spanX = (spanX / 1.5).coerceAtLeast(1e-3) }
        Pill("⌂  Centrar", false, accent) { cx = 0.0; cy = 0.0; spanX = 20.0; traceX = null }
    }
    val tx = traceX
    Text(
        if (tx != null && !invalid) {
            val ty = f(tx)
            "Punto: x = ${fmt(tx)},  f(x) = ${if (ty.isFinite()) fmt(ty) else "indefinido"}"
        } else "Arrastra para mover · pellizca para zoom · toca para ver un punto",
        color = if (tx != null) TextPrimary else TextSecondary, fontSize = 13.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
    if (roots.isNotEmpty()) {
        Hint("Raíces visibles (toca para insertar):")
        PillRow { roots.take(10).forEach { r -> Pill("x = ${fmt(r)}", false, accent) { onInsert(fmt(r)) } } }
    }
}

// ── Equations ─────────────────────────────────────────────────────────────────

@Composable
internal fun ColumnScope.EquationsPanel(accent: Color, onInsert: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    PillRow {
        Pill("Cuadrática", tab == 0, accent) { tab = 0 }
        Pill("Sistema 2×2", tab == 1, accent) { tab = 1 }
    }
    Column(
        Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val insertIfNumber: (String) -> (() -> Unit)? = { s -> { onInsert(s) }.takeIf { evalField(s) != null } }
        if (tab == 0) {
            var a by rememberSaveable { mutableStateOf("1") }
            var b by rememberSaveable { mutableStateOf("-3") }
            var c by rememberSaveable { mutableStateOf("2") }
            Text("a·x² + b·x + c = 0", color = accent, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcField(a, { a = it }, "a", accent, Modifier.weight(1f), KeyboardType.Decimal)
                CalcField(b, { b = it }, "b", accent, Modifier.weight(1f), KeyboardType.Decimal)
                CalcField(c, { c = it }, "c", accent, Modifier.weight(1f), KeyboardType.Decimal)
            }
            val av = evalField(a); val bv = evalField(b); val cv = evalField(c)
            if (av == null || bv == null || cv == null) Hint("Completa a, b y c (acepta expresiones como 1/2 o √(2))")
            else {
                solveQuadratic(av, bv, cv).forEach { (label, value) -> ValueRow(label, value, accent, insertIfNumber(value)) }
                if (av != 0.0) {
                    val d = bv * bv - 4 * av * cv
                    val vx = -bv / (2 * av)
                    val vy = av * vx * vx + bv * vx + cv
                    ValueRow("Discriminante Δ", fmt(d), accent) { onInsert(fmt(d)) }
                    ValueRow("Vértice", "(${fmt(vx)}, ${fmt(vy)})", accent)
                    ValueRow("La parábola abre hacia", if (av > 0) "Arriba ∪" else "Abajo ∩", accent)
                }
            }
        } else {
            var a1 by rememberSaveable { mutableStateOf("2") }
            var b1 by rememberSaveable { mutableStateOf("1") }
            var c1 by rememberSaveable { mutableStateOf("5") }
            var a2 by rememberSaveable { mutableStateOf("1") }
            var b2 by rememberSaveable { mutableStateOf("-1") }
            var c2 by rememberSaveable { mutableStateOf("1") }
            Text("a₁x + b₁y = c₁\na₂x + b₂y = c₂", color = accent, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcField(a1, { a1 = it }, "a₁", accent, Modifier.weight(1f), KeyboardType.Decimal)
                CalcField(b1, { b1 = it }, "b₁", accent, Modifier.weight(1f), KeyboardType.Decimal)
                CalcField(c1, { c1 = it }, "c₁", accent, Modifier.weight(1f), KeyboardType.Decimal)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcField(a2, { a2 = it }, "a₂", accent, Modifier.weight(1f), KeyboardType.Decimal)
                CalcField(b2, { b2 = it }, "b₂", accent, Modifier.weight(1f), KeyboardType.Decimal)
                CalcField(c2, { c2 = it }, "c₂", accent, Modifier.weight(1f), KeyboardType.Decimal)
            }
            val vals = listOf(a1, b1, c1, a2, b2, c2).map(::evalField)
            if (vals.any { it == null }) Hint("Completa los seis coeficientes")
            else {
                val (va1, vb1, vc1, va2, vb2) = vals.map { it!! }
                val sol = solveLinear2(va1, vb1, vc1, va2, vb2, vals[5]!!)
                if (sol == null) ValueRow("Resultado", "Sin solución única", accent)
                else {
                    ValueRow("x", fmt(sol.first), accent) { onInsert(fmt(sol.first)) }
                    ValueRow("y", fmt(sol.second), accent) { onInsert(fmt(sol.second)) }
                }
            }
        }
    }
}

// ── Settings ──────────────────────────────────────────────────────────────────

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit, accent: Color) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FuncColor)
            .clickable { onChange(!checked) }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 16.sp)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = accent,
                uncheckedThumbColor = TextSecondary, uncheckedTrackColor = NumColor, uncheckedBorderColor = ClearColor
            )
        )
    }
}

@Composable
internal fun ColumnScope.SettingsPanel(
    themeIndex: Int, onTheme: (Int) -> Unit,
    soundOn: Boolean, onSound: (Boolean) -> Unit,
    hapticOn: Boolean, onHaptic: (Boolean) -> Unit,
    fractionOn: Boolean, onFraction: (Boolean) -> Unit,
    historyCount: Int, onClearHistory: () -> Unit
) {
    val accent = THEMES[themeIndex].accent
    Column(
        Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionTitle("Tema de color", accent)
        THEMES.chunked(4).forEachIndexed { r, chunk ->
            Row(Modifier.fillMaxWidth()) {
                chunk.forEachIndexed { c, t ->
                    val idx = r * 4 + c
                    val selected = idx == themeIndex
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onTheme(idx) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape).background(t.accent)
                                .then(if (selected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Text("✓", color = onAccentOf(t.accent), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(t.name, color = if (selected) TextPrimary else TextSecondary, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        SectionTitle("Preferencias", accent)
        SettingSwitch("Sonido al tocar", "Clic suave en cada tecla", soundOn, onSound, accent)
        SettingSwitch("Vibración", "Respuesta háptica al presionar", hapticOn, onHaptic, accent)
        SettingSwitch("Mostrar fracciones", "Ej: 0.75 se muestra también como 3/4", fractionOn, onFraction, accent)
        SectionTitle("Historial", accent)
        ValueRow("Cálculos guardados", "$historyCount", accent)
        Text(
            "Borrar historial", color = ErrorColor, fontSize = 16.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FuncColor)
                .clickable(onClick = onClearHistory).padding(14.dp)
        )
        SectionTitle("Trucos", accent)
        listOf(
            "Mantén presionado un botón para usar la función pequeña de su esquina.",
            "Desliza el dedo sobre la pantalla de números para borrar.",
            "Mantén presionado el resultado para copiarlo.",
            "Toca DEG / RAD / GRAD arriba a la izquierda para cambiar el modo de ángulo.",
            "Toca un cálculo del historial para reutilizar su resultado.",
            "Gira el teléfono para ver más funciones científicas."
        ).forEach { Text("•  $it", color = TextSecondary, fontSize = 14.sp) }
        Spacer(Modifier.height(16.dp))
    }
}
