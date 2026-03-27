package com.tonemender.app.ui.auth

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SignUpScreen(
    onGoToSignIn: () -> Unit,
    viewModel: SignUpViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        SignUpHeader(
            didCreateAccount = uiState.didCreateAccount
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.didCreateAccount) {
                    VerificationSentSection(
                        email = uiState.email.trim(),
                        errorMessage = uiState.errorMessage,
                        verificationResendMessage = uiState.verificationResendMessage,
                        isResendingVerification = uiState.isResendingVerification,
                        onResendVerification = viewModel::resendVerificationEmail,
                        onGoToSignIn = onGoToSignIn
                    )
                } else {
                    SignUpFormSection(
                        email = uiState.email,
                        password = uiState.password,
                        confirmPassword = uiState.confirmPassword,
                        errorMessage = uiState.errorMessage,
                        isLoading = uiState.isLoading,
                        onEmailChange = viewModel::updateEmail,
                        onPasswordChange = viewModel::updatePassword,
                        onConfirmPasswordChange = viewModel::updateConfirmPassword,
                        onSignUp = viewModel::signUp,
                        onGoToSignIn = onGoToSignIn
                    )
                }
            }
        }
    }
}

@Composable
private fun SignUpHeader(
    didCreateAccount: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "ToneMender",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = if (didCreateAccount) {
                "Check your email"
            } else {
                "Create your account"
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun VerificationSentSection(
    email: String,
    errorMessage: String?,
    verificationResendMessage: String?,
    isResendingVerification: Boolean,
    onResendVerification: () -> Unit,
    onGoToSignIn: () -> Unit
) {
    Text(
        text = "We sent a confirmation link to $email. Confirm your email to activate your account.",
        style = MaterialTheme.typography.bodyMedium
    )

    Text(
        text = "If you don’t see it, check your spam or junk folder.",
        style = MaterialTheme.typography.bodySmall
    )

    errorMessage?.let {
        ErrorText(it)
    }

    verificationResendMessage?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall
        )
    }

    Button(
        onClick = onResendVerification,
        enabled = !isResendingVerification,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isResendingVerification) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        } else {
            Text("Resend verification email")
        }
    }

    TextButton(
        onClick = onGoToSignIn,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Go to sign in")
    }
}

@Composable
private fun SignUpFormSection(
    email: String,
    password: String,
    confirmPassword: String,
    errorMessage: String?,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignUp: () -> Unit,
    onGoToSignIn: () -> Unit
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation()
    )

    OutlinedTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Confirm password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation()
    )

    errorMessage?.let {
        ErrorText(it)
    }

    Button(
        onClick = onSignUp,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        } else {
            Text("Create Account")
        }
    }

    TextButton(
        onClick = onGoToSignIn,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Back to sign in")
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error
    )
}