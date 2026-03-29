package com.tonemender.app.ui.rewrite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonemender.app.BuildConfig
import com.tonemender.app.data.local.drafts.Draft
import com.tonemender.app.data.remote.ApiErrorParser
import com.tonemender.app.data.repository.AuthRepository
import com.tonemender.app.data.repository.RewriteRepository
import com.tonemender.app.ui.common.UiMessageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RewriteViewModel(
    private val rewriteRepository: RewriteRepository = RewriteRepository()
) : ViewModel() {

    companion object {
        private const val MAX_MESSAGE_CHARS = 2000
    }

    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(RewriteUiState())
    val uiState: StateFlow<RewriteUiState> = _uiState.asStateFlow()

    init {
        refreshUser()
        refreshUsage()
    }

    fun refreshUsage() = loadUsage()
    fun refreshUser() = loadUser()

    private fun loadUser() {
        viewModelScope.launch {
            try {
                val response = authRepository.getMe()
                if (response.isSuccessful) {
                    val isPro = response.body()?.user?.isPro == true
                    updateState { copy(isPro = isPro) }
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadUsage() {
        viewModelScope.launch {
            try {
                val response = rewriteRepository.getUsageStats()
                if (response.isSuccessful) {
                    val stats = response.body()?.stats
                    updateState {
                        copy(
                            usageToday = stats?.today ?: usageToday,
                            usageTotal = stats?.total ?: usageTotal
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun updateMessage(value: String) {
        updateState {
            copy(
                message = value.take(MAX_MESSAGE_CHARS),
                errorMessage = null
            )
        }
    }

    fun updateTone(value: String?) {
        updateState { copy(selectedTone = value, errorMessage = null) }
    }

    fun updateRecipient(value: String?) {
        updateState { copy(selectedRecipient = value, errorMessage = null) }
    }

    fun rewrite() {
        val state = _uiState.value
        val message = state.message.trim()

        if (message.isBlank()) {
            updateState { copy(errorMessage = "Enter a message to rewrite.") }
            return
        }

        updateState { copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val response = rewriteRepository.rewrite(
                    message = message,
                    recipient = state.selectedRecipient.takeIf { state.isPro },
                    tone = state.selectedTone.takeIf { state.isPro }
                )

                if (response.isSuccessful) {
                    handleRewriteSuccess(message, response.body())
                } else {
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = ApiErrorParser.parseMessage(response)
                                ?: "Rewrite failed (${response.code()})."
                        )
                    }
                }
            } catch (e: Exception) {
                handleRewriteException(message, e)
            }
        }
    }

    private fun handleRewriteSuccess(
        originalMessage: String,
        body: com.tonemender.app.data.remote.model.RewriteResponse?
    ) {
        val state = _uiState.value

        val chosenRewrite = when {
            !state.isPro -> body?.clear?.trim()
            state.selectedTone == "soft" -> body?.soft?.trim()
            state.selectedTone == "calm" -> body?.calm?.trim()
            else -> body?.clear?.trim()
        }

        if (chosenRewrite.isNullOrBlank()) {
            updateState {
                copy(
                    isLoading = false,
                    errorMessage = body?.error ?: body?.message ?: "Rewrite failed."
                )
            }
            return
        }

        updateState {
            copy(
                isLoading = false,
                rewrittenMessage = chosenRewrite,
                originalMessageSnapshot = originalMessage,
                errorMessage = null,
                toneScore = body?.toneScore,
                emotionalImpact = body?.emotionalImpact,
                usageToday = body?.usageToday ?: usageToday,
                usageTotal = body?.usageTotal ?: usageTotal
            )
        }

        loadUsage()
    }

    private fun handleRewriteException(message: String, e: Exception) {
        if (BuildConfig.DEBUG) {
            updateState {
                copy(
                    isLoading = false,
                    rewrittenMessage = buildMockRewrite(
                        original = message,
                        tone = selectedTone,
                        recipient = selectedRecipient
                    ),
                    originalMessageSnapshot = message,
                    errorMessage = null,
                    toneScore = 82,
                    emotionalImpact = "Comes across more clear and less reactive."
                )
            }
        } else {
            updateState {
                copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Network error."
                )
            }
        }
    }

    fun saveDraft() {
        val state = _uiState.value
        val original = state.message.trim()
        val rewritten = state.rewrittenMessage.trim()

        if (original.isBlank() || rewritten.isBlank()) {
            updateState {
                copy(errorMessage = "Generate a rewrite before saving a draft.")
            }
            return
        }

        viewModelScope.launch {
            try {
                val response = rewriteRepository.createDraft(
                    originalMessage = original,
                    rewrittenMessage = rewritten,
                    tone = state.selectedTone.takeIf { state.isPro }
                )

                if (response.isSuccessful) {
                    UiMessageManager.showMessage("Draft saved.")
                } else {
                    updateState {
                        copy(
                            errorMessage = ApiErrorParser.parseMessage(response)
                                ?: "Failed to save draft."
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(errorMessage = e.message ?: "Failed to save draft.")
                }
            }
        }
    }

    fun loadDraft(draft: Draft) {
        updateState {
            copy(
                message = draft.originalMessage,
                rewrittenMessage = draft.rewrittenMessage,
                selectedRecipient = draft.recipient,
                selectedTone = draft.tone,
                originalMessageSnapshot = draft.originalMessage,
                isLoading = false,
                errorMessage = null,
                toneScore = null,
                emotionalImpact = null
            )
        }
    }

    fun revertToOriginalMessage() {
        _uiState.value.originalMessageSnapshot?.let {
            updateState { copy(message = it, errorMessage = null) }
        }
    }

    fun useRewriteAsOriginal() {
        val rewritten = _uiState.value.rewrittenMessage.trim()
        if (rewritten.isNotBlank()) {
            updateState { copy(message = rewritten, errorMessage = null) }
        }
    }

    fun clearOriginalMessage() {
        updateState { copy(message = "", errorMessage = null) }
    }

    fun copyRewriteToClipboard() {
        UiMessageManager.showMessage("Rewrite copied.")
    }

    fun shareRewrite() {
        UiMessageManager.showMessage("Opening share sheet…")
    }

    private inline fun updateState(transform: RewriteUiState.() -> RewriteUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private fun buildMockRewrite(
        original: String,
        tone: String?,
        recipient: String?
    ): String {
        val toneLabel = tone ?: "clear"
        val recipientLabel = recipient ?: "person"

        return when (toneLabel) {
            "soft" -> "Hey, I just wanted to say this a little more gently for my $recipientLabel: $original"
            "calm" -> "Here’s a calmer version for my $recipientLabel: $original"
            else -> "Here’s a clearer version for my $recipientLabel: $original"
        }
    }
}