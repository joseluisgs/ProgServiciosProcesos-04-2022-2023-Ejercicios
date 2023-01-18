package repositories

import data.rellenarUsuarios
import models.Usuario

class UserRepositoryImpl {
    var lista: MutableList<Usuario> = mutableListOf()

    init{
        lista.addAll(rellenarUsuarios())
    }
}