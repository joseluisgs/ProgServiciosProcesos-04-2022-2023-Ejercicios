package server

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import common.Request
import common.Response
import common.TypeResponseEnum
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.LoginUser
import model.User
import mu.KotlinLogging
import repositories.UsersRepository
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.*

data class ClientControl(
    private val cliente: Socket
) {
    private var correcto = true
    private var json = Json
    private var logger = KotlinLogging.logger {}
    private var listaUsuarios = UsersRepository().list
    private var algoritmo = Algorithm.HMAC256("AlgoritmoParaElTokenRochiio")
    private var verificador = JWT.require(algoritmo).build()
    private var entrada :DataInputStream? = null
    private var salida :DataOutputStream? = null


     fun start(){
        correcto = true
        entrada = DataInputStream(cliente.inputStream)
        salida = DataOutputStream(cliente.outputStream)

        logger.debug { "Llegando datos de petición de Login" }
        var login = entrada?.readUTF()
        var requestLogin = json.decodeFromString<Request<LoginUser>>(login!!)
        respuestaLogin(requestLogin.data)
        logger.debug { "Enviando datos respuesta login" }

        if(correcto){
            logger.debug { "Llegando datos de la petición de tiempo" }
            var tiempo = entrada?.readUTF()
            var requestTiempo = json.decodeFromString<Request<String>>(tiempo!!)
            comprobarToken(requestTiempo.data)
            logger.debug { "Enviando datos respuesta tiempo" }

            salida!!.close()
            entrada!!.close()
            cliente.close()
        }

    }


    /**
     * Comprobar si el token es correcto y si tiene los permisos.
     */
    private fun comprobarToken(data: String) {
        var decoded = verificador.verify(data)
        if(decoded.expiresAt > Date(System.currentTimeMillis()) ) {

            if (decoded.getClaim("rol").asString() == "admin") {

                var responseTiempo = Response<String>(TypeResponseEnum.OK, LocalDateTime.now().toString())
                salida?.writeUTF((json.encodeToString(responseTiempo))+"\n")

            } else {
                sendIncorrect("No tienes permisos para esta acción")
            }
        }else{
            sendIncorrect("No tienes autorización")
        }
    }


    /**
     * Filtrado y enviado de las respuestas que puede tener al login
     */
    private fun respuestaLogin(userLogin: LoginUser) {
        var encontrado = listaUsuarios.firstOrNull { it.name == userLogin.name }
        if (encontrado == null){
            sendIncorrect("Usuario o contraseña incorrecto")
        }else{
            if (BCrypt.verifyer().verify(userLogin.password.toByteArray(StandardCharsets.UTF_8), encontrado.password).verified){
                crearToken(encontrado)
            }else{
                sendIncorrect("Usuario o contraseña incorrecto")
            }
        }
    }


    /**
     * Respuesta de error
     */
    private fun sendIncorrect(data: String) {
        var response = Response<String>(TypeResponseEnum.ERROR, data)
        logger.debug { "Enviando respuesta de error" }
        salida?.writeUTF((json.encodeToString(response))+"\n")

        correcto=false

        this.cliente.close()
        this.entrada!!.close()
        this.salida!!.close()
    }


    /**
     * Creamos el token y enviamos la respuesta
     */
    private fun crearToken(usuario: User) {
        val token = JWT.create()
            .withIssuer("Servidor")
            .withClaim("name", usuario.name)
            .withClaim("rol", usuario.rol)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 1000L))
            .withJWTId(UUID.randomUUID().toString())
            .sign(algoritmo)

        var response = Response<String>(TypeResponseEnum.OK, token)
        salida?.writeUTF((json.encodeToString(response))+"\n")
    }


}