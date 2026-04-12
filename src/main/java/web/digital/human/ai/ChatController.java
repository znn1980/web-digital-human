package web.digital.human.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author znn
 */
@RestController
public class ChatController {
    private final static Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ChatController(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    @PostMapping(value = "/ai/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chatCompletions(@RequestBody ChatRequest chatRequest) {
        logger.info("CHAT-COMPLETIONS => {}", chatRequest);
        return this.chatClient.prompt()
                .user(chatRequest.question())
                .options(OpenAiChatOptions.builder()
                        .extraBody(Map.of(
                                "enable_thinking", chatRequest.enableThinking(),
                                "thinking", Map.of("type", chatRequest.enableThinking() ? "enabled" : "disabled"),
                                "enable_search", chatRequest.enableSearch(),
                                "web_search", Map.of("enable", chatRequest.enableSearch())
                        )).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatRequest.conversationId()))
                .stream().chatResponse();
    }

    @DeleteMapping("/ai/chat/{conversationId}")
    public void clearHistory(@PathVariable String conversationId) {
        logger.info("CLEAR-HISTORY => {}", conversationId);
        this.chatMemory.clear(conversationId);
    }
}
