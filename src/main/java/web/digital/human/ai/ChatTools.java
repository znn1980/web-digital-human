package web.digital.human.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author znn
 */
@Component
public class ChatTools {
    private final static Logger LOGGER = LoggerFactory.getLogger(ChatTools.class);
    @Value("classpath:/static/ai-hotels/hotel.json")
    private Resource hotel;
    @Value("classpath:/static/ai-hotels/hotels.json")
    private Resource hotels;

    @Tool(description = "获取当前日期和时间")
    public String getCurrentDateTime() {
        LOGGER.info("获取当前日期和时间 => {}", LocalDateTime.now());
        return LocalDateTime.now().toString();
    }

    @Tool(description = "修改酒店房间的价格")
    public String setHotelRoomPrice(@ToolParam(description = "房间类型，类型如：标间、大床房、套房等") String roomName
            , @ToolParam(description = "房间价格") String roomPrice) {
        StringBuilder sb = new StringBuilder();
        sb.append(roomName).append("的价格是：").append(roomPrice);
        LOGGER.info("修改酒店房间的价格 => {} \n{}", roomPrice, sb);
        return sb.toString();
    }

    @Tool(description = "查询酒店房间的价格")
    public String queryHotelRoomPrice(@ToolParam(description = "房间类型，类型如：标间、大床房、套房等。注意：如果没有指定具体类型查询所有房间的价格。")
                                      String roomName) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# 酒店房间的价格如下：\n");
        ((List<?>) new ObjectMapper()
                .readValue(hotel.getURL(), new TypeReference<Map<String, Object>>() {
                }).get("rooms"))
                .forEach(data -> {
                    sb.append("## ").append(((Map<?, ?>) data).get("name")).append("\n");
                    sb.append("1. 房间类型：").append(((Map<?, ?>) data).get("name")).append("\n");
                    sb.append("2. 房间价格：").append(asPrice(100, 500)).append("\n");
                });
        LOGGER.info("查询酒店房间的价格 => {} \n{}", roomName, sb);
        return sb.toString();
    }

    @Tool(description = "查询旅客预订信息")
    public String queryGuestRoom(@ToolParam(description = "旅客姓名") String guestName
            , @ToolParam(description = "入住日期") String checkInDate) {
        StringBuilder sb = new StringBuilder();
        sb.append(guestName).append("，您预订").append("房间")
                .append("，入住日期是：").append(checkInDate)
                .append("，价格是：").append(asPrice(100, 500));
        LOGGER.info("查询旅客预订信息 => {} => {} \n{}", guestName, checkInDate, sb);
        return sb.toString();
    }

    @Tool(description = "查询附近酒店房间的价格")
    public String queryNearHotelPrice(@ToolParam(description = "附近多少公里") String km) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# 附近").append(km).append("公里的酒店如下：\n");
        new ObjectMapper()
                .readValue(hotels.getURL(), new TypeReference<List<Map<String, Object>>>() {
                })
                .forEach(data -> {
                    sb.append("## ").append(data.get("name")).append("\n");
                    sb.append("1. 酒店名称：").append(data.get("name")).append("\n");
                    sb.append("2. 酒店类型：").append(data.get("type")).append("\n");
                    sb.append("3. 酒店地址：").append(data.get("address")).append("\n");
                    ((List<?>) data.get("rooms")).forEach(room -> {
                        sb.append("### ").append("房间的价格如下：").append("\n");
                        sb.append("1. 房间类型：").append(((Map<?, ?>) room).get("name")).append("\n");
                        sb.append("2. 房间价格：").append(asPrice(100, 500)).append("\n");
                    });
                });
        LOGGER.info("查询附近%s公里酒店房间的价格 => {} \n{}", km, sb);
        return sb.toString();
    }

    public static String asPrice(double min, double max) {
        return String.format("￥ %.2f", (Math.random() * (max - min) + min));
    }
}