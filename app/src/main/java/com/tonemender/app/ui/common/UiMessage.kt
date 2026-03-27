package com.tonemender.app.ui.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object UiMessageManager {

    private const val BUFFER_CAPACITY = 10

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = BUFFER_CAPACITY
    )

    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun showMessage(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        _messages.tryEmit(trimmed)
    }
}