package server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.Request
import models.Response
import models.User
import mu.KotlinLogging
import repositories.usersRepository
import utils.checkPassword
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.time.LocalDateTime
import java.util.*

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true }

class ClientControl(
    var cliente: Socket

) : Thread() {

    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private var algorithm = Algorithm.HMAC256("EjercicioRepaso")
    private var verificador = JWT.require(algorithm).build()

    override fun run() {
        createIOFlows()
        processIO()
        exitConexion()
    }

    private fun processIO() {
        val isOk = checkClient(receiveLoginData().content)
        if (isOk) {
            val tiempo = input?.readUTF()
            val requestTiempo = json.decodeFromString<Request<String>>(tiempo!!)
            if (checkToken(requestTiempo.content!!)) {
                logger.info {"Enviando datos respuesta tiempo..." }
                send(LocalDateTime.now().toString(), Response.Type.OK)
            }

            exitConexion()
        }

    }

    private fun createIOFlows() {
        logger.info { "Procesando cliente..." }
        input = DataInputStream(cliente.inputStream)
        output = DataOutputStream(cliente.outputStream)
    }

    private fun send(content: String, type: Response.Type) {
        val response = Response(content, type)
        val jsonResponse = json.encodeToString(response)
        logger.info { "Enviado: $jsonResponse" }
        output?.writeUTF(jsonResponse)
    }

    private fun receiveLoginData(): Request<User> {
        val jsonRequest = input?.readUTF()
        return json.decodeFromString<Request<User>>(jsonRequest!!)
    }


    private fun exitConexion() {
        cliente.close()
        input!!.close()
        output!!.close()

    }

    private fun checkClient(content: User?): Boolean {
        val user = usersRepository().filter { it.name == content?.name }[0]
        val isPasswordOk = checkPassword("pepe1234", content?.password!!)

        if (user != null && isPasswordOk) {
            createToken(user)
            return true
        } else {
            send("Usuario o contraseña incorrecto.", Response.Type.ERROR)
            logger.info { "Error al iniciar sesión." }
            return false
        }
    }

    private fun createToken(user: User) {
        val token = JWT.create()
            .withIssuer("Server")
            .withClaim("rol", user.rol)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 5000L))
            .withJWTId(UUID.randomUUID().toString())
            .sign(algorithm)

        logger.info { "Enviando token al usuario." }
        send(token, Response.Type.OK)
    }

    private fun checkToken(content: String): Boolean {
        return if (verificador.verify(content).expiresAt > Date(System.currentTimeMillis()) &&
            verificador.verify(content).getClaim("rol").asString() == "admin") {
            true

        } else {
            send("No tienes los permisos necesarios.", Response.Type.ERROR)
            false
        }


    }
}