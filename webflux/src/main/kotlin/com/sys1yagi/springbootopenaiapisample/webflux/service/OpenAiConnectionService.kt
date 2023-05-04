package com.sys1yagi.springbootopenaiapisample.webflux.service

import com.sys1yagi.springbootopenaiapisample.webflux.api.openai.ModelName
import com.sys1yagi.springbootopenaiapisample.webflux.api.openai.OneshotChatCompletion
import com.sys1yagi.springbootopenaiapisample.webflux.controller.SseSubscriber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.collect
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.LocalDateTime
import java.util.UUID

@Service
class OpenAiConnectionService(
    val oneshotCompletion: OneshotChatCompletion
) {
    private val subscribers = mutableMapOf<UUID, SseSubscriber>()

    fun createSseConnection(): UUID {
        val uuid = UUID.randomUUID()
        val flux = Flux.create {
            val sseSubscriber = SseSubscriber(
                uuid = uuid,
                sink = it,
                lastAccess = LocalDateTime.now()
            )
            it.onDispose {
                subscribers.remove(uuid)
            }
            subscribers[sseSubscriber.uuid] = sseSubscriber
        }
        return uuid
    }

    fun oneShotStream(
        uuid: UUID,
        systemPrompt: String,
        userPrompt: String,
        modelName: ModelName
    ): UUID? {
        return subscribers[uuid]?.let { sseSubscriber ->
            sseSubscriber.lastAccess = LocalDateTime.now()
            GlobalScope.launch(Dispatchers.IO) {
                oneshotCompletion.call(systemPrompt, userPrompt, modelName).collect { chunk ->
                    val content = chunk.choices[0].message.content
                    if (content.isEmpty()) {
                        sseSubscriber.sink.next(
                            ServerSentEvent.builder<String>()
                                .id(chunk.id)
                                .event("complete")
                                .build()
                        )
                    } else {
                        sseSubscriber.sink.next(
                            ServerSentEvent.builder<String>()
                                .id(chunk.id)
                                .event("message")
                                .data(content)
                                .build()
                        )
                    }
                }
            }
            sseSubscriber.uuid
        }
    }
}
