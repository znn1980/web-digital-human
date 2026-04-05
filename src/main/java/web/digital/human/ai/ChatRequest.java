package web.digital.human.ai;

/**
 * @author znn
 */
public record ChatRequest(
        String uuid, String system, String user,
        boolean enableThinking, boolean enableSearch) {
}