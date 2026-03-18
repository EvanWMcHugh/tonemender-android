package com.tonemender.app.ui.account

data class ChangeEmailUiState(
    val currentEmail: String? = null,
    val newEmail: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)