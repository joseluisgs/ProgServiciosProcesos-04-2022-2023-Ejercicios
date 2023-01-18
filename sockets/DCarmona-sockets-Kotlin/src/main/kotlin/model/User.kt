package model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userName: String,
    val password: String,
    val rol: String?
)
