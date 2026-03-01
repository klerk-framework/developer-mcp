val kotlin_version: String by project
val logback_version: String by project
val mcpVersion: String by project
val ktorVersion: String by project
val kotlin_logging_version: String by project

plugins {
    kotlin("jvm") version "2.3.10"
    id("io.ktor.plugin") version "3.2.3" // "3.4.0" // "3.0.3"
    kotlin("plugin.serialization") version "2.3.10"
}

group = "package dev.klerkframework.klerkmcp"
version = "0.0.1"

application {
    mainClass = "package dev.klerkframework.klerkmcp.ApplicationKt"
}

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk-server:${mcpVersion}")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-cio")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-sse")
    implementation("io.github.microutils:kotlin-logging-jvm:${kotlin_logging_version}")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

kotlin {
    jvmToolchain(17)
}
