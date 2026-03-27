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

    fun updateEmail(value: String) = updateState {
        copy(
            email = value,
            errorMessage = null
        )
    }

    fun submit(onSuccess: () -> Unit = {}) {
        val email = _uiState.value.email.trim()

        if (email.isBlank()) {
            updateState {
                copy(errorMessage = "Enter your email.")
            }
            return
        }

        updateState {
            copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val integrity = getIntegrityToken(
                    action = "forgot_password",
                    email
                ) ?: return@launch

                val response = authRepository.requestPasswordReset(
                    email = email,
                    integrityToken = integrity.token,
                    integrityRequestHash = integrity.requestHash
                )

                if (response.isSuccessful) {
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = null
                        )
                    }

                    UiMessageManager.showMessage(
                        "If that email exists, a reset link has been sent."
                    )

                    onSuccess()
                } else {
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = ApiErrorParser.parseMessage(response)
                                ?: "Could not send reset email."
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Network error."
                    )
                }
            }
        }
    }

    private suspend fun getIntegrityToken(
        action: String,
        vararg inputs: String
    ): IntegrityResult? {
        val requestHash = PlayIntegrityManager.buildRequestHash(action, *inputs)

        val prepareResult = playIntegrityManager.prepare()
        if (prepareResult.isFailure) {
            updateState {
                copy(
                    isLoading = false,
                    errorMessage = prepareResult.exceptionOrNull()?.message
                        ?: "Could not prepare integrity check."
                )
            }
            return null
        }

        val tokenResult = playIntegrityManager.requestToken(requestHash)
        val token = tokenResult.getOrElse {
            updateState {
                copy(
                    isLoading = false,
                    errorMessage = it.message ?: "Could not get integrity token."
                )
            }
            return null
        }

        return IntegrityResult(
            token = token,
            requestHash = requestHash
        )
    }

    private inline fun updateState(transform: ForgotPasswordUiState.() -> ForgotPasswordUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private data class IntegrityResult(
        val token: String,
        val requestHash: String
    )
}