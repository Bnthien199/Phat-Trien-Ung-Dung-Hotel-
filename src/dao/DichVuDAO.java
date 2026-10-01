package dao;

import connectDB.ConnectDB;
import entity.DichVu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DichVuDAO {

    public List<DichVu> getActiveDichVu() {
        List<DichVu> ds = new ArrayList<>();
        String sql = """
                SELECT *
                FROM DichVu
                WHERE LOWER(trangThai) LIKE N'%kinh doanh%'
                  AND LOWER(trangThai) NOT LIKE N'%ngung%'
                  AND LOWER(trangThai) NOT LIKE N'%ngừng%'
                ORDER BY maDichVu
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ds.add(mapDichVu(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tai danh sach dich vu dang kinh doanh. " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public List<DichVu> getAllDichVu() {
        List<DichVu> ds = new ArrayList<>();
        String sql = "SELECT * FROM DichVu ORDER BY maDichVu";

        try (Connection con = ConnectDB.getConnection();
             Statement statement = con.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                ds.add(mapDichVu(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tai danh sach dich vu. " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public boolean create(DichVu dv) {
        String sql = "INSERT INTO DichVu(maDichVu, tenDichVu, donGia, trangThai) VALUES(?, ?, ?, ?)";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, dv.getMaDichVu());
            stmt.setString(2, dv.getTenDichVu());
            stmt.setDouble(3, dv.getDonGia());
            stmt.setString(4, dv.getTrangThai());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong the them dich vu moi. " + buildSqlMessage(e), e);
        }
    }

    public boolean update(DichVu dv) {
        String sql = "UPDATE DichVu SET tenDichVu = ?, donGia = ?, trangThai = ? WHERE maDichVu = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, dv.getTenDichVu());
            stmt.setDouble(2, dv.getDonGia());
            stmt.setString(3, dv.getTrangThai());
            stmt.setString(4, dv.getMaDichVu());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong the cap nhat dich vu. " + buildSqlMessage(e), e);
        }
    }

    public boolean delete(String maDichVu) {
        if (hasUsage(maDichVu)) {
            throw new IllegalStateException("Khong the xoa dich vu da phat sinh du lieu.");
        }

        String sql = "DELETE FROM DichVu WHERE maDichVu = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maDichVu);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong the xoa dich vu. " + buildSqlMessage(e), e);
        }
    }

    public DichVu findByMa(String maDichVu) {
        String sql = "SELECT * FROM DichVu WHERE maDichVu = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maDichVu);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapDichVu(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tim dich vu theo ma. " + buildSqlMessage(e), e);
        }
        return null;
    }

    public boolean hasUsage(String maDichVu) {
        String sql = """
                SELECT TABLE_NAME
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE COLUMN_NAME = 'maDichVu'
                  AND TABLE_NAME <> 'DichVu'
                ORDER BY CASE WHEN TABLE_NAME = 'ChiTietHoaDon' THEN 0 ELSE 1 END, TABLE_NAME
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (tableName == null || tableName.isBlank()) {
                    continue;
                }
                if (existsServiceReference(con, tableName, maDichVu)) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Khong the kiem tra du lieu phat sinh cua dich vu. " + buildSqlMessage(e), e);
        }
    }

    private boolean existsServiceReference(Connection con, String tableName, String maDichVu) throws SQLException {
        String sql = "SELECT TOP 1 1 FROM [" + tableName + "] WHERE maDichVu = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maDichVu);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private DichVu mapDichVu(ResultSet rs) throws SQLException {
        return new DichVu(
                rs.getString("maDichVu"),
                rs.getString("tenDichVu"),
                rs.getDouble("donGia"),
                rs.getString("trangThai")
        );
    }

    private String buildSqlMessage(SQLException e) {
        return "SQLState=" + e.getSQLState() + ", ErrorCode=" + e.getErrorCode() + ", Message=" + e.getMessage();
    }
}
