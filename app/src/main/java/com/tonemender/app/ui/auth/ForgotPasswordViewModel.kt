package com.tonemender.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tonemender.app.data.remote.ApiErrorParser
import com.tonemender.app.data.repository.AuthRepository
import com.tonemender.app.data.security.PlayIntegrityManager
import com.tonemender.app.ui.common.UiMessageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val playIntegrityManager = PlayIntegrityManager(application.applicationContext)

    private val initialEmail = savedStateHandle.get<String>("email").orEmpty()

    private val _uiState = MutableStateFlow(
        ForgotPasswordUiState(email = initialEmail)
    )
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(
            email = value,
            errorMessage = null
        )
    }

    fun submit(onSuccess: () -> Unit = {}) {
        val email = _uiState.value.email.trim()

        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Enter your email."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val requestHash = PlayIntegrityManager.buildRequestHash(
                    "forgot_password",
                    email
                )

                val prepareResult = playIntegrityManager.prepare()
                if (prepareResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = prepareResult.exceptionOrNull()?.message
                            ?: "Could not prepare integrity check."
                    )
                    return@launch
                }

                val tokenResult = playIntegrityManager.requestToken(requestHash)
                val integrityToken = tokenResult.getOrElse {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = it.message ?: "Could not get integrity token."
                    )
                    return@launch
                }

                val response = authRepository.forgotPassword(
                    email = email,
                    integrityToken = integrityToken,
                    integrityRequestHash = requestHash
                )

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                    UiMessageManager.showMessage("If that email exists, a reset link has been sent.")
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = ApiErrorParser.parseMessage(response)
                            ?: "Could not send reset email."
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
}