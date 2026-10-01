package entity;

import java.util.Objects;

public class KhachHang {
    public static final String TRANG_THAI_DANG_HOAT_DONG = "Đang hoạt động";
    public static final String TRANG_THAI_VO_HIEU_HOA = "Vô hiệu hóa";

    private String maKhachHang;
    private String tenKhachHang;
    private String diaChi;
    private boolean gioiTinh;
    private String soDienThoai;
    private String loaiGiayTo;
    private String soGiayTo;
    private String trangThai = TRANG_THAI_DANG_HOAT_DONG;

    public KhachHang() {}

    public KhachHang(String maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public KhachHang(String maKhachHang, String tenKhachHang, String diaChi, boolean gioiTinh, String soDienThoai) {
        this(maKhachHang, tenKhachHang, diaChi, gioiTinh, soDienThoai, null, null);
    }

    public KhachHang(String maKhachHang, String tenKhachHang, String diaChi, boolean gioiTinh,
                     String soDienThoai, String loaiGiayTo, String soGiayTo) {
        this(maKhachHang, tenKhachHang, diaChi, gioiTinh, soDienThoai, loaiGiayTo, soGiayTo, TRANG_THAI_DANG_HOAT_DONG);
    }

    public KhachHang(String maKhachHang, String tenKhachHang, String diaChi, boolean gioiTinh,
                     String soDienThoai, String loaiGiayTo, String soGiayTo, String trangThai) {
        this.maKhachHang = maKhachHang;
        this.tenKhachHang = tenKhachHang;
        this.diaChi = diaChi;
        this.gioiTinh = gioiTinh;
        this.soDienThoai = soDienThoai;
        this.loaiGiayTo = loaiGiayTo;
        this.soGiayTo = soGiayTo;
        setTrangThai(trangThai);
    }

    public String getMaKhachHang() { return maKhachHang; }
    public void setMaKhachHang(String maKhachHang) { 
        if (!maKhachHang.matches("^KH\\d{3,}$")) {
            throw new IllegalArgumentException("Mã khách hàng phải bắt đầu bằng KH và theo sau là ít nhất 3 chữ số");
        }
        this.maKhachHang = maKhachHang; 
    }

    public String getTenKhachHang() { return tenKhachHang; }
    public void setTenKhachHang(String tenKhachHang) { 
        if (tenKhachHang == null || tenKhachHang.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên khách hàng không được rỗng");
        }
        this.tenKhachHang = tenKhachHang; 
    }

    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { 
        if (diaChi == null || diaChi.trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ không được rỗng");
        }
        this.diaChi = diaChi; 
    }

    public boolean isGioiTinh() { return gioiTinh; }
    public void setGioiTinh(boolean gioiTinh) { this.gioiTinh = gioiTinh; }

    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public String getLoaiGiayTo() { return loaiGiayTo; }
    public void setLoaiGiayTo(String loaiGiayTo) { this.loaiGiayTo = loaiGiayTo; }

    public String getSoGiayTo() { return soGiayTo; }
    public void setSoGiayTo(String soGiayTo) { this.soGiayTo = soGiayTo; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) {
        String normalized = trangThai == null ? "" : trangThai.trim();
        if (normalized.isEmpty()) {
            this.trangThai = TRANG_THAI_DANG_HOAT_DONG;
        } else if (TRANG_THAI_VO_HIEU_HOA.equalsIgnoreCase(normalized)) {
            this.trangThai = TRANG_THAI_VO_HIEU_HOA;
        } else {
            this.trangThai = TRANG_THAI_DANG_HOAT_DONG;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(maKhachHang);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        KhachHang other = (KhachHang) obj;
        return Objects.equals(maKhachHang, other.maKhachHang);
    }

    @Override
    public String toString() {
        return "KhachHang [maKhachHang=" + maKhachHang + ", tenKhachHang=" + tenKhachHang + "]";
    }
    public boolean isDangHoatDong() { return !TRANG_THAI_VO_HIEU_HOA.equalsIgnoreCase(trangThai); }
    public void setDangHoatDong(boolean dangHoatDong) {
        this.trangThai = dangHoatDong ? TRANG_THAI_DANG_HOAT_DONG : TRANG_THAI_VO_HIEU_HOA;
    }
}
