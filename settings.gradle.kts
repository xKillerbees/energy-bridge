
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.neoforged.net/releases")
    }
}
// Allow NeoGradle to add its Ivy repo; no repositoriesMode lock here.
rootProject.name = "energy-bridge"
