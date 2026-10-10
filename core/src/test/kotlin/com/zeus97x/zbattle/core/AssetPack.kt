package com.zeus97x.zbattle.core

import java.io.File

/** Locates ZBattle-ZPet-Assets; the Gradle test task passes it as a system property. */
object AssetPack {
    val root: File = File(System.getProperty("zbattle.assetPack") ?: "../ZBattle-ZPet-Assets").absoluteFile

    fun reference(name: String): String = File(root, "reference/$name").readText()

    /** Extracts the string literals of a Java array initializer named [field]. */
    fun javaStrings(source: String, field: String): List<String> {
        val start = source.indexOf("$field=").let { if (it < 0) source.indexOf("$field =") else it }
        require(start >= 0) { "Field $field not found" }
        val open = source.indexOf('{', start)
        var depth = 0
        var end = open
        for (i in open until source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) { end = i; break } }
            }
        }
        return Regex("\"([^\"]*)\"").findAll(source.substring(open, end)).map { it.groupValues[1] }.toList()
    }
}
