package client

import common.Request
import common.Response
import common.TypeRequestEnum
import common.TypeResponseEnum.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.LoginUser
import mu.KotlinLogging
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.lang.Thread.sleep
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.system.exitProcess


var logger = KotlinLogging.logger {}
var entrada: DataInputStream? = null
var salida: DataOutputStream? = null
var server: SSLSocket? = null
val props = Properties()

fun main() = runBlocking{
    props.load(javaClass.classLoader.getResourceAsStream("config.properties"))
    var token: String? = null
    var json = Json


    crearConexion()

    var factory = SSLSocketFactory.getDefault() as SSLSocketFactory
    server = factory.createSocket(InetAddress.getLocalHost().hostAddress, props.getProperty("puerto").toInt()) as SSLSocket

    println("✅ Cliente conectado correctamente.")
    entrada = DataInputStream(server?.inputStream)
    salida = DataOutputStream(server?.outputStream)


    println("✅ Datos de la sesión")
    println("--> Session: ${server?.session} \n" +
            "--> Session Created At: ${server?.session?.creationTime} \n" +
            "--> Session Port: ${server?.session?.peerPort} \n" +
            "--> Session Context: ${server?.session?.sessionContext} \n" +
            "--> Session Protocol: ${server?.session?.protocol}")


    logger.debug { "Creamos la peticion de login" }
    var login = LoginUser("pepe","pepe1234")
    var requestLogin = Request<LoginUser>(type =TypeRequestEnum.LOGIN, data= login)
    logger.debug { "Enviamos peticion de login" }
    salida?.writeUTF((json.encodeToString(requestLogin))+"\n")

    logger.debug { "Recibimos respuesta del servidor de la petición de login"}
    var entradaLogin = entrada?.readUTF()
    var responseLogin = json.decodeFromString<Response<String>>(entradaLogin!!)
    when(responseLogin.type){
        ERROR -> salirPorError(responseLogin.data)
        OK -> {
            token = responseLogin.data
            println("✅ Token recibido --> $token")
        }
    }


    logger.debug { "Creamos la peticion de tiempo" }
    var requestTime = Request<String>(type = TypeRequestEnum.TIME, data = token!!)
    logger.debug { "Enviamos peticion de tiempo" }
    salida?.writeUTF((json.encodeToString(requestTime))+"\n")

    logger.debug { "Recibimos respuesta del servidor de la petición de tiempo"}
    var entradaTiempo = entrada?.readUTF()
    var responseTiempo = json.decodeFromString<Response<String>>(entradaTiempo!!)
    when(responseTiempo.type){
        ERROR -> salirPorError(responseTiempo.data)
        OK -> println("✅ Respuesta Recibida --> ${responseTiempo.data}")
    }


    salir()
}

/**
 * Función para salir
 */
fun salir() {
    server!!.close()
    salida!!.close()
    entrada!!.close()
}


/**
 * Función para salir de la session si ha habido errores
 */
fun salirPorError(data: String) {
    salir()
    println("❌ Error: $data")
    exitProcess(0)
}


/**
 * Creamos la conexión SSL con el servidor
 */
fun crearConexion() {
    println("✅ Encendiendo cliente")

    val fichero = System.getProperty("user.dir") + File.separator + "cert" + File.separator + "client_keysotre.p12"
    if (!Files.exists(Path.of(fichero))) {
        System.err.println("❌ No se ha encontrado el fichero del certificado del cliente.")
        exitProcess(0)
    }

    logger.debug { "Creando conexion SSL" }
    System.setProperty("javax.net.ssl.trustStore", fichero)
    logger.debug { "Añadiendo la clave del cliente" }
    System.setProperty("javax.net.ssl.trustStorePassword", props.getProperty("keyClient"))

}
