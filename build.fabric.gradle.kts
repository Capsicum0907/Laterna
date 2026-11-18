import groovy.json.JsonOutput

plugins {
    `java-library`
    id("dev.architectury.loom") version "1.17.493"
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

repositories {
    maven("https://maven.parchmentmc.org/")
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${property("parchment_minecraft_version")}:${property("parchment_mappings_version")}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    compileOnly("org.jetbrains:annotations:24.1.0")
    if (Targets.java(minecraft) < 16) {
        annotationProcessor("com.pkware.jabel:jabel-javac-plugin:1.0.1-2")
        compileOnly("com.pkware.jabel:jabel-javac-plugin:1.0.1-2")
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

loom {
    runs {
        register("gameTestServer") {
            server()
            name("Game Test Server")
            property("fabric-api.gametest")
            property("fabric-api.gametest.report-file", layout.buildDirectory.file("gametest/report.xml").get().asFile.absolutePath)
            runDir("build/gametest")
        }
    }
}

val modMetadata = mapOf(
    "minecraft_version_range" to property("minecraft_version_range"),
    "loader_dependency_range" to property("loader_dependency_range"),
    "loader_version_range" to property("loader_version_range"),
    "java_version" to Targets.java(minecraft),
    "mod_id" to modId,
    "mod_name" to property("mod_name"),
    "mod_license" to property("mod_license"),
    "mod_version" to property("mod_version"),
    "mod_authors" to JsonOutput.toJson(property("mod_authors")),
    "mod_description" to JsonOutput.toJson(property("mod_description")),
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

tasks.withType<JavaCompile>().configureEach {
    javaCompiler = javaToolchains.compilerFor { languageVersion = JavaLanguageVersion.of(21) }
    sourceCompatibility = "21"
    options.release = Targets.java(minecraft)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}
