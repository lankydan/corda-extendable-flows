package com.lankydanblog.tutorial.cassandra.flows

import com.lankydanblog.tutorial.cassandra.services.MessageRepository
import com.lankydanblog.tutorial.states.MessageState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class CassandraSendMessageFlowTest {

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
          TestCordapp.findCordapp("com.lankydanblog.tutorial.cassandra")
            .withConfig(
              mapOf(
                "cassandra_host" to "localhost",
                "cassandra_port" to 9042,
                "cassandra_cluster" to "cluster_name",
                "cassandra_keyspace" to "corda_testing"
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
      CassandraSendMessageFlow(
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
    println(
      partyA.services.cordaService(MessageRepository::class.java).findAllByParty(
        partyB.info.singleIdentity()
      )
    )
  }
}