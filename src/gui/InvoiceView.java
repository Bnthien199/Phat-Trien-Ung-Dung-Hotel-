package gui;

import connectDB.ConnectDB;
import dao.ChiTietHoaDonDAO;
import dao.ChiTietPhieuDatDAO;
import dao.HoaDonDAO;
import dao.PhieuDatPhongDAO;
import entity.ChiTietHoaDon;
import entity.ChiTietPhieuDat;
import entity.HoaDon;
import entity.KhachHang;
import entity.PhieuDatPhong;

import com.itextpdf.io.font.constants.StandardFonts;
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
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.JOptionPane;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.effect.DropShadow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import util.PricingService;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import util.AppTheme;

public class InvoiceView {
  private static final String BG_LIGHT = AppTheme.BG_LIGHT;
  private static final String PRIMARY = AppTheme.PRIMARY;
  private static final String SUCCESS = AppTheme.SUCCESS;
  private static final String TEXT_DARK = AppTheme.TEXT;
  private static final String TEXT_SUB = AppTheme.MUTED;
  private static final String SURFACE = AppTheme.SURFACE;
  private static final String BORDER = AppTheme.BORDER;
  private static final String BORDER_LIGHT = AppTheme.BORDER_LIGHT;
  private static final String SECONDARY = AppTheme.SECONDARY;
  private static final String SECONDARY_HOVER = AppTheme.SECONDARY_HOVER;
  private static final String SECONDARY_TEXT = AppTheme.SECONDARY_TEXT;
  private static final String TABLE_CSS = buildTableCss();
  private static final DeviceRgb PDF_PRIMARY = new DeviceRgb(13, 148, 136);
  private static final DeviceRgb PDF_TEXT_DARK = new DeviceRgb(15, 23, 42);
  private static final DeviceRgb PDF_TEXT_SUB = new DeviceRgb(100, 116, 139);
  private static final DeviceRgb PDF_SUCCESS = new DeviceRgb(16, 185, 129);
  private static final DeviceRgb PDF_BORDER = new DeviceRgb(203, 213, 225);
  private static final DeviceRgb PDF_ROW_ALT = new DeviceRgb(248, 250, 252);
  private static final DeviceRgb PDF_SUMMARY_BG = new DeviceRgb(240, 253, 250);

  private static final String HOTEL_NAME = "Hotel";
  private static final String HOTEL_ADDRESS = "123 Nguyễn Văn A, Quận 1, TP. Hồ Chí Minh";
  private static final String HOTEL_PHONE = "028 3888 9999";
  private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final ObservableList<HoaDon> dataList = FXCollections.observableArrayList();
  private final ChiTietPhieuDatDAO chiTietPhieuDatDAO = new ChiTietPhieuDatDAO();
  private final ChiTietHoaDonDAO chiTietHoaDonDAO = new ChiTietHoaDonDAO();
  private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
  private final PhieuDatPhongDAO phieuDatPhongDAO = new PhieuDatPhongDAO();
  private final PricingService pricingService = new PricingService();

  private TableView<HoaDon> tableView;
  private TextField txtSearch;
  private DatePicker dpFrom;
  private DatePicker dpTo;
  private Label lblCount;
  private int invoiceLoadVersion;

  public Node createView() {
    if (System.nanoTime() >= 0) {
      return createModernView();
    }
    VBox root = new VBox(20);
    root.setPadding(new Insets(28, 30, 28, 30));
    root.setStyle("-fx-background-color: " + BG_LIGHT + ";");

    HBox header = new HBox(12);
    header.setAlignment(Pos.CENTER_LEFT);

    Label lblIcon = new Label("🧾");
    lblIcon.setFont(Font.font(26));

    VBox headerText = new VBox(2);
    Label lblTitle = new Label("QUẢN LÝ HÓA ĐƠN");
    lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
    lblTitle.setTextFill(Color.web(TEXT_DARK));

    Label lblSub = new Label("Tra cứu hóa đơn, xem chi tiết phòng đã nhận và xuất PDF chuyên nghiệp.");
    lblSub.setFont(Font.font("Segoe UI", 13));
    lblSub.setTextFill(Color.web(TEXT_SUB));

    headerText.getChildren().addAll(lblTitle, lblSub);
    header.getChildren().addAll(lblIcon, headerText);

    Label lblNote = new Label("Bạn có thể lọc theo ngày, tìm theo mã hóa đơn hoặc tên khách hàng.");
    lblNote.setFont(Font.font("Segoe UI", 13));
    lblNote.setTextFill(Color.web("#b45309"));
    lblNote.setStyle("-fx-background-color: #fef3c7; -fx-padding: 10 16; -fx-background-radius: 10;");

    txtSearch = new TextField();
    txtSearch.setPromptText("Tìm theo mã hóa đơn / tên khách hàng...");
    txtSearch.setStyle(
        "-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #d1d5db; -fx-padding: 0 12; -fx-font-size: 14;");
    txtSearch.textProperty().addListener((obs, oldVal, newVal) -> searchInvoice());

    dpFrom = createFilterDatePicker();
    dpTo = createFilterDatePicker();

    Button btnFilterDate = createStyledButton("Lọc", SUCCESS);
    btnFilterDate.setOnAction(e -> handleFilterDate());

    Button btnRefresh = createStyledButton("Làm mới", PRIMARY);
    btnRefresh.setOnAction(e -> handleRefresh());

    HBox toolbar = createFilterPanel(btnFilterDate, btnRefresh);

    tableView = new TableView<>(dataList);
    tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    VBox.setVgrow(tableView, Priority.ALWAYS);
    tableView.setPlaceholder(new Label("Chưa có hóa đơn nào."));
    tableView.setStyle(
        "-fx-background-color: white;" +
            "-fx-border-color: #e2e8f0;" +
            "-fx-border-radius: 4;" +
            "-fx-font-size: 13;");

    tableView.setRowFactory(tv -> {
      var row = new javafx.scene.control.TableRow<HoaDon>();
      row.setOnMouseClicked(event -> {
        if (event.getClickCount() == 2 && !row.isEmpty()) {
          handleViewDetail();
        }
      });
      return row;
    });

    TableColumn<HoaDon, String> colMa = textCol("Mã HĐ", HoaDon::getMaHoaDon, 90, Pos.CENTER);
    TableColumn<HoaDon, String> colPhieu = textCol("Mã phiếu đặt",
        hd -> hd.getPhieuDatPhong() != null ? hd.getPhieuDatPhong().getMaDatPhong() : "", 105, Pos.CENTER);
    TableColumn<HoaDon, String> colKhach = textCol("Khách hàng",
        hd -> hd.getPhieuDatPhong() != null && hd.getPhieuDatPhong().getKhachHang() != null
            ? hd.getPhieuDatPhong().getKhachHang().getTenKhachHang()
            : "",
        150, Pos.CENTER_LEFT);
    TableColumn<HoaDon, String> colNV = textCol("Nhân viên lập",
        hd -> hd.getNhanVien() != null
            ? (hd.getNhanVien().getTenNhanVien() != null && !hd.getNhanVien().getTenNhanVien().isBlank()
                ? hd.getNhanVien().getTenNhanVien()
                : hd.getNhanVien().getMaNhanVien())
            : "",
        140, Pos.CENTER_LEFT);
    TableColumn<HoaDon, String> colNgay = textCol("Ngày lập",
        hd -> hd.getNgayLap() != null ? hd.getNgayLap().format(DATE_TIME_FORMAT) : "", 135, Pos.CENTER);
    TableColumn<HoaDon, String> colTienPhong = moneyCol("Tiền phòng", HoaDon::getTongTienPhong, 120);
    TableColumn<HoaDon, String> colTienDV = moneyCol("Tiền dịch vụ", HoaDon::getTongTienDichVu, 120);
    TableColumn<HoaDon, String> colTienCoc = moneyCol("Tiền cọc", this::getDepositAmount, 120);
    TableColumn<HoaDon, String> colTong = moneyCol("Thành tiền", this::calculateInvoiceFinalAmount, 130);

    tableView.getColumns().addAll(colMa, colKhach, colPhieu, colNV, colNgay, colTienPhong, colTienDV, colTienCoc,
        colTong);

    HBox actionBar = new HBox(12);
    actionBar.setAlignment(Pos.CENTER_RIGHT);

    Button btnDetail = createStyledButton("Xem chi tiết", SUCCESS);
    btnDetail.setOnAction(e -> handleViewDetail());

    Button btnPrint = createStyledButton("Xuất PDF", SUCCESS);
    btnPrint.setOnAction(e -> handlePrint());

    Button btnExport = createStyledButton("Xuất Excel", PRIMARY);
    btnExport.setOnAction(e -> exportExcel());

    actionBar.getChildren().addAll(btnDetail, btnPrint, btnExport);

    root.getChildren().addAll(header, lblNote, toolbar, tableView, actionBar);
    loadData();
    return root;
  }

  private HBox createFilterPanel(Button btnFilterDate, Button btnRefresh) {
    setFixedRegionSize(txtSearch, 300, 40);
    setFixedRegionSize(dpFrom, 140, 40);
    setFixedRegionSize(dpTo, 140, 40);
    setFixedRegionSize(btnFilterDate, 76, 40);
    setFixedRegionSize(btnRefresh, 104, 40);

    VBox searchGroup = createFilterGroup("Tìm kiếm", txtSearch);
    VBox fromGroup = createFilterGroup("Từ ngày", dpFrom);
    VBox toGroup = createFilterGroup("Đến ngày", dpTo);
    VBox filterButtonGroup = createFilterGroup(" ", btnFilterDate);
    VBox refreshButtonGroup = createFilterGroup(" ", btnRefresh);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox filterPanel = new HBox(14);
    filterPanel.setAlignment(Pos.TOP_LEFT);
    filterPanel.setFillHeight(false);
    filterPanel.getChildren().addAll(searchGroup, fromGroup, toGroup, filterButtonGroup, spacer, refreshButtonGroup);
    return filterPanel;
  }

  private VBox createFilterGroup(String labelText, Region input) {
    Label label = new Label(labelText);
    label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
    label.setTextFill(Color.web(TEXT_SUB));
    label.setMinHeight(16);
    label.setPrefHeight(16);

    VBox group = new VBox(6, label, input);
    group.setAlignment(Pos.TOP_LEFT);
    group.setFillWidth(false);
    group.setMinWidth(input.getPrefWidth());
    group.setPrefWidth(input.getPrefWidth());
    return group;
  }

  private DatePicker createFilterDatePicker() {
    DatePicker picker = new DatePicker();
    picker.setStyle("-fx-background-radius:10;-fx-border-radius:10;-fx-border-color:#d1d5db;-fx-font-size:14;");
    return picker;
  }

  private void setFixedRegionSize(Region region, double width, double height) {
    region.setMinSize(width, height);
    region.setPrefSize(width, height);
    region.setMaxSize(width, height);
  }

  private Node createModernView() {
    Button btnDetail = createPrimaryButton("Xem chi tiết", PRIMARY);
    btnDetail.setOnAction(e -> handleViewDetail());

    Button btnPrint = createPrimaryButton("Xuất PDF", PRIMARY);
    btnPrint.setOnAction(e -> handlePrint());

    Button btnExport = createPrimaryButton("Xuất Excel", PRIMARY);
    btnExport.setOnAction(e -> exportExcel());

    VBox root = new VBox(0);
    root.setStyle("-fx-background-color: " + BG_LIGHT + ";");

    txtSearch = new TextField();
    txtSearch.setPromptText("Tìm theo mã hóa đơn hoặc khách hàng...");
    txtSearch.textProperty().addListener((obs, oldVal, newVal) -> searchInvoice());

    dpFrom = createModernDatePicker("Từ ngày");
    dpTo = createModernDatePicker("Đến ngày");

    Button btnFilterDate = createPrimaryButton("Lọc", PRIMARY);
    btnFilterDate.setOnAction(e -> handleFilterDate());

    Button btnRefresh = createSecondaryButton("Làm mới");
    btnRefresh.setOnAction(e -> handleRefresh());

    tableView = new TableView<>(dataList);
    configureModernMainTable();

    TableColumn<HoaDon, String> colMa = textCol("Mã HĐ", HoaDon::getMaHoaDon, 90, Pos.CENTER);
    TableColumn<HoaDon, String> colPhieu = textCol("Mã phiếu đặt",
        hd -> hd.getPhieuDatPhong() != null ? hd.getPhieuDatPhong().getMaDatPhong() : "", 108, Pos.CENTER);
    TableColumn<HoaDon, String> colKhach = textCol("Khách hàng",
        hd -> hd.getPhieuDatPhong() != null && hd.getPhieuDatPhong().getKhachHang() != null
            ? hd.getPhieuDatPhong().getKhachHang().getTenKhachHang()
            : "",
        168, Pos.CENTER_LEFT);
    TableColumn<HoaDon, String> colNV = textCol("Nhân viên lập",
        hd -> hd.getNhanVien() != null
            ? (hd.getNhanVien().getTenNhanVien() != null && !hd.getNhanVien().getTenNhanVien().isBlank()
                ? hd.getNhanVien().getTenNhanVien()
                : hd.getNhanVien().getMaNhanVien())
            : "",
        150, Pos.CENTER_LEFT);
    TableColumn<HoaDon, String> colNgay = textCol("Ngày lập",
        hd -> hd.getNgayLap() != null ? hd.getNgayLap().format(DATE_TIME_FORMAT) : "", 138, Pos.CENTER);
    TableColumn<HoaDon, String> colTienPhong = moneyCol("Tiền phòng", HoaDon::getTongTienPhong, 128);
    TableColumn<HoaDon, String> colTienDV = moneyCol("Tiền dịch vụ", HoaDon::getTongTienDichVu, 128);
    TableColumn<HoaDon, String> colTienCoc = moneyCol("Tiền cọc", this::getDepositAmount, 128);
    TableColumn<HoaDon, String> colTong = totalMoneyCol("Thành tiền", this::calculateInvoiceFinalAmount, 136);

    tableView.getColumns().setAll(
        colMa, colKhach, colPhieu, colNV, colNgay, colTienPhong, colTienDV, colTienCoc, colTong);

    root.getChildren().addAll(
        buildModernPageHeader(btnDetail, btnPrint, btnExport),
        buildModernToolbar(btnFilterDate, btnRefresh),
        buildModernTableCard());
    loadData();
    return root;
  }

  private HBox buildModernPageHeader(Button btnDetail, Button btnPrint, Button btnExport) {
    Label icon = new Label("🧾");
    icon.setFont(Font.font(18));
    icon.setTextFill(Color.web(PRIMARY));

    StackPane iconWrap = new StackPane(icon);
    iconWrap.setPrefSize(44, 44);
    iconWrap.setMinSize(44, 44);
    iconWrap.setMaxSize(44, 44);
    iconWrap.setStyle("-fx-background-color: #ccfbf1; -fx-background-radius: 22;");

    Label lblTitle = new Label("Quản lý hóa đơn");
    lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
    lblTitle.setTextFill(Color.web(TEXT_DARK));

    Label lblSub = new Label("Tra cứu, lọc và xuất hóa đơn theo cùng một chuẩn hiển thị với quản lý khách hàng.");
    lblSub.setFont(Font.font("Segoe UI", 13));
    lblSub.setTextFill(Color.web(TEXT_SUB));

    VBox titleBox = new VBox(2, lblTitle, lblSub);

    lblCount = new Label("— hóa đơn");
    lblCount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
    lblCount.setTextFill(Color.web(PRIMARY));
    lblCount.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 999; -fx-padding: 4 14;");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox header = new HBox(14, iconWrap, titleBox, spacer, lblCount, btnDetail, btnPrint, btnExport);
    header.setAlignment(Pos.CENTER_LEFT);
    header.setPadding(new Insets(28, 28, 16, 28));
    return header;
  }

  private HBox buildModernToolbar(Button btnFilterDate, Button btnRefresh) {
    Region searchWrap = createModernSearchField();
    setFixedRegionSize(searchWrap, 360, 40);
    setFixedRegionSize(dpFrom, 150, 40);
    setFixedRegionSize(dpTo, 150, 40);
    setFixedRegionSize(btnFilterDate, 84, 38);
    setFixedRegionSize(btnRefresh, 108, 38);

    HBox toolbar = new HBox(10, searchWrap, dpFrom, dpTo, btnFilterDate, btnRefresh);
    toolbar.setAlignment(Pos.CENTER_LEFT);
    toolbar.setPadding(new Insets(0, 28, 16, 28));
    return toolbar;
  }

  private Region createModernSearchField() {
    Label searchIcon = new Label("🔍");
    searchIcon.setFont(Font.font(14));
    searchIcon.setPadding(new Insets(0, 0, 0, 12));
    searchIcon.setTextFill(Color.web(TEXT_SUB));

    txtSearch.setStyle(
        "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;" +
            "-fx-font-size: 13;" +
            "-fx-text-fill: " + TEXT_DARK + ";" +
            "-fx-prompt-text-fill: #94a3b8;");
    HBox.setHgrow(txtSearch, Priority.ALWAYS);

    HBox searchWrap = new HBox(6, searchIcon, txtSearch);
    searchWrap.setAlignment(Pos.CENTER_LEFT);
    searchWrap.setPrefHeight(40);
    searchWrap.setMaxWidth(360);
    String normal = inputWrapStyle(BORDER);
    String focused = inputWrapStyle(PRIMARY);
    searchWrap.setStyle(normal);
    txtSearch.focusedProperty().addListener((obs, old, isFocused) -> searchWrap.setStyle(isFocused ? focused : normal));
    return searchWrap;
  }

  private DatePicker createModernDatePicker(String promptText) {
    DatePicker picker = new DatePicker();
    picker.setPromptText(promptText);
    picker.setPrefHeight(40);
    picker.setStyle(
        "-fx-background-color: white;" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-font-size: 13;" +
            "-fx-padding: 0 8;" +
            "-fx-text-fill: " + TEXT_DARK + ";");
    picker.getEditor().setPromptText(promptText);
    picker.getEditor().setStyle(
        "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-font-size: 13;" +
            "-fx-text-fill: " + TEXT_DARK + ";" +
            "-fx-prompt-text-fill: #94a3b8;");
    return picker;
  }

  private VBox buildModernTableCard() {
    VBox.setVgrow(tableView, Priority.ALWAYS);

    Label title = new Label("Danh sách hóa đơn");
    title.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
    title.setTextFill(Color.web(TEXT_DARK));

    Label hint = new Label("Các cột tiền được căn phải, tăng khoảng thở và nhấn nhẹ ở cột thành tiền.");
    hint.setFont(Font.font("Segoe UI", 12));
    hint.setTextFill(Color.web(TEXT_SUB));

    VBox header = new VBox(3, title, hint);

    VBox card = new VBox(16, header, tableView);
    card.setPadding(new Insets(18));
    card.setStyle(
        "-fx-background-color: " + SURFACE + ";" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 16;");
    card.setEffect(new DropShadow(12, Color.rgb(15, 23, 42, 0.07)));
    VBox.setVgrow(card, Priority.ALWAYS);

    VBox shell = new VBox(card);
    shell.setPadding(new Insets(0, 28, 28, 28));
    VBox.setVgrow(shell, Priority.ALWAYS);
    return shell;
  }

  private void configureModernMainTable() {
    tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    tableView.setPlaceholder(new Label("Chưa có hóa đơn nào."));
    tableView.getStylesheets().add(TABLE_CSS);
    tableView.getStyleClass().add("module-table");
    VBox.setVgrow(tableView, Priority.ALWAYS);

    tableView.setRowFactory(tv -> {
      var row = new javafx.scene.control.TableRow<HoaDon>();
      row.setOnMouseEntered(e -> {
        if (!row.isEmpty() && !row.isSelected()) {
          row.setStyle("-fx-background-color: #f8fafc;");
        }
      });
      row.setOnMouseExited(e -> {
        if (!row.isEmpty() && !row.isSelected()) {
          row.setStyle("");
        }
      });
      row.selectedProperty()
          .addListener((obs, old, selected) -> row.setStyle(selected ? "-fx-background-color: #ecfeff;" : ""));
      row.setOnMouseClicked(event -> {
        if (event.getClickCount() == 2 && !row.isEmpty()) {
          handleViewDetail();
        }
      });
      return row;
    });
  }

  private String inputWrapStyle(String borderColor) {
    return "-fx-background-color: white;" +
        "-fx-border-color: " + borderColor + ";" +
        "-fx-border-radius: 10;" +
        "-fx-background-radius: 10;" +
        "-fx-border-width: 1.5;";
  }

  private Button createPrimaryButton(String text, String color) {
    Button button = new Button(text);
    button.setPrefHeight(38);
    button.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
    button.setPadding(new Insets(0, 18, 0, 18));
    button.setTextFill(Color.WHITE);

    String base = "-fx-background-color:" + color + ";" +
        "-fx-text-fill:white;" +
        "-fx-background-radius:10;" +
        "-fx-cursor:hand;" +
        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),6,0,0,2);";
    String hover = "-fx-background-color:derive(" + color + ",-12%);" +
        "-fx-text-fill:white;" +
        "-fx-background-radius:10;" +
        "-fx-cursor:hand;" +
        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),10,0,0,3);";

    button.setStyle(base);
    button.setOnMouseEntered(e -> button.setStyle(hover));
    button.setOnMouseExited(e -> button.setStyle(base));
    return button;
  }

  private Button createSecondaryButton(String text) {
    Button button = new Button(text);
    button.setPrefHeight(38);
    button.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
    button.setPadding(new Insets(0, 18, 0, 18));
    button.setTextFill(Color.web(SECONDARY_TEXT));

    String base = "-fx-background-color:" + SECONDARY + ";" +
        "-fx-text-fill:" + SECONDARY_TEXT + ";" +
        "-fx-background-radius:10;" +
        "-fx-border-color:" + BORDER + ";" +
        "-fx-border-radius:10;" +
        "-fx-cursor:hand;";
    String hover = "-fx-background-color:" + SECONDARY_HOVER + ";" +
        "-fx-text-fill:" + SECONDARY_TEXT + ";" +
        "-fx-background-radius:10;" +
        "-fx-border-color:" + BORDER + ";" +
        "-fx-border-radius:10;" +
        "-fx-cursor:hand;";

    button.setStyle(base);
    button.setOnMouseEntered(e -> button.setStyle(hover));
    button.setOnMouseExited(e -> button.setStyle(base));
    return button;
  }

  public void loadData() {
    loadInvoicesAsync("", null, null, false);
  }

  private void searchInvoice() {
    String kw = txtSearch.getText().trim();
    LocalDate from = dpFrom.getValue();
    LocalDate to = dpTo.getValue();
    loadInvoicesAsync(kw, from, to, true);
  }

  private void handleRefresh() {
    LocalDate today = LocalDate.now();
    txtSearch.clear();
    dpFrom.setValue(today);
    dpTo.setValue(today);
    searchInvoice();
  }

  private void loadInvoicesAsync(String keyword, LocalDate from, LocalDate to, boolean useFilter) {
    int requestVersion = ++invoiceLoadVersion;
    setTableLoading(true);

    Task<List<HoaDon>> task = new Task<>() {
      @Override
      protected List<HoaDon> call() {
        if (ConnectDB.getConnection() == null) {
          return new ArrayList<>();
        }
        return useFilter ? hoaDonDAO.filterHoaDon(keyword, from, to) : hoaDonDAO.getAllHoaDon();
      }
    };

    task.setOnSucceeded(e -> {
      if (requestVersion != invoiceLoadVersion) {
        return;
      }
      List<HoaDon> list = task.getValue();
      dataList.setAll(list != null ? list : new ArrayList<>());
      if (lblCount != null) {
        lblCount.setText(dataList.size() + " hóa đơn");
      }
      setTableLoading(false);
    });
    task.setOnFailed(e -> {
      if (requestVersion != invoiceLoadVersion) {
        return;
      }
      Throwable ex = task.getException();
      if (ex != null)
        ex.printStackTrace();
      dataList.clear();
      if (lblCount != null) {
        lblCount.setText("0 hóa đơn");
      }
      setTableLoading(false);
      showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải dữ liệu hóa đơn: " + (ex != null ? ex.getMessage() : ""));
    });

    Thread loader = new Thread(task, "invoice-load");
    loader.setDaemon(true);
    loader.start();
  }

  private void setTableLoading(boolean loading) {
    if (tableView != null) {
      tableView.setPlaceholder(new Label(loading
          ? "\u0110ang t\u1ea3i d\u1eef li\u1ec7u..."
          : "Ch\u01b0a c\u00f3 h\u00f3a \u0111\u01a1n n\u00e0o."));
    }
    if (loading) {
      dataList.clear();
    }
  }

  private void handleFilterDate() {
    if (dpFrom.getValue() == null && dpTo.getValue() == null) {
      showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn Từ ngày hoặc Đến ngày.");
      return;
    }
    searchInvoice();
  }

  private void handleViewDetail() {
    HoaDon selected = tableView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một hóa đơn để xem chi tiết.");
      return;
    }
    openInvoiceDetailDialog(selected);
  }

  private void openInvoiceDetailDialog(HoaDon hd) {
    Stage detailStage = new Stage();
    detailStage.initModality(Modality.APPLICATION_MODAL);
    detailStage.setTitle("Chi tiết hóa đơn - " + hd.getMaHoaDon());

    InvoiceData invoiceData = buildInvoiceData(hd);

    VBox root = new VBox(14);
    root.setPadding(new Insets(20));
    root.setStyle("-fx-background-color: #f8fafc;");

    VBox previewContent = new VBox(createInvoicePaper(invoiceData, false));
    previewContent.setAlignment(Pos.TOP_CENTER);
    previewContent.setPadding(new Insets(10, 10, 18, 10));

    ScrollPane scrollPane = new ScrollPane(previewContent);
    scrollPane.setFitToWidth(true);
    scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
    VBox.setVgrow(scrollPane, Priority.ALWAYS);

    Button btnClose = createStyledButton("Đóng", "#94a3b8");
    btnClose.setOnAction(e -> detailStage.close());
    HBox actionBar = new HBox(btnClose);
    actionBar.setAlignment(Pos.CENTER_RIGHT);

    root.getChildren().addAll(scrollPane, actionBar);

    Scene scene = new Scene(root, 980, 820);
    addInvoiceStylesheet(scene);
    detailStage.setScene(scene);
    detailStage.setMinWidth(760);
    detailStage.setMinHeight(640);

    util.ModalUtils.setupModalBehavior(detailStage, true, false, null);
    detailStage.show();
  }

  private VBox createInvoicePaper(InvoiceData data, boolean showFooterSignature) {
    VBox paper = new VBox(22);
    paper.setMaxWidth(900);
    paper.setPadding(new Insets(30, 34, 28, 34));
    paper.getStyleClass().add("invoice-paper");
    paper.setStyle(
        "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-radius: 10;");
    paper.setEffect(new DropShadow(18, Color.rgb(15, 23, 42, 0.12)));
    paper.getChildren().addAll(
        createInvoiceHeader(data),
        createInvoiceInfoSection(data),
        createRoomDetailSection(data));

    if (!data.serviceLines.isEmpty()) {
      paper.getChildren().add(createServiceDetailSection(data));
    }

    paper.getChildren().add(createInvoiceSummarySection(data));
    if (showFooterSignature) {
      paper.getChildren().add(createInvoiceFooter());
    }
    return paper;
  }

  private Node createInvoiceHeader(InvoiceData data) {
    HBox header = new HBox(18);
    header.setAlignment(Pos.CENTER_LEFT);
    header.setPadding(new Insets(0, 0, 18, 0));
    header.setStyle("-fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1 0;");

    Label logo = new Label("N06");
    logo.setMinSize(62, 62);
    logo.setAlignment(Pos.CENTER);
    logo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
    logo.setTextFill(Color.WHITE);
    logo.setStyle("-fx-background-color: " + PRIMARY + "; -fx-background-radius: 8; -fx-text-fill: white;");

    VBox hotelInfo = new VBox(4);
    Label hotelName = new Label(HOTEL_NAME);
    hotelName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
    hotelName.setTextFill(Color.web(TEXT_DARK));
    hotelName.setStyle("-fx-text-fill: #222222;");
    Label address = new Label(HOTEL_ADDRESS);
    address.setFont(Font.font("Segoe UI", 12));
    address.setTextFill(Color.web(TEXT_SUB));
    address.setStyle("-fx-text-fill: #444444;");
    address.setWrapText(true);
    Label hotline = new Label("Hotline: " + HOTEL_PHONE);
    hotline.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
    hotline.setTextFill(Color.web(TEXT_SUB));
    hotline.setStyle("-fx-text-fill: #444444;");
    hotelInfo.getChildren().addAll(hotelName, address, hotline);
    HBox.setHgrow(hotelInfo, Priority.ALWAYS);

    VBox titleBox = new VBox(5);
    titleBox.setAlignment(Pos.CENTER_RIGHT);
    Label title = new Label("HÓA ĐƠN THANH TOÁN");
    title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 25));
    title.setTextFill(Color.web(PRIMARY));
    title.setStyle("-fx-text-fill: " + PRIMARY + ";");
    Label invoiceNo = new Label("Mã hóa đơn: " + data.invoiceId);
    invoiceNo.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
    invoiceNo.setTextFill(Color.web(TEXT_DARK));
    invoiceNo.setStyle("-fx-text-fill: #222222;");
    Label invoiceDate = new Label(data.invoiceDate);
    invoiceDate.setFont(Font.font("Segoe UI", 12));
    invoiceDate.setTextFill(Color.web(TEXT_SUB));
    invoiceDate.setStyle("-fx-text-fill: #444444;");
    titleBox.getChildren().addAll(title, invoiceNo, invoiceDate);

    header.getChildren().addAll(logo, hotelInfo, titleBox);
    return header;
  }

  private Node createInvoiceInfoSection(InvoiceData data) {
    GridPane infoGrid = new GridPane();
    infoGrid.setHgap(22);
    infoGrid.setVgap(14);
    infoGrid.setPadding(new Insets(18));
    infoGrid.setStyle(
        "-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

    infoGrid.add(createInfoItem("Mã hóa đơn", data.invoiceId), 0, 0);
    infoGrid.add(createInfoItem("Khách hàng", data.customerName), 1, 0);
    infoGrid.add(createInfoItem("Số điện thoại", data.customerPhone), 0, 1);
    infoGrid.add(createInfoItem("Ngày lập", data.invoiceDate), 1, 1);
    infoGrid.add(createInfoItem("Nhân viên lập phiếu", data.staffName), 0, 2);
    infoGrid.add(createInfoItem("Mã phiếu đặt", data.bookingId), 1, 2);
    return infoGrid;
  }

  private VBox createInfoItem(String labelText, String valueText) {
    VBox item = new VBox(3);
    Label label = new Label(labelText);
    label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
    label.setTextFill(Color.web(TEXT_SUB));
    label.setStyle("-fx-text-fill: #444444;");
    Label value = new Label(valueText == null || valueText.isBlank() ? "--" : valueText);
    value.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
    value.setTextFill(Color.web(TEXT_DARK));
    value.setStyle("-fx-text-fill: #222222;");
    value.setWrapText(true);
    item.getChildren().addAll(label, value);
    return item;
  }

  private Node createRoomDetailSection(InvoiceData data) {
    VBox section = new VBox(10);
    section.getChildren().add(sectionTitle("Chi tiết phòng"));

    TableView<InvoiceRoomLine> roomTable = new TableView<>();
    roomTable.getStyleClass().add("invoice-table");
    roomTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    roomTable.setItems(FXCollections.observableArrayList(data.roomLines));
    roomTable.setPlaceholder(new Label("Không có dữ liệu phòng."));
    roomTable.getColumns().addAll(
        roomTextCol("Phòng", InvoiceRoomLine::roomCode, 80, Pos.CENTER),
        roomTextCol("Loại phòng", InvoiceRoomLine::roomType, 135, Pos.CENTER_LEFT),
        roomTextCol("Thời gian lưu trú", InvoiceRoomLine::stayPeriod, 210, Pos.CENTER),
        roomMoneyCol("Đơn giá", InvoiceRoomLine::price, 120),
        roomTextCol("Số đêm", line -> String.valueOf(line.nights()), 80, Pos.CENTER),
        roomMoneyCol("Thành tiền", InvoiceRoomLine::total, 130));
    sizeInvoiceTable(roomTable, data.roomLines.size(), 300);

    section.getChildren().add(roomTable);
    return section;
  }

  private Node createServiceDetailSection(InvoiceData data) {
    VBox section = new VBox(10);
    section.getChildren().add(sectionTitle("Dịch vụ sử dụng"));

    TableView<InvoiceServiceLine> serviceTable = new TableView<>();
    serviceTable.getStyleClass().add("invoice-table");
    serviceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    serviceTable.setItems(FXCollections.observableArrayList(data.serviceLines));
    serviceTable.setPlaceholder(new Label("Không có dịch vụ sử dụng."));
    serviceTable.getColumns().addAll(
        serviceTextCol("Tên dịch vụ", InvoiceServiceLine::serviceName, 260, Pos.CENTER_LEFT),
        serviceTextCol("Số lượng", line -> String.valueOf(line.quantity()), 90, Pos.CENTER),
        serviceMoneyCol("Đơn giá", InvoiceServiceLine::unitPrice, 130),
        serviceMoneyCol("Thành tiền", InvoiceServiceLine::total, 140));
    sizeInvoiceTable(serviceTable, data.serviceLines.size(), 240);

    section.getChildren().add(serviceTable);
    return section;
  }

  private Label sectionTitle(String text) {
    Label title = new Label(text);
    title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
    title.setTextFill(Color.web(TEXT_DARK));
    title.setStyle("-fx-text-fill: #222222;");
    return title;
  }

  private void sizeInvoiceTable(TableView<?> table, int rowCount, double maxHeight) {
    table.setFixedCellSize(40);
    double preferredHeight = Math.min(maxHeight, 44 + Math.max(1, rowCount) * 40);
    table.setMinHeight(preferredHeight);
    table.setPrefHeight(preferredHeight);
    table.setMaxHeight(preferredHeight);
  }

  private Node createInvoiceSummarySection(InvoiceData data) {
    VBox summary = new VBox(11);
    summary.setMaxWidth(420);
    summary.setPadding(new Insets(18));
    summary.setStyle(
        "-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #cbd5e1; -fx-border-radius: 8;");
    summary.getChildren().addAll(
        summaryRow("Tổng tiền phòng", formatCurrency(data.roomTotal), false, false),
        summaryRow("Tổng tiền dịch vụ", formatCurrency(data.serviceTotal), false, false),
        summaryRow("Tiền đã đặt cọc", formatCreditCurrency(data.depositAmount), true, false),
        summaryRow("THÀNH TIỀN", formatCurrency(data.finalAmount), false, true));

    HBox wrapper = new HBox(summary);
    wrapper.setAlignment(Pos.CENTER_RIGHT);
    return wrapper;
  }

  private Node summaryRow(String labelText, String valueText, boolean credit, boolean total) {
    Label label = new Label(labelText);
    label.setFont(Font.font("Segoe UI", total ? FontWeight.BOLD : FontWeight.NORMAL, total ? 16 : 13));
    label.setTextFill(Color.web(total ? TEXT_DARK : TEXT_SUB));
    label.setStyle("-fx-text-fill: " + (total ? "#222222" : "#444444") + ";");

    Label value = new Label(valueText);
    value.setFont(Font.font("Segoe UI", FontWeight.BOLD, total ? 20 : 14));
    value.setTextFill(Color.web(credit ? SUCCESS : total ? PRIMARY : TEXT_DARK));
    String colorStr = credit ? SUCCESS : total ? PRIMARY : "#222222";
    value.setStyle("-fx-text-fill: " + colorStr + ";");

    HBox row = new HBox(14, label, new Region(), value);
    row.setAlignment(Pos.CENTER);
    HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
    if (total) {
      row.setPadding(new Insets(14, 0, 0, 0));
      row.setStyle("-fx-border-color: #cbd5e1 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
    }
    return row;
  }

  private Node createInvoiceFooter() {
    VBox footer = new VBox(22);
    footer.setPadding(new Insets(14, 0, 0, 0));
    footer.setStyle("-fx-border-color: #e2e8f0 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

    Label thanks = new Label("Cảm ơn quý khách đã sử dụng dịch vụ!");
    thanks.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
    thanks.setTextFill(Color.web(PRIMARY));
    thanks.setStyle("-fx-text-fill: " + PRIMARY + ";");
    thanks.setAlignment(Pos.CENTER);
    thanks.setMaxWidth(Double.MAX_VALUE);

    HBox signatures = new HBox(70);
    signatures.setAlignment(Pos.CENTER);
    signatures.getChildren().addAll(signatureBox("Khách hàng"), signatureBox("Lễ tân"));

    footer.getChildren().addAll(thanks, signatures);
    return footer;
  }

  private VBox signatureBox(String title) {
    VBox box = new VBox(34);
    box.setAlignment(Pos.CENTER);
    Label label = new Label(title);
    label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
    label.setTextFill(Color.web(TEXT_DARK));
    label.setStyle("-fx-text-fill: #222222;");
    Label hint = new Label("(Ký và ghi rõ họ tên)");
    hint.setFont(Font.font("Segoe UI", 11));
    hint.setTextFill(Color.web(TEXT_SUB));
    hint.setStyle("-fx-text-fill: #444444;");
    box.getChildren().addAll(label, hint);
    return box;
  }

  private void addInvoiceStylesheet(Scene scene) {
    if (scene == null) {
      return;
    }
    var stylesheet = getClass().getResource("invoice-view.css");
    if (stylesheet != null) {
      scene.getStylesheets().add(stylesheet.toExternalForm());
      return;
    }
    File stylesheetFile = new File("src/gui/invoice-view.css");
    if (stylesheetFile.isFile()) {
      scene.getStylesheets().add(stylesheetFile.toURI().toString());
    }
  }

  private void handlePrint() {
    HoaDon selected = tableView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một hóa đơn để xuất PDF.");
      return;
    }
    exportPdfInvoice(selected);
  }

  public void exportInvoiceForBooking(String bookingId) {
    if (bookingId == null || bookingId.isBlank()) {
      showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Không tìm thấy mã phiếu đặt phòng để xuất hóa đơn.");
      return;
    }
    HoaDon invoice = hoaDonDAO.getHoaDonByMaPhieu(bookingId.trim());
    if (invoice == null) {
      showAlert(Alert.AlertType.WARNING, "Chưa có hóa đơn",
          "Phiếu " + bookingId + " chưa có hóa đơn để xuất. Vui lòng kiểm tra lại dữ liệu thanh toán.");
      return;
    }
    exportPdfInvoice(invoice);
  }
  private File chooseInvoicePdfSavePath(Component parent, String maHoaDon) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Chọn nơi lưu hóa đơn PDF");
    fileChooser.setApproveButtonText("Lưu PDF");
    fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setAcceptAllFileFilterUsed(false);

    FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF files (*.pdf)", "pdf");
    fileChooser.setFileFilter(pdfFilter);
    fileChooser.setCurrentDirectory(getDefaultInvoiceDirectory());
    fileChooser.setSelectedFile(new File(buildDefaultInvoicePdfFileName(maHoaDon)));

    while (true) {
      int result = fileChooser.showSaveDialog(parent);

      if (result != JFileChooser.APPROVE_OPTION) {
        return null;
      }

      File selectedFile = fileChooser.getSelectedFile();
      if (selectedFile == null) {
        return null;
      }

      selectedFile = ensurePdfExtension(selectedFile);

      if (selectedFile.exists()) {
        int overwrite = JOptionPane.showConfirmDialog(
            parent,
            "File đã tồn tại. Bạn có muốn ghi đè không?",
            "Xác nhận ghi đè",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (overwrite == JOptionPane.YES_OPTION) {
          return selectedFile;
        }

        if (overwrite == JOptionPane.NO_OPTION) {
          continue;
        }

        return null;
      }

      return selectedFile;
    }
  }

  private File ensurePdfExtension(File file) {
    String path = file.getAbsolutePath();
    if (!path.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
      return new File(path + ".pdf");
    }
    return file;
  }

  private File getDefaultInvoiceDirectory() {
    File defaultDirectory = FileSystemView.getFileSystemView().getDefaultDirectory();
    if (defaultDirectory != null && defaultDirectory.isDirectory()) {
      return defaultDirectory;
    }

    String userHome = System.getProperty("user.home");
    if (userHome != null && !userHome.trim().isEmpty()) {
      File documents = new File(userHome, "Documents");
      if (documents.isDirectory()) {
        return documents;
      }
      return new File(userHome);
    }

    return new File(".");
  }

  private String buildDefaultInvoicePdfFileName(String maHoaDon) {
    String safeMaHoaDon = maHoaDon == null ? "" : maHoaDon.trim();
    if (safeMaHoaDon.isEmpty()) {
      return "HoaDon.pdf";
    }
    return "HoaDon_" + safeMaHoaDon + ".pdf";
  }

  private void exportPdfInvoice(HoaDon hd) {
    File file = chooseInvoicePdfSavePath(null, hd != null ? hd.getMaHoaDon() : null);
    if (file == null) {
      return;
    }

    String path = file.getAbsolutePath();
    InvoiceData invoiceData = buildInvoiceData(hd);
    try {
      PdfWriter writer = new PdfWriter(path);
      PdfDocument pdf = new PdfDocument(writer);
      Document doc = new Document(pdf, PageSize.A4);
      doc.setMargins(28, 28, 28, 28);

      PdfFont font = createPdfFont();
      addPdfHeader(doc, font, invoiceData);
      addPdfInvoiceInfo(doc, font, invoiceData);
      addPdfRoomTable(doc, font, invoiceData);
      if (!invoiceData.serviceLines.isEmpty()) {
        addPdfServiceTable(doc, font, invoiceData);
      }
      addPdfSummary(doc, font, invoiceData);
      addPdfFooter(doc, font);

      doc.close();
      JOptionPane.showMessageDialog(
          null,
          "Xuất hóa đơn PDF thành công!\nĐã lưu tại: " + path,
          "Xuất PDF",
          JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(
          null,
          "Không thể xuất hóa đơn PDF. Vui lòng thử lại.",
          "Lỗi xuất PDF",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private PdfFont createPdfFont() throws IOException {
    try {
      return PdfFontFactory.createFont("C:/Windows/Fonts/arial.ttf", "Identity-H",
          PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
    } catch (Exception e) {
      try {
        return PdfFontFactory.createFont("arial.ttf", "Identity-H",
            PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
      } catch (Exception e2) {
        return PdfFontFactory.createFont(StandardFonts.HELVETICA);
      }
    }
  }

  private void addPdfHeader(Document doc, PdfFont font, InvoiceData data) {
    Table header = new Table(UnitValue.createPercentArray(new float[] { 58, 42 })).useAllAvailableWidth();
    header.setMarginBottom(10);

    Table brand = new Table(UnitValue.createPercentArray(new float[] { 18, 82 })).useAllAvailableWidth();
    brand.addCell(pdfLogoCell(font));
    brand.addCell(pdfNoBorderCell()
        .add(pdfText(HOTEL_NAME, font, 15, true).setFontColor(PDF_TEXT_DARK).setMarginBottom(2))
        .add(pdfText(HOTEL_ADDRESS, font, 9, false).setFontColor(PDF_TEXT_SUB).setMarginBottom(1))
        .add(pdfText("Hotline: " + HOTEL_PHONE, font, 9, false).setFontColor(PDF_TEXT_SUB)));

    header.addCell(pdfNoBorderCell().add(brand));
    header.addCell(pdfNoBorderCell()
        .setTextAlignment(TextAlignment.RIGHT)
        .add(pdfText("HÓA ĐƠN THANH TOÁN", font, 18, true).setFontColor(PDF_PRIMARY))
        .add(pdfText("Mã hóa đơn: " + data.invoiceId, font, 10, true).setFontColor(PDF_TEXT_DARK))
        .add(pdfText(data.invoiceDate, font, 9, false).setFontColor(PDF_TEXT_SUB)));
    doc.add(header);
    doc.add(new Paragraph("").setBorderBottom(new SolidBorder(PDF_BORDER, 1)).setMarginBottom(14));
  }

  private com.itextpdf.layout.element.Cell pdfLogoCell(PdfFont font) {
    return new com.itextpdf.layout.element.Cell()
        .setBorder(Border.NO_BORDER)
        .setBackgroundColor(PDF_PRIMARY)
        .setTextAlignment(TextAlignment.CENTER)
        .setVerticalAlignment(VerticalAlignment.MIDDLE)
        .setPadding(8)
        .add(pdfText("N06", font, 14, true).setFontColor(ColorConstants.WHITE));
  }

  private void addPdfInvoiceInfo(Document doc, PdfFont font, InvoiceData data) {
    doc.add(pdfSectionTitle("Thông tin hóa đơn", font));
    Table info = new Table(UnitValue.createPercentArray(new float[] { 18, 32, 18, 32 })).useAllAvailableWidth();
    info.setMarginBottom(14);
    addPdfInfoPair(info, font, "Mã hóa đơn", data.invoiceId);
    addPdfInfoPair(info, font, "Khách hàng", data.customerName);
    addPdfInfoPair(info, font, "Số điện thoại", data.customerPhone);
    addPdfInfoPair(info, font, "Ngày lập", data.invoiceDate);
    addPdfInfoPair(info, font, "Nhân viên", data.staffName);
    addPdfInfoPair(info, font, "Mã phiếu đặt", data.bookingId);
    doc.add(info);
  }

  private void addPdfInfoPair(Table table, PdfFont font, String label, String value) {
    table.addCell(pdfInfoLabel(label, font));
    table.addCell(pdfInfoValue(value, font));
  }

  private void addPdfRoomTable(Document doc, PdfFont font, InvoiceData data) {
    doc.add(pdfSectionTitle("Chi tiết phòng", font));
    Table table = new Table(UnitValue.createPercentArray(new float[] { 12, 18, 26, 15, 10, 19 }))
        .useAllAvailableWidth();
    table.setMarginBottom(14);
    addPdfHeaderCells(table, font, "Phòng", "Loại phòng", "Thời gian lưu trú", "Đơn giá", "Số đêm", "Thành tiền");

    int rowIndex = 0;
    for (InvoiceRoomLine line : data.roomLines) {
      com.itextpdf.kernel.colors.Color rowBg = rowIndex++ % 2 == 0 ? ColorConstants.WHITE : PDF_ROW_ALT;
      table.addCell(pdfDataCell(line.roomCode(), font, TextAlignment.CENTER, rowBg));
      table.addCell(pdfDataCell(line.roomType(), font, TextAlignment.LEFT, rowBg));
      table.addCell(pdfDataCell(line.stayPeriod(), font, TextAlignment.CENTER, rowBg));
      table.addCell(pdfDataCell(formatCurrency(line.price()), font, TextAlignment.RIGHT, rowBg));
      table.addCell(pdfDataCell(String.valueOf(line.nights()), font, TextAlignment.CENTER, rowBg));
      table.addCell(pdfDataCell(formatCurrency(line.total()), font, TextAlignment.RIGHT, rowBg));
    }
    doc.add(table);
  }

  private void addPdfServiceTable(Document doc, PdfFont font, InvoiceData data) {
    doc.add(pdfSectionTitle("Dịch vụ sử dụng", font));
    Table table = new Table(UnitValue.createPercentArray(new float[] { 42, 16, 20, 22 })).useAllAvailableWidth();
    table.setMarginBottom(14);
    addPdfHeaderCells(table, font, "Tên dịch vụ", "Số lượng", "Đơn giá", "Thành tiền");

    int rowIndex = 0;
    for (InvoiceServiceLine line : data.serviceLines) {
      com.itextpdf.kernel.colors.Color rowBg = rowIndex++ % 2 == 0 ? ColorConstants.WHITE : PDF_ROW_ALT;
      table.addCell(pdfDataCell(line.serviceName(), font, TextAlignment.LEFT, rowBg));
      table.addCell(pdfDataCell(String.valueOf(line.quantity()), font, TextAlignment.CENTER, rowBg));
      table.addCell(pdfDataCell(formatCurrency(line.unitPrice()), font, TextAlignment.RIGHT, rowBg));
      table.addCell(pdfDataCell(formatCurrency(line.total()), font, TextAlignment.RIGHT, rowBg));
    }
    doc.add(table);
  }

  private void addPdfHeaderCells(Table table, PdfFont font, String... titles) {
    for (String title : titles) {
      table.addHeaderCell(new com.itextpdf.layout.element.Cell()
          .setBackgroundColor(PDF_PRIMARY)
          .setBorder(new SolidBorder(PDF_PRIMARY, 0.5f))
          .setPadding(7)
          .setTextAlignment(TextAlignment.CENTER)
          .setVerticalAlignment(VerticalAlignment.MIDDLE)
          .add(pdfText(title, font, 9, true).setFontColor(ColorConstants.WHITE)));
    }
  }

  private void addPdfSummary(Document doc, PdfFont font, InvoiceData data) {
    Table wrapper = new Table(UnitValue.createPercentArray(new float[] { 54, 46 })).useAllAvailableWidth();
    wrapper.addCell(pdfNoBorderCell());

    Table summary = new Table(UnitValue.createPercentArray(new float[] { 52, 48 })).useAllAvailableWidth();
    summary.setBackgroundColor(PDF_SUMMARY_BG);
    summary.setBorder(new SolidBorder(PDF_BORDER, 0.8f));
    addPdfSummaryRow(summary, font, "Tổng tiền phòng", formatCurrency(data.roomTotal), false, false);
    addPdfSummaryRow(summary, font, "Tổng tiền dịch vụ", formatCurrency(data.serviceTotal), false, false);
    addPdfSummaryRow(summary, font, "Tiền đã đặt cọc", formatCreditCurrency(data.depositAmount), true, false);
    addPdfSummaryRow(summary, font, "THÀNH TIỀN", formatCurrency(data.finalAmount), false, true);

    wrapper.addCell(pdfNoBorderCell().add(summary));
    doc.add(wrapper);
  }

  private void addPdfSummaryRow(Table table, PdfFont font, String label, String value, boolean credit, boolean total) {
    com.itextpdf.kernel.colors.Color textColor = credit ? PDF_SUCCESS : total ? PDF_PRIMARY : PDF_TEXT_DARK;
    com.itextpdf.kernel.colors.Color rowBg = total ? new DeviceRgb(220, 252, 241) : PDF_SUMMARY_BG;

    table.addCell(new com.itextpdf.layout.element.Cell()
        .setBorder(Border.NO_BORDER)
        .setBorderTop(total ? new SolidBorder(PDF_BORDER, 0.8f) : Border.NO_BORDER)
        .setPadding(total ? 8 : 6)
        .setBackgroundColor(rowBg)
        .add(pdfText(label, font, total ? 11 : 9, total).setFontColor(total ? PDF_TEXT_DARK : PDF_TEXT_SUB)));
    table.addCell(new com.itextpdf.layout.element.Cell()
        .setBorder(Border.NO_BORDER)
        .setBorderTop(total ? new SolidBorder(PDF_BORDER, 0.8f) : Border.NO_BORDER)
        .setPadding(total ? 8 : 6)
        .setBackgroundColor(rowBg)
        .setTextAlignment(TextAlignment.RIGHT)
        .add(pdfText(value, font, total ? 14 : 10, true).setFontColor(textColor)));
  }

  private void addPdfFooter(Document doc, PdfFont font) {
    doc.add(pdfText("Cảm ơn quý khách đã sử dụng dịch vụ!", font, 11, true)
        .setFontColor(PDF_PRIMARY)
        .setTextAlignment(TextAlignment.CENTER)
        .setMarginTop(22));

    Table signature = new Table(UnitValue.createPercentArray(new float[] { 50, 50 })).useAllAvailableWidth();
    signature.setMarginTop(18);
    signature.addCell(pdfSignatureCell(font, "Khách hàng"));
    signature.addCell(pdfSignatureCell(font, "Lễ tân"));
    doc.add(signature);
  }

  private com.itextpdf.layout.element.Cell pdfSignatureCell(PdfFont font, String title) {
    return pdfNoBorderCell()
        .setTextAlignment(TextAlignment.CENTER)
        .add(pdfText(title, font, 10, true).setFontColor(PDF_TEXT_DARK))
        .add(pdfText("(Ký và ghi rõ họ tên)", font, 8, false).setFontColor(PDF_TEXT_SUB).setMarginTop(34));
  }

  private Paragraph pdfSectionTitle(String title, PdfFont font) {
    return pdfText(title, font, 12, true)
        .setFontColor(PDF_TEXT_DARK)
        .setMarginTop(4)
        .setMarginBottom(7);
  }

  private com.itextpdf.layout.element.Cell pdfInfoLabel(String label, PdfFont font) {
    return new com.itextpdf.layout.element.Cell()
        .setBorder(new SolidBorder(PDF_BORDER, 0.5f))
        .setBackgroundColor(PDF_ROW_ALT)
        .setPadding(6)
        .setVerticalAlignment(VerticalAlignment.MIDDLE)
        .add(pdfText(label, font, 8.5f, true).setFontColor(PDF_TEXT_SUB));
  }

  private com.itextpdf.layout.element.Cell pdfInfoValue(String value, PdfFont font) {
    return new com.itextpdf.layout.element.Cell()
        .setBorder(new SolidBorder(PDF_BORDER, 0.5f))
        .setPadding(6)
        .setVerticalAlignment(VerticalAlignment.MIDDLE)
        .add(pdfText(value == null || value.isBlank() ? "--" : value, font, 9.5f, true).setFontColor(PDF_TEXT_DARK));
  }

  private com.itextpdf.layout.element.Cell pdfDataCell(String value, PdfFont font, TextAlignment alignment,
      com.itextpdf.kernel.colors.Color backgroundColor) {
    return new com.itextpdf.layout.element.Cell()
        .setBorder(new SolidBorder(PDF_BORDER, 0.5f))
        .setBackgroundColor(backgroundColor)
        .setPadding(6)
        .setTextAlignment(alignment)
        .setVerticalAlignment(VerticalAlignment.MIDDLE)
        .add(pdfText(value == null || value.isBlank() ? "--" : value, font, 8.5f, false).setFontColor(PDF_TEXT_DARK));
  }

  private com.itextpdf.layout.element.Cell pdfNoBorderCell() {
    return new com.itextpdf.layout.element.Cell()
        .setBorder(Border.NO_BORDER)
        .setPadding(0);
  }

  private Paragraph pdfText(String text, PdfFont font, float size, boolean bold) {
    Paragraph paragraph = new Paragraph(text == null ? "" : text)
        .setFont(font)
        .setFontSize(size)
        .setMargin(0);
    if (bold) {
      paragraph.setBold();
    }
    return paragraph;
  }

  private void exportExcel() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Xuất Excel");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
    fileChooser.setInitialFileName("invoice.xlsx");
    File file = fileChooser.showSaveDialog(null);

    if (file != null) {
      try (Workbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("Danh_Sach_Hoa_Don");

        Row header = sheet.createRow(0);
        String[] columns = {
            "Mã HĐ", "Mã phiếu", "Khách hàng", "Nhân viên", "Ngày lập",
            "Tiền phòng", "Tiền dịch vụ", "Tiền đã đặt cọc", "Thành tiền"
        };
        for (int i = 0; i < columns.length; i++) {
          Cell cell = header.createCell(i);
          cell.setCellValue(columns[i]);
        }

        int rowNum = 1;
        for (HoaDon hd : dataList) {
          Row row = sheet.createRow(rowNum++);
          row.createCell(0).setCellValue(hd.getMaHoaDon());
          row.createCell(1).setCellValue(hd.getPhieuDatPhong() != null ? hd.getPhieuDatPhong().getMaDatPhong() : "");
          String tenKh = hd.getPhieuDatPhong() != null && hd.getPhieuDatPhong().getKhachHang() != null
              ? hd.getPhieuDatPhong().getKhachHang().getTenKhachHang()
              : "";
          row.createCell(2).setCellValue(tenKh);
          row.createCell(3).setCellValue(hd.getNhanVien() != null
              ? (hd.getNhanVien().getTenNhanVien() != null ? hd.getNhanVien().getTenNhanVien()
                  : hd.getNhanVien().getMaNhanVien())
              : "");
          row.createCell(4).setCellValue(hd.getNgayLap() != null ? hd.getNgayLap().format(DATE_TIME_FORMAT) : "");
          row.createCell(5).setCellValue(hd.getTongTienPhong());
          row.createCell(6).setCellValue(hd.getTongTienDichVu());
          row.createCell(7).setCellValue(getDepositAmount(hd));
          row.createCell(8).setCellValue(calculateInvoiceFinalAmount(hd));
        }

        for (int i = 0; i < columns.length; i++) {
          sheet.autoSizeColumn(i);
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
          workbook.write(out);
        }
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Xuất file Excel thành công.");
      } catch (Exception e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xuất Excel: " + e.getMessage());
      }
    }
  }

  private InvoiceData buildInvoiceData(HoaDon hd) {
    PhieuDatPhong booking = resolveBooking(hd);
    String bookingId = booking != null ? booking.getMaDatPhong() : "";
    List<ChiTietPhieuDat> roomDetails = bookingId == null || bookingId.isBlank()
        ? List.of()
        : chiTietPhieuDatDAO.getChiTietPhongHoaDon(bookingId);
    LinkedHashSet<String> returnedRoomIds = roomDetails.stream()
        .map(detail -> detail.getPhong() != null ? detail.getPhong().getMaPhong() : null)
        .filter(roomId -> roomId != null && !roomId.isBlank())
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    List<ChiTietHoaDon> services = chiTietHoaDonDAO.getDSChiTietByMaHD(hd.getMaHoaDon()).stream()
        .filter(detail -> {
          String roomId = detail.getPhong() != null ? detail.getPhong().getMaPhong() : null;
          return roomId != null && returnedRoomIds.contains(roomId);
        })
        .toList();

    for (String roomId : returnedRoomIds) {
      System.out.println("[INVOICE ROOM] " + roomId);
    }
    System.out.println("[INVOICE LOAD] rooms = " + returnedRoomIds.size());

    List<InvoiceRoomLine> roomLines = roomDetails.stream()
        .sorted(Comparator.comparing(detail -> detail.getPhong() != null ? detail.getPhong().getMaPhong() : ""))
        .map(this::toRoomLine)
        .toList();

    List<InvoiceServiceLine> serviceLines = services.stream()
        .map(this::toServiceLine)
        .toList();

    KhachHang khachHang = booking != null ? booking.getKhachHang() : null;
    if (khachHang == null && hd.getPhieuDatPhong() != null) {
      khachHang = hd.getPhieuDatPhong().getKhachHang();
    }
    double computedRoomTotal = roomLines.stream().mapToDouble(InvoiceRoomLine::total).sum();
    double computedServiceTotal = serviceLines.stream().mapToDouble(InvoiceServiceLine::total).sum();
    double roomTotal = hd.getTongTienPhong() > 0 ? hd.getTongTienPhong() : computedRoomTotal;
    double serviceTotal = hd.getTongTienDichVu() > 0 ? hd.getTongTienDichVu() : computedServiceTotal;
    double depositAmount = booking != null ? Math.max(0, booking.getTienDatCoc()) : getDepositAmount(hd);
    double finalAmount = calculateFinalAmount(roomTotal, serviceTotal, depositAmount);

    return new InvoiceData(
        hd.getMaHoaDon(),
        hd.getNgayLap() != null ? hd.getNgayLap().format(DATE_TIME_FORMAT) : "",
        bookingId != null ? bookingId : "",
        khachHang != null && khachHang.getTenKhachHang() != null ? khachHang.getTenKhachHang() : "Khách lẻ",
        khachHang != null && khachHang.getSoDienThoai() != null && !khachHang.getSoDienThoai().isBlank()
            ? khachHang.getSoDienThoai()
            : "Chưa cập nhật",
        hd.getNhanVien() != null
            ? (hd.getNhanVien().getTenNhanVien() != null && !hd.getNhanVien().getTenNhanVien().isBlank()
                ? hd.getNhanVien().getTenNhanVien()
                : hd.getNhanVien().getMaNhanVien())
            : "",
        roomLines,
        serviceLines,
        roomTotal,
        serviceTotal,
        depositAmount,
        finalAmount);
  }

  private PhieuDatPhong resolveBooking(HoaDon hd) {
    if (hd == null || hd.getPhieuDatPhong() == null || hd.getPhieuDatPhong().getMaDatPhong() == null
        || hd.getPhieuDatPhong().getMaDatPhong().isBlank()) {
      return hd != null ? hd.getPhieuDatPhong() : null;
    }

    PhieuDatPhong fallback = hd.getPhieuDatPhong();
    try {
      PhieuDatPhong fullBooking = phieuDatPhongDAO.getPhieuDatPhongByMa(fallback.getMaDatPhong());
      if (fullBooking != null) {
        if (fullBooking.getKhachHang() == null) {
          fullBooking.setKhachHang(fallback.getKhachHang());
        }
        return fullBooking;
      }
    } catch (RuntimeException ignored) {
      // Dữ liệu trên hóa đơn vẫn đủ để hiển thị khi không tải lại được phiếu đặt.
    }
    return fallback;
  }

  private InvoiceRoomLine toRoomLine(ChiTietPhieuDat detail) {
    LocalDateTime checkIn = detail.getThoiDiemNhanThucTe() != null ? detail.getThoiDiemNhanThucTe()
        : detail.getNgayNhan();
    LocalDateTime checkOut = detail.getThoiDiemTraThucTe() != null ? detail.getThoiDiemTraThucTe()
        : detail.getNgayTra();
    int nights = calculateNights(checkIn, checkOut);
    double price = detail.getGiaThuePhong() > 0 ? detail.getGiaThuePhong()
        : pricingService.getStoredBookingPrice(detail);
    double total = price > 0 ? price : pricingService.getStoredBookingPrice(detail);

    return new InvoiceRoomLine(
        detail.getPhong() != null ? detail.getPhong().getMaPhong() : "",
        detail.getPhong() != null ? detail.getPhong().getLoaiPhong() : "",
        checkIn != null ? checkIn.format(DATE_FORMAT) : "--",
        checkOut != null ? checkOut.format(DATE_FORMAT) : "--",
        price,
        nights,
        total);
  }

  private InvoiceServiceLine toServiceLine(ChiTietHoaDon detail) {
    String roomCode = detail.getPhong() != null ? detail.getPhong().getMaPhong() : "";
    String serviceName = detail.getDichVu() != null ? detail.getDichVu().getTenDichVu() : "";
    int quantity = detail.getSoLuong();
    double unitPrice = detail.getDonGiaLuuTru();
    return new InvoiceServiceLine(roomCode, serviceName, quantity, unitPrice, quantity * unitPrice);
  }

  private int calculateNights(LocalDateTime checkIn, LocalDateTime checkOut) {
    if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
      return 1;
    }
    long hours = Duration.between(checkIn, checkOut).toHours();
    return (int) Math.max(1, Math.ceil(hours / 24.0));
  }

  private Path findBrowserExecutable() {
    List<String> candidates = List.of(
        System.getenv("ProgramFiles") + "\\Microsoft\\Edge\\Application\\msedge.exe",
        System.getenv("ProgramFiles(x86)") + "\\Microsoft\\Edge\\Application\\msedge.exe",
        System.getenv("ProgramFiles") + "\\Google\\Chrome\\Application\\chrome.exe",
        System.getenv("ProgramFiles(x86)") + "\\Google\\Chrome\\Application\\chrome.exe");

    for (String candidate : candidates) {
      if (candidate == null) {
        continue;
      }
      File file = new File(candidate);
      if (file.exists()) {
        return file.toPath();
      }
    }
    return null;
  }

  private String buildInvoiceHtml(InvoiceData data) {
    StringBuilder roomRows = new StringBuilder();
    for (InvoiceRoomLine line : data.roomLines) {
      roomRows.append("""
          <tr>
            <td>%s</td>
            <td>%s</td>
            <td>%s</td>
            <td>%s</td>
            <td class="num">%s</td>
            <td class="num">%s</td>
            <td class="num">%s</td>
          </tr>
          """.formatted(
          escapeHtml(line.roomCode()),
          escapeHtml(line.roomType()),
          escapeHtml(line.checkInLabel()),
          escapeHtml(line.checkOutLabel()),
          formatCurrency(line.price()),
          String.valueOf(line.nights()),
          formatCurrency(line.total())));
    }

    StringBuilder serviceRows = new StringBuilder();
    for (InvoiceServiceLine line : data.serviceLines) {
      serviceRows.append("""
          <tr>
            <td>%s</td>
            <td>%s</td>
            <td class="num">%s</td>
            <td class="num">%s</td>
            <td class="num">%s</td>
          </tr>
          """.formatted(
          escapeHtml(line.roomCode()),
          escapeHtml(line.serviceName()),
          line.quantity(),
          formatCurrency(line.unitPrice()),
          formatCurrency(line.total())));
    }

    String serviceSection = data.serviceLines.isEmpty() ? "" : """
        <h3>Dịch vụ phát sinh</h3>
        <table>
          <thead>
            <tr>
              <th>Phòng</th>
              <th>Dịch vụ</th>
              <th class="num">SL</th>
              <th class="num">Đơn giá</th>
              <th class="num">Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            %s
          </tbody>
        </table>
        """.formatted(serviceRows);

    return """
        <!doctype html>
        <html lang="vi">
        <head>
          <meta charset="UTF-8">
          <title>Hóa đơn %s</title>
          <style>
            * { box-sizing: border-box; }
            body {
              font-family: Arial, "Segoe UI", sans-serif;
              color: #0f172a;
              margin: 32px;
              font-size: 13px;
            }
            .header {
              text-align: center;
              margin-bottom: 24px;
            }
            .header h1 {
              margin: 0;
              font-size: 28px;
              letter-spacing: 0.5px;
            }
            .header .meta {
              margin-top: 8px;
              color: #475569;
              line-height: 1.6;
            }
            h2 {
              text-align: center;
              margin: 18px 0 22px;
              font-size: 24px;
            }
            .grid {
              display: grid;
              grid-template-columns: 1fr 1fr;
              gap: 16px;
              margin-bottom: 18px;
            }
            .card {
              border: 1px solid #cbd5e1;
              border-radius: 10px;
              padding: 14px 16px;
            }
            .card h3 {
              margin: 0 0 10px;
              font-size: 15px;
            }
            .line {
              display: flex;
              justify-content: space-between;
              gap: 12px;
              margin: 6px 0;
            }
            table {
              width: 100%;
              border-collapse: collapse;
              margin-top: 12px;
              margin-bottom: 18px;
            }
            th, td {
              border: 1px solid #cbd5e1;
              padding: 8px 10px;
            }
            th {
              background: #e2e8f0;
              text-align: left;
            }
            .num {
              text-align: right;
            }
            .summary {
              width: 340px;
              margin-left: auto;
              border: 1px solid #cbd5e1;
              border-radius: 10px;
              padding: 12px 16px;
            }
            .summary .line.total {
              font-weight: 700;
              font-size: 16px;
              margin-top: 12px;
              padding-top: 12px;
              border-top: 1px solid #cbd5e1;
            }
            .footer {
              text-align: center;
              margin-top: 28px;
              color: #475569;
            }
          </style>
        </head>
        <body>
          <div class="header">
            <h1>%s</h1>
            <div class="meta">%s<br>%s</div>
          </div>
          <h2>HÓA ĐƠN THANH TOÁN</h2>
          <div class="grid">
            <div class="card">
              <h3>Thông tin hóa đơn</h3>
              <div class="line"><span>Mã hóa đơn</span><strong>%s</strong></div>
              <div class="line"><span>Ngày lập</span><span>%s</span></div>
              <div class="line"><span>Nhân viên</span><span>%s</span></div>
              <div class="line"><span>Mã phiếu</span><span>%s</span></div>
            </div>
            <div class="card">
              <h3>Khách hàng</h3>
              <div class="line"><span>Họ tên</span><strong>%s</strong></div>
              <div class="line"><span>Số điện thoại</span><span>%s</span></div>
            </div>
          </div>
          <h3>Danh sách phòng</h3>
          <table>
            <thead>
              <tr>
                <th>Phòng</th>
                <th>Loại</th>
                <th>Nhận phòng</th>
                <th>Trả phòng</th>
                <th class="num">Đơn giá</th>
                <th class="num">Đêm</th>
                <th class="num">Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              %s
            </tbody>
          </table>
          %s
          <div class="summary">
            <div class="line"><span>Tổng tiền phòng</span><span>%s</span></div>
            <div class="line"><span>Tổng tiền dịch vụ</span><span>%s</span></div>
            <div class="line"><span>Tiền đã đặt cọc</span><span>%s</span></div>
            <div class="line total"><span>Thành tiền</span><span>%s</span></div>
          </div>
          <div class="footer">
            Cảm ơn quý khách đã sử dụng dịch vụ của %s.<br>
            Hẹn gặp lại quý khách trong những lần lưu trú tiếp theo.
          </div>
        </body>
        </html>
        """.formatted(
        escapeHtml(data.invoiceId),
        escapeHtml(HOTEL_NAME),
        escapeHtml(HOTEL_ADDRESS),
        escapeHtml(HOTEL_PHONE),
        escapeHtml(data.invoiceId),
        escapeHtml(data.invoiceDate),
        escapeHtml(data.staffName),
        escapeHtml(data.bookingId),
        escapeHtml(data.customerName),
        escapeHtml(data.customerPhone),
        roomRows,
        serviceSection,
        formatCurrency(data.roomTotal),
        formatCurrency(data.serviceTotal),
        formatCreditCurrency(data.depositAmount),
        formatCurrency(data.finalAmount),
        escapeHtml(HOTEL_NAME));
  }

  private String escapeHtml(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  private TableColumn<HoaDon, String> textCol(String title,
      java.util.function.Function<HoaDon, String> valueProvider,
      double width,
      Pos alignment) {
    TableColumn<HoaDon, String> col = new TableColumn<>(title);
    col.setPrefWidth(width);
    col.setCellValueFactory(data -> new SimpleStringProperty(valueProvider.apply(data.getValue())));
    col.setCellFactory(column -> alignedCell(alignment));
    return col;
  }

  private TableColumn<HoaDon, String> moneyCol(String title,
      java.util.function.ToDoubleFunction<HoaDon> valueProvider,
      double width) {
    TableColumn<HoaDon, String> col = new TableColumn<>(title);
    col.setPrefWidth(width);
    col.setCellValueFactory(
        data -> new SimpleStringProperty(formatCurrency(valueProvider.applyAsDouble(data.getValue()))));
    col.setCellFactory(column -> styledCell(Pos.CENTER_RIGHT, false));
    return col;
  }

  private TableColumn<HoaDon, String> totalMoneyCol(String title,
      java.util.function.ToDoubleFunction<HoaDon> valueProvider,
      double width) {
    TableColumn<HoaDon, String> col = new TableColumn<>(title);
    col.setPrefWidth(width);
    col.setCellValueFactory(
        data -> new SimpleStringProperty(formatCurrency(valueProvider.applyAsDouble(data.getValue()))));
    col.setCellFactory(column -> styledCell(Pos.CENTER_RIGHT, true));
    return col;
  }

  private TableColumn<InvoiceRoomLine, String> roomTextCol(String title,
      java.util.function.Function<InvoiceRoomLine, String> provider,
      double width,
      Pos alignment) {
    TableColumn<InvoiceRoomLine, String> col = new TableColumn<>(title);
    col.setPrefWidth(width);
    col.setCellValueFactory(data -> new SimpleStringProperty(provider.apply(data.getValue())));
    col.setCellFactory(column -> genericAlignedCell(alignment));
    return col;
  }

  private TableColumn<InvoiceRoomLine, String> roomMoneyCol(String title,
      java.util.function.ToDoubleFunction<InvoiceRoomLine> provider,
      double width) {
    TableColumn<InvoiceRoomLine, String> col = new TableColumn<>(title);
    col.setPrefWidth(width);
    col.setCellValueFactory(data -> new SimpleStringProperty(formatCurrency(provider.applyAsDouble(data.getValue()))));
    col.setCellFactory(column -> genericAlignedCell(Pos.CENTER_RIGHT));
    return col;
  }

  private TableColumn<InvoiceServiceLine, String> serviceTextCol(String title,
      java.util.function.Function<InvoiceServiceLine, String> provider,
      double width,
      Pos alignment) {
    TableColumn<InvoiceServiceLine, String> col = new TableColumn<>(title);
    col.setPrefWidth(width);
    col.setCellValueFactory(data -> new SimpleStringProperty(provider.apply(data.getValue())));
    col.setCellFactory(column -> genericAlignedCell(alignment));
    return col;
  }

  private TableColumn<InvoiceServiceLine, String> serviceMoneyCol(String title,
      java.util.function.ToDoubleFunction<InvoiceServiceLine> provider,
      double width) {
    TableColumn<InvoiceServiceLine, String> col = new TableColumn<>(title);
    col.setPrefWidth(width);
    col.setCellValueFactory(data -> new SimpleStringProperty(formatCurrency(provider.applyAsDouble(data.getValue()))));
    col.setCellFactory(column -> genericAlignedCell(Pos.CENTER_RIGHT));
    return col;
  }

  private TableCell<HoaDon, String> alignedCell(Pos alignment) {
    return new TableCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty ? null : item);
        setAlignment(alignment);
      }
    };
  }

  private TableCell<HoaDon, String> styledCell(Pos alignment, boolean highlight) {
    return new TableCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty ? null : item);
        setAlignment(alignment);
        if (empty) {
          setStyle("");
        } else if (highlight) {
          setStyle("-fx-font-weight: 700; -fx-text-fill: " + PRIMARY + ";");
        } else if (alignment == Pos.CENTER_RIGHT) {
          setStyle("-fx-font-weight: 600; -fx-text-fill: " + TEXT_DARK + ";");
        } else {
          setStyle("");
        }
      }
    };
  }

  private <T> TableCell<T, String> genericAlignedCell(Pos alignment) {
    return new TableCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty ? null : item);
        setAlignment(alignment);
      }
    };
  }

  private Label infoLabel(String text, boolean isBold) {
    Label label = new Label(text);
    label.setFont(Font.font("Segoe UI", isBold ? FontWeight.BOLD : FontWeight.NORMAL, 14));
    label.setTextFill(Color.web(TEXT_DARK));
    return label;
  }

  private Button createStyledButton(String text, String color) {
    Button b = new Button(text);
    b.setPrefHeight(40);
    b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
    b.setTextFill(Color.WHITE);

    String n = "-fx-background-color:" + color + ";-fx-background-radius:10;-fx-cursor:hand;-fx-padding:0 18;";
    String h = "-fx-background-color:derive(" + color
        + ",-12%);-fx-background-radius:10;-fx-cursor:hand;-fx-padding:0 18;";

    b.setStyle(n);
    b.setOnMouseEntered(e -> b.setStyle(h));
    b.setOnMouseExited(e -> b.setStyle(n));
    return b;
  }

  private static String buildTableCss() {
    String css = ".module-table{" +
        "-fx-background-color:white;" +
        "-fx-border-color:#e2e8f0;" +
        "-fx-border-radius:12;" +
        "-fx-background-radius:12;" +
        "}" +
        ".module-table .column-header-background{" +
        "-fx-background-color:#f8fafc;" +
        "-fx-background-radius:12 12 0 0;" +
        "}" +
        ".module-table .column-header{" +
        "-fx-background-color:transparent;" +
        "-fx-border-color:transparent transparent #e2e8f0 transparent;" +
        "-fx-size:46;" +
        "}" +
        ".module-table .column-header .label{" +
        "-fx-text-fill:#475569;" +
        "-fx-font-weight:bold;" +
        "-fx-font-size:12px;" +
        "-fx-padding:0 14 0 14;" +
        "}" +
        ".module-table .table-cell{" +
        "-fx-padding:11 14 11 14;" +
        "-fx-border-color:transparent transparent #f1f5f9 transparent;" +
        "-fx-font-size:13px;" +
        "-fx-text-fill:#334155;" +
        "}" +
        ".module-table .table-row-cell{" +
        "-fx-background-color:white;" +
        "-fx-cell-size:48px;" +
        "}" +
        ".module-table .table-row-cell:odd{" +
        "-fx-background-color:#fafbfd;" +
        "}" +
        ".module-table .table-row-cell:selected .table-cell{" +
        "-fx-text-fill:#0f766e;" +
        "}" +
        ".module-table .placeholder .label{" +
        "-fx-text-fill:#94a3b8;" +
        "-fx-font-size:14px;" +
        "}";
    try {
      return "data:text/css," + URLEncoder.encode(css, StandardCharsets.UTF_8).replace("+", "%20");
    } catch (Exception ex) {
      return "";
    }
  }

  private String formatCurrency(double value) {
    return String.format(Locale.US, "%,.0f VNĐ", value);
  }

  private String formatCreditCurrency(double value) {
    return value > 0 ? "-" + formatCurrency(value) : formatCurrency(0);
  }

  private double getDepositAmount(HoaDon hd) {
    if (hd == null || hd.getPhieuDatPhong() == null) {
      return 0;
    }
    return Math.max(0, hd.getPhieuDatPhong().getTienDatCoc());
  }

  private double calculateInvoiceFinalAmount(HoaDon hd) {
    if (hd == null) {
      return 0;
    }
    return calculateFinalAmount(hd.getTongTienPhong(), hd.getTongTienDichVu(), getDepositAmount(hd));
  }

  private double calculateFinalAmount(double roomTotal, double serviceTotal, double depositAmount) {
    return Math.max(0, roomTotal + serviceTotal - depositAmount);
  }

  private void showAlert(Alert.AlertType type, String title, String msg) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(msg);
    alert.showAndWait();
  }

  private record InvoiceRoomLine(String roomCode, String roomType, String checkInLabel,
      String checkOutLabel, double price, int nights, double total) {
    private String stayPeriod() {
      return checkInLabel + " - " + checkOutLabel;
    }
  }

  private record InvoiceServiceLine(String roomCode, String serviceName, int quantity,
      double unitPrice, double total) {
  }

  private record InvoiceData(String invoiceId, String invoiceDate, String bookingId, String customerName,
      String customerPhone, String staffName, List<InvoiceRoomLine> roomLines,
      List<InvoiceServiceLine> serviceLines, double roomTotal, double serviceTotal,
      double depositAmount, double finalAmount) {
  }
}
