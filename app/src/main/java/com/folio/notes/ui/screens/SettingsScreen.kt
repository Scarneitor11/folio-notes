package com.folio.notes.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folio.notes.R
import com.folio.notes.data.FolioSettings
import com.folio.notes.data.ThemeMode
import com.folio.notes.logic.FolioPalette
import com.folio.notes.logic.FolioSchemes
import com.folio.notes.logic.HomeLayout
import com.folio.notes.logic.Palettes
import com.folio.notes.ui.components.AuroraBackground
import com.folio.notes.ui.components.FolioMark
import com.folio.notes.ui.components.GlassPanel
import com.folio.notes.ui.components.MiniIconButton
import com.folio.notes.ui.components.Reveal
import com.folio.notes.ui.components.SectionLabel
import com.folio.notes.ui.components.clickableNoRipple
import com.folio.notes.ui.components.pan
import com.folio.notes.ui.components.pressScale
import com.folio.notes.ui.components.rememberHaptics
import com.folio.notes.ui.components.Haptic
import com.folio.notes.ui.theme.Fraunces
import com.folio.notes.ui.theme.LocalAccent
import com.folio.notes.ui.theme.LocalFolioDark
import com.folio.notes.ui.theme.LocalFolioMotion
import com.folio.notes.ui.theme.LocalFolioSettings
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

/**
 * Every knob the app exposes: theme, palette, dynamic colour, contrast, corners,
 * type, motion, haptics, blur, refresh rate, home layout, and backup.
 */
@Composable
fun SettingsScreen(
    settings: FolioSettings,
    onUpdate: ((FolioSettings) -> FolioSettings) -> Unit,
    noteCount: Int,
    trashedCount: Int,
    versionName: String,
    onBack: () -> Unit,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    onImportKeep: (Uri) -> Unit,
    onEmptyTrash: () -> Unit,
    onSeedSamples: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalFolioDark.current
    val accent = LocalAccent.current
    val scheme = MaterialTheme.colorScheme
    val motion = LocalFolioMotion.current
    val haptic = rememberHaptics()

    // Mirrors what Home will show: window width minus the feed's side gutters.
    val density = LocalDensity.current
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val liveColumns = HomeLayout.gridColumns(
        availableWidthDp = with(density) { windowWidth.toDp().value } - 40f,
        preference = settings.gridColumns,
    )

    var exportName by remember { mutableStateOf("folio-backup.json") }
    var seeded by remember { mutableStateOf(false) }
    var emptied by remember { mutableStateOf(false) }

    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) onExport(uri) }

    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onImport(uri) }

    val keepImporter = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onImportKeep(uri) }

    Box(modifier.fillMaxSize()) {
        AuroraBackground(Modifier.fillMaxSize(), accent = accent, blur = settings.blur)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 18.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                    container = scheme.surface.copy(alpha = if (settings.blur) 0.55f else 0.9f),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_section),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.6.sp),
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = if (settings.serifTitles) Fraunces else null,
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                    )
                }
                FolioMark(Modifier.size(24.dp), color = accent.primary.copy(alpha = 0.7f))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
                    .padding(top = 10.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionLabel(stringResource(R.string.settings_appearance))
                StaggeredPanel(index = 0) {
                    SettingRow(
                        icon = Icons.Rounded.SettingsBrightness,
                        title = stringResource(R.string.settings_theme),
                        subtitle = when (settings.themeMode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system_sub)
                            ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light_sub)
                            ThemeMode.DARK -> stringResource(R.string.settings_theme_dark_sub)
                            ThemeMode.AMOLED -> stringResource(R.string.settings_theme_black_sub)
                        },
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Segment(
                            options = listOf(
                                stringResource(R.string.settings_theme_system) to ThemeMode.SYSTEM,
                                stringResource(R.string.settings_theme_light) to ThemeMode.LIGHT,
                                stringResource(R.string.settings_theme_dark) to ThemeMode.DARK,
                                stringResource(R.string.settings_theme_black) to ThemeMode.AMOLED,
                            ),
                            selected = settings.themeMode,
                            onSelect = { mode ->
                                haptic(Haptic.Toggle)
                                onUpdate { it.copy(themeMode = mode) }
                            },
                        )
                    }

                    Divider()

                    SwitchRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = stringResource(R.string.settings_dynamic),
                        subtitle = stringResource(R.string.settings_dynamic_sub),
                        checked = settings.dynamicColor,
                        onChange = { on ->
                            haptic(Haptic.Toggle)
                            onUpdate { it.copy(dynamicColor = on) }
                        },
                    )

                    Divider()

                    // One row whether dynamic colour is on or off; the caption is the only
                    // difference and animates its own height, so the panel never jumps.
                    PaletteRow(
                        selected = settings.paletteIndex,
                        dark = dark,
                        onSelect = { index ->
                            haptic(Haptic.Tap)
                            onUpdate { it.copy(paletteIndex = index, dynamicColor = false) }
                        },
                        caption = if (settings.dynamicColor) {
                            stringResource(R.string.settings_palette_caption)
                        } else {
                            null
                        },
                    )

                    Divider()

                    SliderRow(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.settings_contrast),
                        value = (settings.contrast + 0.6f) / 1.2f,
                        valueLabel = when {
                            settings.contrast < -0.01f -> stringResource(R.string.settings_contrast_soft)
                            settings.contrast > 0.01f -> stringResource(R.string.settings_contrast_punchy)
                            else -> stringResource(R.string.settings_contrast_balanced)
                        },
                        endLabels = stringResource(R.string.settings_contrast_soft) to
                            stringResource(R.string.settings_contrast_punchy),
                        onChange = { raw ->
                            val next = kotlin.math.round((raw * 1.2f - 0.6f) * 10f) / 10f
                            onUpdate { it.copy(contrast = next) }
                        },
                    )

                    Divider()

                    SliderRow(
                        icon = Icons.Rounded.GridView,
                        title = stringResource(R.string.settings_corner),
                        value = (settings.cornerScale - 0.4f) / 1.2f,
                        valueLabel = when {
                            settings.cornerScale < 0.7f -> stringResource(R.string.settings_corner_square)
                            settings.cornerScale > 1.3f -> stringResource(R.string.settings_corner_round)
                            else -> stringResource(
                                R.string.settings_percent,
                                (settings.cornerScale * 100).toInt(),
                            )
                        },
                        endLabels = stringResource(R.string.settings_corner_sharp) to
                            stringResource(R.string.settings_corner_round),
                        onChange = { raw -> onUpdate { s -> s.copy(cornerScale = 0.4f + raw * 1.2f) } },
                    )

                    Divider()

                    SliderRow(
                        icon = Icons.Rounded.Smartphone,
                        title = stringResource(R.string.settings_text_size),
                        value = (settings.fontScale - 0.85f) / 0.4f,
                        valueLabel = stringResource(
                            R.string.settings_percent,
                            (settings.fontScale * 100).toInt(),
                        ),
                        endLabels = stringResource(R.string.settings_small) to
                            stringResource(R.string.settings_large),
                        onChange = { raw -> onUpdate { s -> s.copy(fontScale = 0.85f + raw * 0.4f) } },
                    )

                    Divider()

                    SwitchRow(
                        icon = Icons.Rounded.TextFields,
                        title = stringResource(R.string.settings_serif),
                        subtitle = stringResource(R.string.settings_serif_sub),
                        checked = settings.serifTitles,
                        onChange = { on ->
                            haptic(Haptic.Toggle)
                            onUpdate { it.copy(serifTitles = on) }
                        },
                    )
                }

                SectionLabel(stringResource(R.string.settings_motion))
                StaggeredPanel(index = 1) {
                    SliderRow(
                        icon = Icons.Rounded.Animation,
                        title = stringResource(R.string.settings_animation),
                        value = (settings.motionScale - 0.5f) / 1.5f,
                        valueLabel = when {
                            settings.motionScale < 0.9f -> stringResource(R.string.settings_animation_calm)
                            settings.motionScale > 1.1f -> stringResource(R.string.settings_animation_snappy)
                            else -> stringResource(R.string.settings_animation_standard)
                        },
                        endLabels = stringResource(R.string.settings_animation_calm) to
                            stringResource(R.string.settings_animation_snappy),
                        onChange = { raw -> onUpdate { s -> s.copy(motionScale = 0.5f + raw * 1.5f) } },
                    )

                    Divider()

                    SwitchRow(
                        icon = Icons.Rounded.Vibration,
                        title = stringResource(R.string.settings_haptics),
                        subtitle = stringResource(R.string.settings_haptics_sub),
                        checked = settings.haptics,
                        onChange = { on ->
                            haptic(Haptic.Toggle)
                            onUpdate { it.copy(haptics = on) }
                        },
                    )

                    Divider()

                    SwitchRow(
                        icon = Icons.Rounded.BlurOn,
                        title = stringResource(R.string.settings_blur),
                        subtitle = stringResource(R.string.settings_blur_sub),
                        checked = settings.blur,
                        onChange = { on ->
                            haptic(Haptic.Toggle)
                            onUpdate { it.copy(blur = on) }
                        },
                    )

                    Divider()

                    SwitchRow(
                        icon = Icons.Rounded.Speed,
                        title = stringResource(R.string.settings_refresh),
                        subtitle = stringResource(R.string.settings_refresh_sub),
                        checked = settings.highRefresh,
                        onChange = { on ->
                            haptic(Haptic.Toggle)
                            onUpdate { it.copy(highRefresh = on) }
                        },
                    )
                }

                SectionLabel(stringResource(R.string.settings_home))
                StaggeredPanel(index = 2) {
                    SettingRow(
                        icon = Icons.Rounded.GridView,
                        title = stringResource(R.string.settings_layout),
                        subtitle = if (settings.grid) {
                            stringResource(R.string.settings_layout_grid_sub)
                        } else {
                            stringResource(R.string.settings_layout_list_sub)
                        },
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Segment(
                            options = listOf(
                                stringResource(R.string.settings_layout_grid) to true,
                                stringResource(R.string.settings_layout_list) to false,
                            ),
                            selected = settings.grid,
                            onSelect = { grid ->
                                haptic(Haptic.Toggle)
                                onUpdate { it.copy(grid = grid) }
                            },
                        )
                    }

                    // Column density only means something for the grid, so it folds
                    // away with a height change instead of sitting there greyed out.
                    AnimatedVisibility(
                        visible = settings.grid,
                        enter = expandVertically(
                            animationSpec = motion.bounce(0.4f, 900f),
                        ) + fadeIn(tween(motion.millis(180))),
                        exit = shrinkVertically(
                            animationSpec = tween(motion.millis(160)),
                        ) + fadeOut(tween(motion.millis(120))),
                    ) {
                        Column {
                            Divider()

                            SettingRow(
                                icon = Icons.Rounded.ViewColumn,
                                title = stringResource(R.string.settings_columns),
                                subtitle = if (HomeLayout.isAuto(settings.gridColumns)) {
                                    stringResource(R.string.settings_columns_auto_sub, liveColumns)
                                } else {
                                    stringResource(R.string.settings_columns_fixed_sub, settings.gridColumns)
                                },
                            ) {
                                Spacer(Modifier.height(12.dp))
                                Segment(
                                    options = listOf(
                                        stringResource(R.string.settings_columns_auto) to HomeLayout.AUTO,
                                        stringResource(R.string.settings_columns_two) to 2,
                                        stringResource(R.string.settings_columns_three) to 3,
                                        stringResource(R.string.settings_columns_four) to 4,
                                    ),
                                    selected = if (HomeLayout.isAuto(settings.gridColumns)) {
                                        HomeLayout.AUTO
                                    } else {
                                        settings.gridColumns
                                    },
                                    onSelect = { cols ->
                                        haptic(Haptic.Toggle)
                                        onUpdate { it.copy(gridColumns = cols) }
                                    },
                                )
                            }
                        }
                    }
                }

                SectionLabel(stringResource(R.string.settings_library))
                StaggeredPanel(index = 3) {
                    SettingRow(
                        icon = Icons.Rounded.Insights,
                        title = stringResource(R.string.settings_status),
                        subtitle = stringResource(
                            R.string.settings_status_sub,
                            pluralStringResource(R.plurals.note_count, noteCount, noteCount),
                            trashedCount,
                        ),
                    )

                    Divider()

                    ActionRow(
                        icon = Icons.Rounded.FileDownload,
                        title = stringResource(R.string.settings_export),
                        subtitle = stringResource(R.string.settings_export_sub),
                        onClick = {
                            haptic(Haptic.Tap)
                            exportName = "folio-${System.currentTimeMillis() / 1000}.json"
                            exporter.launch(exportName)
                        },
                    )

                    Divider()

                    ActionRow(
                        icon = Icons.Rounded.FileUpload,
                        title = stringResource(R.string.settings_import),
                        subtitle = stringResource(R.string.settings_import_sub),
                        onClick = {
                            haptic(Haptic.Tap)
                            importer.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                    )

                    Divider()

                    ActionRow(
                        icon = Icons.Rounded.CloudDownload,
                        title = stringResource(R.string.settings_keep_import),
                        subtitle = stringResource(R.string.settings_keep_import_sub),
                        onClick = {
                            haptic(Haptic.Tap)
                            keepImporter.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/json",
                                    "text/html",
                                    "text/plain",
                                    "*/*",
                                ),
                            )
                        },
                    )

                    Divider()

                    ActionRow(
                        icon = Icons.Rounded.Lightbulb,
                        title = if (seeded) {
                            stringResource(R.string.settings_samples_added)
                        } else {
                            stringResource(R.string.settings_samples)
                        },
                        subtitle = if (seeded) {
                            stringResource(R.string.settings_samples_added_sub)
                        } else {
                            stringResource(R.string.settings_samples_sub)
                        },
                        enabled = !seeded,
                        onClick = {
                            haptic(Haptic.Confirm)
                            onSeedSamples()
                            seeded = true
                        },
                    )

                    Divider()

                    ActionRow(
                        icon = Icons.Rounded.DeleteSweep,
                        title = stringResource(R.string.settings_empty_trash),
                        subtitle = if (trashedCount == 0) {
                            stringResource(R.string.settings_trash_empty_sub)
                        } else {
                            stringResource(
                                R.string.settings_trash_remove_sub,
                                pluralStringResource(R.plurals.note_count, trashedCount, trashedCount),
                            )
                        },
                        danger = true,
                        enabled = trashedCount > 0 && !emptied,
                        onClick = {
                            haptic(Haptic.Confirm)
                            onEmptyTrash()
                            emptied = true
                        },
                    )
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FolioMark(Modifier.size(16.dp), color = accent.primary.copy(alpha = 0.5f))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_version, versionName),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StaggeredPanel(index: Int, content: @Composable () -> Unit) {
    Reveal(index = index) {
        Panel(content = content)
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    val settings = LocalFolioSettings.current
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape((26 * settings.cornerScale).dp),
        elevation = 6.dp,
        borderAlpha = 0.26f,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            content()
        }
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingIcon(icon)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.72f),
                    )
                }
            }
            if (trailing != null) trailing()
        }
        content?.invoke()
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    val scheme = MaterialTheme.colorScheme
    val accent = LocalAccent.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent.primary,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val accent = LocalAccent.current
    SettingRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = accent.primary,
                    checkedBorderColor = accent.primary,
                ),
            )
        },
    )
}

@Composable
private fun SliderRow(
    icon: ImageVector,
    title: String,
    value: Float,
    valueLabel: String,
    onChange: (Float) -> Unit,
    endLabels: Pair<String, String>? = null,
) {
    val accent = LocalAccent.current
    val scheme = MaterialTheme.colorScheme
    val motion = LocalFolioMotion.current
    val clamped = value.coerceIn(0f, 1f)
    SettingRow(
        icon = icon,
        title = title,
        trailing = {
            AnimatedContent(
                targetState = valueLabel,
                // The number rolls its way up rather than popping at a smaller size:
                // a label that rescales while you drag a slider looks like it is
                // fighting the finger.
                transitionSpec = {
                    (fadeIn(tween(130)) + slideInVertically(motion.pan(200)) { it / 3 }) togetherWith
                        (fadeOut(tween(90)) + slideOutVertically(motion.pan(160)) { -it / 3 })
                },
                label = "sliderLabel",
            ) { shown ->
                Text(
                    text = shown,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent.primary,
                )
            }
        },
    ) {
        Slider(
            value = clamped,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            colors = SliderDefaults.colors(
                thumbColor = accent.primary,
                activeTrackColor = accent.primary,
                inactiveTrackColor = scheme.outlineVariant.copy(alpha = 0.5f),
            ),
        )
        if (endLabels != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = endLabels.first,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
                Text(
                    text = endLabels.second,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun PaletteRow(
    selected: Int,
    dark: Boolean,
    onSelect: (Int) -> Unit,
    caption: String? = null,
) {
    val settings = LocalFolioSettings.current
    val scheme = MaterialTheme.colorScheme
    val motion = LocalFolioMotion.current
    Column(Modifier.padding(top = 2.dp)) {
        AnimatedVisibility(
            visible = caption != null,
            enter = fadeIn(motion.enter(180)) + expandVertically(motion.enter(200)),
            exit = fadeOut(motion.enter(120)) + shrinkVertically(motion.enter(160)),
        ) {
            Column {
                Text(
                    text = caption.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Palettes.all.forEachIndexed { index, palette ->
                PaletteSwatch(
                    palette = palette,
                    selected = index == selected,
                    dark = dark,
                    corner = (14 * settings.cornerScale).dp,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = Palettes.get(selected).name,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun PaletteSwatch(
    palette: FolioPalette,
    selected: Boolean,
    dark: Boolean,
    corner: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(corner)
    val motion = LocalFolioMotion.current
    val interaction = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = motion.bounce(0.3f, 950f),
        label = "swatch",
    )
    Box(
        modifier = modifier
            .height(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.primary,
                        palette.secondary,
                        palette.tertiary,
                    ),
                ),
            )
            .border(
                width = if (selected) 2.2.dp else 1.dp,
                color = if (selected) scheme.onSurface.copy(alpha = 0.85f) else scheme.outlineVariant.copy(alpha = 0.5f),
                shape = shape,
            )
            .pressScale(interaction, 0.93f)
            .clickableNoRipple(interaction, true, onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun <T> Segment(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val accent = LocalAccent.current
    val scheme = MaterialTheme.colorScheme
    val settings = LocalFolioSettings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape((14 * settings.cornerScale).dp))
            .background(scheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (label, value) ->
            val active = value == selected
            val motion = LocalFolioMotion.current
            val interaction = remember { MutableInteractionSource() }
            val fill by animateColorAsState(
                targetValue = if (active) accent.primary.copy(alpha = 0.9f) else Color.Transparent,
                animationSpec = motion.bounce(0.6f, 700f),
                label = "segmentFill",
            )
            val bump by animateFloatAsState(
                targetValue = if (active) 1.04f else 1f,
                animationSpec = motion.bounce(0.35f, 1100f),
                label = "segmentBump",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = bump
                        scaleY = bump
                    }
                    .clip(RoundedCornerShape((11 * settings.cornerScale).dp))
                    .background(fill)
                    .pressScale(interaction, 0.92f)
                    .clickableNoRipple(interaction, true) { onSelect(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) scheme.onPrimary else scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalFolioMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val iconSpin by animateFloatAsState(
        targetValue = if (pressed) -10f else 0f,
        animationSpec = motion.bounce(0.3f, 1300f),
        label = "actionIcon",
    )
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .pressScale(interaction, 0.95f)
            .clickableNoRipple(interaction, enabled, onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    (if (danger) scheme.error else scheme.onSurfaceVariant).copy(alpha = 0.10f * alpha),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = (if (danger) scheme.error else scheme.onSurfaceVariant).copy(alpha = alpha),
                modifier = Modifier
                    .size(19.dp)
                    .graphicsLayer { rotationZ = iconSpin },
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = (if (danger) scheme.error else scheme.onSurface).copy(alpha = alpha),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.66f * alpha),
            )
        }
    }
}
