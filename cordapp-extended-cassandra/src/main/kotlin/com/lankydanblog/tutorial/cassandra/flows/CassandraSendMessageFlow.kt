package com.lankydanblog.tutorial.cassandra.flows

import com.lankydanblog.tutorial.cassandra.services.MessageRepository
import com.lankydanblog.tutorial.base.flows.SendMessageFlow
import com.lankydanblog.tutorial.base.flows.SendMessageResponder
import com.lankydanblog.tutorial.states.MessageState
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.SignedTransaction

@StartableByRPC
@StartableByService
class CassandraSendMessageFlow(private val message: MessageState) :
  SendMessageFlow(message) {

  override fun preTransactionBuild() {
    serviceHub.cordaService(MessageRepository::class.java).save(
      message,
      sender = true,
      committed = false
    )
    logger.info("Starting transaction for message: $message")
  }

  override fun preSignaturesCollected(transaction: SignedTransaction) {
    val keys = transaction.requiredSigningKeys - ourIdentity.owningKey
    logger.info("Collecting signatures from $keys for transaction for message: $message")
  }

  override fun postSignaturesCollected(transaction: SignedTransaction) {
    logger.info("Collected signatures for transaction for message: $message")
  }

  override fun postTransactionCommitted(transaction: SignedTransaction) {
    serviceHub.cordaService(MessageRepository::class.java).save(
      message,
      sender = true,
      committed = true
    )
    logger.info("Committed transaction for message: $message")
  }
}

@InitiatedBy(SendMessageFlow::class)
class CassandraSendMessageResponder(session: FlowSession) :
  SendMessageResponder(session) {

  override fun preTransactionSigned(transaction: SignedTransaction) {
    val message = transaction.coreTransaction.outputsOfType<MessageState>().single()
    serviceHub.cordaService(MessageRepository::class.java).save(
      message,
      sender = false,
      committed = false
    )
    logger.info("Received transaction with message: $message")
  }

  override fun postTransactionSigned(transaction: SignedTransaction) {
    val message = transaction.coreTransaction.outputsOfType<MessageState>().single()
    logger.info("Signed transaction for message: $message")
  }

  // i think its safe to give access to core transaction
  // kotlins immutable by default and that most of the values are `val` so cant be changed
  override fun postTransactionCommitted(transaction: SignedTransaction) {
    val message = transaction.coreTransaction.outputsOfType<MessageState>().single()
    serviceHub.cordaService(MessageRepository::class.java).save(
      message,
      sender = false,
      committed = true
    )
    logger.info("Committed transaction for message: $message")
  }
}