package com.tonemender.app.ui.drafts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonemender.app.data.local.drafts.Draft
import com.tonemender.app.data.remote.ApiErrorParser
import com.tonemender.app.data.remote.model.DraftDto
import com.tonemender.app.data.repository.RewriteRepository
import com.tonemender.app.ui.common.UiMessageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

class DraftsViewModel(
    private val rewriteRepository: RewriteRepository = RewriteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DraftsUiState())
    val uiState: StateFlow<DraftsUiState> = _uiState.asStateFlow()

    fun loadDrafts() {
        viewModelScope.launch {
            try {
                val response = rewriteRepository.getDrafts()

                if (response.isSuccessful) {
                    val drafts = response.body()
                        ?.drafts
                        .orEmpty()
                        .map { it.toDraft() }
                        .sortedByDescending { it.createdAt }

                    updateState {
                        copy(
                            drafts = drafts,
                            loadErrorMessage = null
                        )
                    }
                } else {
                    updateState {
                        copy(
                            loadErrorMessage = ApiErrorParser.parseMessage(response)
                                ?: "Failed to load drafts."
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(loadErrorMessage = e.message ?: "Failed to load drafts.")
                }
            }
        }
    }

    fun openDraft(draft: Draft, onOpened: (Draft) -> Unit) {
        onOpened(draft)
    }

    fun requestDeleteDraft(draft: Draft) {
        updateState { copy(pendingDeleteDraft = draft) }
    }

    fun cancelDeleteDraft() {
        updateState { copy(pendingDeleteDraft = null) }
    }

    fun confirmDeleteDraft() {
        val draft = _uiState.value.pendingDeleteDraft ?: return

        viewModelScope.launch {
            try {
                val response = rewriteRepository.deleteDraft(draft.id)

                if (response.isSuccessful) {
                    updateState {
                        copy(
                            drafts = drafts.filterNot { it.id == draft.id },
                            pendingDeleteDraft = null
                        )
                    }

                    UiMessageManager.showMessage("Draft deleted.")
                } else {
                    updateState {
                        copy(pendingDeleteDraft = null)
                    }

                    UiMessageManager.showMessage(
                        ApiErrorParser.parseMessage(response) ?: "Failed to delete draft."
                    )
                }
            } catch (e: Exception) {
                updateState {
                    copy(pendingDeleteDraft = null)
                }

                UiMessageManager.showMessage(e.message ?: "Failed to delete draft.")
            }
        }
    }

    fun requestClearAllDrafts() {
        updateState { copy(showClearAllDialog = true) }
    }

    fun cancelClearAllDrafts() {
        updateState { copy(showClearAllDialog = false) }
    }

    fun confirmClearAllDrafts() {
        viewModelScope.launch {
            try {
                val response = rewriteRepository.deleteAllDrafts()

                if (response.isSuccessful) {
                    updateState {
                        copy(
                            drafts = emptyList(),
                            pendingDeleteDraft = null,
                            showClearAllDialog = false
                        )
                    }

                    UiMessageManager.showMessage("All drafts cleared.")
                } else {
                    updateState {
                        copy(showClearAllDialog = false)
                    }

                    UiMessageManager.showMessage(
                        ApiErrorParser.parseMessage(response) ?: "Failed to clear drafts."
                    )
                }
            } catch (e: Exception) {
                updateState {
                    copy(showClearAllDialog = false)
                }

                UiMessageManager.showMessage(e.message ?: "Failed to clear drafts.")
            }
        }
    }

    private fun DraftDto.toDraft(): Draft {
        val chosenRewrite = when (tone) {
            "soft" -> softRewrite
            "calm" -> calmRewrite
            else -> clearRewrite ?: softRewrite ?: calmRewrite
        }

        return Draft(
            id = id,
            originalMessage = original.orEmpty(),
            rewrittenMessage = chosenRewrite.orEmpty(),
            recipient = null,
            tone = tone,
            createdAt = parseTimestamp(createdAt)
        )
    }

    private fun parseTimestamp(value: String?): Long {
        if (value.isNullOrBlank()) return 0L

        return try {
            LocalDateTime.parse(value)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }

    private inline fun updateState(transform: DraftsUiState.() -> DraftsUiState) {
        _uiState.value = _uiState.value.transform()
    }
}