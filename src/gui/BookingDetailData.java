package gui;

import java.util.List;

public class BookingDetailData {
    private final String bookingId;
    private final String customerName;
    private final String phone;
    private final String checkInDate;
    private final String checkOutDate;
    private final List<String> roomList;

    public BookingDetailData(String bookingId, String customerName, String phone,
                             String checkInDate, String checkOutDate, List<String> roomList) {
        this.bookingId = bookingId;
        this.customerName = customerName;
        this.phone = phone;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.roomList = roomList;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPhone() {
        return phone;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public String getCheckOutDate() {
        return checkOutDate;
    }

    public List<String> getRoomList() {
        return roomList;
    }
}
