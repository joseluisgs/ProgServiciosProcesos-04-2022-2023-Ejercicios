package services

import com.toxicbakery.bcrypt.Bcrypt


fun cifraBcrypt(password: String): ByteArray {
    return Bcrypt.hash(password, 12)
}

fun comprobar(password: String, hash: ByteArray): Boolean {
    return Bcrypt.verify(password, hash)
}