package com.tonemender.app.data.local.drafts

data class Draft(
    val id: String,
    val originalMessage: String,
    val rewrittenMessage: String,
    val recipient: String?,
    val tone: String?,
    val createdAt: Long
)