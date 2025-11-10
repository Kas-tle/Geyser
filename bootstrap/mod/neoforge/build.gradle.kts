plugins {
    id("geyser.modded-conventions")
    id("geyser.modrinth-uploading-conventions")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}


val commonBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations {
    get("developmentNeoForge").extendsFrom(commonBundle)
}

// This is provided by "org.cloudburstmc.math.mutable" too, so yeet.
// NeoForge's class loader is *really* annoying.
provided("org.cloudburstmc.math", "api")
provided("com.google.errorprone", "error_prone_annotations")

// Jackson shipped by Minecraft is too old, so we shade & relocate our newer version
relocate("com.fasterxml.jackson")

dependencies {
    // See https://github.com/google/guava/issues/6618
    modules {
        module("com.google.guava:listenablefuture") {
          replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }

    neoForge(libs.neoforge.minecraft)

    commonBundle(project(":mod", configuration = "namedElements")) { isTransitive = false }

    api(project(":mod", configuration = "namedElements"))
    shadowBundle(project(path = ":mod", configuration = "transformProductionNeoForge"))
    shadowBundle(projects.core)

    // Minecraft (1.21.2+) includes jackson. But an old version!
    shadowBundle(libs.jackson.core)
    shadowBundle(libs.jackson.databind)
    shadowBundle(libs.jackson.dataformat.yaml)
    shadowBundle(libs.jackson.annotations)

    // Let's shade in our own api
    shadowBundle(projects.api)

    // cannot be shaded, since neoforge will complain if floodgate-neoforge tries to provide this
    include(projects.common)

    // Include all transitive deps of core via JiJ
    includeTransitive(projects.core)

    modImplementation(libs.cloud.neoforge)
    include(libs.cloud.neoforge)
}

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.neoforge.GeyserNeoForgeMain"
    manifest.attributes["Implementation-Version"] = project.version
}



tasks {
    remapJar {
        archiveBaseName.set("Geyser-NeoForge")
    }

    remapModrinthJar {
        archiveBaseName.set("geyser-neoforge")
    }

    shadowJar {
        // Without this, jackson's service files are not relocated
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        mergeServiceFiles()
    }
}

tasks.named<ProcessResources>("processResources") {
    from(project(":core").sourceSets.main.get().resources)
}

modrinth {
    loaders.add("neoforge")
    uploadFile.set(tasks.getByPath("remapModrinthJar"))
}
