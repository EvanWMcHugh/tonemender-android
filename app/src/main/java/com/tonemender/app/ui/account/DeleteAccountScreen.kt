package com.tonemender.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.tonemender.app.ui.session.SessionViewModel

@Composable
fun DeleteAccountScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    sessionViewModel: SessionViewModel = viewModel(),
    viewModel: DeleteAccountViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
    }

    DeleteAccountDialog(
        visible = uiState.showConfirmDialog,
        isDeleting = uiState.isDeleting,
        onConfirm = {
            viewModel.confirmDelete {
                sessionViewModel.signOut()
                onDeleted()
            }
        },
        onDismiss = viewModel::cancelDelete
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DeleteAccountHeader()

        if (uiState.isLoading) {
            LoadingSection()
        } else {
            DeleteAccountContent(
                email = uiState.email,
                errorMessage = uiState.errorMessage,
                isDeleting = uiState.isDeleting,
                onRequestDelete = viewModel::requestDelete,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun DeleteAccountHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Delete Account",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "This action is permanent and cannot be undone.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LoadingSection() {
    CircularProgressIndicator()
}

@Composable
private fun DeleteAccountContent(
    email: String?,
    errorMessage: String?,
    isDeleting: Boolean,
    onRequestDelete: () -> Unit,
    onBack: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Signed in as",
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = email ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Deleting your account will remove access to your app account and current session.",
                style = MaterialTheme.typography.bodyMedium
            )

            errorMessage?.let {
                ErrorText(it)
            }

            Button(
                onClick = onRequestDelete,
                enabled = !isDeleting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("Delete Account")
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    visible: Boolean,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete account?")
        },
        text = {
            Text("This permanently deletes your ToneMender account and cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("Delete Account")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error
    )
}