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
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )

                val response = authRepository.me()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        email = response.body()?.user?.email,
                        isLoading = false,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Could not load account details."
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

    fun requestDelete() {
        _uiState.value = _uiState.value.copy(
            showConfirmDialog = true,
            errorMessage = null
        )
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(
            showConfirmDialog = false
        )
    }

    fun confirmDelete(onSuccess: () -> Unit) {
        val state = _uiState.value
        val email = state.email.orEmpty()

        _uiState.value = state.copy(
            isDeleting = true,
            errorMessage = null,
            showConfirmDialog = false
        )

        viewModelScope.launch {
            try {
                val requestHash = PlayIntegrityManager.buildRequestHash(
                    "delete_account",
                    email
                )

                val prepareResult = playIntegrityManager.prepare()
                if (prepareResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        errorMessage = prepareResult.exceptionOrNull()?.message
                            ?: "Could not prepare integrity check."
                    )
                    return@launch
                }

                val tokenResult = playIntegrityManager.requestToken(requestHash)
                val integrityToken = tokenResult.getOrElse {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        errorMessage = it.message ?: "Could not get integrity token."
                    )
                    return@launch
                }

                val response = authRepository.deleteAccount(
                    integrityToken = integrityToken,
                    integrityRequestHash = requestHash
                )

                if (response.isSuccessful) {
                    authRepository.clearSession()
                    UiMessageManager.showMessage("Your account has been deleted.")
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        errorMessage = null
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        errorMessage = ApiErrorParser.parseMessage(response)
                            ?: "Could not delete account."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    errorMessage = e.message ?: "Network error."
                )
            }
        }
    }
}