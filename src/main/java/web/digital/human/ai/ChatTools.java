package web.digital.human.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
    private final ObjectMapper mapper;

    public ChatTools(ObjectMapper mapper) {
        this.mapper = mapper
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Tool(description = "获取当前日期和时间")
    public String getCurrentDateTime() {
        LOGGER.info("获取当前日期和时间 => {}", LocalDateTime.now());
        return LocalDateTime.now().toString();
    }

    @Tool(description = "修改酒店的房间价格")
    public String setHotelRoomPrice(
            @ToolParam(description = "房间类型，类型如：标间、大床房、套房等") String roomName
            , @ToolParam(description = "房间价格") String roomPrice) {
        LOGGER.info("修改酒店的房间价格 => {} => {}", roomName, roomPrice);
        return String.format("""
                酒店%s的价格修改完成
                价格是：%s
                """, roomName, roomPrice);
    }

    @Tool(description = """
            查询酒店的房间价格
            1. 输入参数是房间类型，类型如：标间、大床房、套房等。注意：如果没有指定具体类型就返回所有的房间价格
            2. 返回酒店房间信息包含：房间名称、房间价格
            """)
    public List<ChatHotel.Room> queryHotelRoomPrice(@ToolParam(description = """
            房间类型，类型如：标间、大床房、套房等
            """, required = false) String roomName) throws IOException {
        LOGGER.info("查询酒店的房间价格 => {}", roomName);
        return mapper.readValue(hotel.getURL(), ChatHotel.class).rooms()
                .stream().map(room -> room.price(String.format("￥ %.2f", asPrice(100, 500))))
                .toList();
    }

    @Tool(description = "旅客预订酒店房间")
    public String setGuestRoom(@ToolParam(description = "旅客姓名") String guestName
            , @ToolParam(description = "入住日期") String checkInDate
            , @ToolParam(description = "房间类型，类型如：标间、大床房、套房等") String roomName) {
        LOGGER.info("旅客预订酒店房间 => {} => {} => {}", guestName, checkInDate, roomName);
        return String.format("""
                %s，您预订的%s已成功
                入住日期是：%s
                价格是：%s
                """, guestName, roomName, checkInDate, asPrice(100, 500));
    }

    @Tool(description = "查询旅客的预订信息")
    public String queryGuestRoom(@ToolParam(description = "旅客姓名") String guestName
            , @ToolParam(description = "入住日期") String checkInDate) {
        LOGGER.info("查询旅客的预订信息 => {} => {}", guestName, checkInDate);
        return String.format("""
                %s，您预订的房间已成功
                入住日期是：%s
                价格是：%s
                """, guestName, checkInDate, asPrice(100, 500));
    }

    @Tool(description = """
            查询附近的酒店信息
            1. 输入参数是附近多少公里，注意：如果没有指定具体的公里数就返回所有的酒店
            2. 返回酒店信息包含：酒店名称（name）、酒店地址（address）、酒店类型（type）、酒店参考价（price）、酒店位置（point）、房间列表（rooms）
            3. 房间列表包含：房间名称（name）、房间价格（price）
            """)
    public List<ChatHotel> queryNearHotelPrice(
            @ToolParam(description = "附近多少公里", required = false) String km) throws IOException {
        LOGGER.info("查询附近%s公里的酒店信息 => {}", km);
        List<ChatHotel> chatHotels = new ArrayList<>();
        mapper.readValue(hotels.getURL(), new TypeReference<List<ChatHotel>>() {
        }).forEach(hotel -> {
            AtomicReference<Double> sum = new AtomicReference<>(0.0);
            List<ChatHotel.Room> rooms = new ArrayList<>();
            hotel.rooms().forEach(room -> {
                double price = asPrice(100, 500);
                sum.updateAndGet(v -> v + price);
                rooms.add(room.price(String.format("￥ %.2f", price)));
            });
            chatHotels.add(hotel.rooms(rooms)
                    .price(String.format("￥ %.2f", sum.get() / hotel.rooms().size())));
        });
        return chatHotels;
    }

    public static double asPrice(double min, double max) {
        return (Math.random() * (max - min) + min);
    }
}