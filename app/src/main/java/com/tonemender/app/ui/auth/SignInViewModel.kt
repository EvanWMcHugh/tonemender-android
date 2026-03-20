package com.tonemender.app.ui.auth

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tonemender.app.data.remote.ApiErrorParser
import com.tonemender.app.data.repository.AuthRepository
import com.tonemender.app.data.security.PlayIntegrityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val playIntegrityManager = PlayIntegrityManager(application.applicationContext)

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(
            email = value,
            errorMessage = null,
            likelyNeedsVerification = false,
            verificationResendMessage = null
        )
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            errorMessage = null,
            likelyNeedsVerification = false,
            verificationResendMessage = null
        )
    }

    fun signIn(onSuccess: () -> Unit) {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Enter your email and password.",
                likelyNeedsVerification = false,
                verificationResendMessage = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isResendingVerification = false,
            errorMessage = null,
            likelyNeedsVerification = false,
            verificationResendMessage = null
        )

        viewModelScope.launch {
            try {
                val requestHash = PlayIntegrityManager.buildRequestHash(
                    "sign_in",
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

                val response = authRepository.signIn(
                    email = email,
                    password = password,
                    integrityToken = integrityToken,
                    integrityRequestHash = requestHash,
                    deviceName = buildDeviceName()
                )

                if (response.isSuccessful && response.body()?.user != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        likelyNeedsVerification = false,
                        verificationResendMessage = null
                    )
                    onSuccess()
                } else {
                    val message = ApiErrorParser.parseMessage(response) ?: "Sign in failed."

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message,
                        likelyNeedsVerification = looksLikeVerificationIssue(message)
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

    private fun buildDeviceName(): String {
        val manufacturer = Build.MANUFACTURER ?: "Android"
        val model = Build.MODEL ?: "Device"
        return "$manufacturer $model".trim()
    }

    private fun looksLikeVerificationIssue(message: String): Boolean {
        val normalized = message.lowercase()

        return normalized.contains("verify your email") ||
                normalized.contains("verification email") ||
                normalized.contains("email not verified") ||
                normalized.contains("email not confirmed") ||
                normalized.contains("not verified") ||
                normalized.contains("not confirmed") ||
                normalized.contains("confirm your email") ||
                normalized.contains("email confirmation") ||
                normalized.contains("unverified")
    }
}