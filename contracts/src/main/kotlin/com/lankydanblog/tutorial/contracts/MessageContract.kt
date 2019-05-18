package com.lankydanblog.tutorial.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class MessageContract : Contract {
  companion object {
    val CONTRACT_ID = MessageContract::class.qualifiedName!!
  }

  interface Commands : CommandData {
    class Send : TypeOnlyCommandData(), Commands
    class Reply : TypeOnlyCommandData(), Commands
  }

  override fun verify(tx: LedgerTransaction) {
    val command = tx.commands.requireSingleCommand<Commands>()
    when (command.value) {
      is Commands.Send -> requireThat {
        "No inputs should be consumed when sending a message." using (tx.inputs.isEmpty())
        "Only one output state should be created when sending a message." using (tx.outputs.size == 1)
      }
      is Commands.Reply -> requireThat {
        "One input should be consumed when replying to a message." using (tx.inputs.size == 1)
        "Only one output state should be created when replying to a message." using (tx.outputs.size == 1)
      }
    }
  }
}