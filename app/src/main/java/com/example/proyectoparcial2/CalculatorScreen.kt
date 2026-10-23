package com.example.proyectoparcial2

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.floor

// ── Palette ───────────────────────────────────────────────────────────────────
internal val BgColor       = Color(0xFF1C1C1E)
internal val NumColor      = Color(0xFF3A3A3C)
internal val FuncColor     = Color(0xFF2C2C2E)
internal val ClearColor    = Color(0xFF636366)
internal val MemColor      = Color(0xFF252527)
internal val TextPrimary   = Color(0xFFFFFFFF)
internal val TextSecondary = Color(0xFF8E8E93)
internal val ErrorColor    = Color(0xFFFF453A)
private val SuccessColor   = Color(0xFF30D158)

internal class CalcTheme(val name: String, val accent: Color)
internal val THEMES = listOf(
    CalcTheme("Naranja",  Color(0xFFFF9F0A)),
    CalcTheme("Azul",     Color(0xFF0A84FF)),
    CalcTheme("Verde",    Color(0xFF30D158)),
    CalcTheme("Morado",   Color(0xFFBF5AF2)),
    CalcTheme("Rojo",     Color(0xFFE5484D)),
    CalcTheme("Rosa",     Color(0xFFFF375F)),
    CalcTheme("Cian",     Color(0xFF64D2FF)),
    CalcTheme("Amarillo", Color(0xFFFFD60A)),
)

/** Readable text color on top of an accent background. */
internal fun onAccentOf(accent: Color) = if (accent.luminance() > 0.5f) Color.Black else Color.White

/** Number text the parser reads back exactly (no ×10^ rounding). */
internal fun plainNumber(v: Double) =
    if (v == floor(v) && abs(v) < 1e15) v.toLong().toString() else v.toString()

// ── Buttons ───────────────────────────────────────────────────────────────────
private enum class BtnType { Num, Op, Func, Clear, Mem, Special }

private data class BtnDef(
    val label: String,
    val action: String = label,
    val type: BtnType = BtnType.Func,
    val altLabel: String? = null,
    val altAction: String? = null,
    val swapped: Boolean = false,
    val active: Boolean = false
)

private fun num(d: String) = BtnDef(d, type = BtnType.Num)

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun CalculatorScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences("calc_prefs", Context.MODE_PRIVATE) }
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Persisted between app launches
    var themeIndex by remember { mutableIntStateOf(prefs.getInt("theme", 0).coerceIn(0, THEMES.lastIndex)) }
    var soundOn by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    var hapticOn by remember { mutableStateOf(prefs.getBoolean("haptic", true)) }
    var fractionOn by remember { mutableStateOf(prefs.getBoolean("fraction", true)) }
    var angleMode by remember { mutableStateOf(AngleMode.entries.getOrElse(prefs.getInt("angle", 0)) { AngleMode.DEG }) }
    var history by remember { mutableStateOf(loadHistory(prefs)) }
    LaunchedEffect(themeIndex, soundOn, hapticOn, fractionOn, angleMode, history) {
        prefs.edit()
            .putInt("theme", themeIndex).putBoolean("sound", soundOn).putBoolean("haptic", hapticOn)
            .putBoolean("fraction", fractionOn).putInt("angle", angleMode.ordinal)
            .putString("history", history.joinToString("\n") { "${it.first}\t${it.second}" })
            .apply()
    }

    // Survives screen rotation
    var expression by rememberSaveable { mutableStateOf("") }
    var justEvaluated by rememberSaveable { mutableStateOf(false) }
    var memory by rememberSaveable { mutableStateOf(0.0) }
    var hasMemory by rememberSaveable { mutableStateOf(false) }
    var isSecond by rememberSaveable { mutableStateOf(false) }
    var lastAnswer by rememberSaveable { mutableStateOf(0.0) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var tool by rememberSaveable { mutableStateOf<Tool?>(null) }

    var result by remember { mutableStateOf("") }
    var resultValue by remember { mutableStateOf<Double?>(null) }
    var isError by remember { mutableStateOf(false) }
    var errorTick by remember { mutableIntStateOf(0) }
    var toastText by remember { mutableStateOf("") }
    var toastColor by remember { mutableStateOf(SuccessColor) }
    var toastTick by remember { mutableIntStateOf(0) }
    var toastVisible by remember { mutableStateOf(false) }

    val accent = THEMES[themeIndex].accent
    val onAccent = onAccentOf(accent)

    val shake = remember { Animatable(0f) }
    LaunchedEffect(errorTick) {
        if (errorTick > 0) shake.animateTo(0f, keyframes {
            durationMillis = 380
            0f at 0; -14f at 55; 14f at 110; -10f at 165; 10f at 220; -5f at 290; 0f at 380
        })
    }
    LaunchedEffect(toastTick) {
        if (toastTick > 0) { toastVisible = true; delay(1300); toastVisible = false }
    }

    // Live preview while typing
    LaunchedEffect(expression, angleMode) {
        isError = false
        val v = if (expression.isBlank()) null
        else runCatching { CalculatorEngine.evaluate(withAns(expression, lastAnswer), angleMode) }.getOrNull()
        resultValue = v
        result = v?.let(CalculatorEngine::formatResult) ?: ""
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    fun showToast(text: String, color: Color = SuccessColor) {
        toastText = text; toastColor = color; toastTick++
    }

    fun copy(text: String) {
        clipboard.setText(AnnotatedString(text))
        showToast("✓ Copiado")
    }

    fun feedback(type: HapticFeedbackType, sound: Int) {
        if (hapticOn) haptic.performHapticFeedback(type)
        if (soundOn) audio.playSoundEffect(sound, -1f)
    }

    fun insert(s: String) {
        // After "=", operators continue from the result; anything else starts a new calculation
        val continues = s[0] in "+−-×÷^%!, "
        expression = if (justEvaluated && !continues) s else expression + s
        justEvaluated = false
    }

    fun insertValue(v: String) {
        val wrapped = if (v.startsWith("-")) "($v)" else v
        val last = expression.lastOrNull()
        val needsTimes = !justEvaluated && last != null && (last.isLetterOrDigit() || last in ").!%")
        insert(if (needsTimes) "×$wrapped" else wrapped)
    }

    fun evaluateNow() {
        if (expression.isBlank()) return
        try {
            val v = CalculatorEngine.evaluate(withAns(expression, lastAnswer), angleMode)
            val r = CalculatorEngine.formatResult(v)
            if (expression != r) history = (listOf(expression to r) + history).take(100)
            if (v.isFinite()) {
                lastAnswer = v
                expression = r
                justEvaluated = true
            }
        } catch (e: Exception) {
            result = errorMessage(e)
            isError = true
            errorTick++
            if (hapticOn) haptic.performHapticFeedback(HapticFeedbackType.Reject)
        }
    }

    fun onBtn(action: String, silent: Boolean = false) {
        if (!silent) when (action) {
            "=" -> feedback(HapticFeedbackType.Confirm, AudioManager.FX_KEYPRESS_RETURN)
            "⌫", "AC" -> feedback(HapticFeedbackType.VirtualKey, AudioManager.FX_KEYPRESS_DELETE)
            else -> feedback(HapticFeedbackType.VirtualKey, AudioManager.FX_KEYPRESS_STANDARD)
        }
        when (action) {
            "⌫" -> { expression = dropLastToken(expression); justEvaluated = false }
            "AC" -> { expression = ""; result = ""; isError = false; justEvaluated = false }
            "=" -> evaluateNow()
            "DEG" -> angleMode = AngleMode.entries[(angleMode.ordinal + 1) % AngleMode.entries.size]
            "2nd" -> isSecond = !isSecond
            "MC" -> { memory = 0.0; hasMemory = false; showToast("Memoria borrada", TextSecondary) }
            "MR" -> if (hasMemory) insertValue(plainNumber(memory)) else showToast("Memoria vacía", TextSecondary)
            "M+", "M-" -> {
                val v = runCatching { CalculatorEngine.evaluate(withAns(expression, lastAnswer), angleMode) }.getOrNull()
                if (v == null) { showToast("Nada que guardar", TextSecondary); return }
                memory += if (action == "M+") v else -v
                hasMemory = true
                showToast("M = ${CalculatorEngine.formatResult(memory)}")
            }
            "±" -> {
                expression = when {
                    expression.isEmpty() -> "-"
                    expression.startsWith("-(") && expression.endsWith(")") -> expression.drop(2).dropLast(1)
                    expression.startsWith("-") -> expression.drop(1)
                    else -> "-($expression)"
                }
                justEvaluated = false
            }
            else -> insert(action)
        }
    }

    fun onLong(action: String) {
        if (hapticOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (soundOn) audio.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
        onBtn(action, silent = true)
    }

    fun cycleTheme() {
        themeIndex = (themeIndex + 1) % THEMES.size
        feedback(HapticFeedbackType.VirtualKey, AudioManager.FX_KEY_CLICK)
        showToast("●  ${THEMES[themeIndex].name}", THEMES[themeIndex].accent)
    }

    val swipeDelete = rememberUpdatedState { onBtn("⌫") }

    var lastTool by remember { mutableStateOf(Tool.CONST) }
    tool?.let { if (it != lastTool) lastTool = it } // keeps panel content during its exit animation

    BackHandler(enabled = tool != null || showHistory) {
        if (tool != null) tool = null else showHistory = false
    }

    // ── Button layout ─────────────────────────────────────────────────────────
    fun fn(a: String, aAct: String, b: String, bAct: String) =
        if (isSecond) BtnDef(b, bAct, altLabel = a, altAction = aAct, swapped = true)
        else BtnDef(a, aAct, altLabel = b, altAction = bAct)

    val sciRows = listOf(
        listOf(
            BtnDef("AC", type = BtnType.Clear),
            BtnDef(angleMode.name, "DEG", BtnType.Special),
            BtnDef("(", type = BtnType.Clear),
            BtnDef(")", type = BtnType.Clear),
            BtnDef("⌫", type = BtnType.Clear, altLabel = "AC", altAction = "AC")
        ),
        listOf(
            BtnDef("2nd", type = BtnType.Special, active = isSecond),
            fn("x²", "^2", "√", "√("),
            fn("x³", "^3", "∛", "∛("),
            BtnDef("xⁿ", "^"),
            BtnDef("÷", type = BtnType.Op)
        ),
        listOf(
            fn("sin", "sin(", "sin⁻¹", "asin("),
            fn("cos", "cos(", "cos⁻¹", "acos("),
            fn("tan", "tan(", "tan⁻¹", "atan("),
            fn("log", "log(", "10ˣ", "10^"),
            BtnDef("×", type = BtnType.Op)
        ),
        listOf(
            fn("sinh", "sinh(", "sinh⁻¹", "asinh("),
            fn("cosh", "cosh(", "cosh⁻¹", "acosh("),
            fn("tanh", "tanh(", "tanh⁻¹", "atanh("),
            fn("ln", "ln(", "eˣ", "exp("),
            BtnDef("−", type = BtnType.Op)
        ),
        listOf(
            BtnDef("π", altLabel = "φ", altAction = "φ"),
            BtnDef("e"),
            BtnDef("!", altLabel = "Rnd", altAction = "rand"),
            fn("nCr", "ncr(", "nPr", "npr("),
            BtnDef("+", type = BtnType.Op)
        )
    )
    val extraRow = listOf(
        BtnDef("log₂", "log2("),
        BtnDef("⌊x⌋", "floor(", altLabel = "⌈x⌉", altAction = "ceil("),
        BtnDef("gcd", "gcd(", altLabel = "lcm", altAction = "lcm("),
        BtnDef("|x|", "abs("),
        BtnDef("Rnd", "rand")
    )
    val numRows = listOf(
        listOf(BtnDef("MC", type = BtnType.Mem), BtnDef("MR", type = BtnType.Mem),
               BtnDef("M+", type = BtnType.Mem), BtnDef("M-", type = BtnType.Mem)),
        listOf(num("7"), num("8"), num("9"), BtnDef("1/x", "rec(", altLabel = "|x|", altAction = "abs(")),
        listOf(num("4"), num("5"), num("6"), BtnDef("%", altLabel = "mod", altAction = " mod ")),
        listOf(num("1"), num("2"), num("3"), BtnDef(",", altLabel = "EE", altAction = "×10^")),
        listOf(BtnDef("±", type = BtnType.Num), num("0"), num("."), BtnDef("Ans"))
    )

    val btnRow: @Composable (Modifier, List<BtnDef>) -> Unit = { m, list ->
        BtnRow(m, list, accent, onAccent, landscape, onClick = { onBtn(it) }, onLong = { onLong(it) })
    }
    val numPad: @Composable (Modifier) -> Unit = { m ->
        Row(m) {
            Column(Modifier.weight(4f)) { numRows.forEach { btnRow(Modifier.weight(1f), it) } }
            CalcBtn("=", accent, onAccent, Modifier.weight(1f), 36.sp, accentStyle = true, accent = accent,
                onClick = { onBtn("=") })
        }
    }

    val showPreview = result.isNotEmpty() && (isError || result != expression)
    val fraction = if (fractionOn && !isError) resultValue?.let { CalculatorEngine.toFraction(it) } else null

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize().background(BgColor)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {

            AnimatedVisibility(showHistory, enter = expandVertically(), exit = shrinkVertically()) {
                HistoryPanel(
                    history, accent, maxHeight = if (landscape) 150.dp else 260.dp,
                    onPick = { res ->
                        if (runCatching { CalculatorEngine.evaluate(res, angleMode) }.isSuccess) {
                            insertValue(res); showHistory = false
                        }
                    },
                    onCopy = { copy(it) },
                    onClear = { history = emptyList(); showToast("Historial borrado", TextSecondary) },
                    onClose = { showHistory = false }
                )
            }

            // Display (swipe sideways to delete)
            Box(
                Modifier
                    .weight(if (landscape) 2.3f else 2.6f)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0E0E10), BgColor)))
                    .pointerInput(Unit) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = { if (abs(total) > 60.dp.toPx()) swipeDelete.value() }
                        ) { _, delta -> total += delta }
                    }
            ) {
                Box(Modifier.fillMaxWidth().height(2.dp).background(accent.copy(alpha = 0.5f)))
                Column(
                    Modifier.fillMaxSize().offset(x = shake.value.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            InfoBadge(angleMode.name, accent) { onBtn("DEG") }
                            if (hasMemory) InfoBadge("M ${CalculatorEngine.formatResult(memory)}", SuccessColor) { onBtn("MR") }
                            if (isSecond) InfoBadge("2nd", accent) { onBtn("2nd") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(accent.copy(alpha = 0.15f))
                                    .clickable { cycleTheme() }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Box(Modifier.size(9.dp).clip(CircleShape).background(accent))
                                Text("Tema", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                if (showHistory) "▲ Cerrar" else "▼ Historial",
                                color = TextSecondary, fontSize = 12.sp,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .clickable { showHistory = !showHistory }
                                    .padding(horizontal = 6.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    if (showPreview) {
                        Text(
                            highlight(expression, accent, error = false, dim = true),
                            fontSize = if (landscape) 18.sp else 24.sp, fontWeight = FontWeight.Light,
                            textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AnimatedContent(
                            targetState = result,
                            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(100)) },
                            label = "result"
                        ) { r ->
                            Text(
                                r, color = if (isError) ErrorColor else TextPrimary,
                                fontSize = bigFont(r.length, landscape), fontWeight = FontWeight.Light,
                                maxLines = 1, textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null, onClick = {},
                                    onLongClick = { if (!isError) copy(r) }
                                )
                            )
                        }
                    } else {
                        Text(
                            highlight(expression.ifEmpty { "0" }, accent, error = isError, dim = false),
                            fontSize = bigFont(expression.length, landscape), fontWeight = FontWeight.Light,
                            textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null, onClick = {},
                                onLongClick = { if (expression.isNotEmpty()) copy(expression) }
                            )
                        )
                    }
                    if (fraction != null && !landscape) {
                        Text("= $fraction", color = accent, fontSize = 18.sp, textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // Tools strip
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Tool.entries.forEach { t ->
                    Text(
                        t.chip, color = TextPrimary, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(FuncColor)
                            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(50))
                            .clickable {
                                feedback(HapticFeedbackType.VirtualKey, AudioManager.FX_KEY_CLICK)
                                tool = t
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Keypad
            if (landscape) {
                Row(Modifier.weight(4f).padding(horizontal = 4.dp)) {
                    Column(Modifier.weight(1.1f)) { (sciRows + listOf(extraRow)).forEach { btnRow(Modifier.weight(1f), it) } }
                    numPad(Modifier.weight(1f))
                }
            } else {
                Column(Modifier.weight(7.4f).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    sciRows.forEach { btnRow(Modifier.weight(1f), it) }
                    numPad(Modifier.weight(5f))
                }
            }
        }

        // Tool panels (full screen)
        AnimatedVisibility(
            visible = tool != null,
            enter = slideInVertically { it / 4 } + fadeIn(),
            exit = slideOutVertically { it / 4 } + fadeOut()
        ) {
            val t = lastTool
            PanelScaffold(t.title, accent, onClose = { tool = null }) {
                val insertAndClose: (String) -> Unit = { insertValue(it); tool = null; showToast("✓ Insertado") }
                when (t) {
                    Tool.CONST -> ConstantsPanel(accent, insertAndClose) { copy(it) }
                    Tool.UNITS -> UnitsPanel(accent, resultValue, insertAndClose)
                    Tool.STATS -> StatsPanel(accent, insertAndClose)
                    Tool.BASES -> BasesPanel(
                        accent, resultValue?.takeIf { it == floor(it) && abs(it) < 9e15 }?.toLong(), insertAndClose
                    ) { copy(it) }
                    Tool.GRAPH -> GraphPanel(accent, insertAndClose)
                    Tool.EQ -> EquationsPanel(accent, insertAndClose)
                    Tool.SETTINGS -> SettingsPanel(
                        themeIndex, { themeIndex = it },
                        soundOn, { soundOn = it },
                        hapticOn, { hapticOn = it },
                        fractionOn, { fractionOn = it },
                        history.size
                    ) { history = emptyList(); showToast("Historial borrado", TextSecondary) }
                }
            }
        }

        // Toast
        AnimatedVisibility(
            visible = toastVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                toastText, color = toastColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xEE2C2C2E))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun withAns(expr: String, ans: Double) = expr.replace("Ans", "(${plainNumber(ans)})")

private val TOKEN_END = Regex("([a-z][a-z0-9]*\\(|√\\(|∛\\(| mod |×10\\^|Ans|rand)$")

/** Backspace removes whole function names ("sin(") instead of one letter. */
private fun dropLastToken(s: String) = TOKEN_END.find(s)?.let { s.removeRange(it.range) } ?: s.dropLast(1)

private fun errorMessage(e: Exception) = when {
    e is ArithmeticException && e.message?.startsWith("Division") == true -> "No se puede dividir entre 0"
    e is ArithmeticException -> "Resultado indefinido"
    else -> "Error de sintaxis"
}

private fun loadHistory(prefs: SharedPreferences): List<Pair<String, String>> =
    prefs.getString("history", "").orEmpty().lines().mapNotNull { line ->
        val p = line.split('\t')
        if (p.size == 2) p[0] to p[1] else null
    }

private fun bigFont(len: Int, landscape: Boolean): TextUnit {
    val size = when {
        len <= 8 -> 64
        len <= 11 -> 52
        len <= 15 -> 40
        len <= 20 -> 32
        else -> 26
    }
    return (if (landscape) minOf(size, 34) else size).sp
}

private fun highlight(expr: String, accent: Color, error: Boolean, dim: Boolean): AnnotatedString =
    buildAnnotatedString {
        val numColor = if (dim) Color(0xFFAEAEB2) else TextPrimary
        for (c in expr) {
            val color = when {
                error -> ErrorColor
                c.isDigit() || c == '.' -> numColor
                c in "+−-×÷^%!" -> accent
                c.isLetter() || c in "√∛" -> accent.copy(alpha = 0.75f)
                else -> TextSecondary
            }
            withStyle(SpanStyle(color = color)) { append(c) }
        }
    }

@Composable
private fun InfoBadge(label: String, color: Color, onClick: () -> Unit) {
    Text(
        label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(color.copy(alpha = 0.18f))
            .clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

@Composable
private fun BtnRow(
    modifier: Modifier, buttons: List<BtnDef>, accent: Color, onAccent: Color, compact: Boolean,
    onClick: (String) -> Unit, onLong: (String) -> Unit
) {
    Row(modifier.fillMaxWidth()) {
        for (btn in buttons) {
            val accentStyle = btn.type == BtnType.Op || btn.active
            val bg = when {
                accentStyle -> accent
                btn.type == BtnType.Num -> NumColor
                btn.type == BtnType.Clear || btn.type == BtnType.Special -> ClearColor
                btn.type == BtnType.Mem -> MemColor
                else -> FuncColor
            }
            val fg = when {
                accentStyle -> onAccent
                btn.action == "AC" -> Color(0xFFFF6961)
                btn.type == BtnType.Mem -> TextSecondary
                btn.swapped -> accent
                else -> TextPrimary
            }
            val fs = when {
                btn.label.length > 5 -> 12.sp
                btn.label.length > 4 -> 14.sp
                btn.label.length > 3 -> 16.sp
                btn.label.length > 2 -> 18.sp
                else -> 22.sp
            }
            CalcBtn(
                btn.label, bg, fg, Modifier.weight(1f), fs, accentStyle, accent,
                if (compact && btn.altLabel?.endsWith("⁻¹") == true) "⁻¹" else btn.altLabel,
                onLongClick = btn.altAction?.let { alt -> { onLong(alt) } },
                onClick = { onClick(btn.action) }
            )
        }
    }
}

@Composable
private fun CalcBtn(
    label: String, bg: Color, fg: Color, modifier: Modifier, fontSize: TextUnit,
    accentStyle: Boolean, accent: Color, altLabel: String? = null,
    onLongClick: (() -> Unit)? = null, onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 750f),
        label = "btn_scale"
    )
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxHeight()
            .padding(3.dp)
            .scale(scale)
            .clip(shape)
            .background(if (accentStyle) Brush.verticalGradient(listOf(bg, bg.copy(alpha = 0.78f))) else SolidColor(bg))
            .background(Color.White.copy(alpha = if (pressed) 0.12f else 0f))
            .then(if (accentStyle) Modifier.border(1.dp, accent.copy(alpha = 0.35f), shape) else Modifier)
            .combinedClickable(
                interactionSource = source, indication = null,
                onClick = onClick, onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, color = fg, fontSize = fontSize,
            fontWeight = if (accentStyle) FontWeight.Medium else FontWeight.Light,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            // nudged down so the corner label never overlaps it
            modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = if (altLabel != null) 8.dp else 0.dp)
        )
        if (altLabel != null) {
            Text(
                altLabel, color = if (accentStyle) fg.copy(alpha = 0.7f) else accent.copy(alpha = 0.85f),
                fontSize = 9.sp, maxLines = 1,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 6.dp)
            )
        }
    }
}

@Composable
private fun HistoryPanel(
    history: List<Pair<String, String>>, accent: Color, maxHeight: Dp,
    onPick: (String) -> Unit, onCopy: (String) -> Unit, onClear: () -> Unit, onClose: () -> Unit
) {
    Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).background(Color(0xFF242426))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Historial", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            if (history.isNotEmpty()) {
                Text("Borrar", color = ErrorColor, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClear).padding(6.dp))
            }
            Text("✕", color = TextSecondary, fontSize = 16.sp,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 4.dp))
        }
        if (history.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Sin historial aún", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            Text("Toca para usar el resultado · mantén presionado para copiar",
                color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 14.dp))
            LazyColumn {
                items(history) { (expr, res) ->
                    Column(
                        Modifier.fillMaxWidth()
                            .combinedClickable(onClick = { onPick(res) }, onLongClick = { onCopy(res) })
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(expr, color = TextSecondary, fontSize = 13.sp)
                        Text("= $res", color = accent, fontSize = 19.sp)
                    }
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF3A3A3C)))
                }
            }
        }
    }
}
