package server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import kotlin.system.exitProcess

private const val PORT = 6969
private val logger = KotlinLogging.logger {}

fun main() = runBlocking {
    println("✅ Servidor iniciado...")
    logger.debug { "Cargando certificados..." }

    val file = System.getProperty("user.dir") + File.separator + "cert" + File.separator + "repaso_keystore.p12"
    if (!Files.exists(Path.of(file))) {
        logger.error { "Llavero no encontrado..." }
        exitProcess(0)
    }

    System.setProperty("javax.net.debug", "ssl, keymanager, handshake")
    System.setProperty("javax.net.ssl.keyStore", file)
    System.setProperty("javax.net.ssl.keyStorePassword", "repaso")

    val serverFactory = SSLServerSocketFactory.getDefault() as SSLServerSocketFactory
    val serverSocket = withContext(Dispatchers.IO) {
        serverFactory.createServerSocket(PORT)
    } as SSLServerSocket

    serverSocket.enabledCipherSuites = arrayOf("TLS_AES_128_GCM_SHA256")
    serverSocket.enabledProtocols = arrayOf("TLSv1.3")

    println("#️⃣ Servidor iniciado en el puerto $PORT.")
    while (true) {
        val socket = withContext(Dispatchers.IO) {
            serverSocket.accept()
        }

        println("Cliente conectado: ${socket.remoteSocketAddress}")
        launch {
            logger.debug("Entrada del cliente...")
            val gestor = GestorClientes(socket)

            gestor.procesarClientes()
        }
    }
}