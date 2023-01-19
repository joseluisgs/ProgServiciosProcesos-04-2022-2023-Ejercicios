package server

import mu.KotlinLogging
import utils.checkServerCertificate
import utils.loadProperties
import utils.setServerProperties
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory

private const val PORT = 6666

private val logger = KotlinLogging.logger {}


fun main() {
    //Cargamos las propiedades desde un fichero externo
    loadProperties()
    setServerProperties()

    //Certificado del servidor
    checkServerCertificate()

    val serverFactory = SSLServerSocketFactory.getDefault() as SSLServerSocketFactory
    val serverSocket = serverFactory.createServerSocket(PORT) as SSLServerSocket

    while (true) {
        logger.info { "Esperando conexiones..." }
        val control = ClientControl(serverSocket.accept())
        control.start()
    }

}




