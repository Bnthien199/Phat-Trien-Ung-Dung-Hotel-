package dao;

import connectDB.ConnectDB;
import entity.ChiTietHoaDon;
import entity.ChiTietPhieuDat;
import entity.HoaDon;
import entity.KhachHang;
import entity.NhanVien;
import entity.PhieuDatPhong;
import util.BookingStatus;
import util.RoomStatus;
import util.StatusUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

public class HoaDonDAO {
    private static final String REVENUE_TOTAL_SQL = "(ISNULL(hd.tongTienPhong, 0) + ISNULL(hd.tongTienDichVu, 0))";
    private static final String INVALID_ROOM_STATUS_UPDATE_MESSAGE =
            "Cập nhật trạng thái phòng không hợp lệ. Vui lòng tải lại dữ liệu.";
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
    private static final String[] RETURNED_CHECKOUT_STATUSES = {
            ChiTietPhieuDat.TRANG_THAI_DA_TRA,
            "DA_TRA_PHONG",
            "DATRA",
            "DATRAPHONG",
            "Đã trả phòng"
    };
    private static final String REVENUE_BASE_FILTER = """
            FROM HoaDon hd
            WHERE hd.ngayLap >= ?
              AND hd.ngayLap < ?
            """;

    public enum RevenuePeriod {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    public static final class RevenueChartData {
        private final String label;
        private final double revenue;
        private final int invoiceCount;

        public RevenueChartData(String label, double revenue, int invoiceCount) {
            this.label = label;
            this.revenue = revenue;
            this.invoiceCount = invoiceCount;
        }

        public String getLabel() {
            return label;
        }

        public double getRevenue() {
            return revenue;
        }

        public int getInvoiceCount() {
            return invoiceCount;
        }
    }

    public static final class RevenueStatistics {
        private final double totalRevenue;
        private final int totalInvoices;
        private final double averageRevenue;
        private final List<RevenueChartData> chartData;

        public RevenueStatistics(double totalRevenue, int totalInvoices, List<RevenueChartData> chartData) {
            this.totalRevenue = totalRevenue;
            this.totalInvoices = totalInvoices;
            this.averageRevenue = totalInvoices == 0 ? 0 : totalRevenue / totalInvoices;
            this.chartData = chartData == null ? List.of() : List.copyOf(chartData);
        }

        public static RevenueStatistics empty() {
            return new RevenueStatistics(0, 0, List.of());
        }

        public double getTotalRevenue() {
            return totalRevenue;
        }

        public int getTotalInvoices() {
            return totalInvoices;
        }

        public double getAverageRevenue() {
            return averageRevenue;
        }

        public List<RevenueChartData> getChartData() {
            return chartData;
        }
    }

    private record RevenueSummary(double totalRevenue, int totalInvoices) {
    }

    public RevenueStatistics getRevenueStatistics(LocalDate fromDate, LocalDate toDate, RevenuePeriod period) {
        validateRevenueRange(fromDate, toDate);
        List<RevenueChartData> chartData = getRevenueChartData(fromDate, toDate, period);
        RevenueSummary summary = getRevenueSummary(fromDate, toDate);
        return new RevenueStatistics(summary.totalRevenue(), summary.totalInvoices(), chartData);
    }

    public List<RevenueChartData> getRevenueChartData(LocalDate fromDate, LocalDate toDate, RevenuePeriod period) {
        validateRevenueRange(fromDate, toDate);
        RevenuePeriod safePeriod = period == null ? RevenuePeriod.MONTH : period;
        String sql = buildRevenueChartSql(safePeriod);
        List<RevenueChartData> data = new ArrayList<>();

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            bindRevenueRange(stmt, fromDate, toDate);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String label = rs.getString("ky");
                    BigDecimal revenue = rs.getBigDecimal("doanhThu");
                    int invoiceCount = rs.getInt("soHoaDon");

                    if (label == null || label.isBlank() || revenue == null) {
                        continue;
                    }

                    data.add(new RevenueChartData(label.trim(), revenue.doubleValue(), invoiceCount));
                }
            }
        } catch (SQLException e) {
            logSqlException("GetRevenueChartData", sql, fromDate, toDate, e);
            throw new RuntimeException("Khong the lay du lieu bieu do doanh thu. " + buildSqlMessage(e), e);
        }
        return data;
    }

    public int countInvoices(LocalDate fromDate, LocalDate toDate) {
        validateRevenueRange(fromDate, toDate);
        return getRevenueSummary(fromDate, toDate).totalInvoices();
    }

    public double calculateAverageRevenue(LocalDate fromDate, LocalDate toDate) {
        validateRevenueRange(fromDate, toDate);
        RevenueSummary summary = getRevenueSummary(fromDate, toDate);
        return summary.totalInvoices() == 0 ? 0 : summary.totalRevenue() / summary.totalInvoices();
    }

    private RevenueSummary getRevenueSummary(LocalDate fromDate, LocalDate toDate) {
        String sql = "SELECT COALESCE(SUM(" + REVENUE_TOTAL_SQL + "), 0) AS tongDoanhThu, "
                + "COUNT(*) AS soHoaDon " + REVENUE_BASE_FILTER;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            bindRevenueRange(stmt, fromDate, toDate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new RevenueSummary(rs.getDouble("tongDoanhThu"), rs.getInt("soHoaDon"));
                }
            }
        } catch (SQLException e) {
            logSqlException("GetRevenueStatistics", sql, fromDate, toDate, e);
            throw new RuntimeException("Khong the tinh tong hop doanh thu. " + buildSqlMessage(e), e);
        }
        return new RevenueSummary(0, 0);
    }

    private String buildRevenueChartSql(RevenuePeriod period) {
        return switch (period) {
            case DAY -> """
                    SELECT
                        CONVERT(VARCHAR(10), CAST(hd.ngayLap AS DATE), 103) AS ky,
                        SUM(%s) AS doanhThu,
                        COUNT(*) AS soHoaDon,
                        DATEDIFF(DAY, '19000101', CAST(hd.ngayLap AS DATE)) AS sortKey
                    %s
                    GROUP BY CAST(hd.ngayLap AS DATE)
                    ORDER BY sortKey
                    """.formatted(REVENUE_TOTAL_SQL, REVENUE_BASE_FILTER);
            case WEEK -> """
                    SELECT
                        CONCAT(N'Tuần ', DATEPART(WEEK, hd.ngayLap), N'/', DATEPART(YEAR, hd.ngayLap)) AS ky,
                        SUM(%s) AS doanhThu,
                        COUNT(*) AS soHoaDon,
                        DATEPART(YEAR, hd.ngayLap) * 100 + DATEPART(WEEK, hd.ngayLap) AS sortKey
                    %s
                    GROUP BY DATEPART(YEAR, hd.ngayLap), DATEPART(WEEK, hd.ngayLap)
                    ORDER BY sortKey
                    """.formatted(REVENUE_TOTAL_SQL, REVENUE_BASE_FILTER);
            case YEAR -> """
                    SELECT
                        CAST(DATEPART(YEAR, hd.ngayLap) AS VARCHAR(4)) AS ky,
                        SUM(%s) AS doanhThu,
                        COUNT(*) AS soHoaDon,
                        DATEPART(YEAR, hd.ngayLap) AS sortKey
                    %s
                    GROUP BY DATEPART(YEAR, hd.ngayLap)
                    ORDER BY sortKey
                    """.formatted(REVENUE_TOTAL_SQL, REVENUE_BASE_FILTER);
            default -> """
                    SELECT
                        RIGHT('0' + CAST(DATEPART(MONTH, hd.ngayLap) AS VARCHAR(2)), 2)
                            + '/' + CAST(DATEPART(YEAR, hd.ngayLap) AS VARCHAR(4)) AS ky,
                        SUM(%s) AS doanhThu,
                        COUNT(*) AS soHoaDon,
                        DATEPART(YEAR, hd.ngayLap) * 100 + DATEPART(MONTH, hd.ngayLap) AS sortKey
                    %s
                    GROUP BY DATEPART(YEAR, hd.ngayLap), DATEPART(MONTH, hd.ngayLap)
                    ORDER BY sortKey
                    """.formatted(REVENUE_TOTAL_SQL, REVENUE_BASE_FILTER);
        };
    }

    private void validateRevenueRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Khoang thoi gian thong ke khong duoc rong.");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Tu ngay khong duoc lon hon den ngay.");
        }
    }

    private void bindRevenueRange(PreparedStatement stmt, LocalDate fromDate, LocalDate toDate) throws SQLException {
        stmt.setTimestamp(1, Timestamp.valueOf(fromDate.atStartOfDay()));
        stmt.setTimestamp(2, Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
    }

    private void logSqlException(String operation, String sql, LocalDate fromDate, LocalDate toDate, SQLException e) {
        System.err.println("[HoaDonDAO][" + operation + "] SQL failed");
        System.err.println("Range: " + fromDate + " -> " + toDate + " (exclusive end: " + toDate.plusDays(1) + ")");
        System.err.println(sql);
        e.printStackTrace();
    }

    public List<HoaDon> getAllHoaDon() {
        List<HoaDon> ds = new ArrayList<>();
        String sql = """
                SELECT hd.*, pdp.tienDatCoc, kh.tenKhachHang, kh.soDienThoai
                FROM HoaDon hd
                JOIN PhieuDatPhong pdp ON hd.maDatPhong = pdp.maDatPhong
                JOIN KhachHang kh ON pdp.maKhachHang = kh.maKhachHang
                """;

        try (Connection con = ConnectDB.getConnection();
             Statement statement = con.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                ds.add(mapHoaDonWithCustomer(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "Lỗi cơ sở dữ liệu (All Hoa Don): " + e.getMessage(), "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
        return ds;
    }

    public List<HoaDon> filterHoaDon(String keyword, java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        List<HoaDon> ds = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT hd.*, pdp.tienDatCoc, kh.tenKhachHang, kh.soDienThoai
                FROM HoaDon hd
                JOIN PhieuDatPhong pdp ON hd.maDatPhong = pdp.maDatPhong
                JOIN KhachHang kh ON pdp.maKhachHang = kh.maKhachHang
                WHERE 1=1
                """);

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (hd.maHoaDon LIKE ? OR kh.tenKhachHang LIKE ?)");
        }
        if (fromDate != null) {
            sql.append(" AND CAST(hd.ngayLap AS DATE) >= ?");
        }
        if (toDate != null) {
            sql.append(" AND CAST(hd.ngayLap AS DATE) <= ?");
        }

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {

            int paramIdx = 1;
            if (keyword != null && !keyword.trim().isEmpty()) {
                stmt.setString(paramIdx++, "%" + keyword + "%");
                stmt.setString(paramIdx++, "%" + keyword + "%");
            }
            if (fromDate != null) {
                stmt.setDate(paramIdx++, java.sql.Date.valueOf(fromDate));
            }
            if (toDate != null) {
                stmt.setDate(paramIdx++, java.sql.Date.valueOf(toDate));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ds.add(mapHoaDonWithCustomer(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the loc hoa don. " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public HoaDon getHoaDonByMaPhieu(String maPhieu) {
        String sql = """
                SELECT hd.*, pdp.tienDatCoc, kh.maKhachHang, kh.tenKhachHang, kh.soDienThoai, nv.tenNhanVien
                FROM HoaDon hd
                JOIN PhieuDatPhong pdp ON pdp.maDatPhong = hd.maDatPhong
                LEFT JOIN KhachHang kh ON kh.maKhachHang = pdp.maKhachHang
                LEFT JOIN NhanVien nv ON nv.maNhanVien = hd.maNhanVien
                WHERE hd.maDatPhong = ?
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maPhieu);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    PhieuDatPhong phieuDatPhong = new PhieuDatPhong(rs.getString("maDatPhong"));
                    phieuDatPhong.setTienDatCoc(getNullableDouble(rs, "tienDatCoc"));

                    HoaDon hoaDon = new HoaDon(
                            rs.getString("maHoaDon"),
                            rs.getTimestamp("ngayLap").toLocalDateTime(),
                            rs.getDouble("thue"),
                            rs.getDouble("tongTienPhong"),
                            rs.getDouble("tongTienDichVu"),
                            phieuDatPhong,
                            new NhanVien(rs.getString("maNhanVien"))
                    );
                    if (rs.getString("maKhachHang") != null) {
                        hoaDon.getPhieuDatPhong().setKhachHang(new KhachHang(
                                rs.getString("maKhachHang"),
                                rs.getString("tenKhachHang"),
                                "",
                                true,
                                rs.getString("soDienThoai")
                        ));
                    }
                    if (rs.getString("tenNhanVien") != null) {
                        hoaDon.getNhanVien().setTenNhanVien(rs.getString("tenNhanVien"));
                    }
                    return hoaDon;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tai hoa don theo ma phieu. " + buildSqlMessage(e), e);
        }
        return null;
    }

    public String generateNextId() {
        String sql = "SELECT MAX(CAST(SUBSTRING(maHoaDon, 3, LEN(maHoaDon)) AS INT)) FROM HoaDon";
        try (Connection con = ConnectDB.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return String.format("HD%05d", rs.getInt(1) + 1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tao ma hoa don tiep theo. " + buildSqlMessage(e), e);
        }
        return "HD00001";
    }

    public boolean updateTongTien(String maHD, double themTienPhong, double themTienDV) {
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "UPDATE HoaDon SET tongTienPhong = tongTienPhong + ?, tongTienDichVu = tongTienDichVu + ? WHERE maHoaDon = ?")) {
            stmt.setDouble(1, themTienPhong);
            stmt.setDouble(2, themTienDV);
            stmt.setString(3, maHD);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong the cap nhat tong tien hoa don. " + buildSqlMessage(e), e);
        }
    }

    public boolean updateTotalsAbsolute(String maHD, double tongTienPhong, double tongTienDV) {
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "UPDATE HoaDon SET tongTienPhong = ?, tongTienDichVu = ? WHERE maHoaDon = ?")) {
            stmt.setDouble(1, tongTienPhong);
            stmt.setDouble(2, tongTienDV);
            stmt.setString(3, maHD);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong the cap nhat tong tien hoa don tuyet doi. " + buildSqlMessage(e), e);
        }
    }

    public boolean saveCheckout(HoaDon hd, List<ChiTietHoaDon> dsChiTiet, List<String> maPhongTra, boolean closeBooking) {
        String sqlInsertHeader = "INSERT INTO HoaDon VALUES(?, ?, ?, ?, ?, ?, ?)";
        String sqlUpdateHeader = """
                UPDATE HoaDon
                SET ngayLap = ?, thue = ?, tongTienPhong = ?, tongTienDichVu = ?, maDatPhong = ?, maNhanVien = ?
                WHERE maHoaDon = ?
                """;
        String sqlUpsertDetail = """
                MERGE ChiTietHoaDon AS target
                USING (SELECT ? AS maHoaDon, ? AS maDichVu, ? AS maPhong, ? AS soLuong, ? AS donGiaLuuTru) AS source
                ON target.maHoaDon = source.maHoaDon
                   AND target.maDichVu = source.maDichVu
                   AND target.maPhong = source.maPhong
                WHEN MATCHED THEN
                    UPDATE SET soLuong = source.soLuong, donGiaLuuTru = source.donGiaLuuTru
                WHEN NOT MATCHED THEN
                    INSERT (maHoaDon, maDichVu, maPhong, soLuong, donGiaLuuTru)
                    VALUES (source.maHoaDon, source.maDichVu, source.maPhong, source.soLuong, source.donGiaLuuTru);
                """;
        String sqlUpdateRoom = "UPDATE p SET p.trangThai = ? FROM Phong p WHERE p.maPhong = ?";
        String sqlUpdateBooking = "UPDATE pdp SET pdp.trangThai = ? FROM PhieuDatPhong pdp WHERE pdp.maDatPhong = ?";
        List<String> normalizedRooms = normalizeRoomIds(maPhongTra);

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);
            try {
                boolean exists = existsHoaDon(con, hd.getMaHoaDon());
                boolean hasDetailStatus = hasTableColumn(con, "ChiTietPhieuDat", "trangThai");
                boolean hasActualCheckOut = hasTableColumn(con, "ChiTietPhieuDat", "thoiDiemTraThucTe");
                String detailStatusExpression = detailStatusExpression(hasDetailStatus);
                String rawDetailStatusSelect = hasDetailStatus ? "ct.trangThai" : "NULL";
                String activeDetailCondition = hasDetailStatus
                        ? statusInCondition("ct.trangThai", ACTIVE_CHECKOUT_STATUSES.length)
                        : "UPPER(ISNULL(" + detailStatusExpression + ", '')) = ?";
                String returnedDetailCondition = hasDetailStatus
                        ? statusInCondition("ct.trangThai", RETURNED_CHECKOUT_STATUSES.length)
                        : "UPPER(ISNULL(" + detailStatusExpression + ", '')) = ?";
                String sqlCheckDetail = """
                        SELECT TOP 1 %s AS ttChiTiet,
                               %s AS trangThaiChiTietRaw
                        FROM ChiTietPhieuDat ct
                        JOIN Phong p ON p.maPhong = ct.maPhong
                        JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                        WHERE ct.maDatPhong = ?
                          AND ct.maPhong = ?
                        """.formatted(detailStatusExpression, rawDetailStatusSelect);
                String sqlUpdateDetail = buildCheckoutDetailUpdateSql(hasDetailStatus, hasActualCheckOut);
                String sqlCountActiveDetails = """
                        SELECT COUNT(*) AS soPhongDangNhan
                        FROM ChiTietPhieuDat ct
                        JOIN Phong p ON p.maPhong = ct.maPhong
                        JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                        WHERE ct.maDatPhong = ?
                          AND %s
                        """.formatted(activeDetailCondition);
                String sqlCountUnreturnedDetails = """
                        SELECT COUNT(*) AS soPhongChuaTra
                        FROM ChiTietPhieuDat ct
                        JOIN Phong p ON p.maPhong = ct.maPhong
                        JOIN PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
                        WHERE ct.maDatPhong = ?
                          AND NOT (%s)
                        """.formatted(returnedDetailCondition);

                if (exists) {
                    try (PreparedStatement stmt = con.prepareStatement(sqlUpdateHeader)) {
                        stmt.setTimestamp(1, Timestamp.valueOf(hd.getNgayLap()));
                        stmt.setDouble(2, hd.getThue());
                        stmt.setDouble(3, hd.getTongTienPhong());
                        stmt.setDouble(4, hd.getTongTienDichVu());
                        stmt.setString(5, hd.getPhieuDatPhong().getMaDatPhong());
                        stmt.setString(6, hd.getNhanVien().getMaNhanVien());
                        stmt.setString(7, hd.getMaHoaDon());
                        stmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement stmt = con.prepareStatement(sqlInsertHeader)) {
                        stmt.setString(1, hd.getMaHoaDon());
                        stmt.setTimestamp(2, Timestamp.valueOf(hd.getNgayLap()));
                        stmt.setDouble(3, hd.getThue());
                        stmt.setDouble(4, hd.getTongTienPhong());
                        stmt.setDouble(5, hd.getTongTienDichVu());
                        stmt.setString(6, hd.getPhieuDatPhong().getMaDatPhong());
                        stmt.setString(7, hd.getNhanVien().getMaNhanVien());
                        stmt.executeUpdate();
                    }
                }

                if (dsChiTiet != null && !dsChiTiet.isEmpty()) {
                    try (PreparedStatement stmt = con.prepareStatement(sqlUpsertDetail)) {
                        for (ChiTietHoaDon ct : dsChiTiet) {
                            stmt.setString(1, hd.getMaHoaDon());
                            stmt.setString(2, ct.getDichVu().getMaDichVu());
                            stmt.setString(3, ct.getPhong() != null ? ct.getPhong().getMaPhong() : null);
                            stmt.setInt(4, ct.getSoLuong());
                            stmt.setDouble(5, ct.getDonGiaLuuTru());
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                if (!normalizedRooms.isEmpty()) {
                    Timestamp checkoutAt = Timestamp.valueOf(hd.getNgayLap() != null ? hd.getNgayLap() : LocalDateTime.now());
                    try (PreparedStatement checkDetail = con.prepareStatement(sqlCheckDetail);
                         PreparedStatement updateDetail = sqlUpdateDetail != null ? con.prepareStatement(sqlUpdateDetail) : null;
                         PreparedStatement updateRoom = con.prepareStatement(sqlUpdateRoom)) {
                        for (String maPhong : normalizedRooms) {
                            String detailStatus;
                            checkDetail.setString(1, hd.getPhieuDatPhong().getMaDatPhong());
                            checkDetail.setString(2, maPhong);
                            try (ResultSet rs = checkDetail.executeQuery()) {
                                if (!rs.next()) {
                                    throw new SQLException("Phong " + maPhong + " khong o trang thai dang o de tra phong.");
                                }
                                detailStatus = hasDetailStatus
                                        ? normalizeDetailStatus(rs.getString("trangThaiChiTietRaw"))
                                        : normalizeDetailStatus(rs.getString("ttChiTiet"));
                                if (!ChiTietPhieuDat.TRANG_THAI_DA_NHAN.equalsIgnoreCase(detailStatus)) {
                                    throw new SQLException("Phong " + maPhong + " khong o trang thai dang o de tra phong.");
                                }
                            }

                            if (updateDetail != null) {
                                logRoomStatusUpdate("CHECKOUT", hd.getPhieuDatPhong().getMaDatPhong(), maPhong,
                                        detailStatus, ChiTietPhieuDat.TRANG_THAI_DA_TRA);
                                int index = 1;
                                if (hasDetailStatus) {
                                    updateDetail.setString(index++, ChiTietPhieuDat.TRANG_THAI_DA_TRA);
                                }
                                if (hasActualCheckOut) {
                                    updateDetail.setTimestamp(index++, checkoutAt);
                                }
                                updateDetail.setString(index++, hd.getPhieuDatPhong().getMaDatPhong());
                                updateDetail.setString(index, maPhong);
                                if (hasDetailStatus) {
                                    index++;
                                    bindUpperStatuses(updateDetail, index, ACTIVE_CHECKOUT_STATUSES);
                                }
                                int affectedRows = updateDetail.executeUpdate();
                                if (affectedRows != 1) {
                                    throw new SQLException(INVALID_ROOM_STATUS_UPDATE_MESSAGE);
                                }
                            }

                            updateRoom.setString(1, StatusUtils.roomCode(RoomStatus.TRONG.getCode()));
                            updateRoom.setString(2, maPhong);
                            int roomAffectedRows = updateRoom.executeUpdate();
                            if (roomAffectedRows != 1) {
                                throw new SQLException(INVALID_ROOM_STATUS_UPDATE_MESSAGE);
                            }
                        }
                    }
                }

                boolean hasRemainingCheckedInRooms;
                try (PreparedStatement stmt = con.prepareStatement(sqlCountActiveDetails)) {
                    int index = 1;
                    stmt.setString(index++, hd.getPhieuDatPhong().getMaDatPhong());
                    if (hasDetailStatus) {
                        bindUpperStatuses(stmt, index, ACTIVE_CHECKOUT_STATUSES);
                    } else {
                        stmt.setString(index, ChiTietPhieuDat.TRANG_THAI_DA_NHAN);
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        hasRemainingCheckedInRooms = rs.next() && rs.getInt("soPhongDangNhan") > 0;
                    }
                }

                boolean hasUnreturnedRooms;
                try (PreparedStatement stmt = con.prepareStatement(sqlCountUnreturnedDetails)) {
                    int index = 1;
                    stmt.setString(index++, hd.getPhieuDatPhong().getMaDatPhong());
                    if (hasDetailStatus) {
                        bindUpperStatuses(stmt, index, RETURNED_CHECKOUT_STATUSES);
                    } else {
                        stmt.setString(index, ChiTietPhieuDat.TRANG_THAI_DA_TRA);
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        hasUnreturnedRooms = rs.next() && rs.getInt("soPhongChuaTra") > 0;
                    }
                }

                if (closeBooking && !hasRemainingCheckedInRooms && !hasUnreturnedRooms) {
                    try (PreparedStatement stmt = con.prepareStatement(sqlUpdateBooking)) {
                        stmt.setString(1, StatusUtils.bookingCode(BookingStatus.DA_THANH_TOAN.getCode()));
                        stmt.setString(2, hd.getPhieuDatPhong().getMaDatPhong());
                        int updated = stmt.executeUpdate();
                        if (updated <= 0) {
                            throw new SQLException("Khong cap nhat duoc trang thai phieu dat phong "
                                    + hd.getPhieuDatPhong().getMaDatPhong() + " sang DA_THANH_TOAN.");
                        }
                    }
                } else {
                    try (PreparedStatement stmt = con.prepareStatement(sqlUpdateBooking)) {
                        stmt.setString(1, StatusUtils.bookingCode(BookingStatus.DA_NHAN_PHONG.getCode()));
                        stmt.setString(2, hd.getPhieuDatPhong().getMaDatPhong());
                        stmt.executeUpdate();
                    }
                }

                con.commit();
                return true;
            } catch (SQLException e) {
                rollbackQuietly(con, e);
                System.out.println("[SQL ERROR] " + e.getMessage());
                e.printStackTrace();
                if (INVALID_ROOM_STATUS_UPDATE_MESSAGE.equals(e.getMessage())) {
                    throw new RuntimeException(INVALID_ROOM_STATUS_UPDATE_MESSAGE, e);
                }
                javax.swing.JOptionPane.showMessageDialog(null, "Lỗi cơ sở dữ liệu (Checkout): " + e.getMessage(), "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the mo transaction checkout. " + buildSqlMessage(e), e);
        }
    }

    public boolean thanhToan(HoaDon hd, List<ChiTietHoaDon> dsChiTiet) {
        return saveCheckout(hd, dsChiTiet, List.of(), false);
    }

    public boolean create(HoaDon hd) {
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement("INSERT INTO HoaDon VALUES(?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, hd.getMaHoaDon());
            stmt.setTimestamp(2, Timestamp.valueOf(hd.getNgayLap()));
            stmt.setDouble(3, hd.getThue());
            stmt.setDouble(4, hd.getTongTienPhong());
            stmt.setDouble(5, hd.getTongTienDichVu());
            stmt.setString(6, hd.getPhieuDatPhong().getMaDatPhong());
            stmt.setString(7, hd.getNhanVien().getMaNhanVien());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tao hoa don. " + buildSqlMessage(e), e);
        }
    }

    private boolean existsHoaDon(Connection con, String maHoaDon) throws SQLException {
        String sql = "SELECT 1 FROM HoaDon WHERE maHoaDon = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maHoaDon);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void rollbackQuietly(Connection con, SQLException cause) {
        if (con == null) {
            return;
        }
        try {
            con.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
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

    private String buildCheckoutDetailUpdateSql(boolean hasDetailStatus, boolean hasActualCheckOut) {
        List<String> assignments = new ArrayList<>();
        if (hasDetailStatus) {
            assignments.add("trangThai = ?");
        }
        if (hasActualCheckOut) {
            assignments.add("thoiDiemTraThucTe = COALESCE(thoiDiemTraThucTe, ?)");
        }
        if (assignments.isEmpty()) {
            return null;
        }
        return "UPDATE ChiTietPhieuDat SET " + String.join(", ", assignments)
                + " WHERE maDatPhong = ? AND maPhong = ?"
                + (hasDetailStatus ? " AND " + statusInCondition("trangThai", ACTIVE_CHECKOUT_STATUSES.length) : "");
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

    private String normalizeDetailStatus(String status) {
        String normalized = StatusUtils.normalize(status);
        String compact = normalized.replace("_", "");
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

    private void logRoomStatusUpdate(String action, String maDatPhong, String maPhong, String trangThaiCu, String trangThaiMoi) {
        System.out.printf("[DEBUG][%s] maPhieuDat=%s, maPhong=%s, trangThaiCu=%s, trangThaiMoi=%s%n",
                action, maDatPhong, maPhong, trangThaiCu, trangThaiMoi);
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
        return "SQLState=" + e.getSQLState()
                + ", ErrorCode=" + e.getErrorCode()
                + ", Message=" + e.getMessage();
    }

    private List<String> normalizeRoomIds(List<String> maPhongTra) {
        if (maPhongTra == null || maPhongTra.isEmpty()) {
            return List.of();
        }

        Set<String> uniqueRooms = new LinkedHashSet<>();
        for (String maPhong : maPhongTra) {
            if (maPhong != null && !maPhong.isBlank()) {
                uniqueRooms.add(maPhong.trim());
            }
        }
        return new ArrayList<>(uniqueRooms);
    }

    private int countAffectedRows(int[] batchResult) {
        int affected = 0;
        for (int result : batchResult) {
            if (result == Statement.SUCCESS_NO_INFO || result > 0) {
                affected++;
            }
        }
        return affected;
    }

    private HoaDon mapHoaDonWithCustomer(ResultSet rs) throws SQLException {
        KhachHang kh = new KhachHang();
        kh.setTenKhachHang(rs.getString("tenKhachHang"));
        kh.setSoDienThoai(rs.getString("soDienThoai"));

        PhieuDatPhong pdp = new PhieuDatPhong(rs.getString("maDatPhong"));
        pdp.setTienDatCoc(getNullableDouble(rs, "tienDatCoc"));
        pdp.setKhachHang(kh);

        return new HoaDon(
                rs.getString("maHoaDon"),
                rs.getTimestamp("ngayLap").toLocalDateTime(),
                rs.getDouble("thue"),
                rs.getDouble("tongTienPhong"),
                rs.getDouble("tongTienDichVu"),
                pdp,
                new NhanVien(rs.getString("maNhanVien"))
        );
    }

    private double getNullableDouble(ResultSet rs, String columnName) throws SQLException {
        try {
            double value = rs.getDouble(columnName);
            return rs.wasNull() ? 0 : value;
        } catch (SQLException e) {
            return 0;
        }
    }
}
