package com.sys1yagi.springbootopenaiapisample.webflux.api.openai

import com.theokanning.openai.completion.chat.ChatCompletionChunk
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import io.reactivex.Flowable
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class OneshotChatCompletion(
    @Value("\${openai.api_key}") private val token: String
) {
    suspend fun call(systemPrompt: String, userPrompt: String, modelName: ModelName): Flowable<ChatCompletionChunk> {
        val messages: MutableList<ChatMessage> = ArrayList()
        val systemMessage = ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt)
        messages.add(systemMessage)
        messages.add(ChatMessage(ChatMessageRole.USER.value(), userPrompt))

        val service = OpenAiService(token)
        val completionRequest = ChatCompletionRequest.builder()
            .stream(true)
            .model(modelName.value)
            .temperature(0.6)
            .messages(messages)
            .build()

        return service.streamChatCompletion(completionRequest)
    }
}
