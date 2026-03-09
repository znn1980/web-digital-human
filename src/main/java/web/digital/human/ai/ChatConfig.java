package web.digital.human.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * @author znn
 */
@Configuration
public class ChatConfig {

    @Value("classpath:/static/ai-hotels/default-system.md")
    private Resource defaultSystem;
    @Value("classpath:/static/ai-hotels/hotel.json")
    private Resource hotel;

    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(200)
                .build();
    }


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder
            , ChatMemory chatMemory, ChatTools chatTools
            , SystemPromptTemplate systemPromptTemplate) {
        return builder
                .defaultTools(chatTools)
                .defaultSystem(systemPromptTemplate.render())
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                        , MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    public SystemPromptTemplate systemPromptTemplate(ObjectMapper mapper) throws IOException {
        return SystemPromptTemplate.builder()
                .resource(defaultSystem)
                .variables(mapper.readValue(hotel.getURL(), new TypeReference<>() {
                })).build();
    }
}
