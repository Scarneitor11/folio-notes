package com.folio.notes.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FolioSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val paletteIndex: Int = 0,
    val contrast: Float = 0f,
    val cornerScale: Float = 1f,
    val fontScale: Float = 1f,
    val serifTitles: Boolean = true,
    /** 1 = normal, 2 = twice as fast, 0.5 = half speed. */
    val motionScale: Float = 1f,
    val haptics: Boolean = true,
    val blur: Boolean = true,
    val highRefresh: Boolean = true,
    val grid: Boolean = true,
    /** Home feed columns: 0 = adapt to window width, 2..4 = fixed. */
    val gridColumns: Int = 0,
    val sortMode: SortMode = SortMode.UPDATED_DESC,
)

/** Preferences store: values are tiny, so every update rewrites the whole set. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("folio_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<FolioSettings> = _state.asStateFlow()

    val current: FolioSettings get() = _state.value

    fun update(transform: (FolioSettings) -> FolioSettings) {
        val next = transform(_state.value)
        if (next == _state.value) return
        _state.value = next
        write(next)
    }

    private fun read(): FolioSettings = FolioSettings(
        themeMode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM),
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC, true),
        paletteIndex = prefs.getInt(KEY_PALETTE, 0),
        contrast = prefs.getFloat(KEY_CONTRAST, 0f),
        cornerScale = prefs.getFloat(KEY_CORNER, 1f),
        fontScale = prefs.getFloat(KEY_FONT, 1f),
        serifTitles = prefs.getBoolean(KEY_SERIF, true),
        motionScale = prefs.getFloat(KEY_MOTION, 1f),
        haptics = prefs.getBoolean(KEY_HAPTICS, true),
        blur = prefs.getBoolean(KEY_BLUR, true),
        highRefresh = prefs.getBoolean(KEY_REFRESH, true),
        grid = prefs.getBoolean(KEY_GRID, true),
        gridColumns = prefs.getInt(KEY_GRID_COLUMNS, 0),
        sortMode = runCatching {
            SortMode.valueOf(prefs.getString(KEY_SORT, SortMode.UPDATED_DESC.name)!!)
        }.getOrDefault(SortMode.UPDATED_DESC),
    )

    private fun write(settings: FolioSettings) {
        prefs.edit {
            putString(KEY_THEME, settings.themeMode.name)
            putBoolean(KEY_DYNAMIC, settings.dynamicColor)
            putInt(KEY_PALETTE, settings.paletteIndex)
            putFloat(KEY_CONTRAST, settings.contrast)
            putFloat(KEY_CORNER, settings.cornerScale)
            putFloat(KEY_FONT, settings.fontScale)
            putBoolean(KEY_SERIF, settings.serifTitles)
            putFloat(KEY_MOTION, settings.motionScale)
            putBoolean(KEY_HAPTICS, settings.haptics)
            putBoolean(KEY_BLUR, settings.blur)
            putBoolean(KEY_REFRESH, settings.highRefresh)
            putBoolean(KEY_GRID, settings.grid)
            putInt(KEY_GRID_COLUMNS, settings.gridColumns)
            putString(KEY_SORT, settings.sortMode.name)
        }
    }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_DYNAMIC = "dynamic"
        private const val KEY_PALETTE = "palette"
        private const val KEY_CONTRAST = "contrast"
        private const val KEY_CORNER = "corner"
        private const val KEY_FONT = "font"
        private const val KEY_SERIF = "serif"
        private const val KEY_MOTION = "motion"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_BLUR = "blur"
        private const val KEY_REFRESH = "refresh"
        private const val KEY_GRID = "grid"
        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_SORT = "sort"
    }
}
