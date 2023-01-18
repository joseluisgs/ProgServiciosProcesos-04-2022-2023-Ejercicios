package server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import common.Request
import common.Response
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.User
import model.UserLogin
import org.mindrot.jbcrypt.BCrypt
import utils.logDebug
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.time.LocalDateTime
import java.util.*

class GestorCliente(
    private val client: Socket,
    private val userRepository: MutableList<User>,
) {
    private val algoritmo = Algorithm.HMAC256("Contras3ñaS3cr3taYNadi3D3b3Sab3rla")
    private lateinit var user: User
    private lateinit var inputData: DataInputStream
    private lateinit var outputData: DataOutputStream
    fun run() {
        inputData = DataInputStream(client.inputStream)
        outputData = DataOutputStream(client.outputStream)


        val json = inputData.readUTF()
        logDebug("Recibiendo petición de login...")
        val request = Json.decodeFromString<Request<UserLogin>>(json)

        request.content?.let { comprobarPassword(it) }

        if (!client.isClosed) {
            getMessage()
        }
    }

    private fun getMessage() {
        val json = inputData.readUTF()
        val request = Json.decodeFromString<Request<User>>(json)

        logDebug("Recibiendo petición del cliente...")
        request.token?.let {
            logDebug("Comprobando token...")
            comprobarToken(it)
        }

    }

    private fun comprobarToken(token: String) {
        val verifier = JWT.require(algoritmo).build()
        val decode = verifier.verify(token)
        val expiresAt = decode.expiresAt
        val rol = decode.getClaim("rol").asString()

        if (expiresAt < Date(System.currentTimeMillis())) {
            sendResponseError("Fecha de token expirada.")
        } else if (rol != "admin") sendResponseError("No tienes permisos.")
        else {
            val response = Response(
                LocalDateTime.now().toString(),
                Response.Type.OK
            )
            val json = Json.encodeToString(response)
            outputData.writeUTF(json)
        }

    }

    private fun comprobarPassword(userLogin: UserLogin) {
        logDebug("Comprobando password...")
        var passwordGenerated = ""

        val userExist = userRepository.firstOrNull { it.userName == userLogin.userName }
        userExist?.let {
            passwordGenerated = it.password
            this.user = it
            val password = userLogin.password
            if (BCrypt.checkpw(password, passwordGenerated)) {
                logDebug("Contraseña verificada...")
                val response = Response<String>(generateToken(user), Response.Type.OK)
                val json = Json.encodeToString(response)
                outputData.writeUTF(json)

            } else sendResponseError("Nombre de usuario o contraseña no válida")

        } ?: run {
            sendResponseError("Nombre de usuario o contraseña no válida")
        }

    }

    private fun sendResponseError(message: String) {
        val response = Response<String>(message, Response.Type.ERROR)
        val json = Json.encodeToString(response)
        outputData.writeUTF(json)

        inputData.close()
        outputData.close()
        client.close()
    }

    private fun generateToken(user: User): String {
        return JWT.create()
            .withIssuer("Daniel Carmona Rodriguez")
            .withSubject("2DAM")
            .withClaim("userName", user.userName)
            .withClaim("rol", user.rol)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(algoritmo)
    }
}