plugins {
    kotlin("jvm") version "1.9.22" // Versão estável para evitar conflitos no Render
    id("io.ktor.plugin") version "2.3.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application // ADICIONADO: Necessário para o ShadowJar saber o que rodar
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    // Servidor Netty
    implementation("io.ktor:ktor-server-core-jvm:2.3.10")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.10")

    // JSON
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.10")
    implementation("io.ktor:ktor-serialization-gson-jvm:2.3.10")

    // Conexão com Banco de Dados
    implementation("mysql:mysql-connector-java:8.0.33")
}

// CONFIGURAÇÃO DA CLASSE PRINCIPAL
application {
    mainClass.set("org.example.MainKt")
}

// CONFIGURAÇÃO DO ARQUIVO JAR PARA O DOCKER
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("app.jar")
    manifest {
        attributes["Main-Class"] = "org.example.MainKt"
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}