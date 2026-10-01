package entity;

import java.util.Objects;

public class ChiTietBangGia {
    private BangGiaHeader bangGia;
    private String maLoai;
    private double giaTheoGio;
    private double giaQuaDem;
    private double giaTheoNgay;
    private double giaCuoiTuan;
    private double giaLe;
    private double phuThu;

    public ChiTietBangGia() {
    }

    public ChiTietBangGia(BangGiaHeader bangGia, String maLoai) {
        this.bangGia = bangGia;
        this.maLoai = maLoai;
    }

    public ChiTietBangGia(BangGiaHeader bangGia, String maLoai,
                          double giaTheoGio, double giaQuaDem, double giaTheoNgay,
                          double giaCuoiTuan, double giaLe, double phuThu) {
        this.bangGia = bangGia;
        this.maLoai = maLoai;
        this.giaTheoGio = giaTheoGio;
        this.giaQuaDem = giaQuaDem;
        this.giaTheoNgay = giaTheoNgay;
        this.giaCuoiTuan = giaCuoiTuan;
        this.giaLe = giaLe;
        this.phuThu = phuThu;
    }

    public BangGiaHeader getBangGia() {
        return bangGia;
    }

    public void setBangGia(BangGiaHeader bangGia) {
        this.bangGia = bangGia;
    }

    public String getMaLoai() {
        return maLoai;
    }

    public void setMaLoai(String maLoai) {
        if (maLoai == null || maLoai.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã loại phòng không được rỗng");
        }
        this.maLoai = maLoai.trim();
    }

    public double getGiaTheoGio() {
        return giaTheoGio;
    }

    public void setGiaTheoGio(double giaTheoGio) {
        if (giaTheoGio < 0) {
            throw new IllegalArgumentException("Giá theo giờ phải >= 0");
        }
        this.giaTheoGio = giaTheoGio;
    }

    public double getGiaQuaDem() {
        return giaQuaDem;
    }

    public void setGiaQuaDem(double giaQuaDem) {
        if (giaQuaDem < 0) {
            throw new IllegalArgumentException("Giá qua đêm phải >= 0");
        }
        this.giaQuaDem = giaQuaDem;
    }

    public double getGiaTheoNgay() {
        return giaTheoNgay;
    }

    public void setGiaTheoNgay(double giaTheoNgay) {
        if (giaTheoNgay < 0) {
            throw new IllegalArgumentException("Giá theo ngày phải >= 0");
        }
        this.giaTheoNgay = giaTheoNgay;
    }

    public double getGiaCuoiTuan() {
        return giaCuoiTuan;
    }

    public void setGiaCuoiTuan(double giaCuoiTuan) {
        if (giaCuoiTuan < 0) {
            throw new IllegalArgumentException("Giá cuối tuần phải >= 0");
        }
        this.giaCuoiTuan = giaCuoiTuan;
    }

    public double getGiaLe() {
        return giaLe;
    }

    public void setGiaLe(double giaLe) {
        if (giaLe < 0) {
            throw new IllegalArgumentException("Giá lễ phải >= 0");
        }
        this.giaLe = giaLe;
    }

    public double getPhuThu() {
        return phuThu;
    }

    public void setPhuThu(double phuThu) {
        if (phuThu < 0) {
            throw new IllegalArgumentException("Phụ thu phải >= 0");
        }
        this.phuThu = phuThu;
    }

    public String getMaBangGia() {
        return bangGia != null ? bangGia.getMaBangGia() : null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMaBangGia(), maLoai);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ChiTietBangGia other)) return false;
        return Objects.equals(getMaBangGia(), other.getMaBangGia())
                && Objects.equals(maLoai, other.maLoai);
    }

    @Override
    public String toString() {
        return "ChiTietBangGia [maBangGia=" + getMaBangGia() + ", maLoai=" + maLoai + "]";
    }
}