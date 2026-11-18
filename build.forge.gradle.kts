plugins {
    idea
    `java-library`
    id("net.neoforged.moddev.legacyforge") version "2.0.91"
}

val modId = property("mod_id") as String
val minecraft = stonecutter.current.version
val loader = stonecutter.current.project.substringAfterLast('-')

version = property("mod_version") as String
group = property("mod_group_id") as String

base {
    archivesName = "$modId-$loader-$minecraft"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(Targets.java(minecraft))

legacyForge {
    version = property("forge_version") as String

    parchment {
        mappingsVersion = property("parchment_mappings_version") as String
        minecraftVersion = property("parchment_minecraft_version") as String
    }

    runs {
        register("client") {
            client()
            systemProperty("forge.enabledGameTestNamespaces", modId)
        }

        register("server") {
            server()
            programArgument("--nogui")
            systemProperty("forge.enabledGameTestNamespaces", modId)
        }

        register("gameTestServer") {
            type = "gameTestServer"
            systemProperty("forge.enabledGameTestNamespaces", modId)
        }

        configureEach {
            // A forked JVM is not attached to the console, so on a Japanese Windows it falls back to CP932.
            systemProperty("file.encoding", "UTF-8")
            systemProperty("stdout.encoding", "UTF-8")
            systemProperty("stderr.encoding", "UTF-8")
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

val convertResources = tasks.register<ConvertResources>("convertResources") {
    source = rootProject.layout.projectDirectory.dir("src/generated/resources")
    minecraftVersion = minecraft
    loaderName = loader
    output = layout.buildDirectory.dir("generated/convertedResources")
}

sourceSets.main {
    java {
        exclude("**/laterna/data/**")
    }
    resources.srcDir(convertResources)
}

val modMetadata = mapOf(
    "minecraft_version" to minecraft,
    "minecraft_version_range" to property("minecraft_version_range"),
    "loader_dependency_range" to property("loader_dependency_range"),
    "loader_version_range" to property("loader_version_range"),
    "mod_id" to modId,
    "mod_name" to property("mod_name"),
    "mod_license" to property("mod_license"),
    "mod_version" to property("mod_version"),
    "mod_authors" to property("mod_authors"),
    "mod_description" to property("mod_description"),
    "resource_pack_format" to Targets.resourcePackFormat(minecraft),
    "data_pack_format" to Targets.dataPackFormat(minecraft),
)

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    inputs.properties(modMetadata)
    expand(modMetadata)
    from(rootProject.file("src/main/templates/$loader"))
    into("build/generated/sources/modMetadata")
}
sourceSets.main {
    resources.srcDir(generateModMetadata)
}
legacyForge.ideSyncTask(generateModMetadata)

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
