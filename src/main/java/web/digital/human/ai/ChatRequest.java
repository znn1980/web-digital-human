package web.digital.human.ai;

/**
 * @author znn
 */
public record ChatRequest(String uuid, String system, String user) {
    @Override
    public String toString() {
        return String.format("{uuid='%s',system='%s',user='%s'}", uuid, system, user);
    }
}

