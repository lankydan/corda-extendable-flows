package com.lankydanblog.tutorial.validation.flows

import com.lankydanblog.tutorial.base.flows.SendMessageFlow
import com.lankydanblog.tutorial.base.flows.SendMessageResponder
import com.lankydanblog.tutorial.states.MessageState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.transactions.BaseTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

class ExtraValidationSendMessageFlow(message: MessageState) :
  SendMessageFlow(message) {

  override fun extraTransactionValidation(transaction: TransactionBuilder) {
    validate(transaction.toLedgerTransaction(serviceHub))
    requireThat {
      val messages = transaction.toLedgerTransaction(serviceHub).outputsOfType<MessageState>()
      "The must only one output message" using (messages.size == 1)
      val message = messages.single()
      "Message must contain the secret passphrase" using (message.contents.contains("I love Corda"))
    }
  }
}

private fun validate(transaction: BaseTransaction) {
  requireThat {
    val messages = transaction.outputsOfType<MessageState>()
    "The must only one output message" using (messages.size == 1)
    val message = messages.single()
    "Message must contain the secret passphrase" using (message.contents.contains("I love Corda"))
  }
}

@InitiatedBy(SendMessageFlow::class)
class ExtraValidationSendMessageResponder(session: FlowSession) :
  SendMessageResponder(session) {

  override fun extraTransactionValidation(stx: SignedTransaction) {
    validate(stx.coreTransaction)
    requireThat {
      val messages = stx.coreTransaction.outputsOfType<MessageState>()
      "The must only one output message" using (messages.size == 1)
      val message = messages.single()
      "Message must contain the secret passphrase" using (message.contents.contains("I love Corda"))
    }
  }
}