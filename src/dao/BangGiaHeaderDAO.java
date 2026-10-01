package dao;

import connectDB.ConnectDB;
import entity.BangGiaHeader;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BangGiaHeaderDAO {

    public List<BangGiaHeader> getAllBangGiaHeader() {
        List<BangGiaHeader> ds = new ArrayList<>();
        String sql = "SELECT * FROM BangGiaHeader ORDER BY maBangGia";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ds.add(mapHeader(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tải dữ liệu bảng giá từ BangGiaHeader. " + buildSqlMessage(e), e);
        }

        return ds;
    }

    public List<BangGiaHeader> getTatCaBangGia() {
        return getAllBangGiaHeader();
    }

    public List<BangGiaHeader> getDanhSachBangGiaTheoNgay(LocalDate ngay) {
        List<BangGiaHeader> ds = new ArrayList<>();
        for (BangGiaHeader bg : getAllBangGiaHeader()) {
            if (!bg.isDangHoatDong()) {
                continue;
            }
            if (bg.getNgayBatDau() != null && bg.getNgayBatDau().isAfter(ngay)) {
                continue;
            }
            if (bg.getNgayKetThuc() != null && bg.getNgayKetThuc().isBefore(ngay)) {
                continue;
            }
            ds.add(bg);
        }
        return ds;
    }

    public BangGiaHeader getBangGiaByMa(String ma) {
        String sql = "SELECT * FROM BangGiaHeader WHERE maBangGia = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, ma);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapHeader(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tải bảng giá theo mã. " + buildSqlMessage(e), e);
        }

        return null;
    }

    public boolean existsPriceListCode(String maBangGia) {
        String sql = """
                SELECT COUNT(*) AS total
                FROM BangGiaHeader
                WHERE UPPER(LTRIM(RTRIM(maBangGia))) = UPPER(LTRIM(RTRIM(?)))
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maBangGia);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kiểm tra mã bảng giá bị trùng. " + buildSqlMessage(e), e);
        }
    }

    public boolean existsPriceListNameInSameDayTypeAndPeriod(String tenBangGia,
                                                             String loaiNgay,
                                                             LocalDate ngayBatDau,
                                                             LocalDate ngayKetThuc,
                                                             String excludedMaBangGia) {
        if (tenBangGia == null || tenBangGia.isBlank() || ngayBatDau == null) {
            return false;
        }

        LocalDate effectiveEnd = ngayKetThuc != null ? ngayKetThuc : LocalDate.of(9999, 12, 31);
        String sql = """
                SELECT COUNT(*) AS total
                FROM BangGiaHeader
                WHERE UPPER(LTRIM(RTRIM(tenBangGia))) = UPPER(LTRIM(RTRIM(?)))
                  AND UPPER(LTRIM(RTRIM(loaiNgay))) = UPPER(LTRIM(RTRIM(?)))
                  AND (? IS NULL OR UPPER(LTRIM(RTRIM(maBangGia))) <> UPPER(LTRIM(RTRIM(?))))
                  AND ? <= ISNULL(ngayKetThuc, CONVERT(date, '9999-12-31'))
                  AND ? >= ISNULL(ngayBatDau, CONVERT(date, '0001-01-01'))
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            String excludedCode = excludedMaBangGia == null || excludedMaBangGia.isBlank()
                    ? null
                    : excludedMaBangGia.trim();
            stmt.setString(1, tenBangGia.trim());
            stmt.setString(2, normalizeLoaiNgay(loaiNgay));
            if (excludedCode == null) {
                stmt.setNull(3, java.sql.Types.VARCHAR);
                stmt.setNull(4, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(3, excludedCode);
                stmt.setString(4, excludedCode);
            }
            stmt.setDate(5, Date.valueOf(ngayBatDau));
            stmt.setDate(6, Date.valueOf(effectiveEnd));

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kiểm tra tên bảng giá bị trùng. " + buildSqlMessage(e), e);
        }
    }

    public boolean hasOverlappingActivePriceList(String maBangGia,
                                                 String loaiNgay,
                                                 LocalDate ngayBatDau,
                                                 LocalDate ngayKetThuc) {
        if (ngayBatDau == null) {
            return false;
        }

        LocalDate effectiveEnd = ngayKetThuc != null ? ngayKetThuc : LocalDate.of(9999, 12, 31);
        String sql = """
                SELECT COUNT(*) AS total
                FROM BangGiaHeader
                WHERE UPPER(LTRIM(RTRIM(loaiNgay))) = UPPER(LTRIM(RTRIM(?)))
                  AND trangThai = 1
                  AND (? IS NULL OR UPPER(LTRIM(RTRIM(maBangGia))) <> UPPER(LTRIM(RTRIM(?))))
                  AND ? <= ISNULL(ngayKetThuc, CONVERT(date, '9999-12-31'))
                  AND ? >= ISNULL(ngayBatDau, CONVERT(date, '0001-01-01'))
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            String excludedCode = maBangGia == null || maBangGia.isBlank() ? null : maBangGia.trim();
            stmt.setString(1, normalizeLoaiNgay(loaiNgay));
            if (excludedCode == null) {
                stmt.setNull(2, java.sql.Types.VARCHAR);
                stmt.setNull(3, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(2, excludedCode);
                stmt.setString(3, excludedCode);
            }
            stmt.setDate(4, Date.valueOf(ngayBatDau));
            stmt.setDate(5, Date.valueOf(effectiveEnd));

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kiểm tra giao thời gian bảng giá. " + buildSqlMessage(e), e);
        }
    }

    public boolean create(BangGiaHeader bg) {
        String sql = """
                INSERT INTO BangGiaHeader(maBangGia, tenBangGia, loaiBangGia, loaiNgay, ngayBatDau, ngayKetThuc, trangThai)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            bindHeader(stmt, bg, false);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể thêm bảng giá. " + buildSqlMessage(e), e);
        }
    }

    public boolean update(BangGiaHeader bg) {
        String sql = """
                UPDATE BangGiaHeader
                SET tenBangGia = ?, loaiBangGia = ?, loaiNgay = ?, ngayBatDau = ?, ngayKetThuc = ?, trangThai = ?
                WHERE maBangGia = ?
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            bindHeader(stmt, bg, true);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể cập nhật bảng giá. " + buildSqlMessage(e), e);
        }
    }

    public boolean delete(String maBangGia) {
        String sql = "DELETE FROM BangGiaHeader WHERE maBangGia = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maBangGia);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể xóa bảng giá. " + buildSqlMessage(e), e);
        }
    }

    public BangGiaHeader getBangGiaDangHoatDongTheoLoai(String loaiNgay, LocalDate ngay) {
        String normalizedLoaiNgay = normalizeLoaiNgay(loaiNgay);
        return getDanhSachBangGiaTheoNgay(ngay).stream()
                .filter(bg -> normalizedLoaiNgay.equals(normalizeLoaiNgay(bg.getLoaiNgay())))
                .findFirst()
                .orElse(null);
    }

    public BangGiaHeader getBangGiaLeDangHoatDong(LocalDate ngay) {
        return getBangGiaDangHoatDongTheoLoai(BangGiaHeader.LOAI_NGAY_LE, ngay);
    }

    public BangGiaHeader getBangGiaHopLe(LocalDate ngay) {
        BangGiaHeader bangGiaLe = getBangGiaLeDangHoatDong(ngay);
        if (bangGiaLe != null) {
            return bangGiaLe;
        }

        if (isWeekend(ngay)) {
            BangGiaHeader bangGiaCuoiTuan = getBangGiaDangHoatDongTheoLoai(BangGiaHeader.LOAI_CUOI_TUAN, ngay);
            if (bangGiaCuoiTuan != null) {
                return bangGiaCuoiTuan;
            }
        }

        return getBangGiaDangHoatDongTheoLoai(BangGiaHeader.LOAI_NGAY_THUONG, ngay);
    }

    public BangGiaHeader getBangGiaTheoNgay(LocalDate ngay) {
        return getBangGiaHopLe(ngay);
    }

    public boolean isWeekend(LocalDate ngay) {
        if (ngay == null) {
            return false;
        }
        DayOfWeek d = ngay.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    private void bindHeader(PreparedStatement stmt, BangGiaHeader bg, boolean update) throws SQLException {
        int index = 1;
        if (!update) {
            stmt.setString(index++, bg.getMaBangGia());
        }

        stmt.setString(index++, safe(bg.getTenBangGia()));
        stmt.setString(index++, safe(bg.getLoaiBangGia()));
        stmt.setString(index++, normalizeLoaiNgay(bg.getLoaiNgay()));

        if (bg.getNgayBatDau() != null) {
            stmt.setDate(index++, Date.valueOf(bg.getNgayBatDau()));
        } else {
            stmt.setNull(index++, java.sql.Types.DATE);
        }

        if (bg.getNgayKetThuc() != null) {
            stmt.setDate(index++, Date.valueOf(bg.getNgayKetThuc()));
        } else {
            stmt.setNull(index++, java.sql.Types.DATE);
        }

        stmt.setBoolean(index++, bg.isDangHoatDong());

        if (update) {
            stmt.setString(index, bg.getMaBangGia());
        }
    }

    private BangGiaHeader mapHeader(ResultSet rs) throws SQLException {
        String maBangGia = getStringOrDefault(rs, "maBangGia", "");
        String tenBangGia = getStringOrDefault(rs, "tenBangGia", "");
        String loaiBangGia = getStringOrDefault(rs, "loaiBangGia", "");
        String loaiNgay = normalizeLoaiNgay(getStringOrDefault(rs, "loaiNgay", BangGiaHeader.LOAI_NGAY_THUONG));
        LocalDate ngayBatDau = getDateOrNull(rs, "ngayBatDau");
        LocalDate ngayKetThuc = getDateOrNull(rs, "ngayKetThuc");
        boolean trangThai = getTrangThaiAsBoolean(rs, "trangThai", true);

        return new BangGiaHeader(
                maBangGia,
                tenBangGia,
                loaiBangGia,
                loaiNgay,
                ngayBatDau != null ? ngayBatDau : LocalDate.now(),
                ngayKetThuc,
                trangThai,
                0
        );
    }

    private String getStringOrDefault(ResultSet rs, String columnName, String defaultValue) throws SQLException {
        if (!hasColumn(rs, columnName)) {
            return defaultValue;
        }
        String value = rs.getString(columnName);
        return value == null ? defaultValue : value;
    }

    private LocalDate getDateOrNull(ResultSet rs, String columnName) throws SQLException {
        if (!hasColumn(rs, columnName)) {
            return null;
        }
        Date date = rs.getDate(columnName);
        return date == null ? null : date.toLocalDate();
    }

    private boolean getTrangThaiAsBoolean(ResultSet rs, String columnName, boolean defaultValue) throws SQLException {
        if (!hasColumn(rs, columnName)) {
            return defaultValue;
        }

        Object obj = rs.getObject(columnName);
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Boolean b) {
            return b;
        }
        if (obj instanceof Number n) {
            return n.intValue() != 0;
        }

        String normalized = stripVietnamese(obj.toString());
        return !(normalized.equals("0") || normalized.equals("FALSE") || normalized.contains("KHONG"));
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(meta.getColumnLabel(i)) || columnName.equalsIgnoreCase(meta.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }

    public String normalizeLoaiBangGia(String loaiBangGia) {
        return loaiBangGia == null ? "" : loaiBangGia.trim();
    }

    public String normalizeLoaiNgay(String loaiNgay) {
        if (loaiNgay == null || loaiNgay.isBlank()) {
            return BangGiaHeader.LOAI_NGAY_THUONG;
        }

        String raw = loaiNgay.trim();
        String normalized = stripVietnamese(raw).replace(' ', '_');

        if (normalized.equals("NGAY_THUONG") || normalized.equals("THUONG")) {
            return BangGiaHeader.LOAI_NGAY_THUONG;
        }
        if (normalized.equals("CUOI_TUAN") || normalized.equals("WEEKEND")) {
            return BangGiaHeader.LOAI_CUOI_TUAN;
        }
        if (normalized.equals("NGAY_LE") || normalized.equals("LE") || normalized.equals("TET")) {
            return BangGiaHeader.LOAI_NGAY_LE;
        }

        return raw.toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    public String normalizeTrangThai(String trangThai) {
        if (trangThai == null || trangThai.isBlank()) {
            return BangGiaHeader.TRANG_THAI_DANG_HOAT_DONG;
        }
        String normalized = stripVietnamese(trangThai);
        if (normalized.contains("KHONG")) {
            return BangGiaHeader.TRANG_THAI_KHONG_HOAT_DONG;
        }
        return BangGiaHeader.TRANG_THAI_DANG_HOAT_DONG;
    }

    private String stripVietnamese(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('Đ', 'D')
                .replace('đ', 'd');
        return normalized.trim().toUpperCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildSqlMessage(SQLException e) {
        return "SQLState=" + e.getSQLState() + ", ErrorCode=" + e.getErrorCode() + ", Message=" + e.getMessage();
    }
}
