package no.nav.helsemelding.messagegenerator.util

import arrow.core.fold
import kotlin.collections.component1
import kotlin.collections.component2

fun replaceInTemplate(template: String, params: Map<String, String>): String =
    params.fold(template) { acc, (key, value) ->
        acc.replace(key, value)
    }
