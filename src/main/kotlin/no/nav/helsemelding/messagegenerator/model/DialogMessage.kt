package no.nav.helsemelding.messagegenerator.model

import kotlin.uuid.Uuid

data class DialogMessage(
    val id: Uuid,
    val xml: String
)
