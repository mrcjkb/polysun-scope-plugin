import java.text.SimpleDateFormat
import java.util.*

plugins {
    eclipse
    idea
    `java-library`
}

description = "Plots the sensor inputs to a scope during the simulation"
group = "com.github.mrcjkb"

defaultTasks = mutableListOf("buildPlugin")

repositories {
    mavenCentral()
}

java {
    toolchain {
        // Do not use a newer Java version than Polysun uses.
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.BELLSOFT)
    }
}

// The polysun-plugin-if version. Keep this up to date for the latest Polysun version.
// The latest version can be found at: https://search.maven.org/artifact/com.velasolaris.polysun/polysun-plugin-if
val pluginIfVersion = "1.1.0"
dependencies {
    api("com.velasolaris.polysun:polysun-plugin-if:$pluginIfVersion")
    // For unit testing, please refer to the Gradle user manual: https://docs.gradle.org/current/userguide/java_testing.html
}

val javaVersion = System.getProperty("java.version")
val javaVendor = System.getProperty("java.vendor")
val javaVmVersion = System.getProperty("java.vm.version")
val osName = System.getProperty("os.name")
val osArchitecture = System.getProperty("os.arch")
val osVersion = System.getProperty("os.version")

tasks {
    register("buildPlugin", Jar::class) {
        from(configurations.runtimeClasspath.get()
        .onEach { println("add from dependencies: ${it.name}") }
        .map { if (it.isDirectory) it else zipTree(it) })
        archiveFileName.set("${rootProject.name}.jar")
        manifest {
            // The following fields are optional
            attributes["Library"] = rootProject.name
            attributes["Version"] = archiveVersion
            attributes["Website"] = "https://github.com/MrcJkb"
            attributes["Built-By"] = System.getProperty("user.name")
            attributes["Build-Timestamp"] = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date())
            attributes["Created-by"] = "Gradle ${gradle.gradleVersion}"
            attributes["Build-OS"] = "$osName $osArchitecture $osVersion"
            attributes["Build-Jdk"] = "$javaVersion ($javaVendor $javaVmVersion)"
            attributes["Build-OS"] = "$osName $osArchitecture $osVersion"
        }
    }
}
