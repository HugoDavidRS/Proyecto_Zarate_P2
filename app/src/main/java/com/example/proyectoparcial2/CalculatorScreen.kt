package com.example.proyectoparcial2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.floor

// ── Palette ──────────────────────────────────────────────────────────────────
private val BgColor       = Color(0xFF1C1C1E)
private val NumColor      = Color(0xFF3A3A3C)
private val OpColor       = Color(0xFFFF9F0A)
private val FuncColor     = Color(0xFF2C2C2E)
private val ClearColor    = Color(0xFF636366)
private val MemColor      = Color(0xFF252527)
private val SecondActive  = Color(0xFFFF9F0A)
private val EqualColor    = Color(0xFFFF9F0A)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8E8E93)
private val TextOp        = Color(0xFFFFFFFF)
private val ErrorColor    = Color(0xFFFF453A)

// ── Button definition ─────────────────────────────────────────────────────────
private enum class BtnType { Num, Op, Equal, Func, Clear, Mem, Special }

private data class BtnDef(
    val label: String,
    val action: String = label,
    val type: BtnType = BtnType.Func,
    val secondLabel: String? = null,
    val secondAction: String? = null
)

@Composable
fun CalculatorScreen() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var angleMode by remember { mutableStateOf(AngleMode.DEG) }
    var memory by remember { mutableStateOf(0.0) }
    var hasMemory by remember { mutableStateOf(false) }
    var isSecond by remember { mutableStateOf(false) }
    var lastAnswer by remember { mutableStateOf(0.0) }
    var history by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var showHistory by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    // Live evaluation
    LaunchedEffect(expression, angleMode) {
        if (expression.isEmpty()) { result = ""; isError = false; return@LaunchedEffect }
        try {
            val ansVal = if (lastAnswer == floor(lastAnswer) && abs(lastAnswer) < 1e15)
                lastAnswer.toLong().toString() else lastAnswer.toString()
            val exprForEval = expression.replace("Ans", ansVal)
            val v = CalculatorEngine.evaluate(exprForEval, angleMode)
            result = CalculatorEngine.formatResult(v)
            isError = false
        } catch (_: Exception) {
            result = ""; isError = false // don't show error while typing
        }
    }

    fun onBtn(action: String) {
        when (action) {
            "⌫" -> if (expression.isNotEmpty()) expression = expression.dropLast(1)
            "AC" -> { expression = ""; result = ""; isError = false }
            "=" -> {
                if (expression.isEmpty()) return
                try {
                    val ansVal = if (lastAnswer == floor(lastAnswer) && abs(lastAnswer) < 1e15)
                        lastAnswer.toLong().toString() else lastAnswer.toString()
                    val exprForEval = expression.replace("Ans", ansVal)
                    val v = CalculatorEngine.evaluate(exprForEval, angleMode)
                    val r = CalculatorEngine.formatResult(v)
                    history = (listOf(expression to r) + history).take(50)
                    lastAnswer = v
                    expression = r
                    result = ""
                    isError = false
                } catch (e: Exception) {
                    result = e.message?.take(20) ?: "Error"
                    isError = true
                }
            }
            "DEG", "RAD", "GRAD" -> {
                angleMode = when (angleMode) {
                    AngleMode.DEG -> AngleMode.RAD
                    AngleMode.RAD -> AngleMode.GRAD
                    AngleMode.GRAD -> AngleMode.DEG
                }
            }
            "2nd" -> isSecond = !isSecond
            "MC" -> { memory = 0.0; hasMemory = false }
            "MR" -> expression += CalculatorEngine.formatResult(memory)
            "M+" -> {
                try {
                    val ansVal = lastAnswer.toString()
                    memory += CalculatorEngine.evaluate(expression.replace("Ans", ansVal), angleMode)
                    hasMemory = true
                } catch (_: Exception) {}
            }
            "M-" -> {
                try {
                    val ansVal = lastAnswer.toString()
                    memory -= CalculatorEngine.evaluate(expression.replace("Ans", ansVal), angleMode)
                    hasMemory = true
                } catch (_: Exception) {}
            }
            "±" -> expression = when {
                expression.isEmpty() -> "-"
                expression.startsWith("-(") && expression.endsWith(")") ->
                    expression.drop(2).dropLast(1)
                expression.startsWith("-") -> expression.drop(1)
                else -> "-($expression)"
            }
            "Ans" -> expression += "Ans"
            "HIST" -> showHistory = !showHistory
            else -> {
                isError = false
                expression += action
            }
        }
    }

    // Button grid definition (label, action, type, secondLabel, secondAction)
    val row0 = listOf(
        BtnDef(angleMode.name, action = "DEG", type = BtnType.Special),
        BtnDef("(", type = BtnType.Clear),
        BtnDef(")", type = BtnType.Clear),
        BtnDef("%", type = BtnType.Clear),
        BtnDef("⌫", type = BtnType.Clear)
    )
    val row1 = listOf(
        BtnDef("2nd", type = BtnType.Special),
        BtnDef(if (isSecond) "√" else "x²", action = if (isSecond) "√(" else "^2"),
        BtnDef(if (isSecond) "∛" else "x³", action = if (isSecond) "cbrt(" else "^3"),
        BtnDef("xⁿ", action = "^"),
        BtnDef("÷", type = BtnType.Op)
    )
    val row2 = listOf(
        BtnDef(if (isSecond) "asin" else "sin", action = if (isSecond) "asin(" else "sin("),
        BtnDef(if (isSecond) "acos" else "cos", action = if (isSecond) "acos(" else "cos("),
        BtnDef(if (isSecond) "atan" else "tan", action = if (isSecond) "atan(" else "tan("),
        BtnDef(if (isSecond) "10^x" else "log", action = if (isSecond) "10^" else "log("),
        BtnDef("×", type = BtnType.Op)
    )
    val row3 = listOf(
        BtnDef(if (isSecond) "asinh" else "sinh", action = if (isSecond) "asinh(" else "sinh("),
        BtnDef(if (isSecond) "acosh" else "cosh", action = if (isSecond) "acosh(" else "cosh("),
        BtnDef(if (isSecond) "atanh" else "tanh", action = if (isSecond) "atanh(" else "tanh("),
        BtnDef(if (isSecond) "eˣ" else "ln", action = if (isSecond) "exp(" else "ln("),
        BtnDef("−", action = "-", type = BtnType.Op)
    )
    val row4 = listOf(
        BtnDef("π", action = "π"),
        BtnDef("e", action = "e"),
        BtnDef("!", action = "!"),
        BtnDef(if (isSecond) "nPr" else "nCr", action = if (isSecond) "npr(" else "ncr("),
        BtnDef("+", type = BtnType.Op)
    )
    // Memory + number pad rows (4 cols, = button spans right)
    val memRow = listOf(
        BtnDef("MC", type = BtnType.Mem),
        BtnDef("MR", type = BtnType.Mem),
        BtnDef("M+", type = BtnType.Mem),
        BtnDef("M-", type = BtnType.Mem)
    )
    val numRow1 = listOf(
        BtnDef("7", type = BtnType.Num), BtnDef("8", type = BtnType.Num),
        BtnDef("9", type = BtnType.Num), BtnDef("1/x", action = "rec(")
    )
    val numRow2 = listOf(
        BtnDef("4", type = BtnType.Num), BtnDef("5", type = BtnType.Num),
        BtnDef("6", type = BtnType.Num), BtnDef("mod", action = " mod ")
    )
    val numRow3 = listOf(
        BtnDef("1", type = BtnType.Num), BtnDef("2", type = BtnType.Num),
        BtnDef("3", type = BtnType.Num), BtnDef("EE", action = "×10^")
    )
    val numRow4 = listOf(
        BtnDef("±", type = BtnType.Num), BtnDef("0", type = BtnType.Num),
        BtnDef(".", type = BtnType.Num), BtnDef("Ans")
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(BgColor)
            .systemBarsPadding()
    ) {
        // History panel
        AnimatedVisibility(
            visible = showHistory,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            HistoryPanel(history) { showHistory = false }
        }

        // Display
        DisplayArea(
            expression = expression,
            result = result,
            isError = isError,
            hasMemory = hasMemory,
            angleMode = angleMode,
            isSecond = isSecond,
            showHistory = showHistory,
            modifier = Modifier.weight(2.5f),
            onHistoryClick = { showHistory = !showHistory }
        )

        // Scientific rows
        Column(Modifier.weight(7.5f)) {
            for (row in listOf(row0, row1, row2, row3, row4)) {
                BtnRow(Modifier.weight(1f), row, isSecond) { onBtn(it) }
            }
            // Number pad + spanning = button
            Row(Modifier.weight(5f)) {
                Column(Modifier.weight(4f)) {
                    for (row in listOf(memRow, numRow1, numRow2, numRow3, numRow4)) {
                        BtnRow(Modifier.weight(1f), row, isSecond) { onBtn(it) }
                    }
                }
                // = button spanning all 5 number rows
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EqualColor)
                        .clickable { onBtn("=") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("=", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Light)
                }
            }
        }
    }
}

@Composable
private fun DisplayArea(
    expression: String, result: String, isError: Boolean,
    hasMemory: Boolean, angleMode: AngleMode, isSecond: Boolean,
    showHistory: Boolean, modifier: Modifier, onHistoryClick: () -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Top row: badges + history toggle
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(angleMode.name, OpColor)
                if (hasMemory) Badge("M", Color(0xFF30D158))
                if (isSecond) Badge("2nd", SecondActive)
            }
            Text(
                if (showHistory) "▲ History" else "▼ History",
                color = TextSecondary, fontSize = 12.sp,
                modifier = Modifier.clickable { onHistoryClick() }
            )
        }
        Spacer(Modifier.weight(1f))
        // Expression line
        val scrollState = rememberScrollState()
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = expression.ifEmpty { "0" },
                color = if (isError) ErrorColor else TextSecondary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
        // Result line
        if (result.isNotEmpty()) {
            val scrollState2 = rememberScrollState()
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState2),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = result,
                    color = if (isError) ErrorColor else TextPrimary,
                    fontSize = adaptiveFontSize(result),
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

private fun adaptiveFontSize(s: String): TextUnit = when {
    s.length <= 9 -> 52.sp
    s.length <= 14 -> 40.sp
    s.length <= 20 -> 30.sp
    else -> 22.sp
}

@Composable
private fun Badge(label: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BtnRow(modifier: Modifier, buttons: List<BtnDef>, isSecond: Boolean, onClick: (String) -> Unit) {
    Row(modifier.fillMaxWidth()) {
        for (btn in buttons) {
            val isSecondBtn = btn.label == "2nd"
            val bgColor = when {
                isSecondBtn && isSecond -> SecondActive
                btn.type == BtnType.Num -> NumColor
                btn.type == BtnType.Op -> OpColor
                btn.type == BtnType.Clear -> ClearColor
                btn.type == BtnType.Mem -> MemColor
                btn.type == BtnType.Special -> ClearColor
                else -> FuncColor
            }
            val textColor = when (btn.type) {
                BtnType.Num -> TextPrimary
                BtnType.Op -> TextOp
                BtnType.Clear -> TextPrimary
                BtnType.Mem -> TextSecondary
                else -> if (isSecond && btn.label != "2nd") OpColor else TextPrimary
            }
            CalcBtn(
                label = btn.label,
                bg = bgColor,
                fg = textColor,
                modifier = Modifier.weight(1f),
                fontSize = if (btn.label.length > 4) 13.sp else if (btn.label.length > 3) 15.sp else 20.sp
            ) { onClick(btn.action) }
        }
    }
}

@Composable
private fun CalcBtn(
    label: String, bg: Color, fg: Color,
    modifier: Modifier, fontSize: TextUnit = 20.sp,
    onClick: () -> Unit
) {
    Box(
        modifier
            .fillMaxHeight()
            .padding(3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = fontSize,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun HistoryPanel(history: List<Pair<String, String>>, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .background(Color(0xFF2C2C2E))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("✕", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.clickable { onClose() })
        }
        if (history.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No history yet", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn {
                items(history) { (expr, res) ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(expr, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth())
                        Text(res, color = TextPrimary, fontSize = 18.sp, textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth())
                    }
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF48484A)))
                }
            }
        }
    }
}
