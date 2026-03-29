package com.tonemender.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class DraftsResponse(
    @SerializedName("ok")
    val ok: Boolean? = null,

    @SerializedName("drafts")
    val drafts: List<DraftDto> = emptyList()
)