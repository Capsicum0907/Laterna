pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
}

stonecutter {
    create(rootProject) {
        fun node(minecraft: String, loader: String) {
            version("$minecraft-$loader", minecraft).buildscript = "build.$loader.gradle.kts"
        }

        node("1.21.1", "neoforge")

        vcsVersion = "1.21.1-neoforge"
    }
}
