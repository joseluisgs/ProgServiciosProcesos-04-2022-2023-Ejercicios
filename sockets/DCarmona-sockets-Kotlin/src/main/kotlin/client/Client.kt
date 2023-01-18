package client

import common.Request
import common.Response
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.User
import model.UserLogin
import utils.logDebug
import utils.logError
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.system.exitProcess

private const val PUERTO = 6666
private lateinit var token: String
private lateinit var inputData: DataInputStream
private lateinit var outputData: DataOutputStream

fun main() {
    logDebug("Iniciando Cliente...")
    val fichero = System.getProperty("user.dir") + File.separator + "cert" + File.separator + "client_keystore.p12"
    if (!Files.exists(Paths.get(fichero))) {
        logError("No existe el certificado")
        exitProcess(0)
    }

    logDebug("Cargando propiedades...")
    System.setProperty("javax.net.ssl.trustStore", fichero)
    System.setProperty("javax.net.ssl.trustStorePassword", "1234567")

    val clientFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
    val socket = clientFactory.createSocket("localhost", PUERTO) as SSLSocket

    socket.enabledCipherSuites = arrayOf("TLS_AES_128_GCM_SHA256")
    socket.enabledProtocols = arrayOf("TLSv1.3")
    logDebug("Conectando al servidor...")

    inputData = DataInputStream(socket.inputStream)
    outputData = DataOutputStream(socket.outputStream)

    println("Introduce el nombre de usuario:")
    val userName = readln()
    println("Introduce la contraseña:")
    val password = readln()

    sendLogin(userName, password)

    var json = inputData.readUTF()
    var response = Json.decodeFromString<Response<String>>(json)
    logDebug("Recibiendo respuesta del servidor...")
    when (response.type) {
        Response.Type.OK -> {
            token = response.content.toString()
        }

        Response.Type.ERROR -> {
            response.content?.let { logError(it) }
            exitProcess(0)
        }
    }
    sendTimeRequest()

    json = inputData.readUTF()
    response = Json.decodeFromString(json)
    logDebug("Recibiendo respuesta del servidor...")
    when (response.type) {
        Response.Type.OK -> {
            println(response.content)
        }

        Response.Type.ERROR -> {
            response.content?.let { logError(it) }
            exitProcess(0)
        }
    }
}

private fun sendTimeRequest() {
    logDebug("Enviando petición de tiempo...")
    val request = Request<User>(
        null,
        token,
        Request.Type.TIME
    )
    val json = Json.encodeToString<Request<User>>(request)
    outputData.writeUTF(json)
}

private fun sendLogin(userName: String, password: String) {
    logDebug("Enviando información de login...")
    val request = Request(
        UserLogin(userName, password),
        null,
        Request.Type.LOGIN
    )
    val json = Json.encodeToString(request)
    outputData.writeUTF(json)
}




