package com.tonemender.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class RewriteRequest(
    val message: String,
    val recipient: String?,
    val tone: String?
)

data class RewriteResponse(
    val soft: String? = null,
    val calm: String? = null,
    val clear: String? = null,

    @SerializedName("tone_score")
    val toneScore: Int? = null,

    @SerializedName("emotion_prediction")
    val emotionalImpact: String? = null,

    @SerializedName("is_pro")
    val isPro: Boolean? = null,

    @SerializedName("plan_type")
    val planType: String? = null,

    val day: String? = null,

    @SerializedName("free_limit")
    val freeLimit: Int? = null,

    @SerializedName("rewrites_today")
    val usageToday: Int? = null,

    @SerializedName("total")
    val usageTotal: Int? = null,

    val error: String? = null,
    val message: String? = null,
    val code: String? = null
)

data class UsageStatsDto(
    val today: Int = 0,
    val total: Int = 0
)

data class UsageStatsResponse(
    val stats: UsageStatsDto = UsageStatsDto(),
    val day: String? = null
)