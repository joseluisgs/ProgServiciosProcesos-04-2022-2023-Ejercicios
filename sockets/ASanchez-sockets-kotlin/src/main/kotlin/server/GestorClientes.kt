package server

import com.auth0.jwt.algorithms.Algorithm
import common.Request
import common.Response
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.User
import mu.KotlinLogging
import services.comprobar
import services.createToken
import services.verifyToken
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.time.LocalDateTime
import kotlin.system.exitProcess

private var logger = KotlinLogging.logger {}

class GestorClientes(private val cliente: Socket) {
    private val input = DataInputStream(cliente.inputStream)
    private val output = DataOutputStream(cliente.outputStream)
    fun procesarClientes() {
        logger.debug { "Recibiendo datos del cliente..." }
        val loginRequest = input.readUTF()
        val jsonRequestLogin = Json.decodeFromString<Request<User>>(loginRequest)

        //Comprobamos la contraseña codificada con BCrypt y creamos el token.
        if (jsonRequestLogin.type == Request.Type.LOGIN) {
            sendResponse(jsonRequestLogin.content)
        }

        logger.debug { "Recibiendo token por parte del cliente..." }
        val tokenRequest = input.readUTF()

        val jsonRequestToken = Json.decodeFromString<Request<String>>(tokenRequest)
        processToken(jsonRequestToken.content)

        try {
            output.close()
            input.close()
            cliente.close()
        } catch (e: Exception) {
            logger.debug { "❌ Cerrando la conexión con el servidor." }
            exitProcess(0)
        }
    }

    private fun processToken(token: String?) {
        val algoritmo = Algorithm.HMAC256("RepasoPSP")
        logger.debug { "Procesando token..." }

        if (verifyToken(algoritmo, token!!)) {
            logger.debug { "Token verificado." }

            val response = Response(LocalDateTime.now().toString(), Response.Type.OK)
            output.writeUTF(Json.encodeToString(response) + "\n")
        } else {
            notifyError("NO AUTORIZADO.")
        }
    }

    private fun sendResponse(content: User?) {
        println(content.toString())
        if (content == null) {
            notifyError("Usuario no existente.")
        } else {
            if (comprobar("pepe1234", content.password)) {
                val token = createToken("RepasoPSP", "Servidor", "Envío de token al cliente", "pepe", "admin")

                val response = Response(token, Response.Type.OK)
                output.writeUTF(Json.encodeToString(response) + "\n")
                logger.debug { "Token enviado al cliente." }
            } else {
                notifyError("Nombre de usuario o contraseña no válida.")
            }
        }
    }

    private fun notifyError(message: String) {
        val response = Response(message, Response.Type.ERROR)
        logger.debug { "Enviando salida de error..." }

        output.writeUTF(Json.encodeToString(response) + "\n")

        this.cliente.close()
    }
}