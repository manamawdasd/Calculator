package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CalcHistoryItem
import com.example.ui.KalkuUiState
import com.example.ui.components.KeyRole
import com.example.ui.components.KeypadButton
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    uiState: KalkuUiState,
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onOperator: (Char) -> Unit,
    onPercent: () -> Unit,
    onToggleSign: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    onConvertResult: () -> Unit,
    onDismissHistory: () -> Unit,
    onSelectHistoryItem: (CalcHistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
    ) {
        val isWide = maxWidth >= 680.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                CalculatorDisplayPanel(
                    uiState = uiState,
                    onConvertResult = onConvertResult,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                CalculatorKeypad(
                    onDigit = onDigit,
                    onDecimal = onDecimal,
                    onOperator = onOperator,
                    onPercent = onPercent,
                    onToggleSign = onToggleSign,
                    onBackspace = onBackspace,
                    onClear = onClear,
                    onEquals = onEquals,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 440.dp)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalculatorDisplayPanel(
                    uiState = uiState,
                    onConvertResult = onConvertResult,
                    modifier = Modifier
                        .weight(0.37f)
                        .fillMaxWidth()
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )

                CalculatorKeypad(
                    onDigit = onDigit,
                    onDecimal = onDecimal,
                    onOperator = onOperator,
                    onPercent = onPercent,
                    onToggleSign = onToggleSign,
                    onBackspace = onBackspace,
                    onClear = onClear,
                    onEquals = onEquals,
                    modifier = Modifier
                        .weight(0.63f)
                        .fillMaxWidth()
                )
            }
        }
    }

    if (uiState.showHistorySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissHistory,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            HistorySheetContent(
                history = uiState.history,
                onSelectItem = onSelectHistoryItem,
                onClearHistory = onClearHistory
            )
        }
    }
}

@Composable
private fun CalculatorDisplayPanel(
    uiState: KalkuUiState,
    onConvertResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exprScroll = rememberScrollState()
    LaunchedEffect(uiState.formattedExpression) {
        exprScroll.animateScrollTo(exprScroll.maxValue)
    }

    val mainFontSize = when {
        uiState.hasError -> 30.sp
        uiState.formattedExpression.length <= 9 -> 54.sp
        uiState.formattedExpression.length <= 13 -> 42.sp
        uiState.formattedExpression.length <= 18 -> 34.sp
        else -> 28.sp
    }

    Column(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        // Quick action chip to send calculated number directly to currency converter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.canConvertResult && !uiState.hasError,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut() + scaleOut(targetScale = 0.92f)
            ) {
                Surface(
                    onClick = onConvertResult,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("convert_result_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CurrencyExchange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.convert_result_cta),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Secondary previous expression line
        if (uiState.lastEvaluatedExpression.isNotEmpty()) {
            Text(
                text = uiState.lastEvaluatedExpression,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .testTag("calc_previous_expression")
            )
        }

        // Main expression / result display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(exprScroll),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = if (uiState.hasError) {
                    stringResource(R.string.currency_error_calc)
                } else {
                    uiState.formattedExpression
                },
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = mainFontSize,
                lineHeight = mainFontSize * 1.12f,
                color = if (uiState.hasError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                maxLines = 1,
                modifier = Modifier.testTag("calc_main_display")
            )
        }

        // Live preview line while typing an expression
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(top = 4.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (uiState.previewResult.isNotEmpty()) {
                Text(
                    text = "= ${uiState.previewResult}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("calc_preview_display")
                )
            }
        }
    }
}

@Composable
private fun CalculatorKeypad(
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onOperator: (Char) -> Unit,
    onPercent: () -> Unit,
    onToggleSign: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gap = 10.dp
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        // Row 1: C, Backspace, %, ÷
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            KeypadButton(
                label = "C",
                contentDescriptionText = "Hapus semua",
                tag = "key_clear",
                role = KeyRole.UTILITY,
                onClick = onClear,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeypadButton(
                icon = Icons.AutoMirrored.Rounded.Backspace,
                contentDescriptionText = "Hapus satu karakter",
                tag = "key_backspace",
                role = KeyRole.UTILITY,
                onClick = onBackspace,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeypadButton(
                label = "%",
                contentDescriptionText = "Persen",
                tag = "key_percent",
                role = KeyRole.OPERATOR,
                onClick = onPercent,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeypadButton(
                label = "÷",
                contentDescriptionText = "Bagi",
                tag = "key_divide",
                role = KeyRole.OPERATOR,
                fontSize = 28.sp,
                onClick = { onOperator('÷') },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 2: 7, 8, 9, ×
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            listOf("7", "8", "9").forEach { d ->
                KeypadButton(
                    label = d,
                    tag = "key_$d",
                    role = KeyRole.DIGIT,
                    onClick = { onDigit(d) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            KeypadButton(
                label = "×",
                contentDescriptionText = "Kali",
                tag = "key_multiply",
                role = KeyRole.OPERATOR,
                fontSize = 28.sp,
                onClick = { onOperator('×') },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 3: 4, 5, 6, −
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            listOf("4", "5", "6").forEach { d ->
                KeypadButton(
                    label = d,
                    tag = "key_$d",
                    role = KeyRole.DIGIT,
                    onClick = { onDigit(d) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            KeypadButton(
                label = "−",
                contentDescriptionText = "Kurang",
                tag = "key_minus",
                role = KeyRole.OPERATOR,
                fontSize = 28.sp,
                onClick = { onOperator('−') },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 4: 1, 2, 3, +
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            listOf("1", "2", "3").forEach { d ->
                KeypadButton(
                    label = d,
                    tag = "key_$d",
                    role = KeyRole.DIGIT,
                    onClick = { onDigit(d) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            KeypadButton(
                label = "+",
                contentDescriptionText = "Tambah",
                tag = "key_plus",
                role = KeyRole.OPERATOR,
                fontSize = 28.sp,
                onClick = { onOperator('+') },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 5: ±, 0, ., =
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            KeypadButton(
                label = "±",
                contentDescriptionText = "Ubah tanda positif atau negatif",
                tag = "key_toggle_sign",
                role = KeyRole.DIGIT,
                fontSize = 22.sp,
                onClick = onToggleSign,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeypadButton(
                label = "0",
                tag = "key_0",
                role = KeyRole.DIGIT,
                onClick = { onDigit("0") },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeypadButton(
                label = ".",
                contentDescriptionText = "Koma desimal",
                tag = "key_decimal",
                role = KeyRole.DIGIT,
                onClick = onDecimal,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeypadButton(
                label = "=",
                contentDescriptionText = "Hitung hasil",
                tag = "key_equals",
                role = KeyRole.ACCENT,
                fontSize = 28.sp,
                onClick = onEquals,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun HistorySheetContent(
    history: List<CalcHistoryItem>,
    onSelectItem: (CalcHistoryItem) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleLarge
            )
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(stringResource(R.string.history_clear))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectItem(item) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = item.expression,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "= ${item.result}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
