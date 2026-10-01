package dao;

import entity.BangGiaDetail;
import entity.ChiTietBangGia;
import entity.Phong;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class BangGiaDetailDAO {

    private final ChiTietBangGiaDAO chiTietBangGiaDAO = new ChiTietBangGiaDAO();

    public List<BangGiaDetail> getDetailsByMaBangGia(String maBangGia) {
        return chiTietBangGiaDAO.getDetailsByMaBangGia(maBangGia).stream()
                .map(this::toBangGiaDetail)
                .toList();
    }

    public BangGiaDetail getDetailByBangGiaAndLoaiPhong(String maBangGia, String loaiPhong) {
        ChiTietBangGia chiTiet = chiTietBangGiaDAO.getById(maBangGia, loaiPhong);
        return chiTiet == null ? null : toBangGiaDetail(chiTiet);
    }

    public BangGiaDetail getDetailByBangGiaAndLoai(String maBangGia, String loaiPhong) {
        return getDetailByBangGiaAndLoaiPhong(maBangGia, loaiPhong);
    }

    public double getGiaTheoNgay(Phong phong, LocalDate ngay) {
        return chiTietBangGiaDAO.getGiaTheoNgay(phong, ngay);
    }

    public double getGiaTheoGio(Phong phong, LocalDate ngay) {
        return chiTietBangGiaDAO.getGiaTheoGio(phong, ngay);
    }

    public double calculateStayPrice(String maLoai, LocalDateTime checkIn, LocalDateTime checkOut) {
        Phong phongTam = new Phong("P000", maLoai, 1, "TRONG", 0);
        return chiTietBangGiaDAO.calculateStayPrice(phongTam, checkIn, checkOut);
    }

    public boolean deleteByMaBangGia(String maBangGia) {
        return chiTietBangGiaDAO.deleteByMaBangGia(maBangGia);
    }

    public boolean saveOrUpdate(BangGiaDetail ct) {
        if (ct == null || ct.getBangGia() == null) {
            return false;
        }
        return chiTietBangGiaDAO.saveOrUpdate(toChiTietBangGia(ct));
    }

    private BangGiaDetail toBangGiaDetail(ChiTietBangGia chiTiet) {
        return new BangGiaDetail(
                chiTiet.getBangGia(),
                chiTiet.getMaLoai(),
                chiTiet.getGiaTheoGio(),
                chiTiet.getGiaQuaDem(),
                chiTiet.getGiaTheoNgay(),
                chiTiet.getGiaCuoiTuan(),
                chiTiet.getGiaLe(),
                chiTiet.getPhuThu()
        );
    }

    private ChiTietBangGia toChiTietBangGia(BangGiaDetail detail) {
        return new ChiTietBangGia(
                detail.getBangGia(),
                detail.getLoaiPhongApDung(),
                detail.getGiaTheoGio(),
                detail.getGiaQuaDem(),
                detail.getGiaTheoNgay(),
                detail.getGiaCuoiTuan(),
                detail.getGiaLe(),
                detail.getPhuThu()
        );
    }
}
