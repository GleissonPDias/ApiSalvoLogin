plugins {
    kotlin("jvm") version "2.2.0"
    id("io.ktor.plugin") version "2.3.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
// Servidor Netty (O "motor" da API)
    implementation("io.ktor:ktor-server-core-jvm:2.3.10")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.10")

    // JSON (Para o Retrofit entender)
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.10")
    implementation("io.ktor:ktor-serialization-gson-jvm:2.3.10")

    // Conexão com Banco de Dados (Exemplo MySQL)
    implementation("mysql:mysql-connector-java:8.0.33")
}
// Isso permite rodar a API clicando no "Play"
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions{
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}