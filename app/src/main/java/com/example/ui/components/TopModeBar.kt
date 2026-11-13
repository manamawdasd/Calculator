package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.AppMode

@Composable
fun TopModeBar(
    currentMode: AppMode,
    onSelectMode: (AppMode) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    isRefreshingRates: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minimalist Segmented Pill Switcher
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .weight(1f, fill = false)
                .width(232.dp)
                .height(44.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                val segmentWidth = maxWidth / 2
                val indicatorOffset by animateDpAsState(
                    targetValue = if (currentMode == AppMode.CALCULATOR) 0.dp else segmentWidth,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "mode_pill_offset"
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    ModePillItem(
                        label = stringResource(R.string.tab_calculator),
                        selected = currentMode == AppMode.CALCULATOR,
                        onClick = { onSelectMode(AppMode.CALCULATOR) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("mode_tab_calculator")
                    )
                    ModePillItem(
                        label = stringResource(R.string.tab_currency),
                        selected = currentMode == AppMode.CURRENCY,
                        onClick = { onSelectMode(AppMode.CURRENCY) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("mode_tab_currency")
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (isRefreshingRates && currentMode == AppMode.CURRENCY) 360f else 0f,
                animationSpec = if (isRefreshingRates && currentMode == AppMode.CURRENCY) {
                    infiniteRepeatable(animation = tween(durationMillis = 800, easing = LinearEasing))
                } else {
                    tween(durationMillis = 200)
                },
                label = "refresh_spin"
            )

            IconButton(
                onClick = onActionClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(
                        if (currentMode == AppMode.CALCULATOR) "history_button" else "refresh_rates_button"
                    )
            ) {
                Icon(
                    imageVector = if (currentMode == AppMode.CALCULATOR) {
                        Icons.Rounded.History
                    } else {
                        Icons.Rounded.Refresh
                    },
                    contentDescription = if (currentMode == AppMode.CALCULATOR) {
                        stringResource(R.string.history_title)
                    } else {
                        stringResource(R.string.currency_refresh)
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .then(if (currentMode == AppMode.CURRENCY) Modifier.rotate(rotation) else Modifier)
                )
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = if (isDarkTheme) "Mode terang" else "Mode gelap",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun ModePillItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "pill_text_color"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}
