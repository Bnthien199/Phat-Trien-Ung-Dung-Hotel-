package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Callback;

import connectDB.ConnectDB;
import dao.NhanVienDAO;
import entity.NhanVien;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import util.AppTheme;

public class AccountView {

    private static final String BG_LIGHT = AppTheme.BG_LIGHT;
    private static final String PRIMARY = AppTheme.PRIMARY;
    private static final String SUCCESS = AppTheme.SUCCESS;
    private static final String DANGER = AppTheme.DANGER;
    private static final String INFO = AppTheme.INFO;
    private static final String TEXT_DARK = AppTheme.TEXT;
    private static final String TEXT_SUB = AppTheme.MUTED;
    private static final String SURFACE = AppTheme.SURFACE;
    private static final String BORDER = AppTheme.BORDER;
    private static final String SECONDARY = AppTheme.SECONDARY;
    private static final String SECONDARY_HOVER = AppTheme.SECONDARY_HOVER;
    private static final String SECONDARY_TEXT = AppTheme.SECONDARY_TEXT;
    private static final String TABLE_CSS = buildTableCss();

    private TableView<NhanVien> tableView;
    private ObservableList<NhanVien> dataList = FXCollections.observableArrayList();
    private List<NhanVien> allData = new ArrayList<>();
    private TextField txtSearch;
    private Label lblCount;
    private NhanVienDAO dao = new NhanVienDAO();

    public Node createView() {
        if (System.nanoTime() >= 0) {
            return createModernView();
        }
        VBox root = new VBox(20);
        root.setPadding(new Insets(28, 30, 28, 30));
        root.setStyle("-fx-background-color: " + BG_LIGHT + ";");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblIcon = new Label("👥");
        lblIcon.setFont(Font.font(26));

        VBox headerText = new VBox(2);
        Label lblTitle = new Label("QUẢN LÝ TÀI KHOẢN");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        lblTitle.setTextFill(Color.web(TEXT_DARK));

        Label lblSub = new Label("Quản lý thông tin tài khoản nhân viên hệ thống");
        lblSub.setFont(Font.font("Segoe UI", 13));
        lblSub.setTextFill(Color.web(TEXT_SUB));

        headerText.getChildren().addAll(lblTitle, lblSub);
        header.getChildren().addAll(lblIcon, headerText);

        Label lblNote = new Label("🔒  Chỉ Admin mới có quyền quản lý tài khoản nhân viên.");
        lblNote.setFont(Font.font("Segoe UI", 13));
        lblNote.setTextFill(Color.web("#b45309"));
        lblNote.setStyle("-fx-background-color: #fef3c7; -fx-padding: 10 16; -fx-background-radius: 10;");

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍  Tìm theo tên / tên đăng nhập...");
        txtSearch.setPrefWidth(300);
        txtSearch.setPrefHeight(40);
        txtSearch.setStyle(
                "-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #d1d5db; -fx-padding: 0 12; -fx-font-size: 14;");
        txtSearch.textProperty().addListener((obs, old, val) -> handleSearch());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = createStyledButton("➕  Thêm tài khoản", SUCCESS);
        btnAdd.setOnAction(e -> handleAdd());

        Button btnRefresh = createStyledButton("🔄  Làm mới", PRIMARY);
        btnRefresh.setOnAction(e -> handleRefresh());

        toolbar.getChildren().addAll(txtSearch, spacer, btnAdd, btnRefresh);

        tableView = new TableView<>(dataList);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableView.setPlaceholder(new Label("Chưa có dữ liệu tài khoản."));

        // Double click vào dòng để mở form xem/chỉnh sửa
        tableView.setRowFactory(tv -> {
            TableRow<NhanVien> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showEditDialog(row.getItem());
                }
            });
            return row;
        });

        TableColumn<NhanVien, String> colMa = new TableColumn<>("Mã NV");
        colMa.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMaNhanVien()));
        colMa.setPrefWidth(80);

        TableColumn<NhanVien, String> colTen = new TableColumn<>("Họ và Tên");
        colTen.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTenNhanVien()));

        TableColumn<NhanVien, String> colUser = new TableColumn<>("Tên Đăng Nhập");
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTenDangNhap()));

        TableColumn<NhanVien, String> colVT = new TableColumn<>("Vai Trò");
        colVT.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVaiTro()));
        colVT.setPrefWidth(80);

        TableColumn<NhanVien, String> colSDT = new TableColumn<>("SĐT");
        colSDT.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSoDienThoai()));

        TableColumn<NhanVien, String> colCV = new TableColumn<>("Chức Vụ");
        colCV.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getChucVu()));

        TableColumn<NhanVien, Void> colAction = new TableColumn<>("Xóa");
        colAction.setPrefWidth(70);
        colAction.setSortable(false);
        colAction.setCellFactory(createActionCellFactory());

        tableView.getColumns().addAll(colMa, colTen, colUser, colVT, colSDT, colCV, colAction);

        root.getChildren().addAll(header, lblNote, toolbar, tableView);
        return root;
    }

    private Node createModernView() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + BG_LIGHT + ";");

        txtSearch = new TextField();
        txtSearch.setPromptText("Tìm theo mã, tên hoặc tài khoản...");
        txtSearch.textProperty().addListener((obs, old, val) -> handleSearch());

        Button btnAdd = createPrimaryButton("Thêm mới", PRIMARY);
        btnAdd.setOnAction(e -> handleAdd());

        Button btnRefresh = createSecondaryButton("Làm mới");
        btnRefresh.setOnAction(e -> handleRefresh());

        tableView = new TableView<>(dataList);
        configureModernTable();

        TableColumn<NhanVien, String> colMa = textCol("Mã NV", nv -> nv.getMaNhanVien(), 96, Pos.CENTER);
        TableColumn<NhanVien, String> colTen = textCol("Họ và tên", nv -> nv.getTenNhanVien(), 180, Pos.CENTER_LEFT);
        TableColumn<NhanVien, String> colUser = textCol("Tên đăng nhập", nv -> nv.getTenDangNhap(), 160,
                Pos.CENTER_LEFT);
        TableColumn<NhanVien, String> colVT = new TableColumn<>("Vai trò");
        colVT.setPrefWidth(120);
        colVT.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVaiTro()));
        colVT.setCellFactory(col -> createRoleBadgeCell());

        TableColumn<NhanVien, String> colSDT = textCol("SĐT", nv -> nv.getSoDienThoai(), 130, Pos.CENTER);
        TableColumn<NhanVien, String> colCV = textCol("Chức vụ", nv -> nv.getChucVu(), 160, Pos.CENTER_LEFT);

        TableColumn<NhanVien, Void> colAction = new TableColumn<>("");
        colAction.setPrefWidth(72);
        colAction.setSortable(false);
        colAction.setCellFactory(createModernActionCellFactory());

        tableView.getColumns().setAll(colMa, colTen, colUser, colVT, colSDT, colCV, colAction);

        root.getChildren().addAll(
                buildModernHeader(btnAdd),
                buildModernToolbar(btnRefresh),
                buildModernTableCard());
        loadData();
        return root;
    }

    private HBox buildModernHeader(Button btnAdd) {
        Label icon = new Label("👥");
        icon.setFont(Font.font(18));
        icon.setTextFill(Color.web(PRIMARY));

        StackPane iconWrap = new StackPane(icon);
        iconWrap.setPrefSize(44, 44);
        iconWrap.setMinSize(44, 44);
        iconWrap.setMaxSize(44, 44);
        iconWrap.setStyle("-fx-background-color: #ccfbf1; -fx-background-radius: 22;");

        Label lblTitle = new Label("Quản lý tài khoản");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblTitle.setTextFill(Color.web(TEXT_DARK));

        Label lblSub = new Label(
                "Danh sách tài khoản nhân viên theo cùng chuẩn spacing, table và badge của toàn hệ thống.");
        lblSub.setFont(Font.font("Segoe UI", 13));
        lblSub.setTextFill(Color.web(TEXT_SUB));

        VBox titleBox = new VBox(2, lblTitle, lblSub);

        lblCount = new Label("— tài khoản");
        lblCount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblCount.setTextFill(Color.web(PRIMARY));
        lblCount.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 999; -fx-padding: 4 14;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(14, iconWrap, titleBox, spacer, lblCount, btnAdd);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(28, 28, 16, 28));
        return header;
    }

    private HBox buildModernToolbar(Button btnRefresh) {
        Region searchWrap = createModernSearchField();
        searchWrap.setMinSize(360, 40);
        searchWrap.setPrefSize(360, 40);
        searchWrap.setMaxSize(360, 40);
        btnRefresh.setMinHeight(38);
        btnRefresh.setPrefHeight(38);

        HBox toolbar = new HBox(10, searchWrap, btnRefresh);
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
        String normal = inputWrapStyle(BORDER);
        String focused = inputWrapStyle(PRIMARY);
        searchWrap.setStyle(normal);
        txtSearch.focusedProperty()
                .addListener((obs, old, focusedNow) -> searchWrap.setStyle(focusedNow ? focused : normal));
        return searchWrap;
    }

    private VBox buildModernTableCard() {
        VBox.setVgrow(tableView, Priority.ALWAYS);

        Label title = new Label("Danh sách tài khoản");
        title.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        title.setTextFill(Color.web(TEXT_DARK));

        Label hint = new Label("Vai trò hiển thị bằng badge nhẹ, thao tác xóa chuyển thành icon button gọn hơn.");
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

    private void configureModernTable() {
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setPlaceholder(new Label("Chưa có dữ liệu tài khoản."));
        tableView.getStylesheets().add(TABLE_CSS);
        tableView.getStyleClass().add("module-table");
        VBox.setVgrow(tableView, Priority.ALWAYS);

        tableView.setRowFactory(tv -> {
            TableRow<NhanVien> row = new TableRow<>();
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
            row.selectedProperty().addListener(
                    (obs, old, selected) -> row.setStyle(selected ? "-fx-background-color: #ecfeff;" : ""));
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showEditDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private TableColumn<NhanVien, String> textCol(String title,
            java.util.function.Function<NhanVien, String> provider,
            double width,
            Pos alignment) {
        TableColumn<NhanVien, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(d -> new SimpleStringProperty(provider.apply(d.getValue())));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setAlignment(alignment);
            }
        });
        return col;
    }

    private TableCell<NhanVien, String> createRoleBadgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    return;
                }

                Label badge = new Label(normalizeRoleLabel(item));
                badge.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
                badge.setPadding(new Insets(4, 10, 4, 10));
                badge.setStyle(roleBadgeStyle(item));
                HBox wrap = new HBox(badge);
                wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private Callback<TableColumn<NhanVien, Void>, TableCell<NhanVien, Void>> createModernActionCellFactory() {
        return col -> new TableCell<>() {
            private final Button btnDelete = createGhostDangerIconButton();
            private final HBox box = new HBox(btnDelete);

            {
                box.setAlignment(Pos.CENTER);
                btnDelete.setOnAction(e -> {
                    NhanVien nv = getTableView().getItems().get(getIndex());
                    handleDelete(nv);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
                setText(null);
            }
        };
    }

    private Button createGhostDangerIconButton() {
        Button btn = new Button("🗑");
        btn.setMinSize(30, 30);
        btn.setPrefSize(30, 30);
        btn.setMaxSize(30, 30);
        btn.setFont(Font.font("Segoe UI", 12));
        btn.setTextFill(Color.web(DANGER));

        String normal = "-fx-background-color: #fef2f2;" +
                "-fx-text-fill: " + DANGER + ";" +
                "-fx-border-color: #fecaca;" +
                "-fx-border-radius: 999;" +
                "-fx-background-radius: 999;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 0;";
        String hover = "-fx-background-color: #fee2e2;" +
                "-fx-text-fill: " + DANGER + ";" +
                "-fx-border-color: #fca5a5;" +
                "-fx-border-radius: 999;" +
                "-fx-background-radius: 999;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 0;";

        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(normal));
        return btn;
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

    private String inputWrapStyle(String borderColor) {
        return "-fx-background-color: white;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-border-width: 1.5;";
    }

    private String normalizeRoleLabel(String role) {
        String value = role == null ? "" : role.trim();
        if (value.equalsIgnoreCase("admin") || value.equalsIgnoreCase("Quản lý") || value.equalsIgnoreCase("quản lý")) {
            return "ADMIN";
        }
        if (value.equalsIgnoreCase("Lễ_tân") || value.equalsIgnoreCase("Lễ tân")) {
            return "LE_TAN";
        }
        if (value.equalsIgnoreCase("Kế_toán") || value.equalsIgnoreCase("Kế toán")) {
            return "KE_TOAN";
        }
        if (value.equalsIgnoreCase("user")) {
            return "LE_TAN";
        }
        return value.toUpperCase();
    }

    private String roleBadgeStyle(String role) {
        String normalized = normalizeRoleLabel(role);
        if ("ADMIN".equals(normalized)) {
            return "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-background-radius: 999;";
        }
        if ("KE_TOAN".equals(normalized)) {
            return "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-background-radius: 999;";
        }
        return "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-background-radius: 999;";
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

    private Callback<TableColumn<NhanVien, Void>, TableCell<NhanVien, Void>> createActionCellFactory() {
        return col -> new TableCell<NhanVien, Void>() {
            private final Button btnDelete = createIconButton("✕", DANGER, 26, 26);
            private final HBox box = new HBox(btnDelete);

            {
                box.setAlignment(Pos.CENTER);
                btnDelete.setOnAction(e -> {
                    NhanVien nv = getTableView().getItems().get(getIndex());
                    handleDelete(nv);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        };
    }

    private Button createIconButton(String icon, String bgColor, double w, double h) {
        Button btn = new Button(icon);
        btn.setMinSize(w, h);
        btn.setPrefSize(w, h);
        btn.setMaxSize(w, h);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        btn.setTextFill(Color.WHITE);

        String normal = "-fx-background-color: " + bgColor + ";"
                + "-fx-background-radius: 7;"
                + "-fx-cursor: hand;"
                + "-fx-padding: 0;";

        String hover = "-fx-background-color: derive(" + bgColor + ", -15%);"
                + "-fx-background-radius: 7;"
                + "-fx-cursor: hand;"
                + "-fx-padding: 0;";

        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(normal));
        return btn;
    }

    private Button createIconButton(String icon, String bgColor) {
        return createIconButton(icon, bgColor, 34, 34);
    }

    private HBox infoRow(String label, String value) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lbl.setMinWidth(160);
        lbl.setTextFill(Color.web(TEXT_SUB));

        Label val = new Label(value != null ? value : "—");
        val.setFont(Font.font("Segoe UI", 14));
        val.setTextFill(Color.web(TEXT_DARK));

        HBox row = new HBox(12, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 0, 6, 0));
        row.setStyle("-fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");
        return row;
    }

    private void showEditDialog(NhanVien nv) {
        final NhanVien originalData = cloneNhanVien(nv);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Cập nhật tài khoản — " + nv.getMaNhanVien());
        dialog.setHeaderText(null);

        VBox content = new VBox(10);
        content.setPadding(new Insets(24));
        content.setPrefWidth(450);

        Label lblHeader = new Label("CẬP NHẬT TÀI KHOẢN");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblHeader.setTextFill(Color.web(PRIMARY));

        TextField txtMa = field(nv.getMaNhanVien());
        txtMa.setEditable(false);
        txtMa.setStyle(txtMa.getStyle() + "-fx-background-color: #f1f5f9;");

        TextField txtTen = field(nv.getTenNhanVien());
        TextField txtUser = field(nv.getTenDangNhap());
        TextField txtSDT = field(nv.getSoDienThoai());
        TextField txtChucVu = field(nv.getChucVu());

        ComboBox<String> cbVaiTro = new ComboBox<>(FXCollections.observableArrayList("Admin", "User"));
        cbVaiTro.setValue(nv.getVaiTro() != null ? nv.getVaiTro() : "User");
        cbVaiTro.setPrefHeight(40);
        cbVaiTro.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(
                lblHeader,
                infoRow("👤  Mã nhân viên:", nv.getMaNhanVien()),
                lbl("Họ và tên"), txtTen,
                lbl("Tên đăng nhập"), txtUser,
                lbl("Vai trò"), cbVaiTro,
                lbl("Số điện thoại"), txtSDT,
                lbl("Chức vụ"), txtChucVu);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(500);
        scroll.setStyle("-fx-background-color: transparent;");
        dialog.getDialogPane().setContent(scroll);

        ButtonType btnUpdate = new ButtonType("Cập nhật", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnUpdate, ButtonType.CANCEL);

        util.ModalUtils.setupDialogBehavior(dialog, ButtonType.CANCEL, true, true, () -> {
            try {
                NhanVien updated = new NhanVien(
                        txtMa.getText().trim(),
                        txtTen.getText().trim(),
                        txtChucVu.getText().trim(),
                        originalData.getLuongNhanVien(),
                        originalData.isPhaiNhanVien(),
                        txtSDT.getText().trim(),
                        txtUser.getText().trim(),
                        originalData.getMatKhau(),
                        cbVaiTro.getValue());
                return hasAccountChanges(originalData, updated);
            } catch (Exception e) {
                return false;
            }
        });

        Button updateBtn = (Button) dialog.getDialogPane().lookupButton(btnUpdate);
        if (updateBtn != null) {
            updateBtn.setStyle(
                    "-fx-background-color:" + PRIMARY + ";"
                            + "-fx-text-fill:white;"
                            + "-fx-font-weight:bold;"
                            + "-fx-background-radius:8;"
                            + "-fx-cursor:hand;"
                            + "-fx-padding:8 20;");
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == btnUpdate) {
            try {
                NhanVien updated = new NhanVien(
                        txtMa.getText().trim(),
                        txtTen.getText().trim(),
                        txtChucVu.getText().trim(),
                        originalData.getLuongNhanVien(),
                        originalData.isPhaiNhanVien(),
                        txtSDT.getText().trim(),
                        txtUser.getText().trim(),
                        originalData.getMatKhau(),
                        cbVaiTro.getValue());

                if (!hasAccountChanges(originalData, updated)) {
                    alert(Alert.AlertType.INFORMATION, "Thông báo", "Không có thông tin nào được thay đổi.");
                    return;
                }

                if (dao.updateNhanVien(updated)) {
                    alert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật tài khoản " + nv.getMaNhanVien());
                    loadData();
                } else {
                    alert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật.");
                }
            } catch (IllegalArgumentException ex) {
                alert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", ex.getMessage());
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, "Lỗi", ex.getMessage());
            }
        }
    }

    private NhanVien cloneNhanVien(NhanVien nv) {
        return new NhanVien(
                nv.getMaNhanVien(),
                nv.getTenNhanVien(),
                nv.getChucVu(),
                nv.getLuongNhanVien(),
                nv.isPhaiNhanVien(),
                nv.getSoDienThoai(),
                nv.getTenDangNhap(),
                nv.getMatKhau(),
                nv.getVaiTro());
    }

    private boolean hasAccountChanges(NhanVien original, NhanVien updated) {
        if (original == null || updated == null) {
            return true;
        }

        return !Objects.equals(normalizeValue(original.getMaNhanVien()), normalizeValue(updated.getMaNhanVien()))
                || !Objects.equals(normalizeValue(original.getTenNhanVien()), normalizeValue(updated.getTenNhanVien()))
                || !Objects.equals(normalizeValue(original.getTenDangNhap()), normalizeValue(updated.getTenDangNhap()))
                || !Objects.equals(normalizeValue(original.getMatKhau()), normalizeValue(updated.getMatKhau()))
                || !Objects.equals(normalizeValue(original.getVaiTro()), normalizeValue(updated.getVaiTro()))
                || !Objects.equals(normalizeValue(original.getSoDienThoai()), normalizeValue(updated.getSoDienThoai()))
                || !Objects.equals(normalizeValue(original.getChucVu()), normalizeValue(updated.getChucVu()))
                || original.isPhaiNhanVien() != updated.isPhaiNhanVien()
                || Double.compare(original.getLuongNhanVien(), updated.getLuongNhanVien()) != 0;
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private void handleAdd() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Thêm tài khoản mới");
        dialog.setHeaderText(null);

        VBox content = new VBox(10);
        content.setPadding(new Insets(24));
        content.setPrefWidth(450);

        Label lblHeader = new Label("➕  THÊM TÀI KHOẢN MỚI");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblHeader.setTextFill(Color.web(SUCCESS));

        TextField txtMa = field("");
        txtMa.setPromptText("VD: NV011");

        TextField txtTen = field("");
        txtTen.setPromptText("Họ và tên");

        TextField txtUser = field("");
        txtUser.setPromptText("Tên đăng nhập");

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Mật khẩu (tối thiểu 6 ký tự)");
        txtPass.setPrefHeight(40);
        txtPass.setStyle(
                "-fx-background-radius:8;-fx-border-radius:8;-fx-border-color:#d1d5db;-fx-font-size:14;-fx-padding:0 12;");

        TextField txtSDT = field("");
        txtSDT.setPromptText("VD: 0901234567");

        TextField txtChucVu = field("");
        txtChucVu.setPromptText("VD: Lễ tân");

        ComboBox<String> cbVaiTro = new ComboBox<>(FXCollections.observableArrayList("Admin", "User"));
        cbVaiTro.setValue("User");
        cbVaiTro.setPrefHeight(40);
        cbVaiTro.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(
                lblHeader,
                lbl("Mã nhân viên"), txtMa,
                lbl("Họ và tên"), txtTen,
                lbl("Tên đăng nhập"), txtUser,
                lbl("Mật khẩu"), txtPass,
                lbl("Vai trò"), cbVaiTro,
                lbl("Số điện thoại"), txtSDT,
                lbl("Chức vụ"), txtChucVu);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(500);
        scroll.setStyle("-fx-background-color: transparent;");
        dialog.getDialogPane().setContent(scroll);

        ButtonType btnSave = new ButtonType("✅  Tạo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        util.ModalUtils.setupDialogBehavior(dialog, ButtonType.CANCEL, true, true, () -> {
            return !txtMa.getText().trim().isEmpty() || !txtTen.getText().trim().isEmpty()
                    || !txtUser.getText().trim().isEmpty() || !txtPass.getText().trim().isEmpty()
                    || !txtSDT.getText().trim().isEmpty() || !txtChucVu.getText().trim().isEmpty();
        });

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(btnSave);
        if (saveBtn != null) {
            saveBtn.setStyle("-fx-background-color:" + SUCCESS
                    + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 20;");
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == btnSave) {
            try {
                NhanVien nv = new NhanVien(
                        txtMa.getText().trim(),
                        txtTen.getText().trim(),
                        txtChucVu.getText().trim(),
                        0,
                        true,
                        txtSDT.getText().trim(),
                        txtUser.getText().trim(),
                        txtPass.getText().trim(),
                        cbVaiTro.getValue());

                if (dao.addNhanVien(nv)) {
                    alert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm tài khoản " + nv.getMaNhanVien());
                    loadData();
                } else {
                    alert(Alert.AlertType.ERROR, "Lỗi", "Không thể thêm. Mã NV có thể đã tồn tại.");
                }
            } catch (IllegalArgumentException ex) {
                alert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", ex.getMessage());
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, "Lỗi", ex.getMessage());
            }
        }
    }

    private void handleDelete(NhanVien nv) {
        if (nv == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa tài khoản " + nv.getMaNhanVien() + "?");
        confirm.setContentText(nv.getTenNhanVien() + " (" + nv.getTenDangNhap() + ")\nKhông thể hoàn tác!");

        ButtonType btnYes = new ButtonType("🗑  Xóa", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);

        Button delBtn = (Button) confirm.getDialogPane().lookupButton(btnYes);
        if (delBtn != null) {
            delBtn.setStyle("-fx-background-color:" + DANGER
                    + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;");
        }

        confirm.showAndWait().ifPresent(type -> {
            if (type == btnYes) {
                try {
                    if (dao.deleteNhanVien(nv.getMaNhanVien())) {
                        alert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa " + nv.getMaNhanVien());
                        loadData();
                    } else {
                        alert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa.");
                    }
                } catch (Exception ex) {
                    alert(Alert.AlertType.ERROR, "Lỗi", ex.getMessage());
                }
            }
        });
    }

    public void loadData() {
        setTableLoading(true);
        Task<List<NhanVien>> task = new Task<>() {
            @Override
            protected List<NhanVien> call() {
                if (ConnectDB.getConnection() == null) {
                    return new ArrayList<>();
                }
                return dao.getAllNhanVien();
            }
        };

        task.setOnSucceeded(e -> {
            List<NhanVien> list = task.getValue();
            allData = list != null ? list : new ArrayList<>();
            setTableLoading(false);
            handleSearch();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null)
                ex.printStackTrace();
            allData = new ArrayList<>();
            dataList.clear();
            if (lblCount != null) {
                lblCount.setText("0 tài khoản");
            }
            setTableLoading(false);
        });

        Thread loader = new Thread(task, "account-load");
        loader.setDaemon(true);
        loader.start();
    }

    private void handleRefresh() {
        if (txtSearch != null) {
            txtSearch.clear();
        }
        loadData();
    }

    private void setTableLoading(boolean loading) {
        if (tableView != null) {
            tableView.setPlaceholder(new Label(loading
                    ? "\u0110ang t\u1ea3i d\u1eef li\u1ec7u..."
                    : "Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u t\u00e0i kho\u1ea3n."));
        }
        if (loading) {
            dataList.clear();
        }
    }

    private void handleSearch() {
        String kw = txtSearch.getText().trim().toLowerCase();
        if (kw.isEmpty()) {
            dataList.setAll(allData);
            if (lblCount != null) {
                lblCount.setText(dataList.size() + " tài khoản");
            }
            return;
        }

        dataList.setAll(allData.stream()
                .filter(nv -> has(nv.getTenNhanVien(), kw) ||
                        has(nv.getTenDangNhap(), kw) ||
                        has(nv.getMaNhanVien(), kw))
                .collect(Collectors.toList()));
        if (lblCount != null) {
            lblCount.setText(dataList.size() + " tài khoản");
        }
    }

    private boolean has(String s, String kw) {
        return s != null && s.toLowerCase().contains(kw);
    }

    private Label lbl(String t) {
        Label l = new Label(t);
        l.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        l.setTextFill(Color.web(TEXT_SUB));
        return l;
    }

    private TextField field(String v) {
        TextField t = new TextField(v);
        t.setPrefHeight(40);
        t.setStyle(
                "-fx-background-radius:8;-fx-border-radius:8;-fx-border-color:#d1d5db;-fx-font-size:14;-fx-padding:0 12;");
        return t;
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

    private void alert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
