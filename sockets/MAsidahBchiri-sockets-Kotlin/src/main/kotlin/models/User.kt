package models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    var name: String,
    var password: ByteArray,
    var role: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (name != other.name) return false
        if (!password.contentEquals(other.password)) return false
        if (role != other.role) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + role.hashCode()
        return result
    }
}

@Serializable
data class Login(
    var name: String, var password: String
)
