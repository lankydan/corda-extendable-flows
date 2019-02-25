package com.lankydanblog.tutorial.http.flows

import com.lankydanblog.tutorial.base.flows.SendMessageFlow
import com.lankydanblog.tutorial.base.flows.SendMessageResponder
import com.lankydanblog.tutorial.http.services.MessageAcknowledger
import com.lankydanblog.tutorial.states.MessageState
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.SignedTransaction

@StartableByRPC
@StartableByService
class HttpRequestSendMessageFlow(private val message: MessageState) :
  SendMessageFlow(message) {

  override fun preTransactionBuild() {
    serviceHub.cordaService(MessageAcknowledger::class.java)
      .newMessageReceived(message, true)
  }

  // note that `preSignaturesCollected` is not overridden

  override fun postSignaturesCollected(transaction: SignedTransaction) {
    serviceHub.cordaService(MessageAcknowledger::class.java)
      .messageTransactionSigned(message, true)
  }

  override fun postTransactionCommitted(transaction: SignedTransaction) {
    serviceHub.cordaService(MessageAcknowledger::class.java)
      .messageTransactionCommitted(message, true)
  }
}

@InitiatedBy(SendMessageFlow::class)
class HttpRequestSendMessageResponder(session: FlowSession) :
  SendMessageResponder(session) {

  override fun postTransactionSigned(transaction: SignedTransaction) {
    val message = transaction.coreTransaction.outputsOfType<MessageState>().single()
    serviceHub.cordaService(MessageAcknowledger::class.java)
      .messageTransactionSigned(message, true)
  }

  override fun postTransactionCommitted(transaction: SignedTransaction) {
    val message = transaction.coreTransaction.outputsOfType<MessageState>().single()
    serviceHub.cordaService(MessageAcknowledger::class.java)
      .messageTransactionCommitted(message, true)
  }
}