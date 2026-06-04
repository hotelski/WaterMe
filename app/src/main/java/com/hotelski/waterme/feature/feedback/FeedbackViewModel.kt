package com.hotelski.waterme.feature.feedback

import android.app.Application
import android.os.Build
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.BuildConfig
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.feedback.FeedbackRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val selectedTopic: FeedbackTopic = FeedbackTopic.Idea,
    val name: String = "",
    val email: String = "",
    val message: String = "",
    val isSending: Boolean = false,
    val sentMessage: String? = null,
    val sendError: String? = null,
) {
    val canSend: Boolean
        get() = message.isNotBlank() && !isSending
}

sealed interface FeedbackEvent {
    data class TopicSelected(val value: FeedbackTopic) : FeedbackEvent
    data class NameChanged(val value: String) : FeedbackEvent
    data class EmailChanged(val value: String) : FeedbackEvent
    data class MessageChanged(val value: String) : FeedbackEvent
    data object SendClicked : FeedbackEvent
}

class FeedbackViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val feedbackRepository = WaterMeAppContainer.feedbackRepository()
    private val _uiState = MutableStateFlow(FeedbackUiState())
    private var messageDismissJob: Job? = null

    val uiState = _uiState.asStateFlow()

    fun onEvent(event: FeedbackEvent) {
        when (event) {
            is FeedbackEvent.TopicSelected -> updateField { copy(selectedTopic = event.value) }
            is FeedbackEvent.NameChanged -> updateField { copy(name = event.value.take(MAX_NAME_LENGTH)) }
            is FeedbackEvent.EmailChanged -> updateField { copy(email = event.value.take(MAX_EMAIL_LENGTH)) }
            is FeedbackEvent.MessageChanged -> updateField { copy(message = event.value.take(MAX_FEEDBACK_LENGTH)) }
            FeedbackEvent.SendClicked -> sendFeedback()
        }
    }

    private fun sendFeedback() {
        val current = _uiState.value
        if (current.isSending) return

        val trimmedMessage = current.message.trim()
        val trimmedEmail = current.email.trim()

        val validationError = when {
            trimmedMessage.isBlank() -> "Add feedback before sending."
            trimmedEmail.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() ->
                "Enter a valid email or leave it blank."
            else -> null
        }
        if (validationError != null) {
            showMessage(sendError = validationError)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, sentMessage = null, sendError = null) }
            runCatching {
                feedbackRepository.sendFeedback(
                    FeedbackRequest(
                        topic = current.selectedTopic.label,
                        name = current.name.trim().takeIf { it.isNotBlank() },
                        email = trimmedEmail.takeIf { it.isNotBlank() },
                        message = trimmedMessage,
                        appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        androidSdk = Build.VERSION.SDK_INT,
                    ),
                )
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            message = "",
                            isSending = false,
                            sentMessage = "Feedback sent. Thank you.",
                            sendError = null,
                        )
                    }
                    dismissTransientMessageLater(
                        sentMessage = "Feedback sent. Thank you.",
                        sendError = null,
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            sentMessage = null,
                            sendError = error.toUserMessage(),
                        )
                    }
                    dismissTransientMessageLater(
                        sentMessage = null,
                        sendError = error.toUserMessage(),
                    )
                }
        }
    }

    private fun updateField(reducer: FeedbackUiState.() -> FeedbackUiState) {
        _uiState.update { it.reducer().copy(sentMessage = null, sendError = null) }
        messageDismissJob?.cancel()
    }

    private fun showMessage(
        sentMessage: String? = null,
        sendError: String? = null,
    ) {
        _uiState.update { it.copy(sentMessage = sentMessage, sendError = sendError) }
        dismissTransientMessageLater(sentMessage = sentMessage, sendError = sendError)
    }

    private fun dismissTransientMessageLater(
        sentMessage: String?,
        sendError: String?,
    ) {
        messageDismissJob?.cancel()
        messageDismissJob = viewModelScope.launch {
            delay(MESSAGE_VISIBLE_MILLIS)
            _uiState.update { state ->
                if (state.sentMessage == sentMessage && state.sendError == sendError) {
                    state.copy(sentMessage = null, sendError = null)
                } else {
                    state
                }
            }
        }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not send feedback. Try again later."

    private companion object {
        const val MAX_NAME_LENGTH = 80
        const val MAX_EMAIL_LENGTH = 120
        const val MAX_FEEDBACK_LENGTH = 900
        const val MESSAGE_VISIBLE_MILLIS = 3_400L
    }
}
