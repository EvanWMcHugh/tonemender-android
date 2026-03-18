package com.tonemender.app.data.repository

import com.tonemender.app.data.remote.NetworkModule
import com.tonemender.app.data.remote.model.CreateDraftRequest
import com.tonemender.app.data.remote.model.DraftResponse
import com.tonemender.app.data.remote.model.DraftsResponse
import com.tonemender.app.data.remote.model.RewriteRequest
import com.tonemender.app.data.remote.model.RewriteResponse
import com.tonemender.app.data.remote.model.UpdateDraftRequest
import com.tonemender.app.data.remote.model.UsageStatsResponse
import retrofit2.Response

class RewriteRepository {

    suspend fun rewrite(
        message: String,
        recipient: String?,
        tone: String?
    ): Response<RewriteResponse> {
        return NetworkModule.api.rewrite(
            RewriteRequest(
                message = message,
                recipient = recipient,
                tone = tone
            )
        )
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
        recipient: String?,
        tone: String?
    ): Response<DraftResponse> {
        return NetworkModule.api.createDraft(
            CreateDraftRequest(
                message = originalMessage,
                rewrittenMessage = rewrittenMessage,
                recipient = recipient,
                tone = tone
            )
        )
    }

    suspend fun updateDraft(
        draftId: String,
        originalMessage: String,
        rewrittenMessage: String,
        recipient: String?,
        tone: String?
    ): Response<DraftResponse> {
        return NetworkModule.api.updateDraft(
            draftId = draftId,
            body = UpdateDraftRequest(
                message = originalMessage,
                rewrittenMessage = rewrittenMessage,
                recipient = recipient,
                tone = tone
            )
        )
    }

    suspend fun deleteDraft(draftId: String) = NetworkModule.api.deleteDraft(draftId)
}