package com.tamixa.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tamixa.ui.theme.TamixaCardColors
import com.tamixa.ui.theme.TamixaDesignTokens

/**
 * Preview/Demo screen showcasing all core Tamixa UI components.
 * 
 * This screen demonstrates:
 * - TamixaCard with all color variants
 * - TamixaButton variants (Primary, Secondary, Outline, Ghost)
 * - TamixaTextField with various states (default, error, with icons)
 * 
 * Use this as a reference for implementing these components in your screens.
 */
@Composable
fun TamixaComponentsPreview(
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(TamixaDesignTokens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.sectionSpacing)
    ) {
        // Header
        Text(
            text = "Tamixa Design System",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // Cards Section
        Text(
            text = "Cards",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        TamixaCard(
            colors = TamixaCardColors.surface(),
            onClick = { /* Handle click */ }
        ) {
            Text(
                text = "Surface Card",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Default surface card with onClick support",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        TamixaCard(
            colors = TamixaCardColors.primaryContainer()
        ) {
            Text(
                text = "Primary Container Card",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Primary themed card without onClick",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        TamixaCard(
            colors = TamixaCardColors.secondaryContainer()
        ) {
            Text(
                text = "Secondary Container Card",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Secondary themed card",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        TamixaCard(
            colors = TamixaCardColors.surfaceVariant()
        ) {
            Text(
                text = "Surface Variant Card",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Subtle variant for less emphasis",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        // Buttons Section
        Text(
            text = "Buttons",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        TamixaPrimaryButton(
            text = "Primary Button",
            onClick = { loading = !loading },
            modifier = Modifier.fillMaxWidth(),
            loading = loading
        )
        
        TamixaSecondaryButton(
            text = "Secondary Button",
            onClick = { /* Handle click */ },
            modifier = Modifier.fillMaxWidth()
        )
        
        TamixaOutlineButton(
            text = "Outline Button",
            onClick = { /* Handle click */ },
            modifier = Modifier.fillMaxWidth()
        )
        
        TamixaGhostButton(
            text = "Ghost Button",
            onClick = { /* Handle click */ },
            modifier = Modifier.fillMaxWidth()
        )
        
        TamixaPrimaryButton(
            text = "Disabled Button",
            onClick = { /* Handle click */ },
            modifier = Modifier.fillMaxWidth(),
            enabled = false
        )
        
        // Text Fields Section
        Text(
            text = "Text Fields",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        TamixaTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = it.isNotEmpty() && !it.contains("@")
            },
            label = "Email",
            placeholder = "Enter your email",
            error = if (emailError) "Please enter a valid email address" else null,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        TamixaTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            placeholder = "Enter your password",
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        TamixaTextField(
            value = "",
            onValueChange = { },
            label = "Disabled Field",
            placeholder = "This field is disabled",
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(TamixaDesignTokens.screenPaddingBottomWithoutNav))
    }
}
