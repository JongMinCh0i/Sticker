package com.claudeproxy.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.claudeproxy.domain.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClaudeService {

    // Model ID for Claude Haiku 4.5 (cost-efficient)
    // If Model.CLAUDE_HAIKU_4_5 is not available in your SDK version,
    // use: Model.of("claude-haiku-4-5")
    private static final Model MODEL = Model.CLAUDE_HAIKU_4_5;

    private final AnthropicClient anthropicClient;

    public ClaudeService(AnthropicClient anthropicClient) {
        this.anthropicClient = anthropicClient;
    }

    /**
     * Sends the full conversation history to Claude and returns the assistant reply.
     * The last entry in conversationHistory must be the new user message.
     */
    public String chat(List<Message> conversationHistory) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(2048L);

        for (Message msg : conversationHistory) {
            if ("user".equals(msg.getRole())) {
                builder.addUserMessage(msg.getContent());
            } else {
                // addAssistantMessage is available from anthropic-java >= 2.x
                // Fallback: builder.addMessage(MessageParam.builder()
                //     .role(Role.ASSISTANT).content(msg.getContent()).build())
                builder.addAssistantMessage(msg.getContent());
            }
        }

        com.anthropic.models.messages.Message apiResponse =
                anthropicClient.messages().create(builder.build());

        return apiResponse.content().stream()
                .flatMap(block -> block.text().stream())
                .map(tb -> tb.text())
                .collect(Collectors.joining());
    }
}
