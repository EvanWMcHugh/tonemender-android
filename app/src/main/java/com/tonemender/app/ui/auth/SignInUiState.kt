package com.tonemender.app.ui.auth

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val likelyNeedsVerification: Boolean = false
)