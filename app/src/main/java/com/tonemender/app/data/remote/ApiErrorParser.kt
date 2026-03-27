package com.tonemender.app.data.remote

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.tonemender.app.data.remote.model.GenericMessageResponse
import retrofit2.Response

object ApiErrorParser {

    private val gson = Gson()

    fun parseMessage(response: Response<*>): String? {
        val raw = safeReadErrorBody(response) ?: return null

        return tryParseStructuredError(raw) ?: raw
    }

    /* ---------- Internal helpers ---------- */

    private fun safeReadErrorBody(response: Response<*>): String? {
        return try {
            response.errorBody()?.string()?.trim()
        } catch (_: Exception) {
            null
        }?.takeIf { it.isNotBlank() }
    }

    private fun tryParseStructuredError(raw: String): String? {
        return try {
            val parsed = gson.fromJson(raw, GenericMessageResponse::class.java)
            parsed.error ?: parsed.message
        } catch (_: JsonSyntaxException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}