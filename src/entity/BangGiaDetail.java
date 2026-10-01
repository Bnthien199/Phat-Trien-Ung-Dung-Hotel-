package entity;

/**
 * Backward-compatible alias for the older pricing detail model name.
 * The current codebase stores the real implementation in {@link ChiTietBangGia}.
 */
public class BangGiaDetail extends ChiTietBangGia {

    public BangGiaDetail() {
        super();
    }

    public BangGiaDetail(BangGiaHeader bangGia, String loaiPhongApDung) {
        super(bangGia, loaiPhongApDung);
    }

    public BangGiaDetail(BangGiaHeader bangGia, String loaiPhongApDung,
                         double giaTheoGio, double giaQuaDem, double giaTheoNgay,
                         double giaCuoiTuan, double giaLe, double phuThu) {
        super(bangGia, loaiPhongApDung, giaTheoGio, giaQuaDem, giaTheoNgay, giaCuoiTuan, giaLe, phuThu);
    }

    public String getLoaiPhongApDung() {
        return getMaLoai();
    }

    public void setLoaiPhongApDung(String loaiPhongApDung) {
        setMaLoai(loaiPhongApDung);
    }
}
