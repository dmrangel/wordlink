package io.github.dmrangel.wordlink.game

/** Reads app/src/main/assets/dict/<code>.txt, the single copy every platform builds from. */
expect fun readDictionary(code: String): String
