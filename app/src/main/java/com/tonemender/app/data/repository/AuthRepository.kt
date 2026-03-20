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
        return NetworkModule.api.signIn(
            SignInRequest(
                email = email,
                password = password,
                integrityToken = integrityToken,
                integrityRequestHash = integrityRequestHash,
                deviceName = deviceName
            )
        )
    }

    suspend fun signUp(
        email: String,
        password: String,
        integrityToken: String? = null,
        integrityRequestHash: String? = null
    ): Response<GenericMessageResponse> {
        return NetworkModule.api.signUp(
            SignUpRequest(
                email = email,
                password = password,
                integrityToken = integrityToken,
                integrityRequestHash = integrityRequestHash
            )
        )
    }

    suspend fun forgotPassword(
        email: String,
        integrityToken: String? = null,
        integrityRequestHash: String? = null
    ): Response<GenericMessageResponse> {
        return NetworkModule.api.forgotPassword(
            ForgotPasswordRequest(
                email = email,
                integrityToken = integrityToken,
                integrityRequestHash = integrityRequestHash
            )
        )
    }

    suspend fun resendEmailVerification(
        email: String,
        integrityToken: String? = null,
        integrityRequestHash: String? = null
    ): Response<GenericMessageResponse> {
        return NetworkModule.api.resendEmailVerification(
            ResendEmailVerificationRequest(
                email = email,
                integrityToken = integrityToken,
                integrityRequestHash = integrityRequestHash
            )
        )
    }

    suspend fun changeEmail(
        newEmail: String,
        integrityToken: String? = null,
        integrityRequestHash: String? = null
    ): Response<GenericMessageResponse> {
        return NetworkModule.api.changeEmail(
            ChangeEmailRequest(
                newEmail = newEmail,
                integrityToken = integrityToken,
                integrityRequestHash = integrityRequestHash
            )
        )
    }

    suspend fun deleteAccount(
        integrityToken: String? = null,
        integrityRequestHash: String? = null
    ): Response<GenericMessageResponse> {
        return NetworkModule.api.deleteAccount(
            DeleteAccountRequest(
                integrityToken = integrityToken,
                integrityRequestHash = integrityRequestHash
            )
        )
    }

    suspend fun verifyGooglePlayPurchase(
        purchaseToken: String,
        productId: String,
        basePlanId: String
    ): Response<GenericMessageResponse> {
        return NetworkModule.api.verifyGooglePlayPurchase(
            GooglePlayVerifyRequest(
                purchaseToken = purchaseToken,
                productId = productId,
                basePlanId = basePlanId
            )
        )
    }

    suspend fun me() = NetworkModule.api.me()

    suspend fun signOut() = NetworkModule.api.signOut()

    fun clearSession() {
        NetworkModule.clearSessionCookies()
    }
}