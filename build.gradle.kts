plugins {
    kotlin("jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // Servidor Netty (O motor da API)
    implementation("io.ktor:ktor-server-core-jvm:2.3.10")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.10")

    // JSON (Para o Retrofit entender os dados)
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.10")
    implementation("io.ktor:ktor-serialization-gson-jvm:2.3.10")

    // Conexão com Banco de Dados MySQL
    implementation("mysql:mysql-connector-java:8.0.33")

    implementation("org.mindrot:jbcrypt:0.4")
}

// Configuração da aplicação
application {
    mainClass.set("com.example.MainKt")
}

// Configuração do ShadowJar (Gera o arquivo único para o Docker)
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    // Define o nome do arquivo que o Dockerfile vai procurar
    archiveFileName.set("app.jar")

    // Resolve o erro de validação do Gradle 8.14 sem usar 'mainClassName' diretamente
    mergeServiceFiles()

    manifest {
        attributes["Main-Class"] = "org.example.MainKt"
    }
}

// Configuração do compilador para Java 17
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