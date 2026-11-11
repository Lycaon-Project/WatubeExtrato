/*
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

dependencies {
    // the JSON library (org.schabi.newpipe.extractor.json) now lives in the extractor
    implementation(project(":extractor"))
}
