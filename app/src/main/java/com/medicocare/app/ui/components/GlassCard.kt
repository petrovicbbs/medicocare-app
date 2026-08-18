package com.medicocare.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.medicocare.app.ui.theme.LocalSkinEffects
import com.medicocare.app.ui.theme.LocalSkinShapes

/**
 * Zamena za Card(...) koja po difoltu koristi providnu ("staklenu") pozadinu i
 * zaobljenje uglova prema aktivnom skinu (preko LocalSkinShapes/LocalSkinEffects),
 * umesto potpuno neprozirne Material3 podrazumevane kartice. Ako pozivalac
 * eksplicitno prosledi `colors`, ta vrednost se koristi bez izmene — npr. kartice
 * koje namerno koriste akcentovanu pozadinu poput sekundarne teme boje.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    colors: CardColors? = null,
    shape: Shape? = null,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val skinShapes = LocalSkinShapes.current
    val effects = LocalSkinEffects.current
    val resolvedShape = shape ?: RoundedCornerShape(skinShapes.cardRadius)
    val resolvedColors = colors ?: CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = effects.cardAlpha)
    )
    Card(
        modifier = modifier,
        shape = resolvedShape,
        colors = resolvedColors,
        elevation = CardDefaults.cardElevation(defaultElevation = effects.shadowElevation),
        border = border,
        content = content
    )
}

/** Klikabilna varijanta GlassCard-a — zamena za Card(onClick = ...) { ... }. */
@Composable
fun GlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CardColors? = null,
    shape: Shape? = null,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val skinShapes = LocalSkinShapes.current
    val effects = LocalSkinEffects.current
    val resolvedShape = shape ?: RoundedCornerShape(skinShapes.cardRadius)
    val resolvedColors = colors ?: CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = effects.cardAlpha)
    )
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = resolvedShape,
        colors = resolvedColors,
        elevation = CardDefaults.cardElevation(defaultElevation = effects.shadowElevation),
        border = border,
        content = content
    )
}
