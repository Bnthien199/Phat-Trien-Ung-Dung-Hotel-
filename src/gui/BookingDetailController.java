package gui;

import dao.ChiTietPhieuDatDAO;
import dao.PhieuDatPhongDAO;
import entity.ChiTietPhieuDat;
import entity.KhachHang;
import entity.PhieuDatPhong;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class BookingDetailController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PhieuDatPhongDAO phieuDatPhongDAO;
    private final ChiTietPhieuDatDAO chiTietPhieuDatDAO;

    public BookingDetailController() {
        this(new PhieuDatPhongDAO(), new ChiTietPhieuDatDAO());
    }

    public BookingDetailController(PhieuDatPhongDAO phieuDatPhongDAO, ChiTietPhieuDatDAO chiTietPhieuDatDAO) {
        this.phieuDatPhongDAO = phieuDatPhongDAO;
        this.chiTietPhieuDatDAO = chiTietPhieuDatDAO;
    }

    public void openBookingDetail(JFrame parent, String bookingId) {
        try {
            PhieuDatPhong booking = phieuDatPhongDAO.getPhieuDatPhongByMa(bookingId);
            if (booking == null) {
                JOptionPane.showMessageDialog(parent, "Không tìm thấy phiếu đặt phòng: " + bookingId);
                return;
            }

            List<ChiTietPhieuDat> roomDetails = chiTietPhieuDatDAO.getDSChiTietByMaPhieu(bookingId);
            BookingDetailData detailData = toDetailData(booking, roomDetails);
            FormChiTietPhieu detailForm = new FormChiTietPhieu(parent, detailData);
            detailForm.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Không thể mở chi tiết phiếu: " + e.getMessage());
        }
    }

    private BookingDetailData toDetailData(PhieuDatPhong booking, List<ChiTietPhieuDat> roomDetails) {
        KhachHang khachHang = booking.getKhachHang();
        List<String> roomList = roomDetails.stream()
                .map(this::toRoomCardData)
                .filter(room -> room != null && !room.isBlank())
                .collect(Collectors.toList());

        return new BookingDetailData(
                booking.getMaDatPhong(),
                khachHang != null && khachHang.getTenKhachHang() != null ? khachHang.getTenKhachHang() : "Chưa cập nhật",
                khachHang != null && khachHang.getSoDienThoai() != null && !khachHang.getSoDienThoai().isBlank() ? khachHang.getSoDienThoai() : "Chưa cập nhật",
                formatDateTime(booking.getNgayNhan()),
                formatDateTime(booking.getNgayTra()),
                roomList.isEmpty() ? List.of("Không có phòng") : roomList
        );
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "Chưa cập nhật";
    }

    private String toRoomCardData(ChiTietPhieuDat detail) {
        if (detail == null || detail.getPhong() == null) {
            return "";
        }
        return String.join("|",
                safe(detail.getPhong().getMaPhong(), "Chưa cập nhật"),
                safe(detail.getPhong().getLoaiPhong(), "Phòng"),
                formatDateTime(detail.getNgayNhan()),
                formatDateTime(detail.getNgayTra()),
                String.format("%,.0f VND", detail.getGiaThuePhong()),
                safe(detail.getTrangThai(), ChiTietPhieuDat.TRANG_THAI_CHO_NHAN));
    }

    private String safe(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
