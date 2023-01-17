package model

import kotlinx.serialization.Serializable

@Serializable
data class LoginUser(
    val name: String,
    val password: String
)
