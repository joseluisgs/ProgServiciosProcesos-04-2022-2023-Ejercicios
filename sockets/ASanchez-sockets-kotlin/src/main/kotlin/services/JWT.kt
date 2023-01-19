package services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import mu.KotlinLogging
import java.util.*

private var logger = KotlinLogging.logger {}

fun createToken(mensaje: String, issuer: String, subject: String, username: String, role: String): String {
    val algorithm: Algorithm = Algorithm.HMAC256(mensaje)

    val token: String = JWT.create()
        .withIssuer(issuer)
        .withSubject(subject)
        .withClaim("username", username)
        .withClaim("roles", role)
        .withIssuedAt(Date())
        .withExpiresAt(Date(System.currentTimeMillis() + 60000L))
        .withJWTId(UUID.randomUUID().toString())
        .sign(algorithm)

    logger.debug { "Token creado." }
    println(token)

    return token;
}

fun verifyToken(algorithm: Algorithm, token: String): Boolean {
    var isVerified = false
    val verifier = JWT.require(algorithm)
        .build()
    val decodedJWT = verifier.verify(token)

    if (decodedJWT.expiresAt > Date(System.currentTimeMillis())) {
        if ((decodedJWT.getClaim("username").asString() == "pepe") &&
            (decodedJWT.getClaim("roles").asString() == "admin")
        ) {
            isVerified = true
        }
    }

    logger.debug { "Token verificado: $isVerified" }
    return isVerified
}