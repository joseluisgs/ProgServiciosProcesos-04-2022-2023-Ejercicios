# ENUNCIADO

En un servidor tenemos un usuario llamado `"pepe"` con contraseña `"pepe1234"` almacenada con **Bcrypt**. 

Podeis usar una lista con un objeto del tipo user. Lo importante es que el password esté codificado con **Bcrypt**, podeis hacerlo una ves arranque el servidor.

La conexión es segura con `TSL/AES`, por lo que necesitamos claves, certificados y llaveros para el cliente y el servidor.
Además en el usuario `pepe`, vamos a meterle un campo de rol, y tendrá el valor **"admin"**.
> data class User(val username: String, val password: String, val rol: String)

> val user = User("pepe", "lo que sea que de Bcrypt", "admin)

El usuario se conecta al servidor con un **Request** de tipo `LOGIN` y le pasa el usuario y el password, es decir, `"pepe"` y `"pepe1234"`, como ya está cifrado por TSL no hay problemas por la contraseña.

El servidor recibe el paquete, toma la contraseña y usando **Bcrypt** compara la contraseña `"pepe1234"` con lo que tiene el objeto user.

- Si hay error, manda **Response** del tipo `Error` al usuario con el texto, nombre de usuario o contraseña no válida.

- Si es correcto, genera un **JWT** token con los datos del usuario, es decir, pepe y "admin" y de tiempo de expiración 60 segundos y se lo manda en un **Response** al Cliente.

El cliente recibe el token y lo almacena. Le hace una petición **Request** con la hora y le añade el token.

Lo recibe el servidor:
- Si el toquen es válido, no ha caducado y su permiso es admin, recibe un response con la hora del sistema.
- Si hay algún error, recibe un error con NO AUTORIZADO.

<kbd>Ejercicio realizado por Alejandro Sánchez Monzón.</kbd>