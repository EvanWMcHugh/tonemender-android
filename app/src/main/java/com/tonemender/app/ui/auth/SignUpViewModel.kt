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

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(
            email = value,
            errorMessage = null,
            verificationResendMessage = null
        )
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            errorMessage = null,
            verificationResendMessage = null
        )
    }

    fun updateConfirmPassword(value: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = value,
            errorMessage = null,
            verificationResendMessage = null
        )
    }

    fun signUp() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val confirmPassword = _uiState.value.confirmPassword

        when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Enter your email."
                )
                return
            }

            password.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Enter your password."
                )
                return
            }

            confirmPassword.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Confirm your password."
                )
                return
            }

            password.length < 8 -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Password must be at least 8 characters."
                )
                return
            }

            password.length > 200 -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Password is too long."
                )
                return
            }

            password != confirmPassword -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Passwords do not match."
                )
                return
            }
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            verificationResendMessage = null
        )

        viewModelScope.launch {
            try {
                val requestHash = PlayIntegrityManager.buildRequestHash(
                    "sign_up",
                    email,
                    password
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

                val response = authRepository.signUp(
                    email = email,
                    password = password,
                    integrityToken = integrityToken,
                    integrityRequestHash = requestHash
                )

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        didCreateAccount = true,
                        verificationResendMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = ApiErrorParser.parseMessage(response) ?: "Sign up failed."
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

    fun resendVerificationEmail() {
        val email = _uiState.value.email.trim()

        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Enter your email first.",
                verificationResendMessage = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isResendingVerification = true,
            errorMessage = null,
            verificationResendMessage = null
        )

        viewModelScope.launch {
            try {
                val requestHash = PlayIntegrityManager.buildRequestHash(
                    "resend_email_verification",
                    email
                )

                val prepareResult = playIntegrityManager.prepare()
                if (prepareResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isResendingVerification = false,
                        errorMessage = prepareResult.exceptionOrNull()?.message
                            ?: "Could not prepare integrity check."
                    )
                    return@launch
                }

                val tokenResult = playIntegrityManager.requestToken(requestHash)
                val integrityToken = tokenResult.getOrElse {
                    _uiState.value = _uiState.value.copy(
                        isResendingVerification = false,
                        errorMessage = it.message ?: "Could not get integrity token."
                    )
                    return@launch
                }

                val response = authRepository.resendEmailVerification(
                    email = email,
                    integrityToken = integrityToken,
                    integrityRequestHash = requestHash
                )

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isResendingVerification = false,
                        verificationResendMessage =
                            "If that account exists and still needs verification, we sent a confirmation email."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isResendingVerification = false,
                        errorMessage = ApiErrorParser.parseMessage(response)
                            ?: "Could not resend verification email."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isResendingVerification = false,
                    errorMessage = e.message ?: "Network error."
                )
            }
        }
    }
}