package models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val name: String,
    val password: ByteArray,
    val rol: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (name != other.name) return false
        if (!password.contentEquals(other.password)) return false
        if (rol != other.rol) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + rol.hashCode()
        return result
    }
}
