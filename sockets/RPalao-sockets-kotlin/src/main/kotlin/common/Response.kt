package common
import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(
    var type: TypeResponseEnum,
    var data: T
){}

enum class TypeResponseEnum{
    ERROR, OK
}