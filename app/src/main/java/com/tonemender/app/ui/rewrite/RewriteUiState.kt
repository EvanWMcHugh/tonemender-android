package com.tonemender.app.ui.rewrite

data class RewriteUiState(
    val message: String = "",
    val selectedTone: String? = null,
    val selectedRecipient: String? = null,
    val rewrittenMessage: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPro: Boolean = false,
    val originalMessageSnapshot: String? = null,
    val toneScore: Int? = null,
    val emotionalImpact: String? = null,
    val rewritesLeft: Int? = null,
    val limitReached: Boolean = false
) {

    val trimmedMessage: String
        get() = message.trim()

    val messageCount: Int
        get() = message.length

    val canRewrite: Boolean
        get() = trimmedMessage.isNotEmpty() && !isLoading

    val hasRewrite: Boolean
        get() = rewrittenMessage.isNotBlank()

    val hasEditedOriginalSinceRewrite: Boolean
        get() = !originalMessageSnapshot.isNullOrBlank() &&
                trimmedMessage != originalMessageSnapshot.trim()
}