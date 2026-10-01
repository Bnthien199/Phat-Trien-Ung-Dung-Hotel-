package dao;

import connectDB.ConnectDB;
import entity.ChiTietHoaDon;
import entity.DichVu;
import entity.HoaDon;
import entity.Phong;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ChiTietHoaDonDAO {
    public List<ChiTietHoaDon> getByMaHoaDon(String maHD) {
        return getDSChiTietByMaHD(maHD);
    }

    public List<ChiTietHoaDon> getByHoaDonAndRoom(String maHD, String maPhong) {
        List<ChiTietHoaDon> ds = new ArrayList<>();
        String sql = """
                SELECT cthd.maHoaDon, cthd.maDichVu, cthd.maPhong, cthd.soLuong, cthd.donGiaLuuTru,
                       dv.tenDichVu, dv.trangThai,
                       CAST(NULL AS NVARCHAR(50)) AS loaiPhong
                FROM ChiTietHoaDon cthd
                JOIN DichVu dv ON dv.maDichVu = cthd.maDichVu
                WHERE cthd.maHoaDon = ?
                  AND cthd.maPhong = ?
                ORDER BY cthd.maDichVu
                """;
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maHD);
            stmt.setString(2, maPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ds.add(new ChiTietHoaDon(
                            new HoaDon(rs.getString("maHoaDon")),
                            new DichVu(
                                    rs.getString("maDichVu"),
                                    rs.getString("tenDichVu"),
                                    rs.getDouble("donGiaLuuTru"),
                                    rs.getString("trangThai")
                            ),
                            buildPhong(rs),
                            rs.getInt("soLuong"),
                            rs.getDouble("donGiaLuuTru")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tai chi tiet hoa don theo phong. " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public List<ChiTietHoaDon> getDSChiTietByMaHD(String maHD) {
        List<ChiTietHoaDon> ds = new ArrayList<>();
        String sql = """
                SELECT cthd.maHoaDon, cthd.maDichVu, cthd.maPhong, cthd.soLuong, cthd.donGiaLuuTru,
                       dv.tenDichVu, dv.trangThai,
                       CAST(NULL AS NVARCHAR(50)) AS loaiPhong
                FROM ChiTietHoaDon cthd
                JOIN DichVu dv ON dv.maDichVu = cthd.maDichVu
                WHERE cthd.maHoaDon = ?
                ORDER BY cthd.maPhong, cthd.maDichVu
                """;
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maHD);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ChiTietHoaDon ct = new ChiTietHoaDon(
                            new HoaDon(rs.getString("maHoaDon")),
                            new DichVu(
                                    rs.getString("maDichVu"),
                                    rs.getString("tenDichVu"),
                                    rs.getDouble("donGiaLuuTru"),
                                    rs.getString("trangThai")
                            ),
                            buildPhong(rs),
                            rs.getInt("soLuong"),
                            rs.getDouble("donGiaLuuTru")
                    );
                    ds.add(ct);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tai chi tiet hoa don. " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public boolean addOrUpdateService(ChiTietHoaDon ct) {
        String sqlCheck = "SELECT 1 FROM ChiTietHoaDon WHERE maHoaDon = ? AND maPhong = ? AND maDichVu = ?";
        String sqlUpdate = """
                UPDATE ChiTietHoaDon
                SET soLuong = soLuong + ?, donGiaLuuTru = ?
                WHERE maHoaDon = ? AND maPhong = ? AND maDichVu = ?
                """;
        String sqlInsert = """
                INSERT INTO ChiTietHoaDon(maHoaDon, maDichVu, maPhong, soLuong, donGiaLuuTru)
                VALUES(?, ?, ?, ?, ?)
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {
            psCheck.setString(1, ct.getHoaDon().getMaHoaDon());
            psCheck.setString(2, ct.getPhong() != null ? ct.getPhong().getMaPhong() : null);
            psCheck.setString(3, ct.getDichVu().getMaDichVu());
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdate)) {
                        psUpdate.setInt(1, ct.getSoLuong());
                        psUpdate.setDouble(2, ct.getDonGiaLuuTru());
                        psUpdate.setString(3, ct.getHoaDon().getMaHoaDon());
                        psUpdate.setString(4, ct.getPhong() != null ? ct.getPhong().getMaPhong() : null);
                        psUpdate.setString(5, ct.getDichVu().getMaDichVu());
                        return psUpdate.executeUpdate() > 0;
                    }
                }
            }

            try (PreparedStatement psInsert = con.prepareStatement(sqlInsert)) {
                psInsert.setString(1, ct.getHoaDon().getMaHoaDon());
                psInsert.setString(2, ct.getDichVu().getMaDichVu());
                psInsert.setString(3, ct.getPhong() != null ? ct.getPhong().getMaPhong() : null);
                psInsert.setInt(4, ct.getSoLuong());
                psInsert.setDouble(5, ct.getDonGiaLuuTru());
                return psInsert.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the them/cap nhat dich vu hoa don. " + buildSqlMessage(e), e);
        }
    }

    /**
     * Upserts a service row: increments quantity if already present, inserts otherwise.
     * This is the preferred method for adding services during an active checkout session.
     */
    public boolean saveOrUpdateService(String maHoaDon, String maPhong, String maDichVu, int soLuong, double donGia) {
        String sqlCheck = "SELECT soLuong FROM ChiTietHoaDon WHERE maHoaDon = ? AND maPhong = ? AND maDichVu = ?";
        String sqlUpdate = """
                UPDATE ChiTietHoaDon
                SET soLuong = soLuong + ?
                WHERE maHoaDon = ? AND maPhong = ? AND maDichVu = ?
                """;
        String sqlInsert = """
                INSERT INTO ChiTietHoaDon(maHoaDon, maDichVu, maPhong, soLuong, donGiaLuuTru)
                VALUES(?, ?, ?, ?, ?)
                """;
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {
            psCheck.setString(1, maHoaDon);
            psCheck.setString(2, maPhong);
            psCheck.setString(3, maDichVu);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdate)) {
                        psUpdate.setInt(1, soLuong);
                        psUpdate.setString(2, maHoaDon);
                        psUpdate.setString(3, maPhong);
                        psUpdate.setString(4, maDichVu);
                        return psUpdate.executeUpdate() > 0;
                    }
                }
            }
            try (PreparedStatement psInsert = con.prepareStatement(sqlInsert)) {
                psInsert.setString(1, maHoaDon);
                psInsert.setString(2, maDichVu);
                psInsert.setString(3, maPhong);
                psInsert.setInt(4, soLuong);
                psInsert.setDouble(5, donGia);
                return psInsert.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể lưu dịch vụ vào hóa đơn. " + buildSqlMessage(e), e);
        }
    }

    public boolean updateServiceQuantity(String maHoaDon, String maPhong, String maDichVu, int soLuong) {
        String sql = "UPDATE ChiTietHoaDon SET soLuong = ? WHERE maHoaDon = ? AND maPhong = ? AND maDichVu = ?";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, soLuong);
            stmt.setString(2, maHoaDon);
            stmt.setString(3, maPhong);
            stmt.setString(4, maDichVu);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể cập nhật số lượng dịch vụ. " + buildSqlMessage(e), e);
        }
    }

    public boolean deleteService(String maHoaDon, String maPhong, String maDichVu) {
        String sql = "DELETE FROM ChiTietHoaDon WHERE maHoaDon = ? AND maPhong = ? AND maDichVu = ?";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maHoaDon);
            stmt.setString(2, maPhong);
            stmt.setString(3, maDichVu);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể xóa dịch vụ khỏi hóa đơn. " + buildSqlMessage(e), e);
        }
    }

    public boolean create(ChiTietHoaDon ct) {
        String sql = "INSERT INTO ChiTietHoaDon(maHoaDon, maDichVu, maPhong, soLuong, donGiaLuuTru) VALUES(?, ?, ?, ?, ?)";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, ct.getHoaDon().getMaHoaDon());
            stmt.setString(2, ct.getDichVu().getMaDichVu());
            stmt.setString(3, ct.getPhong() != null ? ct.getPhong().getMaPhong() : null);
            stmt.setInt(4, ct.getSoLuong());
            stmt.setDouble(5, ct.getDonGiaLuuTru());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tao chi tiet hoa don. " + buildSqlMessage(e), e);
        }
    }

    private Phong buildPhong(ResultSet rs) throws SQLException {
        String maPhong = rs.getString("maPhong");
        if (maPhong == null || maPhong.isBlank()) {
            return null;
        }
        Phong phong = new Phong(maPhong);
        String loaiPhong = rs.getString("loaiPhong");
        if (loaiPhong != null) {
            phong.setLoaiPhong(loaiPhong);
        }
        return phong;
    }

    private String buildSqlMessage(SQLException e) {
        return "SQLState=" + e.getSQLState() + ", ErrorCode=" + e.getErrorCode() + ", Message=" + e.getMessage();
    }
}
