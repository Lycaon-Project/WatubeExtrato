# Third-party notices

WatubeExtrato is licensed under the GNU General Public License v3.0 or later (see `LICENSE`).
It includes the third-party code below, under its own license.

## nanojson

- Package: `org.schabi.newpipe.extractor.json`
- Copyright 2011 The nanojson Authors
- License: Apache License, Version 2.0 (full text in
  `extractor/src/main/resources/META-INF/LICENSE-nanojson`, also shipped in the JAR)
- Source: https://github.com/mmastrac/nanojson, ported from the TeamNewPipe fork,
  commit `e9d656ddb49a412a5a0a5d5ef20ca7ef09549996`
- Changes: ported to Kotlin; iterative parser with a nesting limit, bounded numbers, and
  exceptions that never quote the parsed text.
