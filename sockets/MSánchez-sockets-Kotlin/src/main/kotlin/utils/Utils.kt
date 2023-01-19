package utils

import de.nycode.bcrypt.hash
import de.nycode.bcrypt.verify
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

private val serverCertificate = System.getProperty("user.dir") + File.separator +
        "cert" + File.separator +
        "server_keystore.p12"

private val clientCertificate = System.getProperty("user.dir") + File.separator +
        "cert" + File.separator +
        "client_keystore.p12"

fun encriptPassword(password: String ): ByteArray {
    return hash(password,12)
}

fun checkPassword(password: String, encriptedPassword : ByteArray ): Boolean {
    return verify(password, encriptedPassword)
}

fun loadProperties(){
    logger.info { "Cargando fichero de propiedades" }
    ApplicationProperties().readProperty("keyServer")
    ApplicationProperties().readProperty("keyClient")
    ApplicationProperties().readProperty("javax.net.debug")
    ApplicationProperties().readProperty("javax.net.ssl.keyStorePassword")
    ApplicationProperties().readProperty("javax.net.ssl.trustStorePassword")

}

fun setServerProperties(){
    System.setProperty("javax.net.debug", "ssl, keymanager, handshake")
    System.setProperty("javax.net.ssl.keyStore", serverCertificate)
    System.setProperty("javax.net.ssl.keyStorePassword", "123456")
}

fun setClientProperties(){
    System.setProperty("javax.net.debug", "ssl, keymanager, handshake")
    System.setProperty("javax.net.ssl.trustStore", clientCertificate)
    System.setProperty("javax.net.ssl.trustStorePassword", "123456")
}



fun checkServerCertificate() {
    if (!Files.exists(Path.of(serverCertificate))) {
        logger.info("El fichero del certificado no se ha encontrado.")
        System.exit(0)
    }
}


