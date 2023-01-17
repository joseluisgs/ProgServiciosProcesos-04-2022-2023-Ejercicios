package repositories

import at.favre.lib.crypto.bcrypt.BCrypt
import model.User
import java.nio.charset.StandardCharsets

class UsersRepository {
    private val ROUNDS = 12
    var list: MutableList<User> = mutableListOf()

    init {
        list.addAll(listOf(
            User("pepe",BCrypt.withDefaults().hash(ROUNDS, "pepe1234".toByteArray(StandardCharsets.UTF_8)), "admin"),
            User("prueba",BCrypt.withDefaults().hash(ROUNDS, "prueba".toByteArray(StandardCharsets.UTF_8)), "user"),
            User("ana",BCrypt.withDefaults().hash(ROUNDS, "anaaaa".toByteArray(StandardCharsets.UTF_8)), "user"),
            User("carlos",BCrypt.withDefaults().hash(ROUNDS, "carlos123".toByteArray(StandardCharsets.UTF_8)), "admin")
        ))
    }
}