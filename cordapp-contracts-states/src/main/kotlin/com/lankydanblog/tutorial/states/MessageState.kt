package com.lankydanblog.tutorial.states

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

data class MessageState(
    val sender: Party,
    val recipient: Party,
    val contents: String,
    override val linearId: UniqueIdentifier,
    override val participants: List<Party> = listOf(sender, recipient)
) : LinearState