package com.example.proyectoparcial2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.floor

// ── Static palette ────────────────────────────────────────────────────────────
private val BgColor      = Color(0xFF1C1C1E)
private val NumColor     = Color(0xFF3A3A3C)
private val FuncColor    = Color(0xFF2C2C2E)
private val ClearColor   = Color(0xFF636366)
private val MemColor     = Color(0xFF252527)
private val TextPrimary  = Color(0xFFFFFFFF)
private val TextSecondary= Color(0xFF8E8E93)
private val ErrorColor   = Color(0xFFFF453A)

// ── Themes (accent colour only — rest of UI is always dark) ──────────────────
private data class CalcTheme(val name: String, val accent: Color)
private val THEMES = listOf(
    CalcTheme("🟠 Naranja",  Color(0xFFFF9F0A)),
    CalcTheme("🔵 Azul",     Color(0xFF0A84FF)),
    CalcTheme("🟢 Verde",    Color(0xFF30D158)),
    CalcTheme("🟣 Morado",   Color(0xFFBF5AF2)),
)

// ── Button types ──────────────────────────────────────────────────────────────
private enum class BtnType { Num, Op, Equal, Func, Clear, Mem, Special }
private data class BtnDef(
    val label: String,
    val action: String = label,
    val type: BtnType = BtnType.Func
)

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun CalculatorScreen() {
    val haptic    = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current

    // ── State ─────────────────────────────────────────────────────────────────
    var expression  by remember { mutableStateOf("") }
    var result      by remember { mutableStateOf("") }
    var angleMode   by remember { mutableStateOf(AngleMode.DEG) }
    var memory      by remember { mutableStateOf(0.0) }
    var hasMemory   by remember { mutableStateOf(false) }
    var isSecond    by remember { mutableStateOf(false) }
    var lastAnswer  by remember { mutableStateOf(0.0) }
    var history     by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var showHistory by remember { mutableStateOf(false) }
    var isError     by remember { mutableStateOf(false) }
    var themeIndex  by remember { mutableStateOf(0) }
    var showBanner  by remember { mutableStateOf(false) }
    var showCopied  by remember { mutableStateOf(false) }

    val theme = THEMES[themeIndex]

    // Shake animation for errors
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            shakeOffset.animateTo(0f, keyframes {
                durationMillis = 380
                0f at 0; -14f at 55; 14f at 110; -10f at 165; 10f at 220; -5f at 290; 0f at 380
            })
        }
    }

    // Banner auto-hide
    LaunchedEffect(showBanner) { if (showBanner) { delay(1300); showBanner = false } }
    LaunchedEffect(showCopied) { if (showCopied) { delay(1100); showCopied = false } }

    // Live evaluation
    LaunchedEffect(expression, angleMode) {
        if (expression.isEmpty()) { result = ""; isError = false; return@LaunchedEffect }
        try {
            val ansVal = ansString(lastAnswer)
            val v = CalculatorEngine.evaluate(expression.replace("Ans", ansVal), angleMode)
            result = CalculatorEngine.formatResult(v)
            isError = false
        } catch (_: Exception) { result = ""; isError = false }
    }

    // ── Button handler ────────────────────────────────────────────────────────
    fun onBtn(action: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        when (action) {
            "⌫" -> if (expression.isNotEmpty()) expression = expression.dropLast(1)
            "AC" -> { expression = ""; result = ""; isError = false }
            "=" -> {
                if (expression.isEmpty()) return
                try {
                    val v = CalculatorEngine.evaluate(
                        expression.replace("Ans", ansString(lastAnswer)), angleMode)
                    val r = CalculatorEngine.formatResult(v)
                    history = (listOf(expression to r) + history).take(50)
                    lastAnswer = v; expression = r; result = ""; isError = false
                } catch (e: Exception) {
                    result = e.message?.take(22) ?: "Error"; isError = true
                }
            }
            "DEG" -> angleMode = when (angleMode) {
                AngleMode.DEG -> AngleMode.RAD
                AngleMode.RAD -> AngleMode.GRAD
                AngleMode.GRAD -> AngleMode.DEG
            }
            "2nd"  -> isSecond = !isSecond
            "MC"   -> { memory = 0.0; hasMemory = false }
            "MR"   -> expression += CalculatorEngine.formatResult(memory)
            "M+"   -> runCatching {
                memory += CalculatorEngine.evaluate(expression.replace("Ans", ansString(lastAnswer)), angleMode)
                hasMemory = true
            }
            "M-"   -> runCatching {
                memory -= CalculatorEngine.evaluate(expression.replace("Ans", ansString(lastAnswer)), angleMode)
                hasMemory = true
            }
            "±"    -> expression = when {
                expression.isEmpty() -> "-"
                expression.startsWith("-(") && expression.endsWith(")") -> expression.drop(2).dropLast(1)
                expression.startsWith("-") -> expression.drop(1)
                else -> "-($expression)"
            }
            "Ans"  -> expression += "Ans"
            "HIST" -> showHistory = !showHistory
            else   -> { isError = false; expression += action }
        }
    }

    // ── Button grid rows ──────────────────────────────────────────────────────
    val row0 = listOf(
        BtnDef(angleMode.name, "DEG", BtnType.Special),
        BtnDef("(",  type = BtnType.Clear), BtnDef(")", type = BtnType.Clear),
        BtnDef("%",  type = BtnType.Clear), BtnDef("⌫", type = BtnType.Clear)
    )
    val row1 = listOf(
        BtnDef("2nd", type = BtnType.Special),
        BtnDef(if (isSecond) "√"    else "x²",  if (isSecond) "√("  else "^2"),
        BtnDef(if (isSecond) "∛"    else "x³",  if (isSecond) "cbrt(" else "^3"),
        BtnDef("xⁿ", "^"),
        BtnDef("÷", type = BtnType.Op)
    )
    val row2 = listOf(
        BtnDef(if (isSecond) "asin" else "sin",  if (isSecond) "asin(" else "sin("),
        BtnDef(if (isSecond) "acos" else "cos",  if (isSecond) "acos(" else "cos("),
        BtnDef(if (isSecond) "atan" else "tan",  if (isSecond) "atan(" else "tan("),
        BtnDef(if (isSecond) "10^x" else "log",  if (isSecond) "10^"   else "log("),
        BtnDef("×", type = BtnType.Op)
    )
    val row3 = listOf(
        BtnDef(if (isSecond) "asinh" else "sinh", if (isSecond) "asinh(" else "sinh("),
        BtnDef(if (isSecond) "acosh" else "cosh", if (isSecond) "acosh(" else "cosh("),
        BtnDef(if (isSecond) "atanh" else "tanh", if (isSecond) "atanh(" else "tanh("),
        BtnDef(if (isSecond) "eˣ"   else "ln",   if (isSecond) "exp("   else "ln("),
        BtnDef("−", "-", BtnType.Op)
    )
    val row4 = listOf(
        BtnDef("π"), BtnDef("e"), BtnDef("!"),
        BtnDef(if (isSecond) "nPr" else "nCr", if (isSecond) "npr(" else "ncr("),
        BtnDef("+", type = BtnType.Op)
    )
    val memRow  = listOf(BtnDef("MC", type=BtnType.Mem), BtnDef("MR", type=BtnType.Mem),
                         BtnDef("M+", type=BtnType.Mem), BtnDef("M-", type=BtnType.Mem))
    val numRow1 = listOf(BtnDef("7",BtnType.Num),BtnDef("8",BtnType.Num),BtnDef("9",BtnType.Num),BtnDef("1/x","rec("))
    val numRow2 = listOf(BtnDef("4",BtnType.Num),BtnDef("5",BtnType.Num),BtnDef("6",BtnType.Num),BtnDef("mod"," mod "))
    val numRow3 = listOf(BtnDef("1",BtnType.Num),BtnDef("2",BtnType.Num),BtnDef("3",BtnType.Num),BtnDef("EE","×10^"))
    val numRow4 = listOf(BtnDef("±",BtnType.Num), BtnDef("0",BtnType.Num), BtnDef(".",BtnType.Num), BtnDef("Ans"))

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize().background(BgColor)) {

        Column(Modifier.fillMaxSize().systemBarsPadding()) {

            // History panel
            AnimatedVisibility(showHistory, enter = expandVertically(), exit = shrinkVertically()) {
                HistoryPanel(history) { showHistory = false }
            }

            // Display
            Column(
                Modifier
                    .weight(2.5f)
                    .fillMaxWidth()
                    .offset(x = shakeOffset.value.dp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            themeIndex = (themeIndex + 1) % THEMES.size
                            showBanner = true
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Badges + history
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ThemeBadge(angleMode.name, theme.accent)
                        if (hasMemory) ThemeBadge("M", Color(0xFF30D158))
                        if (isSecond)  ThemeBadge("2nd", theme.accent)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("🎨", fontSize = 12.sp, modifier = Modifier.combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, onClick = {},
                            onLongClick = {}
                        ))
                        Text(
                            if (showHistory) "▲" else "▼ Historial",
                            color = TextSecondary, fontSize = 12.sp,
                            modifier = Modifier.combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null, onClick = { onBtn("HIST") }, onLongClick = {}
                            )
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Expression
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.End) {
                    Text(
                        expression.ifEmpty { "0" },
                        color = if (isError) ErrorColor else TextSecondary,
                        fontSize = 22.sp, fontWeight = FontWeight.Light,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End
                    )
                }

                // Result (long-press to copy)
                if (result.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.End) {
                        Text(
                            result,
                            color = if (isError) ErrorColor else TextPrimary,
                            fontSize = adaptFontSize(result), fontWeight = FontWeight.Light,
                            maxLines = 1, textAlign = TextAlign.End,
                            modifier = Modifier.combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null, onClick = {},
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    clipboard.setText(AnnotatedString(result))
                                    showCopied = true
                                }
                            )
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
            }

            // Buttons
            Column(Modifier.weight(7.5f)) {
                for (row in listOf(row0, row1, row2, row3, row4)) {
                    BtnRow(Modifier.weight(1f), row, isSecond, theme.accent) { onBtn(it) }
                }
                Row(Modifier.weight(5f)) {
                    Column(Modifier.weight(4f)) {
                        for (row in listOf(memRow, numRow1, numRow2, numRow3, numRow4)) {
                            BtnRow(Modifier.weight(1f), row, isSecond, theme.accent) { onBtn(it) }
                        }
                    }
                    // Big = button
                    Box(
                        Modifier.weight(1f).fillMaxHeight().padding(3.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.accent)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onBtn("=") }, onLongClick = {}
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("=", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }

        // Theme banner overlay
        AnimatedVisibility(
            visible = showBanner,
            enter = fadeIn() + scaleIn(initialScale = 0.75f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xDD2C2C2E))
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(THEMES[themeIndex].name, color = theme.accent, fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }

        // "¡Copiado!" toast
        AnimatedVisibility(
            visible = showCopied,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD2C2C2E))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("✓ Copiado", color = Color(0xFF30D158), fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun ansString(v: Double) =
    if (v == floor(v) && abs(v) < 1e15) v.toLong().toString() else v.toString()

private fun adaptFontSize(s: String): TextUnit = when {
    s.length <= 9  -> 52.sp
    s.length <= 14 -> 40.sp
    s.length <= 20 -> 30.sp
    else           -> 22.sp
}

private fun BtnDef(label: String, type: BtnType) = BtnDef(label, label, type)

@Composable
private fun ThemeBadge(label: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) { Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun BtnRow(
    modifier: Modifier, buttons: List<BtnDef>,
    isSecond: Boolean, accent: Color,
    onClick: (String) -> Unit
) {
    Row(modifier.fillMaxWidth()) {
        for (btn in buttons) {
            val is2nd = btn.label == "2nd"
            val bg = when {
                is2nd && isSecond   -> accent
                btn.type == BtnType.Num     -> NumColor
                btn.type == BtnType.Op      -> accent
                btn.type == BtnType.Clear   -> ClearColor
                btn.type == BtnType.Mem     -> MemColor
                btn.type == BtnType.Special -> ClearColor
                else                        -> FuncColor
            }
            val fg = when (btn.type) {
                BtnType.Mem -> TextSecondary
                else -> if (isSecond && !is2nd) accent else TextPrimary
            }
            val fs = when {
                btn.label.length > 5 -> 12.sp
                btn.label.length > 4 -> 13.sp
                btn.label.length > 3 -> 15.sp
                else                 -> 20.sp
            }
            CalcBtn(btn.label, bg, fg, Modifier.weight(1f), fs) { onClick(btn.action) }
        }
    }
}

@Composable
private fun CalcBtn(
    label: String, bg: Color, fg: Color,
    modifier: Modifier, fontSize: TextUnit = 20.sp,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.87f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 750f),
        label = "btn_scale"
    )
    Box(
        modifier
            .fillMaxHeight()
            .padding(3.dp)
            .scale(scale)
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .combinedClickable(
                interactionSource = source, indication = null,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
                onLongClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = fontSize, fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp))
    }
}

@Composable
private fun HistoryPanel(history: List<Pair<String, String>>, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().heightIn(max = 200.dp).background(Color(0xFF2C2C2E))
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Historial", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("✕", color = TextSecondary, fontSize = 15.sp,
                modifier = Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onClose, onLongClick = {}
                ))
        }
        if (history.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Sin historial aún", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn {
                items(history) { (expr, res) ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)) {
                        Text(expr, color = TextSecondary, fontSize = 13.sp,
                            textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        Text(res, color = TextPrimary, fontSize = 19.sp,
                            textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    }
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF3A3A3C)))
                }
            }
        }
    }
}
