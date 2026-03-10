plugins {
    java
    alias(libs.plugins.lavalink)
}

group = "com.catlava"
version = "1.0.0"

lavalinkPlugin {
    name = "catlava"
    apiVersion = libs.versions.lavalink.api
    serverVersion = libs.versions.lavalink.server
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.lavalink.dev/releases") }
    maven { url = uri("https://maven.lavalink.dev/snapshots") }
}

dependencies {
    // Lavalink API - using version from libs.versions.toml
    implementation("dev.arbjerg:lavalink-api:${libs.versions.lavalink.api.get()}")
    
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    
    // Spring Boot (provided by Lavalink)
    compileOnly("org.springframework.boot:spring-boot-starter:3.1.5")
    compileOnly("org.springframework:spring-core:6.0.11")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // Annotations
    implementation("org.jetbrains.annotations:annotations:24.0.1")
    
    // Lombok (optional - for reducing boilerplate)
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

