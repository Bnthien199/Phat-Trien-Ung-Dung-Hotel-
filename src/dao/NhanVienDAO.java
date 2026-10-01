package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import connectDB.ConnectDB;
import entity.NhanVien;

public class NhanVienDAO {

    private NhanVien mapNhanVien(ResultSet rs) throws SQLException {
        return new NhanVien(
            rs.getString("maNhanVien"),
            rs.getString("tenNhanVien"),
            rs.getString("chucVu"),
            rs.getDouble("luongNhanVien"),
            rs.getBoolean("phaiNhanVien"),
            rs.getString("soDienThoai"),
            rs.getString("tenDangNhap"),
            rs.getString("matKhau"),
            rs.getString("vaiTro")
        );
    }

    public List<NhanVien> getAllNhanVien() {
        List<NhanVien> ds = new ArrayList<>();
        String sql = "SELECT * FROM NhanVien";
        try (Connection con = ConnectDB.getConnection();
             Statement statement = con.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                ds.add(mapNhanVien(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ds;
    }

    public NhanVien getNhanVienByAccount(String user, String pass) {
        String sql = "SELECT * FROM NhanVien WHERE tenDangNhap = ? AND matKhau = ?";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, user);
            stmt.setString(2, pass);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapNhanVien(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public NhanVien getNhanVienByUsername(String user) {
        String sql = "SELECT * FROM NhanVien WHERE tenDangNhap = ?";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, user);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapNhanVien(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean verifyPassword(String maNhanVien, String currentPassword) {
        String sql = "SELECT COUNT(*) FROM NhanVien WHERE maNhanVien = ? AND matKhau = ?";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maNhanVien);
            stmt.setString(2, currentPassword);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePassword(String maNhanVien, String newPassword) {
        String sql = "UPDATE NhanVien SET matKhau = ? WHERE maNhanVien = ?";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, newPassword);
            stmt.setString(2, maNhanVien);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addNhanVien(NhanVien nv) {
        String sql = "INSERT INTO NhanVien (maNhanVien, tenNhanVien, chucVu, luongNhanVien, phaiNhanVien, soDienThoai, tenDangNhap, matKhau, vaiTro) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, nv.getMaNhanVien());
            stmt.setString(2, nv.getTenNhanVien());
            stmt.setString(3, nv.getChucVu());
            stmt.setDouble(4, nv.getLuongNhanVien());
            stmt.setBoolean(5, nv.isPhaiNhanVien());
            stmt.setString(6, nv.getSoDienThoai());
            stmt.setString(7, nv.getTenDangNhap());
            stmt.setString(8, nv.getMatKhau());
            stmt.setString(9, nv.getVaiTro());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateNhanVien(NhanVien nv) {
        String sql = "UPDATE NhanVien SET tenNhanVien = ?, chucVu = ?, luongNhanVien = ?, phaiNhanVien = ?, "
                   + "soDienThoai = ?, tenDangNhap = ?, matKhau = ?, vaiTro = ? WHERE maNhanVien = ?";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, nv.getTenNhanVien());
            stmt.setString(2, nv.getChucVu());
            stmt.setDouble(3, nv.getLuongNhanVien());
            stmt.setBoolean(4, nv.isPhaiNhanVien());
            stmt.setString(5, nv.getSoDienThoai());
            stmt.setString(6, nv.getTenDangNhap());
            stmt.setString(7, nv.getMatKhau());
            stmt.setString(8, nv.getVaiTro());
            stmt.setString(9, nv.getMaNhanVien());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteNhanVien(String maNhanVien) {
        String sql = "DELETE FROM NhanVien WHERE maNhanVien = ?";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maNhanVien);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
