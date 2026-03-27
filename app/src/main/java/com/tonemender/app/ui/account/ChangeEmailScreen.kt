package com.tonemender.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChangeEmailScreen(
    onBack: () -> Unit,
    viewModel: ChangeEmailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentEmail()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ChangeEmailHeader()

        if (uiState.isLoading) {
            LoadingSection()
        } else {
            ChangeEmailForm(
                currentEmail = uiState.currentEmail,
                newEmail = uiState.newEmail,
                errorMessage = uiState.errorMessage,
                isSubmitting = uiState.isSubmitting,
                onNewEmailChange = viewModel::updateNewEmail,
                onSubmit = {
                    viewModel.submit(onSuccess = onBack)
                },
                onBack = onBack
            )
        }
    }
}

@Composable
private fun ChangeEmailHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Change Email",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Request an email change for your account.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LoadingSection() {
    CircularProgressIndicator()
}

@Composable
private fun ChangeEmailForm(
    currentEmail: String?,
    newEmail: String,
    errorMessage: String?,
    isSubmitting: Boolean,
    onNewEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
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
                text = "Current email",
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = currentEmail ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                value = newEmail,
                onValueChange = onNewEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            errorMessage?.let {
                ErrorText(it)
            }

            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("Request Email Change")
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
private fun ErrorText(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error
    )
}