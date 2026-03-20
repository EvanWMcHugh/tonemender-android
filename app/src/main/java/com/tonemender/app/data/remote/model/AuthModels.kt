package com.tonemender.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class SignInRequest(
    val email: String,
    val password: String,
    val integrityToken: String? = null,
    val integrityRequestHash: String? = null,
    val deviceName: String? = null
)

data class SignUpRequest(
    val email: String,
    val password: String,
    val integrityToken: String? = null,
    val integrityRequestHash: String? = null
)

data class ForgotPasswordRequest(
    val email: String,
    val integrityToken: String? = null,
    val integrityRequestHash: String? = null
)

data class ResendEmailVerificationRequest(
    val email: String,
    val integrityToken: String? = null,
    val integrityRequestHash: String? = null
)

data class ChangeEmailRequest(
    val newEmail: String,
    val integrityToken: String? = null,
    val integrityRequestHash: String? = null
)

data class DeleteAccountRequest(
    val integrityToken: String? = null,
    val integrityRequestHash: String? = null
)

data class AuthUserDto(
    val id: String,
    val email: String,
    @SerializedName(value = "is_pro", alternate = ["isPro"])
    val isPro: Boolean? = null,
    @SerializedName(value = "plan_type", alternate = ["planType"])
    val planType: String? = null
)

data class MeResponse(
    val user: AuthUserDto? = null
)

data class GenericMessageResponse(
    val error: String? = null,
    val message: String? = null
)

data class GooglePlayVerifyRequest(
    val purchaseToken: String,
    val productId: String,
    val basePlanId: String
)