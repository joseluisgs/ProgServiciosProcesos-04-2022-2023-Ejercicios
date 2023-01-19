package server

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.Login
import models.Request
import models.Response
import models.User
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.*

class ClientManagement(private val socket: Socket) {
    private var readChannel: DataInputStream? = null
    private var writeChannel: DataOutputStream? = null
    private val json = Json
    private val users = listOf(
        User("pepe", BCrypt.withDefaults().hash(10, "pepe1234".toByteArray(StandardCharsets.UTF_8)), "admin"),
        User("juan", BCrypt.withDefaults().hash(10, "juan1234".toByteArray(StandardCharsets.UTF_8)), "user")
    )

    fun run() {
        readChannel = DataInputStream(socket.getInputStream())
        writeChannel = DataOutputStream(socket.getOutputStream())


        println("\uD83D\uDEEC Recibiendo mensaje del cliente \uD83D\uDEEC")
        val messageLogin = readChannel?.readUTF()
        println("Mensaje recibido!")
        val request = json.decodeFromString<Request<String>>(messageLogin!!)
        if (request.type == Request.Type.LOGIN) {

            val user = request.content
            println("Recibido el usuario: $user")
            println("\uD83D\uDD10 Usuario intentando iniciar sesión \uD83D\uDD10")
            logIn(json.decodeFromString(user!!))

        }

        val messageDate = readChannel?.readUTF()
        //println("Mensaje recibido!")
        val requestDate = json.decodeFromString<Request<String>>(messageDate!!)
        if (requestDate.type == Request.Type.GET) {
            println("\uD83D\uDEEC Recibida petición del cliente \uD83D\uDEEC")
            checkToken(requestDate.content)
        }
    }

    /**
     * Comprueba el token
     */
    private fun checkToken(content: String?) {
        val decoded = JWT.require(Algorithm.HMAC256("AlgorithmoMohaToken")).build().verify(content)
        if (decoded.expiresAt > Date(System.currentTimeMillis())) {
            if (decoded.getClaim("role").asString() == "admin") {
                sendDate()
            } else {
                notAllowed()
            }
        } else {
            expiredToken()
        }
    }

    /**
     * Informa de que no tiene permiso para la operacion
     */
    private fun notAllowed() {
        val response = Response<String>("⛔ No tienes permiso ⛔", Response.Type.NOT_ALLOWED)
        writeChannel?.writeUTF(json.encodeToString(response) + "\n")
        closeConnection()
    }

    /**
     * Envía la fecha del sistema
     */
    private fun sendDate() {
        val response = Response(LocalDateTime.now().toString(), Response.Type.OK)
        writeChannel?.writeUTF(json.encodeToString(response) + "\n")
        closeConnection()
    }

    /**
     * Informa de que el token ha expirado
     */
    private fun expiredToken() {
        val response = Response<String>("⏰ Token caducado ⏰", Response.Type.EXPIRED_TOKEN)
        writeChannel?.writeUTF(json.encodeToString(response) + "\n")
        closeConnection()
    }

    /**
     * Loggea al usuario en el sistema
     */
    private fun logIn(user: Login) {
        var found = users.firstOrNull { it.name == user.name }
        if (found != null) {
            //userNotFound()

            //existe el usuario
            if (BCrypt.verifyer().verify(user.password.toByteArray(StandardCharsets.UTF_8), found.password).verified) {
                logUser(found)
            } else {
                wrongPassword()
            }
        } else {
            userNotFound()
        }
    }

    /**
     * informa de que ha loggeado con exito
     */
    private fun logUser(user: User) {
        val token = generateToken(user)
        val response = Response<String>(token, Response.Type.OK)
        println("\uD83D\uDEEB️ Enviando token de acceso \uD83D\uDEEB")
        writeChannel?.writeUTF(json.encodeToString(response) + "\n")

    }

    /**
     * Genera el token JWT
     */
    private fun generateToken(user: User): String {
        println("\uD83D\uDDDD️ Generando token de acceso \uD83D\uDDDD️")
        return JWT.create()
            .withIssuer("server")
            .withClaim("name", user.name)
            .withClaim("role", user.role)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 60000L))
            .withJWTId(UUID.randomUUID().toString())
            .sign(Algorithm.HMAC256("AlgorithmoMohaToken"))
    }

    /**
     * Informa de que el usuario no existe
     */
    private fun userNotFound() {
        val response = Response<String>("El usuario no existe", Response.Type.ERROR)
        writeChannel?.writeUTF(json.encodeToString(response) + "\n")
        closeConnection()
    }

    /**
     * Informa de que la contraseña es incorrecta
     */
    private fun wrongPassword() {
        val response = Response<String>("Contraseña incorrecta", Response.Type.ERROR)
        writeChannel?.writeUTF(json.encodeToString(response) + "\n")
        closeConnection()

    }

    /**
     * Cierra la conexion
     */
    private fun closeConnection() {
        this.socket.close()
        this.writeChannel?.close()
        this.readChannel?.close()
    }


}