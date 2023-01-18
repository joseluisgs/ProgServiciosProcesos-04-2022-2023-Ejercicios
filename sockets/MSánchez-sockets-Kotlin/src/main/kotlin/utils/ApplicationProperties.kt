package utils

import mu.KotlinLogging
import java.io.IOException
import java.util.*

class ApplicationProperties {
    private val logger = KotlinLogging.logger {}
    private var properties: Properties? = null

    fun ApplicationProperties() {
        properties = Properties()
        try {
            properties!!.load(javaClass.classLoader.getResourceAsStream("application.properties"))
        } catch (ex: IOException) {
            logger.error("IOException ha ocurrido al leer el fichero de propiedades: " + ex.message)
        }
    }

    fun readProperty(keyName: String?): String? {
        return properties?.getProperty(keyName, "No existe esa clave en el fichero de propiedades")
    }
}