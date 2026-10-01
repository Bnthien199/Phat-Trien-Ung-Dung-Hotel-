package dao;

import connectDB.ConnectDB;
import entity.ChiTietPhieuDat;
import entity.KhachHang;
import entity.PhieuDatPhong;
import entity.Phong;
import util.BookingStatus;
import util.RoomStatus;
import util.StatusUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public class ChiTietPhieuDatDAO {

    private static final String[] PENDING_CHECK_IN_STATUSES = {
            ChiTietPhieuDat.TRANG_THAI_CHO_NHAN,
            "CHO_NHAN_PHONG",
            "CHO NHAN",
            "CHO NHAN PHONG",
            "CHO_CHECK_IN",
            "CHO_CHECKIN",
            "CHO CHECK IN",
            "CHO CHECK-IN",
            "Chờ nhận phòng",
            "Chờ check-in"
    };

    private static final String[] OCCUPIED_ROOM_STATUSES = {
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
    private static final String INVALID_ROOM_STATUS_UPDATE_MESSAGE =
            "Cập nhật trạng thái phòng không hợp lệ. Vui lòng tải lại dữ liệu.";

    public List<ChiTietPhieuDat> getDSChiTietByMaPhieu(String maPhieu) {
        return getDSChiTietByMaPhieuAndStatuses(maPhieu);
    }

    public List<ChiTietPhieuDat> getDSChiTietByMaPhieuAndStatuses(String maPhieu, String... trangThaiList) {
        List<ChiTietPhieuDat> ds = new ArrayList<>();
        List<String> normalizedStatuses = normalizeStatuses(trangThaiList);
        String sql = "";

        try (Connection con = ConnectDB.getConnection()) {
            String detailStatusExpression = detailStatusExpression(hasTableColumn(con, "ChiTietPhieuDat", "trangThai"));
            StringBuilder sqlBuilder = new StringBuilder("""
                    SELECT ct.*, %s AS ttChiTiet,
                           pdp.trangThai AS trangThaiPhieuDat,
                           pdp.trangThai AS ttPhieu,
                           p.loaiPhong, p.giaPhong,
                           p.trangThai AS trangThaiPhong,
                           p.trangThai AS ttPhong,
                           p.*
                    FROM ChiTietPhieuDat ct
                    JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                    JOIN Phong p ON ct.maPhong = p.maPhong
                    WHERE ct.maDatPhong = ?
                    """.formatted(detailStatusExpression));

            if (!normalizedStatuses.isEmpty()) {
                sqlBuilder.append(" AND UPPER(ISNULL(").append(detailStatusExpression).append(", '')) IN (");
                StringJoiner joiner = new StringJoiner(", ");
                for (int i = 0; i < normalizedStatuses.size(); i++) {
                    joiner.add("?");
                }
                sqlBuilder.append(joiner).append(')');
            }
            sqlBuilder.append(" ORDER BY ct.ngayNhan, ct.maPhong");
            sql = sqlBuilder.toString();

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, maPhieu);
                int index = 2;
                for (String status : normalizedStatuses) {
                    stmt.setString(index++, status);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ds.add(mapChiTietPhieuDat(rs, false));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể taọ chi tiết phiếu đặt. SQL=" + sql + ". " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public List<ChiTietPhieuDat> getDanhSachDangOByMaPhieu(String maPhieu) {
        return getDSChiTietByMaPhieuAndStatuses(maPhieu, ChiTietPhieuDat.TRANG_THAI_DA_NHAN);
    }

    public List<ChiTietPhieuDat> getDanhSachHoaDonByMaPhieu(String maPhieu) {
        return getDSChiTietByMaPhieuAndStatuses(maPhieu, ChiTietPhieuDat.TRANG_THAI_DA_TRA);
    }

    public List<ChiTietPhieuDat> getChiTietDaTra(String maPhieu) {
        List<ChiTietPhieuDat> ds = new ArrayList<>();
        String sql = "";
        try (Connection con = ConnectDB.getConnection()) {
            String detailStatusExpression = detailStatusExpression(hasTableColumn(con, "ChiTietPhieuDat", "trangThai"));
            sql = """
                    SELECT ct.*, %s AS ttChiTiet,
                           pdp.trangThai AS trangThaiPhieuDat,
                           pdp.trangThai AS ttPhieu,
                           p.loaiPhong, p.giaPhong,
                           p.trangThai AS trangThaiPhong,
                           p.trangThai AS ttPhong,
                           p.*
                    FROM ChiTietPhieuDat ct
                    JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                    JOIN Phong p ON ct.maPhong = p.maPhong
                    WHERE ct.maDatPhong = ?
                    AND UPPER(ISNULL(%s, '')) = 'DA_TRA'
                    ORDER BY ct.ngayNhan, ct.maPhong
                    """.formatted(detailStatusExpression, detailStatusExpression);

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, maPhieu);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ds.add(mapChiTietPhieuDat(rs, false));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "Lỗi cơ sở dữ liệu (Chi tiết đã trả): " + e.getMessage(), "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
        return ds;
    }

    public List<ChiTietPhieuDat> getChiTietPhongHoaDon(String maPhieu) {
        List<ChiTietPhieuDat> ds = new ArrayList<>();
        try (Connection con = ConnectDB.getConnection()) {
            boolean hasRoomSnapshot = hasTableColumn(con, "ChiTietPhieuDat", "loaiPhongSnapshot")
                    && hasTableColumn(con, "ChiTietPhieuDat", "donGiaPhongSnapshot");
            String detailStatusExpression = detailStatusExpression(
                    hasTableColumn(con, "ChiTietPhieuDat", "trangThai"),
                    !hasRoomSnapshot);
            String sql = hasRoomSnapshot
                    ? """
                    SELECT ct.*, %s AS ttChiTiet,
                           pdp.trangThai AS trangThaiPhieuDat,
                           pdp.trangThai AS ttPhieu,
                           COALESCE(ct.loaiPhongSnapshot, '') AS loaiPhong,
                           COALESCE(ct.donGiaPhongSnapshot, ct.giaThuePhong) AS giaPhong,
                           N'TRONG' AS trangThaiPhong,
                           N'TRONG' AS ttPhong,
                           1 AS soNguoi
                    FROM ChiTietPhieuDat ct
                    JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                    WHERE ct.maDatPhong = ?
                      AND UPPER(ISNULL(%s, '')) = 'DA_TRA'
                    ORDER BY ct.ngayNhan, ct.maPhong
                    """.formatted(detailStatusExpression, detailStatusExpression)
                    : """
                    SELECT ct.*, %s AS ttChiTiet,
                           pdp.trangThai AS trangThaiPhieuDat,
                           pdp.trangThai AS ttPhieu,
                           p.loaiPhong, p.giaPhong,
                           p.trangThai AS trangThaiPhong,
                           p.trangThai AS ttPhong,
                           p.*
                    FROM ChiTietPhieuDat ct
                    JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                    JOIN Phong p ON ct.maPhong = p.maPhong
                    WHERE ct.maDatPhong = ?
                      AND UPPER(ISNULL(%s, '')) = 'DA_TRA'
                    ORDER BY ct.ngayNhan, ct.maPhong
                    """.formatted(detailStatusExpression, detailStatusExpression);
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, maPhieu);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ChiTietPhieuDat detail = mapChiTietPhieuDat(rs, false);
                        ds.add(detail);
                        if (detail.getPhong() != null) {
                            System.out.println("[INVOICE ROOM] " + detail.getPhong().getMaPhong());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("[SQL ERROR] " + e.getMessage());
            throw new RuntimeException("Khong the tai chi tiet phong hoa don. " + buildSqlMessage(e), e);
        }
        System.out.println("[INVOICE LOAD] rooms = " + ds.size());
        return ds;
    }

    public boolean create(ChiTietPhieuDat ct) {
        try (Connection con = ConnectDB.getConnection()) {
            boolean hasDetailStatus = hasTableColumn(con, "ChiTietPhieuDat", "trangThai");
            boolean hasActualCheckIn = hasTableColumn(con, "ChiTietPhieuDat", "thoiDiemNhanThucTe");
            boolean hasActualCheckOut = hasTableColumn(con, "ChiTietPhieuDat", "thoiDiemTraThucTe");
            boolean hasRoomSnapshot = hasTableColumn(con, "ChiTietPhieuDat", "tenPhongSnapshot")
                    && hasTableColumn(con, "ChiTietPhieuDat", "loaiPhongSnapshot")
                    && hasTableColumn(con, "ChiTietPhieuDat", "donGiaPhongSnapshot");
            StringBuilder columns = new StringBuilder("maDatPhong, maPhong, giaThuePhong, ngayNhan, ngayTra");
            StringBuilder values = new StringBuilder("?, ?, ?, ?, ?");
            if (hasDetailStatus) {
                columns.append(", trangThai");
                values.append(", ?");
            }
            if (hasActualCheckIn) {
                columns.append(", thoiDiemNhanThucTe");
                values.append(", ?");
            }
            if (hasActualCheckOut) {
                columns.append(", thoiDiemTraThucTe");
                values.append(", ?");
            }
            if (hasRoomSnapshot) {
                columns.append(", tenPhongSnapshot, loaiPhongSnapshot, donGiaPhongSnapshot");
                values.append(", ?, ?, ?");
            }
            String sql = "INSERT INTO ChiTietPhieuDat (" + columns + ") VALUES (" + values + ")";

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                int index = 1;
                stmt.setString(index++, ct.getPhieuDatPhong().getMaDatPhong());
                stmt.setString(index++, ct.getPhong().getMaPhong());
                stmt.setDouble(index++, ct.getGiaThuePhong());
                stmt.setTimestamp(index++, Timestamp.valueOf(ct.getNgayNhan()));
                stmt.setTimestamp(index++, Timestamp.valueOf(ct.getNgayTra()));
                if (hasDetailStatus) {
                    stmt.setString(index++, normalizeStatus(ct.getTrangThai()));
                }
                if (hasActualCheckIn) {
                    stmt.setTimestamp(index++, toTimestamp(ct.getThoiDiemNhanThucTe()));
                }
                if (hasActualCheckOut) {
                    stmt.setTimestamp(index++, toTimestamp(ct.getThoiDiemTraThucTe()));
                }
                if (hasRoomSnapshot) {
                    Phong room = ct.getPhong();
                    stmt.setString(index++, room != null ? room.getMaPhong() : null);
                    stmt.setString(index++, room != null ? room.getLoaiPhong() : null);
                    stmt.setDouble(index++, ct.getGiaThuePhong() > 0
                            ? ct.getGiaThuePhong()
                            : room != null ? room.getGiaPhong() : 0);
                }
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tao chi tiet phieu dat. " + buildSqlMessage(e), e);
        }
    }

    public List<ChiTietPhieuDat> getChiTietByRoom(String maPhong) {
        List<ChiTietPhieuDat> ds = new ArrayList<>();
        String sql = "";
        try (Connection con = ConnectDB.getConnection()) {
            String detailStatusExpression = detailStatusExpression(hasTableColumn(con, "ChiTietPhieuDat", "trangThai"));
            sql = """
                    SELECT ct.*, %s AS ttChiTiet,
                           pdp.trangThai AS trangThaiPhieuDat,
                           pdp.trangThai AS ttPhieu, pdp.maKhachHang,
                           kh.tenKhachHang, kh.soDienThoai, kh.diaChi, kh.gioiTinh, kh.loaiGiayTo, kh.soGiayTo, kh.TrangThai AS trangThaiKhachHang,
                           p.loaiPhong, p.giaPhong,
                           p.trangThai AS trangThaiPhong,
                           p.trangThai AS ttPhong,
                           p.*
                    FROM ChiTietPhieuDat ct
                    JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                    LEFT JOIN KhachHang kh ON kh.maKhachHang = pdp.maKhachHang
                    JOIN Phong p ON ct.maPhong = p.maPhong
                    WHERE ct.maPhong = ?
                    ORDER BY ct.ngayNhan DESC
                    """.formatted(detailStatusExpression);

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, maPhong);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ds.add(mapChiTietPhieuDat(rs, true));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tai chi tiet dat phong theo phong. " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public ChiTietPhieuDat findOccupiedStayAt(String maPhong, LocalDateTime selectedDateTime) {
        List<ChiTietPhieuDat> candidates = getTimelineCandidates(maPhong, selectedDateTime, true);
        for (ChiTietPhieuDat detail : candidates) {
            if (isOccupiedAt(detail, selectedDateTime)) {
                return detail;
            }
        }
        return null;
    }

    public ChiTietPhieuDat findReservedStayAt(String maPhong, LocalDateTime selectedDateTime) {
        List<ChiTietPhieuDat> candidates = getTimelineCandidates(maPhong, selectedDateTime, false);
        for (ChiTietPhieuDat detail : candidates) {
            if (isReservedAt(detail, selectedDateTime)) {
                return detail;
            }
        }
        return null;
    }

    public ChiTietPhieuDat findStayAt(String maPhong, LocalDateTime selectedDateTime) {
        LocalDateTime target = selectedDateTime != null ? selectedDateTime : LocalDateTime.now();
        List<ChiTietPhieuDat> candidates = getTimelineCandidatesAt(maPhong, target);
        for (ChiTietPhieuDat detail : candidates) {
            if (!isCanceled(detail)) {
                return detail;
            }
        }
        return null;
    }

    public ChiTietPhieuDat findActiveStayByRoom(String maPhong) {
        if (maPhong == null || maPhong.isBlank()) {
            return null;
        }
        String sql = """
                SELECT TOP 1
                    ct.*,
                    ct.trangThai AS ttChiTiet,
                    pdp.trangThai AS trangThaiPhieuDat,
                    pdp.trangThai AS ttPhieu,
                    pdp.maKhachHang,
                    kh.tenKhachHang,
                    kh.soDienThoai,
                    kh.diaChi,
                    kh.gioiTinh,
                    kh.loaiGiayTo,
                    kh.soGiayTo,
                    kh.TrangThai AS trangThaiKhachHang,
                    p.loaiPhong,
                    p.giaPhong,
                    p.trangThai AS trangThaiPhong,
                    p.trangThai AS ttPhong,
                    p.*
                FROM ChiTietPhieuDat ct
                JOIN PhieuDatPhong pdp ON ct.maDatPhong = pdp.maDatPhong
                JOIN KhachHang kh ON pdp.maKhachHang = kh.maKhachHang
                JOIN Phong p ON ct.maPhong = p.maPhong
                WHERE ct.maPhong = ?
                  AND (
                      pdp.trangThai IN (
                          'CHO_NHAN', 'DA_NHAN', 'DANG_O', 'DANG_SU_DUNG',
                          'CHO_NHAN_PHONG', 'DA_NHAN_PHONG', 'CHO_XAC_NHAN', 'DA_XAC_NHAN',
                          'DA_DAT_COC', 'CHO_COC',
                          N'CHO_NHAN', N'DA_NHAN', N'DANG_O', N'DANG_SU_DUNG',
                          N'Chờ nhận phòng', N'Đang ở', N'Đã nhận phòng'
                      )
                      OR pdp.trangThai NOT IN ('DA_THANH_TOAN', 'DAHUY', 'DA_HUY', 'DATHANHTOAN', N'Đã thanh toán', N'Đã hủy')
                  )
                ORDER BY ct.ngayNhan DESC
                """;
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapChiTietPhieuDat(rs, true);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    private List<ChiTietPhieuDat> getTimelineCandidatesAt(String maPhong, LocalDateTime selectedDateTime) {
        List<ChiTietPhieuDat> ds = new ArrayList<>();
        if (maPhong == null || maPhong.isBlank()) {
            return ds;
        }

        LocalDateTime target = selectedDateTime != null ? selectedDateTime : LocalDateTime.now();
        LocalDate targetDate = target.toLocalDate();
        String sql = "";

        try (Connection con = ConnectDB.getConnection()) {
            String detailStatusExpression = detailStatusExpression(hasTableColumn(con, "ChiTietPhieuDat", "trangThai"));
            sql = """
                    SELECT ct.*, %s AS ttChiTiet,
                           pdp.trangThai AS trangThaiPhieuDat,
                           pdp.trangThai AS ttPhieu, pdp.maKhachHang,
                           kh.tenKhachHang, kh.soDienThoai, kh.diaChi, kh.gioiTinh, kh.loaiGiayTo, kh.soGiayTo, kh.TrangThai AS trangThaiKhachHang,
                           p.loaiPhong, p.giaPhong,
                           p.trangThai AS trangThaiPhong,
                           p.trangThai AS ttPhong,
                           p.*
                    FROM ChiTietPhieuDat ct
                    JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                    LEFT JOIN KhachHang kh ON kh.maKhachHang = pdp.maKhachHang
                    JOIN Phong p ON ct.maPhong = p.maPhong
                    WHERE ct.maPhong = ?
                      AND CAST(ct.ngayNhan AS DATE) <= ?
                      AND (
                          ct.ngayTra IS NULL
                          OR CAST(ct.ngayTra AS DATE) >= ?
                      )
                    ORDER BY ct.ngayNhan DESC
                    """.formatted(detailStatusExpression);

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, maPhong);
                stmt.setDate(2, java.sql.Date.valueOf(targetDate));
                stmt.setDate(3, java.sql.Date.valueOf(targetDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ds.add(mapChiTietPhieuDat(rs, true));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the truy van timeline phong theo thoi diem. " + buildSqlMessage(e), e);
        }
        return ds;
    }

    private List<ChiTietPhieuDat> getTimelineCandidates(String maPhong, LocalDateTime selectedDateTime, boolean occupiedLookup) {
        List<ChiTietPhieuDat> ds = new ArrayList<>();
        LocalDateTime target = selectedDateTime != null ? selectedDateTime : LocalDateTime.now();
        String timePredicate = occupiedLookup
                ? """
                  ct.ngayNhan <= ?
                  AND (ct.ngayTra IS NULL OR ct.ngayTra > ?)
                  """
                : """
                  ct.ngayNhan <= ?
                  AND (ct.ngayTra IS NULL OR ct.ngayTra > ?)
                  """;
        String sql = "";

        try (Connection con = ConnectDB.getConnection()) {
            String detailStatusExpression = detailStatusExpression(hasTableColumn(con, "ChiTietPhieuDat", "trangThai"));
            sql = """
                    SELECT ct.*, %s AS ttChiTiet,
                           pdp.trangThai AS trangThaiPhieuDat,
                           pdp.trangThai AS ttPhieu, pdp.maKhachHang,
                           kh.tenKhachHang, kh.soDienThoai, kh.diaChi, kh.gioiTinh, kh.loaiGiayTo, kh.soGiayTo, kh.TrangThai AS trangThaiKhachHang,
                           p.loaiPhong, p.giaPhong,
                           p.trangThai AS trangThaiPhong,
                           p.trangThai AS ttPhong,
                           p.*
                    FROM ChiTietPhieuDat ct
                    JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                    LEFT JOIN KhachHang kh ON kh.maKhachHang = pdp.maKhachHang
                    JOIN Phong p ON ct.maPhong = p.maPhong
                    WHERE ct.maPhong = ?
                      AND %s
                    ORDER BY ct.ngayNhan DESC
                    """.formatted(detailStatusExpression, timePredicate);

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, maPhong);
                stmt.setTimestamp(2, Timestamp.valueOf(target));
                stmt.setTimestamp(3, Timestamp.valueOf(target));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ds.add(mapChiTietPhieuDat(rs, true));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the truy van timeline phong. " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public List<ChiTietPhieuDat> getDanhSachChoCheckIn() {
        List<ChiTietPhieuDat> ds = new ArrayList<>();
        String sql = "";

        try (Connection con = ConnectDB.getConnection()) {
            boolean hasDetailStatus = hasTableColumn(con, "ChiTietPhieuDat", "trangThai");
            String detailStatusExpression = detailStatusExpression(hasDetailStatus);
            String pendingCondition = hasDetailStatus
                    ? statusInCondition("ct.trangThai", PENDING_CHECK_IN_STATUSES.length)
                    : "1 = 1";
            sql = """
                    SELECT
                        ct.*, %s AS ttChiTiet,
                        pdp.trangThai AS trangThaiPhieuDat,
                        pdp.trangThai AS ttPhieu,
                        pdp.maKhachHang,
                        kh.tenKhachHang,
                        kh.TrangThai AS trangThaiKhachHang,
                        p.*, p.trangThai AS trangThaiPhong, p.trangThai AS ttPhong
                    FROM ChiTietPhieuDat ct
                    JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                    JOIN KhachHang kh ON kh.maKhachHang = pdp.maKhachHang
                    JOIN Phong p ON p.maPhong = ct.maPhong
                    WHERE %s
                      AND NOT (%s)
                    ORDER BY ct.ngayNhan, ct.maDatPhong, ct.maPhong
                    """.formatted(detailStatusExpression, pendingCondition, statusInCondition("p.trangThai", OCCUPIED_ROOM_STATUSES.length));

            try (PreparedStatement stmt = con.prepareStatement(sql)) {

                int index = 1;
                if (hasDetailStatus) {
                    index = bindUpperStatuses(stmt, index, PENDING_CHECK_IN_STATUSES);
                }
                bindUpperStatuses(stmt, index, OCCUPIED_ROOM_STATUSES);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ChiTietPhieuDat detail = mapChiTietPhieuDat(rs, true);
                        if (isPendingCheckInCandidate(detail)) {
                            ds.add(detail);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi truy van danh sach cho check-in. " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public List<ChiTietPhieuDat> getPendingRoomsByBooking(String maPhieu) {
        List<ChiTietPhieuDat> ds = new ArrayList<>();
        if (maPhieu == null || maPhieu.isBlank()) {
            return ds;
        }

        String sql = "";
        try (Connection con = ConnectDB.getConnection()) {
            boolean hasDetailStatus = hasTableColumn(con, "ChiTietPhieuDat", "trangThai");
            String detailStatusExpression = detailStatusExpression(hasDetailStatus);
            String pendingCondition = hasDetailStatus
                    ? statusInCondition("ct.trangThai", PENDING_CHECK_IN_STATUSES.length)
                    : "1 = 1";
            sql = """
                    SELECT
                        ct.*, %s AS ttChiTiet,
                        pdp.trangThai AS trangThaiPhieuDat,
                        pdp.trangThai AS ttPhieu,
                        pdp.maKhachHang,
                        kh.tenKhachHang,
                        p.*, p.trangThai AS trangThaiPhong, p.trangThai AS ttPhong
                    FROM ChiTietPhieuDat ct
                    JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                    JOIN KhachHang kh ON kh.maKhachHang = pdp.maKhachHang
                    JOIN Phong p ON p.maPhong = ct.maPhong
                    WHERE ct.maDatPhong = ?
                      AND %s
                      AND NOT (%s)
                    ORDER BY ct.ngayNhan, ct.maPhong
                    """.formatted(detailStatusExpression, pendingCondition, statusInCondition("p.trangThai", OCCUPIED_ROOM_STATUSES.length));

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                int index = 1;
                stmt.setString(index++, maPhieu);
                if (hasDetailStatus) {
                    index = bindUpperStatuses(stmt, index, PENDING_CHECK_IN_STATUSES);
                }
                bindUpperStatuses(stmt, index, OCCUPIED_ROOM_STATUSES);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ChiTietPhieuDat detail = mapChiTietPhieuDat(rs, true);
                        if (isPendingCheckInCandidate(detail)) {
                            ds.add(detail);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi truy van phong cho check-in theo phieu. SQL=" + sql + ". " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public List<ChiTietPhieuDat> getCheckoutRoomsByBooking(String maPhieu) {
        List<ChiTietPhieuDat> ds = new ArrayList<>();
        if (maPhieu == null || maPhieu.isBlank()) {
            return ds;
        }

        String sql = "";
        try (Connection con = ConnectDB.getConnection()) {
            boolean hasDetailStatus = hasTableColumn(con, "ChiTietPhieuDat", "trangThai");
            String selectedDetailStatus = hasDetailStatus ? "ct.trangThai" : detailStatusExpression(false);
            String checkoutCondition = hasDetailStatus
                    ? statusInCondition("ct.trangThai", ACTIVE_CHECKOUT_STATUSES.length)
                    : "UPPER(ISNULL(" + selectedDetailStatus + ", '')) = ?";
            sql = """
                    SELECT
                        ct.*, %s AS ttChiTiet,
                        pdp.trangThai AS trangThaiPhieuDat,
                        pdp.trangThai AS ttPhieu,
                        pdp.maKhachHang,
                        kh.tenKhachHang,
                        p.*, p.trangThai AS trangThaiPhong, p.trangThai AS ttPhong
                    FROM ChiTietPhieuDat ct
                    JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                    JOIN KhachHang kh ON kh.maKhachHang = pdp.maKhachHang
                    JOIN Phong p ON p.maPhong = ct.maPhong
                    WHERE ct.maDatPhong = ?
                      AND %s
                    ORDER BY ct.ngayNhan, ct.maPhong
                    """.formatted(selectedDetailStatus, checkoutCondition);

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                int index = 1;
                stmt.setString(index++, maPhieu);
                if (hasDetailStatus) {
                    bindUpperStatuses(stmt, index, ACTIVE_CHECKOUT_STATUSES);
                } else {
                    stmt.setString(index, ChiTietPhieuDat.TRANG_THAI_DA_NHAN);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ChiTietPhieuDat detail = mapChiTietPhieuDat(rs, true);
                        if (isCheckoutCandidate(detail)) {
                            ds.add(detail);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi truy van phong dang o theo phieu. SQL=" + sql + ". " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public boolean checkInTungPhong(String maDatPhong, String maPhong) {
        List<ChiTietPhieuDat> selectedRooms = new ArrayList<>();
        selectedRooms.add(new ChiTietPhieuDat(new PhieuDatPhong(maDatPhong), new Phong(maPhong), 0, null, null));
        return checkInNhieuPhong(selectedRooms);
    }

    public boolean checkInNhieuPhong(List<ChiTietPhieuDat> selectedRooms) {
        if (selectedRooms == null || selectedRooms.isEmpty()) {
            return false;
        }

        Map<String, List<String>> roomsByBooking = new LinkedHashMap<>();
        for (ChiTietPhieuDat detail : selectedRooms) {
            if (detail == null || detail.getPhieuDatPhong() == null || detail.getPhong() == null) {
                continue;
            }
            String maDatPhong = detail.getPhieuDatPhong().getMaDatPhong();
            String maPhong = detail.getPhong().getMaPhong();
            if (maDatPhong == null || maDatPhong.isBlank() || maPhong == null || maPhong.isBlank()) {
                continue;
            }
            roomsByBooking.computeIfAbsent(maDatPhong, key -> new ArrayList<>());
            if (!roomsByBooking.get(maDatPhong).contains(maPhong)) {
                roomsByBooking.get(maDatPhong).add(maPhong);
            }
        }

        if (roomsByBooking.isEmpty()) {
            return false;
        }

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);
            boolean hasDetailStatus = hasTableColumn(con, "ChiTietPhieuDat", "trangThai");
            boolean hasActualCheckIn = hasTableColumn(con, "ChiTietPhieuDat", "thoiDiemNhanThucTe");
            String updateDetailSql = buildCheckInDetailUpdateSql(hasDetailStatus, hasActualCheckIn);
            String detailStatusExpression = detailStatusExpression(hasDetailStatus);
            String rawDetailStatusSelect = hasDetailStatus ? "ct.trangThai" : "NULL";

            try (PreparedStatement checkDetail = con.prepareStatement("""
                         SELECT TOP 1 %s AS ttChiTiet,
                                %s AS trangThaiChiTietRaw,
                                pdp.trangThai AS ttPhieu,
                                p.trangThai AS ttPhong
                         FROM ChiTietPhieuDat ct
                         JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                         JOIN Phong p ON p.maPhong = ct.maPhong
                         WHERE ct.maDatPhong = ?
                           AND ct.maPhong = ?
                         """.formatted(detailStatusExpression, rawDetailStatusSelect));
                 PreparedStatement updateDetail = updateDetailSql != null ? con.prepareStatement(updateDetailSql) : null;
                 PreparedStatement updatePhong = con.prepareStatement("""
                         UPDATE p
                         SET p.trangThai = ?
                         FROM Phong p
                         WHERE p.maPhong = ?
                         """);
                 PreparedStatement updateBooking = con.prepareStatement("""
                         UPDATE pdp
                         SET pdp.trangThai = ?
                         FROM PhieuDatPhong pdp
                         WHERE pdp.maDatPhong = ?
                           AND UPPER(ISNULL(pdp.trangThai, '')) NOT IN (?, ?)
                         """)) {

                for (Map.Entry<String, List<String>> entry : roomsByBooking.entrySet()) {
                    String maDatPhong = entry.getKey();

                    for (String maPhong : entry.getValue()) {
                        String detailStatus;
                        checkDetail.setString(1, maDatPhong);
                        checkDetail.setString(2, maPhong);
                        try (ResultSet rs = checkDetail.executeQuery()) {
                            if (!rs.next()) {
                                con.rollback();
                                return false;
                            }
                            detailStatus = hasDetailStatus
                                    ? normalizeStatus(rs.getString("trangThaiChiTietRaw"))
                                    : normalizeStatus(rs.getString("ttChiTiet"));
                            String bookingStatus = StatusUtils.bookingCode(rs.getString("ttPhieu"));
                            String roomStatus = StatusUtils.roomCode(rs.getString("ttPhong"));
                            if (StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_HUY)
                                    || StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_THANH_TOAN)
                                    || !isPendingCheckInStatus(detailStatus)
                                    || StatusUtils.isRoomStatus(roomStatus, RoomStatus.DANG_O)) {
                                con.rollback();
                                return false;
                            }
                        }

                        if (updateDetail != null) {
                            logRoomStatusUpdate("CHECK-IN", maDatPhong, maPhong, detailStatus, ChiTietPhieuDat.TRANG_THAI_DA_NHAN);
                            int detailIndex = 1;
                            if (hasDetailStatus) {
                                updateDetail.setString(detailIndex++, ChiTietPhieuDat.TRANG_THAI_DA_NHAN);
                            }
                            if (hasActualCheckIn) {
                                updateDetail.setTimestamp(detailIndex++, Timestamp.valueOf(LocalDateTime.now()));
                            }
                            updateDetail.setString(detailIndex++, maDatPhong);
                            updateDetail.setString(detailIndex, maPhong);
                            if (hasDetailStatus) {
                                detailIndex++;
                                bindUpperStatuses(updateDetail, detailIndex, PENDING_CHECK_IN_STATUSES);
                            }
                            int affectedRows = updateDetail.executeUpdate();
                            if (affectedRows != 1) {
                                throw new SQLException(INVALID_ROOM_STATUS_UPDATE_MESSAGE);
                            }
                        }

                        System.out.println("[ROOM STATUS] Current status = " + StatusUtils.roomCode(RoomStatus.DANG_O.getCode()));
                        updatePhong.setString(1, StatusUtils.roomCode(RoomStatus.DANG_O.getCode()));
                        updatePhong.setString(2, maPhong);
                        int roomAffectedRows = updatePhong.executeUpdate();
                        if (roomAffectedRows != 1) {
                            throw new SQLException(INVALID_ROOM_STATUS_UPDATE_MESSAGE);
                        }
                        System.out.println("[CHECK-IN] Update room status: " + maPhong);
                    }

                    updateBooking.setString(1, StatusUtils.bookingCode(BookingStatus.DA_NHAN_PHONG.getCode()));
                    updateBooking.setString(2, maDatPhong);
                    updateBooking.setString(3, StatusUtils.normalize(BookingStatus.DA_HUY.getCode()));
                    updateBooking.setString(4, StatusUtils.normalize(BookingStatus.DA_THANH_TOAN.getCode()));
                    updateBooking.executeUpdate();
                }

                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                e.printStackTrace();
                if (INVALID_ROOM_STATUS_UPDATE_MESSAGE.equals(e.getMessage())) {
                    throw new RuntimeException(INVALID_ROOM_STATUS_UPDATE_MESSAGE, e);
                }
                javax.swing.JOptionPane.showMessageDialog(null, "Lỗi cơ sở dữ liệu (Check-in): " + e.getMessage(), "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the mo transaction check-in tung phong. " + buildSqlMessage(e), e);
        }
    }

    public boolean deletePendingRoom(String maDatPhong, String maPhong) {
        if (maDatPhong == null || maDatPhong.isBlank() || maPhong == null || maPhong.isBlank()) {
            throw new IllegalArgumentException("Ma dat phong/ma phong khong hop le.");
        }

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);
            String detailStatusExpression = detailStatusExpression(hasTableColumn(con, "ChiTietPhieuDat", "trangThai"));
            try (PreparedStatement checkBooking = con.prepareStatement("""
                         SELECT pdp.trangThai AS trangThaiPhieuDat,
                                p.trangThai AS trangThaiPhong,
                                %s AS trangThaiChiTiet
                         FROM PhieuDatPhong pdp
                         JOIN ChiTietPhieuDat ct ON ct.maDatPhong = pdp.maDatPhong
                         JOIN Phong p ON p.maPhong = ct.maPhong
                         WHERE pdp.maDatPhong = ?
                           AND ct.maPhong = ?
                         """.formatted(detailStatusExpression));
                 PreparedStatement deleteDetail = con.prepareStatement("""
                         DELETE FROM ChiTietPhieuDat
                         WHERE maDatPhong = ? AND maPhong = ?
                         """);
                 PreparedStatement countRemaining = con.prepareStatement("""
                         SELECT COUNT(*) AS soPhong
                         FROM ChiTietPhieuDat
                         WHERE maDatPhong = ?
                         """);
                 PreparedStatement deleteBooking = con.prepareStatement("""
                         DELETE FROM PhieuDatPhong
                         WHERE maDatPhong = ?
                         """);
                 PreparedStatement updateRoom = con.prepareStatement("""
                         UPDATE p
                         SET p.trangThai = ?
                         FROM Phong p
                         WHERE p.maPhong = ?
                         """)) {

                checkBooking.setString(1, maDatPhong);
                checkBooking.setString(2, maPhong);
                String bookingStatus;
                String roomStatus;
                String detailStatus;
                try (ResultSet rs = checkBooking.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                    bookingStatus = StatusUtils.bookingCode(rs.getString("trangThaiPhieuDat"));
                    roomStatus = StatusUtils.roomCode(rs.getString("trangThaiPhong"));
                    detailStatus = normalizeStatus(rs.getString("trangThaiChiTiet"));
                }

                if (StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_THANH_TOAN)
                        || StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_HUY)) {
                    throw new RuntimeException("Khong the xoa phong trong phieu da thanh toan/da huy.");
                }
                if (!ChiTietPhieuDat.TRANG_THAI_CHO_NHAN.equalsIgnoreCase(detailStatus)
                        || StatusUtils.isRoomStatus(roomStatus, RoomStatus.DANG_O)) {
                    throw new RuntimeException("Khong the xoa phong da nhan phong hoac da tra.");
                }

                deleteDetail.setString(1, maDatPhong);
                deleteDetail.setString(2, maPhong);
                if (deleteDetail.executeUpdate() <= 0) {
                    con.rollback();
                    return false;
                }

                updateRoom.setString(1, StatusUtils.roomCode(RoomStatus.TRONG.getCode()));
                updateRoom.setString(2, maPhong);
                if (updateRoom.executeUpdate() <= 0) {
                    con.rollback();
                    return false;
                }

                countRemaining.setString(1, maDatPhong);
                int remaining;
                try (ResultSet rs = countRemaining.executeQuery()) {
                    remaining = rs.next() ? rs.getInt("soPhong") : 0;
                }

                if (remaining <= 0) {
                    deleteBooking.setString(1, maDatPhong);
                    if (deleteBooking.executeUpdate() <= 0) {
                        con.rollback();
                        return false;
                    }
                }

                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw new RuntimeException("Khong the xoa dong cho check-in. " + buildSqlMessage(e), e);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the mo transaction xoa dong check-in. " + buildSqlMessage(e), e);
        }
    }

    private boolean isOccupiedAt(ChiTietPhieuDat detail, LocalDateTime target) {
        if (detail == null || detail.getPhieuDatPhong() == null || isCanceled(detail)) {
            return false;
        }
        if (!isCheckedInOrPaid(detail)) {
            return false;
        }

        LocalDateTime checkIn = firstNonNull(detail.getThoiDiemNhanThucTe(), detail.getNgayNhan());
        LocalDateTime checkOut = effectiveOccupiedCheckout(detail);

        // Handle early check-in without actual check-in time recorded
        if (detail.getThoiDiemNhanThucTe() == null && target != null) {
            String detailStatus = normalizeStatus(detail.getTrangThai());
            if (ChiTietPhieuDat.TRANG_THAI_DA_NHAN.equalsIgnoreCase(detailStatus)) {
                if (detail.getNgayNhan() != null && target.toLocalDate().equals(detail.getNgayNhan().toLocalDate()) && target.isBefore(checkIn)) {
                    checkIn = target;
                }
            }
        }

        return contains(target, checkIn, checkOut);
    }

    private boolean isReservedAt(ChiTietPhieuDat detail, LocalDateTime target) {
        if (detail == null || detail.getPhieuDatPhong() == null || isCanceled(detail)) {
            return false;
        }
        return isPendingReservation(detail)
                && contains(target, detail.getNgayNhan(), detail.getNgayTra());
    }

    private boolean isUpcomingCheckInOnSameDay(ChiTietPhieuDat detail, LocalDateTime target) {
        if (detail == null || detail.getPhieuDatPhong() == null || isCanceled(detail)
                || target == null || detail.getNgayNhan() == null) {
            return false;
        }
        return isPendingReservation(detail)
                && target.toLocalDate().equals(detail.getNgayNhan().toLocalDate())
                && target.isBefore(detail.getNgayNhan());
    }

    private boolean isCheckedInOrPaid(ChiTietPhieuDat detail) {
        String detailStatus = normalizeStatus(detail.getTrangThai());
        String bookingStatus = detail.getPhieuDatPhong() != null ? detail.getPhieuDatPhong().getTrangThai() : null;
        return ChiTietPhieuDat.TRANG_THAI_DA_NHAN.equalsIgnoreCase(detailStatus)
                || ChiTietPhieuDat.TRANG_THAI_DA_TRA.equalsIgnoreCase(detailStatus)
                || detail.getThoiDiemNhanThucTe() != null
                || StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_THANH_TOAN);
    }

    private boolean isPendingReservation(ChiTietPhieuDat detail) {
        String detailStatus = normalizeStatus(detail.getTrangThai());
        String bookingStatus = detail.getPhieuDatPhong() != null ? detail.getPhieuDatPhong().getTrangThai() : null;
        boolean pendingBooking = StatusUtils.isBookingStatus(bookingStatus, BookingStatus.CHO_COC)
                || StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_DAT_COC)
                || StatusUtils.isBookingStatus(bookingStatus, BookingStatus.CHO_XAC_NHAN)
                || StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_XAC_NHAN)
                || StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_NHAN_PHONG);
        return pendingBooking && ChiTietPhieuDat.TRANG_THAI_CHO_NHAN.equalsIgnoreCase(detailStatus);
    }

    private boolean isCanceled(ChiTietPhieuDat detail) {
        return detail.getPhieuDatPhong() != null
                && StatusUtils.isBookingStatus(detail.getPhieuDatPhong().getTrangThai(), BookingStatus.DA_HUY);
    }

    private LocalDateTime effectiveOccupiedCheckout(ChiTietPhieuDat detail) {
        if (detail.getThoiDiemTraThucTe() != null) {
            return detail.getThoiDiemTraThucTe();
        }
        String detailStatus = normalizeStatus(detail.getTrangThai());
        if (ChiTietPhieuDat.TRANG_THAI_DA_TRA.equalsIgnoreCase(detailStatus)
                || (detail.getPhieuDatPhong() != null
                && StatusUtils.isBookingStatus(detail.getPhieuDatPhong().getTrangThai(), BookingStatus.DA_THANH_TOAN))) {
            return detail.getNgayTra();
        }
        return null;
    }

    private boolean contains(LocalDateTime target, LocalDateTime checkIn, LocalDateTime checkOut) {
        if (target == null || checkIn == null) {
            return false;
        }
        return !checkIn.isAfter(target) && (checkOut == null || checkOut.isAfter(target));
    }

    private LocalDateTime firstNonNull(LocalDateTime first, LocalDateTime fallback) {
        return first != null ? first : fallback;
    }

    private ChiTietPhieuDat mapChiTietPhieuDat(ResultSet rs, boolean includeBookingAndCustomer) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Phong phong = new Phong(
                rs.getString("maPhong"),
                rs.getString("loaiPhong"),
                getSoNguoiOrSoGiuong(rs),
                hasColumn(meta, "trangThaiPhong") ? rs.getString("trangThaiPhong")
                        : hasColumn(meta, "ttPhong") ? rs.getString("ttPhong")
                        : hasColumn(meta, "trangThai") ? rs.getString("trangThai")
                        : RoomStatus.TRONG.getCode(),
                rs.getDouble("giaPhong")
        );

        PhieuDatPhong phieuDatPhong = new PhieuDatPhong(rs.getString("maDatPhong"));
        if (includeBookingAndCustomer) {
            if (hasColumn(meta, "trangThaiPhieuDat")) {
                phieuDatPhong.setTrangThai(StatusUtils.bookingCode(rs.getString("trangThaiPhieuDat")));
            } else if (hasColumn(meta, "ttPhieu")) {
                phieuDatPhong.setTrangThai(StatusUtils.bookingCode(rs.getString("ttPhieu")));
            }
            if (hasColumn(meta, "maKhachHang")) {
                String maKhachHang = rs.getString("maKhachHang");
                if (maKhachHang != null && !maKhachHang.isBlank()) {
                    phieuDatPhong.setKhachHang(new KhachHang(
                            maKhachHang,
                            getNullableString(rs, "tenKhachHang"),
                            getNullableString(rs, "diaChi"),
                            getNullableBoolean(rs, "gioiTinh"),
                            getNullableString(rs, "soDienThoai"),
                            getNullableString(rs, "loaiGiayTo"),
                            getNullableString(rs, "soGiayTo"),
                            getNullableString(rs, "trangThaiKhachHang")
                    ));
                }
            }
        }

        LocalDateTime ngayNhan = getLocalDateTime(rs, "ngayNhan");
        LocalDateTime ngayTra = getLocalDateTime(rs, "ngayTra");
        if (includeBookingAndCustomer) {
            phieuDatPhong.setNgayNhan(ngayNhan);
            phieuDatPhong.setNgayTra(ngayTra);
        }

        return new ChiTietPhieuDat(
                phieuDatPhong,
                phong,
                rs.getDouble("giaThuePhong"),
                ngayNhan,
                ngayTra,
                normalizeStatus(getNullableString(rs, "ttChiTiet")),
                getLocalDateTime(rs, "thoiDiemNhanThucTe"),
                getLocalDateTime(rs, "thoiDiemTraThucTe")
        );
    }

    private String getNullableString(ResultSet rs, String columnName) {
        try {
            return rs.getString(columnName);
        } catch (SQLException ex) {
            return null;
        }
    }

    private boolean getNullableBoolean(ResultSet rs, String columnName) {
        try {
            return rs.getBoolean(columnName);
        } catch (SQLException ex) {
            return true;
        }
    }

    private LocalDateTime getLocalDateTime(ResultSet rs, String columnName) throws SQLException {
        if (!hasColumn(rs.getMetaData(), columnName)) {
            return null;
        }
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private int getSoNguoiOrSoGiuong(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        if (hasColumn(meta, "soNguoi")) {
            return rs.getInt("soNguoi");
        }
        if (hasColumn(meta, "soGiuong")) {
            return rs.getInt("soGiuong");
        }
        if (hasColumn(meta, "sucChua")) {
            return rs.getInt("sucChua");
        }
        return 1;
    }

    private boolean hasColumn(ResultSetMetaData meta, String columnName) throws SQLException {
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String label = meta.getColumnLabel(i);
            String name = meta.getColumnName(i);
            if (columnName.equalsIgnoreCase(label) || columnName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private List<String> normalizeStatuses(String... trangThaiList) {
        List<String> statuses = new ArrayList<>();
        if (trangThaiList == null) {
            return statuses;
        }
        for (String status : trangThaiList) {
            if (status != null && !status.isBlank()) {
                statuses.add(normalizeStatus(status));
            }
        }
        return statuses;
    }

    private String buildCheckInDetailUpdateSql(boolean hasDetailStatus, boolean hasActualCheckIn) {
        List<String> assignments = new ArrayList<>();
        if (hasDetailStatus) {
            assignments.add("trangThai = ?");
        }
        if (hasActualCheckIn) {
            assignments.add("thoiDiemNhanThucTe = COALESCE(thoiDiemNhanThucTe, ?)");
        }
        if (assignments.isEmpty()) {
            return null;
        }
        return "UPDATE ChiTietPhieuDat SET " + String.join(", ", assignments)
                + " WHERE maDatPhong = ? AND maPhong = ?"
                + (hasDetailStatus ? " AND " + statusInCondition("trangThai", PENDING_CHECK_IN_STATUSES.length) : "");
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

    private boolean isPendingCheckInCandidate(ChiTietPhieuDat detail) {
        if (detail == null || detail.getPhieuDatPhong() == null || detail.getPhong() == null) {
            return false;
        }
        String bookingStatus = detail.getPhieuDatPhong().getTrangThai();
        String roomStatus = detail.getPhong().getTrangThai();
        return !StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_HUY)
                && !StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_THANH_TOAN)
                && isPendingCheckInStatus(detail.getTrangThai())
                && !StatusUtils.isRoomStatus(roomStatus, RoomStatus.DANG_O);
    }

    private boolean isCheckoutCandidate(ChiTietPhieuDat detail) {
        if (detail == null || detail.getPhieuDatPhong() == null || detail.getPhong() == null) {
            return false;
        }
        String bookingStatus = detail.getPhieuDatPhong().getTrangThai();
        return !StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_HUY)
                && !StatusUtils.isBookingStatus(bookingStatus, BookingStatus.DA_THANH_TOAN)
                && ChiTietPhieuDat.TRANG_THAI_DA_NHAN.equalsIgnoreCase(normalizeStatus(detail.getTrangThai()));
    }

    private boolean isPendingCheckInStatus(String status) {
        return ChiTietPhieuDat.TRANG_THAI_CHO_NHAN.equalsIgnoreCase(normalizeStatus(status));
    }

    private void logRoomStatusUpdate(String action, String maDatPhong, String maPhong, String trangThaiCu, String trangThaiMoi) {
        System.out.printf("[DEBUG][%s] maPhieuDat=%s, maPhong=%s, trangThaiCu=%s, trangThaiMoi=%s%n",
                action, maDatPhong, maPhong, trangThaiCu, trangThaiMoi);
    }

    private String detailStatusExpression(boolean hasDetailStatus) {
        return detailStatusExpression(hasDetailStatus, true);
    }

    private String detailStatusExpression(boolean hasDetailStatus, boolean includeRoomStatus) {
        String roomCheckedInCondition = includeRoomStatus
                ? "\n                    OR UPPER(ISNULL(p.trangThai, '')) IN ('DA_NHAN', 'DANHAN', 'DA_NHAN_PHONG', 'DANHANPHONG', 'DANG_O', 'DANGO', N'ĐANG Ở', N'ĐÃ NHẬN PHÒNG')"
                : "";
        String bookingCheckedInFallback = includeRoomStatus
                ? ""
                : "\n                    OR UPPER(ISNULL(pdp.trangThai, '')) IN ('DA_NHAN_PHONG', 'DANHANPHONG', 'DANG_O', 'DANGO', N'ĐANG Ở', N'ĐÃ NHẬN PHÒNG')";

        if (hasDetailStatus) {
            return """
                    CASE
                        WHEN UPPER(ISNULL(pdp.trangThai, '')) IN ('DA_THANH_TOAN', 'DATHANHTOAN') THEN N'DA_TRA'
                        WHEN UPPER(ISNULL(ct.trangThai, '')) IN ('DA_TRA', 'DATRA', 'DA_TRA_PHONG', 'DATRAPHONG', 'DA_THANH_TOAN', 'DATHANHTOAN', N'ĐÃ TRẢ', N'ĐÃ TRẢ PHÒNG', N'ĐÃ THANH TOÁN') THEN N'DA_TRA'
                        WHEN (
                            UPPER(ISNULL(ct.trangThai, '')) IN ('DA_NHAN', 'DANHAN', 'DA_NHAN_PHONG', 'DANHANPHONG', 'DANG_O', 'DANGO', N'ĐANG Ở', N'ĐÃ NHẬN PHÒNG')%s
                        ) THEN N'DA_NHAN'
                        ELSE N'CHO_NHAN'
                    END
                    """.formatted(roomCheckedInCondition);
        }

        return """
                CASE
                    WHEN UPPER(ISNULL(pdp.trangThai, '')) IN ('DA_THANH_TOAN', 'DATHANHTOAN') THEN N'DA_TRA'
                    WHEN (
                        1 = 0%s%s
                    ) THEN N'DA_NHAN'
                    ELSE N'CHO_NHAN'
                END
                """.formatted(roomCheckedInCondition, bookingCheckedInFallback);
    }

    private String normalizeStatus(String status) {
        String normalized = StatusUtils.normalize(status);
        String compact = normalized.replace("_", "");
        if (compact.isBlank()
                || "CHONHAN".equals(compact)
                || "CHONHANPHONG".equals(compact)
                || "CHOCHECKIN".equals(compact)) {
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

    private Timestamp toTimestamp(LocalDateTime value) {
        return value != null ? Timestamp.valueOf(value) : null;
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

    private String buildSqlMessage(SQLException e) {
        return "SQLState=" + e.getSQLState() + ", ErrorCode=" + e.getErrorCode() + ", Message=" + e.getMessage();
    }
}
