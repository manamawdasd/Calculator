package com.example.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

sealed interface EvalResult {
    data class Success(val value: BigDecimal, val formatted: String) : EvalResult
    data object Incomplete : EvalResult
    data object Error : EvalResult
}

object CalculatorEngine {

    private val mathContext = MathContext(15, RoundingMode.HALF_UP)

    private val usSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    fun isOperator(ch: Char): Boolean = ch == '+' || ch == '−' || ch == '×' || ch == '÷'

    fun evaluate(rawExpression: String): EvalResult {
        val cleaned = rawExpression.trim()
        if (cleaned.isEmpty()) return EvalResult.Incomplete

        // Reject trailing binary operator
        val last = cleaned.last()
        if (isOperator(last) || last == '-') {
            // If it ends with a binary operator, it's incomplete (not an error yet)
            return EvalResult.Incomplete
        }

        return try {
            val tokens = tokenize(cleaned)
            if (tokens.isEmpty()) return EvalResult.Incomplete
            val value = parseExpression(tokens)
            val normalized = normalizeBigDecimal(value)
            EvalResult.Success(normalized, formatBigDecimal(normalized))
        } catch (_: ArithmeticException) {
            EvalResult.Error
        } catch (_: IllegalArgumentException) {
            EvalResult.Error
        }
    }

    fun evaluatePreview(rawExpression: String): EvalResult {
        val cleaned = rawExpression.trim()
        if (cleaned.isEmpty()) return EvalResult.Incomplete
        val hasOperatorOrPercent = cleaned.drop(1).any { isOperator(it) || it == '%' }
        if (!hasOperatorOrPercent) return EvalResult.Incomplete
        return evaluate(cleaned)
    }

    fun hasAnyOperator(rawExpression: String): Boolean {
        if (rawExpression.isEmpty()) return false
        return rawExpression.drop(1).any { isOperator(it) || it == '%' }
    }

    fun appendDigit(current: String, digit: String, justEvaluated: Boolean): String {
        if (justEvaluated) {
            return if (digit == "00") "0" else digit
        }
        if (current == "0") {
            return if (digit == "0" || digit == "00") "0" else digit
        }
        val lastNumber = extractTrailingNumberToken(current)
        if (lastNumber == "0" && digit != ".") {
            return current.dropLast(1) + if (digit == "00") "0" else digit
        }
        if (lastNumber == "-0" && digit != ".") {
            return current.dropLast(1) + if (digit == "00") "0" else digit
        }
        if (lastNumber.isEmpty() && digit == "00") {
            return current + "0"
        }
        if (current.endsWith("%")) {
            return "$current×${if (digit == "00") "0" else digit}"
        }
        return current + digit
    }

    fun appendDecimal(current: String, justEvaluated: Boolean): String {
        if (justEvaluated) return "0."
        if (current.isEmpty()) return "0."
        if (current.endsWith("%")) return "$current×0."
        val last = current.last()
        if (isOperator(last) || last == '-') return "${current}0."
        val trailing = extractTrailingNumberToken(current)
        if (trailing.contains('.')) return current
        return "$current."
    }

    fun appendOperator(current: String, op: Char): String {
        if (current.isEmpty()) {
            return if (op == '−') "-" else "0$op"
        }
        if (current == "-") return current
        val last = current.last()
        return when {
            isOperator(last) -> current.dropLast(1) + op
            last == '.' -> "${current.dropLast(1)}$op"
            else -> current + op
        }
    }

    fun appendPercent(current: String): String {
        if (current.isEmpty() || current == "-") return current
        val last = current.last()
        if (isOperator(last) || last == '%' || last == '.') return current
        return "$current%"
    }

    fun toggleSign(current: String): String {
        if (current.isEmpty() || current == "0") return "-"
        if (current == "-") return "0"

        // Find start index of last numeric operand (optionally ending in %)
        var endIdx = current.length
        if (current.endsWith("%")) endIdx--

        var startIdx = endIdx
        while (startIdx > 0 && (current[startIdx - 1].isDigit() || current[startIdx - 1] == '.')) {
            startIdx--
        }
        if (startIdx == endIdx) {
            // Ends with operator -> append unary '-'
            return "$current-"
        }

        val hasUnaryMinus = startIdx > 0 && current[startIdx - 1] == '-' &&
            (startIdx == 1 || isOperator(current[startIdx - 2]))

        return if (hasUnaryMinus) {
            current.removeRange(startIdx - 1, startIdx)
        } else {
            current.substring(0, startIdx) + "-" + current.substring(startIdx)
        }
    }

    fun backspace(current: String): String {
        if (current.length <= 1) return "0"
        val dropped = current.dropLast(1)
        return if (dropped == "-" || dropped.isEmpty()) "0" else dropped
    }

    fun formatExpressionForDisplay(raw: String): String {
        if (raw.isEmpty()) return "0"
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            if (ch.isDigit() || ch == '.') {
                val start = i
                while (i < raw.length && (raw[i].isDigit() || raw[i] == '.')) {
                    i++
                }
                sb.append(formatNumberTokenPreservingTail(raw.substring(start, i)))
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }

    fun formatBigDecimal(value: BigDecimal): String {
        val stripped = normalizeBigDecimal(value)
        val absVal = stripped.abs()
        if (absVal > BigDecimal.ZERO && (absVal >= BigDecimal("1E12") || absVal < BigDecimal("1E-7"))) {
            val sci = DecimalFormat("0.######E0", usSymbols)
            return sci.format(stripped)
        }

        val scale = stripped.scale().coerceAtLeast(0).coerceAtMost(8)
        val rounded = if (stripped.scale() > 8) {
            stripped.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
        } else {
            stripped
        }
        val actualScale = rounded.scale().coerceAtLeast(0)
        val pattern = if (actualScale == 0) "#,##0" else "#,##0." + "#".repeat(actualScale)
        val df = DecimalFormat(pattern, usSymbols)
        df.roundingMode = RoundingMode.HALF_UP
        val formatted = df.format(rounded)
        return if (formatted == "-0") "0" else formatted
    }

    fun toPlainEditableString(value: BigDecimal): String {
        val normalized = normalizeBigDecimal(value)
        val rounded = if (normalized.scale() > 8) {
            normalized.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
        } else {
            normalized
        }
        val plain = rounded.toPlainString()
        return if (plain == "-0") "0" else plain
    }

    private fun normalizeBigDecimal(value: BigDecimal): BigDecimal {
        if (value.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        return value.stripTrailingZeros()
    }

    private fun extractTrailingNumberToken(expr: String): String {
        var i = expr.length - 1
        while (i >= 0 && (expr[i].isDigit() || expr[i] == '.')) {
            i--
        }
        if (i >= 0 && expr[i] == '-' && (i == 0 || isOperator(expr[i - 1]))) {
            i--
        }
        return expr.substring(i + 1)
    }

    private fun formatNumberTokenPreservingTail(token: String): String {
        val dotIdx = token.indexOf('.')
        val intPart = if (dotIdx >= 0) token.substring(0, dotIdx) else token
        val fracPart = if (dotIdx >= 0) token.substring(dotIdx) else ""
        val formattedInt = if (intPart.isEmpty()) {
            ""
        } else {
            intPart.reversed().chunked(3).joinToString(",").reversed()
        }
        return formattedInt + fracPart
    }

    private sealed interface Token {
        data class Number(val value: BigDecimal, val isPercent: Boolean) : Token
        data class Op(val symbol: Char) : Token
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < expr.length) {
            val ch = expr[i]
            val expectUnaryMinus = ch == '-' && (tokens.isEmpty() || tokens.last() is Token.Op)
            if (ch.isDigit() || ch == '.' || expectUnaryMinus) {
                val start = i
                if (expectUnaryMinus) i++
                var dotCount = 0
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                    if (expr[i] == '.') dotCount++
                    if (dotCount > 1) throw IllegalArgumentException("Invalid decimal")
                    i++
                }
                var numStr = expr.substring(start, i)
                if (numStr == "-" || numStr == "." || numStr == "-.") {
                    throw IllegalArgumentException("Invalid number")
                }
                if (numStr.endsWith(".")) numStr = numStr.dropLast(1)
                if (numStr.startsWith(".")) numStr = "0$numStr"
                if (numStr.startsWith("-.")) numStr = "-0" + numStr.substring(1)

                var isPercent = false
                if (i < expr.length && expr[i] == '%') {
                    isPercent = true
                    i++
                }
                tokens.add(Token.Number(BigDecimal(numStr), isPercent))
            } else if (isOperator(ch)) {
                tokens.add(Token.Op(ch))
                i++
            } else {
                throw IllegalArgumentException("Unexpected character: $ch")
            }
        }
        return tokens
    }

    private fun parseExpression(tokens: List<Token>): BigDecimal {
        var idx = 0

        fun parseFactor(): Token.Number {
            if (idx >= tokens.size) throw IllegalArgumentException("Unexpected end")
            val tok = tokens[idx++] as? Token.Number
                ?: throw IllegalArgumentException("Expected number")
            return tok
        }

        fun resolveStandaloneOrMulPercent(tok: Token.Number): BigDecimal {
            return if (tok.isPercent) {
                tok.value.divide(BigDecimal(100), mathContext)
            } else {
                tok.value
            }
        }

        fun parseTerm(): BigDecimal {
            var left = resolveStandaloneOrMulPercent(parseFactor())
            while (idx < tokens.size) {
                val op = tokens[idx] as? Token.Op ?: break
                if (op.symbol != '×' && op.symbol != '÷') break
                idx++
                val rightTok = parseFactor()
                val right = resolveStandaloneOrMulPercent(rightTok)
                left = when (op.symbol) {
                    '×' -> left.multiply(right, mathContext)
                    '÷' -> {
                        if (right.compareTo(BigDecimal.ZERO) == 0) {
                            throw ArithmeticException("Division by zero")
                        }
                        left.divide(right, mathContext)
                    }
                    else -> left
                }
            }
            return left
        }

        var result = parseTerm()
        while (idx < tokens.size) {
            val op = tokens[idx] as? Token.Op ?: break
            if (op.symbol != '+' && op.symbol != '−') break
            idx++
            // Check if the right term is a single percentage token like `100 + 10%`
            val isDirectPercentTerm = idx < tokens.size &&
                tokens[idx] is Token.Number &&
                (tokens[idx] as Token.Number).isPercent &&
                (idx + 1 >= tokens.size || (tokens[idx + 1] as? Token.Op)?.symbol.let { it == '+' || it == '−' || it == null })

            val rightVal = if (isDirectPercentTerm) {
                val pctTok = tokens[idx++] as Token.Number
                result.multiply(pctTok.value, mathContext).divide(BigDecimal(100), mathContext)
            } else {
                parseTerm()
            }

            result = when (op.symbol) {
                '+' -> result.add(rightVal, mathContext)
                '−' -> result.subtract(rightVal, mathContext)
                else -> result
            }
        }
        return result
    }
}
