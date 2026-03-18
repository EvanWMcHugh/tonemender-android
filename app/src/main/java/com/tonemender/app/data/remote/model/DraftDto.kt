package com.tonemender.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class DraftDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("original")
    val original: String? = null,

    @SerializedName("tone")
    val tone: String? = null,

    @SerializedName("soft_rewrite")
    val softRewrite: String? = null,

    @SerializedName("calm_rewrite")
    val calmRewrite: String? = null,

    @SerializedName("clear_rewrite")
    val clearRewrite: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)