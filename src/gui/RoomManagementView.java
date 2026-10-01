package gui;

import dao.PhongDAO;
import entity.Phong;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import util.AppTheme;
import util.RoomStatus;
import util.StatusUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RoomManagementView {

    private static final String C_BG = AppTheme.BG_LIGHT;
    private static final String C_PRIMARY = AppTheme.PRIMARY;
    private static final String C_ACCENT = AppTheme.TEAL_DARK;
    private static final String C_WARNING = AppTheme.WARNING;
    private static final String C_BORDER = AppTheme.BORDER;
    private static final String C_TEXT = AppTheme.TEXT;
    private static final String C_MUTED = AppTheme.MUTED;
    private static final Pattern ROOM_CODE_PATTERN = Pattern.compile("^P\\d{3}$");
    private static final String TABLE_CSS = buildTableCss();
    private static final String ADD_ROOM_TYPE_LABEL = "+ Thêm loại phòng";

    private final PhongDAO dao = new PhongDAO();
    private final TableView<Phong> tableView = new TableView<>();

    private List<Phong> allData = List.of();
    private List<PhongDAO.LoaiPhongItem> roomTypeOptions = new ArrayList<>();
    private final Map<String, String> maLoaiByLoaiPhong = new LinkedHashMap<>();
    private final Map<String, String> loaiPhongByMaLoai = new LinkedHashMap<>();

    private TextField txtSearch;
    private TextField txtMa;
    private TextField txtSoNguoi;
    private TextField txtTang;
    private TextField txtGia;
    private ComboBox<String> cbLoaiPhong;
    private ComboBox<String> cbMaLoai;
    private ComboBox<String> cbTrangThai;
    private Button btnThem;
    private Button btnCapNhat;
    private Button btnLamMoiForm;
    private Label lblFormRoomCode;

    private Phong originalRoomSnapshot;
    private String pendingSelectionCode;
    private boolean suppressRoomTypeSync;
    private boolean openingRoomTypeDialog;

    public Node createView() {
        HBox root = new HBox(20);
        root.setPadding(new Insets(0, 20, 20, 0));
        root.setStyle("-fx-background-color: " + C_BG + ";");

        VBox left = buildLeftPanel();
        HBox.setHgrow(left, Priority.ALWAYS);

        VBox right = buildRightPanel();
        right.setPrefWidth(300);
        right.setMinWidth(280);
        right.setMaxWidth(320);

        root.getChildren().addAll(left, right);
        resetDetailForm(true);
        loadData();
        return root;
    }

    public void loadData() {
        setTableLoading(true);
        Task<List<Phong>> task = new Task<>() {
            @Override
            protected List<Phong> call() {
                return dao.getAllPhong();
            }
        };
        task.setOnSucceeded(e -> {
            allData = task.getValue() != null ? task.getValue() : List.of();
            setTableLoading(false);
            refreshRoomTypeControls(trimToNull(cbLoaiPhong != null ? cbLoaiPhong.getValue() : null),
                    trimToNull(cbMaLoai != null ? cbMaLoai.getValue() : null), true);
            filterData();
            restorePendingSelection();
        });
        task.setOnFailed(e -> {
            allData = List.of();
            tableView.getItems().clear();
            setTableLoading(false);
            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
            showError("Lỗi tải dữ liệu", ex != null ? ex.getMessage() : "Không thể tải danh sách phòng.");
        });
        Thread loader = new Thread(task, "room-management-load");
        loader.setDaemon(true);
        loader.start();
    }

    private void handleRefresh() {
        if (txtSearch != null) {
            txtSearch.clear();
        }
        Phong selected = tableView.getSelectionModel().getSelectedItem();
        pendingSelectionCode = selected != null ? selected.getMaPhong()
                : (originalRoomSnapshot != null ? originalRoomSnapshot.getMaPhong() : null);
        loadData();
    }

    private void setTableLoading(boolean loading) {
        tableView.setPlaceholder(new Label(loading
                ? "Đang tải dữ liệu..."
                : "Chưa có dữ liệu phòng."));
        if (loading) {
            tableView.getItems().clear();
        }
    }

    private VBox buildLeftPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: " + C_BG + ";");
        panel.getChildren().addAll(buildPageHeader(), buildToolbar(), buildTableCard());
        VBox.setVgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private HBox buildPageHeader() {
        Circle avatar = new Circle(22);
        avatar.setFill(Color.web(AppTheme.TEAL_LIGHT));
        Label icon = new Label("\uD83D\uDECF");
        icon.setFont(Font.font(18));
        icon.setTextFill(Color.web(C_PRIMARY));
        StackPane iconWrap = new StackPane(avatar, icon);
        iconWrap.setPrefSize(44, 44);

        Label lblTitle = new Label("Quản lý phòng");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblTitle.setTextFill(Color.web(C_TEXT));

        Label lblSub = new Label("Thêm, sửa, xóa và quản lý danh sách phòng khách sạn");
        lblSub.setFont(Font.font("Segoe UI", 13));
        lblSub.setTextFill(Color.web(C_MUTED));

        VBox titleBox = new VBox(2, lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(28, 20, 16, 28));
        header.getChildren().addAll(iconWrap, titleBox, spacer);
        return header;
    }

    private HBox buildToolbar() {
        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.setFont(Font.font(14));
        searchIcon.setPadding(new Insets(0, 0, 0, 12));

        txtSearch = new TextField();
        txtSearch.setPromptText("Tìm theo mã phòng, loại phòng hoặc tầng...");
        txtSearch.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 0;" +
                        "-fx-font-size: 13;"
        );
        txtSearch.textProperty().addListener((obs, o, n) -> filterData());
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        HBox searchWrap = new HBox(6, searchIcon, txtSearch);
        searchWrap.setAlignment(Pos.CENTER_LEFT);
        searchWrap.setPrefHeight(40);
        String searchBase =
                "-fx-background-color: white;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-width: 1.5;";
        String searchFocused =
                "-fx-background-color: white;" +
                        "-fx-border-color: " + C_ACCENT + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-width: 1.5;";
        searchWrap.setStyle(searchBase);
        txtSearch.focusedProperty().addListener((obs, o, f) -> searchWrap.setStyle(f ? searchFocused : searchBase));
        HBox.setHgrow(searchWrap, Priority.ALWAYS);

        Button btnRefresh = outlineBtn("\u21BB  Làm mới", C_PRIMARY);
        btnRefresh.setOnAction(e -> handleRefresh());

        HBox toolbar = new HBox(10, searchWrap, btnRefresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 20, 14, 28));
        return toolbar;
    }

    private VBox buildTableCard() {
        configureTable();
        tableView.getStylesheets().add(TABLE_CSS);
        tableView.getStyleClass().add("room-table");
        VBox.setVgrow(tableView, Priority.ALWAYS);

        DropShadow shadow = new DropShadow(8, Color.rgb(0, 0, 0, 0.07));
        shadow.setOffsetY(2);
        tableView.setEffect(shadow);

        VBox card = new VBox(tableView);
        card.setPadding(new Insets(0, 20, 0, 28));
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private void configureTable() {
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        tableView.setRowFactory(tv -> {
            TableRow<Phong> row = new TableRow<>();
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty() && !row.isSelected()) {
                    row.setStyle("-fx-background-color: #ecfeff;");
                }
            });
            row.setOnMouseExited(e -> {
                if (!row.isEmpty() && !row.isSelected()) {
                    row.setStyle("");
                }
            });
            row.selectedProperty().addListener((obs, was, now) ->
                    row.setStyle(now ? "-fx-background-color: #ccfbf1;" : ""));
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) {
                    fillForm(row.getItem());
                }
            });
            return row;
        });

        TableColumn<Phong, String> colMa = col("Mã phòng", 80, Phong::getMaPhong);
        TableColumn<Phong, String> colLoai = col("Loại phòng", 90, p -> safeText(p.getLoaiPhong()));
        TableColumn<Phong, String> colNguoi = col("Số người", 70, p -> String.valueOf(p.getSoNguoi()));
        TableColumn<Phong, String> colTang = col("Tầng", 55, p -> String.valueOf(p.getTang()));
        TableColumn<Phong, String> colGia = col("Giá gốc", 110, p -> String.format("%,.0f VND", p.getGiaPhong()));

        TableColumn<Phong, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setPrefWidth(120);
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(StatusUtils.roomLabel(d.getValue().getTrangThai())));
        colStatus.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label badge = new Label("\u25CF  " + item);
                badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                String color;
                String bg;
                switch (StatusUtils.roomCode(item)) {
                    case "DANG_O" -> {
                        color = AppTheme.SUCCESS;
                        bg = "#d1fae5";
                    }
                    case "CHO_XAC_NHAN" -> {
                        color = C_WARNING;
                        bg = "#fffbeb";
                    }
                    case "SUA_CHUA" -> {
                        color = C_MUTED;
                        bg = "#f1f5f9";
                    }
                    default -> {
                        color = AppTheme.TEAL;
                        bg = "#f0fdfa";
                    }
                }
                badge.setTextFill(Color.web(color));
                badge.setStyle("-fx-background-color:" + bg + ";" +
                        "-fx-background-radius:6; -fx-padding: 3 10;");
                setGraphic(badge);
            }
        });

        TableColumn<Phong, Void> colAction = new TableColumn<>("Xóa");
        colAction.setPrefWidth(80);
        colAction.setMinWidth(70);
        colAction.setCellFactory(c -> new TableCell<>() {
            private final Button btn = buildRowTrashBtn();
            {
                btn.setOnAction(e -> {
                    Phong p = getTableView().getItems().get(getIndex());
                    if (p != null) {
                        handleXoa(p);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tableView.getColumns().setAll(colMa, colLoai, colNguoi, colTang, colStatus, colGia, colAction);
    }

    private VBox buildRightPanel() {
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-width: 1;"
        );
        DropShadow shadow = new DropShadow(12, Color.rgb(0, 0, 0, 0.08));
        shadow.setOffsetY(4);
        card.setEffect(shadow);

        card.getChildren().addAll(buildFormHeader(), buildFormBody(), buildFormFooter());

        VBox outer = new VBox(card);
        outer.setPadding(new Insets(28, 20, 0, 0));
        VBox.setVgrow(card, Priority.ALWAYS);
        return outer;
    }

    private VBox buildFormHeader() {
        VBox header = new VBox(6);
        header.setPadding(new Insets(20, 20, 16, 20));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, " + AppTheme.TEAL_DARK + ", " + AppTheme.PRIMARY + ");" +
                        "-fx-background-radius: 14 14 0 0;"
        );

        Label lblTitle = new Label("Chi tiết phòng");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblTitle.setTextFill(Color.WHITE);

        lblFormRoomCode = new Label("Chưa chọn phòng");
        lblFormRoomCode.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblFormRoomCode.setTextFill(Color.web("#a8c4e0"));

        header.getChildren().addAll(new Label("\uD83D\uDECF") {{
            setFont(Font.font(18));
        }}, lblTitle, lblFormRoomCode);
        return header;
    }

    private VBox buildFormBody() {
        txtMa = styledField("VD: P101");
        txtSoNguoi = styledField("VD: 2");
        txtTang = styledField("VD: 1");
        txtGia = styledField("VD: 500000");

        cbLoaiPhong = styledCombo();
        cbMaLoai = styledCombo();
        cbTrangThai = styledCombo(
                RoomStatus.TRONG.getDisplayName(),
                RoomStatus.DANG_O.getDisplayName(),
                RoomStatus.SUA_CHUA.getDisplayName()
        );

        initializeRoomTypeOptions();
        configureRoomTypeControls();
        installRoomTypeSyncHandlers();

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 20, 16, 20));
        body.getChildren().addAll(
                formRow("Mã phòng *", txtMa),
                formRow("Loại phòng *", cbLoaiPhong),
                formRow("Mã loại *", cbMaLoai),
                formRow("Số người *", txtSoNguoi),
                formRow("Tầng *", txtTang),
                formRow("Trạng thái *", cbTrangThai),
                formRow("Giá gốc (VND) *", txtGia)
        );
        VBox.setVgrow(body, Priority.ALWAYS);
        return body;
    }

    private VBox buildFormFooter() {
        btnThem = filledBtn("\uFF0B  Thêm mới", AppTheme.TEAL);
        btnThem.setMaxWidth(Double.MAX_VALUE);
        btnThem.setOnAction(e -> handleThem());

        btnCapNhat = filledBtn("\u270E  Cập nhật", AppTheme.TEAL);
        btnCapNhat.setMaxWidth(Double.MAX_VALUE);
        btnCapNhat.setOnAction(e -> handleCapNhat());

        btnLamMoiForm = outlineBtn("\u21BB  Làm mới", C_PRIMARY);
        btnLamMoiForm.setMaxWidth(Double.MAX_VALUE);
        btnLamMoiForm.setOnAction(e -> resetDetailForm(true));

        VBox footer = new VBox(10, btnThem, btnCapNhat, btnLamMoiForm);
        footer.setPadding(new Insets(4, 20, 20, 20));
        return footer;
    }

    private void filterData() {
        String kw = txtSearch == null || txtSearch.getText() == null
                ? ""
                : txtSearch.getText().trim().toLowerCase();
        tableView.getItems().setAll(allData.stream()
                .filter(p -> kw.isBlank()
                        || safeText(p.getMaPhong()).toLowerCase().contains(kw)
                        || safeText(p.getLoaiPhong()).toLowerCase().contains(kw)
                        || String.valueOf(p.getTang()).contains(kw))
                .collect(Collectors.toList()));
    }

    private void fillForm(Phong p) {
        if (p == null) {
            return;
        }

        ensureRoomTypeOption(p.getLoaiPhong(), p.getMaLoai());
        ensureStatusOption(StatusUtils.roomLabel(p.getTrangThai()));

        txtMa.setText(safeText(p.getMaPhong()));
        txtSoNguoi.setText(String.valueOf(p.getSoNguoi()));
        txtTang.setText(String.valueOf(p.getTang()));
        txtGia.setText(String.valueOf((long) p.getGiaPhong()));
        setRoomTypeSelection(p.getLoaiPhong(), p.getMaLoai());
        cbTrangThai.setValue(StatusUtils.roomLabel(p.getTrangThai()));

        originalRoomSnapshot = clonePhong(p);
        if (lblFormRoomCode != null) {
            lblFormRoomCode.setText("Mã phòng: " + p.getMaPhong());
        }
        updateFormState(true);
        applySensitiveFieldLock(p);
    }

    private void handleThem() {
        if (isEditMode()) {
            showWarning("Thông báo", "Vui lòng bấm \"Làm mới\" để thêm phòng mới.");
            return;
        }

        try {
            Phong p = buildPhongFromForm();
            if (dao.getPhongByMa(p.getMaPhong()) != null) {
                showWarning("Lỗi dữ liệu", "Mã phòng đã tồn tại. Vui lòng nhập mã khác.");
                txtMa.requestFocus();
                txtMa.selectAll();
                return;
            }

            if (dao.create(p)) {
                showInfo("Thông báo", "Thêm phòng thành công.");
                resetDetailForm(true);
                loadData();
            } else {
                showError("Lỗi", "Thêm phòng thất bại. Vui lòng thử lại.");
            }
        } catch (IllegalArgumentException ex) {
            showWarning("Lỗi dữ liệu", ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Lỗi", "Thêm phòng thất bại. Vui lòng thử lại.");
        }
    }

    private void handleCapNhat() {
        Phong selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null || originalRoomSnapshot == null) {
            showWarning("Thông báo", "Vui lòng chọn phòng cần cập nhật.");
            return;
        }

        try {
            Phong updated = buildPhongFromForm();
            updated.setMaPhong(selected.getMaPhong());

            if (!hasRoomChanges(originalRoomSnapshot, updated)) {
                showInfo("Thông báo", "Không có thay đổi nào để cập nhật.");
                return;
            }

            if (!validateUpdateBusinessRules(originalRoomSnapshot, updated)) {
                return;
            }

            if (dao.update(updated)) {
                pendingSelectionCode = updated.getMaPhong();
                showInfo("Thông báo", "Cập nhật phòng thành công.");
                loadData();
            } else {
                showError("Lỗi", "Cập nhật phòng thất bại. Vui lòng thử lại.");
            }
        } catch (IllegalArgumentException ex) {
            showWarning("Lỗi dữ liệu", ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Lỗi", "Cập nhật phòng thất bại. Vui lòng thử lại.");
        }
    }

    private void handleXoa(Phong p) {
        if (p == null) {
            return;
        }

        if (StatusUtils.isRoomStatus(p.getTrangThai(), RoomStatus.DANG_O)) {
            showWarning("Không thể xóa", "Không thể xóa phòng đang có khách ở.");
            return;
        }

        if (!showConfirmation("Xác nhận xóa",
                "Bạn có chắc chắn muốn xóa phòng " + p.getMaPhong() + " không?")) {
            return;
        }

        try {
            if (dao.hasRelatedRoomData(p.getMaPhong())) {
                showWarning("Không thể xóa", "Phòng đang phát sinh dữ liệu và không thể xóa.");
                return;
            }

            if (dao.delete(p.getMaPhong())) {
                if (isEditingRoom(p.getMaPhong())) {
                    resetDetailForm(false);
                }
                showInfo("Thông báo", "Xóa phòng thành công.");
                loadData();
            } else {
                showError("Lỗi", "Xóa phòng thất bại. Vui lòng thử lại.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Lỗi", "Xóa phòng thất bại. Vui lòng thử lại.");
        }
    }

    private Phong buildPhongFromForm() {
        String maPhong = normalizeRoomCode(txtMa != null ? txtMa.getText() : null);
        String loaiPhong = trimToNull(cbLoaiPhong != null ? cbLoaiPhong.getValue() : null);
        String maLoai = resolveMaLoaiForLoaiPhong(loaiPhong);
        String trangThai = trimToNull(cbTrangThai != null ? cbTrangThai.getValue() : null);

        if (!ROOM_CODE_PATTERN.matcher(maPhong).matches()) {
            throw new IllegalArgumentException("Mã phòng phải có định dạng P + 3 chữ số. Ví dụ: P101.");
        }
        if (loaiPhong == null) {
            throw new IllegalArgumentException("Loại phòng là bắt buộc.");
        }
        if (maLoai == null) {
            throw new IllegalArgumentException("Mã loại là bắt buộc.");
        }

        setRoomTypeSelection(loaiPhong, maLoai);

        int soNguoi = parsePositiveInt(txtSoNguoi.getText(), "Số người phải là số nguyên > 0.");
        int tang = parsePositiveInt(txtTang.getText(), "Tầng phải là số nguyên > 0.");
        double giaPhong = parseNonNegativeDouble(txtGia.getText(), "Giá gốc phải là số >= 0.");

        if (trangThai == null) {
            throw new IllegalArgumentException("Trạng thái là bắt buộc.");
        }

        txtMa.setText(maPhong);
        txtSoNguoi.setText(String.valueOf(soNguoi));
        txtTang.setText(String.valueOf(tang));
        txtGia.setText(formatNumberForForm(giaPhong));

        Phong p = new Phong();
        p.setMaPhong(maPhong);
        p.setLoaiPhong(loaiPhong);
        p.setMaLoaiPhong(maLoai);
        p.setSoNguoi(soNguoi);
        p.setTang(tang);
        p.setTrangThai(StatusUtils.roomCode(trangThai));
        p.setGiaPhong(giaPhong);
        return p;
    }

    private boolean validateUpdateBusinessRules(Phong original, Phong updated) {
        if (StatusUtils.isRoomStatus(original.getTrangThai(), RoomStatus.DANG_O)) {
            boolean changedType = !Objects.equals(trimToNull(original.getLoaiPhong()), trimToNull(updated.getLoaiPhong()));
            boolean changedMaLoai = !Objects.equals(trimToNull(original.getMaLoai()), trimToNull(updated.getMaLoai()));
            boolean changedGia = Double.compare(original.getGiaPhong(), updated.getGiaPhong()) != 0;
            boolean changedStatus = !Objects.equals(StatusUtils.roomCode(original.getTrangThai()),
                    StatusUtils.roomCode(updated.getTrangThai()));

            if (changedType || changedMaLoai || changedGia) {
                showWarning("Không thể cập nhật",
                        "Không thể thay đổi loại phòng, mã loại hoặc giá gốc khi phòng đang có khách ở.");
                return false;
            }
            if (changedStatus) {
                showWarning("Không thể cập nhật", "Không thể thay đổi trạng thái phòng đang có khách ở.");
                return false;
            }
        }

        boolean changingToTrong = StatusUtils.isRoomStatus(updated.getTrangThai(), RoomStatus.TRONG)
                && !StatusUtils.isRoomStatus(original.getTrangThai(), RoomStatus.TRONG);
        if (changingToTrong && dao.hasActiveBookingForRoom(original.getMaPhong())) {
            showWarning("Không thể cập nhật",
                    "Không thể chuyển trạng thái phòng sang Trống khi phòng đang có phiếu đặt hoạt động.");
            return false;
        }

        return true;
    }

    private void resetDetailForm(boolean focusRoomCode) {
        originalRoomSnapshot = null;
        pendingSelectionCode = null;

        if (txtMa != null) {
            txtMa.clear();
            txtMa.setEditable(true);
        }
        if (txtSoNguoi != null) {
            txtSoNguoi.clear();
        }
        if (txtTang != null) {
            txtTang.clear();
        }
        if (txtGia != null) {
            txtGia.clear();
        }

        resetRoomTypeDefaults();
        resetStatusDefault();

        applySensitiveFieldLock(null);
        if (tableView != null) {
            tableView.getSelectionModel().clearSelection();
        }
        if (lblFormRoomCode != null) {
            lblFormRoomCode.setText("Chưa chọn phòng");
        }
        updateFormState(false);

        if (focusRoomCode && txtMa != null) {
            Platform.runLater(txtMa::requestFocus);
        }
    }

    private void updateFormState(boolean editMode) {
        if (txtMa != null) {
            txtMa.setEditable(!editMode);
            txtMa.setDisable(false);
        }
        if (btnThem != null) {
            btnThem.setDisable(editMode);
        }
        if (btnCapNhat != null) {
            btnCapNhat.setDisable(!editMode);
        }
        if (btnLamMoiForm != null) {
            btnLamMoiForm.setDisable(false);
        }
    }

    private void applySensitiveFieldLock(Phong room) {
        boolean locked = room != null && StatusUtils.isRoomStatus(room.getTrangThai(), RoomStatus.DANG_O);
        if (cbLoaiPhong != null) {
            cbLoaiPhong.setDisable(locked);
        }
        if (cbMaLoai != null) {
            cbMaLoai.setDisable(true);
            cbMaLoai.setOpacity(1);
        }
        if (txtGia != null) {
            txtGia.setDisable(locked);
        }
    }

    private void restorePendingSelection() {
        if (pendingSelectionCode == null || pendingSelectionCode.isBlank()) {
            return;
        }

        String code = pendingSelectionCode;
        pendingSelectionCode = null;
        for (Phong phong : tableView.getItems()) {
            if (code.equalsIgnoreCase(phong.getMaPhong())) {
                tableView.getSelectionModel().select(phong);
                tableView.scrollTo(phong);
                fillForm(phong);
                break;
            }
        }
    }

    private void initializeRoomTypeOptions() {
        maLoaiByLoaiPhong.clear();
        loaiPhongByMaLoai.clear();

        List<PhongDAO.LoaiPhongItem> loadedOptions;
        try {
            loadedOptions = dao.getDistinctLoaiPhong();
        } catch (Exception ex) {
            ex.printStackTrace();
            loadedOptions = List.of();
        }

        roomTypeOptions = loadedOptions.stream()
                .filter(item -> item != null
                        && trimToNull(item.getLoaiPhong()) != null
                        && trimToNull(item.getMaLoai()) != null)
                .collect(Collectors.toCollection(ArrayList::new));

        for (PhongDAO.LoaiPhongItem item : roomTypeOptions) {
            String loaiPhong = item.getLoaiPhong().trim();
            String maLoai = item.getMaLoai().trim();
            maLoaiByLoaiPhong.putIfAbsent(loaiPhong, maLoai);
            loaiPhongByMaLoai.putIfAbsent(maLoai, loaiPhong);
        }
    }

    private void installRoomTypeSyncHandlers() {
        cbLoaiPhong.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (suppressRoomTypeSync) {
                return;
            }
            if (ADD_ROOM_TYPE_LABEL.equals(newValue)) {
                suppressRoomTypeSync = true;
                try {
                    cbLoaiPhong.setValue(trimToNull(oldValue));
                } finally {
                    suppressRoomTypeSync = false;
                }
                openRoomTypeManagementDialog();
                return;
            }

            String normalizedLoaiPhong = trimToNull(newValue);
            String maLoai = maLoaiByLoaiPhong.get(normalizedLoaiPhong);
            if (maLoai != null) {
                setRoomTypeSelection(normalizedLoaiPhong, maLoai);
            }
        });
    }

    private void ensureRoomTypeOption(String loaiPhong, String maLoai) {
        String normalizedLoaiPhong = trimToNull(loaiPhong);
        String normalizedMaLoai = trimToNull(maLoai);
        if (normalizedLoaiPhong == null || normalizedMaLoai == null) {
            return;
        }

        maLoaiByLoaiPhong.putIfAbsent(normalizedLoaiPhong, normalizedMaLoai);
        loaiPhongByMaLoai.putIfAbsent(normalizedMaLoai, normalizedLoaiPhong);

        if (!cbLoaiPhong.getItems().contains(normalizedLoaiPhong)) {
            int actionIndex = cbLoaiPhong.getItems().indexOf(ADD_ROOM_TYPE_LABEL);
            if (actionIndex >= 0) {
                cbLoaiPhong.getItems().add(actionIndex, normalizedLoaiPhong);
            } else {
                cbLoaiPhong.getItems().add(normalizedLoaiPhong);
            }
        }
        if (!cbMaLoai.getItems().contains(normalizedMaLoai)) {
            cbMaLoai.getItems().add(normalizedMaLoai);
        }
    }

    private void ensureStatusOption(String statusLabel) {
        String normalized = trimToNull(statusLabel);
        if (normalized != null && !cbTrangThai.getItems().contains(normalized)) {
            cbTrangThai.getItems().add(normalized);
        }
    }

    private void resetRoomTypeDefaults() {
        if (cbLoaiPhong == null || cbMaLoai == null) {
            return;
        }
        if (roomTypeOptions.isEmpty()) {
            setRoomTypeSelection(null, null);
            return;
        }
        PhongDAO.LoaiPhongItem first = roomTypeOptions.get(0);
        setRoomTypeSelection(first.getLoaiPhong(), first.getMaLoai());
    }

    private void resetStatusDefault() {
        if (cbTrangThai != null) {
            cbTrangThai.setValue(RoomStatus.TRONG.getDisplayName());
        }
    }

    private void setRoomTypeSelection(String loaiPhong, String maLoai) {
        suppressRoomTypeSync = true;
        try {
            cbLoaiPhong.setValue(loaiPhong);
            cbMaLoai.setValue(maLoai);
        } finally {
            suppressRoomTypeSync = false;
        }
    }

    private void configureRoomTypeControls() {
        installRoomTypeActionCellFactory();
        refreshRoomTypeControls(null, null, false);
        if (cbMaLoai != null) {
            cbMaLoai.setDisable(true);
            cbMaLoai.setOpacity(1);
            cbMaLoai.setFocusTraversable(false);
            hideComboArrow(cbMaLoai);
        }
    }

    private void installRoomTypeActionCellFactory() {
        cbLoaiPhong.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                if (ADD_ROOM_TYPE_LABEL.equals(item)) {
                    Separator separator = new Separator();
                    Label actionLabel = new Label(item);
                    actionLabel.setTextFill(Color.web(C_PRIMARY));
                    actionLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                    VBox wrapper = new VBox(6, separator, actionLabel);
                    wrapper.setPadding(new Insets(4, 0, 0, 0));
                    setText(null);
                    setGraphic(wrapper);
                    return;
                }
                setGraphic(null);
                setText(item);
            }
        });
        cbLoaiPhong.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || ADD_ROOM_TYPE_LABEL.equals(item)) {
                    setText(null);
                    return;
                }
                setText(item);
            }
        });
    }

    private void refreshRoomTypeControls(String preferredLoaiPhong, String preferredMaLoai, boolean preserveCurrentSelection) {
        initializeRoomTypeOptions();
        if (cbLoaiPhong == null || cbMaLoai == null) {
            return;
        }

        String currentLoaiPhong = preserveCurrentSelection ? trimToNull(cbLoaiPhong.getValue()) : null;
        String currentMaLoai = preserveCurrentSelection ? trimToNull(cbMaLoai.getValue()) : null;

        List<String> loaiPhongItems = roomTypeOptions.stream()
                .map(PhongDAO.LoaiPhongItem::getLoaiPhong)
                .collect(Collectors.toCollection(ArrayList::new));
        List<String> maLoaiItems = roomTypeOptions.stream()
                .map(PhongDAO.LoaiPhongItem::getMaLoai)
                .collect(Collectors.toCollection(ArrayList::new));

        loaiPhongItems.add(ADD_ROOM_TYPE_LABEL);
        cbLoaiPhong.getItems().setAll(loaiPhongItems);
        cbMaLoai.getItems().setAll(maLoaiItems);

        String selectedLoaiPhong = trimToNull(preferredLoaiPhong);
        if (selectedLoaiPhong == null || !maLoaiByLoaiPhong.containsKey(selectedLoaiPhong)) {
            selectedLoaiPhong = trimToNull(currentLoaiPhong);
        }
        if (selectedLoaiPhong == null || !maLoaiByLoaiPhong.containsKey(selectedLoaiPhong)) {
            selectedLoaiPhong = roomTypeOptions.isEmpty() ? null : roomTypeOptions.get(0).getLoaiPhong();
        }

        String selectedMaLoai = trimToNull(preferredMaLoai);
        if (selectedMaLoai == null || !loaiPhongByMaLoai.containsKey(selectedMaLoai)) {
            selectedMaLoai = resolveMaLoaiForLoaiPhong(selectedLoaiPhong);
        }
        if (selectedMaLoai == null || !loaiPhongByMaLoai.containsKey(selectedMaLoai)) {
            selectedMaLoai = trimToNull(currentMaLoai);
        }
        if (selectedMaLoai == null || !loaiPhongByMaLoai.containsKey(selectedMaLoai)) {
            selectedMaLoai = roomTypeOptions.isEmpty() ? null : roomTypeOptions.get(0).getMaLoai();
        }

        setRoomTypeSelection(selectedLoaiPhong, selectedMaLoai);
    }

    private String resolveMaLoaiForLoaiPhong(String loaiPhong) {
        String normalizedLoaiPhong = trimToNull(loaiPhong);
        return normalizedLoaiPhong == null ? null : maLoaiByLoaiPhong.get(normalizedLoaiPhong);
    }

    private void hideComboArrow(ComboBox<?> comboBox) {
        if (comboBox == null) {
            return;
        }
        Platform.runLater(() -> {
            Node arrowButton = comboBox.lookup(".arrow-button");
            if (arrowButton instanceof Region region) {
                region.setVisible(false);
                region.setManaged(false);
                region.setPrefWidth(0);
                region.setMinWidth(0);
                region.setMaxWidth(0);
            }

            Node arrow = comboBox.lookup(".arrow");
            if (arrow instanceof Pane pane) {
                pane.setVisible(false);
                pane.setManaged(false);
            } else if (arrow instanceof Region region) {
                region.setVisible(false);
                region.setManaged(false);
            }
        });
    }

    private void openRoomTypeManagementDialog() {
        if (openingRoomTypeDialog) {
            return;
        }

        openingRoomTypeDialog = true;
        try {
            RoomTypeManagementDialog dialog = new RoomTypeManagementDialog(
                    cbLoaiPhong != null && cbLoaiPhong.getScene() != null ? cbLoaiPhong.getScene().getWindow() : null
            );
            RoomTypeManagementDialog.Result result = dialog.showAndWait();
            PhongDAO.LoaiPhongItem preferredSelection = result.preferredSelection();

            refreshRoomTypeControls(
                    preferredSelection != null ? preferredSelection.getLoaiPhong() : trimToNull(cbLoaiPhong.getValue()),
                    preferredSelection != null ? preferredSelection.getMaLoai() : trimToNull(cbMaLoai.getValue()),
                    true
            );

            if (result.changed()) {
                pendingSelectionCode = originalRoomSnapshot != null ? originalRoomSnapshot.getMaPhong() : null;
                loadData();
            }
        } finally {
            openingRoomTypeDialog = false;
        }
    }

    private boolean hasRoomChanges(Phong original, Phong updated) {
        return !Objects.equals(trimToNull(original.getMaPhong()), trimToNull(updated.getMaPhong()))
                || !Objects.equals(trimToNull(original.getLoaiPhong()), trimToNull(updated.getLoaiPhong()))
                || !Objects.equals(trimToNull(original.getMaLoai()), trimToNull(updated.getMaLoai()))
                || original.getSoNguoi() != updated.getSoNguoi()
                || original.getTang() != updated.getTang()
                || !Objects.equals(StatusUtils.roomCode(original.getTrangThai()), StatusUtils.roomCode(updated.getTrangThai()))
                || Double.compare(original.getGiaPhong(), updated.getGiaPhong()) != 0;
    }

    private Phong clonePhong(Phong source) {
        Phong clone = new Phong();
        clone.setMaPhong(source.getMaPhong());
        clone.setLoaiPhong(source.getLoaiPhong());
        clone.setMaLoaiPhong(source.getMaLoai());
        clone.setSoNguoi(source.getSoNguoi());
        clone.setTang(source.getTang());
        clone.setTrangThai(StatusUtils.roomCode(source.getTrangThai()));
        clone.setGiaPhong(source.getGiaPhong());
        return clone;
    }

    private boolean isEditMode() {
        return originalRoomSnapshot != null;
    }

    private boolean isEditingRoom(String maPhong) {
        return originalRoomSnapshot != null
                && Objects.equals(trimToNull(originalRoomSnapshot.getMaPhong()), trimToNull(maPhong));
    }

    private String normalizeRoomCode(String input) {
        String normalized = input == null ? "" : input.trim().toUpperCase();
        if (normalized.startsWith("P")) {
            return normalized;
        }
        return normalized;
    }

    private int parsePositiveInt(String input, String message) {
        String value = trimToNull(input);
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(message);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private double parseNonNegativeDouble(String input, String message) {
        String value = trimToNull(input);
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        try {
            double parsed = Double.parseDouble(value.replace(",", ""));
            if (parsed < 0) {
                throw new IllegalArgumentException(message);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private String formatNumberForForm(double value) {
        if (Math.floor(value) == value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private TableColumn<Phong, String> col(String title, double prefW,
                                           java.util.function.Function<Phong, String> getter) {
        TableColumn<Phong, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new SimpleStringProperty(getter.apply(d.getValue())));
        if (prefW > 0) {
            c.setPrefWidth(prefW);
        }
        return c;
    }

    private Button buildRowTrashBtn() {
        Canvas icon = drawTrashIcon(17, Color.web("#94a3b8"));
        Canvas iconHov = drawTrashIcon(17, Color.web("#ef4444"));
        Button btn = new Button();
        btn.setGraphic(icon);
        String base = "-fx-background-color:transparent;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:5 8;";
        String hover = "-fx-background-color:#fef2f2;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:5 8;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(hover);
            btn.setGraphic(iconHov);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(base);
            btn.setGraphic(icon);
        });
        Tooltip tip = new Tooltip("Xóa phòng");
        tip.setStyle("-fx-font-size:12;");
        btn.setTooltip(tip);
        return btn;
    }

    private Canvas drawTrashIcon(double sz, Color color) {
        Canvas c = new Canvas(sz, sz);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setStroke(color);
        g.setLineWidth(sz / 10.0);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        double u = sz / 16.0;
        g.strokeLine(2 * u, 4.5 * u, 14 * u, 4.5 * u);
        g.strokeLine(6 * u, 4.5 * u, 6 * u, 2.5 * u);
        g.strokeLine(10 * u, 4.5 * u, 10 * u, 2.5 * u);
        g.strokeLine(6 * u, 2.5 * u, 10 * u, 2.5 * u);
        g.strokeRoundRect(3.5 * u, 5.5 * u, 9 * u, 8 * u, 1.5 * u, 1.5 * u);
        g.strokeLine(6 * u, 7.5 * u, 6 * u, 11.5 * u);
        g.strokeLine(8 * u, 7.5 * u, 8 * u, 11.5 * u);
        g.strokeLine(10 * u, 7.5 * u, 10 * u, 11.5 * u);
        return c;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(40);
        tf.setMaxWidth(Double.MAX_VALUE);
        String base =
                "-fx-background-color:#f8fafc;" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:" + C_BORDER + ";" +
                        "-fx-border-radius:8;" +
                        "-fx-border-width:1.5;" +
                        "-fx-padding:0 12;" +
                        "-fx-font-size:13;";
        String focused =
                "-fx-background-color:white;" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:" + C_ACCENT + ";" +
                        "-fx-border-radius:8;" +
                        "-fx-border-width:1.5;" +
                        "-fx-padding:0 12;" +
                        "-fx-font-size:13;";
        tf.setStyle(base);
        tf.focusedProperty().addListener((obs, o, f) -> tf.setStyle(f ? focused : base));
        return tf;
    }

    private ComboBox<String> styledCombo(String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        if (items.length > 0) {
            cb.setValue(items[0]);
        }
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setPrefHeight(40);
        cb.setStyle(
                "-fx-background-color:#f8fafc;" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:" + C_BORDER + ";" +
                        "-fx-border-radius:8;" +
                        "-fx-border-width:1.5;" +
                        "-fx-font-size:13;"
        );
        return cb;
    }

    private VBox formRow(String labelText, Control field) {
        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        lbl.setTextFill(Color.web(C_TEXT));
        return new VBox(6, lbl, field);
    }

    private Button filledBtn(String text, String bg) {
        Button btn = new Button(text);
        btn.setPrefHeight(42);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String base = "-fx-background-color:" + bg + ";-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;";
        String hover = "-fx-background-color:derive(" + bg + ",-14%);-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Button outlineBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefHeight(40);
        btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        String base = "-fx-background-color:transparent;-fx-text-fill:" + color + ";-fx-border-color:" + color + ";-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:0 16;";
        String hover = "-fx-background-color:#f0f4f8;-fx-text-fill:" + color + ";-fx-border-color:" + color + ";-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:0 16;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private void showInfo(String title, String msg) {
        showMessage(Alert.AlertType.INFORMATION, title, msg);
    }

    private void showWarning(String title, String msg) {
        showMessage(Alert.AlertType.WARNING, title, msg);
    }

    private void showError(String title, String msg) {
        showMessage(Alert.AlertType.ERROR, title, msg);
    }

    private void showMessage(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean showConfirmation(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        a.setHeaderText(null);
        return a.showAndWait().filter(type -> type == ButtonType.YES).isPresent();
    }

    private static String buildTableCss() {
        String css =
                ".room-table{" +
                        "-fx-background-color:white;" +
                        "-fx-border-color:#e2e8f0;" +
                        "-fx-border-radius:12;" +
                        "-fx-background-radius:12;" +
                        "}" +
                        ".room-table .column-header-background{" +
                        "-fx-background-color:#f8fafc;" +
                        "-fx-background-radius:12 12 0 0;" +
                        "}" +
                        ".room-table .column-header{" +
                        "-fx-background-color:transparent;" +
                        "-fx-border-color:transparent transparent #e2e8f0 transparent;" +
                        "-fx-size:46;" +
                        "}" +
                        ".room-table .column-header .label{" +
                        "-fx-text-fill:#475569;" +
                        "-fx-font-weight:bold;" +
                        "-fx-font-size:12px;" +
                        "-fx-padding:0 12 0 12;" +
                        "}" +
                        ".room-table .table-cell{" +
                        "-fx-padding:10 12 10 12;" +
                        "-fx-border-color:transparent transparent #f1f5f9 transparent;" +
                        "-fx-font-size:13px;" +
                        "-fx-text-fill:#334155;" +
                        "}" +
                        ".room-table .table-row-cell{" +
                        "-fx-background-color:white;" +
                        "-fx-cell-size:46px;" +
                        "}" +
                        ".room-table .table-row-cell:odd{" +
                        "-fx-background-color:#fafbfd;" +
                        "}" +
                        ".room-table .table-row-cell:selected .table-cell{" +
                        "-fx-text-fill:#1e40af;" +
                        "}" +
                        ".room-table .placeholder .label{" +
                        "-fx-text-fill:#94a3b8;" +
                        "-fx-font-size:14px;" +
                        "}";
        try {
            return "data:text/css," + URLEncoder.encode(css, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception ex) {
            return "";
        }
    }
}
