package com.example.proyectoparcial2

import kotlin.math.*

enum class AngleMode { DEG, RAD, GRAD }

object CalculatorEngine {

    fun evaluate(expr: String, angleMode: AngleMode, x: Double? = null): Double {
        if (expr.isBlank()) throw IllegalArgumentException("Empty")
        val normalized = expr
            .replace("×", "*").replace("÷", "/").replace("−", "-")
            .replace("π", "(pi)").replace("φ", "(phi)")
            .replace("√", "sqrt").replace("∛", "cbrt")
            .trim()
        val parser = Parser(normalized, angleMode, x)
        val result = parser.parseExpr()
        if (!parser.atEnd()) throw IllegalArgumentException("Sintaxis")
        if (result.isNaN()) throw ArithmeticException("NaN")
        return result
    }

    fun formatResult(v: Double): String {
        if (v.isNaN()) return "Undefined"
        if (v.isInfinite()) return if (v > 0) "∞" else "-∞"
        if (v == 0.0 || v == -0.0) return "0"
        val abs = abs(v)
        return when {
            v == floor(v) && abs < 1e15 -> v.toLong().toString()
            abs >= 1e-6 && abs < 1e12 -> {
                val s = "%.10g".format(java.util.Locale.US, v)
                if ('e' in s || 'E' in s) s
                else s.trimEnd('0').trimEnd('.')
            }
            else -> {
                val s = "%.6e".format(java.util.Locale.US, v)
                s.replace(Regex("e([+-])0+(\\d)"), "e$1$2")
                    .replace("e+", "×10^").replace("e-", "×10^-")
            }
        }
    }

    /** Simple fraction (e.g. "1/3") via continued fractions, or null if none fits. */
    fun toFraction(v: Double, maxDen: Long = 10_000): String? {
        if (v.isNaN() || v.isInfinite() || v == floor(v) || abs(v) > 1e9) return null
        val target = abs(v)
        var h0 = 0L; var h1 = 1L; var k0 = 1L; var k1 = 0L
        var x = target
        repeat(30) {
            val a = floor(x).toLong()
            val h2 = a * h1 + h0; val k2 = a * k1 + k0
            if (k2 > maxDen) return null
            h0 = h1; h1 = h2; k0 = k1; k1 = k2
            if (abs(h1.toDouble() / k1 - target) < 1e-9 * max(1.0, target)) {
                return if (k1 == 1L) null else "${if (v < 0) "-" else ""}$h1/$k1"
            }
            val frac = x - a
            if (frac < 1e-12) return null
            x = 1 / frac
        }
        return null
    }
}

private val NAMES = setOf(
    "pi", "e", "phi", "inf", "x", "rand",
    "sin", "cos", "tan", "asin", "arcsin", "acos", "arccos", "atan", "arctan", "atan2",
    "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
    "sqrt", "cbrt", "exp", "log", "log10", "log2", "ln", "abs", "ceil", "floor", "round", "sign", "rec",
    "ncr", "npr", "gcd", "lcm", "max", "min", "pow", "hypot", "logb"
)

private class Parser(private val s: String, private val mode: AngleMode, private val xVal: Double?) {
    var i = 0

    fun atEnd(): Boolean { skipSpaces(); return i >= s.length }

    fun parseExpr(): Double {
        var v = parseTerm()
        while (i < s.length) {
            skipSpaces()
            if (i >= s.length) break
            v = when {
                eat('+') -> v + parseTerm()
                eat('-') -> v - parseTerm()
                else -> break
            }
        }
        return v
    }

    private fun parseTerm(): Double {
        var v = parseUnary()
        while (i < s.length) {
            skipSpaces()
            if (i >= s.length) break
            v = when {
                eat('*') -> v * parseUnary()
                eat('/') -> {
                    val d = parseUnary()
                    if (d == 0.0) throw ArithmeticException("Division by zero")
                    v / d
                }
                eatWord("mod") -> v.rem(parseUnary())
                i < s.length && (s[i] == '(' || s[i].isLetter()) -> v * parseUnary()
                else -> break
            }
        }
        return v
    }

    private fun parseUnary(): Double {
        skipSpaces()
        if (i < s.length && eat('-')) return -parsePower()
        if (i < s.length) eat('+')
        return parsePower()
    }

    private fun parsePower(): Double {
        val base = parsePostfix()
        skipSpaces()
        return if (eat('^')) base.pow(parseUnary()) else base
    }

    private fun parsePostfix(): Double {
        var v = parsePrimary()
        while (i < s.length) {
            skipSpaces()
            v = when {
                eat('!') -> factorial(v)
                eat('%') -> v / 100.0
                else -> break
            }
        }
        return v
    }

    private fun parsePrimary(): Double {
        skipSpaces()
        if (i >= s.length) throw IllegalArgumentException("Unexpected end")
        return when {
            s[i].isDigit() || s[i] == '.' -> parseNumber()
            eat('(') -> { val v = parseExpr(); skipSpaces(); eat(')'); v }
            eat('|') -> { val v = parseExpr(); skipSpaces(); eat('|'); abs(v) }
            s[i].isLetter() -> parseWord()
            else -> throw IllegalArgumentException("Unexpected '${s[i]}'")
        }
    }

    private fun parseNumber(): Double {
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        if (i < s.length && (s[i] == 'e' || s[i] == 'E') &&
            i + 1 < s.length && (s[i + 1].isDigit() || s[i + 1] == '+' || s[i + 1] == '-')) {
            i++
            if (s[i] == '+' || s[i] == '-') i++
            while (i < s.length && s[i].isDigit()) i++
        }
        return s.substring(start, i).toDoubleOrNull()
            ?: throw IllegalArgumentException("Número inválido")
    }

    private fun parseWord(): Double {
        val start = i
        while (i < s.length && (s[i].isLetterOrDigit() || s[i] == '_')) i++
        var word = s.substring(start, i).lowercase()
        if (word !in NAMES) {
            // Adjacent names like "esin" or "xcos": take the longest known prefix
            word = NAMES.filter { word.startsWith(it) }.maxByOrNull { it.length }
                ?: throw IllegalArgumentException("Unknown: $word")
            i = start + word.length
        }
        return when (word) {
            "pi" -> PI
            "e" -> E
            "phi" -> (1.0 + sqrt(5.0)) / 2.0
            "inf" -> Double.POSITIVE_INFINITY
            "x" -> xVal ?: throw IllegalArgumentException("x sin valor")
            "rand" -> Math.random()
            "sin" -> sin(toRad(oneArg()))
            "cos" -> cos(toRad(oneArg()))
            "tan" -> { val a = toRad(oneArg()); if (abs(cos(a)) < 1e-10) Double.NaN else tan(a) }
            "asin", "arcsin" -> fromRad(asin(oneArg()))
            "acos", "arccos" -> fromRad(acos(oneArg()))
            "atan", "arctan" -> fromRad(atan(oneArg()))
            "atan2" -> { val (y, x) = twoArgs(); fromRad(atan2(y, x)) }
            "sinh" -> sinh(oneArg())
            "cosh" -> cosh(oneArg())
            "tanh" -> tanh(oneArg())
            "asinh" -> { val x = oneArg(); ln(x + sqrt(x * x + 1)) }
            "acosh" -> { val x = oneArg(); if (x < 1) Double.NaN else ln(x + sqrt(x * x - 1)) }
            "atanh" -> { val x = oneArg(); if (abs(x) >= 1) Double.NaN else 0.5 * ln((1 + x) / (1 - x)) }
            "sqrt" -> sqrt(oneArg())
            "cbrt" -> cbrt(oneArg())
            "exp" -> exp(oneArg())
            "log", "log10" -> { val x = oneArg(); if (x <= 0) Double.NaN else log10(x) }
            "log2" -> { val x = oneArg(); if (x <= 0) Double.NaN else log2(x) }
            "ln" -> { val x = oneArg(); if (x <= 0) Double.NaN else ln(x) }
            "abs" -> abs(oneArg())
            "ceil" -> ceil(oneArg())
            "floor" -> floor(oneArg())
            "round" -> round(oneArg())
            "sign" -> sign(oneArg())
            "rec" -> { val x = oneArg(); if (x == 0.0) Double.POSITIVE_INFINITY else 1.0 / x }
            "ncr" -> { val (n, r) = twoArgs(); combinations(n, r) }
            "npr" -> { val (n, r) = twoArgs(); permutations(n, r) }
            "gcd" -> { val (a, b) = twoArgs(); gcdF(a, b) }
            "lcm" -> { val (a, b) = twoArgs(); lcmF(a, b) }
            "max" -> { val (a, b) = twoArgs(); max(a, b) }
            "min" -> { val (a, b) = twoArgs(); min(a, b) }
            "pow" -> { val (a, b) = twoArgs(); a.pow(b) }
            "hypot" -> { val (a, b) = twoArgs(); hypot(a, b) }
            "logb" -> { val (base, x) = twoArgs(); if (x <= 0 || base <= 0 || base == 1.0) Double.NaN else log(x, base) }
            else -> throw IllegalArgumentException("Unknown: $word")
        }
    }

    private fun oneArg(): Double {
        skipSpaces()
        if (!eat('(')) throw IllegalArgumentException("Expected '('")
        val v = parseExpr(); skipSpaces(); eat(')'); return v
    }

    private fun twoArgs(): Pair<Double, Double> {
        skipSpaces()
        if (!eat('(')) throw IllegalArgumentException("Expected '('")
        val a = parseExpr(); skipSpaces()
        if (!eat(',')) throw IllegalArgumentException("Expected ','")
        val b = parseExpr(); skipSpaces(); eat(')'); return a to b
    }

    private fun toRad(v: Double) = when (mode) {
        AngleMode.DEG -> Math.toRadians(v)
        AngleMode.RAD -> v
        AngleMode.GRAD -> v * PI / 200.0
    }

    private fun fromRad(v: Double) = when (mode) {
        AngleMode.DEG -> Math.toDegrees(v)
        AngleMode.RAD -> v
        AngleMode.GRAD -> v * 200.0 / PI
    }

    private fun skipSpaces() { while (i < s.length && s[i] == ' ') i++ }

    private fun eat(c: Char): Boolean {
        skipSpaces()
        return if (i < s.length && s[i] == c) { i++; true } else false
    }

    private fun eatWord(w: String): Boolean {
        skipSpaces()
        if (i + w.length > s.length) return false
        if (!s.regionMatches(i, w, 0, w.length, ignoreCase = true)) return false
        if (i + w.length < s.length && (s[i + w.length].isLetterOrDigit() || s[i + w.length] == '_')) return false
        i += w.length; return true
    }
}

private fun factorial(n: Double): Double {
    if (n < 0 || n != floor(n)) throw ArithmeticException("Factorial undefined for $n")
    if (n > 170) return Double.POSITIVE_INFINITY
    var r = 1.0; for (k in 2..n.toLong()) r *= k; return r
}

private fun combinations(n: Double, r: Double): Double {
    if (r < 0 || r > n || n < 0) return 0.0
    if (r == 0.0 || r == n) return 1.0
    val k = minOf(r, n - r)
    return factorial(n) / (factorial(k) * factorial(n - k))
}

private fun permutations(n: Double, r: Double): Double {
    if (r < 0 || r > n || n < 0) return 0.0
    return factorial(n) / factorial(n - r)
}

private fun gcdF(a: Double, b: Double): Double {
    var x = abs(floor(a)).toLong(); var y = abs(floor(b)).toLong()
    while (y != 0L) { val t = y; y = x % y; x = t }; return x.toDouble()
}

private fun lcmF(a: Double, b: Double): Double {
    val g = gcdF(a, b); return if (g == 0.0) 0.0 else abs(floor(a) * floor(b)) / g
}
