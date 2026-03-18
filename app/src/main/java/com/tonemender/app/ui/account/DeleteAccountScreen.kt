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

    if (uiState.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = {
                Text("Delete account?")
            },
            text = {
                Text("This permanently deletes your ToneMender account and cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmDelete {
                            sessionViewModel.signOut()
                            onDeleted()
                        }
                    }
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::cancelDelete
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Delete Account",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "This action is permanent and cannot be undone.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
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
                        text = uiState.email ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "Deleting your account will remove access to your app account and current session.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    uiState.errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = viewModel::requestDelete,
                        enabled = !uiState.isDeleting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isDeleting) {
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
    }
}