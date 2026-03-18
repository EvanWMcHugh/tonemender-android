package com.tonemender.app.ui.drafts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tonemender.app.data.local.drafts.Draft
import java.text.DateFormat
import java.util.Date

@Composable
fun DraftsScreen(
    onBack: () -> Unit,
    onOpenDraft: (Draft) -> Unit,
    viewModel: DraftsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDrafts()
    }

    uiState.pendingDeleteDraft?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteDraft,
            title = {
                Text("Delete draft?")
            },
            text = {
                Text("This will permanently remove this saved draft.")
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteDraft
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::cancelDeleteDraft
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showClearAllDialog) {
        AlertDialog(
            onDismissRequest = viewModel::cancelClearAllDrafts,
            title = {
                Text("Clear all drafts?")
            },
            text = {
                Text("This will permanently remove all saved drafts.")
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmClearAllDrafts
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::cancelClearAllDrafts
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Drafts",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = if (uiState.isEmpty) {
                    "Your saved rewrites will appear here"
                } else {
                    "${uiState.drafts.size} saved draft${if (uiState.drafts.size == 1) "" else "s"}"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            uiState.errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isEmpty) {
            Column(
                modifier = Modifier.weight(1f, fill = true),
                verticalArrangement = Arrangement.Center
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No drafts yet",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Save a rewritten message from the rewrite screen and it will appear here.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = true)
            ) {
                items(uiState.drafts, key = { it.id }) { draft ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatDraftDate(draft.createdAt),
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text = "Original",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(draft.originalMessage)

                            Text(
                                text = "Rewrite",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(draft.rewrittenMessage)

                            draft.tone?.let {
                                Text(
                                    text = "Tone: $it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            draft.recipient?.let {
                                Text(
                                    text = "Recipient: $it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.openDraft(draft, onOpenDraft)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Draft")
                            }

                            TextButton(
                                onClick = { viewModel.requestDeleteDraft(draft) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = viewModel::requestClearAllDrafts,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear All Drafts")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

private fun formatDraftDate(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT
    ).format(Date(timestamp))
}