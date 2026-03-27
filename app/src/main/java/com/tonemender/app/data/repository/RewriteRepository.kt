package com.tonemender.app.data.repository

import com.tonemender.app.data.remote.NetworkModule
import com.tonemender.app.data.remote.model.CreateDraftRequest
import com.tonemender.app.data.remote.model.DraftResponse
import com.tonemender.app.data.remote.model.DraftsResponse
import com.tonemender.app.data.remote.model.GenericMessageResponse
import com.tonemender.app.data.remote.model.RewriteRequest
import com.tonemender.app.data.remote.model.RewriteResponse
import com.tonemender.app.data.remote.model.UpdateDraftRequest
import com.tonemender.app.data.remote.model.UsageStatsResponse
import retrofit2.Response

class RewriteRepository {

    suspend fun rewrite(
        message: String,
        recipient: String? = null,
        tone: String? = null
    ): Response<RewriteResponse> {
        val request = RewriteRequest(
            message = message,
            recipient = recipient,
            tone = tone
        )

        return NetworkModule.api.rewrite(request)
    }

    suspend fun getUsageStats(): Response<UsageStatsResponse> {
        return NetworkModule.api.getUsageStats()
    }

    suspend fun getDrafts(): Response<DraftsResponse> {
        return NetworkModule.api.getDrafts()
    }

    suspend fun createDraft(
        originalMessage: String,
        rewrittenMessage: String,
        recipient: String? = null,
        tone: String? = null
    ): Response<DraftResponse> {
        val request = CreateDraftRequest(
            message = originalMessage,
            rewrittenMessage = rewrittenMessage,
            recipient = recipient,
            tone = tone
        )

        return NetworkModule.api.createDraft(request)
    }

    suspend fun updateDraft(
        draftId: String,
        originalMessage: String,
        rewrittenMessage: String,
        recipient: String? = null,
        tone: String? = null
    ): Response<DraftResponse> {
        val request = UpdateDraftRequest(
            message = originalMessage,
            rewrittenMessage = rewrittenMessage,
            recipient = recipient,
            tone = tone
        )

        return NetworkModule.api.updateDraft(
            draftId = draftId,
            request = request
        )
    }

    suspend fun deleteDraft(draftId: String): Response<GenericMessageResponse> {
        return NetworkModule.api.deleteDraft(draftId)
    }
}