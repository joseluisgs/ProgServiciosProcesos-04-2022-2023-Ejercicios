package server

import models.Usuario
import monitor.UsersDb
import mu.KotlinLogging
import security.Cifrador
import security.FicheroProperties
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.Executors
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.SSLSocket

/*
ENUNCIADO:

En un servidor tenemos un unico usuario llamado "Pepe" con contraseña "Pepe1234" cifrada con Bcrypt

La conexion es segura con TSL/AES, es decir, uso de claves, certificados y llaveros tanto para el servidor como para el cliente.

El modelo usuario tendra un campo de rol, ademas de nombre de usuario y contraseña,
El unico usuario tendra un valor "Admin" en el rol

El usuario se conecta al servidor, este le envia sus datos de login, la informacion es verificada:
    - Si es correcta, el servidor le envia el token correspondiente al usuario de vuelta
    - Si es incorrecta, el servidor le envia un aviso al usuario, sin token.

El usuario, si ha recibido el token, envia una señal al servidor para pedir la hora
El servidor recibe el aviso, este analiza el token:
    - Si es correcto, el servidor le envia la hora como respuesta.
    - Si es incorrecto (token no valido o caducado), le envia un aviso.

- El cliente debera mostrar la informacion de la sesion
- Tanto el cliente como el servidor deberan leer la informacion pertinente de un fichero properties.
- Uso de Request-Response
----------------------------------------------------------------

Solucion propuesta:

Voy a generar una SC donde se almacenen los usuarios, que en este caso solo habra uno como se pide. Al iniciar
el servidor, este se introducira en un mapa protegido con un Reentrantlock.

Voy a hacer uso de una libreria que encontre por estar mas o menos acostumbrado para el tema BCrypt
https://github.com/ToxicBakery/bcrypt-mpp

Tanto para el cifrado de contraseñas como para la generacion de tokens voy a crear clases especificas
que se encarguen de realizar esas operaciones.

Todos los datos del usuario los pondre en variables, asi evito pedir la informacion por teclado.

Ademas y para variar un poco, los llaveros van a ser p12 en vez de jks.

-- GUIA (keytool) --
    1.Creamos el certificado en cuestion; abrimos la terminal:
        keytool -genkey -keyalg RSA -alias <alias> -keystore <keystore.p12> -validity <days> -keysize 2048
    2.Introducimos los datos que se soliciten.
    3.Ese archivo.jks es el que usara el servidor; lo guardamos en la raiz del proyecto (por ejemplo)
    4.Exportamos el certificado; nos desplazamos con cd hasta el directorio donde se encuentre el jks; abrimos terminal
        keytool -export -alias <alias> -storepass <clave> -file <archivo.cer> -keystore <archivo.p12>
    5.Ese certificado lo importamos a un nuevo llavero, que sera el que usara el cliente; lo mismo que en el punto 4
        keytool -import -alias <alias> -file <archivo.cer> -keystore <archivo.p12>
    6.Introducimos los datos que se soliciten
    7.FIN

-- PD: Comando util para ver la informacion del keystore
        keytool -list -v -keystore <archivo.p12>
 */

private val log = KotlinLogging.logger {}

// Informacion del servidor
private const val PUERTO = 6969

private lateinit var serverFactory: SSLServerSocketFactory
private lateinit var servidor: SSLServerSocket

fun main() {
    // Cliente
    var cliente: SSLSocket

    // Pool de hilos
    val pool = Executors.newFixedThreadPool(10)

    // Preparamos la DB para Usuarios
    val userDb = UsersDb()
    // Usuarios
    val users = listOf(
        Usuario("Pepe", Usuario.TipoUser.ADMIN, Cifrador.codifyPassword("Pepe1234"))
    )
    // Introducimos a los usuarios
    repeat(users.size) {
        userDb.register(users[it])
    }

    // Arrancamos el servidor
    log.debug { "Encendiendo servidor..." }

    prepararConexion()

    log.debug { "\t--Servidor esperando..." }
    while (true) {
        cliente = servidor.accept() as SSLSocket
        log.debug { "Peticion de cliente -> " + cliente.inetAddress + " --- " + cliente.port }

        val gc = GestorClientes(cliente, userDb)
        pool.execute(gc)
    }

}

private fun prepararConexion() {
    val workingDir: String = System.getProperty("user.dir")
    // Fichero donde se sacan los datos
    val fichero =
        Paths.get(workingDir + File.separator + "sockets" + File.separator + "MGResa-sockets-Kotlin" + File.separator + "cert" + File.separator + "llavero_server.p12")
            .toString()

    // Properties
    val properties = FicheroProperties.loadProperties()

    System.setProperty("javax.net.ssl.keyStore", fichero)
    System.setProperty("javax.net.ssl.keyStorePassword", properties.getProperty("llavero.server"))

    // Generamos tanto el SSLServerSocketFactory como el SSLServerSocket
    serverFactory = SSLServerSocketFactory.getDefault() as SSLServerSocketFactory
    servidor = serverFactory.createServerSocket(PUERTO) as SSLServerSocket
}
