package com.tonemender.app.ui.rewrite

data class RewriteUiState(
    val message: String = "",
    val selectedTone: String? = null,
    val selectedRecipient: String? = null,
    val rewrittenMessage: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val editingDraftId: String? = null,
    val isPro: Boolean = false,

    val originalMessageSnapshot: String? = null,
    val toneScore: Int? = null,
    val emotionalImpact: String? = null,
    val usageToday: Int = 0,
    val usageTotal: Int = 0
) {
    val messageCount: Int
        get() = message.length

    val canRewrite: Boolean
        get() = message.trim().isNotBlank() && !isLoading

    val hasRewrite: Boolean
        get() = rewrittenMessage.isNotBlank()

    val hasEditedOriginalSinceRewrite: Boolean
        get() = !originalMessageSnapshot.isNullOrBlank() &&
                message.trim() != originalMessageSnapshot.trim()

    val beforeMessage: String
        get() = originalMessageSnapshot?.takeIf { it.isNotBlank() } ?: message
}