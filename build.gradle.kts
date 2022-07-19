plugins {
    id("fabric-loom").version("0.12-SNAPSHOT")
}

val archivesBaseName: String by project
base.archivesName.set(archivesBaseName)
val modVersion: String by project
version = modVersion
val mavenGroup: String by project
group = mavenGroup
val modId = archivesBaseName.toLowerCase()

repositories {}

dependencies {
    // Minecraft and mappings
    val minecraftVersion: String by project
    minecraft("com.mojang", "minecraft", minecraftVersion)
    val yarnMappings: String by project
    mappings("net.fabricmc", "yarn", yarnMappings, null, "v2")
    val loaderVersion: String by project
    modImplementation("net.fabricmc", "fabric-loader", loaderVersion)

    // Fabric API
    val fabricVersion: String by project
    modImplementation("net.fabricmc.fabric-api", "fabric-api", fabricVersion)
}

tasks {
    // Minecraft 1.19 uses Java 17.
    val javaVersion = JavaVersion.VERSION_17
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }

    jar { from("LICENSE") { rename { "${it}_${base.archivesName}" } } }

    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(
                mutableMapOf(
                    "version" to project.version,
                    "group" to group,
                    "modId" to modId,
                    "modName" to base.archivesName
                )
            )
        }
    }

    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion

        // Generate sources jar with Loom in addition to remapped.
        withSourcesJar()
    }
}
