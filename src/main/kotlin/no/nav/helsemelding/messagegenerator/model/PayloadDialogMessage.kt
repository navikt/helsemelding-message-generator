package no.nav.helsemelding.messagegenerator.model

import kotlin.uuid.Uuid

data class PayloadDialogMessage(
    val id: Uuid,
    val payload: ByteArray
)
