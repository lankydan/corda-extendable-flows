package com.lankydanblog.tutorial.http.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.lankydanblog.tutorial.states.MessageState
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.*

@CordaService
class MessageAcknowledger(serviceHub: AppServiceHub) :
  SingletonSerializeAsToken() {

  private val client = OkHttpClient()
  private val mapper = ObjectMapper()

  private val baseUrl: String
  private val newMessageEndpoint: String
  private val signedMessageEndpoint: String
  private val committedMessageEndpoint: String

  init {
    val config = serviceHub.getAppContext().config
    // config parameters cant have `.`s in them
    baseUrl = config.getString("messages_request_path_base")
    newMessageEndpoint = config.getString("messages_request_path_new")
    signedMessageEndpoint = config.getString("messages_request_path_signed")
    committedMessageEndpoint = config.getString("messages_request_path_committed")
  }

  fun newMessageReceived(message: MessageState, sender: Boolean) {
    val dto = MessageDto(
      party = (if (sender) message.sender else message.recipient).toString(),
      id = message.linearId.id,
      content = message.contents,
      committed = false
    )
    val request = Request.Builder().url("http://$baseUrl/$newMessageEndpoint").post(
      RequestBody.create(
        MediaType.parse("application/json; charset=utf-8"),
        mapper.writeValueAsString(dto)
      )
    ).build()
    try {
      client.newCall(request).execute()
    } catch (e: Exception) {
      log.warn("Failed to send request: $request")
    }
  }

  fun messageTransactionSigned(message: MessageState, sender: Boolean) {
    val dto = MessageDto(
      party = (if (sender) message.sender else message.recipient).toString(),
      id = message.linearId.id,
      content = message.contents,
      committed = false
    )
    val request = Request.Builder().url("http://$baseUrl/$signedMessageEndpoint").post(
      RequestBody.create(
        MediaType.parse("application/json"),
        mapper.writeValueAsString(dto)
      )
    ).build()
    try {
      client.newCall(request).execute()
    } catch (e: Exception) {
      log.warn("Failed to send request: $request")
    }
  }

  fun messageTransactionCommitted(message: MessageState, sender: Boolean) {
    val dto = MessageDto(
      party = (if (sender) message.sender else message.recipient).toString(),
      id = message.linearId.id,
      content = message.contents,
      committed = true
    )
    val request =
      Request.Builder().url("http://$baseUrl/$committedMessageEndpoint").post(
        RequestBody.create(
          MediaType.parse("application/json"),
          mapper.writeValueAsString(dto)
        )
      ).build()
    try {
      client.newCall(request).execute()
    } catch (e: Exception) {
      log.warn("Failed to send request: $request")
    }
  }

  private companion object {
    val log = loggerFor<MessageAcknowledger>()
  }

  private data class MessageDto(
    val party: String,
    val id: UUID,
    val content: String,
    val committed: Boolean
  )
}