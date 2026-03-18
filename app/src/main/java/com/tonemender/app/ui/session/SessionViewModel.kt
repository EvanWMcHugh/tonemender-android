package com.tonemender.app.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tonemender.app.data.local.session.SessionStore
import com.tonemender.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
            sessionStore.isSignedInFlow
                .combine(sessionStore.refreshTriggerFlow) { signedIn, _ -> signedIn }
                .collect { signedIn ->
                    _isReady.value = false

                    if (signedIn) {
                        validateSession()
                    } else {
                        _isSignedIn.value = false
                        _isReady.value = true
                    }
                }
        }
    }

    private suspend fun validateSession() {
        try {
            val response = authRepository.me()

            if (response.isSuccessful && response.body()?.user != null) {
                _isSignedIn.value = true
            } else {
                authRepository.clearSession()
                sessionStore.clear()
                _isSignedIn.value = false
            }
        } catch (_: Exception) {
            authRepository.clearSession()
            sessionStore.clear()
            _isSignedIn.value = false
        }

        _isReady.value = true
    }

    fun setSignedIn(value: Boolean) {
        viewModelScope.launch {
            _isReady.value = false
            sessionStore.setSignedIn(value)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isReady.value = false

            try {
                authRepository.signOut()
            } catch (_: Exception) {
            }

            authRepository.clearSession()
            sessionStore.clear()
            _isSignedIn.value = false
            _isReady.value = true
        }
    }
}