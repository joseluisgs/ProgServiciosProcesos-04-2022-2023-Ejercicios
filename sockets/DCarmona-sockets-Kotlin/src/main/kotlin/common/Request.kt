package common

import kotlinx.serialization.Serializable

@Serializable
data class Request<T>(
    val content: T?,
    val token: String?,
    val type: Type
){
    enum class Type{
        LOGIN,TIME
    }
}
