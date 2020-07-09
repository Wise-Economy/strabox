import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.3.72"
}

version = "1.0.0"
group = "com.wiseeconomy"

application {
    mainClass.set("com.wiseeconomy.MainKt")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.http4k", "http4k-core", "3.252.0")
    implementation("org.http4k", "http4k-contract", "3.252.0")
    implementation("org.http4k", "http4k-server-jetty", "3.252.0")
    implementation("org.http4k", "http4k-client-okhttp", "3.252.0")
    implementation("org.http4k", "http4k-format-jackson", "3.252.0")
    implementation("org.http4k", "http4k-metrics-micrometer", "3.252.0")
    implementation("org.http4k", "http4k-client-apache", "3.252.0")
    implementation("io.github.microutils", "kotlin-logging", "1.7.10")
    implementation("org.postgresql", "postgresql", "42.2.14")
    implementation("org.flywaydb", "flyway-core", "6.5.0")
    implementation("org.jetbrains.exposed", "exposed-core", "0.24.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.24.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.24.1")
    implementation("org.jetbrains.exposed", "exposed-java-time", "0.24.1")
    implementation("com.zaxxer", "HikariCP", "3.4.5")
    implementation("com.sksamuel.hoplite", "hoplite-hocon", "1.3.1")
    implementation("org.slf4j", "slf4j-nop", "1.7.30")
    implementation("org.webjars", "swagger-ui", "3.28.0")
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    implementation("ch.qos.logback", "logback-core", "1.2.3")
    implementation("org.slf4j", "slf4j-api", "1.7.30")
}
