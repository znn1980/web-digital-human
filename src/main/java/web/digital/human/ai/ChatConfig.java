package web.digital.human.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * @author znn
 */
@Configuration
public class ChatConfig {

    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        //注入：内存聊天记忆仓库
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        //注入：内存聊天记忆
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .build();
    }


    @Bean
    public ChatClient chatClient(ChatModel chatModel
            , ChatMemory chatMemory, ChatTools chatTools, VectorStore vectorStore
            , SystemPromptTemplate systemPromptTemplate) {
        return ChatClient.builder(chatModel)
                //ToolsCalling：模拟酒店查询房价、预订、等功能
                .defaultTools(chatTools)
                //系统提示词
                .defaultSystem(systemPromptTemplate.render())
                .defaultAdvisors(
                        //日志
                        SimpleLoggerAdvisor.builder().build()
                        //聊天记忆
                        , MessageChatMemoryAdvisor.builder(chatMemory).build()
                        //知识库
                        , QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .similarityThreshold(0.7)
                                        .topK(4)
                                        .build()).build()
                )
                .build();
    }

    @Bean
    public SystemPromptTemplate systemPromptTemplate(ObjectMapper mapper
            , @Value("classpath:/static/ai-hotels/default-system.md") Resource defaultSystem
            , @Value("classpath:/static/ai-hotels/hotel.json") Resource hotel) throws IOException {
        //注入：系统提示词
        return SystemPromptTemplate.builder()
                .resource(defaultSystem)
                .variables(mapper.readValue(hotel.getInputStream(), new TypeReference<>() {
                })).build();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        //注入：基于内存的向量存储
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public ApplicationRunner runner(VectorStore vectorStore
            , @Value("classpath:/static/ai-hotels/知识库/墨历酒店.docx") Resource doc
            , @Value("classpath:/static/ai-hotels/知识库/墨历酒店.md") Resource md
            , @Value("classpath:/static/ai-hotels/知识库/墨历酒店.pdf") Resource pdf
            , @Value("classpath:/static/ai-hotels/知识库/墨历酒店.txt") Resource txt) {
        //加载（RAG）知识库
        return args -> {
            vectorStore.add(new TikaDocumentReader(doc, ExtractedTextFormatter.defaults()).get());
            vectorStore.add(new MarkdownDocumentReader(md, MarkdownDocumentReaderConfig.defaultConfig()).get());
            vectorStore.add(new PagePdfDocumentReader(pdf, PdfDocumentReaderConfig.defaultConfig()).get());
            vectorStore.add(TokenTextSplitter.builder().build().apply(new TextReader(txt).get()));
        };
    }
}
