package io.github.dmrangel.wordlink.game

// The tests are ES modules, which have no require(); Node 22 offers this instead.
private val fs: dynamic = js("process.getBuiltinModule('fs')")
private val path: dynamic = js("process.getBuiltinModule('path')")

// Node runs the tests from somewhere under core/build, so search upwards.
actual fun readDictionary(code: String): String {
    var dir: String = js("process.cwd()") as String
    while (true) {
        val file = path.join(dir, "app", "src", "main", "assets", "dict", "$code.txt") as String
        if (fs.existsSync(file) as Boolean) return fs.readFileSync(file, "utf8") as String
        val parent = path.dirname(dir) as String
        if (parent == dir) error("dictionary $code not found")
        dir = parent
    }
}
