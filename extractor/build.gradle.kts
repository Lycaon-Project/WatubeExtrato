/*
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.google.protobuf)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vyarus.animalsniffer)
    `maven-publish`
}

/*
 * Compiled by the JDK 25 toolchain, but run by its consumers on Java 21 (the build and unit test
 * JVM of the Watube app) and turned into Android bytecode by D8: the class files are Java 21's,
 * for the Java (protobuf) and the Kotlin sources alike.
 */
private val bytecodeTarget = 21

tasks.withType<JavaCompile>().configureEach {
    options.release.set(bytecodeTarget)
}

kotlin {
    // A library: every public declaration must state its visibility and type on purpose
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(bytecodeTarget.toString()))
        allWarningsAsErrors = true
    }
}

// The library compiles against the JDK 25 API, but runs on Android (ART) from API 31, which
// lacks most Java 21+ APIs: fail the build when the code uses one of them. The signature is the
// Android API 26 one plus the APIs of desugar_jdk_libs_nio 2.x (the app's core library
// desugaring): stricter than API 31, so passing it is enough. No 31 signature with
// desugaring exists.
animalsniffer {
    toolVersion = libs.versions.animalsniffer.get()
    sourceSets = listOf(project.sourceSets.main.get())
    ignore = listOf(
        // Records are rewritten by D8, which the signature does not model
        "java.lang.Record",
        // Only reached by Java deserialization of the serializable channel tab builder
        // lambdas, which the app never does (it only puts enums in its Bundles)
        "java.lang.invoke.SerializedLambda",
    )
}

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    main {
        // the time ago patterns, generated in Kotlin by :timeago-generator
        kotlin.srcDir("../timeago-parser/src/main/kotlin")
    }
}

// Protobuf files would uselessly end up in the JAR otherwise, see
// https://github.com/google/protobuf-gradle-plugin/issues/390
tasks.jar {
    exclude("**/*.proto")
    includeEmptyDirs = false
}

tasks.test {
    // Test logging setup
    testLogging {
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }

    // Pass on downloader type to tests for different CI jobs. See DownloaderFactory.java and ci.yml
    if (System.getProperties().containsKey("downloader")) {
        systemProperty("downloader", System.getProperty("downloader"))
    }
    useJUnitPlatform()
}

dependencies {
    implementation(libs.jsoup)
    implementation(libs.google.protobuf)

    implementation(libs.mozilla.rhino.core)

    signature(variantOf(libs.android.api.signature) { classifier("coreLib2Nio"); artifactType("signature") })

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)

    testImplementation(libs.squareup.okhttp)
    testImplementation(libs.google.gson)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.lib.get()}"
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                named("java") {
                    option("lite")
                }
            }
        }
    }
}

// Run "./gradlew publishReleasePublicationToLocalRepository" to generate release JARs locally
publishing {
    publications {
        val mavenGroupId = "net.newpipe"
        val mavenArtifactId = "extractor"
        fun MavenPublication.setupPOM() = pom {
            name = "NewPipe Extractor"
            description = "A library for extracting data from streaming websites, used in NewPipe"
            url = "https://github.com/TeamNewPipe/NewPipeExtractor"

            licenses {
                license {
                    name = "GNU General Public License v3.0 or later"
                    url = "https://www.gnu.org/licenses/gpl-3.0.txt"
                }
            }

            scm {
                url = "https://github.com/TeamNewPipe/NewPipeExtractor"
                connection = "scm:git:git@github.com:TeamNewPipe/NewPipeExtractor.git"
                developerConnection = "scm:git:git@github.com:TeamNewPipe/NewPipeExtractor.git"
            }

            developers {
                developer {
                    id = "newpipe"
                    name = "Team NewPipe"
                    email = "team@newpipe.net"
                }
            }
        }

        create<MavenPublication>("release") {
            groupId = mavenGroupId
            artifactId = mavenArtifactId
            version = rootProject.version.toString()

            afterEvaluate {
                from(components["java"])
            }

            setupPOM()
        }
        repositories {
            maven {
                name = "local"
                url = uri(layout.buildDirectory.dir("maven"))
            }
        }
    }
}
