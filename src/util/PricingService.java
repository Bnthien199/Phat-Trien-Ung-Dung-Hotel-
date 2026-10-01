package util;

import dao.BangGiaDetailDAO;
import dao.ChiTietHoaDonDAO;
import entity.ChiTietHoaDon;
import entity.ChiTietPhieuDat;
import entity.Phong;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

public class PricingService {
    private static final LocalTime HOURLY_WINDOW_START = LocalTime.of(7, 0);
    private static final LocalTime HOURLY_WINDOW_END = LocalTime.of(22, 0);
    private static final LocalTime DAILY_BOUNDARY_TIME = LocalTime.NOON;

    private final BangGiaDetailDAO bangGiaDetailDAO;
    private final ChiTietHoaDonDAO chiTietHoaDonDAO;

    public PricingService() {
        this(new BangGiaDetailDAO(), new ChiTietHoaDonDAO());
    }

    public PricingService(BangGiaDetailDAO bangGiaDetailDAO, ChiTietHoaDonDAO chiTietHoaDonDAO) {
        this.bangGiaDetailDAO = bangGiaDetailDAO;
        this.chiTietHoaDonDAO = chiTietHoaDonDAO;
    }

    public double getAppliedPriceByDate(Phong phong, LocalDate ngay) {
        if (phong == null || ngay == null) {
            return 0;
        }
        double price = bangGiaDetailDAO.getGiaTheoNgay(phong, ngay);
        return price > 0 ? price : phong.getGiaPhong();
    }

    public double getAppliedHourlyPriceByDate(Phong phong, LocalDate ngay) {
        if (phong == null || ngay == null) {
            return 0;
        }
        double hourly = bangGiaDetailDAO.getGiaTheoGio(phong, ngay);
        if (hourly > 0) {
            return hourly;
        }

        double daily = getAppliedPriceByDate(phong, ngay);
        return daily > 0 ? daily / 24.0 : 0;
    }

    public double getAppliedPriceForRoomAndDate(Phong phong, LocalDate ngay) {
        return getAppliedPriceByDate(phong, ngay);
    }

    public double calculateEstimatedBookingPrice(Phong phong, LocalDate fromDate, LocalDate toDate) {
        if (phong == null || fromDate == null) {
            return 0;
        }
        LocalDate effectiveToDate = (toDate == null || !toDate.isAfter(fromDate))
                ? fromDate.plusDays(1)
                : toDate;
        LocalDateTime checkIn = fromDate.atTime(DAILY_BOUNDARY_TIME);
        LocalDateTime checkOut = effectiveToDate.atTime(DAILY_BOUNDARY_TIME);
        return calculateBookingCharge(phong, checkIn, checkOut).getTongTien();
    }

    public double calculateEstimatedBookingPrice(Phong phong, LocalDateTime checkIn, LocalDateTime checkOut) {
        return calculateBookingCharge(phong, checkIn, checkOut).getTongTien();
    }

    public BookingChargeResult calculateBookingCharge(Phong phong, LocalDateTime checkIn, LocalDateTime checkOut) {
        if (phong == null || checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return BookingChargeResult.invalid();
        }

        if (isHourlyBooking(checkIn, checkOut)) {
            double soGio = Duration.between(checkIn, checkOut).toMinutes() / 60.0;
            double giaTheoGio = getAppliedHourlyPriceByDate(phong, checkIn.toLocalDate());
            double tongTien = soGio * giaTheoGio;
            return BookingChargeResult.theoGio(soGio, tongTien);
        }

        int soNgay = calculateStandardDailyNights(checkIn, checkOut);
        LocalDate baseDay = checkIn.toLocalDate();
        double tongTien = 0;
        for (int i = 0; i < soNgay; i++) {
            tongTien += getAppliedPriceByDate(phong, baseDay.plusDays(i));
        }
        return BookingChargeResult.theoNgay(soNgay, tongTien);
    }

    public String determineBookingType(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return "theo_ngay";
        }
        return isHourlyBooking(checkIn, checkOut) ? "theo_gio" : "theo_ngay";
    }

    public double getStoredBookingPrice(ChiTietPhieuDat chiTiet) {
        if (chiTiet == null) {
            return 0;
        }
        if (chiTiet.getGiaThuePhong() > 0) {
            return chiTiet.getGiaThuePhong();
        }
        if (chiTiet.getPhong() == null || chiTiet.getNgayNhan() == null) {
            return 0;
        }
        LocalDateTime checkOut = chiTiet.getNgayTra() != null
                ? chiTiet.getNgayTra()
                : chiTiet.getNgayNhan().plusDays(1);
        return calculateBookingCharge(chiTiet.getPhong(), chiTiet.getNgayNhan(), checkOut).getTongTien();
    }

    public double calculateStoredBookingTotal(List<ChiTietPhieuDat> chiTietList) {
        if (chiTietList == null) {
            return 0;
        }
        return chiTietList.stream()
                .mapToDouble(this::getStoredBookingPrice)
                .sum();
    }

    public double calculateRoomCharge(ChiTietPhieuDat chiTiet, LocalDateTime checkoutTime) {
        if (chiTiet == null || chiTiet.getPhong() == null || chiTiet.getNgayNhan() == null) {
            return 0;
        }

        LocalDateTime effectiveCheckout = checkoutTime != null ? checkoutTime : LocalDateTime.now();
        if (effectiveCheckout.isBefore(chiTiet.getNgayNhan())) {
            effectiveCheckout = chiTiet.getNgayNhan();
        }

        return calculateBookingCharge(chiTiet.getPhong(), chiTiet.getNgayNhan(), effectiveCheckout).getTongTien();
    }

    public double calculateRoomCharge(List<ChiTietPhieuDat> chiTietList, LocalDateTime checkoutTime) {
        if (chiTietList == null) {
            return 0;
        }
        return chiTietList.stream()
                .mapToDouble(ct -> calculateRoomCharge(ct, checkoutTime))
                .sum();
    }

    public double calculateServiceCharge(String maHoaDon) {
        if (maHoaDon == null || maHoaDon.isBlank()) {
            return 0;
        }

        List<ChiTietHoaDon> details = chiTietHoaDonDAO.getByMaHoaDon(maHoaDon);
        if (details == null) {
            details = Collections.emptyList();
        }

        return details.stream()
                .mapToDouble(item -> item.getSoLuong() * item.getDonGiaLuuTru())
                .sum();
    }

    private boolean isHourlyBooking(LocalDateTime checkIn, LocalDateTime checkOut) {
        return checkIn.toLocalDate().equals(checkOut.toLocalDate())
                && isWithinHourlyWindow(checkIn.toLocalTime())
                && isWithinHourlyWindow(checkOut.toLocalTime());
    }

    private boolean isWithinHourlyWindow(LocalTime time) {
        return !time.isBefore(HOURLY_WINDOW_START) && !time.isAfter(HOURLY_WINDOW_END);
    }

    private int calculateStandardDailyNights(LocalDateTime checkIn, LocalDateTime checkOut) {
        LocalDateTime baseBoundary = checkIn.toLocalDate().atTime(DAILY_BOUNDARY_TIME);
        double hours = Duration.between(baseBoundary, checkOut).toMinutes() / 60.0;
        int soNgay = (int) Math.ceil(Math.max(0, hours) / 24.0);
        return Math.max(1, soNgay);
    }

    public static final class BookingChargeResult {
        private final String loaiDatPhong;
        private final double soGio;
        private final int soNgay;
        private final double tongTien;

        private BookingChargeResult(String loaiDatPhong, double soGio, int soNgay, double tongTien) {
            this.loaiDatPhong = loaiDatPhong;
            this.soGio = soGio;
            this.soNgay = soNgay;
            this.tongTien = tongTien;
        }

        public static BookingChargeResult theoGio(double soGio, double tongTien) {
            return new BookingChargeResult("theo_gio", soGio, 0, tongTien);
        }

        public static BookingChargeResult theoNgay(int soNgay, double tongTien) {
            return new BookingChargeResult("theo_ngay", 0, soNgay, tongTien);
        }

        public static BookingChargeResult invalid() {
            return new BookingChargeResult("theo_ngay", 0, 0, 0);
        }

        public String getLoaiDatPhong() {
            return loaiDatPhong;
        }

        public double getSoGio() {
            return soGio;
        }

        public int getSoNgay() {
            return soNgay;
        }

        public double getTongTien() {
            return tongTien;
        }
    }
}
