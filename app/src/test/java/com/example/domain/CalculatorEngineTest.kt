package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

    @Test
    fun `operator precedence evaluates multiplication before addition`() {
        val result = CalculatorEngine.evaluate("2+3×4")
        assertTrue(result is EvalResult.Success)
        assertEquals("14", (result as EvalResult.Success).formatted)
    }

    @Test
    fun `smart percentage adds and subtracts percentage of base value`() {
        val added = CalculatorEngine.evaluate("100000+11%")
        assertTrue(added is EvalResult.Success)
        assertEquals("111,000", (added as EvalResult.Success).formatted)

        val discounted = CalculatorEngine.evaluate("200−25%")
        assertTrue(discounted is EvalResult.Success)
        assertEquals("150", (discounted as EvalResult.Success).formatted)
    }

    @Test
    fun `division by zero returns error`() {
        val result = CalculatorEngine.evaluate("42÷0")
        assertTrue(result is EvalResult.Error)
    }

    @Test
    fun `toggleSign flips last operand sign cleanly`() {
        assertEquals("-50", CalculatorEngine.toggleSign("50"))
        assertEquals("50", CalculatorEngine.toggleSign("-50"))
        assertEquals("100+-25", CalculatorEngine.toggleSign("100+25"))
        assertEquals("100+25", CalculatorEngine.toggleSign("100+-25"))
    }

    @Test
    fun `formatExpressionForDisplay preserves decimal tail while grouping thousands`() {
        assertEquals("1,250,000.", CalculatorEngine.formatExpressionForDisplay("1250000."))
        assertEquals("1,250.05×2", CalculatorEngine.formatExpressionForDisplay("1250.05×2"))
    }
}
