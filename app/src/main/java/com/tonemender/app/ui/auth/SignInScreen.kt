package com.tonemender.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tonemender.app.ui.session.SessionViewModel

@Composable
fun SignInScreen(
    onGoToSignUp: () -> Unit,
    onGoToForgotPassword: (String) -> Unit,
    sessionViewModel: SessionViewModel,
    viewModel: SignInViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val canSubmit = uiState.email.isNotBlank() &&
            uiState.password.isNotBlank() &&
            !uiState.isLoading &&
            !uiState.isResendingVerification

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection()

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EmailField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail
                )

                PasswordField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword
                )

                uiState.errorMessage?.let {
                    ErrorText(it)
                }

                if (uiState.likelyNeedsVerification) {
                    VerificationCard(
                        email = uiState.email,
                        isLoading = uiState.isLoading,
                        isResending = uiState.isResendingVerification,
                        message = uiState.verificationResendMessage,
                        onResend = { viewModel.resendVerificationEmail() }
                    )
                }

                SignInButton(
                    isLoading = uiState.isLoading,
                    enabled = canSubmit,
                    onClick = {
                        if (canSubmit) {
                            viewModel.signIn {
                                sessionViewModel.setSignedIn(true)
                            }
                        }
                    }
                )

                TextButton(
                    onClick = { onGoToForgotPassword(uiState.email) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Forgot password?")
                }

                TextButton(
                    onClick = onGoToSignUp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create account")
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
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
            text = "Sign in to your account",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmailField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation()
    )
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun VerificationCard(
    email: String,
    isLoading: Boolean,
    isResending: Boolean,
    message: String?,
    onResend: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your account may still need email verification.",
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = "If you already created your account, request a new verification email and then try signing in again.",
                style = MaterialTheme.typography.bodySmall
            )

            TextButton(
                onClick = onResend,
                enabled = !isLoading && !isResending && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isResending) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("Resend verification email")
                }
            }

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SignInButton(
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        } else {
            Text("Sign In")
        }
    }
}