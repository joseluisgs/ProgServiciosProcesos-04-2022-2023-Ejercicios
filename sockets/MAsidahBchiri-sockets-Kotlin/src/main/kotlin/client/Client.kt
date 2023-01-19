package client

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.Login
import models.Request
import models.Response
import models.User
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.Properties
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

private val userPepe =
    User("pepe", BCrypt.withDefaults().hash(10, "pepe1234".toByteArray(StandardCharsets.UTF_8)), "admin")

private val json = Json
private val properties = Properties()
private var token: String? = null
fun main(args: Array<String>): Unit = runBlocking {
    makeConnection()
    properties.load(javaClass.classLoader.getResourceAsStream("config.properties"))


    println("\uD83D\uDD0E Buscando servidor con el que conectar \uD83D\uDD0E")
    val serverFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
    val server = serverFactory.createSocket(
        InetAddress.getLocalHost().hostAddress,
        properties.getProperty("port").toInt()
    ) as SSLSocket

    println("✅ Conectado ✅")
    val readChannel = DataInputStream(server.inputStream)
    val writeChannel = DataOutputStream(server.outputStream)

    println("Loggeando como: pepe")
    var loginRequest = Request<String>(json.encodeToString(Login(userPepe.name, "pepe1234")), Request.Type.LOGIN)
    writeChannel.writeUTF(json.encodeToString(loginRequest))

    println("⏱️ Esperando respuesta ⏱️")
    val messageLogin = readChannel.readUTF()
    val responseLogin = json.decodeFromString<Response<String>>(messageLogin)
    when (responseLogin.type) {
        Response.Type.OK -> {
            println("✅ Sesión iniciada ✅")
            token = responseLogin.content
            println("\uD83D\uDD11 Tu token de acceso es: $token")

            println("Solicitando fecha del sistema...")
            val requestDate = Request<String>(token, Request.Type.GET)
            writeChannel.writeUTF(json.encodeToString(requestDate))
            println("⏱️ Esperando respuesta ⏱️")
            val messageDate = readChannel.readUTF()
            val responseDate = json.decodeFromString<Response<String>>(messageDate)
            when (responseDate.type) {
                Response.Type.OK -> {
                    println("\uD83D\uDCC5 Fecha recibida \uD83D\uDCC5")
                    println(responseDate.content)
                }

                else -> {
                    println("❌ Ha habido un error ❌ -> ${responseDate.content}")
                }
            }
        }

        else -> {
            println("❌ Ha habido un error ❌ -> ${responseLogin.content}")

        }
    }
    server.close()
    readChannel.close()
    writeChannel.close()
}

fun makeConnection() {
    val file = System.getProperty("user.dir") + File.separator + "cert" + File.separator + "client_keystore.p12"
    System.setProperty("javax.net.ssl.trustStore", file)
    System.setProperty("javax.net.ssl.trustStorePassword", "123456")
}



