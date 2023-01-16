package client

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.mensajes.Request
import models.mensajes.Response
import mu.KotlinLogging
import security.FicheroProperties
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.math.BigInteger
import java.net.InetAddress
import java.nio.file.Paths
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/*
Para poder probar si el usuario no tiene permisos, habra que cambiar su rol en el archivo Servidor.kt
 */
private val log = KotlinLogging.logger {}
private val json = Json

// Informacion del cliente y la conexion a realizar
private lateinit var direccion: InetAddress
private lateinit var clienteFactory: SSLSocketFactory
private lateinit var servidor: SSLSocket
private const val PUERTO = 6969

private lateinit var request: Request<String> // Todas las request seran de este tipo, la dejamos como lateinit para usarla en los metodos

private var salida: Boolean = false

private var token: String? =
    null // Se actualizara mientras el cliente este funcionando, y funcionara hasta la fecha limite.


fun main() {
    while (!salida) {
        if (token == null) {
            token = solicitarToken()
        } else {
            solicitud()
        }
    }
}

private fun solicitud() {
    // Menu simple que sirve para enviar la peticion cuando queramos o salir de la aplicacion
    println(
        """
        
        1.PEDIR HORA
        2.SALIR
    """.trimIndent()
    )
    val opcion = readln().toInt()

    when (opcion) {
        1 -> {
            log.debug { "\tEnviando solicitud" }
            request = Request(token, null, null, Request.Type.CONSULT)
        }

        2 -> {
            log.debug { "\tSaliendo de la APP..." }
        }
    }

    if (opcion == 2) {
        salida = true
    } else {
        // Preparamos de nuevo la conexion
        prepararConexion()

        // Canales de entrada-salida
        val salida = DataOutputStream(servidor.outputStream)
        val entrada = DataInputStream(servidor.inputStream)

        // Enviamos la peticion y esperamos la respuesta
        salida.writeUTF(json.encodeToString(request) + "\n")
        log.debug { "Enviado $request" }

        val responseSolicitud: Response<String> = json.decodeFromString(entrada.readUTF())

        val response = responseSolicitud.content
        log.debug { "Respuesta del servidor: $response" }

        // Si la respuesta es que el token ha caducado, lo ponemos a null
        if (responseSolicitud.type == Response.Type.TOKEN_EXPIRED) token = null
    }
}

private fun solicitarToken(): String? {
    // Preparamos al usuario
    val name = "Pepe"
    val password = "Pepe1234"

    // Preparamos la posible respuesta, que sera el token que podemos, o no, recibir
    val response: String?

    // Preparamos la conexion
    prepararConexion()

    // Preparamos los canales de entrada-salida
    val salida = DataOutputStream(servidor.outputStream)
    val entrada = DataInputStream(servidor.inputStream)

    // Enviamos el request de login
    request = Request(null, name, password, Request.Type.LOGIN)
    salida.writeUTF(json.encodeToString(request) + "\n")
    log.debug { "Se envio $request" }

    // Esperamos la respuesta
    val responseToken: Response<String> = json.decodeFromString(entrada.readUTF())

    response = if (responseToken.content != null) {
        log.debug { "\tSe recibio token" }
        responseToken.content
    } else {
        log.debug { "\tEl usuario no existe" }
        null
    }
    return response
}

private fun prepararConexion() {
    val workingDir: String = System.getProperty("user.dir")
    // Fichero de donde se sacan los datos del llavero
    val fichero =
        Paths.get(workingDir + File.separator + "sockets" + File.separator + "MGResa-sockets-Kotlin" + File.separator + "certClient" + File.separator + "llavero_client.p12")
            .toString()

    // Properties
    val properties = FicheroProperties.loadProperties()

    /*
    Para depurar y ver el dialogo y handshake -- NO es obligatorio

    A veces durante la ejecucion, este saldra el ultimo, parecera que el programa se ha colgado o ha dado fallo pero no.
     */
    System.setProperty("javax.net.debug", "ssl, keymanager, handshake")

    // Cargamos la informacion del llavero del cliente
    System.setProperty("javax.net.ssl.trustStore", fichero)
    System.setProperty("javax.net.ssl.trustStorePassword", properties.getProperty("llavero.client"))

    direccion = InetAddress.getLocalHost()
    clienteFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
    servidor = clienteFactory.createSocket(direccion, PUERTO) as SSLSocket

    // Sacar la informacion de la sesion
    informacionSesion(servidor)
}

private fun informacionSesion(servidor: SSLSocket) {
    val sesion = servidor.session
    // SERVIDOR
    println(
        """
        -Servidor: ${sesion.peerHost}
        -Cifrado: ${sesion.cipherSuite}
        -Protocolo: ${sesion.protocol}
        -IDentificador: ${BigInteger(sesion.id)}
        -Creacion de la sesion: ${sesion.creationTime}
        
    """.trimIndent()
    )
    // CERTIFICADO
    val certificado = sesion.peerCertificates[0] as X509Certificate
    println(
        """
        -Propietario: ${certificado.subjectX500Principal}
        -Algoritmo: ${certificado.sigAlgName}
        -Tipo: ${certificado.type}
        -Emisor: ${certificado.issuerX500Principal}
        -Numero de serie: ${certificado.serialNumber}
        
    """.trimIndent()
    )
}