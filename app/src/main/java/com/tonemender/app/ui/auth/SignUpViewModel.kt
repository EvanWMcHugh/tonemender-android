package com.tonemender.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tonemender.app.data.remote.ApiErrorParser
import com.tonemender.app.data.repository.AuthRepository
import com.tonemender.app.data.security.PlayIntegrityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignUpViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val playIntegrityManager = PlayIntegrityManager(application.applicationContext)

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    /* ---------- Input ---------- */

    fun updateEmail(value: String) = updateState {
        copy(
            email = value,
            errorMessage = null,
            verificationResendMessage = null
        )
    }

    fun updatePassword(value: String) = updateState {
        copy(
            password = value,
            errorMessage = null,
            verificationResendMessage = null
        )
    }

    fun updateConfirmPassword(value: String) = updateState {
        copy(
            confirmPassword = value,
            errorMessage = null,
            verificationResendMessage = null
        )
    }

    /* ---------- Sign Up ---------- */

    fun signUp() {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password
        val confirmPassword = state.confirmPassword

        val validationError = validateInput(email, password, confirmPassword)
        if (validationError != null) {
            updateState { copy(errorMessage = validationError) }
            return
        }

        updateState {
            copy(
                isLoading = true,
                errorMessage = null,
                verificationResendMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val integrity = getIntegrityToken("sign_up", email, password) ?: return@launch

                val response = authRepository.signUp(
                    email = email,
                    password = password,
                    integrityToken = integrity.token,
                    integrityRequestHash = integrity.requestHash
                )

                if (response.isSuccessful) {
                    updateState {
                        copy(
                            isLoading = false,
                            didCreateAccount = true,
                            errorMessage = null,
                            verificationResendMessage = null
                        )
                    }
                } else {
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = ApiErrorParser.parseMessage(response)
                                ?: "Sign up failed."
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

    /* ---------- Resend Verification ---------- */

    fun resendVerificationEmail() {
        val email = _uiState.value.email.trim()

        if (email.isBlank()) {
            updateState {
                copy(
                    errorMessage = "Enter your email first.",
                    verificationResendMessage = null
                )
            }
            return
        }

        updateState {
            copy(
                isResendingVerification = true,
                errorMessage = null,
                verificationResendMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val integrity = getIntegrityToken("resend_email_verification", email)
                    ?: return@launch

                val response = authRepository.resendEmailVerification(
                    email = email,
                    integrityToken = integrity.token,
                    integrityRequestHash = integrity.requestHash
                )

                if (response.isSuccessful) {
                    updateState {
                        copy(
                            isResendingVerification = false,
                            verificationResendMessage =
                                "If that account exists and still needs verification, we sent a confirmation email."
                        )
                    }
                } else {
                    updateState {
                        copy(
                            isResendingVerification = false,
                            errorMessage = ApiErrorParser.parseMessage(response)
                                ?: "Could not resend verification email."
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isResendingVerification = false,
                        errorMessage = e.message ?: "Network error."
                    )
                }
            }
        }
    }

    /* ---------- Helpers ---------- */

    private fun validateInput(
        email: String,
        password: String,
        confirmPassword: String
    ): String? {
        return when {
            email.isBlank() -> "Enter your email."
            password.isBlank() -> "Enter your password."
            confirmPassword.isBlank() -> "Confirm your password."
            password.length < 8 -> "Password must be at least 8 characters."
            password.length > 200 -> "Password is too long."
            password != confirmPassword -> "Passwords do not match."
            else -> null
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
                    isResendingVerification = false,
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
                    isResendingVerification = false,
                    errorMessage = it.message ?: "Could not get integrity token."
                )
            }
            return null
        }

        return IntegrityResult(token = token, requestHash = requestHash)
    }

    private inline fun updateState(transform: SignUpUiState.() -> SignUpUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private data class IntegrityResult(
        val token: String,
        val requestHash: String
    )
}