package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JakartaSansFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily

enum class KeyRole {
    DIGIT,
    OPERATOR,
    UTILITY,
    ACCENT
}

@Composable
fun KeypadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    contentDescriptionText: String = label ?: "",
    tag: String = "",
    role: KeyRole = KeyRole.DIGIT,
    fontSize: TextUnit = 24.sp
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "keypad_scale"
    )

    val containerColor: Color
    val contentColor: Color
    when (role) {
        KeyRole.DIGIT -> {
            containerColor = MaterialTheme.colorScheme.surfaceVariant
            contentColor = MaterialTheme.colorScheme.onSurface
        }
        KeyRole.OPERATOR -> {
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            contentColor = MaterialTheme.colorScheme.primary
        }
        KeyRole.UTILITY -> {
            containerColor = MaterialTheme.colorScheme.secondaryContainer
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
        KeyRole.ACCENT -> {
            containerColor = MaterialTheme.colorScheme.primary
            contentColor = MaterialTheme.colorScheme.onPrimary
        }
    }

    val fontFamily = if (role == KeyRole.UTILITY && label != null && label.any { it.isLetter() }) {
        JakartaSansFontFamily
    } else {
        JetBrainsMonoFontFamily
    }

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier
            .minimumInteractiveComponentSize()
            .scale(scale)
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
            .semantics {
                this.role = Role.Button
                if (contentDescriptionText.isNotEmpty()) {
                    this.contentDescription = contentDescriptionText
                }
            },
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = contentColor
                )
            } else if (label != null) {
                Text(
                    text = label,
                    fontFamily = fontFamily,
                    fontWeight = if (role == KeyRole.ACCENT || role == KeyRole.OPERATOR) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Medium
                    },
                    fontSize = fontSize,
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}
