plugins {
    id("java")
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.spacerulerwill"
version = "0.1.0"

repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")

        // Avoid duplicate resource errors
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        // Recommended: prevent conflicts with other plugins
        relocate("com.fasterxml.jackson", "your.shaded.jackson")

        // Useful for libraries using ServiceLoader
        mergeServiceFiles()
    }

    jar {
        enabled = false
    }
}