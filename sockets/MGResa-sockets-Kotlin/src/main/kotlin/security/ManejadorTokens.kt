package security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.*

object ManejadorTokens {

    private val properties = FicheroProperties.loadProperties()

    // Algoritmo
    private val algoritmo: Algorithm = Algorithm.HMAC256(properties.getProperty("clave.algoritmo"))

    // Creamos el token, se pide que el token expire en 60 segundos.
    // Para filtrar el tipo de usuario, vamos a crear un CLaim que sea el rol, se pide por parametros.
    fun createToken(rol: String): String {
        val jwtToken: String = JWT.create()
            .withClaim("rol", rol)
            .withExpiresAt(Date(System.currentTimeMillis() + 60000)) // 1 minuto
            .sign(algoritmo)

        //println(jwtToken)
        return jwtToken
    }

    // Funcion que tratara de verificar el token, si no, devuelve un null.
    fun decodeToken(jwtToken: String): DecodedJWT? {
        val verifier = JWT.require(algoritmo)
            .build()

        val token: DecodedJWT? = try {
            verifier.verify(jwtToken)
        } catch (_: TokenExpiredException) {
            null
        }

        return token
    }
}
