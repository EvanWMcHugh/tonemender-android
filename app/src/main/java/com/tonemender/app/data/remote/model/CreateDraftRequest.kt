package com.tonemender.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class CreateDraftRequest(
    @SerializedName("message")
    val message: String,

    @SerializedName("rewritten_message")
    val rewrittenMessage: String,

    @SerializedName("recipient")
    val recipient: String? = null,

    @SerializedName("tone")
    val tone: String? = null
)