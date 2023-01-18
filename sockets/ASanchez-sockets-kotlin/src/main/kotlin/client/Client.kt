package client

import common.Request
import common.Response
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.User
import mu.KotlinLogging
import services.cifrar
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Path
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.system.exitProcess

private const val PORT = 6969
private val logger = KotlinLogging.logger {}

fun main() {
    val file = System.getProperty("user.dir") + File.separator + "cert" + File.separator + "cliente_keystore.p12"
    if (!Files.exists(Path.of(file))) {
        logger.error { "Llavero no encontrado..." }
        exitProcess(0)
    }

    System.setProperty("javax.net.debug", "ssl, keymanager, handshake")
    System.setProperty("javax.net.ssl.trustStore", file)
    System.setProperty("javax.net.ssl.trustStorePassword", "repaso")

    val serverFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
    val serverSocket = serverFactory.createSocket(InetAddress.getLocalHost().hostAddress, PORT) as SSLSocket

    println("👤 Cliente conectado.")
    val input = DataInputStream(serverSocket.inputStream)
    val output = DataOutputStream(serverSocket.outputStream)

    val user = User("pepe", cifrar("pepe1234"), "admin")
    val jsonRequestlogin = Request(user, Request.Type.LOGIN)
    logger.debug { "Enviando datos de inicio de sesión al servidor..." }
    output.writeUTF((Json.encodeToString(jsonRequestlogin)) + "\n")

    logger.debug { "Recibiendo respuesta del servidor..." }
    val responseLogin = input.readUTF()
    val token = Json.decodeFromString<Response<String>>(responseLogin)

    logger.debug { "Recibiendo token..." }
    when (token.type) {
        Response.Type.ERROR -> {
            logger.error { "Enviando salida de error..." }

            serverSocket.close()
            exitProcess(0)
        }

        Response.Type.OK -> println("⚜️ Token recibido: ${token.content}")
    }

    logger.debug { "Reenviando token con hora actual..." }
    val request = Request(token.content, Request.Type.TIME)
    output.writeUTF(Json.encodeToString(request) + "\n")
    logger.debug { "Token con hora reenviado." }

    logger.debug { "Recibiendo hora actual del servidor" }
    val timeInput = input.readUTF()
    val time = Json.decodeFromString<Response<String>>(timeInput)
    logger.debug { "Hora del servidor recibida." }
    println("💲 Hora actual del servidor: ${time.content}")

    output.close()
    input.close()

    // Al cerrar el cliente da una excepción antes de cerrarse
    // a pesar de que el programa y la conexión funciona completamente. He investigado y al parecer
    // da en algunos sistemas operativos por el antivirus o por problemas con el router.
    serverSocket.close()
    exitProcess(0)
}