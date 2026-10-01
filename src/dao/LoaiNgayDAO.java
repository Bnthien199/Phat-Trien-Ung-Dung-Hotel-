package dao;

import connectDB.ConnectDB;
import entity.BangGiaHeader;
import entity.LoaiNgay;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LoaiNgayDAO {

    public List<LoaiNgay> getActiveLoaiNgay() {
        return loadAllLoaiNgay();
    }

    public List<LoaiNgay> loadAllLoaiNgay() {
        List<LoaiNgay> items = new ArrayList<>();
        String sql = """
                SELECT MaLoaiNgay, TenLoaiNgay, TrangThai
                FROM LoaiNgay
                WHERE TrangThai = 1
                ORDER BY
                    CASE UPPER(LTRIM(RTRIM(MaLoaiNgay)))
                        WHEN 'NGAY_THUONG' THEN 1
                        WHEN 'CUOI_TUAN' THEN 2
                        WHEN 'NGAY_LE' THEN 3
                        ELSE 10
                    END,
                    TenLoaiNgay
                """;
        try (Connection con = ConnectDB.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new LoaiNgay(
                            rs.getString("MaLoaiNgay"),
                            rs.getString("TenLoaiNgay"),
                            rs.getBoolean("TrangThai")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tải danh sách loại ngày. " + buildSqlMessage(e), e);
        }
        return items;
    }

    public boolean create(LoaiNgay item) {
        return addLoaiNgay(item);
    }

    public boolean addLoaiNgay(LoaiNgay item) {
        if (item == null) {
            return false;
        }

        String maLoaiNgay = normalizeCode(item.getMaLoaiNgay());
        String tenLoaiNgay = normalizeName(item.getTenLoaiNgay());
        if (maLoaiNgay == null || tenLoaiNgay == null) {
            return false;
        }

        String sql = """
                INSERT INTO LoaiNgay(MaLoaiNgay, TenLoaiNgay, TrangThai)
                SELECT ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM LoaiNgay
                    WHERE UPPER(LTRIM(RTRIM(MaLoaiNgay))) = UPPER(LTRIM(RTRIM(?)))
                       OR UPPER(LTRIM(RTRIM(TenLoaiNgay))) = UPPER(LTRIM(RTRIM(?)))
                )
                """;
        try (Connection con = ConnectDB.getConnection()) {
            ensureTableExists(con);
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, maLoaiNgay);
                stmt.setString(2, tenLoaiNgay);
                stmt.setBoolean(3, item.isTrangThai());
                stmt.setString(4, maLoaiNgay);
                stmt.setString(5, tenLoaiNgay);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            if (isDuplicateKey(e)) {
                return false;
            }
            throw new RuntimeException("Không thể thêm loại ngày. " + buildSqlMessage(e), e);
        }
    }

    public boolean existsByCode(String maLoaiNgay) {
        return existsByMaLoaiNgay(maLoaiNgay);
    }

    public boolean existsByMaLoaiNgay(String maLoaiNgay) {
        String normalizedCode = normalizeCode(maLoaiNgay);
        if (normalizedCode == null) {
            return false;
        }

        String sql = """
                SELECT TOP 1 1
                FROM LoaiNgay
                WHERE UPPER(LTRIM(RTRIM(MaLoaiNgay))) = UPPER(LTRIM(RTRIM(?)))
                """;
        try (Connection con = ConnectDB.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, normalizedCode);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kiểm tra mã loại ngày. " + buildSqlMessage(e), e);
        }
    }

    public boolean existsByName(String tenLoaiNgay) {
        return existsByTenLoaiNgay(tenLoaiNgay);
    }

    public boolean existsByTenLoaiNgay(String tenLoaiNgay) {
        String normalizedName = normalizeName(tenLoaiNgay);
        if (normalizedName == null) {
            return false;
        }

        String sql = """
                SELECT TOP 1 1
                FROM LoaiNgay
                WHERE UPPER(LTRIM(RTRIM(TenLoaiNgay))) = UPPER(LTRIM(RTRIM(?)))
                """;
        try (Connection con = ConnectDB.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, normalizedName);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kiểm tra tên loại ngày. " + buildSqlMessage(e), e);
        }
    }

    public String normalizeCode(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return stripVietnamese(trimmed).replaceAll("[^A-Z0-9_-]", "_").replaceAll("_+", "_");
    }

    public String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }

    public void ensureDefaultLoaiNgay() {
        try (Connection con = ConnectDB.getConnection()) {
            ensureTableReady(con);
        } catch (SQLException e) {
            throw new RuntimeException("Không thể khởi tạo loại ngày mặc định. " + buildSqlMessage(e), e);
        }
    }

    private void ensureTableReady(Connection con) throws SQLException {
        ensureTableExists(con);
        seedDefault(con, BangGiaHeader.LOAI_NGAY_THUONG, "Ngày thường");
        seedDefault(con, BangGiaHeader.LOAI_CUOI_TUAN, "Cuối tuần");
        seedDefault(con, BangGiaHeader.LOAI_NGAY_LE, "Ngày lễ");
    }

    private void ensureTableExists(Connection con) throws SQLException {
        if (!tableExists(con, "LoaiNgay")) {
            createTable(con);
        }
    }

    private void createTable(Connection con) throws SQLException {
        String sql = """
                CREATE TABLE LoaiNgay (
                    MaLoaiNgay VARCHAR(30) NOT NULL PRIMARY KEY,
                    TenLoaiNgay NVARCHAR(100) NOT NULL UNIQUE,
                    TrangThai BIT NOT NULL DEFAULT 1
                )
                """;
        try (Statement stmt = con.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void seedDefault(Connection con, String maLoaiNgay, String tenLoaiNgay) throws SQLException {
        String sql = """
                IF NOT EXISTS (
                    SELECT 1
                    FROM LoaiNgay
                    WHERE UPPER(LTRIM(RTRIM(MaLoaiNgay))) = UPPER(LTRIM(RTRIM(?)))
                       OR UPPER(LTRIM(RTRIM(TenLoaiNgay))) = UPPER(LTRIM(RTRIM(?)))
                )
                BEGIN
                INSERT INTO LoaiNgay(MaLoaiNgay, TenLoaiNgay, TrangThai)
                VALUES (?, ?, 1)
                END
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maLoaiNgay);
            stmt.setString(2, tenLoaiNgay);
            stmt.setString(3, maLoaiNgay);
            stmt.setString(4, tenLoaiNgay);
            stmt.executeUpdate();
        }
    }

    private boolean tableExists(Connection con, String tableName) throws SQLException {
        String sql = """
                SELECT 1
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = ?
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String stripVietnamese(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('Đ', 'D')
                .replace('đ', 'd');
        return normalized.trim().toUpperCase(Locale.ROOT);
    }

    public boolean isDuplicateKeyException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLException sqlException && isDuplicateKey(sqlException)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isDuplicateKey(SQLException e) {
        return "23000".equals(e.getSQLState()) || e.getErrorCode() == 2601 || e.getErrorCode() == 2627;
    }

    private String buildSqlMessage(SQLException e) {
        return "SQLState=" + e.getSQLState() + ", ErrorCode=" + e.getErrorCode() + ", Message=" + e.getMessage();
    }
}
