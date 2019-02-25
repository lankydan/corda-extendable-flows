package com.lankydanblog.tutorial.states

import com.lankydanblog.tutorial.contracts.MessageContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(MessageContract::class)
data class MessageState(
  val sender: Party,
  val recipient: Party,
  val contents: String,
  override val linearId: UniqueIdentifier,
  override val participants: List<Party> = listOf(sender, recipient)
) : LinearState