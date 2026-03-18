package com.tonemender.app.ui.account

data class AccountUiState(
    val loading: Boolean = true,
    val email: String? = null,
    val isPro: Boolean = false,
    val planType: String? = null,
    val rewritesToday: Int = 0,
    val totalRewrites: Int = 0,
    val error: String? = null
)