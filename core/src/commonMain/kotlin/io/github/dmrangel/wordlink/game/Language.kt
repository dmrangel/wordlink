package io.github.dmrangel.wordlink.game

enum class Language(val code: String, val nativeName: String) {
    English("en", "English"),
    Portuguese("pt", "Português");

    companion object {
        fun fromCode(code: String?): Language? = entries.firstOrNull { it.code == code }
    }
}
