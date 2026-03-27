package com.tonemender.app.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tonemender.app.data.local.session.SessionStore
import com.tonemender.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionStore = SessionStore(application.applicationContext)
    private val authRepository = AuthRepository()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.isSignedInFlow.collect { signedIn ->
                if (signedIn) {
                    validateSession()
                } else {
                    markSignedOutReady()
                }
            }
        }

        viewModelScope.launch {
            sessionStore.refreshTriggerFlow.collect {
                if (sessionStore.isSignedIn()) {
                    validateSession()
                } else {
                    markSignedOutReady()
                }
            }
        }
    }

    private suspend fun validateSession() {
        _isReady.value = false

        try {
            val response = authRepository.getMe()
            val user = response.body()?.user

            if (response.isSuccessful && user != null) {
                _isSignedIn.value = true
            } else {
                clearLocalSessionState()
            }
        } catch (_: Exception) {
            clearLocalSessionState()
        } finally {
            _isReady.value = true
        }
    }

    fun setSignedIn(value: Boolean) {
        viewModelScope.launch {
            _isReady.value = false
            sessionStore.setSignedIn(value)

            if (!value) {
                _isSignedIn.value = false
                _isReady.value = true
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isReady.value = false

            try {
                authRepository.signOut()
            } catch (_: Exception) {
                // Intentionally ignored:
                // local sign-out should still succeed even if the network request fails.
            }

            clearLocalSessionState()
            _isReady.value = true
        }
    }

    private suspend fun clearLocalSessionState() {
        authRepository.clearSession()
        sessionStore.clear()
        _isSignedIn.value = false
    }

    private fun markSignedOutReady() {
        _isSignedIn.value = false
        _isReady.value = true
    }
}