package repositories

import models.User
import utils.encriptPassword

fun usersRepository() = listOf<User>(
    User("pepe", encriptPassword("pepe1234"), "admin"),
    User("pepa", encriptPassword("pepa1234"), "admin"),
    User("pepito", encriptPassword("pepito1234"), "user"),
    User("pepita", encriptPassword("pepita1234"), "user")



    )