package web.digital.human.ai;

import java.util.List;

/**
 * @author znn
 */
public record ChatHotel(String name, String address, String type, String price, Point point, List<Room> rooms) {
    public ChatHotel price(String price) {
        return new ChatHotel(name, address, type, price, point, rooms);
    }

    public ChatHotel rooms(List<Room> rooms) {
        return new ChatHotel(name, address, type, price, point, rooms);
    }

    public record Point(String lng, String lat) {
    }

    public record Room(String name, String price) {
        public Room price(String price) {
            return new Room(name, price);
        }
    }

    public record CheckIN(String guestName, String dateTime, String roomName, String roomPrice) {
    }
}
