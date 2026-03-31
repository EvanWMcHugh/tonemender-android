package com.tonemender.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class RewriteRequest(
    @SerializedName("message")
    val message: String,
    @SerializedName("recipient")
    val recipient: String?,
    @SerializedName("tone")
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
    @SerializedName("today")
    val today: Int = 0,
    @SerializedName("total")
    val total: Int = 0
)

data class UsageStatsResponse(
    @SerializedName("stats")
    val stats: UsageStatsDto = UsageStatsDto(),
    @SerializedName("day")
    val day: String? = null
)