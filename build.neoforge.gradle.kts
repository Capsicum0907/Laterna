plugins {
    idea
    `java-library`
    `maven-publish`
    id("net.neoforged.moddev") version "2.0.91"
}

val modId = property("mod_id") as String
val minecraft = stonecutter.current.version
val loader = stonecutter.current.project.substringAfterLast('-')

version = property("mod_version") as String
group = property("mod_group_id") as String

repositories {
    mavenLocal()
}

base {
    archivesName = "$modId-$loader-$minecraft"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(Targets.java(minecraft))

neoForge {
    version = property("neo_version") as String

    parchment {
        mappingsVersion = property("parchment_mappings_version") as String
        minecraftVersion = property("parchment_minecraft_version") as String
    }

    runs {
        register("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        register("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        register("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        register("data") {
            data()
            systemProperty("laterna.icon", rootProject.file("branding/icon.png").absolutePath)
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", rootProject.file("src/generated/resources/").absolutePath,
                "--existing", rootProject.file("src/main/resources/").absolutePath,
            )
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

sourceSets.main {
    resources {
        srcDir(rootProject.file("src/generated/resources"))
        // The data run's per-provider hashes; without this they are packed into the jar.
        exclude(".cache/**")
    }
}

val localRuntime = configurations.create("localRuntime")
configurations.runtimeClasspath {
    extendsFrom(localRuntime)
}

val modMetadata = mapOf(
    "minecraft_version" to minecraft,
    "minecraft_version_range" to property("minecraft_version_range"),
    "neo_version" to property("neo_version"),
    "loader_dependency_range" to property("loader_dependency_range"),
    "loader_version_range" to property("loader_version_range"),
    "mod_id" to modId,
    "mod_name" to property("mod_name"),
    "mod_license" to property("mod_license"),
    "mod_version" to property("mod_version"),
    "mod_authors" to property("mod_authors"),
    "mod_description" to property("mod_description"),
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
neoForge.ideSyncTask(generateModMetadata)

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri(layout.projectDirectory.dir("repo"))
        }
    }
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
