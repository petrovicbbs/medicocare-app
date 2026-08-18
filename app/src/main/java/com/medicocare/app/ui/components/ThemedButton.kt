package com.medicocare.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.medicocare.app.ui.theme.LocalSkinShapes

/**
 * Zamena za Button(...) koja primenjuje zaobljenje dugmadi prema aktivnom skinu.
 * Namerno OSTAJE potpuno neprozirna (bez "glass" providnosti) — dugmad su
 * primarne akcije i moraju ostati maksimalno čitljiva/uočljiva u aplikaciji za
 * lekove, za razliku od kartica i dijaloga koji smeju biti blago providni.
 */
@Composable
fun ThemedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val skinShapes = LocalSkinShapes.current
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(skinShapes.buttonRadius),
        colors = colors,
        content = content
    )
}
