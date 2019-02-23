package com.lankydanblog.tutorial.cassandra.services

import com.datastax.driver.core.Session
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.eq
import com.datastax.driver.mapping.Mapper
import com.datastax.driver.mapping.annotations.ClusteringColumn
import com.datastax.driver.mapping.annotations.PartitionKey
import com.datastax.driver.mapping.annotations.Table
import com.lankydanblog.tutorial.states.MessageState
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import java.util.*

@CordaService
class MessageRepository(serviceHub: AppServiceHub) :
  SingletonSerializeAsToken() {

  private val session: Session
  private val mapper: Mapper<MessageEntity>

  init {
    val databaseService = serviceHub.cordaService(CassandraDatabaseService::class.java)
    session = databaseService.session
    mapper = databaseService.mappingManager.mapper(MessageEntity::class.java)
  }

  fun save(message: MessageState, sender: Boolean, committed: Boolean): MessageEntity {
    return MessageEntity(
      party = (if (sender) message.sender else message.recipient).toString(),
      id = message.linearId.id,
      content = message.contents,
      committed = committed
    ).also { mapper.save(it) }
  }

  fun findAllByParty(party: Party): List<MessageEntity> {
    val results = session.execute(
      QueryBuilder.select().from(TABLE).where(
        eq(PARTY, party.toString())
      )
    )
    return mapper.map(results).all()
  }

  private companion object {
    const val TABLE = "messages_by_party"
    const val PARTY = "party"
  }
}

@Table(name = "messages_by_party")
data class MessageEntity(
  @PartitionKey
  val party: String = "",
  @ClusteringColumn
  val id: UUID = UUID.randomUUID(),
  val content: String = "",
  val committed: Boolean = false
)