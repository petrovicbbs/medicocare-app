package com.medicocare.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.window.DialogProperties
import com.medicocare.app.ui.theme.LocalSkinEffects
import com.medicocare.app.ui.theme.LocalSkinShapes

/**
 * Zamena za AlertDialog(...) sa zaobljenjem uglova i providnošću prema aktivnom
 * skinu. Potpis prati standardni AlertDialog (onDismissRequest/confirmButton/
 * dismissButton/icon/title/text) tako da postojeći pozivi mogu direktno da pređu
 * na ovu komponentu.
 */
@Composable
fun ThemedDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape? = null,
    containerColor: Color? = null,
    properties: DialogProperties = DialogProperties()
) {
    val skinShapes = LocalSkinShapes.current
    val effects = LocalSkinEffects.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape ?: RoundedCornerShape(skinShapes.dialogRadius),
        containerColor = containerColor ?: MaterialTheme.colorScheme.surface.copy(alpha = effects.dialogAlpha),
        properties = properties
    )
}
