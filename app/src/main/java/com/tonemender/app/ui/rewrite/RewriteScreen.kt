package com.tonemender.app.ui.rewrite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tonemender.app.data.local.drafts.Draft

private val toneOptions = listOf("soft", "calm", "clear")
private val recipientOptions = listOf("partner", "friend", "family", "coworker")

@Composable
fun RewriteScreen(
    onGoToDrafts: () -> Unit,
    onGoToAccount: () -> Unit,
    onGoToUpgrade: () -> Unit,
    initialDraft: Draft? = null,
    onDraftConsumed: () -> Unit = {},
    viewModel: RewriteViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = rememberClipboard(context)

    LaunchedEffect(Unit) {
        viewModel.refreshUser()
        viewModel.refreshUsage()
    }

    LaunchedEffect(initialDraft?.id) {
        initialDraft?.let {
            viewModel.loadDraft(it)
            onDraftConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        HeaderSection(uiState.isPro, uiState.editingDraftId != null, onGoToUpgrade)

        MessageInputSection(uiState, viewModel)

        if (uiState.isPro) {
            ToneSection(uiState, viewModel)
            RecipientSection(uiState, viewModel)
        } else {
            ProUpsellCard(onGoToUpgrade)
        }

        RewriteButton(
            isLoading = uiState.isLoading,
            enabled = uiState.canRewrite,
            onClick = viewModel::rewrite
        )

        uiState.errorMessage?.let {
            ErrorText(it)
        }

        if (uiState.hasRewrite) {
            RewriteResultSection(uiState, viewModel, clipboard, context)
        }

        NavigationSection(onGoToDrafts, onGoToAccount, onGoToUpgrade)

        UsageText(uiState)
    }
}

/* ---------- Sections ---------- */

@Composable
private fun HeaderSection(isPro: Boolean, isEditing: Boolean, onUpgrade: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ToneMender Rewrite", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (isEditing) "Editing saved draft"
                else "Rewrite your message with a better tone",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        AssistChip(
            onClick = { if (!isPro) onUpgrade() },
            label = { Text(if (isPro) "Pro" else "Free") }
        )
    }
}

@Composable
private fun MessageInputSection(uiState: RewriteUiState, viewModel: RewriteViewModel) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.message,
                onValueChange = viewModel::updateMessage,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Original message") },
                minLines = 6,
                maxLines = 12
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${uiState.messageCount}/2000", style = MaterialTheme.typography.bodySmall)

                if (uiState.hasEditedOriginalSinceRewrite) {
                    TextButton(onClick = viewModel::revertToOriginalMessage) {
                        Text("Revert to original")
                    }
                }
            }

            OutlinedButton(
                onClick = viewModel::clearOriginalMessage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
private fun ToneSection(uiState: RewriteUiState, viewModel: RewriteViewModel) {
    SelectionCard(
        title = "Tone",
        selected = uiState.selectedTone,
        options = toneOptions,
        onSelect = { value ->
            viewModel.updateTone(if (uiState.selectedTone == value) null else value)
        }
    )
}

@Composable
private fun RecipientSection(uiState: RewriteUiState, viewModel: RewriteViewModel) {
    SelectionCard(
        title = "Recipient",
        selected = uiState.selectedRecipient,
        options = recipientOptions,
        onSelect = { value ->
            viewModel.updateRecipient(if (uiState.selectedRecipient == value) null else value)
        }
    )
}

@Composable
private fun SelectionCard(
    title: String,
    selected: String?,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach {
                    FilterChip(
                        selected = selected == it,
                        onClick = { onSelect(it) },
                        label = { Text(it.replaceFirstChar { c -> c.uppercase() }) }
                    )
                }
            }

            Text(
                text = "Selected: ${selected ?: "None"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ProUpsellCard(onUpgrade: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tone + Recipient are Pro features", style = MaterialTheme.typography.titleMedium)
            Text(
                "Upgrade to ToneMender Pro for better rewrites.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedButton(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) {
                Text("Upgrade to Pro")
            }
        }
    }
}

@Composable
private fun RewriteButton(isLoading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) CircularProgressIndicator(strokeWidth = 2.dp)
        else Text("Rewrite")
    }
}

@Composable
private fun RewriteResultSection(
    uiState: RewriteUiState,
    viewModel: RewriteViewModel,
    clipboard: ClipboardManager,
    context: Context
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            OutlinedTextField(
                value = uiState.rewrittenMessage,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = viewModel::saveDraft, modifier = Modifier.fillMaxWidth()) {
                Text(if (uiState.editingDraftId != null) "Update Draft" else "Save Draft")
            }

            OutlinedButton(onClick = viewModel::useRewriteAsOriginal) {
                Text("Use this")
            }

            OutlinedButton(
                onClick = {
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("Rewrite", uiState.rewrittenMessage)
                    )
                    viewModel.copyRewriteToClipboard()
                }
            ) {
                Text("Copy")
            }

            OutlinedButton(
                onClick = {
                    viewModel.shareRewrite()
                    context.startActivity(
                        Intent.createChooser(
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, uiState.rewrittenMessage)
                                type = "text/plain"
                            },
                            "Share rewrite"
                        )
                    )
                }
            ) {
                Text("Share")
            }
        }
    }
}

@Composable
private fun NavigationSection(
    onGoToDrafts: () -> Unit,
    onGoToAccount: () -> Unit,
    onGoToUpgrade: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onGoToDrafts) { Text("Drafts") }
        TextButton(onClick = onGoToAccount) { Text("Account") }
        TextButton(onClick = onGoToUpgrade) { Text("Upgrade") }
    }
}

@Composable
private fun UsageText(uiState: RewriteUiState) {
    Text(
        text = "Usage today: ${uiState.usageToday} • Total rewrites: ${uiState.usageTotal}",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun rememberClipboard(context: Context): ClipboardManager {
    return remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
}