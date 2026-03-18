package com.tonemender.app.data.local.cookies

import android.content.Context
import androidx.core.content.edit
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class PersistentCookieJar(context: Context) : CookieJar {

    private val prefs = context.getSharedPreferences("tm_cookies", Context.MODE_PRIVATE)

    // Key format: name|domain|path
    private val cookieStore = mutableMapOf<String, Cookie>()

    init {
        loadPersistedCookies()
        removeExpiredCookies()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()

        cookies.forEach { cookie ->
            val key = cookieKey(cookie)

            if (cookie.expiresAt < now) {
                cookieStore.remove(key)
            } else {
                cookieStore[key] = cookie
            }
        }

        persistCookies()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        removeExpiredCookies()

        val validCookies = cookieStore.values.filter { cookie ->
            cookie.matches(url)
        }

        return validCookies
    }

    fun clear() {
        cookieStore.clear()
        prefs.edit { clear() }
    }

    private fun removeExpiredCookies() {
        val now = System.currentTimeMillis()
        val iterator = cookieStore.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.expiresAt < now) {
                iterator.remove()
            }
        }

        persistCookies()
    }

    private fun cookieKey(cookie: Cookie): String {
        return "${cookie.name}|${cookie.domain}|${cookie.path}"
    }

    private fun persistCookies() {
        prefs.edit {
            clear()

            cookieStore.forEach { (key, cookie) ->
                val serialized = listOf(
                    cookie.name,
                    cookie.value,
                    cookie.expiresAt.toString(),
                    cookie.domain,
                    cookie.path,
                    cookie.secure.toString(),
                    cookie.httpOnly.toString(),
                    cookie.hostOnly.toString(),
                    cookie.persistent.toString()
                ).joinToString("|")

                putString(key, serialized)
            }
        }
    }

    private fun loadPersistedCookies() {
        prefs.all.forEach { (key, value) ->
            val serialized = value as? String ?: return@forEach
            val parts = serialized.split("|")
            if (parts.size < 9) return@forEach

            try {
                val name = parts[0]
                val valuePart = parts[1]
                val expiresAt = parts[2].toLong()
                val domain = parts[3]
                val path = parts[4]
                val secure = parts[5].toBoolean()
                val httpOnly = parts[6].toBoolean()
                val hostOnly = parts[7].toBoolean()

                val builder = Cookie.Builder()
                    .name(name)
                    .value(valuePart)
                    .expiresAt(expiresAt)
                    .path(path)

                if (hostOnly) {
                    builder.hostOnlyDomain(domain)
                } else {
                    builder.domain(domain)
                }

                if (secure) builder.secure()
                if (httpOnly) builder.httpOnly()

                val cookie = builder.build()
                cookieStore[key] = cookie
            } catch (_: Exception) {
                // Ignore malformed cookie entries
            }
        }
    }
}