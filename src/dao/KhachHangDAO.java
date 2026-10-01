package dao;

import connectDB.ConnectDB;
import entity.KhachHang;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class KhachHangDAO {

    public List<KhachHang> getAllKhachHang() {
        List<KhachHang> ds = new ArrayList<>();
        String sql = "SELECT * FROM KhachHang ORDER BY maKhachHang";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ds.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tai danh sach khach hang. " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public KhachHang getKhachHangBySdt(String sdt) {
        String sql = "SELECT * FROM KhachHang WHERE soDienThoai = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, sdt);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tim khach hang theo so dien thoai. " + buildSqlMessage(e), e);
        }
        return null;
    }

    public KhachHang getKhachHangByPhoneOrDocument(String value) {
        String sql = """
                SELECT TOP 1 *
                FROM KhachHang
                WHERE soDienThoai = ? OR soGiayTo = ?
                ORDER BY maKhachHang
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, value);
            stmt.setString(2, value);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tìm khách hàng theo số điện thoại hoặc giấy tờ. " + buildSqlMessage(e), e);
        }
        return null;
    }

    public boolean existsByPhone(String phone) {
        String sql = "SELECT COUNT(*) FROM KhachHang WHERE soDienThoai = ?";
        return existsByQuery(sql, phone);
    }

    public boolean existsByPhoneExceptCustomerId(String phone, String customerId) {
        String sql = "SELECT COUNT(*) FROM KhachHang WHERE soDienThoai = ? AND maKhachHang <> ?";
        return existsByQuery(sql, phone, customerId);
    }

    public boolean existsByDocument(String soGiayTo) {
        String sql = "SELECT COUNT(*) FROM KhachHang WHERE soGiayTo = ?";
        return existsByQuery(sql, soGiayTo);
    }

    public boolean existsByDocumentExceptCustomerId(String soGiayTo, String customerId) {
        String sql = "SELECT COUNT(*) FROM KhachHang WHERE soGiayTo = ? AND maKhachHang <> ?";
        return existsByQuery(sql, soGiayTo, customerId);
    }

    public KhachHang getKhachHangByMa(String ma) {
        String sql = "SELECT * FROM KhachHang WHERE maKhachHang = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, ma);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tìm khách hàng theo mã. " + buildSqlMessage(e), e);
        }
        return null;
    }

    public boolean create(KhachHang kh) {
        String sql = """
                INSERT INTO KhachHang (maKhachHang, tenKhachHang, diaChi, gioiTinh, soDienThoai, loaiGiayTo, soGiayTo, TrangThai)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, kh.getMaKhachHang());
            stmt.setString(2, kh.getTenKhachHang());
            stmt.setString(3, kh.getDiaChi());
            stmt.setBoolean(4, kh.isGioiTinh());
            stmt.setString(5, kh.getSoDienThoai());
            stmt.setString(6, kh.getLoaiGiayTo());
            stmt.setString(7, kh.getSoGiayTo());
            stmt.setString(8, kh.getTrangThai());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tạo khách hàng mới. " + buildSqlMessage(e), e);
        }
    }

    public boolean update(KhachHang kh) {
        String sql = """
                UPDATE KhachHang
                SET tenKhachHang = ?, diaChi = ?, gioiTinh = ?, soDienThoai = ?, loaiGiayTo = ?, soGiayTo = ?, TrangThai = ?
                WHERE maKhachHang = ?
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, kh.getTenKhachHang());
            stmt.setString(2, kh.getDiaChi());
            stmt.setBoolean(3, kh.isGioiTinh());
            stmt.setString(4, kh.getSoDienThoai());
            stmt.setString(5, kh.getLoaiGiayTo());
            stmt.setString(6, kh.getSoGiayTo());
            stmt.setString(7, kh.getTrangThai());
            stmt.setString(8, kh.getMaKhachHang());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể cập nhật khách hàng. " + buildSqlMessage(e), e);
        }
    }

    public boolean delete(String maKhachHang) {
        String sql = "DELETE FROM KhachHang WHERE maKhachHang = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maKhachHang);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể xóa khách hàng. " + buildSqlMessage(e), e);
        }
    }

    public boolean hasInvoice(String maKhachHang) {
        String sql = """
                SELECT COUNT(*) AS soHoaDon
                FROM HoaDon hd
                JOIN PhieuDatPhong pdp ON hd.maDatPhong = pdp.maDatPhong
                WHERE pdp.maKhachHang = ?
                """;
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maKhachHang);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("soHoaDon") > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra hóa đơn của khách hàng. " + buildSqlMessage(e), e);
        }
        return false;
    }

    public boolean isCurrentlyStaying(String maKhachHang) {
        String sql = """
                SELECT COUNT(*) AS soPhieuDangO
                FROM PhieuDatPhong
                WHERE maKhachHang = ?
                  AND UPPER(ISNULL(trangThai, '')) IN ('DA_NHAN_PHONG', 'DANG_O')
                """;
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maKhachHang);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("soPhieuDangO") > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra trạng thái lưu trú của khách hàng. " + buildSqlMessage(e), e);
        }
        return false;
    }

    public String generateNextId() {
        String sql = "SELECT MAX(CAST(SUBSTRING(maKhachHang, 3, LEN(maKhachHang)) AS INT)) FROM KhachHang";

        try (Connection con = ConnectDB.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return String.format("KH%03d", rs.getInt(1) + 1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể sinh mã khách hàng mới. " + buildSqlMessage(e), e);
        }
        return "KH001";
    }

    private KhachHang mapRow(ResultSet rs) throws SQLException {
        return new KhachHang(
                rs.getString("maKhachHang"),
                rs.getString("tenKhachHang"),
                rs.getString("diaChi"),
                rs.getBoolean("gioiTinh"),
                rs.getString("soDienThoai"),
                getNullableString(rs, "loaiGiayTo"),
                getNullableString(rs, "soGiayTo"),
                getNullableString(rs, "TrangThai"));
    }

    private String getNullableString(ResultSet rs, String columnName) throws SQLException {
        try {
            return rs.getString(columnName);
        } catch (SQLException e) {
            return null;
        }
    }

    private boolean existsByQuery(String sql, String... params) {
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setString(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Khong the kiem tra trung thong tin khach hang. " + buildSqlMessage(e), e);
        }
    }

    private String buildSqlMessage(SQLException e) {
        return "SQLState=" + e.getSQLState() + ", ErrorCode=" + e.getErrorCode() + ", Message=" + e.getMessage();
    }

    public boolean updateTrangThai(String maKhachHang, boolean trangThai) {
        String sql = """
                UPDATE KhachHang
                SET TrangThai = ?
                WHERE maKhachHang = ?
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, trangThai ? KhachHang.TRANG_THAI_DANG_HOAT_DONG : KhachHang.TRANG_THAI_VO_HIEU_HOA);
            stmt.setString(2, maKhachHang);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong the cap nhat trang thai khach hang. " + buildSqlMessage(e), e);
        }
    }
}
