package com.lankydanblog.tutorial.server.config.jackson

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.lankydanblog.tutorial.server.NodeRPCConnection
import net.corda.client.jackson.JacksonSupport
import net.corda.core.crypto.SecureHash
import net.corda.core.node.services.Vault
import org.springframework.boot.jackson.JsonComponentModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfiguration {

  @Bean
  fun rpcObjectMapper(
    rpc: NodeRPCConnection,
    jsonComponentModule: JsonComponentModule
  ): ObjectMapper {
    val mapper = JacksonSupport.createDefaultMapper(rpc.proxy/*, fullParties = true*/)
    mapper.registerModule(jsonComponentModule)
    mapper.registerModule(MixinModule())
    return mapper
  }

  class MixinModule : SimpleModule() {
    init {
      setMixInAnnotation(Vault.Update::class.java, VaultUpdateMixin::class.java)
      setMixInAnnotation(SecureHash::class.java, SecureHashMixin::class.java)
    }
  }

  abstract class VaultUpdateMixin {
    @JsonIgnore
    abstract fun isEmpty()
  }

  @JsonDeserialize(using = JacksonSupport.SecureHashDeserializer::class)
  abstract class SecureHashMixin

}