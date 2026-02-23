# BSPluginUtils
==========

Current Version: 0.1.15

Maven Dependency:
-----------------
```xml
<repository>
    <id>bs-repo-releases</id>
    <url>https://repo.bsdevelopment.org/releases/</url>
</repository>

<dependency>
    <groupId>org.bsdevelopment.pluginutils</groupId>
    <artifactId>BSPluginUtils</artifactId>
    <version>0.1.15</version>  <!-- This version is automatically updated -->
</dependency>
```

Gradle Dependency (Groovy DSL):
-------------------------------
```groovy
repositories {
    maven {
        url 'https://repo.bsdevelopment.org/releases'
    }
}

dependencies {
    implementation 'com.bsdevelopment.pluginutils:BSPluginUtils:0.1.15' // This version is automatically updated
}
```


Gradle Dependency (Kotlin DSL):
-------------------------------
```kotlin
repositories {
    maven("https://repo.bsdevelopment.org/releases")
}

dependencies {
    implementation("com.bsdevelopment.pluginutils:BSPluginUtils:0.1.15") // This version is automatically updated
}
```

Publishing
----------
To publish the artifact to the Maven repository, run:

>    ./gradlew publish

When you run the publish task, the following occurs:
1. Your artifact is published using the version defined in gradle.properties.
2. This README file is updated automatically – all occurrences of version numbers (for example, "0.1.15") are replaced with the published version.
3. The patch version in gradle.properties is incremented for the next release.

License
-------
This project is licensed under the MIT License.