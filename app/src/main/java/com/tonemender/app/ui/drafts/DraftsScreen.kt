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
import androidx.compose.ui.text.style.TextOverflow
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

    DeleteDraftDialog(
        draft = uiState.pendingDeleteDraft,
        onConfirm = viewModel::confirmDeleteDraft,
        onDismiss = viewModel::cancelDeleteDraft
    )

    ClearAllDraftsDialog(
        visible = uiState.showClearAllDialog,
        onConfirm = viewModel::confirmClearAllDrafts,
        onDismiss = viewModel::cancelClearAllDrafts
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        DraftsHeader(
            draftCount = uiState.drafts.size,
            isEmpty = uiState.isEmpty,
            errorMessage = uiState.errorMessage
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isEmpty) {
            EmptyDraftsState(
                modifier = Modifier.weight(1f, fill = true)
            )
        } else {
            DraftsList(
                drafts = uiState.drafts,
                onOpenDraft = { draft ->
                    viewModel.openDraft(draft, onOpenDraft)
                },
                onDeleteDraft = viewModel::requestDeleteDraft,
                modifier = Modifier.weight(1f, fill = true)
            )

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

@Composable
private fun DraftsHeader(
    draftCount: Int,
    isEmpty: Boolean,
    errorMessage: String?
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
            text = if (isEmpty) {
                "Your saved rewrites will appear here"
            } else {
                "$draftCount saved draft${if (draftCount == 1) "" else "s"}"
            },
            style = MaterialTheme.typography.bodyMedium
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EmptyDraftsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
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
}

@Composable
private fun DraftsList(
    drafts: List<Draft>,
    onOpenDraft: (Draft) -> Unit,
    onDeleteDraft: (Draft) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(
            items = drafts,
            key = { it.id }
        ) { draft ->
            DraftCard(
                draft = draft,
                onOpenDraft = { onOpenDraft(draft) },
                onDeleteDraft = { onDeleteDraft(draft) }
            )
        }
    }
}

@Composable
private fun DraftCard(
    draft: Draft,
    onOpenDraft: () -> Unit,
    onDeleteDraft: () -> Unit
) {
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
            Text(
                text = draft.originalMessage,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Rewrite",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = draft.rewrittenMessage,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

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
                onClick = onOpenDraft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Draft")
            }

            TextButton(
                onClick = onDeleteDraft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun DeleteDraftDialog(
    draft: Draft?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (draft == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete draft?")
        },
        text = {
            Text("This will permanently remove this saved draft.")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ClearAllDraftsDialog(
    visible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Clear all drafts?")
        },
        text = {
            Text("This will permanently remove all saved drafts.")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Clear All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDraftDate(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT
    ).format(Date(timestamp))
}