package client

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.Request
import models.Response
import models.User
import mu.KotlinLogging
import utils.encriptPassword
import utils.loadProperties
import utils.setClientProperties
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.UnknownHostException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.system.exitProcess

private const val PORT = 6666
private var ADDRESS: String = ""
private var server: SSLSocket? = null
private var input: DataInputStream? = null
private var output: DataOutputStream? = null
var token: String? = null

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true }

fun main() {
    loadProperties()
    setClientProperties()
    checkConexion()
    conectToServer()
    processIO()
    exitConexion()
}

fun checkConexion() {
    try {
        ADDRESS = InetAddress.getLocalHost().hostAddress
    } catch (ex: UnknownHostException) {
        logger.info { "Cliente->ERROR: No se encuentra dirección del servidor" }
        System.exit(-1)
    }
}

fun conectToServer() {
    val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
    server = factory.createSocket(ADDRESS, PORT) as SSLSocket

    input = DataInputStream(server!!.inputStream)
    output = DataOutputStream(server!!.outputStream)

}

fun processIO() {
    userLogin()
    val responseLogin = receive()
    if (responseLogin.type == Response.Type.OK) {
        token = responseLogin.content
        logger.info { "Ha iniciado sesión correctamente." }
    } else {
        logger.info { "Error. No ha sido posible iniciar sesión." }
        exitConexion()
    }

    userTime()
    val responseTime = receive()
    if (responseTime.type == Response.Type.OK) {
        token = responseTime.content
        logger.info { "Tiempo recibido del servidor: $token" }
    } else {
        logger.info { "Error. No ha sido posible recibir la fecha del servidor." }
        exitConexion()
    }

    exitConexion()
}


fun userLogin() {
    userLogin(User("pepe", encriptPassword("pepe1234"), ""), Request.Type.LOGIN)
}

fun userTime() {
    val request = Request(token, Request.Type.TIME)
    val jsonRequest = json.encodeToString(request)
    logger.info { "Enviando petición de tiempo : $jsonRequest" }
    output?.writeUTF(jsonRequest)
}

private fun userLogin(content: User, type: Request.Type) {
    val request = Request(content, type)
    val jsonRequest = json.encodeToString(request)
    logger.info { "Enviando petición de login : $jsonRequest" }
    output?.writeUTF(jsonRequest)
}

private fun receive(): Response<String> {
    val jsonResponse = input?.readUTF()
    return json.decodeFromString(jsonResponse!!)
}

fun exitConexion() {
    server!!.close()
    input!!.close()
    output!!.close()
    exitProcess(0)
}




