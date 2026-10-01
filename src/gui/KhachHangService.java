package gui;

import dao.KhachHangDAO;
import entity.KhachHang;

import java.util.List;

public class KhachHangService {

    public static final String GIAY_TO_CCCD = "CCCD";
    public static final String GIAY_TO_HO_CHIEU = "Hộ chiếu";

    private final KhachHangDAO dao = new KhachHangDAO();

    public List<KhachHang> getAll() {
        return dao.getAllKhachHang();
    }

    public List<KhachHang> getAllActive() {
        return dao.getAllKhachHang().stream()
                .filter(KhachHang::isDangHoatDong)
                .toList();
    }

    public List<KhachHang> searchByMa(String keyword) {
        String normalized = nullSafe(keyword).trim().toLowerCase();
        return getAll().stream()
                .filter(kh -> nullSafe(kh.getMaKhachHang()).toLowerCase().contains(normalized))
                .toList();
    }

    public List<KhachHang> searchByPhone(String keyword) {
        String normalized = nullSafe(keyword).trim();
        return getAll().stream()
                .filter(kh -> nullSafe(kh.getSoDienThoai()).contains(normalized))
                .toList();
    }

    public List<KhachHang> searchByDocument(String keyword) {
        String normalized = nullSafe(keyword).trim().toLowerCase();
        return getAll().stream()
                .filter(kh -> nullSafe(kh.getSoGiayTo()).trim().toLowerCase().contains(normalized))
                .toList();
    }

    public KhachHang findByPhone(String phone) {
        String normalized = nullSafe(phone).trim();
        return getAll().stream()
                .filter(kh -> normalized.equals(nullSafe(kh.getSoDienThoai()).trim()))
                .findFirst()
                .orElse(null);
    }

    public KhachHang findByPhoneOrDocument(String value) {
        String normalized = nullSafe(value).trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return dao.getKhachHangByPhoneOrDocument(normalized);
    }

    public boolean create(KhachHang kh) {
        return dao.create(kh);
    }

    public boolean update(KhachHang kh) {
        return dao.update(kh);
    }

    public boolean existsByPhone(String phone) {
        String normalized = nullSafe(phone).trim();
        return !normalized.isEmpty() && dao.existsByPhone(normalized);
    }

    public boolean existsByPhoneExceptCustomerId(String phone, String customerId) {
        String normalizedPhone = nullSafe(phone).trim();
        String normalizedCustomerId = nullSafe(customerId).trim();
        return !normalizedPhone.isEmpty()
                && !normalizedCustomerId.isEmpty()
                && dao.existsByPhoneExceptCustomerId(normalizedPhone, normalizedCustomerId);
    }

    public boolean existsByDocument(String soGiayTo) {
        String normalized = nullSafe(soGiayTo).trim();
        return !normalized.isEmpty() && dao.existsByDocument(normalized);
    }

    public boolean existsByDocumentExceptCustomerId(String soGiayTo, String customerId) {
        String normalizedDocument = nullSafe(soGiayTo).trim();
        String normalizedCustomerId = nullSafe(customerId).trim();
        return !normalizedDocument.isEmpty()
                && !normalizedCustomerId.isEmpty()
                && dao.existsByDocumentExceptCustomerId(normalizedDocument, normalizedCustomerId);
    }

    public boolean hasInvoice(String maKhachHang) {
        return dao.hasInvoice(maKhachHang);
    }

    public boolean isCurrentlyStaying(String maKhachHang) {
        return dao.isCurrentlyStaying(maKhachHang);
    }

    public boolean delete(String maKhachHang) {
        return dao.delete(maKhachHang);
    }

    public boolean updateTrangThai(String maKhachHang, boolean dangHoatDong) {
        return dao.updateTrangThai(maKhachHang, dangHoatDong);
    }

    public String generateNextId() {
        return dao.generateNextId();
    }

    public boolean hasChanges(KhachHang original, KhachHang updated) {
        if (!nullSafe(original.getTenKhachHang()).trim()
                .equals(nullSafe(updated.getTenKhachHang()).trim()))
            return true;
        if (!nullSafe(original.getDiaChi()).trim()
                .equals(nullSafe(updated.getDiaChi()).trim()))
            return true;
        if (!nullSafe(original.getSoDienThoai()).trim()
                .equals(nullSafe(updated.getSoDienThoai()).trim()))
            return true;
        if (!nullSafe(original.getLoaiGiayTo()).trim()
                .equals(nullSafe(updated.getLoaiGiayTo()).trim()))
            return true;
        if (!nullSafe(original.getSoGiayTo()).trim()
                .equals(nullSafe(updated.getSoGiayTo()).trim()))
            return true;
        if (!nullSafe(original.getTrangThai()).trim()
                .equals(nullSafe(updated.getTrangThai()).trim()))
            return true;
        return original.isGioiTinh() != updated.isGioiTinh();
    }

    public String validateTen(String ten) {
        if (ten == null || ten.trim().isEmpty()) {
            return "Vui lòng nhập tên khách hàng.";
        }
        if (ten.startsWith(" ") || ten.endsWith(" ")) {
            return "Tên khách hàng không được có dấu cách ở đầu hoặc cuối.";
        }
        String[] words = ten.trim().split("\\s+");
        if (words.length < 2) {
            return "Vui lòng nhập đầy đủ họ và tên khách hàng.";
        }
        return null;
    }

    public String validateSdt(String sdt) {
        if (sdt == null || sdt.trim().isEmpty())
            return "Số điện thoại không được để trống!";
        if (!sdt.matches("^0[0-9]{9}$"))
            return "Số điện thoại không hợp lệ!\n(Phải có 10 chữ số, bắt đầu bằng 0)";
        return null;
    }

    public String validateLoaiGiayTo(String loaiGiayTo) {
        if (normalizeLoaiGiayTo(loaiGiayTo) == null) {
            return "Vui lòng chọn loại giấy tờ.";
        }
        return null;
    }

    public String validateSoGiayTo(String loaiGiayTo, String soGiayTo) {
        String normalizedType = normalizeLoaiGiayTo(loaiGiayTo);
        String normalizedValue = nullSafe(soGiayTo).trim();
        if (normalizedType == null) {
            return "Vui lòng chọn loại giấy tờ.";
        }
        if (normalizedValue.isEmpty()) {
            return "Vui lòng nhập số CCCD hoặc hộ chiếu.";
        }
        if (GIAY_TO_CCCD.equals(normalizedType)) {
            if (!normalizedValue.matches("\\d{12}")) {
                return "CCCD phải gồm đúng 12 chữ số.";
            }
            return null;
        }
        // Hộ chiếu: A-Z + 0-9, 6-12 ký tự
        if (!normalizedValue.matches("[A-Z0-9]{6,12}")) {
            return "Hộ chiếu chỉ gồm chữ hoa và số (6–12 ký tự).";
        }
        return null;
    }

    public String normalizeLoaiGiayTo(String loaiGiayTo) {
        String raw = nullSafe(loaiGiayTo).trim();
        String normalized = raw.toUpperCase();
        if (GIAY_TO_CCCD.equalsIgnoreCase(raw) || "CAN CUOC".equals(normalized) || "CĂN CƯỚC".equals(raw)) {
            return GIAY_TO_CCCD;
        }
        if (GIAY_TO_HO_CHIEU.equalsIgnoreCase(raw) || "HO CHIEU".equals(normalized) || "PASSPORT".equals(normalized)) {
            return GIAY_TO_HO_CHIEU;
        }
        return null;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
