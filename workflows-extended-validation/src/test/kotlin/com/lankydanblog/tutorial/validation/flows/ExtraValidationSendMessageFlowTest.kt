package com.lankydanblog.tutorial.validation.flows

import com.lankydanblog.tutorial.base.flows.SendMessageFlow
import com.lankydanblog.tutorial.base.flows.SendMessageResponder
import com.lankydanblog.tutorial.states.MessageState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.IllegalArgumentException
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class validationSendMessageFlowTest {

  private lateinit var mockNetwork: MockNetwork
  private lateinit var partyA: StartedMockNode
  private lateinit var partyB: StartedMockNode
  private lateinit var notaryNode: MockNetworkNotarySpec

  @Before
  fun setup() {
    notaryNode = MockNetworkNotarySpec(CordaX500Name("Notary", "London", "GB"))
    mockNetwork = MockNetwork(
      MockNetworkParameters(
        notarySpecs = listOf(notaryNode),
        cordappsForAllNodes = listOf(
          TestCordapp.findCordapp("com.lankydanblog.tutorial.validation"),
          TestCordapp.findCordapp("com.lankydanblog.tutorial.base"),
          TestCordapp.findCordapp("com.lankydanblog.tutorial.states")
        )
      )
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

  // extra validation responder is registered by default
  // need to manually set the original responder to run test properly
  @Test
  fun `Extra validation flow validates valid transaction`() {
    partyB.registerInitiatedFlow(SendMessageResponder::class.java)
    val future = partyA.startFlow(
      ExtraValidationSendMessageFlow(
        MessageState(
          contents = "hi I love Corda",
          recipient = partyB.info.singleIdentity(),
          sender = partyA.info.singleIdentity(),
          linearId = UniqueIdentifier()
        )
      )
    )
    mockNetwork.runNetwork()
    future.get()
  }

  @Test
  fun `Extra validation flow throws error due to extra validation`() {
    partyB.registerInitiatedFlow(SendMessageResponder::class.java)
    val future = partyA.startFlow(
      ExtraValidationSendMessageFlow(
        MessageState(
          contents = "hi",
          recipient = partyB.info.singleIdentity(),
          sender = partyA.info.singleIdentity(),
          linearId = UniqueIdentifier()
        )
      )
    )
    mockNetwork.runNetwork()
    val e = assertFailsWith<IllegalArgumentException> {
      future.getOrThrow()
    }
    assertEquals(
      "Failed requirement: Message must contain the secret passphrase", e.message
    )
  }

  @Test
  fun `Extra validation responder validates valid transaction`() {
    partyB.registerInitiatedFlow(ExtraValidationSendMessageResponder::class.java)
    val future = partyA.startFlow(
      SendMessageFlow(
        MessageState(
          contents = "hi I love Corda",
          recipient = partyB.info.singleIdentity(),
          sender = partyA.info.singleIdentity(),
          linearId = UniqueIdentifier()
        )
      )
    )
    mockNetwork.runNetwork()
    future.get()
  }

  @Test
  fun `Extra validation responder throws error due to extra validation`() {
    partyB.registerInitiatedFlow(ExtraValidationSendMessageResponder::class.java)
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
    val e = assertFailsWith<FlowException> {
      future.getOrThrow()
    }
    assertEquals(
      "java.lang.IllegalArgumentException: Failed requirement: Message must contain the secret passphrase", e.cause!!.message
    )
  }
}