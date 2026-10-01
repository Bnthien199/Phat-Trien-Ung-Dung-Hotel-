package gui;

import entity.KhachHang;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;
import util.AppTheme;
import util.ReportExportUtils;
import util.ReportExportUtils.SaveTarget;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomerView {
    private static final Logger LOGGER = Logger.getLogger(CustomerView.class.getName());

    // ── palette (đồng bộ AppTheme) ──────────────────────────────────
    private static final String C_BG = AppTheme.BG_LIGHT; // #f4f7fb
    private static final String C_SURFACE = AppTheme.SURFACE; // #ffffff
    private static final String C_PRIMARY = AppTheme.PRIMARY;
    private static final String C_ACCENT = AppTheme.TEAL_DARK;
    private static final String C_SUCCESS = AppTheme.SUCCESS; // #0f766e
    private static final String C_BORDER = AppTheme.BORDER; // #d7deea
    private static final String C_TEXT = AppTheme.TEXT; // #233044
    private static final String C_MUTED = AppTheme.MUTED; // #64748b

    private static final String TABLE_CSS = buildTableCss();

    // ── state ────────────────────────────────────────────────────────
    private final KhachHangService service = new KhachHangService();
    private final TableView<KhachHang> tableView = new TableView<>();
    private List<KhachHang> allData = new ArrayList<>();
    private TextField txtSearch;
    private ComboBox<String> cboSearchType;
    private ComboBox<String> cboTrangThai;
    private Label lblCount;
    private Consumer<KhachHang> onBookingRequest;
    private KhachHang selectedCustomer;

    // ═════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═════════════════════════════════════════════════════════════════

    public Node createView() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + C_BG + ";");

        root.getChildren().addAll(buildPageHeader(), buildToolbar(), buildTableCard());
        loadData();
        return root;
    }

    public void setOnBookingRequest(Consumer<KhachHang> callback) {
        this.onBookingRequest = callback;
    }

    public void loadData() {
        Task<List<KhachHang>> task = new Task<>() {
            @Override
            protected List<KhachHang> call() {
                return service.getAll();
            }
        };
        task.setOnSucceeded(e -> {
            allData = task.getValue() != null ? task.getValue() : new ArrayList<>();
            filterData();
            if (lblCount != null) {
                lblCount.setText(allData.size() + " khách hàng");
            }
        });
        new Thread(task).start();
    }

    // ═════════════════════════════════════════════════════════════════
    // HEADER — title + subtitle + count badge
    // ═════════════════════════════════════════════════════════════════

    private HBox buildPageHeader() {
        // avatar icon
        Circle avatar = new Circle(22);
        avatar.setFill(Color.web(AppTheme.TEAL_LIGHT));

        Label icon = new Label("👤");
        icon.setFont(Font.font(18));
        icon.setTextFill(Color.web(C_PRIMARY));
        StackPane iconWrap = new StackPane(avatar, icon);
        iconWrap.setPrefSize(44, 44);

        // titles
        Label lblTitle = new Label("Quản lý khách hàng");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblTitle.setTextFill(Color.web(C_TEXT));

        Label lblSub = new Label("Xem, thêm, tìm kiếm và cập nhật thông tin khách hàng");
        lblSub.setFont(Font.font("Segoe UI", 13));
        lblSub.setTextFill(Color.web(C_MUTED));

        VBox titleBox = new VBox(2, lblTitle, lblSub);

        // count badge
        lblCount = new Label("— khách hàng");
        lblCount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblCount.setTextFill(Color.web(C_ACCENT));
        lblCount.setStyle(
                "-fx-background-color: #eff6ff;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 4 14;");

        // action buttons
        Button btnDatPhong = buildFilledBtn("🛏  Đặt Phòng", AppTheme.TEAL);
        btnDatPhong.setOnAction(e -> {
            if (selectedCustomer == null) {
                new Alert(Alert.AlertType.WARNING, "Vui lòng chọn khách hàng.").showAndWait();
                return;
            }
            if (!selectedCustomer.isDangHoatDong()) {
                new Alert(Alert.AlertType.WARNING, "Khách hàng đã bị vô hiệu hóa").showAndWait();
                return;
            }
            if (onBookingRequest != null) {
                onBookingRequest.accept(selectedCustomer);
            }
        });

        Button btnThem = buildFilledBtn("＋  Thêm mới", AppTheme.TEAL);
        btnThem.setOnAction(e -> openCustomerDialog(null));

        Button btnExport = buildFilledBtn("Xuất báo cáo", AppTheme.PRIMARY);
        btnExport.setOnAction(e -> handleExportReport());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(28, 28, 16, 28));
        header.getChildren().addAll(iconWrap, titleBox, spacer, lblCount, btnExport, btnDatPhong, btnThem);
        return header;
    }

    // ═════════════════════════════════════════════════════════════════
    // TOOLBAR — search + filter
    // ═════════════════════════════════════════════════════════════════

    private HBox buildToolbar() {
        // search icon label
        Label searchIcon = new Label("🔍");
        searchIcon.setFont(Font.font(14));
        searchIcon.setPadding(new Insets(0, 0, 0, 12));

        txtSearch = new TextField();
        txtSearch.setPromptText("Tìm kiếm khách hàng...");
        txtSearch.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 0;" +
                        "-fx-font-size: 13;");
        txtSearch.textProperty().addListener((obs, o, n) -> filterData());
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        HBox searchWrap = new HBox(6, searchIcon, txtSearch);
        searchWrap.setAlignment(Pos.CENTER_LEFT);
        searchWrap.setPrefHeight(40);
        searchWrap.setMaxWidth(360);
        String searchBase = "-fx-background-color: white;" +
                "-fx-border-color: " + C_BORDER + ";" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-border-width: 1.5;";
        String searchFocus = "-fx-background-color: white;" +
                "-fx-border-color: " + C_ACCENT + ";" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-border-width: 1.5;";
        searchWrap.setStyle(searchBase);
        txtSearch.focusedProperty()
                .addListener((obs, o, focused) -> searchWrap.setStyle(focused ? searchFocus : searchBase));

        cboSearchType = new ComboBox<>();
        cboSearchType.getItems().addAll("Theo mã khách hàng", "Theo số điện thoại");
        cboSearchType.setValue("Theo mã khách hàng");
        cboSearchType.setPrefHeight(40);
        cboSearchType.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-font-size: 13;");
        cboSearchType.setOnAction(e -> filterData());

        cboTrangThai = new ComboBox<>();
        cboTrangThai.getItems().addAll("Tất cả", KhachHang.TRANG_THAI_DANG_HOAT_DONG, KhachHang.TRANG_THAI_VO_HIEU_HOA);
        cboTrangThai.setValue(KhachHang.TRANG_THAI_DANG_HOAT_DONG);
        cboTrangThai.setPrefHeight(40);
        cboTrangThai.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-font-size: 13;");
        cboTrangThai.setOnAction(e -> filterData());

        HBox toolbar = new HBox(10, searchWrap, cboSearchType, cboTrangThai);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 28, 16, 28));
        return toolbar;
    }

    // ═════════════════════════════════════════════════════════════════
    // TABLE CARD
    // ═════════════════════════════════════════════════════════════════

    private VBox buildTableCard() {
        configureTable();
        tableView.getStylesheets().add(TABLE_CSS);
        tableView.getStyleClass().add("customer-table");
        VBox.setVgrow(tableView, Priority.ALWAYS);

        VBox card = new VBox(tableView);
        card.setPadding(new Insets(0, 28, 28, 28));
        VBox.setVgrow(card, Priority.ALWAYS);

        DropShadow shadow = new DropShadow(8, Color.rgb(0, 0, 0, 0.07));
        shadow.setOffsetY(2);
        tableView.setEffect(shadow);
        return card;
    }

    private void configureTable() {
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> selectedCustomer = newSelection);

        tableView.setRowFactory(tv -> {
            TableRow<KhachHang> row = new TableRow<>();
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty() && !row.isSelected())
                    row.setStyle("-fx-background-color: #ecfeff;");
            });
            row.setOnMouseExited(e -> {
                if (!row.isEmpty() && !row.isSelected())
                    row.setStyle("");
            });
            row.selectedProperty()
                    .addListener((obs, was, now) -> row.setStyle(now ? "-fx-background-color: #ccfbf1;" : ""));
            row.setOnMouseClicked(evt -> {
                if (row.isEmpty()) {
                    return;
                }
                tableView.getSelectionModel().select(row.getItem());
                tableView.getFocusModel().focus(row.getIndex());
                if (evt.getClickCount() == 2) {
                    openCustomerDialog(row.getItem());
                }
            });
            return row;
        });

        TableColumn<KhachHang, String> colMa = col("Mã KH", 90,
                KhachHang::getMaKhachHang);
        TableColumn<KhachHang, String> colTen = col("Tên khách hàng", 200,
                KhachHang::getTenKhachHang);
        TableColumn<KhachHang, String> colGT = col("Giới tính", 80,
                kh -> kh.isGioiTinh() ? "Nam" : "Nữ");
        TableColumn<KhachHang, String> colSdt = col("Số điện thoại", 130,
                KhachHang::getSoDienThoai);
        TableColumn<KhachHang, String> colDiaChi = col("Địa chỉ", -1,
                KhachHang::getDiaChi);

        TableColumn<KhachHang, String> colTrangThai = new TableColumn<>("Trạng thái");
        colTrangThai.setPrefWidth(130);
        colTrangThai.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTrangThai()));
        colTrangThai.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                    if (KhachHang.TRANG_THAI_DANG_HOAT_DONG.equals(item)) {
                        badge.setStyle(
                                "-fx-background-color: #ecfdf5; -fx-text-fill: #059669; -fx-padding: 4 10; -fx-background-radius: 20;");
                    } else {
                        badge.setStyle(
                                "-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-padding: 4 10; -fx-background-radius: 20;");
                    }
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        tableView.getColumns().setAll(colMa, colTen, colGT, colSdt, colDiaChi, colTrangThai);
    }

    private TableColumn<KhachHang, String> col(String title, double prefW,
            java.util.function.Function<KhachHang, String> getter) {
        TableColumn<KhachHang, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new SimpleStringProperty(getter.apply(d.getValue())));
        if (prefW > 0)
            c.setPrefWidth(prefW);
        return c;
    }

    // ═════════════════════════════════════════════════════════════════
    // FILTER
    // ═════════════════════════════════════════════════════════════════

    private void filterData() {
        String kw = txtSearch != null ? txtSearch.getText().trim() : "";
        String type = cboSearchType.getValue();
        String statusFilter = cboTrangThai != null ? cboTrangThai.getValue() : KhachHang.TRANG_THAI_DANG_HOAT_DONG;

        List<KhachHang> filtered = allData.stream().filter(kh -> {
            boolean matchStatus = "Tất cả".equals(statusFilter)
                    || (KhachHang.TRANG_THAI_DANG_HOAT_DONG.equals(statusFilter) && kh.isDangHoatDong())
                    || (KhachHang.TRANG_THAI_VO_HIEU_HOA.equals(statusFilter) && !kh.isDangHoatDong());
            if (!matchStatus)
                return false;

            if (kw.isBlank())
                return true;
            if ("Theo số điện thoại".equals(type)) {
                return safe(kh.getSoDienThoai()).toLowerCase().contains(kw.toLowerCase());
            } else {
                return safe(kh.getMaKhachHang()).toLowerCase().contains(kw.toLowerCase()) ||
                        safe(kh.getTenKhachHang()).toLowerCase().contains(kw.toLowerCase());
            }
        }).toList();

        tableView.getItems().setAll(filtered);
        restoreSelection();
    }

    // ═════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════

    private void openCustomerDialog(KhachHang customer) {
        new CustomerDialog(ownerWindow(), customer, service, this::loadData).show();
    }

    private void handleExportReport() {
        List<KhachHang> rows = tableView.getItems().stream()
                .filter(Objects::nonNull)
                .toList();

        if (rows.isEmpty()) {
            showMessage(Alert.AlertType.WARNING, "Xuất báo cáo", "Không có dữ liệu để xuất báo cáo.");
            return;
        }

        SaveTarget target = ReportExportUtils.chooseReportSaveTarget(
                ownerWindow(),
                "Xuất báo cáo khách hàng",
                ReportExportUtils.defaultFileName("BaoCao_KhachHang"));
        if (target == null) {
            return;
        }

        try {
            if (target.format() == ReportExportUtils.ReportFormat.PDF) {
                ReportExportUtils.exportCustomersPdf(target.file(), rows);
            } else {
                ReportExportUtils.exportCustomersExcel(target.file(), rows);
            }
            showMessage(Alert.AlertType.INFORMATION, "Xuất báo cáo", "Xuất báo cáo thành công.");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Không thể xuất báo cáo khách hàng", ex);
            showMessage(Alert.AlertType.ERROR, "Lỗi xuất báo cáo", "Không thể xuất báo cáo: " + ex.getMessage());
        }
    }

    private Window ownerWindow() {
        return tableView.getScene() != null ? tableView.getScene().getWindow() : null;
    }

    private void showMessage(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        Window owner = ownerWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void restoreSelection() {
        if (selectedCustomer == null) {
            tableView.getSelectionModel().clearSelection();
            return;
        }

        for (KhachHang customer : tableView.getItems()) {
            if (selectedCustomer.equals(customer)) {
                tableView.getSelectionModel().select(customer);
                tableView.scrollTo(customer);
                return;
            }
        }

        selectedCustomer = null;
        tableView.getSelectionModel().clearSelection();
    }

    private Button buildFilledBtn(String text, String bg) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        btn.setPrefHeight(38);
        btn.setPadding(new Insets(0, 18, 0, 18));
        String base = "-fx-background-color:" + bg + ";-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),6,0,0,2);";
        String hover = "-fx-background-color:derive(" + bg
                + ",-12%);-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),10,0,0,3);";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    // ─── Table CSS via data URI ──────────────────────────────────────
    private static String buildTableCss() {
        String css = ".customer-table{" +
                "-fx-background-color:white;" +
                "-fx-border-color:#e2e8f0;" +
                "-fx-border-radius:12;" +
                "-fx-background-radius:12;" +
                "}" +
                ".customer-table .column-header-background{" +
                "-fx-background-color:#f8fafc;" +
                "-fx-background-radius:12 12 0 0;" +
                "}" +
                ".customer-table .column-header{" +
                "-fx-background-color:transparent;" +
                "-fx-border-color:transparent transparent #e2e8f0 transparent;" +
                "-fx-size:46;" +
                "}" +
                ".customer-table .column-header .label{" +
                "-fx-text-fill:#475569;" +
                "-fx-font-weight:bold;" +
                "-fx-font-size:12px;" +
                "-fx-padding:0 14 0 14;" +
                "}" +
                ".customer-table .table-cell{" +
                "-fx-padding:11 14 11 14;" +
                "-fx-border-color:transparent transparent #f1f5f9 transparent;" +
                "-fx-font-size:13px;" +
                "-fx-text-fill:#334155;" +
                "}" +
                ".customer-table .table-row-cell{" +
                "-fx-background-color:white;" +
                "-fx-cell-size:46px;" +
                "}" +
                ".customer-table .table-row-cell:odd{" +
                "-fx-background-color:#fafbfd;" +
                "}" +
                ".customer-table .table-row-cell:selected .table-cell{" +
                "-fx-text-fill:#1e40af;" +
                "}" +
                ".customer-table .placeholder .label{" +
                "-fx-text-fill:#94a3b8;" +
                "-fx-font-size:14px;" +
                "}";
        try {
            return "data:text/css," + URLEncoder.encode(css, StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception ex) {
            return "";
        }
    }
}
