package server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import common.Request
import common.Response
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.LoginUser
import models.Usuario
import mu.KotlinLogging
import repositories.UserRepositoryImpl
import services.comprobar
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.*

private val json = Json
private val logger = KotlinLogging.logger {}

data class GestorUsuarios(
    private val cliente: Socket
) {
    val repository = UserRepositoryImpl()
    val entrada = DataInputStream(cliente.inputStream)
    val salida = DataOutputStream(cliente.outputStream)
    private var algoritmo = Algorithm.HMAC256("TokenServerPSP")
    private var verify = JWT.require(algoritmo).build()
    fun start() {


        val jsonRequest = entrada.readUTF()
        val request = json.decodeFromString<Request<LoginUser>>(jsonRequest)
        logger.debug { "Recibido: $request" }
        comprobarLogin(request.content!!)


        logger.debug { "Llegando datos de la petición" }
        val tiempo = entrada.readUTF()
        val requestTiempo = json.decodeFromString<Request<String>>(tiempo)
        comprobarTiempo(requestTiempo.content!!)
        logger.debug { "Enviando datos respuesta tiempo" }

        salir()
    }

    private fun salir() {
        salida.close()
        entrada.close()
        cliente.close()
    }

    private fun comprobarTiempo(tiempo: String) {
        val decoded = verify.verify(tiempo)
        if(decoded.expiresAt > Date(System.currentTimeMillis()) ) {

            if (decoded.getClaim("rol").asString() == "admin") {
                val responseTiempo = Response<String>(LocalDateTime.now().toString(), Response.Type.OK)
                salida.writeUTF((json.encodeToString(responseTiempo))+"\n")

            } else {
                val responseError = Response<String>("No tienes permiso para realizar la tarea", Response.Type.ERROR)
                salida.writeUTF((json.encodeToString(responseError))+"\n")
                salir()
            }
        }else{
            val responseError = Response<String>("No autorizado", Response.Type.ERROR)
            salida.writeUTF((json.encodeToString(responseError))+"\n")
            salir()
        }
    }

    private fun comprobarLogin(usuario: LoginUser) {
        logger.debug { "Prueba:" + repository.lista }
        logger.debug {usuario}
        repository.lista.firstOrNull { it.username == usuario.username }?.let {
            val password = comprobar(usuario.password, it.password)
            if(password){
                logger.debug{"Correcto"}
                crearToken(it)
            }else{
                val response = Response<String>("Login incorrecto. Usuario o contrsaeña no válidos", Response.Type.ERROR)
                salida.writeUTF((json.encodeToString(response))+"\n")
            }
        }
    }

    private fun crearToken(usuario: Usuario) {
        val token = JWT.create()
            .withIssuer("Servidor")
            .withClaim("name", usuario.username)
            .withClaim("rol", usuario.rol)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 1000L))
            .withJWTId(UUID.randomUUID().toString())
            .sign(algoritmo)

        val response = Response<String>(token, Response.Type.OK)
        salida.writeUTF((json.encodeToString(response))+"\n")
    }


}