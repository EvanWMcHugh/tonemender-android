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
        loadUser()
        loadUsage()
    }

    fun refreshUsage() {
        loadUsage()
    }

    fun refreshUser() {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            try {
                val response = authRepository.me()

                if (response.isSuccessful) {
                    val user = response.body()?.user
                    _uiState.value = _uiState.value.copy(
                        isPro = user?.isPro == true
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun loadUsage() {
        viewModelScope.launch {
            try {
                val response = rewriteRepository.getUsageStats()
                if (response.isSuccessful) {
                    val stats = response.body()?.stats
                    _uiState.value = _uiState.value.copy(
                        usageToday = stats?.today ?: _uiState.value.usageToday,
                        usageTotal = stats?.total ?: _uiState.value.usageTotal
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    fun updateMessage(value: String) {
        val trimmedToLimit = value.take(MAX_MESSAGE_CHARS)
        val state = _uiState.value

        _uiState.value = state.copy(
            message = trimmedToLimit,
            errorMessage = null
        )
    }

    fun updateTone(value: String?) {
        _uiState.value = _uiState.value.copy(
            selectedTone = value,
            errorMessage = null
        )
    }

    fun updateRecipient(value: String?) {
        _uiState.value = _uiState.value.copy(
            selectedRecipient = value,
            errorMessage = null
        )
    }

    fun rewrite() {
        val currentState = _uiState.value
        val message = currentState.message.trim()

        if (message.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "Enter a message to rewrite."
            )
            return
        }

        _uiState.value = currentState.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val recipientToSend =
                    if (_uiState.value.isPro) _uiState.value.selectedRecipient else null

                val toneToSend =
                    if (_uiState.value.isPro) _uiState.value.selectedTone else null

                val response = rewriteRepository.rewrite(
                    message = message,
                    recipient = recipientToSend,
                    tone = toneToSend
                )

                if (response.isSuccessful) {
                    val body = response.body()

                    val chosenRewrite = when {
                        !_uiState.value.isPro -> body?.clear?.trim()
                        _uiState.value.selectedTone == "soft" -> body?.soft?.trim()
                        _uiState.value.selectedTone == "calm" -> body?.calm?.trim()
                        else -> body?.clear?.trim()
                    }

                    if (!chosenRewrite.isNullOrBlank()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            rewrittenMessage = chosenRewrite,
                            originalMessageSnapshot = message,
                            errorMessage = null,
                            toneScore = body?.toneScore,
                            emotionalImpact = body?.emotionalImpact,
                            usageToday = body?.usageToday ?: _uiState.value.usageToday,
                            usageTotal = body?.usageTotal ?: _uiState.value.usageTotal
                        )

                        loadUsage()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = body?.error
                                ?: body?.message
                                ?: "Rewrite failed."
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = ApiErrorParser.parseMessage(response)
                            ?: "Rewrite failed (${response.code()})."
                    )
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        rewrittenMessage = buildMockRewrite(
                            original = message,
                            tone = if (_uiState.value.isPro) _uiState.value.selectedTone else null,
                            recipient = if (_uiState.value.isPro) _uiState.value.selectedRecipient else null
                        ),
                        originalMessageSnapshot = message,
                        errorMessage = null,
                        toneScore = 82,
                        emotionalImpact = "Comes across more clear and less reactive."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Network error."
                    )
                }
            }
        }
    }

    fun revertToOriginalMessage() {
        val snapshot = _uiState.value.originalMessageSnapshot
        if (!snapshot.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                message = snapshot,
                errorMessage = null
            )
        }
    }

    fun useRewriteAsOriginal() {
        val rewritten = _uiState.value.rewrittenMessage.trim()
        if (rewritten.isBlank()) return

        _uiState.value = _uiState.value.copy(
            message = rewritten,
            errorMessage = null
        )
    }

    fun clearOriginalMessage() {
        _uiState.value = _uiState.value.copy(
            message = "",
            errorMessage = null
        )
    }

    fun saveDraft() {
        val state = _uiState.value
        val original = state.message.trim()
        val rewritten = state.rewrittenMessage.trim()

        if (original.isBlank() || rewritten.isBlank()) {
            _uiState.value = state.copy(
                errorMessage = "Generate a rewrite before saving a draft."
            )
            return
        }

        viewModelScope.launch {
            try {
                val response = if (state.editingDraftId != null) {
                    rewriteRepository.updateDraft(
                        draftId = state.editingDraftId,
                        originalMessage = original,
                        rewrittenMessage = rewritten,
                        recipient = if (state.isPro) state.selectedRecipient else null,
                        tone = if (state.isPro) state.selectedTone else null
                    )
                } else {
                    rewriteRepository.createDraft(
                        originalMessage = original,
                        rewrittenMessage = rewritten,
                        recipient = if (state.isPro) state.selectedRecipient else null,
                        tone = if (state.isPro) state.selectedTone else null
                    )
                }

                if (response.isSuccessful) {
                    val returnedDraft = response.body()?.draft

                    _uiState.value = _uiState.value.copy(
                        editingDraftId = returnedDraft?.id ?: state.editingDraftId,
                        errorMessage = null
                    )

                    UiMessageManager.showMessage(
                        if (state.editingDraftId != null) "Draft updated." else "Draft saved."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = ApiErrorParser.parseMessage(response) ?: "Failed to save draft."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to save draft."
                )
            }
        }
    }

    fun loadDraft(draft: Draft) {
        _uiState.value = _uiState.value.copy(
            message = draft.originalMessage,
            rewrittenMessage = draft.rewrittenMessage,
            selectedRecipient = draft.recipient,
            selectedTone = draft.tone,
            errorMessage = null,
            isLoading = false,
            editingDraftId = draft.id,
            originalMessageSnapshot = draft.originalMessage,
            toneScore = null,
            emotionalImpact = null
        )
    }

    fun copyRewriteToClipboard() {
        UiMessageManager.showMessage("Rewrite copied.")
    }

    fun shareRewrite() {
        UiMessageManager.showMessage("Opening share sheet…")
    }

    fun shareBeforeAfter() {
        UiMessageManager.showMessage("Opening before/after share sheet…")
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