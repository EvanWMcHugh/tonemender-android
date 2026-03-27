package com.tonemender.app.data.repository

import com.tonemender.app.data.remote.NetworkModule
import com.tonemender.app.data.remote.model.ChangeEmailRequest
import com.tonemender.app.data.remote.model.DeleteAccountRequest
import com.tonemender.app.data.remote.model.ForgotPasswordRequest
import com.tonemender.app.data.remote.model.GenericMessageResponse
import com.tonemender.app.data.remote.model.GooglePlayVerifyRequest
import com.tonemender.app.data.remote.model.MeResponse
import com.tonemender.app.data.remote.model.ResendEmailVerificationRequest
import com.tonemender.app.data.remote.model.SignInRequest
import com.tonemender.app.data.remote.model.SignUpRequest
import retrofit2.Response

class AuthRepository {

    suspend fun signIn(
        email: String,
        password: String,
        integrityToken: String? = null,
        integrityRequestHash: String? = null,
        deviceName: String? = null
    ): Response<MeResponse> {
        val request = SignInRequest(
            email = email,
            password = password,
            integrityToken = integrityToken,
            integrityRequestHash = integrityRequestHash,
            deviceName = deviceName
        )

        return NetworkModule.api.signIn(request)
    }

    suspend fun signUp(
        email: String,
        password: String,
        integrityToken: String? = null,
        integrityRequestHash: String? = null
    ): Response<GenericMessageResponse> {
        val request = SignUpRequest(
            email = email,
            password = password,
            integrityToken = integrityToken,
            integrityRequestHash = integrityRequestHash
        )

        return NetworkModule.api.signUp(request)
    }

    suspend fun requestPasswordReset(
        email: String,
        integrityToken: String? = null,
        integrityRequestHash: String? = null
    ): Response<GenericMessageResponse> {
        val request = ForgotPasswordRequest(
            email = email,
            integrityToken = integrityToken,
            integrityRequestHash = integrityRequestHash
        )

        return NetworkModule.api.requestPasswordReset(request)
    }

    suspend fun resendEmailVerification(
        email: String,
        integrityToken: String? = null,
        integrityRequestHash: String? = null
    ): Response<GenericMessageResponse> {
        val request = ResendEmailVerificationRequest(
            email = email,
            integrityToken = integrityToken,
            integrityRequestHash = integrityRequestHash
        )

        return NetworkModule.api.resendEmailVerification(request)
    }

    suspend fun requestEmailChange(
        newEmail: String,
        integrityToken: String? = null,
        integrityRequestHash: String? = null
    ): Response<GenericMessageResponse> {
        val request = ChangeEmailRequest(
            newEmail = newEmail,
            integrityToken = integrityToken,
            integrityRequestHash = integrityRequestHash
        )

        return NetworkModule.api.requestEmailChange(request)
    }

    suspend fun deleteAccount(
        integrityToken: String? = null,
        integrityRequestHash: String? = null
    ): Response<GenericMessageResponse> {
        val request = DeleteAccountRequest(
            integrityToken = integrityToken,
            integrityRequestHash = integrityRequestHash
        )

        return NetworkModule.api.deleteAccount(request)
    }

    suspend fun verifyGooglePlayPurchase(
        purchaseToken: String,
        productId: String,
        basePlanId: String
    ): Response<GenericMessageResponse> {
        val request = GooglePlayVerifyRequest(
            purchaseToken = purchaseToken,
            productId = productId,
            basePlanId = basePlanId
        )

        return NetworkModule.api.verifyGooglePlayPurchase(request)
    }

    suspend fun getMe(): Response<MeResponse> {
        return NetworkModule.api.getMe()
    }

    suspend fun signOut(): Response<GenericMessageResponse> {
        return NetworkModule.api.signOut()
    }

    fun clearSession() {
        NetworkModule.clearSessionCookies()
    }
}