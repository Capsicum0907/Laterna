pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
}

stonecutter {
    create(rootProject) {
        fun node(minecraft: String, loader: String, toolchain: String = loader) {
            version("$minecraft-$loader", minecraft).buildscript = "build.$toolchain.gradle.kts"
        }

        node("1.16.5", "forge", toolchain = "forge-loom")
        node("1.18.2", "forge")
        node("1.19.2", "forge")
        node("1.20.1", "forge")
        node("1.21.1", "neoforge")
        node("1.21.1", "fabric")

        vcsVersion = "1.21.1-neoforge"
    }
}
