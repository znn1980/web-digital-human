package web.digital.human.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @author znn
 */
@RestController
public class ChatController {
    private final static Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ChatController(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    @PostMapping(value = "/ai/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatCompletions(@RequestBody ChatRequest chatRequest) {
        LOGGER.info("CHAT-COMPLETIONS => {}", chatRequest);
        return this.chatClient.prompt()
                .system(chatRequest.system()).user(chatRequest.user())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatRequest.uuid()))
                .stream().content();
    }

    @DeleteMapping("/ai/chat/{conversationId}")
    public void clearHistory(@PathVariable("conversationId") String conversationId) {
        LOGGER.info("CLEAR-HISTORY => {}", conversationId);
        this.chatMemory.clear(conversationId);
    }
}
