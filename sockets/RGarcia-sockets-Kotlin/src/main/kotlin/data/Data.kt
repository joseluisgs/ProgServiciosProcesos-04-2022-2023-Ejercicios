package data

import com.toxicbakery.bcrypt.Bcrypt
import models.Usuario
import services.cifraBcrypt

fun rellenarUsuarios(): List<Usuario> {
    return listOf(
        Usuario("ruben", cifraBcrypt("prueba"), "user"),
        Usuario("admin", cifraBcrypt("admin"), "user"),
        Usuario("pepe", cifraBcrypt("pepe1234"), "admin"),
    )
}