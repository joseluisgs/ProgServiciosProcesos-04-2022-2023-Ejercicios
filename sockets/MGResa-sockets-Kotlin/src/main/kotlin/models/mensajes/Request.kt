package models.mensajes

import kotlinx.serialization.Serializable

@Serializable
data class Request<T>(
    val token: String?, // Token para el control de roles y el control de acciones del usuario
    val content: T?, // En este caso podria ser un String simplemente, cuando se pida la hora ambos estaran null
    val content2: String?, // Para el paso de password en el login como String
    val type: Type

) {
    enum class Type {
        LOGIN, CONSULT
    }
}