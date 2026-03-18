package com.tonemender.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonemender.app.data.repository.AuthRepository
import com.tonemender.app.data.repository.RewriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val rewriteRepository = RewriteRepository()

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState

    fun loadAccount() {
        viewModelScope.launch {
            val previous = _uiState.value

            try {
                _uiState.value = previous.copy(
                    loading = true,
                    error = null
                )

                val meResponse = authRepository.me()
                val statsResponse = rewriteRepository.getUsageStats()

                if (!meResponse.isSuccessful) {
                    _uiState.value = previous.copy(
                        loading = false,
                        error = "Failed to load account (${meResponse.code()})"
                    )
                    return@launch
                }

                val user = meResponse.body()?.user
                val stats = if (statsResponse.isSuccessful) statsResponse.body()?.stats else null

                _uiState.value = AccountUiState(
                    loading = false,
                    email = user?.email,
                    isPro = user?.isPro ?: false,
                    planType = user?.planType,
                    rewritesToday = stats?.today ?: 0,
                    totalRewrites = stats?.total ?: 0,
                    error = if (user == null) "No signed-in user found" else null
                )
            } catch (e: Exception) {
                _uiState.value = previous.copy(
                    loading = false,
                    error = e.message ?: "Failed to load account."
                )
            }
        }
    }
}