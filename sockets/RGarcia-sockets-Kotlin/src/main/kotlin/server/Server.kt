package server


import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import utils.ApplicationProperties
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private const val PUERTO = 2525

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true }

fun main()= runBlocking{
    var numConexiones = 0
    println("Iniciando servidor en el puerto: $PUERTO")
    logger.debug { "Iniciando el servidor en dirección: localhost:$PUERTO" }
    val fichero = checkCertificado()
    val properties = ApplicationProperties()
    setupProperties(properties, fichero)

    val serverFactory = SSLServerSocketFactory.getDefault() as SSLServerSocketFactory
    val serverSocket = serverFactory.createServerSocket(PUERTO) as SSLServerSocket

    serverSocket.enabledCipherSuites = arrayOf("TLS_AES_128_GCM_SHA256")
    serverSocket.enabledProtocols = arrayOf("TLSv1.3")

    while (true){
        println("Esperando cliente...")
        var cliente = serverSocket.accept()

        coroutineScope {
            GestorUsuarios(cliente).start()
        }

    }

}

private fun setupProperties(properties: ApplicationProperties, fichero: String) {
    logger.debug { "Cargando fichero de propiedades" }
    System.setProperty("javax.net.debug", properties.readProperty("javax.net.debug"))
    System.setProperty("javax.net.ssl.keyStore", fichero)
    System.setProperty("javax.net.ssl.keyStorePassword", properties.readProperty("javax.net.ssl.keyStorePassword"))
}

private fun checkCertificado(): String {
    logger.debug { "Cargando fichero del llavero" }
    val fichero = System.getProperty("user.dir") + File.separator + "cert" + File.separator + "keystore.p12"
    if (!Files.exists(Path.of(fichero))) {
        logger.error { "No se encontró el archivo de certificado del servidor" }
        exitProcess(0)
    }
    return fichero
}

