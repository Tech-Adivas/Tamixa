package com.tamixa.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tamixa.ui.theme.TamixaColors
import com.tamixa.ui.theme.TamixaDesignTokens

/**
 * Tamixa-styled text field with consistent styling and validation states.
 * Supports label, placeholder, error messages, and icons.
 */
@Composable
fun TamixaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            label = if (label != null) {
                { Text(label) }
            } else null,
            placeholder = if (placeholder != null) {
                { Text(placeholder, style = MaterialTheme.typography.bodyMedium) }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = error != null,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            maxLines = maxLines,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(TamixaDesignTokens.inputRadius),
            colors = OutlinedTextFieldDefaults.colors(
                // Focused state
                focusedBorderColor = TamixaColors.deepTeal,
                focusedLabelColor = TamixaColors.deepTeal,
                focusedTextColor = TamixaColors.onInputSurface,
                
                // Unfocused state
                unfocusedBorderColor = TamixaColors.onInputSurface.copy(alpha = 0.38f),
                unfocusedLabelColor = TamixaColors.onInputSurface.copy(alpha = 0.60f),
                unfocusedTextColor = TamixaColors.onInputSurface,
                
                // Error state
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error,
                errorTextColor = TamixaColors.onInputSurface,
                
                // Disabled state
                disabledBorderColor = TamixaColors.onInputSurface.copy(alpha = 0.12f),
                disabledLabelColor = TamixaColors.onInputSurface.copy(alpha = 0.38f),
                disabledTextColor = TamixaColors.onInputSurface.copy(alpha = 0.38f),
                
                // Container
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorContainerColor = Color.White,
                disabledContainerColor = TamixaColors.cream.copy(alpha = 0.12f)
            )
        )
        
        // Error message
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
