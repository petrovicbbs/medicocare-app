package com.medicocare.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily

@Composable
fun MedicoCareTheme(
    skin: AppSkin = AppSkin.PODRAZUMEVANA,
    darkTheme: Boolean,
    customConfig: CustomSkinConfig? = null,
    fontFamily: AppFontFamily = AppFontFamily.AUTOMATSKI,
    textSize: TextSizeOption = TextSizeOption.NORMALNA,
    transparencyMode: TransparencyMode = TransparencyMode.STANDARDNA,
    animationsMode: AnimationsMode = AnimationsMode.UKLJUCENE,
    content: @Composable () -> Unit
) {
    val skinStyle = SkinStyles.styleFor(skin)
    val effects = SkinStyles.applyTransparencyMode(skinStyle.effects, transparencyMode)

    val colors = if (skin == AppSkin.CUSTOM && customConfig != null) {
        val accent = customConfig.accentColor
        val background = customConfig.backgroundColor
        val onBackground = if (background.luminance() > 0.5f) Color.Black else Color.White
        if (darkTheme) {
            darkColorScheme(
                primary = accent,
                secondary = accent,
                tertiary = accent,
                error = ErrorRed
            )
        } else {
            lightColorScheme(
                primary = accent,
                onPrimary = if (accent.luminance() > 0.5f) Color.Black else Color.White,
                secondary = accent,
                tertiary = accent,
                background = background,
                onBackground = onBackground,
                surface = background,
                onSurface = onBackground,
                error = ErrorRed
            )
        }
    } else if (darkTheme) {
        darkColorScheme(
            primary = skin.secondary,
            secondary = skin.primary,
            tertiary = skin.tertiary,
            error = ErrorRed
        )
    } else {
        lightColorScheme(
            primary = skin.primary,
            onPrimary = Color.White,
            secondary = skin.secondary,
            tertiary = skin.tertiary,
            background = BackgroundLight,
            surface = SurfaceLight,
            error = ErrorRed
        )
    }

    val typography = if (skin == AppSkin.CUSTOM && customConfig != null) {
        // Prilagođeni skin i dalje potpuno kontroliše svoj font preko sopstvenog
        // CustomFontStyle/fontScale izbora u Podešavanjima (nepromenjeno ponašanje).
        scaledTypography(MaterialTheme.typography, customConfig.fontScale, customConfig.fontStyle.toFontFamily())
    } else {
        val resolvedFont = fontFamily.resolve(fallback = skinStyle.suggestedFont)
        scaledTypography(MaterialTheme.typography, textSize.scale, resolvedFont)
    }

    CompositionLocalProvider(
        LocalSkinShapes provides skinStyle.shapes,
        LocalSkinEffects provides effects,
        LocalAnimationsEnabled provides (animationsMode == AnimationsMode.UKLJUCENE)
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            content = content
        )
    }
}

/** Skalira veličinu fonta i primenjuje izabranu fontsku porodicu na sve stilove tipografije. */
private fun scaledTypography(base: Typography, scale: Float, fontFamily: FontFamily): Typography = Typography(
    displayLarge = base.displayLarge.copy(fontSize = base.displayLarge.fontSize * scale, fontFamily = fontFamily),
    displayMedium = base.displayMedium.copy(fontSize = base.displayMedium.fontSize * scale, fontFamily = fontFamily),
    displaySmall = base.displaySmall.copy(fontSize = base.displaySmall.fontSize * scale, fontFamily = fontFamily),
    headlineLarge = base.headlineLarge.copy(fontSize = base.headlineLarge.fontSize * scale, fontFamily = fontFamily),
    headlineMedium = base.headlineMedium.copy(fontSize = base.headlineMedium.fontSize * scale, fontFamily = fontFamily),
    headlineSmall = base.headlineSmall.copy(fontSize = base.headlineSmall.fontSize * scale, fontFamily = fontFamily),
    titleLarge = base.titleLarge.copy(fontSize = base.titleLarge.fontSize * scale, fontFamily = fontFamily),
    titleMedium = base.titleMedium.copy(fontSize = base.titleMedium.fontSize * scale, fontFamily = fontFamily),
    titleSmall = base.titleSmall.copy(fontSize = base.titleSmall.fontSize * scale, fontFamily = fontFamily),
    bodyLarge = base.bodyLarge.copy(fontSize = base.bodyLarge.fontSize * scale, fontFamily = fontFamily),
    bodyMedium = base.bodyMedium.copy(fontSize = base.bodyMedium.fontSize * scale, fontFamily = fontFamily),
    bodySmall = base.bodySmall.copy(fontSize = base.bodySmall.fontSize * scale, fontFamily = fontFamily),
    labelLarge = base.labelLarge.copy(fontSize = base.labelLarge.fontSize * scale, fontFamily = fontFamily),
    labelMedium = base.labelMedium.copy(fontSize = base.labelMedium.fontSize * scale, fontFamily = fontFamily),
    labelSmall = base.labelSmall.copy(fontSize = base.labelSmall.fontSize * scale, fontFamily = fontFamily)
)
