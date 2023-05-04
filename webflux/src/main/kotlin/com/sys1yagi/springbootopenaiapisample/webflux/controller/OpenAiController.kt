package com.sys1yagi.springbootopenaiapisample.webflux.controller

import com.sys1yagi.springbootopenaiapisample.webflux.api.openai.ModelName
import com.sys1yagi.springbootopenaiapisample.webflux.service.OpenAiConnectionService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.FluxSink
import java.time.LocalDateTime
import java.util.UUID

data class SseSubscriber(
    val uuid: UUID,
    val sink: FluxSink<ServerSentEvent<String>>,
    var lastAccess: LocalDateTime = LocalDateTime.now()
)

@RestController
@RequestMapping("/api/openai")
class OpenAiController(private val openAiService: OpenAiConnectionService) {

    @GetMapping("/sse", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun createSseConnection(): UUID {
        return openAiService.createSseConnection()
    }

    data class OneShotPostBody(
        val uuid: UUID,
        val systemPrompt: String,
        val userPrompt: String
    )

    @PostMapping("/sse/oneshot")
    fun streamSSE(
        @RequestBody body: OneShotPostBody
    ): ResponseEntity<UUID> {
        val uuid = openAiService.oneShotStream(
            body.uuid,
            body.systemPrompt,
            body.userPrompt,
            ModelName.GPT_3_5_TURBO
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(uuid)
    }
}
