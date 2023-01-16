package security

import com.toxicbakery.bcrypt.Bcrypt

object Cifrador {

    // Funcion rapida que recibe un string y lo encripta, yo he elegido 16 saltos.
    // Para verificar la contraseña, existe una funcion de la propia libreria que se usara en la SC
    fun codifyPassword(password: String): ByteArray {
        return Bcrypt.hash(password, 16)
    }
}