package dao;

import connectDB.ConnectDB;
import entity.BangGiaHeader;
import entity.ChiTietBangGia;
import entity.Phong;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChiTietBangGiaDAO {

    private final BangGiaHeaderDAO bangGiaHeaderDAO = new BangGiaHeaderDAO();

    public List<ChiTietBangGia> getDetailsByMaBangGia(String maBangGia) {
        List<ChiTietBangGia> ds = new ArrayList<>();
        String sql = """
                SELECT maBangGia, maLoai, giaTheoGio, giaQuaDem, giaTheoNgay, giaCuoiTuan, giaLe, phuThu
                FROM ChiTietBangGia
                WHERE maBangGia = ?
                ORDER BY maLoai
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maBangGia);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ds.add(mapChiTiet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tải chi tiết bảng giá. " + buildSqlMessage(e), e);
        }
        return ds;
    }

    public ChiTietBangGia getById(String maBangGia, String maLoai) {
        String sql = """
                SELECT maBangGia, maLoai, giaTheoGio, giaQuaDem, giaTheoNgay, giaCuoiTuan, giaLe, phuThu
                FROM ChiTietBangGia
                WHERE maBangGia = ? AND maLoai = ?
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maBangGia);
            stmt.setString(2, maLoai);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapChiTiet(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tải chi tiết bảng giá theo khóa. " + buildSqlMessage(e), e);
        }
        return null;
    }

    public boolean existsByBangGiaAndLoai(String maBangGia, String maLoai) {
        String sql = """
                SELECT TOP 1 1
                FROM ChiTietBangGia
                WHERE maBangGia = ? AND maLoai = ?
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maBangGia);
            stmt.setString(2, maLoai);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kiểm tra chi tiết bảng giá bị trùng. " + buildSqlMessage(e), e);
        }
    }

    public boolean create(ChiTietBangGia ct) {
        String sql = """
                INSERT INTO ChiTietBangGia(maBangGia, maLoai, giaTheoGio, giaQuaDem, giaTheoNgay, giaCuoiTuan, giaLe, phuThu)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = ConnectDB.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            try {
                con.setAutoCommit(false);
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    bindChiTiet(stmt, ct);
                    boolean inserted = stmt.executeUpdate() > 0;
                    con.commit();
                    return inserted;
                }
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể thêm chi tiết bảng giá. " + buildSqlMessage(e), e);
        }
    }

    public boolean update(ChiTietBangGia ct) {
        String sql = """
                UPDATE ChiTietBangGia
                SET giaTheoGio = ?, giaQuaDem = ?, giaTheoNgay = ?, giaCuoiTuan = ?, giaLe = ?, phuThu = ?
                WHERE maBangGia = ? AND maLoai = ?
                """;

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setDouble(1, ct.getGiaTheoGio());
            stmt.setDouble(2, ct.getGiaQuaDem());
            stmt.setDouble(3, ct.getGiaTheoNgay());
            stmt.setDouble(4, ct.getGiaCuoiTuan());
            stmt.setDouble(5, ct.getGiaLe());
            stmt.setDouble(6, ct.getPhuThu());
            stmt.setString(7, ct.getBangGia().getMaBangGia());
            stmt.setString(8, ct.getMaLoai());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể cập nhật chi tiết bảng giá. " + buildSqlMessage(e), e);
        }
    }

    public boolean delete(String maBangGia, String maLoai) {
        String sql = "DELETE FROM ChiTietBangGia WHERE maBangGia = ? AND maLoai = ?";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maBangGia);
            stmt.setString(2, maLoai);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể xóa chi tiết bảng giá. " + buildSqlMessage(e), e);
        }
    }

    public boolean deleteByMaBangGia(String maBangGia) {
        String sql = "DELETE FROM ChiTietBangGia WHERE maBangGia = ?";
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maBangGia);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể xóa toàn bộ chi tiết bảng giá. " + buildSqlMessage(e), e);
        }
    }

    public boolean saveOrUpdate(ChiTietBangGia ct) {
        ChiTietBangGia existing = getById(ct.getBangGia().getMaBangGia(), ct.getMaLoai());
        return existing == null ? create(ct) : update(ct);
    }

    public double getGiaTheoNgay(Phong phong, LocalDate ngay) {
        if (phong == null || ngay == null || phong.getMaLoaiPhong() == null || phong.getMaLoaiPhong().isBlank()) {
            return 0;
        }

        BangGiaHeader bangGia = bangGiaHeaderDAO.getBangGiaTheoNgay(ngay);
        if (bangGia == null) {
            return 0;
        }

        ChiTietBangGia ct = getById(bangGia.getMaBangGia(), phong.getMaLoaiPhong());
        if (ct == null) {
            return 0;
        }

        return resolveAppliedDailyRate(bangGia, ct, ngay);
    }

    public double getGiaTheoGio(Phong phong, LocalDate ngay) {
        if (phong == null || ngay == null || phong.getMaLoaiPhong() == null || phong.getMaLoaiPhong().isBlank()) {
            return 0;
        }

        BangGiaHeader bangGia = bangGiaHeaderDAO.getBangGiaTheoNgay(ngay);
        if (bangGia == null) {
            return 0;
        }

        ChiTietBangGia ct = getById(bangGia.getMaBangGia(), phong.getMaLoaiPhong());
        if (ct == null) {
            return 0;
        }

        return choosePositive(ct.getGiaTheoGio(), ct.getGiaTheoNgay() / 24.0);
    }

    public double calculateStayPrice(Phong phong, LocalDateTime checkIn, LocalDateTime checkOut) {
        if (phong == null || checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return 0;
        }

        LocalDate startDate = checkIn.toLocalDate();
        LocalDate endDateExclusive = checkOut.toLocalDate();
        if (!endDateExclusive.isAfter(startDate)) {
            endDateExclusive = startDate.plusDays(1);
        }

        double total = 0;
        for (LocalDate current = startDate; current.isBefore(endDateExclusive); current = current.plusDays(1)) {
            total += getGiaTheoNgay(phong, current);
        }
        return total;
    }

    public double resolveAppliedDailyRate(BangGiaHeader bangGia, ChiTietBangGia chiTiet, LocalDate ngay) {
        if (bangGia == null || chiTiet == null) {
            return 0;
        }

        String loaiNgay = bangGiaHeaderDAO.normalizeLoaiNgay(bangGia.getLoaiNgay());
        if (BangGiaHeader.LOAI_NGAY_LE.equals(loaiNgay)) {
            return choosePositive(chiTiet.getGiaLe(), chiTiet.getGiaTheoNgay(), chiTiet.getGiaCuoiTuan());
        }
        if (BangGiaHeader.LOAI_CUOI_TUAN.equals(loaiNgay)) {
            return choosePositive(chiTiet.getGiaCuoiTuan(), chiTiet.getGiaTheoNgay(), chiTiet.getGiaLe());
        }

        if (bangGiaHeaderDAO.isWeekend(ngay)) {
            return choosePositive(chiTiet.getGiaCuoiTuan(), chiTiet.getGiaTheoNgay());
        }
        return choosePositive(chiTiet.getGiaTheoNgay(), chiTiet.getGiaCuoiTuan(), chiTiet.getGiaLe());
    }

    private double choosePositive(double... values) {
        for (double value : values) {
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private void bindChiTiet(PreparedStatement stmt, ChiTietBangGia ct) throws SQLException {
        stmt.setString(1, ct.getBangGia().getMaBangGia());
        stmt.setString(2, ct.getMaLoai());
        stmt.setDouble(3, ct.getGiaTheoGio());
        stmt.setDouble(4, ct.getGiaQuaDem());
        stmt.setDouble(5, ct.getGiaTheoNgay());
        stmt.setDouble(6, ct.getGiaCuoiTuan());
        stmt.setDouble(7, ct.getGiaLe());
        stmt.setDouble(8, ct.getPhuThu());
    }

    private ChiTietBangGia mapChiTiet(ResultSet rs) throws SQLException {
        String maBangGia = rs.getString("maBangGia");
        BangGiaHeader bangGia = bangGiaHeaderDAO.getBangGiaByMa(maBangGia);
        if (bangGia == null) {
            bangGia = new BangGiaHeader(maBangGia);
        }

        return new ChiTietBangGia(
                bangGia,
                rs.getString("maLoai"),
                rs.getDouble("giaTheoGio"),
                rs.getDouble("giaQuaDem"),
                rs.getDouble("giaTheoNgay"),
                rs.getDouble("giaCuoiTuan"),
                rs.getDouble("giaLe"),
                rs.getDouble("phuThu")
        );
    }

    private String buildSqlMessage(SQLException e) {
        return "SQLState=" + e.getSQLState() + ", ErrorCode=" + e.getErrorCode() + ", Message=" + e.getMessage();
    }
}
