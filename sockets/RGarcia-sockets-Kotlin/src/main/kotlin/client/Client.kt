package client

import common.Request
import common.Response
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.LoginUser
import models.Usuario
import mu.KotlinLogging
import utils.ApplicationProperties
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.system.exitProcess

private const val PUERTO = 2525
private const val SERVER = "localhost"

private val logger = KotlinLogging.logger {}
private val json = Json
private var token: String? = null
var socket: SSLSocket? = null
val entrada : DataInputStream? = null
val salida : DataOutputStream? = null

fun main() {
    val properties = ApplicationProperties()
    println("Iniciando cliente...")

    logger.debug { "Cargando fichero del llavero Cliente" }
    val fichero =  System.getProperty("user.dir") + ApplicationProperties().readProperty("certificado.cliente")
    if (!Files.exists(Path.of(fichero))) {
        System.err.println("No se encuentra el fichero de certificado del servidor")
        exitProcess(0)
    }
    System.setProperty("javax.net.debug", properties.readProperty("javax.net.debug"))
    System.setProperty("javax.net.ssl.trustStore", fichero)
    System.setProperty("javax.net.ssl.trustStorePassword", properties.readProperty("javax.net.ssl.trustStorePassword"))

    val clientFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
    socket = clientFactory.createSocket(SERVER, PUERTO) as SSLSocket

    logger.debug { "Soporta: ${socket!!.supportedProtocols.contentToString()}" }
    socket!!.enabledCipherSuites = arrayOf("TLS_AES_128_GCM_SHA256")
    socket!!.enabledProtocols = arrayOf("TLSv1.3")

    logger.debug { "Cliente conectado a $SERVER:$PUERTO"}

    val entrada = DataInputStream(socket!!.inputStream)
    val salida = DataOutputStream(socket!!.outputStream)

    //Primero enviamos los datos del login
    logger.debug { "Intentando loguearse" }
    val requestLogin = Request<LoginUser>(LoginUser("pepe", "pepe1234"), Request.Type.INICIAR_SESION)
    val jsonRequest = json.encodeToString(requestLogin)
    logger.debug { "Enviando: $jsonRequest" }
    salida.writeUTF(jsonRequest+"\n")

    //Despues recibimos la respuesta por parte del servidor
    logger.debug { "Recibimos respuesta del logueo en el servidor..."}
    val entradaServer = entrada.readUTF()
    val responseLogin = json.decodeFromString<Response<String>>(entradaServer)
    when(responseLogin.type){
        Response.Type.ERROR -> respuestaError(responseLogin.content!!)
        Response.Type.OK -> {
            token = responseLogin.content
            println("Token recibido del servidor: $token")
        }
    }

    logger.debug { "Creamos la peticion de tiempo y la enviamos" }
    val requestTime = Request<String>(token!!, Request.Type.TIME)
    salida.writeUTF((json.encodeToString(requestTime))+"\n")

    logger.debug { "Recibimos respuesta del servidor de la petición de tiempo"}
    val entradaTiempo = entrada.readUTF()
    val responseTiempo = json.decodeFromString<Response<String>>(entradaTiempo)
    when(responseTiempo.type){
        Response.Type.ERROR -> respuestaError(responseTiempo.content!!)
        Response.Type.OK ->{
            println("Respuesta Recibida: ${responseTiempo.content}")
            salir()
        }
    }


}
fun salir() {
    socket?.close()
    salida?.close()
    entrada?.close()
    exitProcess(0)
}


fun respuestaError(data: String) {
    logger.error { "Error" }
    println(data)
    salir()
}

