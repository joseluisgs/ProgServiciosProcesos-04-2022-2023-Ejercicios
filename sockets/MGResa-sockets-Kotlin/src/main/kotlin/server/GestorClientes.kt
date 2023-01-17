package server

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.Usuario
import models.mensajes.Request
import models.mensajes.Response
import monitor.UsersDb
import mu.KotlinLogging
import security.ManejadorTokens
import java.io.DataInputStream
import java.io.DataOutputStream
import java.time.LocalTime
import javax.net.ssl.SSLSocket


private val log = KotlinLogging.logger {}
private val json = Json

class GestorClientes(private val cliente: SSLSocket, private val usersDb: UsersDb) : Runnable {

    // Preparamos los canales de entrada-salida
    private val salida = DataOutputStream(cliente.outputStream)
    private val entrada = DataInputStream(cliente.inputStream)

    // Preparo un Boolean que se volvera True si el Token ha caducado, lo que enviara el response adecuado al usuario
    private var tokenExpired = false

    override fun run() {
        // Leemos el request que envie el cliente y lo guardamos en una variable
        val request = lecturaRequest()
        // Comprobamos el token, si no existe, se comprueba que el request sea de tipo GET_TOKEN, si no se intentara verificar
        val permisos = comprobarToken(request)

        // Una vez comprobado el token, se opera segun el tipo de request
        if (!tokenExpired) {
            when (request.type) {
                Request.Type.LOGIN -> enviarToken(request)
                Request.Type.CONSULT -> consultarHora(permisos)
            }
        } else tokenExpiredSignal()
        cliente.close()
    }

    // Si cuenta con los permisos, se le envia la hora, si no, un simple aviso
    private fun consultarHora(permisos: Boolean) {
        log.debug { "\tEnviando la hora" }

        val response = if (!permisos) {
            log.debug { "No tiene permisos" }

            Response("No tiene permisos", Response.Type.ERROR)
        } else {
            log.debug { "Tiene permisos" }
            Response("${LocalTime.now()}", Response.Type.OK)
        }
        salida.writeUTF(json.encodeToString(response) + "\n")
    }

    // Si el token ha caducado, se ejecuta esta funcion.
    private fun tokenExpiredSignal() {
        log.debug { "Token caducado" }

        val response = Response("Token caducado", Response.Type.TOKEN_EXPIRED)
        salida.writeUTF(json.encodeToString(response) + "\n")
    }

    // Luego de comprobar que el usuario exista, se le envia el token.
    private fun enviarToken(request: Request<String>) {
        log.debug { "Procesando token..." }

        val user = usersDb.login(request.content!!, request.content2!!)

        val response = if (user == null) {
            log.debug { "Usario no encontrado" }
            Response(null, Response.Type.ERROR)
        } else {
            val token = ManejadorTokens.createToken(user.rol.rol)
            Response(token, Response.Type.OK)
        }
        salida.writeUTF(json.encodeToString(response) + "\n")
    }

    /*
    Haciendo uso de la clase ManejadorTokens, lo verificamos, y si ha sido posible, lo enviamos
    Hay que tener en cuenta que el request no sea del tipo GetToken para poder verificar que el token ha caducado y que
    simplemente el usuario solo intenta iniciar sesion

    Si eso ha sido posible, luego verificaremos el tipo de usuario, como solo es posible ser dos tipos en este problema
    el Boolean me sirve. Tendremos en cuenta el tipo de rol del usuario obteniendo el claim que se hizo.
     */
    private fun comprobarToken(request: Request<String>): Boolean {
        log.debug { "Comprobando token..." }

        var funcionDisponible = true

        val token = request.token?.let { ManejadorTokens.decodeToken(it) }

        if (token != null) {
            if (token.getClaim("rol").toString().contains(Usuario.TipoUser.USER.rol)) {
                funcionDisponible = false
            }
        } else if (request.type != Request.Type.LOGIN) {
            tokenExpired = true
        }
        return funcionDisponible
    }

    private fun lecturaRequest(): Request<String> {
        log.debug { "Procesando request..." }
        return json.decodeFromString(entrada.readUTF())
    }
}
