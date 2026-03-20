package com.tonemender.app.ui.auth

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isResendingVerification: Boolean = false,
    val errorMessage: String? = null,
    val verificationResendMessage: String? = null,
    val didCreateAccount: Boolean = false
)