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

    /* ---------- Load ---------- */

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
                            errorMessage = null
                        )
                    }
                } else {
                    updateState {
                        copy(
                            errorMessage = ApiErrorParser.parseMessage(response)
                                ?: "Failed to load drafts."
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(errorMessage = e.message ?: "Failed to load drafts.")
                }
            }
        }
    }

    fun openDraft(draft: Draft, onOpened: (Draft) -> Unit) {
        onOpened(draft)
    }

    /* ---------- Delete single ---------- */

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
                            pendingDeleteDraft = null,
                            errorMessage = null
                        )
                    }

                    UiMessageManager.showMessage("Draft deleted.")
                } else {
                    updateState {
                        copy(
                            pendingDeleteDraft = null,
                            errorMessage = ApiErrorParser.parseMessage(response)
                                ?: "Failed to delete draft."
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        pendingDeleteDraft = null,
                        errorMessage = e.message ?: "Failed to delete draft."
                    )
                }
            }
        }
    }

    /* ---------- Clear all ---------- */

    fun requestClearAllDrafts() {
        updateState { copy(showClearAllDialog = true) }
    }

    fun cancelClearAllDrafts() {
        updateState { copy(showClearAllDialog = false) }
    }

    fun confirmClearAllDrafts() {
        val drafts = _uiState.value.drafts

        viewModelScope.launch {
            var hadError = false

            drafts.forEach { draft ->
                try {
                    val response = rewriteRepository.deleteDraft(draft.id)
                    if (!response.isSuccessful) hadError = true
                } catch (_: Exception) {
                    hadError = true
                }
            }

            updateState {
                copy(
                    drafts = if (hadError) drafts else emptyList(),
                    pendingDeleteDraft = null,
                    showClearAllDialog = false,
                    errorMessage = if (hadError) "Some drafts could not be deleted." else null
                )
            }

            UiMessageManager.showMessage(
                if (hadError) "Some drafts could not be deleted." else "All drafts cleared."
            )

            if (hadError) {
                loadDrafts()
            }
        }
    }

    /* ---------- Mapping ---------- */

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

    /* ---------- Helpers ---------- */

    private inline fun updateState(transform: DraftsUiState.() -> DraftsUiState) {
        _uiState.value = _uiState.value.transform()
    }
}