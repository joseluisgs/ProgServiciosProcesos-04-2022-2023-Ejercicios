package utils

import mu.KotlinLogging

private val logger = KotlinLogging.logger {  }

fun logDebug(message: String){
    logger.debug{message}
}
fun logError(message: String){
    logger.error{message}
}