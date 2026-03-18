package com.tonemender.app.data.security

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class PlayIntegrityManager(
    context: Context
) {
    private val integrityManager = IntegrityManagerFactory.createStandard(context)

    private var standardIntegrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null

    suspend fun prepare(): Result<Unit> {
        return try {
            val request = StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(GOOGLE_CLOUD_PROJECT_NUMBER)
                .build()

            standardIntegrityTokenProvider = integrityManager.prepareIntegrityToken(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            standardIntegrityTokenProvider = null
            Result.failure(e)
        }
    }

    suspend fun requestToken(requestHash: String): Result<String> {
        return try {
            val provider = standardIntegrityTokenProvider
                ?: return Result.failure(IllegalStateException("Integrity provider not prepared."))

            val tokenRequest = StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()

            val token = provider.request(tokenRequest).await().token()
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val GOOGLE_CLOUD_PROJECT_NUMBER: Long = 525812793867

        fun buildRequestHash(vararg parts: String): String {
            val raw = parts.joinToString(separator = "|") { it.trim() }
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}