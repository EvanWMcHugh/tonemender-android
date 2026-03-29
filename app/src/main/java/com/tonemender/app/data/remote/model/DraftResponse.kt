package com.tonemender.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class DraftResponse(
    @SerializedName("ok")
    val ok: Boolean? = null,

    @SerializedName("draft")
    val draft: DraftDto? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: String? = null
)