package com.tonemender.app.ui.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tonemender.app.data.remote.ApiErrorParser
import com.tonemender.app.data.repository.AuthRepository
import com.tonemender.app.data.security.PlayIntegrityManager
import com.tonemender.app.ui.common.UiMessageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChangeEmailViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val playIntegrityManager = PlayIntegrityManager(application.applicationContext)

    private val _uiState = MutableStateFlow(ChangeEmailUiState())
    val uiState: StateFlow<ChangeEmailUiState> = _uiState.asStateFlow()

    init {
        loadCurrentEmail()
    }

    fun updateNewEmail(value: String) {
        _uiState.value = _uiState.value.copy(
            newEmail = value,
            errorMessage = null
        )
    }

    fun loadCurrentEmail() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )

                val response = authRepository.me()
                if (response.isSuccessful) {
                    val email = response.body()?.user?.email
                    _uiState.value = _uiState.value.copy(
                        currentEmail = email,
                        isLoading = false,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Could not load account email."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Network error."
                )
            }
        }
    }

    fun submit(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        val newEmail = state.newEmail.trim()
        val currentEmail = state.currentEmail?.trim().orEmpty()

        when {
            newEmail.isBlank() -> {
                _uiState.value = state.copy(
                    errorMessage = "Enter your new email."
                )
                return
            }

            currentEmail.equals(newEmail, ignoreCase = true) -> {
                _uiState.value = state.copy(
                    errorMessage = "Enter a different email."
                )
                return
            }
        }

        _uiState.value = state.copy(
            isSubmitting = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val requestHash = PlayIntegrityManager.buildRequestHash(
                    "change_email",
                    currentEmail,
                    newEmail
                )

                val prepareResult = playIntegrityManager.prepare()
                if (prepareResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = prepareResult.exceptionOrNull()?.message
                            ?: "Could not prepare integrity check."
                    )
                    return@launch
                }

                val tokenResult = playIntegrityManager.requestToken(requestHash)
                val integrityToken = tokenResult.getOrElse {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = it.message ?: "Could not get integrity token."
                    )
                    return@launch
                }

                val response = authRepository.changeEmail(
                    newEmail = newEmail,
                    integrityToken = integrityToken,
                    integrityRequestHash = requestHash
                )

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = null
                    )
                    UiMessageManager.showMessage(
                        "Email change requested. Check your new email for the confirmation link."
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = ApiErrorParser.parseMessage(response)
                            ?: "Could not request email change."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = e.message ?: "Network error."
                )
            }
        }
    }
}