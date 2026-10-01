package util;

import dao.ChiTietPhieuDatDAO;
import dao.PhongDAO;
import entity.ChiTietPhieuDat;
import entity.Phong;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RoomAvailabilityService {
    private final ChiTietPhieuDatDAO chiTietPhieuDatDAO;
    private final PhongDAO phongDAO;

    public RoomAvailabilityService() {
        this(new ChiTietPhieuDatDAO(), new PhongDAO());
    }

    public RoomAvailabilityService(ChiTietPhieuDatDAO chiTietPhieuDatDAO) {
        this(chiTietPhieuDatDAO, new PhongDAO());
    }

    public RoomAvailabilityService(ChiTietPhieuDatDAO chiTietPhieuDatDAO, PhongDAO phongDAO) {
        this.chiTietPhieuDatDAO = chiTietPhieuDatDAO;
        this.phongDAO = phongDAO;
    }

    public RoomDisplayStatus getRoomStatusOnDate(Phong room, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return getRoomStatusAt(room, targetDate.atStartOfDay());
    }

    public RoomDisplayStatus getRoomStatusAt(Phong room, LocalDateTime selectedDateTime) {
        if (room == null) {
            return RoomDisplayStatus.TRONG;
        }

        LocalDateTime target = selectedDateTime != null ? selectedDateTime : LocalDateTime.now();
        LocalDate selectedDate = target.toLocalDate();
        System.out.println("[ROOM MAP] selectedDate = " + selectedDate);
        System.out.println("[ROOM STATUS] maPhong = " + room.getMaPhong());
        if (phongDAO.isUnderMaintenanceAt(room.getMaPhong(), target)
                || (isCurrentMoment(target) && StatusUtils.isRoomStatus(room.getTrangThai(), RoomStatus.SUA_CHUA))) {
            System.out.println("[DISPLAY STATUS] " + RoomDisplayStatus.SUA_CHUA);
            return RoomDisplayStatus.SUA_CHUA;
        }

        ChiTietPhieuDat stay = chiTietPhieuDatDAO.findStayAt(room.getMaPhong(), target);
        if (stay == null) {
            System.out.println("[BOOKING RANGE] from = null, to = null");
            System.out.println("[MATCH DATE] false");
            System.out.println("[DISPLAY STATUS] " + RoomDisplayStatus.TRONG);
            return RoomDisplayStatus.TRONG;
        }

        LocalDate ngayNhan = getBookingStartDate(stay);
        LocalDate ngayTra = getBookingEndDate(stay);
        boolean isDateInRange = isDateInBookingRange(stay, selectedDate);
        System.out.println("[BOOKING RANGE] from = " + ngayNhan + ", to = " + ngayTra);
        System.out.println("[MATCH DATE] " + isDateInRange);
        if (!isDateInRange) {
            System.out.println("[DISPLAY STATUS] " + RoomDisplayStatus.TRONG);
            return RoomDisplayStatus.TRONG;
        }

        LocalDate today = LocalDate.now();
        RoomDisplayStatus displayStatus;
        if (isCheckedOutOrPaid(stay)) {
            displayStatus = RoomDisplayStatus.DA_THANH_TOAN;
        } else if (selectedDate.isBefore(today)) {
            displayStatus = RoomDisplayStatus.DA_THANH_TOAN;
        } else if (selectedDate.isAfter(today) && isPendingReservation(stay)) {
            displayStatus = RoomDisplayStatus.DA_DAT;
        } else if (selectedDate.isEqual(today) && isPendingReservation(stay)) {
            displayStatus = RoomDisplayStatus.DA_DAT;
        } else {
            displayStatus = RoomDisplayStatus.DANG_O;
        }
        System.out.println("[DISPLAY STATUS] " + displayStatus);
        return displayStatus;
    }

    public List<Phong> filterRoomsByFloor(List<Phong> rooms, Integer floor) {
        if (rooms == null) {
            return List.of();
        }
        return rooms.stream()
                .filter(room -> floor == null || room.getTang() == floor)
                .sorted(Comparator.comparingInt(Phong::getTang).thenComparing(Phong::getMaPhong))
                .collect(Collectors.toList());
    }

    private boolean isCurrentMoment(LocalDateTime target) {
        LocalDateTime now = LocalDateTime.now();
        return !target.isBefore(now.minusMinutes(5)) && !target.isAfter(now.plusMinutes(5));
    }

    private boolean isCheckedOutOrPaid(ChiTietPhieuDat detail) {
        if (detail == null || detail.getPhieuDatPhong() == null) {
            return false;
        }
        String detailStatus = normalizeDetailStatus(detail.getTrangThai());
        return ChiTietPhieuDat.TRANG_THAI_DA_TRA.equalsIgnoreCase(detailStatus)
                || detail.getThoiDiemTraThucTe() != null
                || StatusUtils.isBookingStatus(detail.getPhieuDatPhong().getTrangThai(), BookingStatus.DA_THANH_TOAN);
    }

    private boolean isPendingReservation(ChiTietPhieuDat detail) {
        if (detail == null || detail.getPhieuDatPhong() == null) {
            return false;
        }
        String detailStatus = normalizeDetailStatus(detail.getTrangThai());
        String bookingStatus = detail.getPhieuDatPhong().getTrangThai();
        boolean pendingBooking = StatusUtils.isBookingStatus(bookingStatus, BookingStatus.CHO_COC)
                || StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_DAT_COC)
                || StatusUtils.isBookingStatus(bookingStatus, BookingStatus.CHO_XAC_NHAN)
                || StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_XAC_NHAN)
                || StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_NHAN_PHONG);
        return pendingBooking && ChiTietPhieuDat.TRANG_THAI_CHO_NHAN.equalsIgnoreCase(detailStatus);
    }

    private String normalizeDetailStatus(String status) {
        String normalized = StatusUtils.normalize(status);
        String compact = normalized.replace("_", "");
        if (compact.isBlank() || "CHONHAN".equals(compact) || "CHOCHECKIN".equals(compact)) {
            return ChiTietPhieuDat.TRANG_THAI_CHO_NHAN;
        }
        if ("DANHAN".equals(compact) || "NHANPHONG".equals(compact)
                || "DANHANPHONG".equals(compact) || "DANGO".equals(compact)) {
            return ChiTietPhieuDat.TRANG_THAI_DA_NHAN;
        }
        if ("DATRA".equals(compact) || "DATRAPHONG".equals(compact)
                || "DATHANHTOAN".equals(compact) || "TRAPHONG".equals(compact)
                || "THANHTOAN".equals(compact)) {
            return ChiTietPhieuDat.TRANG_THAI_DA_TRA;
        }
        return normalized;
    }

    private LocalDate getBookingStartDate(ChiTietPhieuDat detail) {
        LocalDateTime start = detail != null ? detail.getNgayNhan() : null;
        return start != null ? start.toLocalDate() : null;
    }

    private LocalDate getBookingEndDate(ChiTietPhieuDat detail) {
        LocalDateTime end = detail != null ? detail.getNgayTra() : null;
        return end != null ? end.toLocalDate() : null;
    }

    private boolean isDateInBookingRange(ChiTietPhieuDat detail, LocalDate selectedDate) {
        if (detail == null || selectedDate == null) {
            return false;
        }
        LocalDate startDate = getBookingStartDate(detail);
        LocalDate endDate = getBookingEndDate(detail);
        if (startDate == null) {
            return false;
        }
        return !selectedDate.isBefore(startDate)
                && (endDate == null || !selectedDate.isAfter(endDate));
    }
}
