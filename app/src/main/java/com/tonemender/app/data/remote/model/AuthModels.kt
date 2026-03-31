package com.tonemender.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class SignInRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("integrityToken")
    val integrityToken: String? = null,
    @SerializedName("integrityRequestHash")
    val integrityRequestHash: String? = null,
    @SerializedName("deviceName")
    val deviceName: String? = null
)

data class SignUpRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("integrityToken")
    val integrityToken: String? = null,
    @SerializedName("integrityRequestHash")
    val integrityRequestHash: String? = null
)

data class ForgotPasswordRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("integrityToken")
    val integrityToken: String? = null,
    @SerializedName("integrityRequestHash")
    val integrityRequestHash: String? = null
)

data class ResendEmailVerificationRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("integrityToken")
    val integrityToken: String? = null,
    @SerializedName("integrityRequestHash")
    val integrityRequestHash: String? = null
)

data class ChangeEmailRequest(
    @SerializedName("newEmail")
    val newEmail: String,
    @SerializedName("integrityToken")
    val integrityToken: String? = null,
    @SerializedName("integrityRequestHash")
    val integrityRequestHash: String? = null
)

data class DeleteAccountRequest(
    @SerializedName("turnstileToken")
    val turnstileToken: String? = null,
    @SerializedName("integrityToken")
    val integrityToken: String? = null,
    @SerializedName("integrityRequestHash")
    val integrityRequestHash: String? = null
)

data class AuthUserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName(value = "is_pro", alternate = ["isPro"])
    val isPro: Boolean? = null,
    @SerializedName(value = "plan_type", alternate = ["planType"])
    val planType: String? = null
)

data class MeResponse(
    @SerializedName("user")
    val user: AuthUserDto? = null,
    @SerializedName("ok")
    val ok: Boolean? = null,
    @SerializedName("error")
    val error: String? = null,
    @SerializedName("message")
    val message: String? = null
)

data class GenericMessageResponse(
    @SerializedName("ok")
    val ok: Boolean? = null,
    @SerializedName("error")
    val error: String? = null,
    @SerializedName("message")
    val message: String? = null
)

data class GooglePlayVerifyRequest(
    @SerializedName("purchaseToken")
    val purchaseToken: String,
    @SerializedName("productId")
    val productId: String,
    @SerializedName("basePlanId")
    val basePlanId: String
)