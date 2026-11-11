package com.folio.notes.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.folio.notes.ui.theme.LocalFolioSettings

enum class Haptic { Tap, Toggle, LongPress, Confirm }

fun Modifier.clickableNoRipple(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = clickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    onClick = onClick,
)

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = clickable(
    indication = null,
    interactionSource = null,
    onClick = onClick,
)

/** Settings-aware haptic feedback; silently does nothing when disabled. */
@Composable
fun rememberHaptics(): (Haptic) -> Unit {
    val view: View = LocalView.current
    val enabled = LocalFolioSettings.current.haptics
    val perform: (Haptic) -> Unit = remember(view, enabled) {
        { kind ->
            if (enabled) {
                val constant = when (kind) {
                    Haptic.Tap -> HapticFeedbackConstants.KEYBOARD_TAP
                    Haptic.Toggle -> HapticFeedbackConstants.CLOCK_TICK
                    Haptic.LongPress -> HapticFeedbackConstants.LONG_PRESS
                    Haptic.Confirm -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.VIRTUAL_KEY
                    }
                }
                runCatching { view.performHapticFeedback(constant) }
            }
        }
    }
    return perform
}
