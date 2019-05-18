package com.lankydanblog.tutorial.base.flows

import co.paralleluniverse.fibers.Suspendable
import com.lankydanblog.tutorial.contracts.MessageContract
import com.lankydanblog.tutorial.contracts.MessageContract.Commands.Send
import com.lankydanblog.tutorial.states.MessageState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
open class SendMessageFlow(private val message: MessageState) :
  FlowLogic<SignedTransaction>() {

  open fun extraTransactionValidation(transaction: TransactionBuilder) {
    // to be implemented by sub type flows - otherwise do nothing
  }

  @Suspendable
  final override fun call(): SignedTransaction {
    logger.info("Started sending message ${message.contents}")
    val tx = verifyAndSign(transaction())
    val sessions = listOf(initiateFlow(message.recipient))
    val stx = collectSignature(tx, sessions)
    return subFlow(FinalityFlow(stx, sessions)).also {
      logger.info("Finished sending message ${message.contents}")
    }
  }

  @Suspendable
  private fun collectSignature(
    transaction: SignedTransaction,
    sessions: List<FlowSession>
  ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))

  private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
    extraTransactionValidation(transaction)
    transaction.verify(serviceHub)
    return serviceHub.signInitialTransaction(transaction)
  }

  private fun transaction() =
    TransactionBuilder(notary()).apply {
      addOutputState(message, MessageContract.CONTRACT_ID)
      addCommand(Command(Send(), message.participants.map(Party::owningKey)))
    }

  private fun notary() = serviceHub.networkMapCache.notaryIdentities.first()
}

@InitiatedBy(SendMessageFlow::class)
open class SendMessageResponder(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

  open fun extraTransactionValidation(stx: SignedTransaction) {
    // to be implemented by sub type flows - otherwise do nothing
  }

  @Suspendable
  final override fun call(): SignedTransaction {
    val stx = subFlow(object : SignTransactionFlow(session) {
      override fun checkTransaction(stx: SignedTransaction) {
        extraTransactionValidation(stx)
      }
    })
    val tx = subFlow(
      ReceiveFinalityFlow(
        otherSideSession = session,
        expectedTxId = stx.id
      )
    )
    logger.info("Received transaction from finality")
    return tx
  }
}