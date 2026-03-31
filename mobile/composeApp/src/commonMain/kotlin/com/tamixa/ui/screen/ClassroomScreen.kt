package com.tamixa.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tamixa.network.ClassroomDto
import com.tamixa.ui.components.AppScreenBackground
import com.tamixa.ui.components.TamixaScreenTopBar
import com.tamixa.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomScreen(
    classrooms: List<ClassroomDto>,
    loading: Boolean,
    error: String?,
    joinLoading: Boolean,
    joinError: String?,
    joinSuccess: ClassroomDto?,
    onRetry: () -> Unit,
    onJoin: (code: String) -> Unit,
    onClearJoin: () -> Unit,
    onBack: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }

    // Show success snackbar
    LaunchedEffect(joinSuccess) {
        if (joinSuccess != null) {
            codeInput = ""
            onClearJoin()
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TamixaScreenTopBar(
                title = "My Classrooms",
                onBack = onBack,
                useTransparentBackground = true
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppScreenBackground(showClouds = false)
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(TamixaDesignTokens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(TamixaDesignTokens.cardSpacing),
                contentPadding = PaddingValues(bottom = TamixaDesignTokens.screenPaddingBottomWithNav)
            ) {
                // Join classroom card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusLarge),
                        colors = TamixaCardColors.surface(),
                        elevation = CardDefaults.cardElevation(defaultElevation = TamixaDesignTokens.cardElevation)
                    ) {
                        Column(
                            modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Join a Classroom", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TamixaContentColors.cardPrimary())
                            OutlinedTextField(
                                value = codeInput,
                                onValueChange = { codeInput = it.uppercase().take(6) },
                                label = { Text("Classroom code") },
                                placeholder = { Text("e.g. ABC123") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (codeInput.length == 6) onJoin(codeInput)
                                }),
                                isError = joinError != null
                            )
                            joinError?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            Button(
                                onClick = { onJoin(codeInput) },
                                enabled = codeInput.length == 6 && !joinLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(TamixaDesignTokens.buttonRadius)
                            ) {
                                if (joinLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Join")
                                }
                            }
                        }
                    }
                }

                // Classrooms list
                if (loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TamixaColors.goldAccent)
                        }
                    }
                } else if (error != null) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetry) { Text("Retry", color = TamixaColors.goldAccent) }
                        }
                    }
                } else if (classrooms.isEmpty()) {
                    item {
                        Text(
                            "No classrooms yet. Ask your teacher for a code!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TamixaContentColors.cardSecondary(),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    item {
                        Text("My Classrooms", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TamixaContentColors.cardSecondary())
                    }
                    items(classrooms) { classroom ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(TamixaDesignTokens.cardRadiusMedium),
                            colors = TamixaCardColors.surface(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(TamixaDesignTokens.cardContentPadding), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(classroom.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TamixaContentColors.cardPrimary())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    classroom.subject?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TamixaContentColors.cardSecondary()) }
                                    classroom.gradeLevel?.let { Text("• Grade $it", style = MaterialTheme.typography.bodySmall, color = TamixaContentColors.cardSecondary()) }
                                }
                                Text("Code: ${classroom.code}", style = MaterialTheme.typography.labelSmall, color = TamixaColors.goldAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}
