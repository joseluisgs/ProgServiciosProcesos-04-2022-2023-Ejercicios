package common

import kotlinx.serialization.Serializable

@Serializable
data class Request<T>(
    var type: TypeRequestEnum,
    var data: T,
)

enum class TypeRequestEnum{
    LOGIN, TIME
}
