package com.lankydanblog.tutorial.base.flows

import com.lankydanblog.tutorial.states.MessageState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.identity.CordaX500Name
import net.corda.core.toFuture
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SendMessageFlowTest {

  private lateinit var mockNetwork: MockNetwork
  private lateinit var partyA: StartedMockNode
  private lateinit var partyB: StartedMockNode
  private lateinit var notaryNode: MockNetworkNotarySpec

  @Before
  fun setup() {
    notaryNode = MockNetworkNotarySpec(CordaX500Name("Notary", "London", "GB"))
    mockNetwork = MockNetwork(
      listOf(
        "com.lankydanblog"
      ),
      notarySpecs = listOf(notaryNode)
    )
    partyA =
      mockNetwork.createNode(
        MockNodeParameters(
          legalName = CordaX500Name(
            "PartyA",
            "Berlin",
            "DE"
          )
        )
      )

    partyB =
      mockNetwork.createNode(
        MockNodeParameters(
          legalName = CordaX500Name(
            "PartyB",
            "Berlin",
            "DE"
          )
        )
      )
    mockNetwork.runNetwork()
  }

  @After
  fun tearDown() {
    mockNetwork.stopNodes()
  }

  @Test
  fun `Responder does not have extra validation in it`() {
    val future = partyA.startFlow(
      SendMessageFlow(
        MessageState(
          contents = "hi",
          recipient = partyB.info.singleIdentity(),
          sender = partyA.info.singleIdentity(),
          linearId = UniqueIdentifier()
        )
      )
    )
    mockNetwork.runNetwork()
    future.get()
  }
}