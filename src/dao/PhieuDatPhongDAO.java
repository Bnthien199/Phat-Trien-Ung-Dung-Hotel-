package dao;

import connectDB.ConnectDB;
import entity.ChiTietPhieuDat;
import entity.HuyDatPhong;
import entity.KhachHang;
import entity.NhanVien;
import entity.PhieuDatPhong;
import entity.Phong;
import util.BookingStatus;
import util.DepositPolicy;
import util.RoomStatus;
import util.StatusUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public class PhieuDatPhongDAO {
    private static final String BOOKING_SELECT_COLUMNS = """
                pdp.maDatPhong, pdp.ngayDat, pdp.trangThai, pdp.maNhanVien, pdp.maKhachHang,
                pdp.soDemLuuTru, pdp.tongTienPhong, pdp.tienDatCoc, pdp.tienConLai,
                pdp.tyLeDatCoc, pdp.hinhThucDatCoc, pdp.ngayDatCoc,
                pdp.maNhanVienNhanCoc, pdp.ghiChuDatCoc, pdp.soTienKhachDua, pdp.soTienThucNhan, pdp.tienThuaTraKhach
            """;

    private static final String BOOKING_GROUP_COLUMNS = """
                pdp.maDatPhong, pdp.ngayDat, pdp.trangThai, pdp.maNhanVien, pdp.maKhachHang,
                pdp.soDemLuuTru, pdp.tongTienPhong, pdp.tienDatCoc, pdp.tienConLai,
                pdp.tyLeDatCoc, pdp.hinhThucDatCoc, pdp.ngayDatCoc,
                pdp.maNhanVienNhanCoc, pdp.ghiChuDatCoc, pdp.soTienKhachDua, pdp.soTienThucNhan, pdp.tienThuaTraKhach
            """;
    private static final String[] ACTIVE_CHECKOUT_STATUSES = {
            ChiTietPhieuDat.TRANG_THAI_DA_NHAN,
            "DA_NHAN_PHONG",
            "DANG_O",
            "DANHAN",
            "DANHANPHONG",
            "DANGO",
            "NHANPHONG",
            "Đang ở",
            "Đã nhận phòng"
    };

    public List<PhieuDatPhong> getAllPhieuDatPhong() {
        List<PhieuDatPhong> ds = new ArrayList<>();
        String sql = """
                SELECT %s, kh.tenKhachHang, kh.soDienThoai, kh.diaChi, kh.gioiTinh, kh.loaiGiayTo, kh.soGiayTo, kh.TrangThai AS trangThaiKhachHang,
                       MIN(ct.ngayNhan) AS ngayNhan, MAX(ct.ngayTra) AS ngayTra,
                       STRING_AGG(ct.maPhong, ', ') AS dsMaPhong, COUNT(ct.maPhong) AS soLuongPhong
                FROM PhieuDatPhong pdp
                JOIN KhachHang kh ON pdp.maKhachHang = kh.maKhachHang
                LEFT JOIN ChiTietPhieuDat ct ON pdp.maDatPhong = ct.maDatPhong
                GROUP BY %s,
                         kh.tenKhachHang, kh.soDienThoai, kh.diaChi, kh.gioiTinh, kh.loaiGiayTo, kh.soGiayTo, kh.TrangThai
                ORDER BY pdp.ngayDat DESC, pdp.maDatPhong DESC
                """.formatted(BOOKING_SELECT_COLUMNS, BOOKING_GROUP_COLUMNS);

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ds.add(mapPhieuDatPhong(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[SQL ERROR] " + e.getMessage());
            javax.swing.JOptionPane.showMessageDialog(null, "Lỗi cơ sở dữ liệu (All Phieu Dat): " + e.getMessage(), "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
        return ds;
    }

    public List<PhieuDatPhong> getDanhSachChoCheckout() {
        List<PhieuDatPhong> ds = new ArrayList<>();
        String sql = "";

        try (Connection con = ConnectDB.getConnection()) {
            boolean hasDetailStatus = hasTableColumn(con, "ChiTietPhieuDat", "trangThai");
            String detailStatusExpression = detailStatusExpression(hasDetailStatus);
            String checkoutCondition = hasDetailStatus
                    ? statusInCondition("ct.trangThai", ACTIVE_CHECKOUT_STATUSES.length)
                    : "UPPER(ISNULL(" + detailStatusExpression + ", '')) = ?";
            sql = """
                SELECT %s, kh.tenKhachHang, kh.soDienThoai, kh.diaChi, kh.gioiTinh, kh.loaiGiayTo, kh.soGiayTo, kh.TrangThai AS trangThaiKhachHang,
                       MIN(ct.ngayNhan) AS ngayNhan, MAX(ct.ngayTra) AS ngayTra,
                       STRING_AGG(ct.maPhong, ', ') AS dsMaPhong, COUNT(ct.maPhong) AS soLuongPhong
                FROM PhieuDatPhong pdp
                JOIN KhachHang kh ON pdp.maKhachHang = kh.maKhachHang
                JOIN ChiTietPhieuDat ct ON pdp.maDatPhong = ct.maDatPhong
                JOIN Phong p ON p.maPhong = ct.maPhong
                WHERE %s
                GROUP BY %s,
                         kh.tenKhachHang, kh.soDienThoai, kh.diaChi, kh.gioiTinh, kh.loaiGiayTo, kh.soGiayTo, kh.TrangThai
                ORDER BY MIN(ct.ngayNhan), pdp.maDatPhong DESC
                """.formatted(BOOKING_SELECT_COLUMNS, checkoutCondition, BOOKING_GROUP_COLUMNS);

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                if (hasDetailStatus) {
                    bindUpperStatuses(stmt, 1, ACTIVE_CHECKOUT_STATUSES);
                } else {
                    stmt.setString(1, ChiTietPhieuDat.TRANG_THAI_DA_NHAN);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ds.add(mapPhieuDatPhong(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[SQL ERROR] " + e.getMessage());
            javax.swing.JOptionPane.showMessageDialog(null, "Lỗi cơ sở dữ liệu (Checkout List): " + e.getMessage(), "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
        return ds;
    }

    public List<PhieuDatPhong> getDanhSachPhongDiTrongNgay(LocalDate date) {
        List<PhieuDatPhong> ds = new ArrayList<>();
        String sql = """
                SELECT %s, kh.tenKhachHang, kh.soDienThoai, kh.diaChi, kh.gioiTinh, kh.loaiGiayTo, kh.soGiayTo, kh.TrangThai AS trangThaiKhachHang,
                       MIN(ct.ngayNhan) AS ngayNhan, MAX(ct.ngayTra) AS ngayTra,
                       STRING_AGG(ct.maPhong, ', ') AS dsMaPhong, COUNT(ct.maPhong) AS soLuongPhong,
                       SUM(CASE WHEN UPPER(ISNULL(ct.trangThai, '')) IN ('DA_TRA', 'DATRA', 'DA_TRA_PHONG', 'DATRAPHONG', 'DA_THANH_TOAN', 'DATHANHTOAN') THEN 1 ELSE 0 END) AS soPhongDaTra,
                       COUNT(ct.maPhong) AS tongSoPhong
                FROM PhieuDatPhong pdp
                JOIN KhachHang kh ON pdp.maKhachHang = kh.maKhachHang
                JOIN ChiTietPhieuDat ct ON pdp.maDatPhong = ct.maDatPhong
                WHERE CAST(ct.ngayTra AS DATE) = ?
                GROUP BY %s,
                         kh.tenKhachHang, kh.soDienThoai, kh.diaChi, kh.gioiTinh, kh.loaiGiayTo, kh.soGiayTo, kh.TrangThai
                ORDER BY MAX(ct.ngayTra) ASC, pdp.maDatPhong DESC
                """.formatted(BOOKING_SELECT_COLUMNS, BOOKING_GROUP_COLUMNS);

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PhieuDatPhong phieu = mapPhieuDatPhong(rs);
                    
                    String bookingStatus = rs.getString("trangThai");
                    int soPhongDaTra = rs.getInt("soPhongDaTra");
                    int tongSoPhong = rs.getInt("tongSoPhong");
                    
                    if (StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_THANH_TOAN)) {
                        phieu.setTrangThai("DA_THANH_TOAN");
                    } else if (soPhongDaTra >= tongSoPhong && tongSoPhong > 0) {
                        phieu.setTrangThai("DA_TRA_PHONG");
                    } else {
                        phieu.setTrangThai("CHO_TRA_PHONG");
                    }
                    
                    ds.add(phieu);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[SQL ERROR] " + e.getMessage());
            javax.swing.JOptionPane.showMessageDialog(null, "Lỗi cơ sở dữ liệu (Phong Di List): " + e.getMessage(), "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        return ds;
    }

    public PhieuDatPhong getPhieuDatPhongByMa(String maDatPhong) {
        String sql = """
                SELECT %s, kh.tenKhachHang, kh.soDienThoai, kh.diaChi, kh.gioiTinh, kh.loaiGiayTo, kh.soGiayTo, kh.TrangThai AS trangThaiKhachHang,
                       MIN(ct.ngayNhan) AS ngayNhan, MAX(ct.ngayTra) AS ngayTra,
                       STRING_AGG(ct.maPhong, ', ') AS dsMaPhong, COUNT(ct.maPhong) AS soLuongPhong
                FROM PhieuDatPhong pdp
                JOIN KhachHang kh ON pdp.maKhachHang = kh.maKhachHang
                LEFT JOIN ChiTietPhieuDat ct ON pdp.maDatPhong = ct.maDatPhong
                WHERE pdp.maDatPhong = ?
                GROUP BY %s,
                         kh.tenKhachHang, kh.soDienThoai, kh.diaChi, kh.gioiTinh, kh.loaiGiayTo, kh.soGiayTo, kh.TrangThai
                """.formatted(BOOKING_SELECT_COLUMNS, BOOKING_GROUP_COLUMNS);

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maDatPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapPhieuDatPhong(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tai phieu dat phong theo ma. " + buildSqlMessage(e), e);
        }
        return null;
    }

    public PhieuDatPhong getPhieuDangOTheoPhong(String maPhong) {
        String sql = """
                SELECT pdp.*, kh.tenKhachHang, kh.soDienThoai, kh.diaChi, kh.gioiTinh, kh.loaiGiayTo, kh.soGiayTo, kh.TrangThai AS trangThaiKhachHang,
                       ct.ngayNhan, ct.ngayTra, ct.maPhong AS dsMaPhong, 1 AS soLuongPhong
                FROM PhieuDatPhong pdp
                JOIN ChiTietPhieuDat ct ON pdp.maDatPhong = ct.maDatPhong
                JOIN KhachHang kh ON pdp.maKhachHang = kh.maKhachHang
                WHERE ct.maPhong = ?
                  AND UPPER(ISNULL(pdp.trangThai, '')) IN ('DA_NHAN_PHONG', 'DANHANPHONG', 'DANG_O', 'DANGO')
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maPhong);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapPhieuDatPhong(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tim phieu dang o theo phong. " + buildSqlMessage(e), e);
        }
        return null;
    }

    public boolean checkIn(String maPhieu) {
        List<ChiTietPhieuDat> pendingRooms = new ChiTietPhieuDatDAO()
                .getPendingRoomsByBooking(maPhieu);
        if (pendingRooms.isEmpty()) {
            return false;
        }
        return new ChiTietPhieuDatDAO().checkInNhieuPhong(pendingRooms);
    }

    public boolean datPhong(PhieuDatPhong pdp, List<ChiTietPhieuDat> dsChiTiet) {
        validateBookingInput(pdp, dsChiTiet);
        normalizeDepositForBooking(pdp, dsChiTiet);

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);
            try {
                boolean hasDetailStatus = hasTableColumn(con, "ChiTietPhieuDat", "trangThai");
                boolean hasActualCheckIn = hasTableColumn(con, "ChiTietPhieuDat", "thoiDiemNhanThucTe");
                boolean hasActualCheckOut = hasTableColumn(con, "ChiTietPhieuDat", "thoiDiemTraThucTe");
                boolean hasRoomSnapshot = hasTableColumn(con, "ChiTietPhieuDat", "tenPhongSnapshot")
                        && hasTableColumn(con, "ChiTietPhieuDat", "loaiPhongSnapshot")
                        && hasTableColumn(con, "ChiTietPhieuDat", "donGiaPhongSnapshot");
                ensureDefaultBookingStatuses(con);
                String bookingStatus = resolveValidBookingStatus(con, pdp.getTrangThai());
                pdp.setTrangThai(bookingStatus);
                StringBuilder detailColumns = new StringBuilder("maDatPhong, maPhong, giaThuePhong, ngayNhan, ngayTra");
                StringBuilder detailValues = new StringBuilder("?, ?, ?, ?, ?");
                if (hasDetailStatus) {
                    detailColumns.append(", trangThai");
                    detailValues.append(", ?");
                }
                if (hasActualCheckIn) {
                    detailColumns.append(", thoiDiemNhanThucTe");
                    detailValues.append(", ?");
                }
                if (hasActualCheckOut) {
                    detailColumns.append(", thoiDiemTraThucTe");
                    detailValues.append(", ?");
                }
                if (hasRoomSnapshot) {
                    detailColumns.append(", tenPhongSnapshot, loaiPhongSnapshot, donGiaPhongSnapshot");
                    detailValues.append(", ?, ?, ?");
                }
                String insertDetailSql = "INSERT INTO ChiTietPhieuDat (" + detailColumns + ") VALUES (" + detailValues + ")";

                try (PreparedStatement stmtHeader = con.prepareStatement("""
                         INSERT INTO PhieuDatPhong (
                             maDatPhong, ngayDat, trangThai, maNhanVien, maKhachHang,
                             soDemLuuTru, tongTienPhong, tienDatCoc, tienConLai,
                             tyLeDatCoc, hinhThucDatCoc, ngayDatCoc,
                             maNhanVienNhanCoc, ghiChuDatCoc,
                             soTienKhachDua, soTienThucNhan, tienThuaTraKhach
                         ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                         """);
                     PreparedStatement stmtDetail = con.prepareStatement(insertDetailSql);
                     PreparedStatement stmtDepositHistory = con.prepareStatement("""
                         INSERT INTO LichSuThanhToanCoc (
                             maDatPhong, loaiGiaoDich, soTien, hinhThuc, thoiGian, maNhanVien, ghiChu
                         ) VALUES (?, N'DAT_COC', ?, ?, ?, ?, ?)
                         """);
                     PreparedStatement stmtRoom = con.prepareStatement(
                             "UPDATE p SET p.trangThai = ? FROM Phong p WHERE p.maPhong = ?")) {

                    stmtHeader.setString(1, pdp.getMaDatPhong());
                    stmtHeader.setTimestamp(2, Timestamp.valueOf(pdp.getNgayDat()));
                    stmtHeader.setString(3, bookingStatus);
                    stmtHeader.setString(4, pdp.getNhanVien().getMaNhanVien());
                    stmtHeader.setString(5, pdp.getKhachHang().getMaKhachHang());
                    stmtHeader.setInt(6, pdp.getSoDemLuuTru());
                    stmtHeader.setDouble(7, pdp.getTongTienPhong());
                    stmtHeader.setDouble(8, pdp.getTienDatCoc());
                    stmtHeader.setDouble(9, pdp.getTienConLai());
                    stmtHeader.setDouble(10, pdp.getTyLeDatCoc());
                    stmtHeader.setString(11, pdp.getHinhThucDatCoc());
                    stmtHeader.setTimestamp(12, Timestamp.valueOf(pdp.getNgayDatCoc()));
                    stmtHeader.setString(13, pdp.getNhanVienNhanCoc() != null
                            ? pdp.getNhanVienNhanCoc().getMaNhanVien()
                            : pdp.getNhanVien().getMaNhanVien());
                    stmtHeader.setString(14, pdp.getGhiChuDatCoc());
                    stmtHeader.setDouble(15, pdp.getSoTienKhachDua());
                    stmtHeader.setDouble(16, pdp.getSoTienThucNhan());
                    stmtHeader.setDouble(17, pdp.getTienThuaTraKhach());
                    stmtHeader.executeUpdate();

                    stmtDepositHistory.setString(1, pdp.getMaDatPhong());
                    stmtDepositHistory.setDouble(2, pdp.getTienDatCoc());
                    stmtDepositHistory.setString(3, pdp.getHinhThucDatCoc());
                    stmtDepositHistory.setTimestamp(4, Timestamp.valueOf(pdp.getNgayDatCoc()));
                    stmtDepositHistory.setString(5, pdp.getNhanVienNhanCoc() != null
                            ? pdp.getNhanVienNhanCoc().getMaNhanVien()
                            : pdp.getNhanVien().getMaNhanVien());
                    stmtDepositHistory.setString(6, pdp.getGhiChuDatCoc());
                    stmtDepositHistory.executeUpdate();

                    String detailStatus = StatusUtils.isBookingStatus(pdp.getTrangThai(), BookingStatus.DA_NHAN_PHONG)
                            ? ChiTietPhieuDat.TRANG_THAI_DA_NHAN
                            : ChiTietPhieuDat.TRANG_THAI_CHO_NHAN;
                    String roomStatus = StatusUtils.isBookingStatus(pdp.getTrangThai(), BookingStatus.DA_NHAN_PHONG)
                            ? StatusUtils.roomCode(RoomStatus.DANG_O.getCode())
                            : StatusUtils.roomCode(RoomStatus.CHO_XAC_NHAN.getCode());

                    for (ChiTietPhieuDat ct : dsChiTiet) {
                        int detailIndex = 1;
                        stmtDetail.setString(detailIndex++, pdp.getMaDatPhong());
                        stmtDetail.setString(detailIndex++, ct.getPhong().getMaPhong());
                        stmtDetail.setDouble(detailIndex++, ct.getGiaThuePhong());
                        stmtDetail.setTimestamp(detailIndex++, Timestamp.valueOf(ct.getNgayNhan()));
                        stmtDetail.setTimestamp(detailIndex++, Timestamp.valueOf(ct.getNgayTra()));
                        if (hasDetailStatus) {
                            stmtDetail.setString(detailIndex++, detailStatus);
                        }
                        if (hasActualCheckIn) {
                            stmtDetail.setTimestamp(detailIndex++, StatusUtils.isBookingStatus(pdp.getTrangThai(), BookingStatus.DA_NHAN_PHONG)
                                    ? Timestamp.valueOf(ct.getNgayNhan())
                                    : null);
                        }
                        if (hasActualCheckOut) {
                            stmtDetail.setTimestamp(detailIndex++, null);
                        }
                        if (hasRoomSnapshot) {
                            Phong room = ct.getPhong();
                            stmtDetail.setString(detailIndex++, room != null ? room.getMaPhong() : null);
                            stmtDetail.setString(detailIndex++, room != null ? room.getLoaiPhong() : null);
                            stmtDetail.setDouble(detailIndex++, ct.getGiaThuePhong() > 0
                                    ? ct.getGiaThuePhong()
                                    : room != null ? room.getGiaPhong() : 0);
                        }
                        stmtDetail.executeUpdate();

                        stmtRoom.setString(1, roomStatus);
                        stmtRoom.setString(2, ct.getPhong().getMaPhong());
                        int updated = stmtRoom.executeUpdate();
                        if (updated <= 0) {
                            throw new SQLException("Khong cap nhat duoc trang thai phong " + ct.getPhong().getMaPhong());
                        }
                    }
                }

                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw new RuntimeException("Luu phieu dat phong that bai. " + buildSqlMessage(e), e);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the mo transaction dat phong. " + buildSqlMessage(e), e);
        }
    }

    public HuyDatPhong tinhHoanCoc(String maDatPhong, LocalDateTime ngayHuy) {
        PhieuDatPhong phieu = getPhieuDatPhongByMa(maDatPhong);
        if (phieu == null) {
            return null;
        }
        LocalDateTime cancelAt = ngayHuy != null ? ngayHuy : LocalDateTime.now();
        LocalDateTime depositAt = phieu.getNgayDatCoc() != null ? phieu.getNgayDatCoc() : phieu.getNgayDat();
        int days = DepositPolicy.daysSinceDeposit(depositAt, cancelAt);
        double refundRate = DepositPolicy.refundRatePercent(days);
        double depositAmount = phieu.getTienDatCoc();
        double refundAmount = DepositPolicy.calculateRefund(depositAmount, refundRate);

        HuyDatPhong huy = new HuyDatPhong();
        huy.setMaDatPhong(phieu.getMaDatPhong());
        huy.setKhachHang(phieu.getKhachHang());
        huy.setNgayDatCoc(depositAt);
        huy.setNgayHuy(cancelAt);
        huy.setSoNgaySauDatCoc(days);
        huy.setTienCocBanDau(depositAmount);
        huy.setTyLeHoanCoc(refundRate);
        huy.setTienHoanLai(refundAmount);
        huy.setTienKhachSanGiu(DepositPolicy.roundMoney(Math.max(0, depositAmount - refundAmount)));
        return huy;
    }

    public boolean huyPhieu(String maDatPhong) {
        HuyDatPhong request = tinhHoanCoc(maDatPhong, LocalDateTime.now());
        if (request == null) {
            return false;
        }
        request.setLyDoHuy("Huy phieu");
        request.setHinhThucHoanTien("Tien mat");
        return huyPhieu(request);
    }

    public boolean huyPhieu(HuyDatPhong request) {
        if (request == null || request.getMaDatPhong() == null || request.getMaDatPhong().isBlank()) {
            throw new IllegalArgumentException("Ma dat phong khong hop le.");
        }
        if (request.getLyDoHuy() == null || request.getLyDoHuy().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui long nhap ly do huy phong.");
        }

        String sqlGetBooking = """
                SELECT pdp.maDatPhong, pdp.maKhachHang, pdp.maNhanVien, pdp.ngayDat,
                       pdp.trangThai, pdp.tienDatCoc, pdp.ngayDatCoc,
                       kh.tenKhachHang, kh.soDienThoai
                FROM PhieuDatPhong pdp
                JOIN KhachHang kh ON kh.maKhachHang = pdp.maKhachHang
                WHERE pdp.maDatPhong = ?
                """;
        String sqlCheckOccupied = """
                SELECT COUNT(*) AS soPhongDangO
                FROM ChiTietPhieuDat ct
                JOIN Phong p ON p.maPhong = ct.maPhong
                WHERE ct.maDatPhong = ?
                  AND UPPER(ISNULL(p.trangThai, '')) = ?
                """;
        String sqlCancelBooking = """
                UPDATE PhieuDatPhong
                SET trangThai = ?
                WHERE maDatPhong = ?
                """;
        String sqlReleaseRooms = """
                UPDATE p
                SET p.trangThai = ?
                FROM Phong p
                JOIN ChiTietPhieuDat ct ON ct.maPhong = p.maPhong
                WHERE ct.maDatPhong = ?
                  AND UPPER(ISNULL(p.trangThai, '')) <> ?
                """;
        String sqlInsertCancel = """
                INSERT INTO HuyDatPhong (
                    maDatPhong, maKhachHang, ngayDatCoc, ngayHuy, soNgaySauDatCoc,
                    tienCocBanDau, tyLeHoanCoc, tienHoanLai, tienKhachSanGiu,
                    lyDoHuy, hinhThucHoanTien, maNhanVienXuLy, thoiGianXuLy
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String sqlInsertHistory = """
                INSERT INTO LichSuThanhToanCoc (
                    maDatPhong, loaiGiaoDich, soTien, hinhThuc, thoiGian, maNhanVien, ghiChu
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement getBooking = con.prepareStatement(sqlGetBooking);
                 PreparedStatement checkOccupied = con.prepareStatement(sqlCheckOccupied);
                 PreparedStatement cancelBooking = con.prepareStatement(sqlCancelBooking);
                 PreparedStatement releaseRooms = con.prepareStatement(sqlReleaseRooms);
                 PreparedStatement insertCancel = con.prepareStatement(sqlInsertCancel);
                 PreparedStatement insertHistory = con.prepareStatement(sqlInsertHistory)) {

                getBooking.setString(1, request.getMaDatPhong());
                String maKhachHang;
                String maNhanVienDatPhong;
                String currentStatus;
                LocalDateTime ngayDat;
                LocalDateTime ngayDatCoc;
                double tienDatCoc;
                try (ResultSet rs = getBooking.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                    maKhachHang = rs.getString("maKhachHang");
                    maNhanVienDatPhong = rs.getString("maNhanVien");
                    currentStatus = StatusUtils.bookingCode(rs.getString("trangThai"));
                    ngayDat = rs.getTimestamp("ngayDat").toLocalDateTime();
                    Timestamp depositTs = rs.getTimestamp("ngayDatCoc");
                    ngayDatCoc = depositTs != null ? depositTs.toLocalDateTime() : ngayDat;
                    tienDatCoc = rs.getDouble("tienDatCoc");
                }

                if (StatusUtils.isBookingStatus(currentStatus, BookingStatus.DA_THANH_TOAN)
                        || StatusUtils.isBookingStatus(currentStatus, BookingStatus.DA_HUY)
                        || StatusUtils.isBookingStatus(currentStatus, BookingStatus.DA_NHAN_PHONG)) {
                    con.rollback();
                    return false;
                }

                checkOccupied.setString(1, request.getMaDatPhong());
                checkOccupied.setString(2, StatusUtils.normalize(RoomStatus.DANG_O.getCode()));
                try (ResultSet rs = checkOccupied.executeQuery()) {
                    if (rs.next() && rs.getInt("soPhongDangO") > 0) {
                        throw new RuntimeException("Khong the huy phieu khi con phong dang o.");
                    }
                }

                LocalDateTime cancelAt = request.getNgayHuy() != null ? request.getNgayHuy() : LocalDateTime.now();
                LocalDateTime processedAt = LocalDateTime.now();
                int days = DepositPolicy.daysSinceDeposit(ngayDatCoc, cancelAt);
                double refundRate = DepositPolicy.refundRatePercent(days);
                double refundAmount = DepositPolicy.calculateRefund(tienDatCoc, refundRate);
                double retainedAmount = DepositPolicy.roundMoney(Math.max(0, tienDatCoc - refundAmount));
                String employeeId = request.getNhanVienXuLy() != null
                        && request.getNhanVienXuLy().getMaNhanVien() != null
                        && !request.getNhanVienXuLy().getMaNhanVien().isBlank()
                        ? request.getNhanVienXuLy().getMaNhanVien()
                        : maNhanVienDatPhong;

                insertCancel.setString(1, request.getMaDatPhong());
                insertCancel.setString(2, maKhachHang);
                insertCancel.setTimestamp(3, Timestamp.valueOf(ngayDatCoc));
                insertCancel.setTimestamp(4, Timestamp.valueOf(cancelAt));
                insertCancel.setInt(5, days);
                insertCancel.setDouble(6, tienDatCoc);
                insertCancel.setDouble(7, refundRate);
                insertCancel.setDouble(8, refundAmount);
                insertCancel.setDouble(9, retainedAmount);
                insertCancel.setString(10, request.getLyDoHuy().trim());
                insertCancel.setString(11, request.getHinhThucHoanTien());
                insertCancel.setString(12, employeeId);
                insertCancel.setTimestamp(13, Timestamp.valueOf(processedAt));
                insertCancel.executeUpdate();

                cancelBooking.setString(1, StatusUtils.bookingCode(BookingStatus.DA_HUY.getCode()));
                cancelBooking.setString(2, request.getMaDatPhong());
                if (cancelBooking.executeUpdate() <= 0) {
                    con.rollback();
                    return false;
                }

                releaseRooms.setString(1, StatusUtils.roomCode(RoomStatus.TRONG.getCode()));
                releaseRooms.setString(2, request.getMaDatPhong());
                releaseRooms.setString(3, StatusUtils.normalize(RoomStatus.DANG_O.getCode()));
                releaseRooms.executeUpdate();

                if (refundAmount > 0) {
                    insertDepositHistory(insertHistory, request.getMaDatPhong(), "HOAN_COC", refundAmount,
                            request.getHinhThucHoanTien(), processedAt, employeeId, request.getLyDoHuy());
                }
                if (retainedAmount > 0) {
                    insertDepositHistory(insertHistory, request.getMaDatPhong(), "GIU_COC", retainedAmount,
                            null, processedAt, employeeId, request.getLyDoHuy());
                }

                con.commit();

                request.setNgayDatCoc(ngayDatCoc);
                request.setNgayHuy(cancelAt);
                request.setSoNgaySauDatCoc(days);
                request.setTienCocBanDau(tienDatCoc);
                request.setTyLeHoanCoc(refundRate);
                request.setTienHoanLai(refundAmount);
                request.setTienKhachSanGiu(retainedAmount);
                request.setThoiGianXuLy(processedAt);
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw new RuntimeException("Khong the huy phieu dat phong. " + buildSqlMessage(e), e);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the mo transaction huy phieu. " + buildSqlMessage(e), e);
        }
    }

    public String generateNextId() {
        try (Connection con = ConnectDB.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT MAX(CAST(SUBSTRING(maDatPhong, 3, LEN(maDatPhong)) AS INT)) FROM PhieuDatPhong")) {
            if (rs.next()) {
                return String.format("DP%03d", rs.getInt(1) + 1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the sinh ma phieu dat phong moi. " + buildSqlMessage(e), e);
        }
        return "DP001";
    }

    private void validateBookingInput(PhieuDatPhong pdp, List<ChiTietPhieuDat> dsChiTiet) {
        if (pdp == null) {
            throw new IllegalArgumentException("Phieu dat phong khong duoc null.");
        }
        if (pdp.getMaDatPhong() == null || pdp.getMaDatPhong().isBlank()) {
            throw new IllegalArgumentException("Ma phieu dat phong khong hop le.");
        }
        if (pdp.getNhanVien() == null || pdp.getNhanVien().getMaNhanVien() == null || pdp.getNhanVien().getMaNhanVien().isBlank()) {
            throw new IllegalArgumentException("Ma nhan vien khong hop le.");
        }
        if (pdp.getKhachHang() == null || pdp.getKhachHang().getMaKhachHang() == null || pdp.getKhachHang().getMaKhachHang().isBlank()) {
            throw new IllegalArgumentException("Ma khach hang khong hop le.");
        }
        if (dsChiTiet == null || dsChiTiet.isEmpty()) {
            throw new IllegalArgumentException("Danh sach chi tiet dat phong khong duoc rong.");
        }

        for (ChiTietPhieuDat ct : dsChiTiet) {
            if (ct == null || ct.getPhong() == null || ct.getPhong().getMaPhong() == null || ct.getPhong().getMaPhong().isBlank()) {
                throw new IllegalArgumentException("Co chi tiet phong khong hop le.");
            }
            if (ct.getNgayNhan() == null || ct.getNgayTra() == null) {
                throw new IllegalArgumentException("Ngay nhan/ngay tra khong duoc null.");
            }
            if (ct.getNgayTra().isBefore(ct.getNgayNhan())) {
                throw new IllegalArgumentException("Ngay tra phai lon hon hoac bang ngay nhan.");
            }
        }
    }

    private void normalizeDepositForBooking(PhieuDatPhong pdp, List<ChiTietPhieuDat> dsChiTiet) {
        double computedTotal = dsChiTiet.stream()
                .mapToDouble(ChiTietPhieuDat::getGiaThuePhong)
                .sum();
        double totalRoomAmount = pdp.getTongTienPhong() > 0
                ? pdp.getTongTienPhong()
                : computedTotal;
        double requiredDeposit = DepositPolicy.calculateDeposit(totalRoomAmount);

        if (totalRoomAmount <= 0) {
            throw new IllegalArgumentException("Tong tien phong phai lon hon 0 de xac nhan dat coc.");
        }
        if (pdp.getHinhThucDatCoc() == null || pdp.getHinhThucDatCoc().isBlank()) {
            throw new IllegalArgumentException("Vui long chon hinh thuc dat coc.");
        }
        double customerPaid = pdp.getSoTienKhachDua() > 0
                ? pdp.getSoTienKhachDua()
                : pdp.getTienDatCoc() > 0 ? pdp.getTienDatCoc() : requiredDeposit;
        if (customerPaid + 0.01 < requiredDeposit) {
            throw new IllegalArgumentException("Tien coc toi thieu la 50% tong tien phong.");
        }
        double actualDeposit = DepositPolicy.calculateReceivedDeposit(totalRoomAmount, customerPaid);

        pdp.setTongTienPhong(DepositPolicy.roundMoney(totalRoomAmount));
        pdp.setTyLeDatCoc(DepositPolicy.DEPOSIT_RATE_PERCENT);
        pdp.setTienDatCoc(actualDeposit);
        pdp.setTienConLai(DepositPolicy.calculateRemaining(totalRoomAmount, actualDeposit));
        pdp.setSoTienKhachDua(DepositPolicy.roundMoney(customerPaid));
        pdp.setSoTienThucNhan(actualDeposit);
        pdp.setTienThuaTraKhach(DepositPolicy.calculateChange(totalRoomAmount, customerPaid));
        if (pdp.getNgayDatCoc() == null) {
            pdp.setNgayDatCoc(LocalDateTime.now());
        }
        if (pdp.getNhanVienNhanCoc() == null) {
            pdp.setNhanVienNhanCoc(pdp.getNhanVien());
        }
        pdp.setTrangThai(StatusUtils.bookingCode(pdp.getTrangThai()));
    }

    private void ensureDefaultBookingStatuses(Connection con) throws SQLException {
        if (!hasTable(con, "TrangThaiPhieuDat")) {
            return;
        }

        BookingStatus[] statuses = BookingStatus.values();
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < statuses.length; i++) {
            if (i > 0) {
                values.append(", ");
            }
            values.append("(?, ?, ?, ?)");
        }

        String sql = """
                MERGE dbo.TrangThaiPhieuDat AS target
                USING (VALUES %s) AS source(maCode, tenHienThi, moTa, thuTu)
                ON target.maCode = source.maCode
                WHEN NOT MATCHED THEN
                    INSERT (maCode, tenHienThi, moTa, thuTu)
                    VALUES (source.maCode, source.tenHienThi, source.moTa, source.thuTu);
                """.formatted(values);

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            int index = 1;
            for (int i = 0; i < statuses.length; i++) {
                BookingStatus status = statuses[i];
                stmt.setString(index++, status.getCode());
                stmt.setString(index++, status.getDisplayName());
                stmt.setString(index++, "Trang thai " + status.getDisplayName());
                stmt.setInt(index++, i + 1);
            }
            stmt.executeUpdate();
        }
    }

    private String resolveValidBookingStatus(Connection con, String rawStatus) throws SQLException {
        String status = StatusUtils.bookingCode(rawStatus);
        if (!hasTable(con, "TrangThaiPhieuDat") || bookingStatusExists(con, status)) {
            return status;
        }

        String defaultStatus = BookingStatus.CHO_XAC_NHAN.getCode();
        if (!defaultStatus.equals(status) && bookingStatusExists(con, defaultStatus)) {
            return defaultStatus;
        }

        throw new SQLException("Ma trang thai phieu dat khong ton tai trong TrangThaiPhieuDat(maCode): " + status);
    }

    private boolean bookingStatusExists(Connection con, String status) throws SQLException {
        String sql = "SELECT 1 FROM dbo.TrangThaiPhieuDat WHERE maCode = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insertDepositHistory(PreparedStatement stmt, String maDatPhong, String loaiGiaoDich,
                                      double soTien, String hinhThuc, LocalDateTime thoiGian,
                                      String maNhanVien, String ghiChu) throws SQLException {
        stmt.setString(1, maDatPhong);
        stmt.setString(2, loaiGiaoDich);
        stmt.setDouble(3, soTien);
        stmt.setString(4, hinhThuc);
        stmt.setTimestamp(5, Timestamp.valueOf(thoiGian));
        stmt.setString(6, maNhanVien);
        stmt.setString(7, ghiChu);
        stmt.executeUpdate();
    }

    private PhieuDatPhong mapPhieuDatPhong(ResultSet rs) throws SQLException {
        KhachHang kh = new KhachHang(
                rs.getString("maKhachHang"),
                rs.getString("tenKhachHang"),
                rs.getString("diaChi"),
                rs.getBoolean("gioiTinh"),
                rs.getString("soDienThoai"),
                getNullableString(rs, "loaiGiayTo"),
                getNullableString(rs, "soGiayTo"),
                getNullableString(rs, "trangThaiKhachHang")
        );

        PhieuDatPhong phieu = new PhieuDatPhong(
                rs.getString("maDatPhong"),
                rs.getTimestamp("ngayDat").toLocalDateTime(),
                rs.getString("trangThai"),
                new NhanVien(rs.getString("maNhanVien")),
                kh
        );

        Timestamp ngayNhan = rs.getTimestamp("ngayNhan");
        Timestamp ngayTra = rs.getTimestamp("ngayTra");
        phieu.setNgayNhan(ngayNhan != null ? ngayNhan.toLocalDateTime() : null);
        phieu.setNgayTra(ngayTra != null ? ngayTra.toLocalDateTime() : null);
        phieu.setDsMaPhong(rs.getString("dsMaPhong"));
        phieu.setSoLuongPhong(rs.getInt("soLuongPhong"));
        phieu.setSoDemLuuTru(getNullableInt(rs, "soDemLuuTru"));
        phieu.setTongTienPhong(getNullableDouble(rs, "tongTienPhong"));
        phieu.setTienDatCoc(getNullableDouble(rs, "tienDatCoc"));
        phieu.setTienConLai(getNullableDouble(rs, "tienConLai"));
        double tyLeDatCoc = getNullableDouble(rs, "tyLeDatCoc");
        phieu.setTyLeDatCoc(tyLeDatCoc > 0 ? tyLeDatCoc : DepositPolicy.DEPOSIT_RATE_PERCENT);
        phieu.setHinhThucDatCoc(getNullableString(rs, "hinhThucDatCoc"));
        Timestamp ngayDatCoc = getNullableTimestamp(rs, "ngayDatCoc");
        phieu.setNgayDatCoc(ngayDatCoc != null ? ngayDatCoc.toLocalDateTime() : null);
        String maNhanVienNhanCoc = getNullableString(rs, "maNhanVienNhanCoc");
        if (maNhanVienNhanCoc != null && !maNhanVienNhanCoc.isBlank()) {
            phieu.setNhanVienNhanCoc(new NhanVien(maNhanVienNhanCoc));
        }
        phieu.setGhiChuDatCoc(getNullableString(rs, "ghiChuDatCoc"));
        phieu.setSoTienKhachDua(getNullableDouble(rs, "soTienKhachDua"));
        phieu.setSoTienThucNhan(getNullableDouble(rs, "soTienThucNhan"));
        phieu.setTienThuaTraKhach(getNullableDouble(rs, "tienThuaTraKhach"));
        return phieu;
    }

    private boolean hasTable(Connection con, String tableName) throws SQLException {
        String sql = """
                SELECT 1
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = 'dbo'
                  AND TABLE_NAME = ?
                  AND TABLE_TYPE = 'BASE TABLE'
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasTableColumn(Connection con, String tableName, String columnName) throws SQLException {
        String sql = """
                SELECT 1
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            stmt.setString(2, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String statusInCondition(String columnExpression, int valueCount) {
        StringJoiner placeholders = new StringJoiner(", ");
        for (int i = 0; i < valueCount; i++) {
            placeholders.add("?");
        }
        return "UPPER(ISNULL(" + columnExpression + ", N'')) IN (" + placeholders + ")";
    }

    private int bindUpperStatuses(PreparedStatement stmt, int startIndex, String[] statuses) throws SQLException {
        int index = startIndex;
        for (String status : statuses) {
            stmt.setString(index++, status == null ? "" : status.toUpperCase(Locale.ROOT));
        }
        return index;
    }

    private String detailStatusExpression(boolean hasDetailStatus) {
        String activeRoomStatusCondition = activeRoomStatusCondition();
        if (hasDetailStatus) {
            return """
                    CASE
                        WHEN UPPER(ISNULL(pdp.trangThai, '')) IN ('DA_THANH_TOAN', 'DATHANHTOAN', 'DA_HUY', 'DAHUY', 'HUY') THEN N'DA_TRA'
                        WHEN UPPER(ISNULL(ct.trangThai, '')) IN ('DA_TRA', 'DATRA', 'DA_TRA_PHONG', 'DATRAPHONG', 'DA_THANH_TOAN', 'DATHANHTOAN') THEN N'DA_TRA'
                        WHEN (
                            UPPER(ISNULL(ct.trangThai, '')) IN ('DA_NHAN', 'DANHAN', 'DA_NHAN_PHONG', 'DANHANPHONG', 'DANG_O', 'DANGO', 'NHANPHONG')
                            OR %s
                        ) THEN N'DA_NHAN'
                        ELSE N'CHO_NHAN'
                    END
                    """.formatted(activeRoomStatusCondition);
        }

        return """
                CASE
                    WHEN UPPER(ISNULL(pdp.trangThai, '')) IN ('DA_THANH_TOAN', 'DATHANHTOAN', 'DA_HUY', 'DAHUY', 'HUY') THEN N'DA_TRA'
                    WHEN %s THEN N'DA_NHAN'
                    ELSE N'CHO_NHAN'
                END
                """.formatted(activeRoomStatusCondition);
    }

    private String activeRoomStatusCondition() {
        return """
                UPPER(ISNULL(p.trangThai, '')) IN (
                    'DA_NHAN', 'DANHAN',
                    'DA_NHAN_PHONG', 'DANHANPHONG',
                    'DANG_O', 'DANGO',
                    'NHANPHONG'
                )
                """;
    }

    private String buildSqlMessage(SQLException e) {
        return "SQLState=" + e.getSQLState() + ", ErrorCode=" + e.getErrorCode() + ", Message=" + e.getMessage();
    }

    private String getNullableString(ResultSet rs, String columnName) throws SQLException {
        try {
            return rs.getString(columnName);
        } catch (SQLException e) {
            return null;
        }
    }

    private int getNullableInt(ResultSet rs, String columnName) {
        try {
            return rs.getInt(columnName);
        } catch (SQLException e) {
            return 0;
        }
    }

    private double getNullableDouble(ResultSet rs, String columnName) {
        try {
            return rs.getDouble(columnName);
        } catch (SQLException e) {
            return 0;
        }
    }

    private Timestamp getNullableTimestamp(ResultSet rs, String columnName) {
        try {
            return rs.getTimestamp(columnName);
        } catch (SQLException e) {
            return null;
        }
    }
}
