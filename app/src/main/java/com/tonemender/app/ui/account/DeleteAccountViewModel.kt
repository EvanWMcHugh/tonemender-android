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

class DeleteAccountViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val playIntegrityManager = PlayIntegrityManager(application.applicationContext)

    private val _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    fun loadCurrentUser() {
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
                    updateState {
                        copy(
                            email = response.body()?.user?.email,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                } else {
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = "Could not load account details."
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

    fun requestDelete() {
        updateState {
            copy(
                showConfirmDialog = true,
                errorMessage = null
            )
        }
    }

    fun cancelDelete() {
        updateState {
            copy(showConfirmDialog = false)
        }
    }

    fun confirmDelete(onSuccess: () -> Unit) {
        val email = _uiState.value.email.orEmpty()

        updateState {
            copy(
                isDeleting = true,
                errorMessage = null,
                showConfirmDialog = false
            )
        }

        viewModelScope.launch {
            try {
                val integrity = getIntegrityToken(
                    action = "delete_account",
                    email
                ) ?: return@launch

                val response = authRepository.deleteAccount(
                    integrityToken = integrity.token,
                    integrityRequestHash = integrity.requestHash
                )

                if (response.isSuccessful) {
                    authRepository.clearSession()
                    UiMessageManager.showMessage("Your account has been deleted.")

                    updateState {
                        copy(
                            isDeleting = false,
                            errorMessage = null
                        )
                    }

                    onSuccess()
                } else {
                    updateState {
                        copy(
                            isDeleting = false,
                            errorMessage = ApiErrorParser.parseMessage(response)
                                ?: "Could not delete account."
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isDeleting = false,
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
                    isDeleting = false,
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
                    isDeleting = false,
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

    private inline fun updateState(transform: DeleteAccountUiState.() -> DeleteAccountUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private data class IntegrityResult(
        val token: String,
        val requestHash: String
    )
}