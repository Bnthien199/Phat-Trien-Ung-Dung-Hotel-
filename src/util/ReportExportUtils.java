package util;

import dao.HoaDonDAO.RevenueChartData;
import dao.HoaDonDAO.RevenueStatistics;
import entity.KhachHang;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ReportExportUtils {
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");

    private static final short COLOR_PRIMARY = 0x31;
    private static final short COLOR_HEADER = 0x3F;
    private static final DeviceRgb PDF_PRIMARY = new DeviceRgb(13, 148, 136);
    private static final DeviceRgb PDF_BORDER = new DeviceRgb(203, 213, 225);
    private static final DeviceRgb PDF_ROW_ALT = new DeviceRgb(248, 250, 252);

    private ReportExportUtils() {
    }

    public enum ReportFormat {
        EXCEL,
        PDF
    }

    public record SaveTarget(File file, ReportFormat format) {
    }

    public static String defaultFileName(String prefix) {
        return prefix + "_" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".xlsx";
    }

    public static SaveTarget chooseReportSaveTarget(Window owner, String title, String defaultFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        FileChooser.ExtensionFilter excelFilter = new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx");
        FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf");
        chooser.getExtensionFilters().setAll(excelFilter, pdfFilter);
        chooser.setSelectedExtensionFilter(excelFilter);
        chooser.setInitialFileName(defaultFileName == null || defaultFileName.isBlank()
                ? defaultFileName("BaoCao")
                : ensureFileNameExtension(defaultFileName, "xlsx"));

        File selected = chooser.showSaveDialog(owner);
        if (selected == null) {
            return null;
        }

        String lowerPath = selected.getAbsolutePath().toLowerCase(Locale.ROOT);
        ReportFormat format;
        if (lowerPath.endsWith(".pdf")) {
            format = ReportFormat.PDF;
        } else if (lowerPath.endsWith(".xlsx")) {
            format = ReportFormat.EXCEL;
        } else {
            format = chooser.getSelectedExtensionFilter() == pdfFilter ? ReportFormat.PDF : ReportFormat.EXCEL;
        }

        return new SaveTarget(withExtension(selected, format == ReportFormat.PDF ? "pdf" : "xlsx"), format);
    }

    public static void exportRevenueExcel(File file, String period, LocalDate from, LocalDate to,
            RevenueStatistics statistics, List<RevenueChartData> buckets) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("DoanhThu");
            ReportStyles styles = createStyles(workbook);

            createTitle(sheet, styles, "BÁO CÁO DOANH THU", 3);
            int rowIndex = 2;
            rowIndex = createMetaRow(sheet, rowIndex, styles, "Kỳ thống kê", safe(period));
            rowIndex = createMetaRow(sheet, rowIndex, styles, "Từ ngày", from != null ? from.format(DATE_FORMAT) : "");
            rowIndex = createMetaRow(sheet, rowIndex, styles, "Đến ngày", to != null ? to.format(DATE_FORMAT) : "");
            rowIndex = createMetaRow(sheet, rowIndex, styles, "Tổng doanh thu",
                    statistics != null ? statistics.getTotalRevenue() : 0, styles.money);
            rowIndex = createMetaRow(sheet, rowIndex, styles, "Số hóa đơn",
                    statistics != null ? statistics.getTotalInvoices() : 0, styles.integer);
            rowIndex = createMetaRow(sheet, rowIndex, styles, "Doanh thu trung bình",
                    statistics != null ? statistics.getAverageRevenue() : 0, styles.money);
            rowIndex++;

            Row header = sheet.createRow(rowIndex++);
            writeHeader(header, styles, "Kỳ", "Doanh thu", "Số hóa đơn");

            if (buckets != null) {
                for (RevenueChartData bucket : buckets) {
                    if (bucket == null) {
                        continue;
                    }
                    Row row = sheet.createRow(rowIndex++);
                    writeCell(row, 0, bucket.getLabel(), styles.text);
                    writeCell(row, 1, bucket.getRevenue(), styles.money);
                    writeCell(row, 2, bucket.getInvoiceCount(), styles.integer);
                }
            }

            autosize(sheet, 3);
            writeWorkbook(file, workbook);
        }
    }

    public static void exportCustomersExcel(File file, List<KhachHang> customers) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("KhachHang");
            ReportStyles styles = createStyles(workbook);

            createTitle(sheet, styles, "BÁO CÁO DANH SÁCH KHÁCH HÀNG", 8);
            int rowIndex = 2;
            rowIndex = createMetaRow(sheet, rowIndex, styles, "Thời gian xuất",
                    LocalDateTime.now().format(DATE_TIME_FORMAT));
            rowIndex = createMetaRow(sheet, rowIndex, styles, "Số dòng dữ liệu", customers != null ? customers.size() : 0,
                    styles.integer);
            rowIndex++;

            Row header = sheet.createRow(rowIndex++);
            writeHeader(header, styles,
                    "Mã khách hàng", "Tên khách hàng", "Giới tính", "Số điện thoại",
                    "Loại giấy tờ", "Số giấy tờ", "Địa chỉ", "Trạng thái");

            if (customers != null) {
                for (KhachHang customer : customers) {
                    if (customer == null) {
                        continue;
                    }
                    Row row = sheet.createRow(rowIndex++);
                    writeCell(row, 0, safe(customer.getMaKhachHang()), styles.text);
                    writeCell(row, 1, safe(customer.getTenKhachHang()), styles.text);
                    writeCell(row, 2, customer.isGioiTinh() ? "Nam" : "Nữ", styles.text);
                    writeCell(row, 3, safe(customer.getSoDienThoai()), styles.text);
                    writeCell(row, 4, safe(customer.getLoaiGiayTo()), styles.text);
                    writeCell(row, 5, safe(customer.getSoGiayTo()), styles.text);
                    writeCell(row, 6, safe(customer.getDiaChi()), styles.text);
                    writeCell(row, 7, customer.isDangHoatDong() ? "Đang hoạt động" : "Đã vô hiệu hóa", styles.text);
                }
            }

            autosize(sheet, 8);
            writeWorkbook(file, workbook);
        }
    }

    public static void exportRevenuePdf(File file, String period, LocalDate from, LocalDate to,
            RevenueStatistics statistics, List<RevenueChartData> buckets) throws Exception {
        try (PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf, PageSize.A4)) {
            document.setMargins(28, 28, 28, 28);
            PdfFont font = createPdfFont();
            addPdfTitle(document, font, "BÁO CÁO DOANH THU");
            addPdfMeta(document, font, "Kỳ thống kê", safe(period));
            addPdfMeta(document, font, "Từ ngày", from != null ? from.format(DATE_FORMAT) : "");
            addPdfMeta(document, font, "Đến ngày", to != null ? to.format(DATE_FORMAT) : "");
            addPdfMeta(document, font, "Tổng doanh thu",
                    formatMoney(statistics != null ? statistics.getTotalRevenue() : 0));
            addPdfMeta(document, font, "Số hóa đơn",
                    String.valueOf(statistics != null ? statistics.getTotalInvoices() : 0));
            addPdfMeta(document, font, "Doanh thu trung bình",
                    formatMoney(statistics != null ? statistics.getAverageRevenue() : 0));

            Table table = new Table(UnitValue.createPercentArray(new float[] { 38, 34, 28 })).useAllAvailableWidth();
            addPdfHeaderCells(table, font, "Kỳ", "Doanh thu", "Số hóa đơn");
            int rowIndex = 0;
            if (buckets != null) {
                for (RevenueChartData bucket : buckets) {
                    if (bucket == null) {
                        continue;
                    }
                    com.itextpdf.kernel.colors.Color rowColor = rowIndex++ % 2 == 0 ? ColorConstants.WHITE : PDF_ROW_ALT;
                    addPdfCell(table, font, bucket.getLabel(), TextAlignment.LEFT, rowColor);
                    addPdfCell(table, font, formatMoney(bucket.getRevenue()), TextAlignment.RIGHT, rowColor);
                    addPdfCell(table, font, String.valueOf(bucket.getInvoiceCount()), TextAlignment.CENTER, rowColor);
                }
            }
            document.add(table);
        }
    }

    public static void exportCustomersPdf(File file, List<KhachHang> customers) throws Exception {
        try (PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf, PageSize.A4.rotate())) {
            document.setMargins(28, 24, 28, 24);
            PdfFont font = createPdfFont();
            addPdfTitle(document, font, "BÁO CÁO DANH SÁCH KHÁCH HÀNG");
            addPdfMeta(document, font, "Thời gian xuất", LocalDateTime.now().format(DATE_TIME_FORMAT));
            addPdfMeta(document, font, "Số dòng dữ liệu", String.valueOf(customers != null ? customers.size() : 0));

            Table table = new Table(UnitValue.createPercentArray(new float[] { 12, 22, 9, 13, 12, 14, 25, 14 }))
                    .useAllAvailableWidth();
            addPdfHeaderCells(table, font,
                    "Mã KH", "Tên khách hàng", "Giới tính", "Số điện thoại",
                    "Loại giấy tờ", "Số giấy tờ", "Địa chỉ", "Trạng thái");
            int rowIndex = 0;
            if (customers != null) {
                for (KhachHang customer : customers) {
                    if (customer == null) {
                        continue;
                    }
                    com.itextpdf.kernel.colors.Color rowColor = rowIndex++ % 2 == 0 ? ColorConstants.WHITE : PDF_ROW_ALT;
                    addPdfCell(table, font, safe(customer.getMaKhachHang()), TextAlignment.LEFT, rowColor);
                    addPdfCell(table, font, safe(customer.getTenKhachHang()), TextAlignment.LEFT, rowColor);
                    addPdfCell(table, font, customer.isGioiTinh() ? "Nam" : "Nữ", TextAlignment.CENTER, rowColor);
                    addPdfCell(table, font, safe(customer.getSoDienThoai()), TextAlignment.LEFT, rowColor);
                    addPdfCell(table, font, safe(customer.getLoaiGiayTo()), TextAlignment.LEFT, rowColor);
                    addPdfCell(table, font, safe(customer.getSoGiayTo()), TextAlignment.LEFT, rowColor);
                    addPdfCell(table, font, safe(customer.getDiaChi()), TextAlignment.LEFT, rowColor);
                    addPdfCell(table, font, customer.isDangHoatDong() ? "Đang hoạt động" : "Đã vô hiệu hóa",
                            TextAlignment.LEFT, rowColor);
                }
            }
            document.add(table);
        }
    }

    private static ReportStyles createStyles(Workbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(COLOR_PRIMARY);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor((short) 1);

        Font metaFont = workbook.createFont();
        metaFont.setBold(true);

        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);
        title.setAlignment(HorizontalAlignment.CENTER);
        title.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(COLOR_PRIMARY);
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle metaLabel = workbook.createCellStyle();
        metaLabel.setFont(metaFont);

        CellStyle text = workbook.createCellStyle();
        text.setWrapText(true);
        text.setVerticalAlignment(VerticalAlignment.TOP);

        CellStyle money = workbook.createCellStyle();
        money.setDataFormat(dataFormat.getFormat("#,##0"));
        money.setAlignment(HorizontalAlignment.RIGHT);

        CellStyle integer = workbook.createCellStyle();
        integer.setDataFormat(dataFormat.getFormat("#,##0"));
        integer.setAlignment(HorizontalAlignment.RIGHT);

        return new ReportStyles(title, header, metaLabel, text, money, integer);
    }

    private static void createTitle(Sheet sheet, ReportStyles styles, String title, int lastColumn) {
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(24);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.max(0, lastColumn - 1)));
    }

    private static int createMetaRow(Sheet sheet, int rowIndex, ReportStyles styles, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        writeCell(row, 0, label, styles.metaLabel);
        writeCell(row, 1, value, styles.text);
        return rowIndex + 1;
    }

    private static int createMetaRow(Sheet sheet, int rowIndex, ReportStyles styles, String label, double value,
            CellStyle valueStyle) {
        Row row = sheet.createRow(rowIndex);
        writeCell(row, 0, label, styles.metaLabel);
        writeCell(row, 1, value, valueStyle);
        return rowIndex + 1;
    }

    private static void writeHeader(Row row, ReportStyles styles, String... columns) {
        for (int i = 0; i < columns.length; i++) {
            writeCell(row, i, columns[i], styles.header);
        }
    }

    private static void writeCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static void writeCell(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static void autosize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 800, 16000));
        }
    }

    private static void writeWorkbook(File file, Workbook workbook) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            Files.createDirectories(parent.toPath());
        }
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }
    }

    private static void addPdfTitle(Document document, PdfFont font, String title) {
        document.add(new Paragraph(title)
                .setFont(font)
                .setBold()
                .setFontSize(16)
                .setFontColor(PDF_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(12));
    }

    private static void addPdfMeta(Document document, PdfFont font, String label, String value) {
        document.add(new Paragraph(label + ": " + (value == null || value.isBlank() ? "--" : value))
                .setFont(font)
                .setFontSize(9)
                .setMargin(0)
                .setMarginBottom(3));
    }

    private static void addPdfHeaderCells(Table table, PdfFont font, String... titles) {
        for (String title : titles) {
            table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                    .setBackgroundColor(PDF_PRIMARY)
                    .setBorder(new SolidBorder(PDF_PRIMARY, 0.5f))
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(new Paragraph(title).setFont(font).setBold().setFontSize(8.5f).setFontColor(ColorConstants.WHITE)
                            .setMargin(0)));
        }
    }

    private static void addPdfCell(Table table, PdfFont font, String value, TextAlignment alignment,
            com.itextpdf.kernel.colors.Color backgroundColor) {
        table.addCell(new com.itextpdf.layout.element.Cell()
                .setBorder(new SolidBorder(PDF_BORDER, 0.5f))
                .setBackgroundColor(backgroundColor)
                .setPadding(5)
                .setTextAlignment(alignment)
                .add(new Paragraph(value == null || value.isBlank() ? "--" : value)
                        .setFont(font)
                        .setFontSize(8)
                        .setMargin(0)));
    }

    private static PdfFont createPdfFont() throws Exception {
        String windir = System.getenv("WINDIR");
        if (windir != null && !windir.isBlank()) {
            Path arialPath = Path.of(windir, "Fonts", "arial.ttf");
            if (Files.exists(arialPath)) {
                return PdfFontFactory.createFont(arialPath.toString(), "Identity-H",
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            }
        }
        return PdfFontFactory.createFont(StandardFonts.HELVETICA);
    }

    private static String ensureFileNameExtension(String fileName, String extension) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith("." + extension)) {
            return fileName;
        }
        return stripKnownExtension(fileName) + "." + extension;
    }

    private static File withExtension(File file, String extension) {
        String path = file.getAbsolutePath();
        if (path.toLowerCase(Locale.ROOT).endsWith("." + extension)) {
            return file;
        }
        return new File(stripKnownExtension(path) + "." + extension);
    }

    private static String stripKnownExtension(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xlsx") || lower.endsWith(".pdf")) {
            return value.substring(0, value.lastIndexOf('.'));
        }
        return value;
    }

    private static String formatMoney(double value) {
        return MONEY_FORMAT.format(value) + " VND";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record ReportStyles(CellStyle title, CellStyle header, CellStyle metaLabel,
            CellStyle text, CellStyle money, CellStyle integer) {
    }
}
