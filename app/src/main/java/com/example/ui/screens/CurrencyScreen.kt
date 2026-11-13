package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CurrencyCatalog
import com.example.data.CurrencyInfo
import com.example.data.RateSource
import com.example.ui.InputSide
import com.example.ui.KalkuUiState
import com.example.ui.components.KeyRole
import com.example.ui.components.KeypadButton
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(
    uiState: KalkuUiState,
    unitRateText: String,
    formattedFromDisplay: String,
    formattedToDisplay: String,
    onSelectSide: (InputSide) -> Unit,
    onOpenPicker: (InputSide) -> Unit,
    onSwap: () -> Unit,
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onDismissPicker: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCurrency: (CurrencyInfo) -> Unit,
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
                CurrencyConversionPanel(
                    uiState = uiState,
                    unitRateText = unitRateText,
                    formattedFromDisplay = formattedFromDisplay,
                    formattedToDisplay = formattedToDisplay,
                    onSelectSide = onSelectSide,
                    onOpenPicker = onOpenPicker,
                    onSwap = onSwap,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                CurrencyKeypad(
                    onDigit = onDigit,
                    onDecimal = onDecimal,
                    onBackspace = onBackspace,
                    onClear = onClear,
                    onSwap = onSwap,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 440.dp)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CurrencyConversionPanel(
                    uiState = uiState,
                    unitRateText = unitRateText,
                    formattedFromDisplay = formattedFromDisplay,
                    formattedToDisplay = formattedToDisplay,
                    onSelectSide = onSelectSide,
                    onOpenPicker = onOpenPicker,
                    onSwap = onSwap,
                    modifier = Modifier
                        .weight(0.47f)
                        .fillMaxWidth()
                )

                CurrencyKeypad(
                    onDigit = onDigit,
                    onDecimal = onDecimal,
                    onBackspace = onBackspace,
                    onClear = onClear,
                    onSwap = onSwap,
                    modifier = Modifier
                        .weight(0.53f)
                        .fillMaxWidth()
                )
            }
        }
    }

    if (uiState.pickerTargetSide != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val selectedCode = if (uiState.pickerTargetSide == InputSide.FROM) {
            uiState.fromCurrency.code
        } else {
            uiState.toCurrency.code
        }

        ModalBottomSheet(
            onDismissRequest = onDismissPicker,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            CurrencyPickerContent(
                searchQuery = uiState.currencySearchQuery,
                selectedCode = selectedCode,
                onSearchChange = onSearchQueryChange,
                onSelectCurrency = onSelectCurrency
            )
        }
    }
}

@Composable
private fun CurrencyConversionPanel(
    uiState: KalkuUiState,
    unitRateText: String,
    formattedFromDisplay: String,
    formattedToDisplay: String,
    onSelectSide: (InputSide) -> Unit,
    onOpenPicker: (InputSide) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var swapRotationTarget by remember { mutableFloatStateOf(0f) }
    val swapRotation by animateFloatAsState(
        targetValue = swapRotationTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "swap_spin"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Rate indicator badge
        RateStatusBanner(
            unitRateText = unitRateText,
            rateSource = uiState.rateSource,
            lastUpdatedMillis = uiState.lastUpdatedMillis
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Stacked cards with centered floating swap button
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CurrencyAmountCard(
                    label = stringResource(R.string.currency_from),
                    currency = uiState.fromCurrency,
                    amountText = formattedFromDisplay,
                    isActive = uiState.activeSide == InputSide.FROM,
                    onCardClick = { onSelectSide(InputSide.FROM) },
                    onPickerClick = { onOpenPicker(InputSide.FROM) },
                    cardTag = "currency_from_card",
                    pickerTag = "currency_from_picker",
                    amountTag = "currency_from_amount",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                CurrencyAmountCard(
                    label = stringResource(R.string.currency_to),
                    currency = uiState.toCurrency,
                    amountText = formattedToDisplay,
                    isActive = uiState.activeSide == InputSide.TO,
                    onCardClick = { onSelectSide(InputSide.TO) },
                    onPickerClick = { onOpenPicker(InputSide.TO) },
                    cardTag = "currency_to_card",
                    pickerTag = "currency_to_picker",
                    amountTag = "currency_to_amount",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }

            // Floating Swap Button overlapping card boundary
            Surface(
                onClick = {
                    swapRotationTarget += 180f
                    onSwap()
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 4.dp,
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.background),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .testTag("currency_swap_fab")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = stringResource(R.string.currency_swap),
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(swapRotation)
                    )
                }
            }
        }
    }
}

@Composable
private fun RateStatusBanner(
    unitRateText: String,
    rateSource: RateSource,
    lastUpdatedMillis: Long?
) {
    val dotColor = when (rateSource) {
        RateSource.LIVE -> MaterialTheme.colorScheme.tertiary
        RateSource.CACHED -> MaterialTheme.colorScheme.primary
        RateSource.FALLBACK -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusSuffix = when (rateSource) {
        RateSource.LIVE -> {
            val timeStr = lastUpdatedMillis?.let {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
            } ?: ""
            if (timeStr.isNotEmpty()) "Live • $timeStr" else "Live"
        }
        RateSource.CACHED -> stringResource(R.string.currency_offline_cached)
        RateSource.FALLBACK -> stringResource(R.string.currency_fallback_rates)
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = unitRateText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("unit_rate_text")
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusSuffix,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CurrencyAmountCard(
    label: String,
    currency: CurrencyInfo,
    amountText: String,
    isActive: Boolean,
    onCardClick: () -> Unit,
    onPickerClick: () -> Unit,
    cardTag: String,
    pickerTag: String,
    amountTag: String,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        label = "card_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        label = "card_border"
    )

    val amountFontSize = when {
        amountText.length <= 10 -> 32.sp
        amountText.length <= 14 -> 26.sp
        else -> 21.sp
    }

    val scrollState = rememberScrollState()

    Surface(
        onClick = onCardClick,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
        modifier = modifier.testTag(cardTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = currency.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Currency selector pill
                Surface(
                    onClick = onPickerClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag(pickerTag)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = currency.flag,
                            fontSize = 18.sp
                        )
                        Text(
                            text = currency.code,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Ganti mata uang ${currency.code}",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currency.symbol,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp, end = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = amountText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = amountFontSize,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.testTag(amountTag)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyKeypad(
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gap = 10.dp
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        // Left 3 numeric columns
        Column(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            val rows = listOf(
                listOf("7", "8", "9"),
                listOf("4", "5", "6"),
                listOf("1", "2", "3")
            )
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    row.forEach { d ->
                        KeypadButton(
                            label = d,
                            tag = "currency_key_$d",
                            role = KeyRole.DIGIT,
                            onClick = { onDigit(d) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                KeypadButton(
                    label = "00",
                    contentDescriptionText = "Nol ganda",
                    tag = "currency_key_00",
                    role = KeyRole.DIGIT,
                    fontSize = 21.sp,
                    onClick = { onDigit("00") },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                KeypadButton(
                    label = "0",
                    tag = "currency_key_0",
                    role = KeyRole.DIGIT,
                    onClick = { onDigit("0") },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                KeypadButton(
                    label = ".",
                    contentDescriptionText = "Koma desimal",
                    tag = "currency_key_decimal",
                    role = KeyRole.DIGIT,
                    onClick = onDecimal,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        // Right utility column: Backspace, C, Swap
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            KeypadButton(
                icon = Icons.AutoMirrored.Rounded.Backspace,
                contentDescriptionText = "Hapus satu digit",
                tag = "currency_key_backspace",
                role = KeyRole.UTILITY,
                onClick = onBackspace,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            KeypadButton(
                label = "C",
                contentDescriptionText = "Reset angka",
                tag = "currency_key_clear",
                role = KeyRole.UTILITY,
                onClick = onClear,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            KeypadButton(
                icon = Icons.Rounded.SwapVert,
                contentDescriptionText = stringResource(R.string.currency_swap),
                tag = "currency_key_swap",
                role = KeyRole.ACCENT,
                onClick = onSwap,
                modifier = Modifier.weight(2f).fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CurrencyPickerContent(
    searchQuery: String,
    selectedCode: String,
    onSearchChange: (String) -> Unit,
    onSelectCurrency: (CurrencyInfo) -> Unit
) {
    val filtered = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            CurrencyCatalog.allCurrencies
        } else {
            CurrencyCatalog.allCurrencies.filter {
                it.code.lowercase().contains(q) || it.name.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.currency_select_title),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(stringResource(R.string.currency_search_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("currency_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered, key = { it.code }) { item ->
                val isSelected = item.code == selectedCode
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCurrency(item) }
                        .testTag("currency_option_${item.code}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = item.flag, fontSize = 24.sp)
                            Column {
                                Text(
                                    text = item.code,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = item.symbol,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Terpilih",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
