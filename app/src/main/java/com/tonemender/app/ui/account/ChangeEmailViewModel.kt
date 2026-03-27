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

    fun updateNewEmail(value: String) = updateState {
        copy(
            newEmail = value,
            errorMessage = null
        )
    }

    fun loadCurrentEmail() {
        viewModelScope.launch {
            updateState {
                copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val response = authRepository.getMe()

                if (response.isSuccessful) {
                    val email = response.body()?.user?.email

                    updateState {
                        copy(
                            currentEmail = email,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                } else {
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = "Could not load account email."
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

    fun submit(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        val newEmail = state.newEmail.trim()
        val currentEmail = state.currentEmail?.trim().orEmpty()

        val validationError = validateInput(newEmail, currentEmail)
        if (validationError != null) {
            updateState { copy(errorMessage = validationError) }
            return
        }

        updateState {
            copy(
                isSubmitting = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val integrity = getIntegrityToken(newEmail) ?: run {
                    updateState {
                        copy(
                            isSubmitting = false,
                            errorMessage = "Could not complete integrity check."
                        )
                    }
                    return@launch
                }

                val (integrityToken, integrityRequestHash) = integrity

                val response = authRepository.requestEmailChange(
                    newEmail = newEmail,
                    integrityToken = integrityToken,
                    integrityRequestHash = integrityRequestHash
                )

                if (response.isSuccessful) {
                    updateState {
                        copy(
                            isSubmitting = false,
                            errorMessage = null
                        )
                    }

                    UiMessageManager.showMessage(
                        "Email change requested. Check your new email for the confirmation link."
                    )

                    onSuccess()
                } else {
                    updateState {
                        copy(
                            isSubmitting = false,
                            errorMessage = ApiErrorParser.parseMessage(response)
                                ?: "Could not request email change."
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Network error."
                    )
                }
            }
        }
    }

    private fun validateInput(
        newEmail: String,
        currentEmail: String
    ): String? {
        return when {
            newEmail.isBlank() -> "Enter your new email."
            newEmail.equals(currentEmail, ignoreCase = true) -> "Enter a different email."
            else -> null
        }
    }

    private suspend fun getIntegrityToken(
        email: String
    ): Pair<String, String>? {
        val requestHash = PlayIntegrityManager.buildRequestHash(
            "change_email",
            email
        )

        val prepareResult = playIntegrityManager.prepare()
        if (prepareResult.isFailure) return null

        val tokenResult = playIntegrityManager.requestToken(requestHash)
        val token = tokenResult.getOrElse { return null }

        return token to requestHash
    }

    private inline fun updateState(transform: ChangeEmailUiState.() -> ChangeEmailUiState) {
        _uiState.value = _uiState.value.transform()
    }
}