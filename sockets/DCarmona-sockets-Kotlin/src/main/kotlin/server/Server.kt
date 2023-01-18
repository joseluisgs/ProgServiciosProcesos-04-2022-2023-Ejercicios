package server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import model.User
import org.mindrot.jbcrypt.BCrypt
import utils.logDebug
import utils.logError
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import kotlin.system.exitProcess

private const val PUERTO = 6666
fun main() {
    logDebug("Iniciando datos...")
    val userRepository = generateData()
    logDebug("Iniciando el servidor...")
    val fichero = System.getProperty("user.dir") + File.separator + "cert" + File.separator + "server_keystore.p12"

    if (!Files.exists(Paths.get(fichero))) {
        logError("No existe el certificado")
        exitProcess(0)
    }

    logDebug("Cargando propiedades...")
    System.setProperty("javax.net.ssl.keyStore", fichero)
    System.setProperty("javax.net.ssl.keyStorePassword", "1234567")

    val serverFactory = SSLServerSocketFactory.getDefault() as SSLServerSocketFactory
    val serverSocket = serverFactory.createServerSocket(PUERTO) as SSLServerSocket

    serverSocket.enabledCipherSuites = arrayOf("TLS_AES_128_GCM_SHA256")
    serverSocket.enabledProtocols = arrayOf("TLSv1.3")
    logDebug("Servidor listo")

    while (true) {
        logDebug("Esperando conexiones...")
        val client = serverSocket.accept()
        logDebug("Cliente conectado: ${client.remoteSocketAddress}")

        CoroutineScope(Dispatchers.IO).launch {
            GestorCliente(client, userRepository).run()
        }
    }
}

fun generateData(): MutableList<User> {
    return mutableListOf(
        User("dani", BCrypt.hashpw("1234", BCrypt.gensalt(12)), null),
        User("pepe", BCrypt.hashpw("pepe1234", BCrypt.gensalt(12)), "admin"),
        User("marta", BCrypt.hashpw("123456", BCrypt.gensalt(12)), "admin")
    )
}

