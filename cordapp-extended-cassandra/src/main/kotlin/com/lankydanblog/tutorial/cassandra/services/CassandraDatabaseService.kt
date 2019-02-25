package com.lankydanblog.tutorial.cassandra.services

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session
import com.datastax.driver.core.schemabuilder.SchemaBuilder.createKeyspace
import com.datastax.driver.mapping.DefaultNamingStrategy
import com.datastax.driver.mapping.DefaultPropertyMapper
import com.datastax.driver.mapping.MappingConfiguration
import com.datastax.driver.mapping.MappingManager
import com.datastax.driver.mapping.NamingConventions.LOWER_CAMEL_CASE
import com.datastax.driver.mapping.NamingConventions.LOWER_SNAKE_CASE
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils.normalizeSpace
import java.nio.charset.Charset
import java.util.*

@CordaService
class CassandraDatabaseService(private val serviceHub: AppServiceHub) :
  SingletonSerializeAsToken() {

  lateinit var session: Session
  lateinit var mappingManager: MappingManager

  init {
    log.info("Initiating connection to Cassandra")
    connect()
    log.info("Finished connecting to Cassandra")
  }

  private fun connect() {
    val config = serviceHub.getAppContext().config
    val cluster = cluster(
      host = config.getString("cassandra_host"),
      port = config.getInt("cassandra_port"),
      cluster = config.getString("cassandra_cluster")
    )
    session =
      session(cluster = cluster, keyspace = config.getString("cassandra_keyspace"))
    mappingManager = mappingManager(session)
  }

  private fun cluster(host: String, port: Int, cluster: String): Cluster {
    return Cluster.builder()
      .addContactPoint(host)
      .withPort(port)
      .withClusterName(cluster)
      .build()
  }

  private fun session(cluster: Cluster, keyspace: String): Session {
    val session = cluster.connect()
    setupKeyspace(session, keyspace)
    return session
  }

  private fun setupKeyspace(session: Session, keyspace: String) {
    val replication = HashMap<String, Any>()
    replication["class"] = "SimpleStrategy"
    replication["replication_factor"] = 1
    session.execute(createKeyspace(keyspace).ifNotExists().with().replication(replication))
    session.execute("USE $keyspace")
    executeSetupScript(session)
  }

  private fun executeSetupScript(session: Session) {
    val content = IOUtils.toString(
      this::class.java.getResourceAsStream("/cql/setup.cql"),
      Charset.defaultCharset()
    )
    val cql = normalizeSpace(content)
      .split(Regex("(?<=;)"))
      .filter(String::isNotBlank)
    cql.forEach { session.execute(it) }
  }

  private fun mappingManager(session: Session): MappingManager {
    val propertyMapper = DefaultPropertyMapper()
      .setNamingStrategy(DefaultNamingStrategy(LOWER_CAMEL_CASE, LOWER_SNAKE_CASE))
    val configuration =
      MappingConfiguration.builder().withPropertyMapper(propertyMapper).build()
    return MappingManager(session, configuration)
  }

  private companion object {
    val log = loggerFor<CassandraDatabaseService>()
  }
}