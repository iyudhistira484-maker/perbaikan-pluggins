import com.github.jengelman.gradle.plugins.shadow.transformers.PreserveFirstFoundResourceTransformer

plugins {
    id("sr.base-logic")
    id("com.gradleup.shadow")
}

dependencies {
    api(projects.skinsrestorerBuildData)
    api(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerScissors)

    api(libs.gson)
    implementation(libs.mariadb.java.client) {
        exclude("com.github.waffle", "waffle-jna")
    }
    implementation(libs.postgresql)
    implementation(libs.hikari.cp)

    api(libs.configme)
    api(libs.injector) {
        exclude("javax.annotation")
    }

    api(libs.cloud.annotations)
    annotationProcessor(libs.cloud.annotations)
    implementation(libs.cloud.processors.requirements)
    implementation(libs.cloud.processors.cooldown)
    implementation(libs.cloud.brigadier)
    compileOnly(libs.brigadier)
    implementation(libs.cloud.translations.core)
    implementation(libs.cloud.minecraft.extras)
    implementation(libs.cloud.translations.minecraft.extras)
    api(libs.reflect)

    implementation(libs.bstats.base) {
        isTransitive = false
    }

    compileOnly(libs.spigot.api)
    compileOnly(libs.floodgate.api)

    api(libs.bundles.adventure.shared)
}

tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
        filesNotMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
        transform<PreserveFirstFoundResourceTransformer>()
        exclude("META-INF/annotations.shadow.kotlin_module")
        relocate("net.kyori", "net.skinsrestorer.shadow.kyori")
    }
}
