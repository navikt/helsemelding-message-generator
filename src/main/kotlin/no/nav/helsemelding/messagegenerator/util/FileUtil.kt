package no.nav.helsemelding.messagegenerator.util

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.InputStream

private val log = KotlinLogging.logger {}

fun <R> readFile(fileName: String, transform: (BufferedReader) -> R): R? {
    val inputStream: InputStream? = object {}.javaClass.getResourceAsStream("/$fileName")

    if (inputStream == null) {
        log.error { "Could not load file $fileName" }
        return null
    }

    return inputStream.bufferedReader().use(transform)
}

fun readFileToList(fileName: String): List<String>? = readFile(fileName, BufferedReader::readLines)

fun readFileToString(fileName: String): String? = readFile(fileName, BufferedReader::readText)
