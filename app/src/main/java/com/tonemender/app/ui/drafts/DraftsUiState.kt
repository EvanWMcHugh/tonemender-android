package com.tonemender.app.ui.drafts

import com.tonemender.app.data.local.drafts.Draft

data class DraftsUiState(
    val drafts: List<Draft> = emptyList(),
    val pendingDeleteDraft: Draft? = null,
    val showClearAllDialog: Boolean = false,
    val loadErrorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = drafts.isEmpty()
}