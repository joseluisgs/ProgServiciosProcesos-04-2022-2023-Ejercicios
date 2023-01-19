package server

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import models.User
import java.io.File
import java.util.Properties
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = runBlocking {
    val properties = Properties()
    properties.load(javaClass.classLoader.getResourceAsStream("config.properties"))
    val port = properties.getProperty("port").toInt()

    val file = System.getProperty("user.dir") + File.separator + "cert" + File.separator + "server_keystore.p12"
    if (file == null) {
        println("No se han encotnrado certificados de servidor")
        exitProcess(0)
    }

    System.setProperty("javax.net.ssl.keyStore", file) //llavero
    System.setProperty("javax.net.ssl.keyStorePassword", properties.getProperty("keyServer").toString()) //clave

    //crear servidor
    println("\uD83D\uDEF0️Inciando servidor\uD83D\uDEF0️")

    val serverFactory = SSLServerSocketFactory.getDefault() as SSLServerSocketFactory
    val server = serverFactory.createServerSocket(port) as SSLServerSocket

    while (true) {
        println("✅Servidor iniciado✅")
        println("Esperando clientes...")

        val socket = server.accept()
        println("Conexión recibida!!")
        thread {
            ClientManagement(socket).run()
        }
    }
}