package com.example.proyectoparcial2

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

// ── Statistics ────────────────────────────────────────────────────────────────

data class Stats(
    val n: Int, val sum: Double, val sumSq: Double, val mean: Double, val median: Double,
    val modes: List<Double>, val min: Double, val max: Double,
    val varPop: Double, val varSample: Double
) {
    val sdPop get() = sqrt(varPop)
    val sdSample get() = sqrt(varSample)
}

fun parseNumberList(s: String): List<Double> =
    s.split(Regex("[,;\\s]+")).mapNotNull { it.trim().toDoubleOrNull() }

fun computeStats(xs: List<Double>): Stats? {
    if (xs.isEmpty()) return null
    val n = xs.size
    val sum = xs.sum()
    val mean = sum / n
    val sorted = xs.sorted()
    val median = if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2
    val counts = xs.groupingBy { it }.eachCount()
    val top = counts.values.max()
    val modes = if (top > 1) counts.filterValues { it == top }.keys.sorted() else emptyList()
    val ss = xs.sumOf { (it - mean).pow(2) }
    return Stats(
        n, sum, xs.sumOf { it * it }, mean, median, modes, sorted.first(), sorted.last(),
        ss / n, if (n > 1) ss / (n - 1) else Double.NaN
    )
}

// ── Unit conversion ───────────────────────────────────────────────────────────

class UnitDef(val name: String, val symbol: String, val toBase: (Double) -> Double, val fromBase: (Double) -> Double)
class UnitCategory(val name: String, val units: List<UnitDef>)

private fun lin(name: String, symbol: String, factor: Double) =
    UnitDef(name, symbol, { it * factor }, { it / factor })

fun convertUnit(v: Double, from: UnitDef, to: UnitDef) = to.fromBase(from.toBase(v))

val UNIT_CATEGORIES = listOf(
    UnitCategory("Longitud", listOf(
        lin("Metro", "m", 1.0), lin("Kilómetro", "km", 1000.0), lin("Centímetro", "cm", 0.01),
        lin("Milímetro", "mm", 0.001), lin("Micrómetro", "µm", 1e-6), lin("Milla", "mi", 1609.344),
        lin("Yarda", "yd", 0.9144), lin("Pie", "ft", 0.3048), lin("Pulgada", "in", 0.0254),
        lin("Milla náutica", "nmi", 1852.0), lin("Año luz", "ly", 9.4607304725808e15)
    )),
    UnitCategory("Masa", listOf(
        lin("Kilogramo", "kg", 1.0), lin("Gramo", "g", 0.001), lin("Miligramo", "mg", 1e-6),
        lin("Tonelada", "t", 1000.0), lin("Libra", "lb", 0.45359237), lin("Onza", "oz", 0.028349523125),
        lin("Quilate", "ct", 0.0002)
    )),
    UnitCategory("Temperatura", listOf(
        UnitDef("Celsius", "°C", { it }, { it }),
        UnitDef("Fahrenheit", "°F", { (it - 32) * 5 / 9 }, { it * 9 / 5 + 32 }),
        UnitDef("Kelvin", "K", { it - 273.15 }, { it + 273.15 })
    )),
    UnitCategory("Área", listOf(
        lin("Metro²", "m²", 1.0), lin("Kilómetro²", "km²", 1e6), lin("Centímetro²", "cm²", 1e-4),
        lin("Hectárea", "ha", 1e4), lin("Acre", "ac", 4046.8564224), lin("Pie²", "ft²", 0.09290304),
        lin("Pulgada²", "in²", 0.00064516)
    )),
    UnitCategory("Volumen", listOf(
        lin("Litro", "L", 1.0), lin("Mililitro", "mL", 0.001), lin("Metro³", "m³", 1000.0),
        lin("Galón (US)", "gal", 3.785411784), lin("Cuarto (US)", "qt", 0.946352946),
        lin("Taza (US)", "cup", 0.2365882365), lin("Onza líquida", "fl oz", 0.0295735295625)
    )),
    UnitCategory("Velocidad", listOf(
        lin("Metro/segundo", "m/s", 1.0), lin("Kilómetro/hora", "km/h", 1 / 3.6),
        lin("Milla/hora", "mph", 0.44704), lin("Nudo", "kn", 1852.0 / 3600), lin("Pie/segundo", "ft/s", 0.3048),
        lin("Velocidad del sonido", "Mach", 343.0)
    )),
    UnitCategory("Tiempo", listOf(
        lin("Segundo", "s", 1.0), lin("Milisegundo", "ms", 0.001), lin("Minuto", "min", 60.0),
        lin("Hora", "h", 3600.0), lin("Día", "d", 86400.0), lin("Semana", "sem", 604800.0),
        lin("Año", "a", 31557600.0)
    )),
    UnitCategory("Datos", listOf(
        lin("Byte", "B", 1.0), lin("Bit", "bit", 0.125), lin("Kilobyte", "KB", 1024.0),
        lin("Megabyte", "MB", 1024.0.pow(2)), lin("Gigabyte", "GB", 1024.0.pow(3)),
        lin("Terabyte", "TB", 1024.0.pow(4))
    )),
    UnitCategory("Ángulo", listOf(
        lin("Grado", "°", 1.0), lin("Radián", "rad", 180 / PI), lin("Gradián", "grad", 0.9),
        lin("Vuelta", "rev", 360.0), lin("Minuto de arco", "′", 1.0 / 60), lin("Segundo de arco", "″", 1.0 / 3600)
    )),
    UnitCategory("Energía", listOf(
        lin("Joule", "J", 1.0), lin("Kilojoule", "kJ", 1000.0), lin("Caloría", "cal", 4.184),
        lin("Kilocaloría", "kcal", 4184.0), lin("Watt-hora", "Wh", 3600.0),
        lin("Kilowatt-hora", "kWh", 3.6e6), lin("Electronvoltio", "eV", 1.602176634e-19)
    )),
    UnitCategory("Presión", listOf(
        lin("Pascal", "Pa", 1.0), lin("Kilopascal", "kPa", 1000.0), lin("Bar", "bar", 1e5),
        lin("Atmósfera", "atm", 101325.0), lin("PSI", "psi", 6894.757293168),
        lin("Milímetro Hg", "mmHg", 133.322387415)
    ))
)

// ── Constants ─────────────────────────────────────────────────────────────────

class PhysConst(val symbol: String, val name: String, val value: Double, val unit: String)

val CONSTANT_GROUPS = listOf(
    "Matemáticas" to listOf(
        PhysConst("π", "Pi", PI, ""),
        PhysConst("e", "Número de Euler", kotlin.math.E, ""),
        PhysConst("φ", "Número áureo", (1 + sqrt(5.0)) / 2, ""),
        PhysConst("√2", "Raíz de 2", sqrt(2.0), ""),
        PhysConst("γ", "Euler–Mascheroni", 0.5772156649015329, "")
    ),
    "Física" to listOf(
        PhysConst("c", "Velocidad de la luz", 299792458.0, "m/s"),
        PhysConst("G", "Constante gravitacional", 6.67430e-11, "N·m²/kg²"),
        PhysConst("g", "Gravedad estándar", 9.80665, "m/s²"),
        PhysConst("h", "Constante de Planck", 6.62607015e-34, "J·s"),
        PhysConst("ħ", "Planck reducida", 1.054571817e-34, "J·s"),
        PhysConst("qₑ", "Carga elemental", 1.602176634e-19, "C"),
        PhysConst("mₑ", "Masa del electrón", 9.1093837015e-31, "kg"),
        PhysConst("mₚ", "Masa del protón", 1.67262192369e-27, "kg"),
        PhysConst("ε₀", "Permitividad del vacío", 8.8541878128e-12, "F/m"),
        PhysConst("μ₀", "Permeabilidad del vacío", 1.25663706212e-6, "N/A²"),
        PhysConst("σ", "Stefan–Boltzmann", 5.670374419e-8, "W/m²K⁴")
    ),
    "Química" to listOf(
        PhysConst("Nₐ", "Número de Avogadro", 6.02214076e23, "1/mol"),
        PhysConst("k", "Constante de Boltzmann", 1.380649e-23, "J/K"),
        PhysConst("R", "Constante de los gases", 8.314462618, "J/mol·K"),
        PhysConst("F", "Constante de Faraday", 96485.33212, "C/mol"),
        PhysConst("atm", "Atmósfera estándar", 101325.0, "Pa"),
        PhysConst("u", "Unidad de masa atómica", 1.66053906660e-27, "kg")
    )
)

// ── Roots of f(x) in an interval ─────────────────────────────────────────────

fun findRoots(f: (Double) -> Double, from: Double, to: Double, samples: Int = 400): List<Double> {
    val roots = mutableListOf<Double>()
    val step = (to - from) / samples
    var x0 = from
    var y0 = f(x0)
    for (k in 1..samples) {
        val x1 = from + k * step
        val y1 = f(x1)
        if (y0.isFinite() && y1.isFinite()) {
            if (y0 == 0.0) roots += x0
            else if (y0 * y1 < 0) {
                var lo = x0; var hi = x1; var flo = y0
                repeat(60) {
                    val mid = (lo + hi) / 2
                    val fm = f(mid)
                    if (fm.isFinite()) { if (flo * fm <= 0) hi = mid else { lo = mid; flo = fm } }
                }
                val r = (lo + hi) / 2
                // Sign changes across asymptotes (tan, 1/x) are not roots
                if (abs(f(r)) < 1e-6) roots += r
            }
        }
        x0 = x1; y0 = y1
    }
    if (y0 == 0.0) roots += x0
    return roots
}

// ── Equations ─────────────────────────────────────────────────────────────────

/** Roots of ax²+bx+c=0 as (label, value) pairs; complex roots as "re + im i". */
fun solveQuadratic(a: Double, b: Double, c: Double): List<Pair<String, String>> {
    val fmt = { v: Double -> CalculatorEngine.formatResult(v) }
    if (a == 0.0) {
        if (b == 0.0) return listOf("Resultado" to if (c == 0.0) "Infinitas soluciones" else "Sin solución")
        return listOf("x" to fmt(-c / b))
    }
    val d = b * b - 4 * a * c
    return when {
        d > 0 -> listOf("x₁" to fmt((-b + sqrt(d)) / (2 * a)), "x₂" to fmt((-b - sqrt(d)) / (2 * a)))
        d == 0.0 -> listOf("x (raíz doble)" to fmt(-b / (2 * a)))
        else -> {
            val re = fmt(-b / (2 * a))
            val im = fmt(abs(sqrt(-d) / (2 * a)))
            listOf("x₁" to "$re + ${im}i", "x₂" to "$re − ${im}i")
        }
    }
}

/** Solves a1x+b1y=c1, a2x+b2y=c2 (Cramer). Null when there is no unique solution. */
fun solveLinear2(a1: Double, b1: Double, c1: Double, a2: Double, b2: Double, c2: Double): Pair<Double, Double>? {
    val det = a1 * b2 - a2 * b1
    if (abs(det) < 1e-12) return null
    return (c1 * b2 - c2 * b1) / det to (a1 * c2 - a2 * c1) / det
}

// ── Calculus ──────────────────────────────────────────────────────────────────

fun derivative(f: (Double) -> Double, x: Double): Double {
    val h = 1e-5 * kotlin.math.max(1.0, abs(x))
    return (f(x + h) - f(x - h)) / (2 * h)
}

/** Simpson's rule; NaN if f is undefined somewhere in [a, b]. */
fun integrate(f: (Double) -> Double, a: Double, b: Double, n: Int = 1000): Double {
    if (a == b) return 0.0
    val h = (b - a) / n
    var s = f(a) + f(b)
    for (k in 1 until n) s += f(a + k * h) * (if (k % 2 == 1) 4 else 2)
    return s * h / 3
}

// ── Matrices ──────────────────────────────────────────────────────────────────

typealias Matrix = List<List<Double>>

fun matAdd(a: Matrix, b: Matrix): Matrix = a.indices.map { i -> a[i].indices.map { j -> a[i][j] + b[i][j] } }
fun matSub(a: Matrix, b: Matrix): Matrix = a.indices.map { i -> a[i].indices.map { j -> a[i][j] - b[i][j] } }
fun matMul(a: Matrix, b: Matrix): Matrix =
    a.indices.map { i -> b[0].indices.map { j -> a[i].indices.sumOf { k -> a[i][k] * b[k][j] } } }
fun matTranspose(a: Matrix): Matrix = a[0].indices.map { j -> a.indices.map { i -> a[i][j] } }

fun matDet(a: Matrix): Double {
    val m = a.map { it.toDoubleArray() }.toMutableList()
    val n = m.size
    var det = 1.0
    for (c in 0 until n) {
        val p = (c until n).maxBy { abs(m[it][c]) }
        if (abs(m[p][c]) < 1e-12) return 0.0
        if (p != c) { val t = m[p]; m[p] = m[c]; m[c] = t; det = -det }
        det *= m[c][c]
        for (r in c + 1 until n) {
            val f = m[r][c] / m[c][c]
            for (k in c until n) m[r][k] -= f * m[c][k]
        }
    }
    return det
}

/** Gauss–Jordan inverse, or null when the matrix is singular. */
fun matInverse(a: Matrix): Matrix? {
    val n = a.size
    val m = MutableList(n) { r -> DoubleArray(2 * n) { c -> if (c < n) a[r][c] else if (c - n == r) 1.0 else 0.0 } }
    for (c in 0 until n) {
        val p = (c until n).maxBy { abs(m[it][c]) }
        if (abs(m[p][c]) < 1e-12) return null
        val t = m[p]; m[p] = m[c]; m[c] = t
        val pv = m[c][c]
        for (k in 0 until 2 * n) m[c][k] /= pv
        for (r in 0 until n) if (r != c) {
            val f = m[r][c]
            for (k in 0 until 2 * n) m[r][k] -= f * m[c][k]
        }
    }
    return m.map { row -> (n until 2 * n).map { row[it] + 0.0 } }
}

// ── Linear regression ─────────────────────────────────────────────────────────

data class Regression(val n: Int, val slope: Double, val intercept: Double, val r: Double)

fun parsePairs(s: String): List<Pair<Double, Double>> =
    s.split(Regex("[;\n]+")).mapNotNull { part -> parseNumberList(part).takeIf { it.size == 2 }?.let { it[0] to it[1] } }

fun linearRegression(pts: List<Pair<Double, Double>>): Regression? {
    if (pts.size < 2) return null
    val n = pts.size
    val mx = pts.sumOf { it.first } / n
    val my = pts.sumOf { it.second } / n
    val sxx = pts.sumOf { (it.first - mx).pow(2) }
    val syy = pts.sumOf { (it.second - my).pow(2) }
    val sxy = pts.sumOf { (it.first - mx) * (it.second - my) }
    if (sxx == 0.0) return null
    val slope = sxy / sxx
    return Regression(n, slope, my - slope * mx, if (syy == 0.0) Double.NaN else sxy / sqrt(sxx * syy))
}

// ── Finance ───────────────────────────────────────────────────────────────────

fun compoundAmount(principal: Double, ratePct: Double, years: Double, perYear: Double) =
    principal * (1 + ratePct / 100 / perYear).pow(perYear * years)

fun loanPayment(principal: Double, ratePct: Double, months: Int): Double {
    val i = ratePct / 100 / 12
    return if (i == 0.0) principal / months else principal * i / (1 - (1 + i).pow(-months))
}

// ── Dates (proleptic Gregorian, days since 1970-01-01) ───────────────────────

fun daysFromCivil(y0: Int, m: Int, d: Int): Long {
    val y = (if (m <= 2) y0 - 1 else y0).toLong()
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

fun civilFromDays(z0: Long): Triple<Int, Int, Int> {
    val z = z0 + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val y = (yoe + era * 400).toInt()
    return Triple(if (m <= 2) y + 1 else y, m, d)
}

/** 0 = Monday … 6 = Sunday. */
fun dayOfWeek(day: Long) = Math.floorMod(day + 3, 7L).toInt()

fun daysInMonth(y: Int, m: Int) =
    (daysFromCivil(if (m == 12) y + 1 else y, if (m == 12) 1 else m + 1, 1) - daysFromCivil(y, m, 1)).toInt()

/** Calendar difference (years, months, days) between two days, from <= to. */
fun calendarDiff(from: Long, to: Long): Triple<Int, Int, Int> {
    val (y1, m1, d1) = civilFromDays(from)
    val (y2, m2, d2) = civilFromDays(to)
    var y = y2 - y1; var m = m2 - m1; var d = d2 - d1
    if (d < 0) {
        m -= 1
        d += daysInMonth(if (m2 == 1) y2 - 1 else y2, if (m2 == 1) 12 else m2 - 1)
    }
    if (m < 0) { y -= 1; m += 12 }
    return Triple(y, m, d)
}

/** Monday–Friday days in [from, to). */
fun businessDays(from: Long, to: Long): Long {
    val total = to - from
    var count = total / 7 * 5
    for (k in 0 until total % 7) if (dayOfWeek(from + total / 7 * 7 + k) < 5) count++
    return count
}

// ── Number theory ─────────────────────────────────────────────────────────────

fun primeFactors(n0: Long): List<Pair<Long, Int>> {
    val out = mutableListOf<Pair<Long, Int>>()
    var n = n0
    var p = 2L
    while (p * p <= n) {
        var e = 0
        while (n % p == 0L) { n /= p; e++ }
        if (e > 0) out += p to e
        p += if (p == 2L) 1 else 2
    }
    if (n > 1) out += n to 1
    return out
}

fun isPrime(n: Long): Boolean {
    if (n < 2) return false
    if (n % 2 == 0L) return n == 2L
    var p = 3L
    while (p * p <= n) { if (n % p == 0L) return false; p += 2 }
    return true
}

fun divisors(factors: List<Pair<Long, Int>>): List<Long> {
    var ds = listOf(1L)
    for ((p, e) in factors) {
        ds = ds.flatMap { d -> var pk = 1L; (0..e).map { val v = d * pk; pk *= p; v } }
    }
    return ds.sorted()
}

fun gcdL(a: Long, b: Long): Long { var x = abs(a); var y = abs(b); while (y != 0L) { val t = y; y = x % y; x = t }; return x }
