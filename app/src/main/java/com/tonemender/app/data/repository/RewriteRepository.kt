package com.tonemender.app.data.repository

import com.tonemender.app.data.remote.NetworkModule
import com.tonemender.app.data.remote.model.CreateDraftRequest
import com.tonemender.app.data.remote.model.DeleteDraftRequest
import com.tonemender.app.data.remote.model.DraftResponse
import com.tonemender.app.data.remote.model.DraftsResponse
import com.tonemender.app.data.remote.model.GenericMessageResponse
import com.tonemender.app.data.remote.model.RewriteRequest
import com.tonemender.app.data.remote.model.RewriteResponse
import com.tonemender.app.data.remote.model.UsageResponse
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

    suspend fun getUsage(): Response<UsageResponse> {
        return NetworkModule.api.getUsage()
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
        tone: String? = null
    ): Response<DraftResponse> {
        val request = CreateDraftRequest(
            original = originalMessage,
            tone = tone,
            softRewrite = if (tone == "soft") rewrittenMessage else null,
            calmRewrite = if (tone == "calm") rewrittenMessage else null,
            clearRewrite = if (tone == "clear" || tone == null) rewrittenMessage else null
        )

        return NetworkModule.api.createDraft(request)
    }

    suspend fun deleteDraft(draftId: String): Response<GenericMessageResponse> {
        return NetworkModule.api.deleteDraft(
            DeleteDraftRequest(draftId = draftId)
        )
    }

    suspend fun deleteAllDrafts(): Response<GenericMessageResponse> {
        return NetworkModule.api.deleteAllDrafts()
    }
}