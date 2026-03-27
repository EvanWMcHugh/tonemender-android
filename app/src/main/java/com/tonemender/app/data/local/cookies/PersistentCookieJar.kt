package com.tonemender.app.data.local.cookies

import android.content.Context
import androidx.core.content.edit
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class PersistentCookieJar(context: Context) : CookieJar {

    companion object {
        private const val PREFS_NAME = "tm_cookies"
        private const val FIELD_SEPARATOR = "|"
        private const val MIN_SERIALIZED_PARTS = 9
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Key format: name|domain|path
    private val cookieStore = mutableMapOf<String, Cookie>()

    init {
        loadPersistedCookies()
        removeExpiredCookies()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        var changed = false

        cookies.forEach { cookie ->
            val key = cookieKey(cookie)

            if (cookie.expiresAt < now) {
                changed = changed || cookieStore.remove(key) != null
            } else {
                cookieStore[key] = cookie
                changed = true
            }
        }

        if (changed) {
            persistCookies()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        removeExpiredCookies()
        return cookieStore.values.filter { it.matches(url) }
    }

    fun clear() {
        cookieStore.clear()
        prefs.edit { clear() }
    }

    private fun removeExpiredCookies() {
        val now = System.currentTimeMillis()
        val iterator = cookieStore.iterator()
        var changed = false

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.expiresAt < now) {
                iterator.remove()
                changed = true
            }
        }

        if (changed) {
            persistCookies()
        }
    }

    private fun cookieKey(cookie: Cookie): String {
        return "${cookie.name}$FIELD_SEPARATOR${cookie.domain}$FIELD_SEPARATOR${cookie.path}"
    }

    private fun persistCookies() {
        prefs.edit {
            clear()

            cookieStore.forEach { (key, cookie) ->
                putString(key, serializeCookie(cookie))
            }
        }
    }

    private fun serializeCookie(cookie: Cookie): String {
        return listOf(
            cookie.name,
            cookie.value,
            cookie.expiresAt.toString(),
            cookie.domain,
            cookie.path,
            cookie.secure.toString(),
            cookie.httpOnly.toString(),
            cookie.hostOnly.toString(),
            cookie.persistent.toString()
        ).joinToString(FIELD_SEPARATOR)
    }

    private fun loadPersistedCookies() {
        prefs.all.forEach { (key, value) ->
            val serialized = value as? String ?: return@forEach
            val cookie = deserializeCookie(serialized) ?: return@forEach
            cookieStore[key] = cookie
        }
    }

    private fun deserializeCookie(serialized: String): Cookie? {
        val parts = serialized.split(FIELD_SEPARATOR)
        if (parts.size < MIN_SERIALIZED_PARTS) return null

        return try {
            val name = parts[0]
            val value = parts[1]
            val expiresAt = parts[2].toLong()
            val domain = parts[3]
            val path = parts[4]
            val secure = parts[5].toBoolean()
            val httpOnly = parts[6].toBoolean()
            val hostOnly = parts[7].toBoolean()

            val builder = Cookie.Builder()
                .name(name)
                .value(value)
                .expiresAt(expiresAt)
                .path(path)

            if (hostOnly) {
                builder.hostOnlyDomain(domain)
            } else {
                builder.domain(domain)
            }

            if (secure) builder.secure()
            if (httpOnly) builder.httpOnly()

            builder.build()
        } catch (_: Exception) {
            null
        }
    }
}