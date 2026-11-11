package com.folio.notes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.folio.notes.R

val Fraunces = FontFamily(
    Font(R.font.fraunces_400, FontWeight.Normal),
    Font(R.font.fraunces_500, FontWeight.Medium),
    Font(R.font.fraunces_600, FontWeight.SemiBold),
    Font(R.font.fraunces_700, FontWeight.Bold),
)

val FrauncesItalic = FontFamily(
    Font(R.font.fraunces_600i, FontWeight.SemiBold, style = androidx.compose.ui.text.font.FontStyle.Italic),
)

val Jakarta = FontFamily(
    Font(R.font.jakarta_300, FontWeight.Light),
    Font(R.font.jakarta_400, FontWeight.Normal),
    Font(R.font.jakarta_500, FontWeight.Medium),
    Font(R.font.jakarta_600, FontWeight.SemiBold),
    Font(R.font.jakarta_700, FontWeight.Bold),
)

/** Editorial display face for titles, sans for everything functional. */
fun folioTypography(fontScale: Float = 1f, serifTitles: Boolean = true): Typography {
    val display = if (serifTitles) Fraunces else Jakarta
    val s = fontScale.coerceIn(0.85f, 1.35f)
    fun size(value: Float) = (value * s).sp

    return Typography(
        displayLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = size(54f),
            lineHeight = size(58f),
            letterSpacing = (-1.1).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = size(44f),
            lineHeight = size(48f),
            letterSpacing = (-0.9).sp,
        ),
        displaySmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = size(36f),
            lineHeight = size(40f),
            letterSpacing = (-0.7).sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = size(32f),
            lineHeight = size(38f),
            letterSpacing = (-0.6).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = size(27f),
            lineHeight = size(33f),
            letterSpacing = (-0.4).sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = size(23f),
            lineHeight = size(29f),
            letterSpacing = (-0.2).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = size(21f),
            lineHeight = size(27f),
            letterSpacing = (-0.2).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = Jakarta,
            fontWeight = FontWeight.SemiBold,
            fontSize = size(16f),
            lineHeight = size(22f),
            letterSpacing = 0.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = Jakarta,
            fontWeight = FontWeight.Medium,
            fontSize = size(14f),
            lineHeight = size(20f),
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = Jakarta,
            fontWeight = FontWeight.Normal,
            fontSize = size(16.5f),
            lineHeight = size(26f),
            letterSpacing = 0.05.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = Jakarta,
            fontWeight = FontWeight.Normal,
            fontSize = size(14.5f),
            lineHeight = size(22f),
            letterSpacing = 0.08.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = Jakarta,
            fontWeight = FontWeight.Normal,
            fontSize = size(12.5f),
            lineHeight = size(18f),
            letterSpacing = 0.1.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = Jakarta,
            fontWeight = FontWeight.SemiBold,
            fontSize = size(13.5f),
            lineHeight = size(18f),
            letterSpacing = 0.2.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = Jakarta,
            fontWeight = FontWeight.Medium,
            fontSize = size(11.5f),
            lineHeight = size(16f),
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = Jakarta,
            fontWeight = FontWeight.Medium,
            fontSize = size(10.5f),
            lineHeight = size(14f),
            letterSpacing = 0.8.sp,
        ),
    )
}
