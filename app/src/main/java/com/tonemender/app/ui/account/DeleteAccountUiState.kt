package com.tonemender.app.ui.account

data class DeleteAccountUiState(
    val email: String? = null,
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val showConfirmDialog: Boolean = false
)