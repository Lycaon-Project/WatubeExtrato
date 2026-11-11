/*
 * SPDX-FileCopyrightText: 2011 The nanojson Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Lightweight JSON input/output library, written in Kotlin.
 *
 * <p>
 * For reading JSON, see {@link org.schabi.newpipe.extractor.json.JsonParser}. For writing JSON,
 * see {@link org.schabi.newpipe.extractor.json.JsonWriter}.
 * </p>
 *
 * <p>
 * NOTICE: this package is a Kotlin port of nanojson, Copyright 2011 The nanojson Authors,
 * licensed under the Apache License, Version 2.0 (https://github.com/mmastrac/nanojson; ported
 * from the TeamNewPipe fork, commit e9d656ddb49a412a5a0a5d5ef20ca7ef09549996). It was modified
 * for WatubeExtrato: iterative parser with a nesting limit, bounded numbers, and exceptions
 * that never quote the parsed text.
 * </p>
 */
package org.schabi.newpipe.extractor.json;
