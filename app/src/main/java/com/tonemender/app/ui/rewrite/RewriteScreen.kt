package com.tonemender.app.ui.rewrite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

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
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ToneMender Rewrite",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text = if (uiState.editingDraftId != null) {
                            "Editing saved draft"
                        } else {
                            "Rewrite your message with a better tone"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                AssistChip(
                    onClick = {
                        if (!uiState.isPro) onGoToUpgrade()
                    },
                    label = {
                        Text(if (uiState.isPro) "Pro" else "Free")
                    }
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.message,
                    onValueChange = viewModel::updateMessage,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Original message") },
                    placeholder = { Text("Paste or type the message you want rewritten") },
                    minLines = 6,
                    maxLines = 12
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${uiState.messageCount}/2000",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (uiState.hasEditedOriginalSinceRewrite) {
                        TextButton(
                            onClick = viewModel::revertToOriginalMessage
                        ) {
                            Text("Revert to original")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::clearOriginalMessage,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear")
                    }
                }
            }
        }

        if (uiState.isPro) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Tone",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        toneOptions.forEach { tone ->
                            FilterChip(
                                selected = uiState.selectedTone == tone,
                                onClick = {
                                    viewModel.updateTone(
                                        if (uiState.selectedTone == tone) null else tone
                                    )
                                },
                                label = { Text(tone.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    Text(
                        text = "Selected tone: ${uiState.selectedTone ?: "None"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Recipient",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recipientOptions.forEach { recipient ->
                            FilterChip(
                                selected = uiState.selectedRecipient == recipient,
                                onClick = {
                                    viewModel.updateRecipient(
                                        if (uiState.selectedRecipient == recipient) null else recipient
                                    )
                                },
                                label = { Text(recipient.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    Text(
                        text = "Selected recipient: ${uiState.selectedRecipient ?: "None"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tone + Recipient are Pro features",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Upgrade to ToneMender Pro to choose the tone and recipient for more tailored rewrites.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedButton(
                        onClick = onGoToUpgrade,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Upgrade to Pro")
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.rewrite() },
            enabled = uiState.canRewrite,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            } else {
                Text("Rewrite")
            }
        }

        uiState.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (uiState.hasRewrite) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Rewritten message",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = uiState.rewrittenMessage,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6,
                        maxLines = 12
                    )

                    Button(
                        onClick = { viewModel.saveDraft() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.editingDraftId != null) {
                                "Update Draft"
                            } else {
                                "Save Draft"
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = viewModel::useRewriteAsOriginal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use this")
                    }

                    OutlinedButton(
                        onClick = {
                            val clip = ClipData.newPlainText(
                                "ToneMender Rewrite",
                                uiState.rewrittenMessage
                            )
                            clipboard.setPrimaryClip(clip)
                            viewModel.copyRewriteToClipboard()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy Rewrite")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.shareRewrite()
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, uiState.rewrittenMessage)
                                type = "text/plain"
                            }

                            val chooser = Intent.createChooser(shareIntent, "Share rewrite")
                            context.startActivity(chooser)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share Rewrite")
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Before & After",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Before",
                        style = MaterialTheme.typography.labelLarge
                    )

                    OutlinedTextField(
                        value = uiState.beforeMessage,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )

                    Text(
                        text = "After",
                        style = MaterialTheme.typography.labelLarge
                    )

                    OutlinedTextField(
                        value = uiState.rewrittenMessage,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )

                    OutlinedButton(
                        onClick = {
                            viewModel.shareBeforeAfter()

                            val shareText = buildString {
                                appendLine("Before:")
                                appendLine(uiState.beforeMessage)
                                appendLine()
                                appendLine("After:")
                                appendLine(uiState.rewrittenMessage)
                            }

                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }

                            val chooser = Intent.createChooser(shareIntent, "Share before & after")
                            context.startActivity(chooser)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share Before & After")
                    }
                }
            }

            if (uiState.toneScore != null || !uiState.emotionalImpact.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Analysis",
                            style = MaterialTheme.typography.titleMedium
                        )

                        uiState.toneScore?.let { score ->
                            Text(
                                text = "Tone score: $score",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        uiState.emotionalImpact?.takeIf { it.isNotBlank() }?.let { impact ->
                            Text(
                                text = "Emotional impact: $impact",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onGoToDrafts,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Drafts")
            }

            TextButton(
                onClick = onGoToAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Account")
            }

            TextButton(
                onClick = onGoToUpgrade,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upgrade")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Usage today: ${uiState.usageToday} • Total rewrites: ${uiState.usageTotal}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}