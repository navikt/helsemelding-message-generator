package no.nav.helsemelding.messagegenerator.model

import kotlin.uuid.Uuid

sealed class DialogMessage(
    open val id: String?,
    open val xml: String
)

data class ValidDialogMessage(
    val uuid: Uuid,
    override val xml: String
) : DialogMessage(uuid.toString(), xml)

data class InvalidDialogMessage(
    override val id: String?,
    override val xml: String
) : DialogMessage(id, xml)
