package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CalcHistoryItem
import com.example.data.CurrencyCatalog
import com.example.data.CurrencyInfo
import com.example.data.CurrencyRepository
import com.example.data.RateSource
import com.example.domain.CalculatorEngine
import com.example.domain.EvalResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class AppMode {
    CALCULATOR,
    CURRENCY
}

enum class InputSide {
    FROM,
    TO
}

data class KalkuUiState(
    val mode: AppMode = AppMode.CALCULATOR,
    val darkThemeOverride: Boolean? = null,
    // Calculator
    val rawExpression: String = "0",
    val formattedExpression: String = "0",
    val previewResult: String = "",
    val lastEvaluatedExpression: String = "",
    val hasError: Boolean = false,
    val justEvaluated: Boolean = false,
    val canConvertResult: Boolean = false,
    val history: List<CalcHistoryItem> = emptyList(),
    val showHistorySheet: Boolean = false,
    // Currency
    val fromCurrency: CurrencyInfo = CurrencyCatalog.find("USD"),
    val toCurrency: CurrencyInfo = CurrencyCatalog.find("IDR"),
    val activeSide: InputSide = InputSide.FROM,
    val fromRawInput: String = "1",
    val toRawInput: String = "15850",
    val rates: Map<String, Double> = CurrencyCatalog.fallbackUsdRates,
    val rateSource: RateSource = RateSource.FALLBACK,
    val lastUpdatedMillis: Long? = null,
    val isRefreshing: Boolean = false,
    val currencyError: String? = null,
    val pickerTargetSide: InputSide? = null,
    val currencySearchQuery: String = ""
)

class KalkuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CurrencyRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(KalkuUiState())
    val uiState: StateFlow<KalkuUiState> = _uiState.asStateFlow()

    private val usSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    init {
        viewModelScope.launch {
            val savedPair = repository.selectedPairFlow.first()
            val savedTheme = repository.darkThemeOverrideFlow.first()
            val savedHistory = repository.historyFlow.first()
            val snapshot = repository.loadInitialRates()

            val fromInfo = CurrencyCatalog.find(savedPair.first)
            val toInfo = CurrencyCatalog.find(savedPair.second)

            _uiState.update { state ->
                val converted = convertAmount(
                    rawAmount = state.fromRawInput,
                    source = fromInfo,
                    target = toInfo,
                    rates = snapshot.rates
                )
                state.copy(
                    darkThemeOverride = savedTheme,
                    history = savedHistory,
                    fromCurrency = fromInfo,
                    toCurrency = toInfo,
                    rates = snapshot.rates,
                    rateSource = snapshot.source,
                    lastUpdatedMillis = snapshot.updatedAtMillis,
                    toRawInput = converted
                )
            }

            refreshExchangeRates()
        }
    }

    fun setMode(mode: AppMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun toggleDarkTheme(currentlyDark: Boolean) {
        val next = !currentlyDark
        _uiState.update { it.copy(darkThemeOverride = next) }
        viewModelScope.launch {
            repository.saveDarkThemeOverride(next)
        }
    }

    // --- Calculator actions ---

    fun onCalcDigit(digit: String) {
        _uiState.update { state ->
            val base = if (state.hasError) "0" else state.rawExpression
            val nextRaw = CalculatorEngine.appendDigit(base, digit, state.justEvaluated)
            buildUpdatedCalcState(state, nextRaw, justEvaluated = false, lastExpr = if (state.justEvaluated) "" else state.lastEvaluatedExpression)
        }
    }

    fun onCalcDecimal() {
        _uiState.update { state ->
            val base = if (state.hasError) "0" else state.rawExpression
            val nextRaw = CalculatorEngine.appendDecimal(base, state.justEvaluated)
            buildUpdatedCalcState(state, nextRaw, justEvaluated = false, lastExpr = if (state.justEvaluated) "" else state.lastEvaluatedExpression)
        }
    }

    fun onCalcOperator(op: Char) {
        _uiState.update { state ->
            val base = if (state.hasError) "0" else state.rawExpression
            val nextRaw = CalculatorEngine.appendOperator(base, op)
            buildUpdatedCalcState(state, nextRaw, justEvaluated = false, lastExpr = "")
        }
    }

    fun onCalcPercent() {
        _uiState.update { state ->
            val base = if (state.hasError) "0" else state.rawExpression
            val nextRaw = CalculatorEngine.appendPercent(base)
            buildUpdatedCalcState(state, nextRaw, justEvaluated = false, lastExpr = "")
        }
    }

    fun onCalcToggleSign() {
        _uiState.update { state ->
            val base = if (state.hasError) "0" else state.rawExpression
            val nextRaw = CalculatorEngine.toggleSign(base)
            buildUpdatedCalcState(state, nextRaw, justEvaluated = false, lastExpr = if (state.justEvaluated) "" else state.lastEvaluatedExpression)
        }
    }

    fun onCalcBackspace() {
        _uiState.update { state ->
            if (state.hasError || state.justEvaluated) {
                buildUpdatedCalcState(state, "0", justEvaluated = false, lastExpr = "")
            } else {
                val nextRaw = CalculatorEngine.backspace(state.rawExpression)
                buildUpdatedCalcState(state, nextRaw, justEvaluated = false, lastExpr = state.lastEvaluatedExpression)
            }
        }
    }

    fun onCalcClear() {
        _uiState.update { state ->
            buildUpdatedCalcState(state, "0", justEvaluated = false, lastExpr = "")
        }
    }

    fun onCalcEquals() {
        val current = _uiState.value
        if (current.hasError) {
            onCalcClear()
            return
        }
        when (val result = CalculatorEngine.evaluate(current.rawExpression)) {
            is EvalResult.Success -> {
                val plain = CalculatorEngine.toPlainEditableString(result.value)
                val formattedExpr = CalculatorEngine.formatExpressionForDisplay(current.rawExpression)
                val hadOperator = CalculatorEngine.hasAnyOperator(current.rawExpression)
                val newHistory = if (hadOperator) {
                    val item = CalcHistoryItem(
                        id = System.currentTimeMillis(),
                        expression = formattedExpr,
                        result = result.formatted,
                        plainResult = plain
                    )
                    (listOf(item) + current.history).take(30)
                } else {
                    current.history
                }

                _uiState.update { state ->
                    state.copy(
                        rawExpression = plain,
                        formattedExpression = result.formatted,
                        previewResult = "",
                        lastEvaluatedExpression = if (hadOperator) formattedExpr else "",
                        hasError = false,
                        justEvaluated = true,
                        canConvertResult = result.value.compareTo(BigDecimal.ZERO) > 0,
                        history = newHistory
                    )
                }

                if (hadOperator) {
                    viewModelScope.launch {
                        repository.saveHistory(newHistory)
                    }
                }
            }
            EvalResult.Error -> {
                _uiState.update { state ->
                    state.copy(
                        hasError = true,
                        previewResult = "",
                        lastEvaluatedExpression = CalculatorEngine.formatExpressionForDisplay(state.rawExpression),
                        justEvaluated = true,
                        canConvertResult = false
                    )
                }
            }
            EvalResult.Incomplete -> {
                // Keep state as-is when expression ends with operator
            }
        }
    }

    fun sendCalculatorValueToCurrency() {
        val state = _uiState.value
        if (state.hasError) return

        val numericValue = when (val res = CalculatorEngine.evaluate(state.rawExpression)) {
            is EvalResult.Success -> res.value
            else -> return
        }
        if (numericValue.compareTo(BigDecimal.ZERO) <= 0) return

        val plain = CalculatorEngine.toPlainEditableString(numericValue)
        _uiState.update { current ->
            val converted = convertAmount(
                rawAmount = plain,
                source = current.fromCurrency,
                target = current.toCurrency,
                rates = current.rates
            )
            current.copy(
                mode = AppMode.CURRENCY,
                activeSide = InputSide.FROM,
                fromRawInput = plain,
                toRawInput = converted
            )
        }
    }

    fun setShowHistorySheet(show: Boolean) {
        _uiState.update { it.copy(showHistorySheet = show) }
    }

    fun useHistoryItem(item: CalcHistoryItem) {
        _uiState.update { state ->
            buildUpdatedCalcState(
                state = state.copy(showHistorySheet = false),
                nextRaw = item.plainResult,
                justEvaluated = true,
                lastExpr = item.expression
            )
        }
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
        viewModelScope.launch {
            repository.saveHistory(emptyList())
        }
    }

    private fun buildUpdatedCalcState(
        state: KalkuUiState,
        nextRaw: String,
        justEvaluated: Boolean,
        lastExpr: String
    ): KalkuUiState {
        val formattedExpr = CalculatorEngine.formatExpressionForDisplay(nextRaw)
        val preview = when (val p = CalculatorEngine.evaluatePreview(nextRaw)) {
            is EvalResult.Success -> p.formatted
            EvalResult.Error -> "Tidak bisa dibagi 0"
            EvalResult.Incomplete -> ""
        }
        val evalNow = CalculatorEngine.evaluate(nextRaw)
        val canConvert = evalNow is EvalResult.Success && evalNow.value.compareTo(BigDecimal.ZERO) > 0

        return state.copy(
            rawExpression = nextRaw,
            formattedExpression = formattedExpr,
            previewResult = preview,
            lastEvaluatedExpression = lastExpr,
            hasError = false,
            justEvaluated = justEvaluated,
            canConvertResult = canConvert
        )
    }

    // --- Currency actions ---

    fun refreshExchangeRates() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, currencyError = null) }
            val result = repository.fetchLiveRates()
            result.onSuccess { snapshot ->
                _uiState.update { state ->
                    val updatedOpposite = recomputeOpposite(
                        state = state,
                        rates = snapshot.rates
                    )
                    state.copy(
                        rates = snapshot.rates,
                        rateSource = snapshot.source,
                        lastUpdatedMillis = snapshot.updatedAtMillis,
                        isRefreshing = false,
                        currencyError = null,
                        fromRawInput = if (state.activeSide == InputSide.TO) updatedOpposite else state.fromRawInput,
                        toRawInput = if (state.activeSide == InputSide.FROM) updatedOpposite else state.toRawInput
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        isRefreshing = false,
                        currencyError = "Gagal memperbarui kurs"
                    )
                }
            }
        }
    }

    fun selectActiveSide(side: InputSide) {
        _uiState.update { state ->
            if (state.activeSide == side) return@update state
            // Convert formatted target to editable plain raw input
            val currentActiveVal = if (side == InputSide.FROM) state.fromRawInput else state.toRawInput
            val plain = currentActiveVal.replace(",", "")
            state.copy(
                activeSide = side,
                fromRawInput = if (side == InputSide.FROM) plain else state.fromRawInput,
                toRawInput = if (side == InputSide.TO) plain else state.toRawInput
            )
        }
    }

    fun onCurrencyDigit(digit: String) {
        _uiState.update { state ->
            val currentInput = if (state.activeSide == InputSide.FROM) state.fromRawInput else state.toRawInput
            val nextInput = appendCurrencyDigit(currentInput, digit)
            applyCurrencyInputChange(state, nextInput)
        }
    }

    fun onCurrencyDecimal() {
        _uiState.update { state ->
            val currentInput = if (state.activeSide == InputSide.FROM) state.fromRawInput else state.toRawInput
            val nextInput = if (currentInput.contains('.')) currentInput else "$currentInput."
            applyCurrencyInputChange(state, nextInput)
        }
    }

    fun onCurrencyBackspace() {
        _uiState.update { state ->
            val currentInput = if (state.activeSide == InputSide.FROM) state.fromRawInput else state.toRawInput
            val nextInput = if (currentInput.length <= 1) "0" else currentInput.dropLast(1)
            applyCurrencyInputChange(state, nextInput)
        }
    }

    fun onCurrencyClear() {
        _uiState.update { state ->
            applyCurrencyInputChange(state, "0")
        }
    }

    fun swapCurrencies() {
        _uiState.update { state ->
            val newFrom = state.toCurrency
            val newTo = state.fromCurrency
            val activeRaw = if (state.activeSide == InputSide.FROM) state.fromRawInput else state.toRawInput
            val converted = if (state.activeSide == InputSide.FROM) {
                convertAmount(activeRaw, newFrom, newTo, state.rates)
            } else {
                convertAmount(activeRaw, newTo, newFrom, state.rates)
            }

            viewModelScope.launch {
                repository.saveCurrencyPair(newFrom.code, newTo.code)
            }

            if (state.activeSide == InputSide.FROM) {
                state.copy(
                    fromCurrency = newFrom,
                    toCurrency = newTo,
                    toRawInput = converted
                )
            } else {
                state.copy(
                    fromCurrency = newFrom,
                    toCurrency = newTo,
                    fromRawInput = converted
                )
            }
        }
    }

    fun openCurrencyPicker(side: InputSide) {
        _uiState.update { it.copy(pickerTargetSide = side, currencySearchQuery = "") }
    }

    fun closeCurrencyPicker() {
        _uiState.update { it.copy(pickerTargetSide = null, currencySearchQuery = "") }
    }

    fun updateCurrencySearch(query: String) {
        _uiState.update { it.copy(currencySearchQuery = query) }
    }

    fun selectCurrency(currency: CurrencyInfo) {
        _uiState.update { state ->
            val targetSide = state.pickerTargetSide ?: return@update state
            var newFrom = state.fromCurrency
            var newTo = state.toCurrency

            if (targetSide == InputSide.FROM) {
                if (currency.code == state.toCurrency.code) {
                    newTo = state.fromCurrency
                }
                newFrom = currency
            } else {
                if (currency.code == state.fromCurrency.code) {
                    newFrom = state.toCurrency
                }
                newTo = currency
            }

            viewModelScope.launch {
                repository.saveCurrencyPair(newFrom.code, newTo.code)
            }

            val updatedOpposite = if (state.activeSide == InputSide.FROM) {
                convertAmount(state.fromRawInput, newFrom, newTo, state.rates)
            } else {
                convertAmount(state.toRawInput, newTo, newFrom, state.rates)
            }

            state.copy(
                fromCurrency = newFrom,
                toCurrency = newTo,
                pickerTargetSide = null,
                currencySearchQuery = "",
                fromRawInput = if (state.activeSide == InputSide.TO) updatedOpposite else state.fromRawInput,
                toRawInput = if (state.activeSide == InputSide.FROM) updatedOpposite else state.toRawInput
            )
        }
    }

    fun getUnitRateText(): String {
        val state = _uiState.value
        val fromRate = state.rates[state.fromCurrency.code] ?: 1.0
        val toRate = state.rates[state.toCurrency.code] ?: 1.0
        val unitRate = BigDecimal.valueOf(toRate).divide(BigDecimal.valueOf(fromRate), MathContext.DECIMAL64)
        val formatted = formatConvertedValue(unitRate, state.toCurrency)
        return "1 ${state.fromCurrency.code} = $formatted ${state.toCurrency.code}"
    }

    fun formatActiveCurrencyInput(raw: String): String {
        return CalculatorEngine.formatExpressionForDisplay(raw)
    }

    private fun applyCurrencyInputChange(state: KalkuUiState, nextRaw: String): KalkuUiState {
        return if (state.activeSide == InputSide.FROM) {
            val converted = convertAmount(nextRaw, state.fromCurrency, state.toCurrency, state.rates)
            state.copy(fromRawInput = nextRaw, toRawInput = converted)
        } else {
            val converted = convertAmount(nextRaw, state.toCurrency, state.fromCurrency, state.rates)
            state.copy(toRawInput = nextRaw, fromRawInput = converted)
        }
    }

    private fun recomputeOpposite(state: KalkuUiState, rates: Map<String, Double>): String {
        return if (state.activeSide == InputSide.FROM) {
            convertAmount(state.fromRawInput, state.fromCurrency, state.toCurrency, rates)
        } else {
            convertAmount(state.toRawInput, state.toCurrency, state.fromCurrency, rates)
        }
    }

    private fun appendCurrencyDigit(current: String, digit: String): String {
        val clean = current.replace(",", "")
        if (clean.length >= 15) return clean
        if (clean == "0") {
            return if (digit == "0" || digit == "00") "0" else digit
        }
        return clean + digit
    }

    private fun convertAmount(
        rawAmount: String,
        source: CurrencyInfo,
        target: CurrencyInfo,
        rates: Map<String, Double>
    ): String {
        val sanitized = rawAmount.replace(",", "").trim()
        if (sanitized.isEmpty() || sanitized == ".") return "0"
        val amount = sanitized.toBigDecimalOrNull() ?: return "0"
        if (amount.compareTo(BigDecimal.ZERO) == 0) return "0"

        val sourceUsdRate = rates[source.code] ?: 1.0
        val targetUsdRate = rates[target.code] ?: 1.0
        if (sourceUsdRate == 0.0) return "0"

        val converted = amount
            .multiply(BigDecimal.valueOf(targetUsdRate), MathContext.DECIMAL64)
            .divide(BigDecimal.valueOf(sourceUsdRate), MathContext.DECIMAL64)

        return formatConvertedValue(converted, target)
    }

    private fun formatConvertedValue(value: BigDecimal, currency: CurrencyInfo): String {
        val absVal = value.abs()
        if (absVal.compareTo(BigDecimal.ZERO) == 0) return "0"

        return when {
            absVal < BigDecimal("0.01") -> {
                val df = DecimalFormat("#,##0.######", usSymbols)
                df.roundingMode = RoundingMode.HALF_UP
                df.format(value)
            }
            absVal < BigDecimal.ONE -> {
                val df = DecimalFormat("#,##0.00##", usSymbols)
                df.roundingMode = RoundingMode.HALF_UP
                df.format(value)
            }
            currency.decimals == 0 && absVal >= BigDecimal("10") -> {
                val df = DecimalFormat("#,##0", usSymbols)
                df.roundingMode = RoundingMode.HALF_UP
                df.format(value)
            }
            else -> {
                val df = DecimalFormat("#,##0.00", usSymbols)
                df.roundingMode = RoundingMode.HALF_UP
                df.format(value)
            }
        }
    }
}
