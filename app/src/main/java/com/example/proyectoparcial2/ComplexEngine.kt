package com.example.proyectoparcial2

import kotlin.math.*

data class Complex(val re: Double, val im: Double = 0.0) {
    operator fun plus(o: Complex) = Complex(re + o.re, im + o.im)
    operator fun minus(o: Complex) = Complex(re - o.re, im - o.im)
    operator fun times(o: Complex) = Complex(re * o.re - im * o.im, re * o.im + im * o.re)
    operator fun div(o: Complex): Complex {
        val d = o.re * o.re + o.im * o.im
        if (d == 0.0) throw ArithmeticException("Division by zero")
        return Complex((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d)
    }
    operator fun unaryMinus() = Complex(-re, -im)
    val abs get() = hypot(re, im)
    val arg get() = atan2(im, re)
    val isFinite get() = re.isFinite() && im.isFinite()
}

private val ONE = Complex(1.0)
private val I = Complex(0.0, 1.0)

private fun cexp(z: Complex): Complex { val m = exp(z.re); return Complex(m * cos(z.im), m * sin(z.im)) }

private fun cln(z: Complex): Complex {
    if (z.re == 0.0 && z.im == 0.0) throw ArithmeticException("NaN")
    return Complex(ln(z.abs), z.arg)
}

private fun cpow(a: Complex, b: Complex): Complex {
    if (b.im == 0.0 && b.re == floor(b.re) && abs(b.re) <= 1024) {
        // Exact repeated squaring for integer powers: i^2 = -1 exactly
        var n = abs(b.re).toInt(); var base = a; var r = ONE
        while (n > 0) { if (n and 1 == 1) r *= base; base *= base; n = n shr 1 }
        return if (b.re < 0) ONE / r else r
    }
    if (a.re == 0.0 && a.im == 0.0) return if (b.re > 0) Complex(0.0) else throw ArithmeticException("NaN")
    return cexp(b * cln(a))
}

private fun csqrt(z: Complex): Complex {
    val r = z.abs
    val im = sqrt((r - z.re) / 2)
    return Complex(sqrt((r + z.re) / 2), if (z.im < 0) -im else im)
}

private fun csin(z: Complex) = Complex(sin(z.re) * cosh(z.im), cos(z.re) * sinh(z.im))
private fun ccos(z: Complex) = Complex(cos(z.re) * cosh(z.im), -sin(z.re) * sinh(z.im))
private fun csinh(z: Complex) = Complex(sinh(z.re) * cos(z.im), cosh(z.re) * sin(z.im))
private fun ccosh(z: Complex) = Complex(cosh(z.re) * cos(z.im), sinh(z.re) * sin(z.im))

private val CNAMES = setOf(
    "pi", "e", "phi", "i", "sqrt", "cbrt", "exp", "ln", "log", "sin", "cos", "tan",
    "sinh", "cosh", "tanh", "abs", "arg", "conj", "re", "im", "rec"
)

private class CParser(private val s: String, private val mode: AngleMode, private val vars: Map<Char, Double>) {
    var i = 0

    fun atEnd(): Boolean { skipSpaces(); return i >= s.length }

    fun parseExpr(): Complex {
        var v = parseTerm()
        while (true) {
            v = when {
                eat('+') -> v + parseTerm()
                eat('-') -> v - parseTerm()
                else -> return v
            }
        }
    }

    private fun parseTerm(): Complex {
        var v = parseUnary()
        while (true) {
            skipSpaces()
            v = when {
                eat('*') -> v * parseUnary()
                eat('/') -> v / parseUnary()
                i < s.length && (s[i] == '(' || s[i].isLetter()) -> v * parseUnary()
                else -> return v
            }
        }
    }

    private fun parseUnary(): Complex {
        if (eat('-')) return -parsePower()
        eat('+')
        return parsePower()
    }

    private fun parsePower(): Complex {
        val base = parsePostfix()
        return if (eat('^')) cpow(base, parseUnary()) else base
    }

    private fun parsePostfix(): Complex {
        var v = parsePrimary()
        while (true) {
            v = when {
                eat('%') -> v / Complex(100.0)
                eat('!') -> {
                    if (v.im != 0.0 || v.re < 0 || v.re != floor(v.re)) throw ArithmeticException("NaN")
                    var r = 1.0; for (k in 2..v.re.toLong()) r *= k; Complex(r)
                }
                else -> return v
            }
        }
    }

    private fun parsePrimary(): Complex {
        skipSpaces()
        if (i >= s.length) throw IllegalArgumentException("Unexpected end")
        return when {
            s[i].isDigit() || s[i] == '.' -> Complex(parseNumber())
            eat('(') -> { val v = parseExpr(); eat(')'); v }
            eat('|') -> { val v = parseExpr(); eat('|'); Complex(v.abs) }
            s[i] in 'A'..'F' -> { val c = s[i++]; Complex(vars[c] ?: throw IllegalArgumentException("Variable $c vacía")) }
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
        return s.substring(start, i).toDoubleOrNull() ?: throw IllegalArgumentException("Número inválido")
    }

    private fun parseWord(): Complex {
        val start = i
        while (i < s.length && (s[i].isLetterOrDigit() || s[i] == '_')) i++
        var word = s.substring(start, i).lowercase()
        if (word !in CNAMES) {
            word = CNAMES.filter { word.startsWith(it) }.maxByOrNull { it.length }
                ?: throw IllegalArgumentException("Unknown: $word")
            i = start + word.length
        }
        return when (word) {
            "pi" -> Complex(PI)
            "e" -> Complex(E)
            "phi" -> Complex((1 + sqrt(5.0)) / 2)
            "i" -> I
            "sqrt" -> csqrt(one())
            "cbrt" -> one().let { if (it.im == 0.0) Complex(cbrt(it.re)) else cpow(it, Complex(1.0 / 3)) }
            "exp" -> cexp(one())
            "ln" -> cln(one())
            "log" -> cln(one()) / Complex(ln(10.0))
            "sin" -> csin(toRad(one()))
            "cos" -> ccos(toRad(one()))
            "tan" -> toRad(one()).let { csin(it) / ccos(it) }
            "sinh" -> csinh(one())
            "cosh" -> ccosh(one())
            "tanh" -> one().let { csinh(it) / ccosh(it) }
            "abs" -> Complex(one().abs)
            "arg" -> Complex(fromRad(one().arg))
            "conj" -> one().let { Complex(it.re, -it.im) }
            "re" -> Complex(one().re)
            "im" -> Complex(one().im)
            "rec" -> ONE / one()
            else -> throw IllegalArgumentException("Unknown: $word")
        }
    }

    private fun one(): Complex {
        if (!eat('(')) throw IllegalArgumentException("Expected '('")
        val v = parseExpr(); eat(')'); return v
    }

    private fun toRad(z: Complex) = when (mode) {
        AngleMode.DEG -> z * Complex(PI / 180)
        AngleMode.RAD -> z
        AngleMode.GRAD -> z * Complex(PI / 200)
    }

    private fun fromRad(v: Double) = when (mode) {
        AngleMode.DEG -> Math.toDegrees(v)
        AngleMode.RAD -> v
        AngleMode.GRAD -> v * 200 / PI
    }

    private fun skipSpaces() { while (i < s.length && s[i] == ' ') i++ }

    private fun eat(c: Char): Boolean {
        skipSpaces()
        return if (i < s.length && s[i] == c) { i++; true } else false
    }
}

object ComplexEngine {
    fun evaluate(expr: String, mode: AngleMode, vars: Map<Char, Double> = emptyMap()): Complex {
        if (expr.isBlank()) throw IllegalArgumentException("Empty")
        val p = CParser(CalculatorEngine.normalize(expr), mode, vars)
        val z = p.parseExpr()
        if (!p.atEnd()) throw IllegalArgumentException("Sintaxis")
        if (!z.isFinite) throw ArithmeticException("NaN")
        return clean(z)
    }

    /** Drops floating-point dust such as 1e-17i. */
    fun clean(z: Complex): Complex {
        val eps = 1e-12 * max(1.0, z.abs)
        return Complex(if (abs(z.re) < eps) 0.0 else z.re, if (abs(z.im) < eps) 0.0 else z.im)
    }

    fun format(z: Complex, decimals: Int = -1): String {
        val fmt = { v: Double -> CalculatorEngine.formatResult(v, decimals) }
        if (z.im == 0.0) return fmt(z.re)
        val imAbs = abs(z.im)
        val imStr = if (imAbs == 1.0) "i" else "${fmt(imAbs)}i"
        if (z.re == 0.0) return (if (z.im < 0) "-" else "") + imStr
        return "${fmt(z.re)} ${if (z.im < 0) "−" else "+"} $imStr"
    }
}

class EvalResult(val text: String, val value: Double?)

/** Real evaluation first; falls back to complex numbers (√(-4) = 2i, expressions with i). */
fun evaluateAny(expr: String, mode: AngleMode, vars: Map<Char, Double>, decimals: Int = -1): EvalResult {
    try {
        val v = CalculatorEngine.evaluate(expr, mode, vars = vars)
        return EvalResult(CalculatorEngine.formatResult(v, decimals), v)
    } catch (e: Exception) {
        val z = try { ComplexEngine.evaluate(expr, mode, vars) } catch (_: Exception) { throw e }
        return if (z.im == 0.0) EvalResult(CalculatorEngine.formatResult(z.re, decimals), z.re)
        else EvalResult(ComplexEngine.format(z, decimals), null)
    }
}
