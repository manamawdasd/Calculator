package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.TopModeBar
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CurrencyScreen
import com.example.ui.theme.KalkuTheme

@Composable
fun KalkuApp(
    viewModel: KalkuViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val isDark = uiState.darkThemeOverride ?: systemDark

    BackHandler(enabled = uiState.mode == AppMode.CURRENCY) {
        viewModel.setMode(AppMode.CALCULATOR)
    }

    KalkuTheme(darkTheme = isDark) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TopModeBar(
                    currentMode = uiState.mode,
                    onSelectMode = viewModel::setMode,
                    isDarkTheme = isDark,
                    onToggleTheme = { viewModel.toggleDarkTheme(isDark) },
                    isRefreshingRates = uiState.isRefreshing,
                    onActionClick = {
                        if (uiState.mode == AppMode.CALCULATOR) {
                            viewModel.setShowHistorySheet(true)
                        } else {
                            viewModel.refreshExchangeRates()
                        }
                    }
                )

                Crossfade(
                    targetState = uiState.mode,
                    animationSpec = tween(durationMillis = 220),
                    modifier = Modifier.weight(1f),
                    label = "mode_crossfade"
                ) { mode ->
                    when (mode) {
                        AppMode.CALCULATOR -> {
                            CalculatorScreen(
                                uiState = uiState,
                                onDigit = viewModel::onCalcDigit,
                                onDecimal = viewModel::onCalcDecimal,
                                onOperator = viewModel::onCalcOperator,
                                onPercent = viewModel::onCalcPercent,
                                onToggleSign = viewModel::onCalcToggleSign,
                                onBackspace = viewModel::onCalcBackspace,
                                onClear = viewModel::onCalcClear,
                                onEquals = viewModel::onCalcEquals,
                                onConvertResult = viewModel::sendCalculatorValueToCurrency,
                                onDismissHistory = { viewModel.setShowHistorySheet(false) },
                                onSelectHistoryItem = viewModel::useHistoryItem,
                                onClearHistory = viewModel::clearHistory
                            )
                        }
                        AppMode.CURRENCY -> {
                            val formattedFrom = if (uiState.activeSide == InputSide.FROM) {
                                viewModel.formatActiveCurrencyInput(uiState.fromRawInput)
                            } else {
                                uiState.fromRawInput
                            }
                            val formattedTo = if (uiState.activeSide == InputSide.TO) {
                                viewModel.formatActiveCurrencyInput(uiState.toRawInput)
                            } else {
                                uiState.toRawInput
                            }

                            CurrencyScreen(
                                uiState = uiState,
                                unitRateText = viewModel.getUnitRateText(),
                                formattedFromDisplay = formattedFrom,
                                formattedToDisplay = formattedTo,
                                onSelectSide = viewModel::selectActiveSide,
                                onOpenPicker = viewModel::openCurrencyPicker,
                                onSwap = viewModel::swapCurrencies,
                                onDigit = viewModel::onCurrencyDigit,
                                onDecimal = viewModel::onCurrencyDecimal,
                                onBackspace = viewModel::onCurrencyBackspace,
                                onClear = viewModel::onCurrencyClear,
                                onDismissPicker = viewModel::closeCurrencyPicker,
                                onSearchQueryChange = viewModel::updateCurrencySearch,
                                onSelectCurrency = viewModel::selectCurrency
                            )
                        }
                    }
                }
            }
        }
    }
}
