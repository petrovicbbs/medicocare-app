package com.medicocare.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import com.medicocare.app.ui.theme.LocalSkinEffects
import com.medicocare.app.ui.theme.LocalSkinShapes

/**
 * Zamena za OutlinedTextField(...) sa providnom pozadinom i zaobljenjem uglova
 * prema aktivnom skinu. Potpis prati najčešće korišćeni OutlinedTextField
 * (String vrednost) tako da postojeći pozivi mogu direktno da pređu na ovu
 * komponentu bez menjanja parametara.
 */
@Composable
fun ThemedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape? = null,
    colors: TextFieldColors? = null
) {
    val skinShapes = LocalSkinShapes.current
    val effects = LocalSkinEffects.current
    val resolvedShape = shape ?: RoundedCornerShape(skinShapes.inputRadius)
    val resolvedColors = colors ?: OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = effects.inputAlpha),
        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = (effects.inputAlpha + 0.12f).coerceAtMost(1f))
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = resolvedShape,
        colors = resolvedColors
    )
}
