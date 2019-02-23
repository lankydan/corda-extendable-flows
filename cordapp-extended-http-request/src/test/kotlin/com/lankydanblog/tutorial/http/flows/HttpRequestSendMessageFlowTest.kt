package com.lankydanblog.tutorial.http.flows

import com.lankydanblog.tutorial.states.MessageState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class HttpRequestSendMessageFlowTest {

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
          TestCordapp.findCordapp("com.lankydanblog.tutorial.http")
            .withConfig(
              mapOf(
                "messages_request_path_base" to "localhost:8080",
                "messages_request_path_new" to "new",
                "messages_request_path_signed" to "signed",
                "messages_request_path_committed" to "committed"
              )
            ),
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

  @Test
  fun `Flow runs without errors`() {
    val future1 = partyA.startFlow(
      HttpRequestSendMessageFlow(
        MessageState(
          contents = "hi",
          recipient = partyB.info.singleIdentity(),
          sender = partyA.info.singleIdentity(),
          linearId = UniqueIdentifier()
        )
      )
    )
    mockNetwork.runNetwork()
    println("done: ${future1.get()}")
  }
}