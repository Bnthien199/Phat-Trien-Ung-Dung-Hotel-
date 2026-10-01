package util;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import entity.ChiTietPhieuDat;
import entity.KhachHang;
import entity.NhanVien;
import entity.PhieuDatPhong;
import entity.Phong;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class DepositReceiptPdfExporter {
    private static final DeviceRgb PDF_PRIMARY = new DeviceRgb(13, 148, 136);
    private static final DeviceRgb PDF_TEXT_DARK = new DeviceRgb(15, 23, 42);
    private static final DeviceRgb PDF_TEXT_SUB = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb PDF_BORDER = new DeviceRgb(203, 213, 225);
    private static final DeviceRgb PDF_ROW_ALT = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb PDF_SUMMARY_BG = new DeviceRgb(240, 253, 250);

    private static final Locale VIETNAM = new Locale("vi", "VN");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DepositReceiptPdfExporter() {
    }

    public static File chooseSavePath(Component parent, String maPhieu) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn nơi lưu phiếu đặt cọc PDF");
        fileChooser.setApproveButtonText("Lưu");
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF files (*.pdf)", "pdf"));
        fileChooser.setCurrentDirectory(getDefaultDirectory());
        fileChooser.setSelectedFile(new File(buildDefaultFileName(maPhieu)));

        while (true) {
            int result = fileChooser.showSaveDialog(parent);
            if (result != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            File selected = fileChooser.getSelectedFile();
            if (selected == null) {
                return null;
            }

            selected = ensurePdfExtension(selected);
            if (!selected.exists()) {
                return selected;
            }

            int overwrite = JOptionPane.showConfirmDialog(
                    parent,
                    "File đã tồn tại. Bạn có muốn ghi đè không?",
                    "Xác nhận ghi đè",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (overwrite == JOptionPane.YES_OPTION) {
                return selected;
            }
            if (overwrite == JOptionPane.NO_OPTION) {
                continue;
            }
            return null;
        }
    }

    public static void export(File file, PhieuDatPhong booking, List<ChiTietPhieuDat> details) throws IOException {
        validateExportData(booking, details);

        try (PdfWriter writer = new PdfWriter(file.getAbsolutePath());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {
            document.setMargins(28, 28, 28, 28);
            PdfFont font = createVietnameseFont();

            addHeader(document, font, booking);
            addBookingInfo(document, font, booking);
            addCustomerInfo(document, font, booking.getKhachHang());
            addRoomTable(document, font, details);
            addMoneySummary(document, font, booking, details);
            addCancellationPolicy(document, font);
            addSignatures(document, font);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Không thể xuất phiếu đặt cọc PDF.", ex);
        }
    }

    public static void showSuccessAndAskToOpen(Component parent, File file) {
        JOptionPane.showMessageDialog(
                parent,
                "Xuất phiếu đặt cọc thành công.",
                "Xuất phiếu đặt cọc",
                JOptionPane.INFORMATION_MESSAGE);

        int open = JOptionPane.showConfirmDialog(
                parent,
                "Bạn có muốn mở file PDF không?",
                "Mở file PDF",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (open == JOptionPane.YES_OPTION) {
            openPdf(parent, file);
        }
    }

    public static void openPdf(Component parent, File file) {
        try {
            if (file == null || !file.isFile()) {
                throw new IOException("File PDF không tồn tại.");
            }
            if (!Desktop.isDesktopSupported()) {
                throw new IOException("Máy hiện tại không hỗ trợ mở file tự động.");
            }
            Desktop.getDesktop().open(file);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Không thể mở file PDF: " + ex.getMessage(),
                    "Mở file PDF",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void validateExportData(PhieuDatPhong booking, List<ChiTietPhieuDat> details) {
        if (booking == null) {
            throw new IllegalArgumentException("Chưa chọn phiếu đặt phòng để xuất phiếu đặt cọc.");
        }
        if (isBlank(booking.getMaDatPhong())) {
            throw new IllegalArgumentException("Phiếu đặt phòng thiếu mã phiếu.");
        }
        if (booking.getKhachHang() == null) {
            throw new IllegalArgumentException("Phiếu đặt phòng thiếu thông tin khách hàng.");
        }
        if (booking.getTienDatCoc() <= 0) {
            throw new IllegalArgumentException("Phiếu chưa có tiền đặt cọc.");
        }
        if (isBookingFullyPaid(booking)) {
            throw new IllegalArgumentException("Phiếu đã thanh toán hoàn tất nên không thể xuất phiếu đặt cọc.");
        }
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("Phiếu đặt phòng thiếu danh sách phòng chi tiết.");
        }
    }

    private static void addHeader(Document document, PdfFont font, PhieuDatPhong booking) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{58, 42})).useAllAvailableWidth();
        header.setMarginBottom(10);

        Table brand = new Table(UnitValue.createPercentArray(new float[]{18, 82})).useAllAvailableWidth();
        brand.addCell(logoCell(font));
        brand.addCell(noBorderCell()
                .add(text("HỆ THỐNG QUẢN LÝ KHÁCH SẠN", font, 15, true).setFontColor(PDF_TEXT_DARK))
                .add(text("Quản lý đặt phòng, nhận phòng và thanh toán", font, 9, false)
                        .setFontColor(PDF_TEXT_SUB).setMarginTop(2)));

        header.addCell(noBorderCell().add(brand));
        header.addCell(noBorderCell()
                .setTextAlignment(TextAlignment.RIGHT)
                .add(text("PHIẾU ĐẶT CỌC", font, 18, true).setFontColor(PDF_PRIMARY))
                .add(text("Mã phiếu đặt: " + safe(booking.getMaDatPhong()), font, 10, true)
                        .setFontColor(PDF_TEXT_DARK))
                .add(text("Ngày lập phiếu: " + formatDateTime(firstDate(booking.getNgayDatCoc(), booking.getNgayDat())),
                        font, 9, false).setFontColor(PDF_TEXT_SUB)));
        document.add(header);
        document.add(new Paragraph("").setBorderBottom(new SolidBorder(PDF_BORDER, 1)).setMarginBottom(14));
    }

    private static void addBookingInfo(Document document, PdfFont font, PhieuDatPhong booking) {
        document.add(sectionTitle("ThÃ´ng tin phiáº¿u Ä‘áº·t", font));
        Table table = new Table(UnitValue.createPercentArray(new float[]{20, 30, 20, 30})).useAllAvailableWidth();
        table.setMarginBottom(12);
        addInfoPair(table, font, "MÃ£ phiáº¿u Ä‘áº·t", booking.getMaDatPhong());
        addInfoPair(table, font, "NgÃ y láº­p phiáº¿u", formatDateTime(firstDate(booking.getNgayDatCoc(), booking.getNgayDat())));
        addInfoPair(table, font, "NhÃ¢n viÃªn nháº­n cá»c", employeeLabel(firstEmployee(booking.getNhanVienNhanCoc(), booking.getNhanVien())));
        addInfoPair(table, font, "Trạng thái thanh toán", paymentStatusLabel(booking));
        addInfoPair(table, font, "Ghi chÃº", booking.getGhiChuDatCoc());
        document.add(table);
    }

    private static void addCustomerInfo(Document document, PdfFont font, KhachHang customer) {
        document.add(sectionTitle("Thông tin khách hàng", font));
        Table table = new Table(UnitValue.createPercentArray(new float[]{20, 30, 20, 30})).useAllAvailableWidth();
        table.setMarginBottom(12);
        addInfoPair(table, font, "Họ tên", customer != null ? customer.getTenKhachHang() : "");
        addInfoPair(table, font, "Số điện thoại", customer != null ? customer.getSoDienThoai() : "");
        addInfoPair(table, font, "CCCD/Hộ chiếu", documentLabel(customer));
        addInfoPair(table, font, "Địa chỉ", customer != null ? customer.getDiaChi() : "");
        document.add(table);
    }

    private static void addRoomTable(Document document, PdfFont font, List<ChiTietPhieuDat> details) {
        document.add(sectionTitle("Thông tin đặt phòng", font));
        Table table = new Table(UnitValue.createPercentArray(new float[]{12, 20, 21, 21, 12, 14}))
                .useAllAvailableWidth();
        table.setMarginBottom(12);
        addHeaderCells(table, font, "Phòng", "Loại phòng", "Ngày nhận", "Ngày trả", "Số đêm/giờ", "Thành tiền");

        int rowIndex = 0;
        for (ChiTietPhieuDat detail : details) {
            com.itextpdf.kernel.colors.Color rowBg = rowIndex++ % 2 == 0 ? ColorConstants.WHITE : PDF_ROW_ALT;
            Phong room = detail != null ? detail.getPhong() : null;
            table.addCell(dataCell(room != null ? room.getMaPhong() : "", font, TextAlignment.CENTER, rowBg));
            table.addCell(dataCell(room != null ? room.getLoaiPhong() : "", font, TextAlignment.LEFT, rowBg));
            table.addCell(dataCell(formatDateTime(detail != null ? detail.getNgayNhan() : null), font,
                    TextAlignment.CENTER, rowBg));
            table.addCell(dataCell(formatDateTime(detail != null ? detail.getNgayTra() : null), font,
                    TextAlignment.CENTER, rowBg));
            table.addCell(dataCell(stayLengthLabel(detail), font, TextAlignment.CENTER, rowBg));
            table.addCell(dataCell(formatMoney(detail != null ? detail.getGiaThuePhong() : 0), font,
                    TextAlignment.RIGHT, rowBg));
        }
        document.add(table);
    }

    private static void addMoneySummary(Document document, PdfFont font, PhieuDatPhong booking,
                                        List<ChiTietPhieuDat> details) {
        double roomTotal = booking.getTongTienPhong() > 0 ? booking.getTongTienPhong() : sumRoomTotal(details);
        double deposit = booking.getTienDatCoc();
        double remaining = booking.getTienConLai() > 0 ? booking.getTienConLai()
                : DepositPolicy.calculateRemaining(roomTotal, deposit);

        document.add(sectionTitle("ThÃ´ng tin tiá»n", font));
        Table wrapper = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        wrapper.setMarginBottom(12);
        wrapper.addCell(noBorderCell());

        Table summary = new Table(UnitValue.createPercentArray(new float[]{52, 48})).useAllAvailableWidth();
        summary.setBackgroundColor(PDF_SUMMARY_BG);
        summary.setBorder(new SolidBorder(PDF_BORDER, 0.8f));
        addSummaryRow(summary, font, "Tá»•ng tiá»n phÃ²ng", formatMoney(roomTotal), false);
        addSummaryRow(summary, font, "Tiá»n Ä‘áº·t cá»c", formatMoney(deposit), false);
        addSummaryRow(summary, font, "CÃ²n láº¡i", formatMoney(remaining), false);
        addSummaryRow(summary, font, "Trạng thái thanh toán", paymentStatusLabel(booking), false);
        addSummaryRow(summary, font, "HÃ¬nh thá»©c cá»c", safe(booking.getHinhThucDatCoc()), true);
        wrapper.addCell(noBorderCell().add(summary));
        document.add(wrapper);
    }

    private static void addCancellationPolicy(Document document, PdfFont font) {
        document.add(sectionTitle("Chính sách hủy phòng", font));
        Cell policy = new Cell()
                .setBorder(new SolidBorder(PDF_BORDER, 0.5f))
                .setBackgroundColor(PDF_ROW_ALT)
                .setPadding(10);
        policy.add(text("- Hủy trong ngày đặt cọc: hoàn 100% tiền cọc.", font, 9, false)
                .setFontColor(PDF_TEXT_DARK).setMarginBottom(3));
        policy.add(text("- Từ 1 đến 2 ngày sau đặt cọc: hoàn 75% tiền cọc.", font, 9, false)
                .setFontColor(PDF_TEXT_DARK).setMarginBottom(3));
        policy.add(text("- Từ 3 đến dưới 7 ngày sau đặt cọc: hoàn 50% tiền cọc.", font, 9, false)
                .setFontColor(PDF_TEXT_DARK).setMarginBottom(3));
        policy.add(text("- Từ 7 ngày trở lên: không hoàn cọc.", font, 9, false).setFontColor(PDF_TEXT_DARK));
        document.add(new Table(UnitValue.createPercentArray(new float[]{100})).useAllAvailableWidth().addCell(policy));
    }

    private static void addSignatures(Document document, PdfFont font) {
        Table signature = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        signature.setMarginTop(22);
        signature.addCell(signatureCell(font, "Người lập phiếu"));
        signature.addCell(signatureCell(font, "Khách hàng"));
        document.add(signature);
    }

    private static Cell logoCell(PdfFont font) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(PDF_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(8)
                .add(text("N06", font, 14, true).setFontColor(ColorConstants.WHITE));
    }

    private static void addInfoPair(Table table, PdfFont font, String label, String value) {
        table.addCell(infoLabel(label, font));
        table.addCell(infoValue(value, font));
    }

    private static void addHeaderCells(Table table, PdfFont font, String... titles) {
        for (String title : titles) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(PDF_PRIMARY)
                    .setBorder(new SolidBorder(PDF_PRIMARY, 0.5f))
                    .setPadding(7)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .add(text(title, font, 9, true).setFontColor(ColorConstants.WHITE)));
        }
    }

    private static void addSummaryRow(Table table, PdfFont font, String label, String value, boolean total) {
        com.itextpdf.kernel.colors.Color rowBg = total ? new DeviceRgb(220, 252, 241) : PDF_SUMMARY_BG;
        table.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderTop(total ? new SolidBorder(PDF_BORDER, 0.8f) : Border.NO_BORDER)
                .setPadding(total ? 8 : 6)
                .setBackgroundColor(rowBg)
                .add(text(label, font, total ? 10 : 9, total).setFontColor(total ? PDF_TEXT_DARK : PDF_TEXT_SUB)));
        table.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderTop(total ? new SolidBorder(PDF_BORDER, 0.8f) : Border.NO_BORDER)
                .setPadding(total ? 8 : 6)
                .setBackgroundColor(rowBg)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(text(value, font, total ? 10 : 9.5f, true).setFontColor(PDF_TEXT_DARK)));
    }

    private static Cell signatureCell(PdfFont font, String title) {
        return noBorderCell()
                .setTextAlignment(TextAlignment.CENTER)
                .add(text(title, font, 10, true).setFontColor(PDF_TEXT_DARK))
                .add(text("(Ký và ghi rõ họ tên)", font, 8, false).setFontColor(PDF_TEXT_SUB).setMarginTop(38));
    }

    private static Paragraph sectionTitle(String title, PdfFont font) {
        return text(title, font, 12, true)
                .setFontColor(PDF_TEXT_DARK)
                .setMarginTop(4)
                .setMarginBottom(7);
    }

    private static Cell infoLabel(String label, PdfFont font) {
        return new Cell()
                .setBorder(new SolidBorder(PDF_BORDER, 0.5f))
                .setBackgroundColor(PDF_ROW_ALT)
                .setPadding(6)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(text(label, font, 8.5f, true).setFontColor(PDF_TEXT_SUB));
    }

    private static Cell infoValue(String value, PdfFont font) {
        return new Cell()
                .setBorder(new SolidBorder(PDF_BORDER, 0.5f))
                .setPadding(6)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(text(display(value), font, 9.5f, true).setFontColor(PDF_TEXT_DARK));
    }

    private static Cell dataCell(String value, PdfFont font, TextAlignment alignment,
                                 com.itextpdf.kernel.colors.Color backgroundColor) {
        return new Cell()
                .setBorder(new SolidBorder(PDF_BORDER, 0.5f))
                .setBackgroundColor(backgroundColor)
                .setPadding(6)
                .setTextAlignment(alignment)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(text(display(value), font, 8.5f, false).setFontColor(PDF_TEXT_DARK));
    }

    private static Cell noBorderCell() {
        return new Cell().setBorder(Border.NO_BORDER).setPadding(0);
    }

    private static Paragraph text(String text, PdfFont font, float size, boolean bold) {
        Paragraph paragraph = new Paragraph(text == null ? "" : text)
                .setFont(font)
                .setFontSize(size)
                .setMargin(0);
        if (bold) {
            paragraph.setBold();
        }
        return paragraph;
    }

    private static PdfFont createVietnameseFont() throws IOException {
        Path[] candidates = {
                Path.of("C:/Windows/Fonts/arial.ttf"),
                Path.of("C:/Windows/Fonts/ARIAL.TTF"),
                Path.of("C:/Windows/Fonts/times.ttf"),
                Path.of("C:/Windows/Fonts/tahoma.ttf"),
                Path.of("C:/Windows/Fonts/segoeui.ttf"),
                Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
                Path.of("/usr/share/fonts/dejavu/DejaVuSans.ttf")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                try {
                    return PdfFontFactory.createFont(candidate.toString(), PdfEncodings.IDENTITY_H,
                            PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                } catch (Exception ignored) {
                    // Try the next Unicode font candidate.
                }
            }
        }
        throw new IOException("Không tìm thấy font Unicode để nhúng vào PDF. Hãy cài Arial, Times New Roman hoặc DejaVu Sans.");
    }

    private static File getDefaultDirectory() {
        File defaultDirectory = FileSystemView.getFileSystemView().getDefaultDirectory();
        if (defaultDirectory != null && defaultDirectory.isDirectory()) {
            return defaultDirectory;
        }
        String userHome = System.getProperty("user.home");
        return userHome == null || userHome.isBlank() ? new File(".") : new File(userHome);
    }

    private static String buildDefaultFileName(String maPhieu) {
        String safeId = sanitizeFileName(isBlank(maPhieu) ? "ChuaCoMa" : maPhieu.trim());
        return "PhieuDatCoc_" + safeId + ".pdf";
    }

    private static File ensurePdfExtension(File file) {
        String path = file.getAbsolutePath();
        return path.toLowerCase(Locale.ROOT).endsWith(".pdf") ? file : new File(path + ".pdf");
    }

    private static String sanitizeFileName(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? "--" : value.format(DATE_TIME_FORMAT);
    }

    private static String formatMoney(double value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(VIETNAM);
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);
        return formatter.format(Math.round(Math.max(0, value))) + " VNĐ";
    }

    private static String stayLengthLabel(ChiTietPhieuDat detail) {
        if (detail == null || detail.getNgayNhan() == null || detail.getNgayTra() == null
                || !detail.getNgayTra().isAfter(detail.getNgayNhan())) {
            return "--";
        }
        String type = new PricingService().determineBookingType(detail.getNgayNhan(), detail.getNgayTra());
        if ("theo_gio".equals(type)) {
            double hours = Duration.between(detail.getNgayNhan(), detail.getNgayTra()).toMinutes() / 60.0;
            return formatHours(hours);
        }
        return DepositPolicy.calculateNights(detail.getNgayNhan(), detail.getNgayTra()) + " đêm";
    }

    private static String formatHours(double hours) {
        if (Math.abs(hours - Math.round(hours)) < 0.01) {
            return Math.round(hours) + " giờ";
        }
        return String.format(VIETNAM, "%.1f giờ", hours);
    }

    private static double sumRoomTotal(List<ChiTietPhieuDat> details) {
        if (details == null) {
            return 0;
        }
        return details.stream()
                .filter(item -> item != null)
                .mapToDouble(ChiTietPhieuDat::getGiaThuePhong)
                .sum();
    }

    private static String documentLabel(KhachHang customer) {
        if (customer == null) {
            return "";
        }
        String type = safe(customer.getLoaiGiayTo());
        String number = safe(customer.getSoGiayTo());
        if (type.isBlank()) {
            return number;
        }
        if (number.isBlank()) {
            return type;
        }
        return type + ": " + number;
    }

    private static String employeeLabel(NhanVien employee) {
        if (employee == null) {
            return "";
        }
        String id = safe(employee.getMaNhanVien());
        String name = safe(employee.getTenNhanVien());
        if (id.isBlank()) {
            return name;
        }
        if (name.isBlank()) {
            return id;
        }
        return id + " - " + name;
    }

    private static boolean isBookingFullyPaid(PhieuDatPhong booking) {
        return booking != null
                && (StatusUtils.isBookingStatus(booking.getTrangThai(), BookingStatus.DA_THANH_TOAN)
                || booking.getTienConLai() <= 0.0001);
    }

    private static String paymentStatusLabel(PhieuDatPhong booking) {
        if (booking == null) {
            return "";
        }
        if (isBookingFullyPaid(booking)) {
            return "Đã thanh toán";
        }
        String label = StatusUtils.bookingLabel(booking.getTrangThai());
        return isBlank(label) ? "Chưa thanh toán hết" : label;
    }

    private static NhanVien firstEmployee(NhanVien first, NhanVien fallback) {
        return first != null ? first : fallback;
    }

    private static LocalDateTime firstDate(LocalDateTime first, LocalDateTime fallback) {
        return first != null ? first : fallback;
    }

    private static String display(String value) {
        return isBlank(value) ? "--" : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
