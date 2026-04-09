package web.digital.human.ai;

/**
 * @author znn
 */
public record ChatRequest(
        String conversationId, String question,
        boolean enableThinking, boolean enableSearch) {
}