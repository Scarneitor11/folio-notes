package com.folio.notes.ui.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.folio.notes.ui.theme.LocalFolioMotion
import com.folio.notes.ui.theme.LocalFolioSettings

/**
 * Counts overlays rather than storing one boolean, so a menu that becomes a dialog
 * cannot accidentally clear the blur while the next surface is already opening.
 */
@Stable
class OverlayBlurState {
    var depth by mutableIntStateOf(0)
        private set

    internal fun push() {
        depth++
    }

    internal fun pop() {
        depth = (depth - 1).coerceAtLeast(0)
    }
}

val LocalOverlayBlur = staticCompositionLocalOf { OverlayBlurState() }

@Composable
fun rememberOverlayBlurState(): OverlayBlurState = remember { OverlayBlurState() }

/** Marks the calling menu/dialog as an overlay for as long as it is composed. */
@Composable
fun OverlayBlurEffect(active: Boolean = true) {
    val host = LocalOverlayBlur.current
    DisposableEffect(host, active) {
        if (active) host.push()
        onDispose { if (active) host.pop() }
    }
}

/**
 * Blurs the screen behind an overlay. Android 12 is the first release with a
 * hardware RenderEffect-backed blur; older devices keep the translucent scrim
 * and skip the extra full-screen layer rather than paying for a software blur.
 */
@Composable
fun Modifier.overlayBackdropBlur(active: Boolean, radius: Dp = 14.dp): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    val enabled = LocalFolioSettings.current.blur
    val motion = LocalFolioMotion.current
    // Keep this call unconditional even when blur is disabled. A conditional
    // animateDpAsState changes the composition slot count when the setting flips
    // while a menu is open, which is a real "different number of composables" crash.
    val animated by animateDpAsState(
        targetValue = if (active && enabled) radius else 0.dp,
        animationSpec = tween(
            durationMillis = if (motion.reduced) 1 else motion.millis(220),
            easing = FastOutSlowInEasing,
        ),
        label = "overlayBlur",
    )
    return if (enabled) this.blur(animated) else this
}
