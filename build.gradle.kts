import java.util.Properties

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

println("Username: "+findProperty("BS_REPO_USER"))
println("Password: "+findProperty("BS_REPO_PASS"))


// Ensure your 'version' is read from gradle.properties (default Gradle behavior):
println("Current version: $version")

// 1) Define a task to increment the patch version in gradle.properties
val incrementPatchVersion by tasks.registering {
    group = "versioning"
    description = "Increments patch version in gradle.properties"

    doLast {
        // Path to the root gradle.properties file
        val propsFile = project.rootProject.file("gradle.properties")

        // Load the current properties
        val props = Properties()
        propsFile.inputStream().use { props.load(it) }

        // Get the current version (e.g., 1.2.3 or 1.2.3-SNAPSHOT)
        val currentVersion = props.getProperty("version") ?: run {
            println("No 'version' found in gradle.properties")
            return@doLast
        }

        // Simple regex to capture major.minor.patch with optional -SNAPSHOT
        val pattern = Regex("""^(\d+)\.(\d+)\.(\d+)(-SNAPSHOT)?$""")
        val match = pattern.matchEntire(currentVersion)

        if (match != null) {
            val (major, minor, patch, snapshot) = match.destructured
            val newPatch = patch.toInt() + 1

            // If the old version had "-SNAPSHOT", keep it
            val newVersion = if (snapshot.isNotEmpty()) {
                "$major.$minor.$newPatch-SNAPSHOT"
            } else {
                "$major.$minor.$newPatch"
            }

            // Update gradle.properties with the new version
            props.setProperty("version", newVersion)
            propsFile.outputStream().use { props.store(it, null) }

            println("Auto-incremented patch version from $currentVersion to $newVersion")
        } else {
            println("WARNING: Version '$currentVersion' is not in 'X.Y.Z[-SNAPSHOT]' format. Skipping increment.")
        }
    }
}

tasks.publish {
    dependsOn("clean", "build")
    finalizedBy(incrementPatchVersion)
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