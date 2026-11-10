package dev.andikune.keyboardio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickCalculatorTest {
    @Test fun respectsOperatorPrecedence() {
        assertEquals("14", QuickCalculator.evaluate("2 + 3 * 4"))
    }
    @Test fun supportsParenthesesAndUnarySigns() {
        assertEquals("-12", QuickCalculator.evaluate("-(2 + 4) * 2"))
    }
    @Test fun supportsDecimalCommaAndRemainder() {
        assertEquals("2.5", QuickCalculator.evaluate("7,5 % 5"))
        assertEquals("3.75", QuickCalculator.evaluate("1,5 + 2.25"))
    }
    @Test fun rejectsDivisionByZeroAndMalformedExpressions() {
        assertNull(QuickCalculator.evaluate("5 / 0"))
        assertNull(QuickCalculator.evaluate("2 +"))
        assertNull(QuickCalculator.evaluate("(2 + 3"))
        assertNull(QuickCalculator.evaluate("1 / (3 - 3)"))
    }
    @Test fun rejectsCodeAndOversizedInput() {
        assertNull(QuickCalculator.evaluate("1; System.exit(0)"))
        assertNull(QuickCalculator.evaluate("1".repeat(161)))
    }
    @Test fun supportsNestedExpressionsAndWhitespace() {
        assertEquals("20", QuickCalculator.evaluate(" (2 + (3 * 4)) + 6 "))
    }
    @Test fun keepsDecimalArithmeticAccurate() {
        assertEquals("0.3", QuickCalculator.evaluate("0.1 + 0.2"))
        assertEquals("0.3", QuickCalculator.evaluate("0.9 / 3"))
        assertEquals("0", QuickCalculator.evaluate("-0"))
    }
}
