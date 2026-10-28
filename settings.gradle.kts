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
    // auto-provisions the Java toolchains (JDK 17 here): no manual Jdk installation needed
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        // JitPack builds any GitHub repository on demand: only nanojson comes from it
        exclusiveContent {
            forRepository { maven(url = "https://jitpack.io") }
            filter { includeGroup("com.github.TeamNewPipe") }
        }
    }
}
include("extractor", "timeago-generator")
rootProject.name = "NewPipeExtractor"
