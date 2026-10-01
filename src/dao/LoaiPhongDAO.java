package dao;

import connectDB.ConnectDB;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LoaiPhongDAO {
    private static final String TABLE_NAME = "LoaiPhong";
    private static final String MISSING_TABLE_MESSAGE =
            "Bang LoaiPhong chua ton tai. Hay chay script sql/20260522_add_loai_phong_table.sql "
                    + "hoac tao lai database bang sql/create_database.sql truoc khi mo Quan ly phong.";

    public List<PhongDAO.LoaiPhongItem> getAllLoaiPhong() {
        List<PhongDAO.LoaiPhongItem> items = new ArrayList<>();
        String sql = "SELECT maLoai, loaiPhong FROM LoaiPhong ORDER BY loaiPhong, maLoai";
        try (Connection con = ConnectDB.getConnection()) {
            ensureTableReady(con);
            try (PreparedStatement stmt = con.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new PhongDAO.LoaiPhongItem(
                            rs.getString("loaiPhong"),
                            rs.getString("maLoai")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tai danh sach loai phong. " + buildSqlMessage(e), e);
        }
        return items;
    }

    public boolean create(PhongDAO.LoaiPhongItem item) {
        String sql = "INSERT INTO LoaiPhong(maLoai, loaiPhong) VALUES (?, ?)";
        try (Connection con = ConnectDB.getConnection()) {
            ensureTableReady(con);
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, item.getMaLoai());
                stmt.setString(2, item.getLoaiPhong());
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the them loai phong. " + buildSqlMessage(e), e);
        }
    }

    public boolean update(String originalMaLoai, PhongDAO.LoaiPhongItem updatedItem) {
        try (Connection con = ConnectDB.getConnection()) {
            ensureTableReady(con);
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                String normalizedOriginalCode = normalizeCode(originalMaLoai);
                String normalizedNewCode = normalizeCode(updatedItem.getMaLoai());
                String normalizedNewName = normalizeName(updatedItem.getLoaiPhong());

                if (normalizedOriginalCode == null || normalizedNewCode == null || normalizedNewName == null) {
                    throw new SQLException("Du lieu loai phong khong hop le de cap nhat.");
                }

                PhongDAO.LoaiPhongItem existingItem = getByCode(con, normalizedOriginalCode);
                if (existingItem == null) {
                    throw new SQLException("Khong tim thay loai phong can cap nhat: " + normalizedOriginalCode);
                }

                int affectedRows;
                if (normalizedOriginalCode.equalsIgnoreCase(normalizedNewCode)) {
                    affectedRows = updateRoomTypeNameOnly(con, normalizedOriginalCode, normalizedNewName);
                } else {
                    affectedRows = migrateRoomTypeCode(
                            con,
                            normalizedOriginalCode,
                            normalizedNewCode,
                            normalizedNewName
                    );
                }

                con.commit();
                con.setAutoCommit(oldAutoCommit);
                return affectedRows > 0;
            } catch (SQLException ex) {
                con.rollback();
                con.setAutoCommit(oldAutoCommit);
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the cap nhat loai phong. " + buildSqlMessage(e), e);
        }
    }

    public boolean existsByLoaiPhong(String loaiPhong, String excludeMaLoai) {
        String sql = """
                SELECT TOP 1 1
                FROM LoaiPhong
                WHERE UPPER(LTRIM(RTRIM(loaiPhong))) = UPPER(LTRIM(RTRIM(?)))
                  AND (? IS NULL OR UPPER(LTRIM(RTRIM(maLoai))) <> UPPER(LTRIM(RTRIM(?))))
                """;
        try (Connection con = ConnectDB.getConnection()) {
            ensureTableReady(con);
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, loaiPhong);
                stmt.setString(2, excludeMaLoai);
                stmt.setString(3, excludeMaLoai);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the kiem tra ten loai phong. " + buildSqlMessage(e), e);
        }
    }

    public boolean existsByMaLoai(String maLoai, String excludeMaLoai) {
        String sql = """
                SELECT TOP 1 1
                FROM LoaiPhong
                WHERE UPPER(LTRIM(RTRIM(maLoai))) = UPPER(LTRIM(RTRIM(?)))
                  AND (? IS NULL OR UPPER(LTRIM(RTRIM(maLoai))) <> UPPER(LTRIM(RTRIM(?))))
                """;
        try (Connection con = ConnectDB.getConnection()) {
            ensureTableReady(con);
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, maLoai);
                stmt.setString(2, excludeMaLoai);
                stmt.setString(3, excludeMaLoai);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the kiem tra ma loai phong. " + buildSqlMessage(e), e);
        }
    }

    private void ensureTableReady(Connection con) throws SQLException {
        if (!tableExists(con, TABLE_NAME)) {
            throw new SQLException(MISSING_TABLE_MESSAGE);
        }
    }

    private PhongDAO.LoaiPhongItem getByCode(Connection con, String maLoai) throws SQLException {
        String sql = """
                SELECT TOP 1 maLoai, loaiPhong
                FROM LoaiPhong
                WHERE UPPER(LTRIM(RTRIM(maLoai))) = UPPER(LTRIM(RTRIM(?)))
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maLoai);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PhongDAO.LoaiPhongItem(
                            rs.getString("loaiPhong"),
                            rs.getString("maLoai")
                    );
                }
                return null;
            }
        }
    }

    private int updateRoomTypeNameOnly(Connection con, String maLoai, String loaiPhong) throws SQLException {
        String updateLoaiPhongSql = """
                UPDATE LoaiPhong
                SET loaiPhong = ?, ngayCapNhat = SYSUTCDATETIME()
                WHERE UPPER(LTRIM(RTRIM(maLoai))) = UPPER(LTRIM(RTRIM(?)))
                """;
        int updatedRows;
        try (PreparedStatement stmt = con.prepareStatement(updateLoaiPhongSql)) {
            stmt.setString(1, loaiPhong);
            stmt.setString(2, maLoai);
            updatedRows = stmt.executeUpdate();
        }
        syncRoomTypeNameInPhong(con, maLoai, loaiPhong);
        return updatedRows;
    }

    private int migrateRoomTypeCode(Connection con,
                                    String oldCode,
                                    String newCode,
                                    String newName) throws SQLException {
        String placeholderName = buildMigrationPlaceholder(oldCode);

        try (PreparedStatement stmt = con.prepareStatement(
                "UPDATE LoaiPhong SET loaiPhong = ?, ngayCapNhat = SYSUTCDATETIME() WHERE UPPER(LTRIM(RTRIM(maLoai))) = UPPER(LTRIM(RTRIM(?)))")) {
            stmt.setString(1, placeholderName);
            stmt.setString(2, oldCode);
            stmt.executeUpdate();
        }

        try (PreparedStatement stmt = con.prepareStatement(
                "INSERT INTO LoaiPhong(maLoai, loaiPhong, ngayCapNhat) VALUES (?, ?, SYSUTCDATETIME())")) {
            stmt.setString(1, newCode);
            stmt.setString(2, newName);
            stmt.executeUpdate();
        }

        syncRoomTypeCodeReferences(con, oldCode, newCode);
        syncRoomTypeNameInPhong(con, newCode, newName);

        try (PreparedStatement stmt = con.prepareStatement(
                "DELETE FROM LoaiPhong WHERE UPPER(LTRIM(RTRIM(maLoai))) = UPPER(LTRIM(RTRIM(?)))")) {
            stmt.setString(1, oldCode);
            int deletedRows = stmt.executeUpdate();
            return deletedRows > 0 ? 1 : 0;
        }
    }

    private void syncRoomTypeCodeReferences(Connection con, String oldCode, String newCode) throws SQLException {
        for (String tableName : findTablesHavingMaLoai(con)) {
            String sql = "UPDATE " + quoteIdentifier(tableName)
                    + " SET " + quoteIdentifier("maLoai") + " = ?"
                    + " WHERE UPPER(LTRIM(RTRIM(" + quoteIdentifier("maLoai") + "))) = UPPER(LTRIM(RTRIM(?)))";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, newCode);
                stmt.setString(2, oldCode);
                stmt.executeUpdate();
            }
        }
    }

    private void syncRoomTypeNameInPhong(Connection con, String maLoai, String loaiPhong) throws SQLException {
        if (!tableExists(con, "Phong")) {
            return;
        }
        String sql = """
                UPDATE Phong
                SET loaiPhong = ?
                WHERE UPPER(LTRIM(RTRIM(maLoai))) = UPPER(LTRIM(RTRIM(?)))
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, loaiPhong);
            stmt.setString(2, maLoai);
            stmt.executeUpdate();
        }
    }

    private List<String> findTablesHavingMaLoai(Connection con) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metaData = con.getMetaData();
        try (ResultSet rs = metaData.getColumns(con.getCatalog(), null, "%", "maLoai")) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String tableType = getTableType(con, tableName);
                if (tableName == null
                        || "LoaiPhong".equalsIgnoreCase(tableName)
                        || (tableType != null && !"BASE TABLE".equalsIgnoreCase(tableType))) {
                    continue;
                }
                if (!tables.contains(tableName)) {
                    tables.add(tableName);
                }
            }
        }
        return tables;
    }

    private String getTableType(Connection con, String tableName) throws SQLException {
        String sql = """
                SELECT TABLE_TYPE
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = ?
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("TABLE_TYPE") : null;
            }
        }
    }

    private String quoteIdentifier(String identifier) {
        return "[" + identifier.replace("]", "]]") + "]";
    }

    private String buildMigrationPlaceholder(String maLoai) {
        String suffix = normalizeCode(maLoai);
        if (suffix == null) {
            suffix = "TMP";
        }
        String placeholder = "__MIG__" + suffix.toUpperCase(Locale.ROOT);
        return placeholder.length() > 50 ? placeholder.substring(0, 50) : placeholder;
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private String buildSqlMessage(SQLException e) {
        return "SQLState=" + e.getSQLState() + ", ErrorCode=" + e.getErrorCode() + ", Message=" + e.getMessage();
    }
}
