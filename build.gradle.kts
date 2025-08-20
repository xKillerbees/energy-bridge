
plugins {
    id("net.neoforged.gradle.userdev") version "7.0.152"
    id("maven-publish")
}

base { archivesName.set(providers.gradleProperty("archives_base_name")) }

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
}

dependencies {
    implementation("net.neoforged:neoforge:${providers.gradleProperty("neoforge_version").get()}")
}

tasks.processResources {
    val props = mapOf("version" to version)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
}
