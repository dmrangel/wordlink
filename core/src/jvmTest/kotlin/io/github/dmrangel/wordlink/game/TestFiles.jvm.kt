package io.github.dmrangel.wordlink.game

import java.io.File

actual fun readDictionary(code: String): String {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
        val file = File(dir, "app/src/main/assets/dict/$code.txt")
        if (file.exists()) return file.readText()
        dir = dir.parentFile
    }
    error("dictionary $code not found")
}
