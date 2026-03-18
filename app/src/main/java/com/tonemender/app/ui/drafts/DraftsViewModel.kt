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

                    _uiState.value = _uiState.value.copy(
                        drafts = drafts,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = ApiErrorParser.parseMessage(response) ?: "Failed to load drafts."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load drafts."
                )
            }
        }
    }

    fun openDraft(draft: Draft, onOpened: (Draft) -> Unit) {
        onOpened(draft)
    }

    fun requestDeleteDraft(draft: Draft) {
        _uiState.value = _uiState.value.copy(
            pendingDeleteDraft = draft
        )
    }

    fun cancelDeleteDraft() {
        _uiState.value = _uiState.value.copy(
            pendingDeleteDraft = null
        )
    }

    fun confirmDeleteDraft() {
        val draft = _uiState.value.pendingDeleteDraft ?: return

        viewModelScope.launch {
            try {
                val response = rewriteRepository.deleteDraft(draft.id)

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        drafts = _uiState.value.drafts.filterNot { it.id == draft.id },
                        pendingDeleteDraft = null,
                        errorMessage = null
                    )
                    UiMessageManager.showMessage("Draft deleted.")
                } else {
                    _uiState.value = _uiState.value.copy(
                        pendingDeleteDraft = null,
                        errorMessage = ApiErrorParser.parseMessage(response) ?: "Failed to delete draft."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    pendingDeleteDraft = null,
                    errorMessage = e.message ?: "Failed to delete draft."
                )
            }
        }
    }

    fun requestClearAllDrafts() {
        _uiState.value = _uiState.value.copy(
            showClearAllDialog = true
        )
    }

    fun cancelClearAllDrafts() {
        _uiState.value = _uiState.value.copy(
            showClearAllDialog = false
        )
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

            _uiState.value = _uiState.value.copy(
                drafts = if (hadError) _uiState.value.drafts else emptyList(),
                pendingDeleteDraft = null,
                showClearAllDialog = false,
                errorMessage = if (hadError) "Some drafts could not be deleted." else null
            )

            UiMessageManager.showMessage(
                if (hadError) "Some drafts could not be deleted." else "All drafts cleared."
            )

            if (hadError) {
                loadDrafts()
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
            LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }
}