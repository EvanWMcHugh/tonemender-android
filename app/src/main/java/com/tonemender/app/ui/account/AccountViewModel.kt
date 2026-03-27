package com.tonemender.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonemender.app.data.repository.AuthRepository
import com.tonemender.app.data.repository.RewriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val rewriteRepository: RewriteRepository = RewriteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun loadAccount() {
        viewModelScope.launch {
            updateState {
                copy(
                    loading = true,
                    error = null
                )
            }

            try {
                val meResponse = authRepository.getMe()
                val statsResponse = rewriteRepository.getUsageStats()

                if (!meResponse.isSuccessful) {
                    updateState {
                        copy(
                            loading = false,
                            error = "Failed to load account (${meResponse.code()})."
                        )
                    }
                    return@launch
                }

                val user = meResponse.body()?.user
                val stats = if (statsResponse.isSuccessful) {
                    statsResponse.body()?.stats
                } else {
                    null
                }

                updateState {
                    copy(
                        loading = false,
                        email = user?.email,
                        isPro = user?.isPro == true,
                        planType = user?.planType,
                        rewritesToday = stats?.today ?: 0,
                        totalRewrites = stats?.total ?: 0,
                        error = if (user == null) "No signed-in user found." else null
                    )
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        loading = false,
                        error = e.message ?: "Failed to load account."
                    )
                }
            }
        }
    }

    private inline fun updateState(transform: AccountUiState.() -> AccountUiState) {
        _uiState.value = _uiState.value.transform()
    }
}