/*
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // auto-provisions the Java toolchains (JDK 25 here): no manual Jdk installation needed
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // nanojson, the only dependency from JitPack, is now part of the extractor
        // (org.schabi.newpipe.extractor.json): every dependency comes from Maven Central
        mavenCentral()
    }
}
include("extractor", "timeago-generator")
rootProject.name = "NewPipeExtractor"
