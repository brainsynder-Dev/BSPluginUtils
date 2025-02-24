import java.util.*

plugins {
    id("java")
    id("maven-publish")
    alias(libs.plugins.shadow)
}
var mainPackage = "org.bsdevelopment.pluginutils"

group = mainPackage

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io/")
}

dependencies {
    compileOnly(libs.spigot)
    implementation(libs.minimaljson)
    implementation(libs.nbtapi)
    implementation(libs.paperlib)
    implementation(libs.scheduler)
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveBaseName.set("BSPluginUtils")
        archiveClassifier.set("")

        relocate("com.github.Anon8281.universalScheduler", "$mainPackage.libs.scheduler")
        relocate("io.papermc.lib", "$mainPackage.libs.paperlib")
        relocate("com.eclipsesource.json", "$mainPackage.libs.json")
        relocate("de.tr7zw.changeme.nbtapi", "$mainPackage.libs.nbtapi")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.register("cleanInstall") {
    dependsOn("clean", "build")
}

println("Username: " + findProperty("BS_REPO_USER"))
println("Password: " + findProperty("BS_REPO_PASS"))


tasks.publish {
    dependsOn("clean", "build")
    finalizedBy("updateReadmeVersion")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "bs-repo"
            url = uri("https://repo.bsdevelopment.org/releases")
            credentials {
                username = findProperty("BS_REPO_USER") as String?
                password = findProperty("BS_REPO_PASS") as String?
            }
        }
    }
}

// Task to increment the patch version in gradle.properties.
tasks.register("incrementPatchVersion") {
    group = "versioning"
    description = "Increments patch version in gradle.properties and commits changes to gradle.properties."

    doLast {
        // Locate the gradle.properties file in the root project.
        val propsFile = rootProject.file("gradle.properties")
        val props = Properties().apply {
            propsFile.inputStream().use { load(it) }
        }

        // Retrieve the current version (e.g., 1.2.3 or 1.2.3-SNAPSHOT).
        val currentVersion = props.getProperty("version") ?: run {
            println("No 'version' found in gradle.properties")
            return@doLast
        }

        // Regex to capture major.minor.patch with optional -SNAPSHOT.
        val pattern = Regex("""^(\d+)\.(\d+)\.(\d+)(-SNAPSHOT)?$""")
        val matchResult = pattern.matchEntire(currentVersion)
        if (matchResult != null) {
            val (major, minor, patch, snapshot) = matchResult.destructured
            val newPatch = patch.toInt() + 1
            val newVersion = if (snapshot.isNotEmpty()) {
                "$major.$minor.$newPatch-SNAPSHOT"
            } else {
                "$major.$minor.$newPatch"
            }

            // Update the version in gradle.properties.
            props.setProperty("version", newVersion)
            propsFile.outputStream().use { props.store(it, null) }
            println("Auto-incremented patch version from $currentVersion to $newVersion")


            // Execute Git commands to add, commit, and push the changes.
            listOf(
                listOf("git", "add", "gradle.properties"),
                listOf(
                    "git",
                    "commit",
                    "-m",
                    "chore: prepared for next release by incrementing patch version in gradle.properties to: $newVersion",
                    "gradle.properties"
                ),
                listOf("git", "push")
            ).forEach { cmd ->
                exec {
                    commandLine = cmd
                }
            }
        } else {
            println("WARNING: Version '$currentVersion' is not in 'X.Y.Z[-SNAPSHOT]' format. Skipping increment.")
        }
    }
}

// Task to update README.md with the new version and commit changes.
// This task runs the incrementPatchVersion task so that the version is prepared for the next release.
tasks.register("updateReadmeVersion") {
    group = "versioning"
    description = "Updates README.md with the new version and commits changes to README.md."
    finalizedBy("incrementPatchVersion")

    doLast {
        // Re-read gradle.properties to get the updated version.
        val props = Properties().apply {
            rootProject.file("gradle.properties").inputStream().use { load(it) }
        }
        val currentVersion = props.getProperty("version")
            ?: error("No 'version' property found in gradle.properties!")

        // Locate and update the README.md file.
        val readmeFile = file("README.md")
        if (!readmeFile.exists()) {
            throw GradleException("README.md file not found!")
        }
        // Replace the version string in README.md (adjust the regex if needed).
        val versionRegex = Regex("""\d+\.\d+\.\d+(?:-SNAPSHOT)?""")
        val updatedContent = readmeFile.readText().replace(versionRegex, currentVersion)
        readmeFile.writeText(updatedContent)
        println("README.md updated with version: $currentVersion")

        // Execute Git commands to add, commit, and push the changes.
        listOf(
            listOf("git", "add", "README.md"),
            listOf(
                "git",
                "commit",
                "-m",
                "docs: updated version listed in README.md file to: $currentVersion",
                "README.md"
            ),
            listOf("git", "push")
        ).forEach { cmd ->
            exec {
                commandLine = cmd
            }
        }
    }
}
