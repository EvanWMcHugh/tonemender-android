package com.tonemender.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeleteDraftRequest(
    @SerializedName("draftId")
    val draftId: String
)