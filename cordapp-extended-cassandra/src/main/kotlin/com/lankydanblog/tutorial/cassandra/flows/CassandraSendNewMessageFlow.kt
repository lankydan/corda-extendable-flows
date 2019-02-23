package com.lankydanblog.tutorial.cassandra.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import com.lankydanblog.tutorial.states.MessageState

@InitiatingFlow
@StartableByRPC
class CassandraSendNewMessageFlow(private val message: MessageState) : FlowLogic<SignedTransaction>() {
  @Suspendable
  override fun call(): SignedTransaction {
    return subFlow(
      CassandraSendMessageFlow(
        message
      )
    )
  }
}