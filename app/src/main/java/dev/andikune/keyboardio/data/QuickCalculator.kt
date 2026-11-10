package dev.andikune.keyboardio.data

import java.math.BigDecimal

/**
 * Small, side-effect-free arithmetic parser. Never evaluates user input as code.
 * Supports +, -, *, /, %, parentheses, decimals and unary signs.
 */
object QuickCalculator {
    fun evaluate(expression: String): String? {
        val normalized = expression.trim().replace(',', '.')
        if (normalized.isEmpty() || normalized.length > 160) return null
        return runCatching {
            val parser = Parser(normalized)
            val result = parser.parse()
            if (!result.isFinite()) return null
            BigDecimal.valueOf(result).stripTrailingZeros().toPlainString()
        }.getOrNull()
    }

    private class Parser(private val input: String) {
        private var index = 0

        fun parse(): Double {
            val value = parseExpression()
            skipSpaces()
            require(index == input.length) { "Unexpected token" }
            return value
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipSpaces()
                value = when {
                    consume('+') -> value + parseTerm()
                    consume('-') -> value - parseTerm()
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseUnary()
            while (true) {
                skipSpaces()
                value = when {
                    consume('*') -> value * parseUnary()
                    consume('/') -> {
                        val divisor = parseUnary()
                        require(divisor != 0.0) { "Division by zero" }
                        value / divisor
                    }
                    consume('%') -> {
                        val divisor = parseUnary()
                        require(divisor != 0.0) { "Division by zero" }
                        value % divisor
                    }
                    else -> return value
                }
            }
        }

        private fun parseUnary(): Double {
            skipSpaces()
            return when {
                consume('+') -> parseUnary()
                consume('-') -> -parseUnary()
                else -> parsePrimary()
            }
        }

        private fun parsePrimary(): Double {
            skipSpaces()
            if (consume('(')) {
                val value = parseExpression()
                skipSpaces()
                require(consume(')')) { "Missing closing parenthesis" }
                return value
            }
            return parseNumber()
        }

        private fun parseNumber(): Double {
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
            return input.substring(start, index).toDouble()
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
