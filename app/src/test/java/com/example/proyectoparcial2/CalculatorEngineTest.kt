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
}
