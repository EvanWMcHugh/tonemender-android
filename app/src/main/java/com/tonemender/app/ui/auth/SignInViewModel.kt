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
        updateState {
            copy(
                email = value,
                errorMessage = null,
                likelyNeedsVerification = false,
                verificationResendMessage = null
            )
        }
    }

    fun updatePassword(value: String) {
        updateState {
            copy(
                password = value,
                errorMessage = null,
                likelyNeedsVerification = false,
                verificationResendMessage = null
            )
        }
    }

    fun signIn(onSuccess: () -> Unit) {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            updateState {
                copy(
                    errorMessage = "Enter your email and password.",
                    likelyNeedsVerification = false,
                    verificationResendMessage = null
                )
            }
            return
        }

        updateState {
            copy(
                isLoading = true,
                isResendingVerification = false,
                errorMessage = null,
                likelyNeedsVerification = false,
                verificationResendMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val integrityToken = getIntegrityToken(
                    action = "sign_in",
                    email,
                    password
                ) ?: return@launch

                val response = authRepository.signIn(
                    email = email,
                    password = password,
                    integrityToken = integrityToken.token,
                    integrityRequestHash = integrityToken.requestHash,
                    deviceName = buildDeviceName()
                )

                if (response.isSuccessful && response.body()?.user != null) {
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = null,
                            likelyNeedsVerification = false,
                            verificationResendMessage = null
                        )
                    }
                    onSuccess()
                } else {
                    val rawError = try {
                        response.errorBody()?.string()
                    } catch (_: Exception) {
                        null
                    }

                    val message = ApiErrorParser.parseMessage(response)
                        ?: rawError
                        ?: "Sign in failed."

                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = message,
                            likelyNeedsVerification = looksLikeVerificationIssue(message)
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
                val integrityToken = getIntegrityToken(
                    action = "resend_email_verification",
                    email
                ) ?: return@launch

                val response = authRepository.resendEmailVerification(
                    email = email,
                    integrityToken = integrityToken.token,
                    integrityRequestHash = integrityToken.requestHash
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
                    val rawError = try {
                        response.errorBody()?.string()
                    } catch (_: Exception) {
                        null
                    }

                    updateState {
                        copy(
                            isResendingVerification = false,
                            errorMessage = ApiErrorParser.parseMessage(response)
                                ?: rawError
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

    private inline fun updateState(transform: SignInUiState.() -> SignInUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private data class IntegrityResult(
        val token: String,
        val requestHash: String
    )
}