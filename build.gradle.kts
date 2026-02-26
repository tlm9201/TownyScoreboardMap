plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
}

group = "timo"
version = "1.0"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.TownyAdvanced:Towny:0.102.0.0")

    val scoreboardLibraryVersion = "2.6.0"
    implementation("net.megavex:scoreboard-library-api:$scoreboardLibraryVersion")
    runtimeOnly("net.megavex:scoreboard-library-implementation:$scoreboardLibraryVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<ProcessResources> {
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version,
            "name" to project.name
        )
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("net.megavex.scoreboardlibrary", "timo.townyscoreboardmap.libs.scoreboardlibrary")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
