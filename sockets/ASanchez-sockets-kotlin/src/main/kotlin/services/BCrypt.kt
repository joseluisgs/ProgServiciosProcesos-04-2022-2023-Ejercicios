package services

import com.toxicbakery.bcrypt.Bcrypt

fun cifrar(cadena: String): ByteArray {
    return Bcrypt.hash(cadena, 12)
}

fun comprobar(expected: String, input: ByteArray): Boolean {
    return Bcrypt.verify(expected, input)
}