import org.xml.sax.InputSource
import java.io.StringReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

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
    maven("https://repo.bsdevelopment.org/releases/")
    maven("https://jitpack.io/")
}

dependencies {
    compileOnly(libs.spigot)
    implementation(libs.minimaljson)
    implementation(libs.nbtapi)
    implementation(libs.paperlib)
    implementation(libs.scheduler)
    implementation("org.bsdevelopment.nbt:BSNbt:V1-B1")

    implementation(libs.dialogSpigot)
    implementation(libs.dialogPaper)

    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Use whichever MockBukkit coordinate you’re actually using.
    // (The README shows examples for both Maven Central + JitPack.) :contentReference[oaicite:3]{index=3}
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.101.0")

    // If you want paper-api on the test classpath, align it to MockBukkit’s manifest:
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

}

tasks {

    jar { enabled = false }

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
        relocate("io.github.projectunified.unidialog", "$mainPackage.libs.dialog")
    }

    test {
        useJUnitPlatform()
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
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
            // ✅ publish the SHADOW component so the POM doesn’t expose your shaded deps
            from(components["shadow"])

            // keep sources if you want them available in your repo
            artifact(tasks.named("sourcesJar"))
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

tasks.register("updateSpigotVersion") {
    group = "versioning"
    description = "→ Fetch latest CraftBukkit POM, bump libs.versions.toml spigot, and append to ServerVersion.java"

    doLast {
        // Use my script to get around cloudflare :P
        val pomUrl = "https://assets.bsdevelopment.org/scripts/spigot.php?path=SPIGOT/repos/craftbukkit/raw/pom.xml?at=refs%2Fheads%2Fmaster"
        val client  = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder().uri(URI.create(pomUrl)).GET().build()
        val pomXml = client.send(request, HttpResponse.BodyHandlers.ofString()).body()

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(pomXml)))

        // parse <version> and <minecraft_version>
        val fullVersion = doc.getElementsByTagName("version").item(0).textContent.trim()
        val nmsVersion = "v"+doc.getElementsByTagName("minecraft_version").item(0).textContent.trim()

        // Shorten the version, and split it into major minor and patch numbers
        val shortVersion = fullVersion.substringBefore("-")
        val (major, minor, patch) = shortVersion.split('.').map { it.toInt() }
        val fieldName = "v${major}_${minor}_${patch}"

        // Output results
        println("↳ BASE <version> → $shortVersion")
        println("↳ POM <version> → $fullVersion")
        println("↳ POM <minecraft_version> → $nmsVersion")

        // --- update libs.versions.toml ---
        file("gradle/libs.versions.toml").let { toml ->
            toml.writeText(
                toml.readText().replace(
                    Regex("""(?m)^(spigot\s*=\s*").*(")"""),
                    "$1$fullVersion$2"
                )
            )
        }
        println("Updated toml → spigot = \"$fullVersion\"")

        // --- Parse and update ServerVersion.java ---
        val javaPath = "src/main/java/${mainPackage.replace('.', '/')}/version/ServerVersion.java"
        val src      = file(javaPath).readText()
        val literalToField = Regex(
            """public\s+static\s+ServerVersion\s+(\w+)\s*=\s*register\(\s*Triple\.of\(\d+,\s*\d+,\s*\d+\),\s*"([^"]*)"\s*\)\s*;"""
        ).findAll(src).associate { it.groupValues[2] to it.groupValues[1] }

        // --- choose based on existing NMS reuse ---
        val registration = if (literalToField.containsKey(nmsVersion)) {
            // reuse the existing NMS version form previous version
            println("  ↳ Duplicate NMS → ${literalToField[nmsVersion]}")
            "public static ServerVersion $fieldName = register(Triple.of($major, $minor, $patch), ${literalToField[nmsVersion]});"
        } else {
            // new NMS version
            println("  ↳ New NMS → $nmsVersion")
            "public static ServerVersion $fieldName = register(Triple.of($major, $minor, $patch), \"$nmsVersion\");"
        }

        val marker = "// ---- AUTOMATION: END ---- //"
        val idx    = src.indexOf(marker).takeIf { it >= 0 } ?: error("Couldn't find '$marker' in $javaPath")
        val before = src.substring(0, idx)
        val after  = src.substring(idx)

        // update ServerVersion.java
        file(javaPath).writeText(buildString {
            append(before).append(registration).append("\n")
            append("    ").append(marker)
            append(after.removePrefix(marker))
        })

        println("Inserted: $registration")
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
