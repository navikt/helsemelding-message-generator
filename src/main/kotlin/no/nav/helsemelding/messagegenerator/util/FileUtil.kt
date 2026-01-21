package no.nav.helsemelding.messagegenerator.util

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.InputStream
import kotlin.io.bufferedReader

private val log = KotlinLogging.logger {}

fun readFileToList(fileName: String): List<String> {
    val inputStream: InputStream? = object {}.javaClass.getResourceAsStream("/$fileName")

    if (inputStream == null) {
        log.error { "Could not load file $fileName" }
        return emptyList()
    }

    return inputStream.bufferedReader().use(BufferedReader::readLines)
}

fun readFileToString(fileName: String): String? {
    val inputStream: InputStream? = object {}.javaClass.getResourceAsStream("/$fileName")

    if (inputStream == null) {
        log.error { "Could not load file $fileName" }
        return null
    }

    return inputStream.bufferedReader().use(BufferedReader::readText)
}
