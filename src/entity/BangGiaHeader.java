package entity;

import java.time.LocalDate;
import java.util.Objects;

public class BangGiaHeader {
    public static final String LOAI_NGAY_THUONG = "NGÀY_THƯỜNG";
    public static final String LOAI_CUOI_TUAN = "CUỐI_TUẦN";
    public static final String LOAI_NGAY_LE = "NGÀY_LỄ";

    public static final String TRANG_THAI_DANG_HOAT_DONG = "Đang hoạt động";
    public static final String TRANG_THAI_KHONG_HOAT_DONG = "Không hoạt động";

    private String maBangGia;
    private String tenBangGia;
    private String loaiBangGia;
    private String loaiNgay;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private boolean trangThai;
    private double phanTramTang;

    public BangGiaHeader() {
        this.trangThai = true;
        this.phanTramTang = 0;
        this.loaiBangGia = "";
        this.loaiNgay = LOAI_NGAY_THUONG;
    }

    public BangGiaHeader(String maBangGia) {
        this();
        this.maBangGia = maBangGia;
    }

    public BangGiaHeader(String maBangGia, String tenBangGia, LocalDate ngayBatDau, LocalDate ngayKetThuc, String loaiNgay) {
        this(maBangGia, tenBangGia, "", loaiNgay, ngayBatDau, ngayKetThuc, true, 0);
    }

    public BangGiaHeader(String maBangGia, String tenBangGia, LocalDate ngayBatDau, LocalDate ngayKetThuc,
                         String loaiNgay, String trangThai, double phanTramTang) {
        this(maBangGia, tenBangGia, "", loaiNgay, ngayBatDau, ngayKetThuc,
                TRANG_THAI_DANG_HOAT_DONG.equalsIgnoreCase(trangThai), phanTramTang);
    }

    public BangGiaHeader(String maBangGia, String tenBangGia, String loaiBangGia, String loaiNgay,
                         LocalDate ngayBatDau, LocalDate ngayKetThuc, boolean trangThai) {
        this(maBangGia, tenBangGia, loaiBangGia, loaiNgay, ngayBatDau, ngayKetThuc, trangThai, 0);
    }

    public BangGiaHeader(String maBangGia, String tenBangGia, String loaiBangGia, String loaiNgay,
                         LocalDate ngayBatDau, LocalDate ngayKetThuc, boolean trangThai, double phanTramTang) {
        this.maBangGia = maBangGia;
        this.tenBangGia = tenBangGia;
        this.loaiBangGia = loaiBangGia;
        this.loaiNgay = loaiNgay;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
        this.trangThai = trangThai;
        this.phanTramTang = phanTramTang;
    }

    public String getMaBangGia() {
        return maBangGia;
    }

    public void setMaBangGia(String maBangGia) {
        this.maBangGia = maBangGia;
    }

    public String getTenBangGia() {
        return tenBangGia;
    }

    public void setTenBangGia(String tenBangGia) {
        this.tenBangGia = tenBangGia;
    }

    public String getLoaiBangGia() {
        return loaiBangGia;
    }

    public void setLoaiBangGia(String loaiBangGia) {
        this.loaiBangGia = loaiBangGia;
    }

    public String getLoaiNgay() {
        return loaiNgay;
    }

    public void setLoaiNgay(String loaiNgay) {
        this.loaiNgay = loaiNgay;
    }

    public LocalDate getNgayBatDau() {
        return ngayBatDau;
    }

    public void setNgayBatDau(LocalDate ngayBatDau) {
        this.ngayBatDau = ngayBatDau;
    }

    public LocalDate getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(LocalDate ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }

    public String getTrangThai() {
        return trangThai ? TRANG_THAI_DANG_HOAT_DONG : TRANG_THAI_KHONG_HOAT_DONG;
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public boolean getTrangThaiBoolean() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai != null
                && !TRANG_THAI_KHONG_HOAT_DONG.equalsIgnoreCase(trangThai.trim());
    }

    public double getPhanTramTang() {
        return phanTramTang;
    }

    public void setPhanTramTang(double phanTramTang) {
        this.phanTramTang = phanTramTang;
    }

    public boolean isDangHoatDong() {
        return trangThai;
    }

    public boolean isDacBiet() {
        return LOAI_NGAY_LE.equalsIgnoreCase(loaiNgay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maBangGia);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BangGiaHeader other)) return false;
        return Objects.equals(maBangGia, other.maBangGia);
    }
}
