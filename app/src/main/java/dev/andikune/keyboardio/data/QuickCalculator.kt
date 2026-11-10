package dev.andikune.keyboardio.data

import java.math.BigDecimal
import java.math.MathContext

/**
 * Side-effect-free arithmetic parser. Never evaluates user input as code.
 * Supports +, -, *, /, %, parentheses, decimals and unary signs.
 */
object QuickCalculator {
    fun evaluate(expression: String): String? {
        val normalized = expression.trim().replace(',', '.')
        if (normalized.isEmpty() || normalized.length > 160) return null
        return runCatching {
            val parser = Parser(normalized)
            parser.parse().stripTrailingZeros().toPlainString()
        }.getOrNull()
    }

    private class Parser(private val input: String) {
        private var index = 0

        fun parse(): BigDecimal {
            val value = parseExpression()
            skipSpaces()
            require(index == input.length) { "Unexpected token" }
            return value
        }

        private fun parseExpression(): BigDecimal {
            var value = parseTerm()
            while (true) {
                skipSpaces()
                value = when {
                    consume('+') -> value.add(parseTerm())
                    consume('-') -> value.subtract(parseTerm())
                    else -> return value
                }
            }
        }

        private fun parseTerm(): BigDecimal {
            var value = parseUnary()
            while (true) {
                skipSpaces()
                value = when {
                    consume('*') -> value.multiply(parseUnary())
                    consume('/') -> {
                        val divisor = parseUnary()
                        require(divisor.compareTo(BigDecimal.ZERO) != 0) { "Division by zero" }
                        value.divide(divisor, MathContext.DECIMAL64)
                    }
                    consume('%') -> {
                        val divisor = parseUnary()
                        require(divisor.compareTo(BigDecimal.ZERO) != 0) { "Division by zero" }
                        value.remainder(divisor)
                    }
                    else -> return value
                }
            }
        }

        private fun parseUnary(): BigDecimal {
            skipSpaces()
            return when {
                consume('+') -> parseUnary()
                consume('-') -> parseUnary().negate()
                else -> parsePrimary()
            }
        }

        private fun parsePrimary(): BigDecimal {
            skipSpaces()
            if (consume('(')) {
                val value = parseExpression()
                skipSpaces()
                require(consume(')')) { "Missing closing parenthesis" }
                return value
            }
            return parseNumber()
        }

        private fun parseNumber(): BigDecimal {
            skipSpaces()
            val start = index
            var hasDigit = false
            while (index < input.length && input[index].isDigit()) {
                index++
                hasDigit = true
            }
            if (index < input.length && input[index] == '.') {
                index++
                while (index < input.length && input[index].isDigit()) {
                    index++
                    hasDigit = true
                }
            }
            require(hasDigit) { "Number expected" }
            return BigDecimal(input.substring(start, index))
        }

        private fun consume(char: Char): Boolean {
            if (index < input.length && input[index] == char) {
                index++
                return true
            }
            return false
        }

        private fun skipSpaces() {
            while (index < input.length && input[index].isWhitespace()) index++
        }
    }
}
