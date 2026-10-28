package io.github.gelassen.wordinmemory.model

/**
 * Simple UI model for a chat bubble.
 * role: "user" | "assistant" | "system"
 */
data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val role: String,
    val content: String,
    val isError: Boolean = false
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_SYSTEM = "system"
    }
}
