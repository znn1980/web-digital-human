package web.digital.human.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;

/**
 * @author znn
 */
public class ChatTools {
    private final static Logger LOGGER = LoggerFactory.getLogger(ChatTools.class);

    @Tool(description = "获取当前日期和时间")
    public String getCurrentDateTime() {
        LOGGER.info("获取当前日期和时间 => {}", LocalDateTime.now());
        return LocalDateTime.now().toString();
    }

    @Tool(description = "修改酒店房间的价格")
    public String setHotelRoomPrice(@ToolParam(description = "房间类型，类型如：标间、大床房、套房等") String roomType
            , @ToolParam(description = "房间价格") String roomPrice) {
        LOGGER.info("修改酒店房间的价格 => {} => {}", roomType, roomPrice);
        return String.format("酒店%s的价格是%s", roomType, roomPrice);
    }
}
