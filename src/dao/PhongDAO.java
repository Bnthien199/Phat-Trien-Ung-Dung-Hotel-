package dao;

import connectDB.ConnectDB;
import entity.Phong;
import util.BookingStatus;
import util.RoomStatus;
import util.StatusUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PhongDAO {

    public static class LoaiPhongItem {
        private String loaiPhong;
        private String maLoai;
        private boolean addNewItem;

        public LoaiPhongItem(String loaiPhong, String maLoai) {
            this(loaiPhong, maLoai, false);
        }

        public LoaiPhongItem(String loaiPhong, String maLoai, boolean addNewItem) {
            this.loaiPhong = loaiPhong;
            this.maLoai = maLoai;
            this.addNewItem = addNewItem;
        }

        public static LoaiPhongItem addNewItem() {
            return new LoaiPhongItem("+ Thêm loại phòng", "", true);
        }

        public String getLoaiPhong() {
            return loaiPhong;
        }

        public String getMaLoai() {
            return maLoai;
        }

        public boolean isAddNewItem() {
            return addNewItem;
        }

        @Override
        public String toString() {
            return loaiPhong != null ? loaiPhong : "";
        }
    }

    public List<LoaiPhongItem> getDistinctLoaiPhong() {
        return new LoaiPhongDAO().getAllLoaiPhong();
    }

    public List<Phong> filterPhong(String trangThai, String loaiPhong) {
        List<Phong> ds = new ArrayList<>();
        String normalizedStatus = trangThai != null && !trangThai.isBlank()
                ? StatusUtils.roomCode(trangThai)
                : null;

        StringBuilder sql = new StringBuilder("SELECT p.* FROM Phong p WHERE 1=1");
        if (normalizedStatus != null) {
            sql.append(" AND p.trangThai = ?");
        }
        if (loaiPhong != null && !loaiPhong.isBlank()) {
            sql.append(" AND p.loaiPhong = ?");
        }

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {

            int idx = 1;
            if (normalizedStatus != null) {
                stmt.setString(idx++, normalizedStatus);
            }
            if (loaiPhong != null && !loaiPhong.isBlank()) {
                stmt.setString(idx, loaiPhong);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ds.add(mapPhong(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the loc danh sach phong. " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public List<Phong> getAllPhong() {
        List<Phong> dsPhong = new ArrayList<>();
        String sql = "SELECT * FROM Phong";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                dsPhong.add(mapPhong(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tai danh sach phong. " + buildSqlMessage(e), e);
        }
        return dsPhong;
    }

    public boolean create(Phong p) {
        try (Connection con = ConnectDB.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(
                    "INSERT INTO Phong(maPhong, loaiPhong, maLoai, soNguoi, tang, trangThai, giaPhong) VALUES(?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, p.getMaPhong());
                stmt.setString(2, p.getLoaiPhong());
                stmt.setString(3, p.getMaLoai());
                stmt.setInt(4, p.getSoNguoi());
                stmt.setInt(5, p.getTang());
                stmt.setString(6, StatusUtils.roomCode(p.getTrangThai()));
                stmt.setDouble(7, p.getGiaPhong());
                return stmt.executeUpdate() > 0;
            } catch (SQLException ex) {
                try (PreparedStatement legacyStmt = con.prepareStatement(
                        "INSERT INTO Phong(maPhong, loaiPhong, soGiuong, trangThai, giaPhong, sucChua, maLoai) VALUES(?, ?, ?, ?, ?, ?, ?)")) {
                    legacyStmt.setString(1, p.getMaPhong());
                    legacyStmt.setString(2, p.getLoaiPhong());
                    legacyStmt.setInt(3, p.getSoGiuong());
                    legacyStmt.setString(4, StatusUtils.roomCode(p.getTrangThai()));
                    legacyStmt.setDouble(5, p.getGiaPhong());
                    legacyStmt.setInt(6, p.getSucChua());
                    legacyStmt.setString(7, p.getMaLoai());
                    return legacyStmt.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tao phong moi. " + buildSqlMessage(e), e);
        }
    }

    public boolean update(Phong p) {
        try (Connection con = ConnectDB.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(
                    "UPDATE Phong SET loaiPhong=?, maLoai=?, soNguoi=?, tang=?, trangThai=?, giaPhong=? WHERE maPhong=?")) {
                stmt.setString(1, p.getLoaiPhong());
                stmt.setString(2, p.getMaLoai());
                stmt.setInt(3, p.getSoNguoi());
                stmt.setInt(4, p.getTang());
                stmt.setString(5, StatusUtils.roomCode(p.getTrangThai()));
                stmt.setDouble(6, p.getGiaPhong());
                stmt.setString(7, p.getMaPhong());
                return stmt.executeUpdate() > 0;
            } catch (SQLException ex) {
                try (PreparedStatement legacyStmt = con.prepareStatement(
                        "UPDATE Phong SET loaiPhong=?, soGiuong=?, trangThai=?, giaPhong=?, sucChua=?, maLoai=? WHERE maPhong=?")) {
                    legacyStmt.setString(1, p.getLoaiPhong());
                    legacyStmt.setInt(2, p.getSoGiuong());
                    legacyStmt.setString(3, StatusUtils.roomCode(p.getTrangThai()));
                    legacyStmt.setDouble(4, p.getGiaPhong());
                    legacyStmt.setInt(5, p.getSucChua());
                    legacyStmt.setString(6, p.getMaLoai());
                    legacyStmt.setString(7, p.getMaPhong());
                    return legacyStmt.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the cap nhat phong. " + buildSqlMessage(e), e);
        }
    }

    public boolean updateTrangThai(String maPhong, String trangThai) {
        String sql = "UPDATE Phong SET trangThai = ? WHERE maPhong = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, StatusUtils.roomCode(trangThai));
            stmt.setString(2, maPhong);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong the cap nhat trang thai phong. SQL=" + sql
                    + ", params=[" + StatusUtils.roomCode(trangThai) + ", " + maPhong + "]. "
                    + buildSqlMessage(e), e);
        }
    }

    public Phong getPhongByMa(String ma) {
        String sql = "SELECT * FROM Phong WHERE maPhong = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, ma);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapPhong(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tim phong theo ma. " + buildSqlMessage(e), e);
        }
        return null;
    }

    public boolean hasActiveBookingForRoom(String maPhong) {
        String sql = """
                SELECT TOP 1 1
                FROM ChiTietPhieuDat ct
                JOIN PhieuDatPhong pdp ON ct.maDatPhong = pdp.maDatPhong
                WHERE ct.maPhong = ?
                  AND UPPER(ISNULL(pdp.trangThai, '')) NOT IN (?, ?)
                """;
        try (Connection con = ConnectDB.getConnection()) {
            if (!tableExists(con, "ChiTietPhieuDat") || !tableExists(con, "PhieuDatPhong")) {
                return false;
            }
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, maPhong);
                stmt.setString(2, StatusUtils.normalize(BookingStatus.DA_HUY.getCode()));
                stmt.setString(3, StatusUtils.normalize(BookingStatus.DA_THANH_TOAN.getCode()));
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the kiem tra phieu dat dang hoat dong theo phong. " + buildSqlMessage(e), e);
        }
    }

    public boolean hasRelatedRoomData(String maPhong) {
        try (Connection con = ConnectDB.getConnection()) {
            return hasRoomReference(con, "ChiTietPhieuDat", maPhong)
                    || hasRoomReference(con, "ChiTietHoaDon", maPhong)
                    || hasRoomReference(con, "LichSuTrangThaiPhong", maPhong);
        } catch (SQLException e) {
            throw new RuntimeException("Khong the kiem tra du lieu phat sinh cua phong. " + buildSqlMessage(e), e);
        }
    }

    public boolean delete(String maPhong) {
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement("DELETE FROM Phong WHERE maPhong = ?")) {
            stmt.setString(1, maPhong);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong the xoa phong. " + buildSqlMessage(e), e);
        }
    }

    public List<Phong> getPhongTrongTheoThoiGian(java.time.LocalDateTime checkIn, java.time.LocalDateTime checkOut) {
        List<Phong> ds = new ArrayList<>();

        String sql = """
                SELECT p.*
                FROM Phong p
                WHERE p.trangThai <> ?
                  AND p.maPhong NOT IN (
                      SELECT ct.maPhong
                      FROM ChiTietPhieuDat ct
                      JOIN PhieuDatPhong pdp ON ct.maDatPhong = pdp.maDatPhong
                      WHERE UPPER(ISNULL(pdp.trangThai, '')) NOT IN (?, ?)
                        AND NOT (ct.ngayTra <= ? OR ct.ngayNhan >= ?)
                  )
                ORDER BY p.maPhong
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, StatusUtils.roomCode(RoomStatus.SUA_CHUA.getCode()));
            stmt.setString(2, StatusUtils.normalize(BookingStatus.DA_HUY.getCode()));
            stmt.setString(3, StatusUtils.normalize(BookingStatus.DA_THANH_TOAN.getCode()));
            stmt.setTimestamp(4, java.sql.Timestamp.valueOf(checkIn));
            stmt.setTimestamp(5, java.sql.Timestamp.valueOf(checkOut));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ds.add(mapPhong(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the truy van phong trong. " + buildSqlMessage(e), e);
        }

        return ds;
    }

    public List<Phong> findAvailableRooms(java.time.LocalDateTime checkIn, java.time.LocalDateTime checkOut) {
        return getPhongTrongTheoThoiGian(checkIn, checkOut);
    }

    public boolean isUnderMaintenanceAt(String maPhong, java.time.LocalDateTime selectedDateTime) {
        if (maPhong == null || maPhong.isBlank() || selectedDateTime == null) {
            return false;
        }
        String tableCheck = """
                SELECT 1
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = 'LichSuTrangThaiPhong'
                """;
        String sql = """
                SELECT TOP 1 1
                FROM LichSuTrangThaiPhong lstp
                WHERE lstp.maPhong = ?
                  AND UPPER(ISNULL(lstp.trangThai, '')) IN ('SUA_CHUA', 'BAO_TRI')
                  AND lstp.tuNgay <= ?
                  AND (lstp.denNgay IS NULL OR lstp.denNgay > ?)
                """;
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement check = con.prepareStatement(tableCheck)) {
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
            }
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                java.sql.Timestamp target = java.sql.Timestamp.valueOf(selectedDateTime);
                stmt.setString(1, maPhong);
                stmt.setTimestamp(2, target);
                stmt.setTimestamp(3, target);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the truy van lich su sua chua phong. " + buildSqlMessage(e), e);
        }
    }

    private Phong mapPhong(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();

        String ma = rs.getString("maPhong");
        String loai = hasColumn(meta, "loaiPhong") ? rs.getString("loaiPhong") : "";
        int soNguoi = getSoNguoiOrSoGiuong(rs, meta);
        int tang = hasColumn(meta, "tang") ? rs.getInt("tang") : inferTang(ma);
        String trangThai = hasColumn(meta, "trangThai") ? rs.getString("trangThai") : RoomStatus.TRONG.getCode();
        double gia = hasColumn(meta, "giaPhong") ? rs.getDouble("giaPhong") : 0;
        String maLoai = hasColumn(meta, "maLoai") ? rs.getString("maLoai") : null;

        Phong phong = new Phong(ma, loai, soNguoi, tang, trangThai, gia);
        phong.setMaLoaiPhong(maLoai);
        return phong;
    }

    private int getSoNguoiOrSoGiuong(ResultSet rs, ResultSetMetaData meta) throws SQLException {
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

    private int inferTang(String maPhong) {
        if (maPhong == null || maPhong.length() < 2) {
            return 1;
        }
        String digits = maPhong.replaceAll("\\D+", "");
        if (digits.length() < 2) {
            return 1;
        }
        try {
            return Integer.parseInt(digits.substring(0, digits.length() - 2));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private boolean hasRoomReference(Connection con, String tableName, String maPhong) throws SQLException {
        if (!tableExists(con, tableName)) {
            return false;
        }
        String sql = "SELECT TOP 1 1 FROM " + tableName + " WHERE maPhong = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
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

    private String buildSqlMessage(SQLException e) {
        return "SQLState=" + e.getSQLState() + ", ErrorCode=" + e.getErrorCode() + ", Message=" + e.getMessage();
    }
}
