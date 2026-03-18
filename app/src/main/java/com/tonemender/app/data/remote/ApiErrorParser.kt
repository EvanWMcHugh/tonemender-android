package com.tonemender.app.data.remote

import com.google.gson.Gson
import com.tonemender.app.data.remote.model.GenericMessageResponse
import retrofit2.Response

object ApiErrorParser {
    private val gson = Gson()

    fun parseMessage(response: Response<*>): String? {
        return try {
            val raw = response.errorBody()?.string()?.trim()
            if (raw.isNullOrBlank()) return null

            val parsed = gson.fromJson(raw, GenericMessageResponse::class.java)
            parsed.error ?: parsed.message ?: raw
        } catch (_: Exception) {
            null
        }
    }
}