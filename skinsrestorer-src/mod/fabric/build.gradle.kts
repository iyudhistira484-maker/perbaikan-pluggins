plugins {
    id("sr.base-logic")
    id("com.gradleup.shadow")
    id("dev.architectury.loom-no-remap") version "1.17.480"
}

base {
    archivesName = "SkinsRestorer-Mod-Fabric"
}

loom {
    silentMojangMappingsLicense()

    accessWidenerPath = project(":skinsrestorer-mod-common").file("src/main/resources/skinsrestorer.accesswidener")
}

val shadowBundle: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    minecraft("net.minecraft:minecraft:${rootProject.property("modMcVersion")}")

    implementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")

    // Fabric API
    implementation("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_api_version")}")
    include("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_api_version")}")

    // Cloud command framework for Fabric
    implementation("org.incendo:cloud-fabric:${rootProject.property("cloud_fabric_version")}")
    include("org.incendo:cloud-fabric:${rootProject.property("cloud_fabric_version")}")

    // Fabric permissions API
    implementation("me.lucko:fabric-permissions-api:${rootProject.property("fabric_permissions_api_version")}")
    include("me.lucko:fabric-permissions-api:${rootProject.property("fabric_permissions_api_version")}")

    implementation(project(path = ":skinsrestorer-mod-common")) { isTransitive = false }
    shadowBundle(
        project(
            path = ":skinsrestorer-mod-common",
        )
    ) { isTransitive = false }

    // Shared project dependencies - added to common for compile-time and shadowBundle for packaging
    setOf(
        projects.skinsrestorerShared,
        projects.multiver.miniplaceholders,
        projects.multiver.viaversion
    ).forEach {
        implementation(it) {
            exclude("org.slf4j")
            exclude("com.google.code.gson")
            exclude("com.google.errorprone")
        }
        shadowBundle(it) {
            exclude("org.slf4j")
            exclude("com.google.code.gson")
            exclude("com.google.errorprone")
        }
    }
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to inputs.properties["version"]))
    }

    from(loom.accessWidenerPath) {
        into("/")
    }
}

tasks.shadowJar {
    val mainOutputDirectories = sourceSets.main.get().output.files.map { it.toPath().toAbsolutePath().normalize() }

    dependsOn(tasks.jar)
    from(zipTree(tasks.jar.flatMap { it.archiveFile }))
    from(rootProject.layout.projectDirectory.file("LICENSE"))
    eachFile {
        val sourcePath = file.toPath().toAbsolutePath().normalize()
        if (mainOutputDirectories.any(sourcePath::startsWith)) {
            exclude()
        }
    }

    configurations = listOf(shadowBundle)
    archiveClassifier.set("")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
}

tasks.jar {
    archiveClassifier.set("raw")
}
