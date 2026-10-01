USE QuanLyKhachSan_N06_v5;
GO
    -- Cập nhật trạng thái bảng Phong
    UPDATE Phong SET trangThai = 'TRONG' WHERE trangThai = N'Trống';
    UPDATE Phong SET trangThai = 'DANG_O' WHERE trangThai = N'Đang ở';
    UPDATE Phong SET trangThai = 'SUA_CHUA' WHERE trangThai = N'Sửa chữa';
    -- Cập nhật trạng thái bảng PhieuDatPhong (nếu có)
    UPDATE PhieuDatPhong SET trangThai = 'CHO_XAC_NHAN' WHERE trangThai = N'Chờ xác nhận';
    UPDATE PhieuDatPhong SET trangThai = 'DA_THANH_TOAN' WHERE trangThai = N'Đã thanh toán';
    UPDATE PhieuDatPhong SET trangThai = 'DA_HUY' WHERE trangThai = N'Đã hủy';
    UPDATE PhieuDatPhong SET trangThai = 'DANG_O' WHERE trangThai = N'Đang ở';
   GO
