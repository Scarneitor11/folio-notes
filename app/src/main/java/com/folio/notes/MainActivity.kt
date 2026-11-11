package com.folio.notes

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.folio.notes.data.FolioDatabase
import com.folio.notes.data.NotesRepository
import com.folio.notes.data.SettingsStore
import com.folio.notes.data.ThemeMode
import com.folio.notes.ui.FolioRoot
import com.folio.notes.ui.theme.FolioTheme
import com.folio.notes.ui.theme.LocalHighRefresh
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var store: SettingsStore
    private lateinit var repository: NotesRepository
    private val sharedText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            extractSharedText(intent)?.let { sharedText.value = it }
        }

        store = SettingsStore(applicationContext)
        repository = NotesRepository(FolioDatabase(applicationContext), lifecycleScope)

        var ready = false
        splash.setKeepOnScreenCondition { !ready }
        lifecycleScope.launch {
            repository.load()
            // Hold the splash only until the first read lands; the system keeps it
            // up through the first content frame, so no extra branding delay.
            runCatching { repository.loaded.first { it } }
            applyRefreshMode(store.current.highRefresh)
            ready = true
        }

        applySystemBars()

        setContent {
            val settings by store.state.collectAsStateWithLifecycle()
            val notes by repository.notes.collectAsStateWithLifecycle()

            FolioTheme(settings = settings) {
                CompositionLocalProvider(
                    LocalHighRefresh provides { wanted ->
                        store.update { it.copy(highRefresh = wanted) }
                        applyRefreshMode(wanted)
                    },
                ) {
                    FolioRoot(
                        notes = notes,
                        settings = settings,
                        repository = repository,
                        versionName = BuildConfig.VERSION_NAME,
                        sharedText = sharedText.value,
                        onSharedTextConsumed = { sharedText.value = null },
                        onSettings = { transform -> store.update(transform) },
                    )
                }
            }

            LaunchedEffect(settings.highRefresh) {
                applyRefreshMode(settings.highRefresh)
            }

            LaunchedEffect(settings.themeMode) {
                applySystemBars()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) applyRefreshMode(store.current.highRefresh)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedText(intent)?.let { sharedText.value = it }
    }

    private fun extractSharedText(intent: Intent?): String? {
        val value = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            else -> null
        }
        return value?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * The app chooses its own light/dark palette, so the system bar icons have to
     * follow the app's theme and not the system's. Transparent scrims keep the
     * content edge-to-edge; only the icon tint changes.
     */
    private fun applySystemBars() {
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val dark = when (store.current.themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK, ThemeMode.AMOLED -> true
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
        )
    }

    /**
     * Android picks a display mode on its own, and it does not always pick the
     * fastest one. Ask for the highest refresh rate the panel advertises when the
     * user wants it, and settle back to ~60 Hz when they do not.
     */
    private fun applyRefreshMode(enabled: Boolean) {
        val display: Display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display ?: return
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
        val modes = runCatching { display.supportedModes }.getOrNull().orEmpty()
        val chosen = if (enabled) {
            modes.maxByOrNull { it.refreshRate }?.modeId
        } else {
            modes.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }?.modeId
        } ?: return
        val attrs = window.attributes
        if (attrs.preferredDisplayModeId == chosen) return
        attrs.preferredDisplayModeId = chosen
        window.attributes = attrs
    }
}
