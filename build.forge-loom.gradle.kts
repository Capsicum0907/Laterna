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
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.parchmentmc.org/")
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${property("parchment_minecraft_version")}:${property("parchment_mappings_version")}@zip")
    })
    forge("net.minecraftforge:forge:${property("forge_version")}")
    compileOnly("org.jetbrains:annotations:24.1.0")
    annotationProcessor("com.pkware.jabel:jabel-javac-plugin:1.0.1-2")
    compileOnly("com.pkware.jabel:jabel-javac-plugin:1.0.1-2")
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
        exclude("**/laterna/LaternaTests.java")
        exclude("**/laterna/LaternaTags.java")
    }
    resources.srcDir(convertResources)
}

val devFix = sourceSets.create("devFix") {
    resources.srcDir(layout.projectDirectory.dir("devfix"))
}

loom {
    mods {
        maybeCreate("main").sourceSet(devFix)
    }
}

tasks.withType<net.fabricmc.loom.task.AbstractRunTask>().configureEach {
    dependsOn(devFix.processResourcesTaskName)
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

tasks.withType<JavaCompile>().configureEach {
    // Jabel lets the source use newer syntax while the bytecode stays at the Java this Minecraft runs on.
    javaCompiler = javaToolchains.compilerFor { languageVersion = JavaLanguageVersion.of(21) }
    sourceCompatibility = "21"
    options.release = Targets.java(minecraft)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}
