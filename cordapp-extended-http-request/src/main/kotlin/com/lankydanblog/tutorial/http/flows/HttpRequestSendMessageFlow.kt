package com.lankydanblog.tutorial.http.flows

import com.lankydanblog.tutorial.base.flows.SendMessageFlow
import com.lankydanblog.tutorial.base.flows.SendMessageResponder
import com.lankydanblog.tutorial.states.MessageState
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction

@StartableByRPC
@StartableByService
class HttpRequestSendMessageFlow(message: MessageState) :
  SendMessageFlow(message) {

  override fun preTransactionBuild() {
    super.preTransactionBuild()
  }

  override fun preSignaturesCollected(transaction: SignedTransaction) {
    super.preSignaturesCollected(transaction)
  }

  override fun postSignaturesCollected(transaction: SignedTransaction) {
    super.postSignaturesCollected(transaction)
  }

  override fun postTransactionCommitted(transaction: SignedTransaction) {
    super.postTransactionCommitted(transaction)
  }
}

@InitiatedBy(SendMessageFlow::class)
class CassandraSendMessageResponder(session: FlowSession) :
  SendMessageResponder(session) {

  override fun preTransactionSigned(transaction: SignedTransaction) {
    super.preTransactionSigned(transaction)
  }

  override fun postTransactionSigned(transaction: SignedTransaction) {
    super.postTransactionSigned(transaction)
  }

  override fun postTransactionCommitted(transaction: SignedTransaction) {
    super.postTransactionCommitted(transaction)
  }
}