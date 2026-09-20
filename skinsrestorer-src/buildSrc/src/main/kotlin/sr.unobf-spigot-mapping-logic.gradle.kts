plugins {
    java
    id("sr.formatting-logic")
    id("xyz.wagyourtail.jvmdowngrader")
}

// Create extension first, before applying the plugin
val mapping = extensions.create("mapping", MappingExtension::class.java)

plugins.apply(UnobfMappingPlugin::class.java)

dependencies {
    implementation(project(":multiver:bukkit:shared"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    downgradeJar {
        downgradeTo = JavaVersion.VERSION_17
        archiveClassifier = "downgraded-17"
    }
}

tasks.classes {
    finalizedBy(tasks.downgradeJar)
}

configurations {
    create("remapped") {
        isCanBeResolved = false
        isCanBeConsumed = true
        outgoing.artifact(tasks.downgradeJar.flatMap { it.archiveFile }) {
            builtBy(tasks.downgradeJar)
        }

        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }
}
