package security

import java.io.File
import java.io.FileInputStream
import java.nio.file.Paths
import java.util.*

object FicheroProperties {

    private val fs = File.separator // Para que quede un path mas compacto

    fun loadProperties(): Properties {
        val workingDir: String = System.getProperty("user.dir")
        // Fichero properties
        val ficheroProperties =
            Paths.get(workingDir + fs + "sockets" + fs + "MGResa-sockets-Kotlin" + fs + "src" + fs + "main" + fs + "resources" + fs + "config.properties")

        val properties = Properties()

        // Cargamos el archivo
        properties.load(FileInputStream(ficheroProperties.toString()))

        return properties
    }
}