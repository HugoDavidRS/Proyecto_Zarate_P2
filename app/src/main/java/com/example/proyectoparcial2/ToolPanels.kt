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
    GRAPH("∿  Gráfica", "Graficadora"),
    EQ("x²  Ecuaciones", "Ecuaciones"),
    MATRIX("▦  Matrices", "Matrices"),
    STATS("σ  Estadística", "Estadística"),
    FINANCE("$  Finanzas", "Calculadora financiera"),
    DATES("◷  Fechas", "Calculadora de fechas"),
    NUMBERS("ℙ  Primos", "Números y primos"),
    BASES("#  Bases", "Conversor de bases"),
    VARS("A  Variables", "Variables A–F"),
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
internal fun SectionTitle(text: String, accent: Color) {
    Text(text, color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
}

@Composable
internal fun Hint(text: String) {
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

