package com.tonemender.app.data.remote.api

import com.tonemender.app.data.remote.model.ChangeEmailRequest
import com.tonemender.app.data.remote.model.CreateDraftRequest
import com.tonemender.app.data.remote.model.DeleteAccountRequest
import com.tonemender.app.data.remote.model.DraftResponse
import com.tonemender.app.data.remote.model.DraftsResponse
import com.tonemender.app.data.remote.model.ForgotPasswordRequest
import com.tonemender.app.data.remote.model.GenericMessageResponse
import com.tonemender.app.data.remote.model.GooglePlayVerifyRequest
import com.tonemender.app.data.remote.model.MeResponse
import com.tonemender.app.data.remote.model.ResendEmailVerificationRequest
import com.tonemender.app.data.remote.model.RewriteRequest
import com.tonemender.app.data.remote.model.RewriteResponse
import com.tonemender.app.data.remote.model.SignInRequest
import com.tonemender.app.data.remote.model.SignUpRequest
import com.tonemender.app.data.remote.model.UpdateDraftRequest
import com.tonemender.app.data.remote.model.UsageStatsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ToneMenderApi {

    // Auth
    @POST("api/auth/sign-in")
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<MeResponse>

    @POST("api/auth/sign-up")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response<GenericMessageResponse>

    @POST("api/auth/request-password-reset")
    suspend fun requestPasswordReset(
        @Body request: ForgotPasswordRequest
    ): Response<GenericMessageResponse>

    @POST("api/auth/resend-email-verification")
    suspend fun resendEmailVerification(
        @Body request: ResendEmailVerificationRequest
    ): Response<GenericMessageResponse>

    @POST("api/auth/request-email-change")
    suspend fun requestEmailChange(
        @Body request: ChangeEmailRequest
    ): Response<GenericMessageResponse>

    @POST("api/auth/sign-out")
    suspend fun signOut(): Response<GenericMessageResponse>

    // User
    @GET("api/user/me")
    suspend fun getMe(): Response<MeResponse>

    @POST("api/user/delete-account")
    suspend fun deleteAccount(
        @Body request: DeleteAccountRequest
    ): Response<GenericMessageResponse>

    // Rewrite
    @POST("api/rewrite")
    suspend fun rewrite(
        @Body request: RewriteRequest
    ): Response<RewriteResponse>

    @GET("api/usage/stats")
    suspend fun getUsageStats(): Response<UsageStatsResponse>

    // Drafts / Messages
    @GET("api/messages")
    suspend fun getDrafts(): Response<DraftsResponse>

    @POST("api/messages")
    suspend fun createDraft(
        @Body request: CreateDraftRequest
    ): Response<DraftResponse>

    @PUT("api/messages/{draftId}")
    suspend fun updateDraft(
        @Path("draftId") draftId: String,
        @Body request: UpdateDraftRequest
    ): Response<DraftResponse>

    @DELETE("api/messages/{draftId}")
    suspend fun deleteDraft(
        @Path("draftId") draftId: String
    ): Response<GenericMessageResponse>

    // Billing
    @POST("api/billing/google/verify")
    suspend fun verifyGooglePlayPurchase(
        @Body request: GooglePlayVerifyRequest
    ): Response<GenericMessageResponse>
}