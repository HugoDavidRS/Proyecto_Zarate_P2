package com.example.proyectoparcial2

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

private fun fmt(v: Double) = CalculatorEngine.formatResult(v)

@Composable
private fun ColumnScope.ScrollBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content
    )
}

@Composable
private fun Tabs(labels: List<String>, selected: Int, accent: Color, onSelect: (Int) -> Unit) {
    PillRow { labels.forEachIndexed { i, l -> Pill(l, i == selected, accent) { onSelect(i) } } }
}

// ── Statistics ────────────────────────────────────────────────────────────────

@Composable
internal fun ColumnScope.StatsPanel(accent: Color, onInsert: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Tabs(listOf("Una variable", "Regresión lineal"), tab, accent) { tab = it }
    if (tab == 0) OneVarStats(accent, onInsert) else RegressionStats(accent, onInsert)
}

@Composable
private fun ColumnScope.OneVarStats(accent: Color, onInsert: (String) -> Unit) {
    var data by rememberSaveable { mutableStateOf("") }
    val st = computeStats(parseNumberList(data))
    CalcField(data, { data = it }, "Datos (separados por comas o espacios)", accent, singleLine = false)
    PillRow {
        Pill("Ejemplo", false, accent) { data = "4, 8, 15, 16, 23, 42" }
        Pill("Limpiar", false, accent) { data = "" }
    }
    ScrollBody {
        if (st == null) Hint("Escribe números para calcular sus estadísticas. Toca un resultado para insertarlo.")
        else {
            listOf(
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
            ).forEach { (label, value) ->
                ValueRow(label, if (value.isFinite()) fmt(value) else "—", accent,
                    onClick = { onInsert(fmt(value)) }.takeIf { value.isFinite() })
            }
            ValueRow("Moda", if (st.modes.isEmpty()) "Sin moda" else st.modes.joinToString(", ") { fmt(it) }, accent)
        }
    }
}

@Composable
private fun ColumnScope.RegressionStats(accent: Color, onInsert: (String) -> Unit) {
    var data by rememberSaveable { mutableStateOf("") }
    var xq by rememberSaveable { mutableStateOf("") }
    val reg = linearRegression(parsePairs(data))
    CalcField(data, { data = it }, "Pares x, y (uno por línea o separados por ;)", accent, singleLine = false)
    PillRow {
        Pill("Ejemplo", false, accent) { data = "1, 2\n2, 4.1\n3, 5.9\n4, 8.2\n5, 9.9" }
        Pill("Limpiar", false, accent) { data = "" }
    }
    ScrollBody {
        if (reg == null) Hint("Escribe al menos 2 pares con valores de x distintos")
        else {
            val sign = if (reg.intercept < 0) "−" else "+"
            ValueRow("Recta de ajuste", "y = ${fmt(reg.slope)}x $sign ${fmt(abs(reg.intercept))}", accent)
            ValueRow("Pendiente m", fmt(reg.slope), accent) { onInsert(fmt(reg.slope)) }
            ValueRow("Ordenada al origen b", fmt(reg.intercept), accent) { onInsert(fmt(reg.intercept)) }
            ValueRow("Correlación r", if (reg.r.isFinite()) fmt(reg.r) else "—", accent)
            ValueRow("Determinación r²", if (reg.r.isFinite()) fmt(reg.r * reg.r) else "—", accent)
            ValueRow("Puntos usados", "${reg.n}", accent)
            CalcField(xq, { xq = it }, "Predecir: escribe un valor de x", accent, keyboardType = KeyboardType.Decimal)
            evalField(xq)?.let { x ->
                val y = reg.slope * x + reg.intercept
                ValueRow("ŷ cuando x = ${fmt(x)}", fmt(y), accent) { onInsert(fmt(y)) }
            }
        }
    }
}

// ── Matrices ──────────────────────────────────────────────────────────────────

private fun cellsOf(s: String) = s.split(";").let { l -> List(9) { l.getOrElse(it) { "0" } } }

private fun toMatrix(cells: List<String>, n: Int): Matrix? =
    List(n) { r -> List(n) { c -> evalField(cells[r * 3 + c]) ?: return null } }

private fun short(v: Double) = fmt(v).let { if (it.length <= 9) it else "%.5g".format(Locale.US, v) }

@Composable
private fun MiniField(value: String, onChange: (String) -> Unit, accent: Color, modifier: Modifier) {
    BasicTextField(
        value, onChange,
        modifier.clip(RoundedCornerShape(8.dp)).background(FuncColor)
            .border(1.dp, ClearColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        singleLine = true,
        textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun MatrixInput(cells: List<String>, n: Int, accent: Color, onCell: (Int, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in 0 until n) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (c in 0 until n) MiniField(cells[r * 3 + c], { onCell(r * 3 + c, it) }, accent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MatrixResult(m: Matrix, accent: Color, onCopy: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FuncColor)
            .border(1.5.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        m.forEach { row ->
            Row {
                row.forEach { v ->
                    Text(short(v), color = TextPrimary, fontSize = 16.sp, textAlign = TextAlign.Center,
                        maxLines = 1, modifier = Modifier.weight(1f))
                }
            }
        }
    }
    Pill("Copiar resultado", false, accent) {
        onCopy(m.joinToString("; ", "[", "]") { row -> row.joinToString(", ") { fmt(it) } })
    }
}

@Composable
internal fun ColumnScope.MatrixPanel(accent: Color, onInsert: (String) -> Unit, onCopy: (String) -> Unit) {
    var n by rememberSaveable { mutableIntStateOf(2) }
    var aS by rememberSaveable { mutableStateOf("2;1;0;1;3;0;0;0;1") }
    var bS by rememberSaveable { mutableStateOf("1;2;0;3;4;0;0;0;1") }
    var op by rememberSaveable { mutableIntStateOf(0) }
    val ops = listOf("det A", "det B", "A⁻¹", "B⁻¹", "Aᵀ", "A + B", "A − B", "A × B", "B × A", "A²")
    val a = toMatrix(cellsOf(aS), n)
    val b = toMatrix(cellsOf(bS), n)
    fun edit(s: String, idx: Int, v: String) =
        cellsOf(s).toMutableList().also { it[idx] = v.replace(";", "") }.joinToString(";")

    ScrollBody {
        Tabs(listOf("2 × 2", "3 × 3"), n - 2, accent) { n = it + 2 }
        SectionTitle("Matriz A", accent)
        MatrixInput(cellsOf(aS), n, accent) { i, v -> aS = edit(aS, i, v) }
        SectionTitle("Matriz B", accent)
        MatrixInput(cellsOf(bS), n, accent) { i, v -> bS = edit(bS, i, v) }
        SectionTitle("Operación", accent)
        Tabs(ops, op, accent) { op = it }
        if (a == null || b == null) Hint("Revisa que todas las celdas tengan números válidos")
        else when (op) {
            0, 1 -> {
                val d = matDet(if (op == 0) a else b)
                ValueRow(ops[op], fmt(d), accent) { onInsert(fmt(d)) }
            }
            2, 3 -> matInverse(if (op == 2) a else b)?.let { MatrixResult(it, accent, onCopy) }
                ?: ValueRow("Resultado", "No tiene inversa (det = 0)", accent)
            4 -> MatrixResult(matTranspose(a), accent, onCopy)
            5 -> MatrixResult(matAdd(a, b), accent, onCopy)
            6 -> MatrixResult(matSub(a, b), accent, onCopy)
            7 -> MatrixResult(matMul(a, b), accent, onCopy)
            8 -> MatrixResult(matMul(b, a), accent, onCopy)
            else -> MatrixResult(matMul(a, a), accent, onCopy)
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ── Variables ─────────────────────────────────────────────────────────────────

@Composable
internal fun ColumnScope.VarsPanel(
    accent: Color, vars: Map<Char, Double>, current: Double?,
    onStore: (Char) -> Unit, onClear: (Char) -> Unit, onInsert: (String) -> Unit
) {
    Hint("Guarda el resultado actual en una letra y úsala en cualquier cálculo, por ejemplo 2A + B.")
    Text(
        if (current != null) "Resultado actual: ${fmt(current)}" else "No hay un resultado para guardar",
        color = if (current != null) TextPrimary else TextSecondary, fontSize = 15.sp
    )
    ScrollBody {
        ('A'..'F').forEach { c ->
            val v = vars[c]
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FuncColor).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text("$c", color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                Text(v?.let(::fmt) ?: "vacía", color = if (v == null) TextSecondary else TextPrimary,
                    fontSize = 16.sp, maxLines = 1, modifier = Modifier.weight(1f))
                if (current != null) Pill("Guardar", false, accent) { onStore(c) }
                if (v != null) {
                    Pill("Usar", true, accent) { onInsert("$c") }
                    Text("✕", color = TextSecondary, fontSize = 16.sp,
                        modifier = Modifier.clip(CircleShape).clickable { onClear(c) }.padding(6.dp))
                }
            }
        }
    }
}

// ── Finance ───────────────────────────────────────────────────────────────────

private fun money(v: Double) = if (v.isFinite()) "$" + "%,.2f".format(Locale.US, v) else "—"
private fun pct(v: Double) = "%.2f %%".format(Locale.US, v)

@Composable
private fun FieldRow(vararg fields: Triple<String, String, (String) -> Unit>, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        fields.forEach { (label, value, onChange) ->
            CalcField(value, onChange, label, accent, Modifier.weight(1f), KeyboardType.Decimal)
        }
    }
}

@Composable
internal fun ColumnScope.FinancePanel(accent: Color, onInsert: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val ins: (Double) -> () -> Unit = { v -> { onInsert("%.2f".format(Locale.US, v)) } }
    Tabs(listOf("Interés compuesto", "Préstamo", "Descuento", "Propina"), tab, accent) { tab = it }
    ScrollBody {
        when (tab) {
            0 -> {
                var p by rememberSaveable { mutableStateOf("10000") }
                var r by rememberSaveable { mutableStateOf("8") }
                var t by rememberSaveable { mutableStateOf("5") }
                var n by rememberSaveable { mutableStateOf("12") }
                FieldRow(Triple("Capital inicial", p) { p = it }, Triple("Tasa anual %", r) { r = it }, accent = accent)
                FieldRow(Triple("Años", t) { t = it }, Triple("Capitalizaciones/año", n) { n = it }, accent = accent)
                val pv = evalField(p); val rv = evalField(r); val tv = evalField(t); val nv = evalField(n)
                if (pv == null || rv == null || tv == null || nv == null || nv <= 0 || tv < 0) Hint("Completa todos los campos")
                else {
                    val fv = compoundAmount(pv, rv, tv, nv)
                    ValueRow("Monto final", money(fv), accent, ins(fv))
                    ValueRow("Intereses ganados", money(fv - pv), accent, ins(fv - pv))
                    ValueRow("Rendimiento total", pct((fv / pv - 1) * 100), accent)
                    SectionTitle("Crecimiento año por año", accent)
                    for (y in 1..min(tv.toInt(), 50)) ValueRow("Año $y", money(compoundAmount(pv, rv, y.toDouble(), nv)), accent)
                }
            }
            1 -> {
                var p by rememberSaveable { mutableStateOf("100000") }
                var r by rememberSaveable { mutableStateOf("12") }
                var m by rememberSaveable { mutableStateOf("24") }
                FieldRow(Triple("Monto del préstamo", p) { p = it }, Triple("Tasa anual %", r) { r = it }, accent = accent)
                FieldRow(Triple("Plazo en meses", m) { m = it }, accent = accent)
                val pv = evalField(p); val rv = evalField(r); val months = evalField(m)?.toInt()
                if (pv == null || rv == null || months == null || months !in 1..600) Hint("Completa los campos (plazo de 1 a 600 meses)")
                else {
                    val pay = loanPayment(pv, rv, months)
                    ValueRow("Pago mensual", money(pay), accent, ins(pay))
                    ValueRow("Total pagado", money(pay * months), accent, ins(pay * months))
                    ValueRow("Intereses totales", money(pay * months - pv), accent, ins(pay * months - pv))
                    SectionTitle("Tabla de amortización", accent)
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Mes", "Interés", "Capital", "Saldo").forEachIndexed { i, h ->
                            Text(h, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                                modifier = Modifier.weight(if (i == 0) 0.5f else 1f))
                        }
                    }
                    var balance: Double = pv
                    for (k in 1..months) {
                        val interest = balance * rv / 100 / 12
                        val capital = pay - interest
                        balance = (balance - capital).coerceAtLeast(0.0)
                        Row(Modifier.fillMaxWidth()) {
                            listOf("$k", money(interest), money(capital), money(balance)).forEachIndexed { i, s ->
                                Text(s, color = TextPrimary, fontSize = 12.sp,
                                    textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                                    modifier = Modifier.weight(if (i == 0) 0.5f else 1f))
                            }
                        }
                    }
                }
            }
            2 -> {
                var price by rememberSaveable { mutableStateOf("1000") }
                var off by rememberSaveable { mutableStateOf("20") }
                var tax by rememberSaveable { mutableStateOf("0") }
                FieldRow(Triple("Precio", price) { price = it }, Triple("Descuento %", off) { off = it }, accent = accent)
                FieldRow(Triple("Impuesto / IVA %", tax) { tax = it }, accent = accent)
                PillRow { listOf(10, 15, 20, 25, 30, 50).forEach { d -> Pill("$d %", off == "$d", accent) { off = "$d" } } }
                val pv = evalField(price); val dv = evalField(off); val tv = evalField(tax)
                if (pv == null || dv == null || tv == null) Hint("Completa todos los campos")
                else {
                    val discounted = pv * (1 - dv / 100)
                    val taxAmount = discounted * tv / 100
                    ValueRow("Ahorras", money(pv - discounted), accent, ins(pv - discounted))
                    ValueRow("Precio con descuento", money(discounted), accent, ins(discounted))
                    if (tv != 0.0) ValueRow("Impuesto", money(taxAmount), accent, ins(taxAmount))
                    ValueRow("Precio final", money(discounted + taxAmount), accent, ins(discounted + taxAmount))
                }
            }
            else -> {
                var bill by rememberSaveable { mutableStateOf("500") }
                var tip by rememberSaveable { mutableStateOf("15") }
                var people by rememberSaveable { mutableStateOf("1") }
                FieldRow(Triple("Cuenta", bill) { bill = it }, Triple("Propina %", tip) { tip = it }, accent = accent)
                FieldRow(Triple("Personas", people) { people = it }, accent = accent)
                PillRow { listOf(10, 15, 18, 20, 25).forEach { t -> Pill("$t %", tip == "$t", accent) { tip = "$t" } } }
                val bv = evalField(bill); val tv = evalField(tip); val pv = evalField(people)
                if (bv == null || tv == null || pv == null || pv < 1) Hint("Completa todos los campos")
                else {
                    val tipAmount = bv * tv / 100
                    ValueRow("Propina", money(tipAmount), accent, ins(tipAmount))
                    ValueRow("Total", money(bv + tipAmount), accent, ins(bv + tipAmount))
                    ValueRow("Cada persona paga", money((bv + tipAmount) / floor(pv)), accent, ins((bv + tipAmount) / floor(pv)))
                }
            }
        }
    }
}

// ── Dates ─────────────────────────────────────────────────────────────────────

private val MONTHS = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
    "agosto", "septiembre", "octubre", "noviembre", "diciembre")
private val WEEKDAYS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
private const val DAY_MS = 86_400_000L

private fun today(): Long {
    val c = Calendar.getInstance()
    return daysFromCivil(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

private fun formatDate(day: Long): String {
    val (y, m, d) = civilFromDays(day)
    return "${WEEKDAYS[dayOfWeek(day)].replaceFirstChar { it.uppercase() }}, $d de ${MONTHS[m - 1]} de $y"
}

private fun ymd(t: Triple<Int, Int, Int>) =
    "${t.first} ${if (t.first == 1) "año" else "años"}, ${t.second} ${if (t.second == 1) "mes" else "meses"}, " +
        "${t.third} ${if (t.third == 1) "día" else "días"}"

/** Sign that starts in each month and the day it starts. */
private val ZODIAC = listOf(
    20 to "Acuario ♒", 19 to "Piscis ♓", 21 to "Aries ♈", 20 to "Tauro ♉", 21 to "Géminis ♊", 21 to "Cáncer ♋",
    23 to "Leo ♌", 23 to "Virgo ♍", 23 to "Libra ♎", 23 to "Escorpio ♏", 22 to "Sagitario ♐", 22 to "Capricornio ♑"
)

internal fun zodiac(m: Int, d: Int): String =
    if (d >= ZODIAC[m - 1].first) ZODIAC[m - 1].second else ZODIAC[(m + 10) % 12].second

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, day: Long, accent: Color, onChange: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FuncColor)
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { open = true }.padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(formatDate(day), color = TextPrimary, fontSize = 17.sp)
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day * DAY_MS)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { onChange(it / DAY_MS) }; open = false }) {
                    Text("Aceptar", color = accent)
                }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar", color = accent) } }
        ) {
            DatePicker(
                state,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = accent, selectedDayContentColor = onAccentOf(accent),
                    selectedYearContainerColor = accent, selectedYearContentColor = onAccentOf(accent),
                    todayDateBorderColor = accent, todayContentColor = accent
                )
            )
        }
    }
}

@Composable
internal fun ColumnScope.DatesPanel(accent: Color, onInsert: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val now = remember { today() }
    Tabs(listOf("Diferencia", "Edad", "Sumar días"), tab, accent) { tab = it }
    ScrollBody {
        when (tab) {
            0 -> {
                var from by rememberSaveable { mutableStateOf(now) }
                var to by rememberSaveable { mutableStateOf(now + 100) }
                DateField("Desde", from, accent) { from = it }
                DateField("Hasta", to, accent) { to = it }
                val lo = minOf(from, to); val hi = maxOf(from, to)
                val days = hi - lo
                if (from > to) Hint("La fecha final es anterior a la inicial; se muestra la diferencia absoluta.")
                ValueRow("Días", "$days", accent) { onInsert("$days") }
                ValueRow("Semanas", "${days / 7} semanas y ${days % 7} días", accent)
                ValueRow("Tiempo", ymd(calendarDiff(lo, hi)), accent)
                ValueRow("Días hábiles (lun–vie)", "${businessDays(lo, hi)}", accent) { onInsert("${businessDays(lo, hi)}") }
                ValueRow("Horas", "%,d".format(Locale.US, days * 24), accent)
            }
            1 -> {
                var birth by rememberSaveable { mutableStateOf(daysFromCivil(2000, 1, 1)) }
                DateField("Fecha de nacimiento", birth, accent) { birth = it }
                if (birth > now) Hint("La fecha de nacimiento está en el futuro")
                else {
                    val (bYear, bm, bd) = civilFromDays(birth)
                    val (ty, _, _) = civilFromDays(now)
                    fun birthdayIn(y: Int) = daysFromCivil(y, bm, min(bd, daysInMonth(y, bm)))
                    val next = birthdayIn(ty).let { if (it < now) birthdayIn(ty + 1) else it }
                    val age = calendarDiff(birth, now)
                    ValueRow("Edad", ymd(age), accent) { onInsert("${age.first}") }
                    ValueRow("Días vividos", "%,d".format(Locale.US, now - birth), accent) { onInsert("${now - birth}") }
                    ValueRow("Semanas vividas", "%,d".format(Locale.US, (now - birth) / 7), accent)
                    ValueRow("Naciste un", WEEKDAYS[dayOfWeek(birth)], accent)
                    ValueRow(
                        "Próximo cumpleaños",
                        if (next == now) "¡Es hoy!" else "en ${next - now} días (${WEEKDAYS[dayOfWeek(next)]})", accent
                    )
                    ValueRow("Cumplirás", "${civilFromDays(next).first - bYear} años", accent)
                    ValueRow("Signo zodiacal", zodiac(bm, bd), accent)
                }
            }
            else -> {
                var base by rememberSaveable { mutableStateOf(now) }
                var n by rememberSaveable { mutableStateOf("30") }
                DateField("Fecha inicial", base, accent) { base = it }
                CalcField(n, { n = it }, "Días a sumar (negativo para restar)", accent, keyboardType = KeyboardType.Decimal)
                PillRow {
                    listOf("-30", "7", "15", "30", "90", "180", "365").forEach { d -> Pill(d, n == d, accent) { n = d } }
                }
                val k = evalField(n)?.toLong()
                if (k == null || abs(k) > 3_000_000) Hint("Escribe un número de días válido")
                else {
                    ValueRow("Resultado", formatDate(base + k), accent)
                    ValueRow("Desde hoy", (base + k - now).let { d ->
                        when { d == 0L -> "Es hoy"; d > 0 -> "Faltan $d días"; else -> "Hace ${-d} días" }
                    }, accent)
                }
            }
        }
    }
}

// ── Number theory ─────────────────────────────────────────────────────────────

private class NumberInfo(val factors: List<Pair<Long, Int>>, val divisors: List<Long>, val prev: Long?, val next: Long)

private fun numberInfo(n: Long): NumberInfo {
    val factors = primeFactors(n)
    var next = n + 1
    while (!isPrime(next)) next++
    return NumberInfo(factors, divisors(factors), (n - 1 downTo 2).firstOrNull { isPrime(it) }, next)
}

private const val SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹"
private fun sup(n: Int) = n.toString().map { SUPERSCRIPTS[it - '0'] }.joinToString("")

@Composable
internal fun ColumnScope.NumbersPanel(accent: Color, onInsert: (String) -> Unit) {
    var nText by rememberSaveable { mutableStateOf("360") }
    var listText by rememberSaveable { mutableStateOf("12, 18, 30") }
    val n = nText.trim().toLongOrNull()?.takeIf { it in 1..1_000_000_000_000 }

    CalcField(nText, { s -> nText = s.filter { it.isDigit() } }, "Número entero (hasta 1 billón)", accent,
        keyboardType = KeyboardType.Number)
    ScrollBody {
        if (n == null) Hint("Escribe un entero entre 1 y 1,000,000,000,000")
        else {
            val info = remember(n) { numberInfo(n) }
            val factors = info.factors
            val divs = info.divisors
            val prime = n >= 2 && factors.size == 1 && factors[0].second == 1
            ValueRow("¿Es primo?", when { n == 1L -> "No (el 1 no es primo)"; prime -> "Sí ✓"; else -> "No" }, accent)
            ValueRow("Factorización",
                if (n == 1L) "1" else factors.joinToString(" × ") { (p, e) -> if (e == 1) "$p" else "$p${sup(e)}" }, accent)
            ValueRow("Cantidad de divisores", "${divs.size}", accent)
            ValueRow("Suma de divisores", "${divs.sum()}", accent) { onInsert("${divs.sum()}") }
            ValueRow("Primo anterior", info.prev?.toString() ?: "—", accent)
            ValueRow("Siguiente primo", "${info.next}", accent) { onInsert("${info.next}") }
            ValueRow("Par o impar", if (n % 2 == 0L) "Par" else "Impar", accent)
            val root = sqrt(n.toDouble())
            ValueRow("Raíz cuadrada", fmt(root) + if (root == floor(root)) "  (cuadrado perfecto)" else "", accent)
            SectionTitle("Divisores", accent)
            Text(divs.take(120).joinToString(", ") + if (divs.size > 120) " …" else "",
                color = TextPrimary, fontSize = 14.sp)
        }
        SectionTitle("MCD y MCM de varios números", accent)
        CalcField(listText, { listText = it }, "Enteros separados por comas", accent)
        val nums = parseNumberList(listText).filter { it == floor(it) && it > 0 && it < 1e15 }.map { it.toLong() }
        if (nums.size < 2) Hint("Escribe al menos dos enteros positivos")
        else {
            val g = nums.reduce(::gcdL)
            val l = runCatching { nums.reduce { a, b -> Math.multiplyExact(a / gcdL(a, b), b) } }.getOrNull()
            ValueRow("MCD (máximo común divisor)", "$g", accent) { onInsert("$g") }
            ValueRow("MCM (mínimo común múltiplo)", l?.toString() ?: "Demasiado grande", accent,
                onClick = l?.let { v -> { onInsert("$v") } })
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
private fun ActionRow(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text, color = color, fontSize = 16.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FuncColor)
            .clickable(onClick = onClick).padding(14.dp)
    )
}

internal class SettingsState(
    val themeIndex: Int, val onTheme: (Int) -> Unit,
    val light: Boolean, val onLight: (Boolean) -> Unit,
    val sound: Boolean, val onSound: (Boolean) -> Unit,
    val haptic: Boolean, val onHaptic: (Boolean) -> Unit,
    val fraction: Boolean, val onFraction: (Boolean) -> Unit,
    val decimals: Int, val onDecimals: (Int) -> Unit,
    val historyCount: Int, val onClearHistory: () -> Unit, val onShareHistory: () -> Unit,
    val onTutorial: () -> Unit
)

@Composable
internal fun ColumnScope.SettingsPanel(s: SettingsState) {
    val accent = THEMES[s.themeIndex].accent
    ScrollBody {
        SectionTitle("Apariencia", accent)
        THEMES.chunked(4).forEachIndexed { r, chunk ->
            Row(Modifier.fillMaxWidth()) {
                chunk.forEachIndexed { c, t ->
                    val idx = r * 4 + c
                    val selected = idx == s.themeIndex
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { s.onTheme(idx) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape).background(t.accent)
                                .then(if (selected) Modifier.border(3.dp, TextPrimary, CircleShape) else Modifier),
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
        SettingSwitch("Modo claro", "Fondo blanco para usar con mucha luz", s.light, s.onLight, accent)

        SectionTitle("Preferencias", accent)
        SettingSwitch("Sonido al tocar", "Clic suave en cada tecla", s.sound, s.onSound, accent)
        SettingSwitch("Vibración", "Respuesta háptica al presionar", s.haptic, s.onHaptic, accent)
        SettingSwitch("Formas exactas", "Muestra 0.75 = 3/4 y 1.5707… = π/2", s.fraction, s.onFraction, accent)

        SectionTitle("Decimales del resultado", accent)
        PillRow {
            listOf(-1, 0, 2, 4, 6, 8).forEach { d ->
                Pill(if (d < 0) "Automático" else "$d", s.decimals == d, accent) { s.onDecimals(d) }
            }
        }

        SectionTitle("Historial", accent)
        ValueRow("Cálculos guardados", "${s.historyCount}", accent)
        ActionRow("Compartir historial", accent, s.onShareHistory)
        ActionRow("Borrar historial", ErrorColor, s.onClearHistory)

        SectionTitle("Ayuda", accent)
        ActionRow("Ver el tutorial de nuevo", accent, s.onTutorial)
        listOf(
            "Mantén presionado un botón para usar la función pequeña de su esquina.",
            "Toca la expresión para mover el cursor y corregir en medio.",
            "Desliza el dedo sobre la pantalla de números para borrar.",
            "Mantén presionado el resultado para copiarlo.",
            "Toca «= 3/4» o «= π/2» debajo del resultado para usar la forma exacta.",
            "Usa i (mantén presionado e) para números complejos: (2+3i)×(1−i).",
            "Gira el teléfono para ver más funciones científicas.",
            "Agrega el widget «Último resultado» a tu pantalla de inicio."
        ).forEach { Text("•  $it", color = TextSecondary, fontSize = 14.sp) }
        Text("Calculadora Científica · versión 3.0", color = TextSecondary, fontSize = 12.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
    }
}

// ── First-launch tutorial ─────────────────────────────────────────────────────

private val TUTORIAL = listOf(
    Triple("∑", "¡Bienvenido!", "Una calculadora científica completa: trigonometría, logaritmos, números complejos, matrices, gráficas y mucho más."),
    Triple("2nd", "Mantén presionado", "Los botones con una etiqueta pequeña en la esquina tienen una segunda función. Mantenlos presionados para usarla."),
    Triple("⌫", "Edita fácilmente", "Toca la expresión para mover el cursor. Desliza sobre la pantalla para borrar. Mantén presionado el resultado para copiarlo."),
    Triple("∿", "Herramientas", "Debajo de la pantalla tienes graficadora, ecuaciones, matrices, unidades, estadística, finanzas, fechas y más."),
    Triple("●", "Hazla tuya", "Cambia el color con «Tema». En Ajustes puedes activar el modo claro, sonidos, vibración y decimales fijos.")
)

@Composable
internal fun Onboarding(accent: Color, onDone: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Box(
        Modifier.fillMaxSize().background(BgColor)
            .clickable(interactionSource = null, indication = null) {}
            .systemBarsPadding().padding(28.dp)
    ) {
        Text("Saltar", color = TextSecondary, fontSize = 15.sp,
            modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(8.dp)).clickable(onClick = onDone).padding(8.dp))
        AnimatedContent(page, label = "tutorial", modifier = Modifier.align(Alignment.Center)) { p ->
            val (icon, title, body) = TUTORIAL[p]
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier.size(120.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text(icon, color = accent, fontSize = if (icon.length > 1) 40.sp else 56.sp, fontWeight = FontWeight.Bold) }
                Text(title, color = accent, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(body, color = TextPrimary, fontSize = 17.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TUTORIAL.indices.forEach { i ->
                    Box(Modifier.size(if (i == page) 22.dp else 8.dp, 8.dp).clip(CircleShape)
                        .background(if (i == page) accent else ClearColor))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                if (page == TUTORIAL.lastIndex) "¡Empezar!" else "Siguiente",
                color = onAccentOf(accent), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(accent)
                    .clickable { if (page == TUTORIAL.lastIndex) onDone() else page++ }.padding(16.dp)
            )
        }
    }
}
