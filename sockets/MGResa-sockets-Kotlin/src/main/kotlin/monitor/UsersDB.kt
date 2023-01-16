package monitor

import com.toxicbakery.bcrypt.Bcrypt
import models.Usuario
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val log = KotlinLogging.logger {}

class UsersDb {

    private val listaUsuarios = mutableMapOf<Int, Usuario>()

    private var contadorId = AtomicInteger(0)

    // Lock
    private val lock = ReentrantLock()
    private val loginUser = lock.newCondition()
    private val addUser = lock.newCondition()

    private var escritor = false
    private var lector = AtomicInteger(0)

    fun register(item: Usuario) {
        lock.withLock {
            while (lector.toInt() > 0) {
                addUser.await()
            }
            escritor = true

            contadorId.incrementAndGet()
            item.id = contadorId.toInt()

            listaUsuarios[contadorId.toInt()] = item
            log.debug { "\tUsuario agregado -> $item" }

            escritor = false
            loginUser.signalAll()
        }

    }

    fun login(name: String, password: String): Usuario? {
        lock.withLock {
            while (escritor) {
                loginUser.await()
            }
            lector.incrementAndGet()
            var user: Usuario? = null

            listaUsuarios.forEach {
                if (it.value.nombre.equals(name, false) && Bcrypt.verify(password, it.value.password)) {
                    user = it.value
                    println(user)
                }
            }

            lector.decrementAndGet()
            addUser.signalAll()
            return user
        }
    }
}