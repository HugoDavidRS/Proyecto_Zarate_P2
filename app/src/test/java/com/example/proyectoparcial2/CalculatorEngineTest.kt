package com.example.proyectoparcial2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CalculatorEngineTest {
    private fun ev(s: String, x: Double? = null) = CalculatorEngine.evaluate(s, AngleMode.DEG, x)

    @Test fun evaluatesExpressions() {
        assertEquals(7.0, ev("1+2×3"), 1e-12)
        assertEquals(1.0, ev("sin(90)"), 1e-12)
        assertEquals(10.0, ev("ncr(5,2)"), 1e-12)
        assertEquals(6.0, ev("2x", 3.0), 1e-12)
        assertEquals(3.0, ev("√(9)"), 1e-12)
        assertEquals(Math.PI * Math.E, ev("πe"), 1e-12)
        assertEquals(Math.E * 0.5, ev("esin(30)"), 1e-12)
        assertEquals(2.0, ev("7 mod 5"), 1e-12)
        assertEquals(-10.0, ev("2(-5)"), 1e-12)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTrailingGarbage() { ev("2)3") }

    @Test fun formatsWithDotInAnyLocale() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-ES"))
            assertEquals("0.5", CalculatorEngine.formatResult(0.5))
        } finally { Locale.setDefault(old) }
    }

    @Test fun fractions() {
        assertEquals("1/3", CalculatorEngine.toFraction(1.0 / 3))
        assertEquals("-3/4", CalculatorEngine.toFraction(-0.75))
        assertEquals("3/10", CalculatorEngine.toFraction(0.1 + 0.2))
        assertNull(CalculatorEngine.toFraction(Math.PI))
        assertNull(CalculatorEngine.toFraction(2.0))
    }

    @Test fun toolsMath() {
        val s = computeStats(listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0))!!
        assertEquals(5.0, s.mean, 1e-12)
        assertEquals(2.0, s.sdPop, 1e-12)
        assertEquals(4.5, s.median, 1e-12)
        assertEquals(listOf(4.0), s.modes)

        val roots = findRoots({ Math.sin(it) }, -4.0, 4.0)
        assertEquals(3, roots.size)
        assertEquals(-Math.PI, roots[0], 1e-9)
        assertTrue(findRoots({ Math.tan(it) }, 1.0, 2.0).isEmpty())

        assertEquals(listOf("x₁" to "2", "x₂" to "1"), solveQuadratic(1.0, -3.0, 2.0))
        assertEquals(3.0 to -1.0, solveLinear2(1.0, 1.0, 2.0, 1.0, -1.0, 4.0))

        val temp = UNIT_CATEGORIES.first { it.name == "Temperatura" }.units
        assertEquals(212.0, convertUnit(100.0, temp[0], temp[1]), 1e-9)
    }

    @Test fun complexNumbers() {
        assertEquals("5 + i", evaluateAny("(2+3i)×(1−i)", AngleMode.DEG, emptyMap()).text)
        assertEquals("2i", evaluateAny("√(-4)", AngleMode.DEG, emptyMap()).text)
        assertEquals(-1.0, evaluateAny("i^2", AngleMode.DEG, emptyMap()).value!!, 1e-12)
        assertEquals("3", evaluateAny("1+2", AngleMode.DEG, emptyMap()).text)
        assertEquals(5.0, ComplexEngine.evaluate("abs(3+4i)", AngleMode.DEG).re, 1e-12)
    }

    @Test fun variablesDecimalsAndPiForms() {
        assertEquals(11.0, CalculatorEngine.evaluate("2A+B", AngleMode.DEG, vars = mapOf('A' to 4.0, 'B' to 3.0)), 1e-12)
        assertEquals("0.33", CalculatorEngine.formatResult(1.0 / 3, decimals = 2))
        assertEquals("π/2", CalculatorEngine.toPiForm(Math.PI / 2))
        assertEquals("3π/4", CalculatorEngine.toPiForm(3 * Math.PI / 4))
        assertEquals("-2π", CalculatorEngine.toPiForm(-2 * Math.PI))
        assertNull(CalculatorEngine.toPiForm(1.0))
    }

    @Test fun calculusAndMatrices() {
        assertEquals(2.0, integrate({ Math.sin(it) }, 0.0, Math.PI), 1e-9)
        assertEquals(6.0, derivative({ it * it }, 3.0), 1e-6)
        val a = listOf(listOf(2.0, 1.0), listOf(1.0, 3.0))
        assertEquals(5.0, matDet(a), 1e-12)
        val inv = matInverse(a)!!
        val id = matMul(a, inv)
        assertEquals(1.0, id[0][0], 1e-12); assertEquals(0.0, id[0][1], 1e-12)
        assertNull(matInverse(listOf(listOf(1.0, 2.0), listOf(2.0, 4.0))))
        assertEquals(-306.0, matDet(listOf(listOf(6.0, 1.0, 1.0), listOf(4.0, -2.0, 5.0), listOf(2.0, 8.0, 7.0))), 1e-9)
    }

    @Test fun datesPrimesFinanceRegression() {
        assertEquals(0L, daysFromCivil(1970, 1, 1))
        assertEquals(Triple(2024, 2, 29), civilFromDays(daysFromCivil(2024, 2, 29)))
        assertEquals(3, dayOfWeek(daysFromCivil(1970, 1, 1)))               // Thursday
        assertEquals(Triple(26, 9, 21), calendarDiff(daysFromCivil(2000, 1, 1), daysFromCivil(2026, 10, 22)))
        assertEquals(5L, businessDays(daysFromCivil(2026, 10, 19), daysFromCivil(2026, 10, 26)))
        assertEquals("Libra ♎", zodiac(10, 1)); assertEquals("Capricornio ♑", zodiac(1, 5))

        assertEquals(listOf(2L to 3, 3L to 2, 5L to 1), primeFactors(360))
        assertEquals(24, divisors(primeFactors(360)).size)
        assertTrue(isPrime(1_000_000_007)); assertTrue(!isPrime(1))

        assertEquals(10000 * Math.pow(1 + 0.08 / 12, 60.0), compoundAmount(10000.0, 8.0, 5.0, 12.0), 1e-6)
        assertEquals(4707.35, loanPayment(100000.0, 12.0, 24), 0.01)

        val reg = linearRegression(parsePairs("1, 3\n2, 5\n3, 7"))!!
        assertEquals(2.0, reg.slope, 1e-12); assertEquals(1.0, reg.intercept, 1e-12); assertEquals(1.0, reg.r, 1e-12)
    }
}
