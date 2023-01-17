package server

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main():Unit = runBlocking{
    val props = Properties()
    props.load(javaClass.classLoader.getResourceAsStream("config.properties"))

    val puerto = props.getProperty("puerto").toInt()
    var logger = KotlinLogging.logger {}

    println("🤖Encendiendo servidor")

    val fichero = System.getProperty("user.dir") + File.separator + "cert" + File.separator + "server_keystore.p12"
    if (!Files.exists(Path.of(fichero))) {
        System.err.println("🤖 No se ha encontrado el fichero del certificado del servidor.")
        exitProcess(0)
    }

    logger.debug { "Creando servidor SSL" }
    System.setProperty("javax.net.ssl.keyStore", fichero) // Llavero
    System.setProperty("javax.net.ssl.keyStorePassword", props.getProperty("keyServer").toString()) // Clave de acceso

    var factory = SSLServerSocketFactory.getDefault() as SSLServerSocketFactory
    var server = factory.createServerSocket(puerto) as SSLServerSocket


    println("🤖 Servidor iniciado correctamente.")


    while (true){
        println("🤖 Esperando cliente.")

        var cliente = server.accept()

        thread {
            ClientControl(cliente).start()
        }

    }


}