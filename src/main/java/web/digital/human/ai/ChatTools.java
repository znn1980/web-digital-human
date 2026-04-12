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
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author znn
 */
@Component
public class ChatTools {
    private final static Logger logger = LoggerFactory.getLogger(ChatTools.class);
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
        logger.info("获取当前日期和时间 => {}", LocalDateTime.now());
        return LocalDateTime.now().toString();
    }

    @Tool(description = """
            修改酒店的房间价格
            1. 需要提供酒店房间名称与房间价格
            2. 返回修改后的酒店房间信息包含：房间名称（name）、房间价格（price）
            """)
    public ChatHotel.Room updateHotelRoom(
            @ToolParam(description = "房间名称，如：标间、大床房、套房等") String roomName
            , @ToolParam(description = "房间价格") String roomPrice) {
        logger.info("修改酒店的房间价格 => {} => {}", roomName, roomPrice);
        return new ChatHotel.Room(roomName, roomPrice);
    }

    @Tool(description = """
            查询酒店的房间价格
            1. 输入参数是房间名称，如：标间、大床房、套房等。注意：如果没有指定具体的房间名称就返回所有的房间价格
            2. 返回酒店房间信息包含：房间名称（name）、房间价格（price）
            """)
    public List<ChatHotel.Room> queryHotelRoom(@ToolParam(description = """
            房间名称，类型如：标间、大床房、套房等
            """, required = false) String roomName) throws IOException {
        logger.info("查询酒店的房间价格 => {}", roomName);
        return mapper.readValue(hotel.getInputStream(), ChatHotel.class).rooms()
                .stream().map(room -> room.price(String.format("￥ %.2f", asPrice(100, 500))))
                .toList();
    }

    @Tool(description = """
            旅客预订酒店房间
            1. 需要提供旅客姓名、入住日期、房间名称
            2. 返回旅客预定信息包含：旅客姓名（guestName）、入住日期（dateTime）、房间名称（roomName）、房间价格（roomPrice）
            """)
    public ChatHotel.CheckIN setGuestRoom(@ToolParam(description = "旅客姓名") String guestName
            , @ToolParam(description = "入住日期") String dateTime
            , @ToolParam(description = "房间名称，如：标间、大床房、套房等") String roomName) {
        logger.info("旅客预订酒店房间 => {} => {} => {}", guestName, dateTime, roomName);
        return new ChatHotel.CheckIN(guestName, dateTime, roomName, String.format("￥ %.2f", asPrice(100, 500)));
    }

    @Tool(description = """
            查询旅客的预订信息
            1. 需要提供旅客姓名、入住日期
            2. 返回旅客预定信息包含：旅客姓名（guestName）、入住日期（dateTime）、房间名称（roomName）、房间价格（roomPrice）
            """)
    public ChatHotel.CheckIN queryGuestRoom(@ToolParam(description = "旅客姓名") String guestName
            , @ToolParam(description = "入住日期") String dateTime) {
        logger.info("查询旅客的预订信息 => {} => {}", guestName, dateTime);
        String[] rooms = {"标间", "大床房", "套房"};
        return new ChatHotel.CheckIN(guestName, dateTime
                , rooms[new Random().nextInt(rooms.length)], String.format("￥ %.2f", asPrice(100, 500)));
    }

    @Tool(description = """
            查询附近的酒店信息
            1. 输入参数是附近多少公里，注意：如果没有指定具体的公里数就返回所有的酒店
            2. 返回酒店信息包含：酒店名称（name）、酒店地址（address）、酒店类型（type）、酒店参考价（price）、酒店位置（point）、房间列表（rooms）
            3. 房间列表包含：房间名称（name）、房间价格（price）
            """)
    public List<ChatHotel> queryHotel(
            @ToolParam(description = "附近多少公里", required = false) String km) throws IOException {
        logger.info("查询附近的酒店信息 => {}", km);
        List<ChatHotel> chatHotels = new ArrayList<>();
        mapper.readValue(hotels.getInputStream(), new TypeReference<List<ChatHotel>>() {
        }).forEach(hotel -> {
            AtomicReference<Double> sum = new AtomicReference<>(0.0);
            List<ChatHotel.Room> rooms = new ArrayList<>();
            hotel.rooms().forEach(room -> {
                double price = asPrice(100, 500);
                sum.set(sum.get() + price);
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