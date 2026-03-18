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

    @POST("api/auth/sign-in")
    suspend fun signIn(
        @Body body: SignInRequest
    ): Response<MeResponse>

    @POST("api/auth/sign-up")
    suspend fun signUp(
        @Body body: SignUpRequest
    ): Response<GenericMessageResponse>

    @POST("api/auth/request-password-reset")
    suspend fun forgotPassword(
        @Body body: ForgotPasswordRequest
    ): Response<GenericMessageResponse>

    @POST("api/auth/request-email-change")
    suspend fun changeEmail(
        @Body body: ChangeEmailRequest
    ): Response<GenericMessageResponse>

    @POST("api/user/delete-account")
    suspend fun deleteAccount(
        @Body body: DeleteAccountRequest
    ): Response<GenericMessageResponse>

    @GET("api/user/me")
    suspend fun me(): Response<MeResponse>

    @POST("api/auth/sign-out")
    suspend fun signOut(): Response<GenericMessageResponse>

    @POST("api/rewrite")
    suspend fun rewrite(
        @Body body: RewriteRequest
    ): Response<RewriteResponse>

    @GET("api/usage/stats")
    suspend fun getUsageStats(): Response<UsageStatsResponse>

    @GET("api/messages")
    suspend fun getDrafts(): Response<DraftsResponse>

    @POST("api/messages")
    suspend fun createDraft(
        @Body body: CreateDraftRequest
    ): Response<DraftResponse>

    @PUT("api/messages/{draftId}")
    suspend fun updateDraft(
        @Path("draftId") draftId: String,
        @Body body: UpdateDraftRequest
    ): Response<DraftResponse>

    @DELETE("api/messages/{draftId}")
    suspend fun deleteDraft(
        @Path("draftId") draftId: String
    ): Response<GenericMessageResponse>

    @POST("api/billing/google/verify")
    suspend fun verifyGooglePlayPurchase(
        @Body body: GooglePlayVerifyRequest
    ): Response<GenericMessageResponse>
}