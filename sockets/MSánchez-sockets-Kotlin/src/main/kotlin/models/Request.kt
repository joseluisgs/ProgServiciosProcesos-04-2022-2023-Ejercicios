package models

import kotlinx.serialization.Serializable

@Serializable
data class Request<T>(
    val content: T?,
    val type: Type,
) {
    enum class Type {
        LOGIN, TIME
    }
}
