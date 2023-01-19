import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.20"
    application
}

group = "me.mohamed"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    //serialización
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    //corrutinas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    //ktor network
    implementation("io.ktor:ktor-network:2.1.3")
    //logger
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.3")
    //bcrypt
    implementation("at.favre.lib:bcrypt:0.9.0")
    //jwt
    implementation("com.auth0:java-jwt:4.2.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}