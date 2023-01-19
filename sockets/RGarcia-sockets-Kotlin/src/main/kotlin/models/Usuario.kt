package models

import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val username: String,
    val password: ByteArray,
    val rol: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Usuario

        if (username != other.username) return false
        if (!password.contentEquals(other.password)) return false
        if (rol != other.rol) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + rol.hashCode()
        return result
    }
}