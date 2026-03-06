package web.digital.human.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

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

    @Tool(description = "查询附近酒店房间的价格")
    public String queryHotelPrice(@ToolParam(description = "附近多少公里") String km) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("附近").append(km).append("公里的酒店：\n");
        ((ArrayList<?>) new ObjectMapper()
                .readValue(FileCopyUtils.copyToByteArray(this.getClass()
                        .getResourceAsStream("/static/ai-hotels/hotels.json")), Map.class)
                .get("data")).forEach(data -> {
            sb.append("名称：").append(((Map<?, ?>) data).get("name")).append("\n");
            sb.append("地址：").append(((Map<?, ?>) data).get("address")).append("\n");
            sb.append("价格：").append(asPrice(100, 500)).append("\n");
            sb.append("---\n");
        });
        return sb.toString();
    }

    public static String asPrice(double min, double max) {
        return String.format("￥ %.2f", (Math.random() * (max - min) + min));
    }
}
