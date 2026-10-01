package gui;

import dao.DichVuDAO;
import entity.DichVu;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import util.AppTheme;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ServiceView {

    private static final String BORDER_ERR = "#ef4444";
    private static final String BG_ERR = "#fef2f2";
    private static final String TABLE_CSS = buildTableCss();
    private static final Locale LOCALE_VI = new Locale("vi", "VN");

    private final TableView<DichVu> tableView = new TableView<>();
    private final ObservableList<DichVu> dataList = FXCollections.observableArrayList();
    private final DichVuDAO dao = new DichVuDAO();

    private List<DichVu> allData = new ArrayList<>();
    private TextField txtSearch;

    public Node createView() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: " + AppTheme.BG_LIGHT + ";");

        root.getChildren().addAll(buildHeader(), buildToolbar(), buildTableCard());
        VBox.setVgrow(root.getChildren().get(root.getChildren().size() - 1), Priority.ALWAYS);

        loadData();
        return root;
    }

    private HBox buildHeader() {
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconWrap = new StackPane();
        iconWrap.setPrefSize(48, 48);
        iconWrap.setStyle(
                "-fx-background-color: " + AppTheme.TEAL_LIGHT + ";" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #cfeeea;" +
                        "-fx-border-radius: 16;");
        Label icon = new Label("DV");
        icon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        icon.setTextFill(Color.web(AppTheme.TEAL_DARK));
        iconWrap.getChildren().add(icon);

        VBox titleBox = new VBox(4);
        Label title = new Label("Quản lý dịch vụ");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        title.setTextFill(Color.web(AppTheme.TEXT));

        Label sub = new Label("Theo dõi danh sách dịch vụ, đơn giá và trạng thái kinh doanh trong khách sạn.");
        sub.setFont(Font.font("Segoe UI", 12.5));
        sub.setTextFill(Color.web(AppTheme.TEXT_SUB));
        titleBox.getChildren().addAll(title, sub);

        header.getChildren().addAll(iconWrap, titleBox);
        return header;
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        HBox searchWrap = new HBox(10);
        searchWrap.setAlignment(Pos.CENTER_LEFT);
        searchWrap.setPadding(new Insets(0, 14, 0, 14));
        searchWrap.setPrefHeight(44);
        searchWrap.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e5eaf2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1.5;");

        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.setTextFill(Color.web(AppTheme.MUTED));

        txtSearch = new TextField();
        txtSearch.setPromptText("Tìm theo mã dịch vụ, tên dịch vụ hoặc trạng thái...");
        txtSearch.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        searchWrap.getChildren().addAll(searchIcon, txtSearch);

        Button btnSearch = createFilledButton("Tìm kiếm", AppTheme.PRIMARY);
        btnSearch.setOnAction(e -> handleSearch());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = createFilledButton("＋ Thêm mới", AppTheme.TEAL);
        btnAdd.setOnAction(e -> handleAdd());

        Button btnRefresh = createOutlineButton("↻ Làm mới", AppTheme.PRIMARY);
        btnRefresh.setOnAction(e -> handleRefresh());

        HBox.setHgrow(searchWrap, Priority.ALWAYS);
        toolbar.getChildren().addAll(searchWrap, btnSearch, spacer, btnAdd, btnRefresh);
        return toolbar;
    }

    private VBox buildTableCard() {
        configureTable();

        Label title = new Label("Danh sách dịch vụ");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        title.setTextFill(Color.web(AppTheme.TEXT));

        Label guide = new Label("Nhấn đúp vào một dòng để chỉnh sửa dịch vụ.");
        guide.setFont(Font.font("Segoe UI", 12));
        guide.setTextFill(Color.web(AppTheme.MUTED));

        StackPane tableShell = new StackPane(tableView);
        tableShell.setPadding(new Insets(0));
        tableShell.setStyle(
                "-fx-background-color: transparent;");

        VBox card = new VBox(12, title, guide, tableShell);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #e5eaf2;" +
                        "-fx-border-radius: 18;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 24, 0, 0, 8);");
        return card;
    }

    private void configureTable() {
        tableView.setItems(dataList);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setFixedCellSize(48);
        tableView.setEditable(false);
        tableView.setFocusTraversable(false);
        tableView.setStyle("-fx-background-color: transparent;");

        if (!tableView.getStylesheets().contains(TABLE_CSS)) {
            tableView.getStylesheets().add(TABLE_CSS);
        }
        if (!tableView.getStyleClass().contains("service-table")) {
            tableView.getStyleClass().add("service-table");
        }

        if (!tableView.getStylesheets().contains(TABLE_CSS)) {
            tableView.getStylesheets().add(TABLE_CSS);
        }
        if (!tableView.getStyleClass().contains("service-table")) {
            tableView.getStyleClass().add("service-table");
        }

        tableView.setRowFactory(tv -> {
            TableRow<DichVu> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showEditDialog(row.getItem());
                }
            });
            return row;
        });

        TableColumn<DichVu, String> colMa = new TableColumn<>("Mã dịch vụ");
        colMa.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMaDichVu()));
        colMa.prefWidthProperty().bind(tableView.widthProperty().multiply(0.15));
        colMa.setReorderable(false);
        colMa.setStyle("-fx-alignment: CENTER;");
        colMa.setCellFactory(col -> buildTextCell(Pos.CENTER, FontWeight.SEMI_BOLD, "#334155"));

        TableColumn<DichVu, String> colTen = new TableColumn<>("Tên dịch vụ");
        colTen.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTenDichVu()));
        colTen.prefWidthProperty().bind(tableView.widthProperty().multiply(0.35));
        colTen.setReorderable(false);
        colTen.setStyle("-fx-alignment: CENTER;");
        colTen.setCellFactory(col -> buildTextCell(Pos.CENTER_LEFT, FontWeight.NORMAL, AppTheme.TEXT_SUB));

        TableColumn<DichVu, String> colGia = new TableColumn<>("Đơn giá");
        colGia.setCellValueFactory(d -> new SimpleStringProperty(formatCurrency(d.getValue().getDonGia())));
        colGia.prefWidthProperty().bind(tableView.widthProperty().multiply(0.20));
        colGia.setReorderable(false);
        colGia.setStyle("-fx-alignment: CENTER;");
        colGia.setCellFactory(col -> buildTextCell(Pos.CENTER_RIGHT, FontWeight.SEMI_BOLD, "#334155"));

        TableColumn<DichVu, String> colTT = new TableColumn<>("Trạng thái");
        colTT.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTrangThai()));
        colTT.prefWidthProperty().bind(tableView.widthProperty().multiply(0.20));
        colTT.setReorderable(false);
        colTT.setStyle("-fx-alignment: CENTER;");
        colTT.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : buildStatusBadge(item));
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<DichVu, Void> colAction = new TableColumn<>("Xóa");
        colAction.setSortable(false);
        colAction.setReorderable(false);
        colAction.prefWidthProperty().bind(tableView.widthProperty().multiply(0.10));
        colAction.setStyle("-fx-alignment: CENTER;");
        colAction.setCellFactory(createActionCellFactory());

        tableView.getColumns().setAll(colMa, colTen, colGia, colTT, colAction);
    }

    private TableCell<DichVu, String> buildTextCell(Pos alignment, FontWeight weight, String textColor) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                setAlignment(alignment);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                setFont(Font.font("Segoe UI", weight, 13));
                setTextFill(Color.web(textColor));
            }
        };
    }

    private HBox buildStatusBadge(String status) {
        boolean active = "Đang kinh doanh".equalsIgnoreCase(status);

        Circle dot = new Circle(4, Color.web(active ? "#0f766e" : "#9f1239"));
        Label text = new Label(status);
        text.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        text.setTextFill(Color.web(active ? "#0f766e" : "#9f1239"));

        HBox badge = new HBox(6, dot, text);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(6, 12, 6, 12));
        badge.setMaxWidth(Region.USE_PREF_SIZE); // Đảm bảo pill không bị dãn ngang
        badge.setStyle(
                "-fx-background-color: " + (active ? "#ecfdf5" : "#fff1f2") + ";" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-color: " + (active ? "#bbf7d0" : "#fecdd3") + ";" +
                        "-fx-border-radius: 999;" +
                        "-fx-border-width: 1;");
        return badge;
    }

    private Callback<TableColumn<DichVu, Void>, TableCell<DichVu, Void>> createActionCellFactory() {
        return col -> new TableCell<>() {
            private final Button btnDelete = buildTrashButton();
            private final HBox box = new HBox(btnDelete);

            {
                box.setAlignment(Pos.CENTER);
                btnDelete.setOnAction(e -> {
                    DichVu dv = getTableView().getItems().get(getIndex());
                    handleDelete(dv);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty ? null : box);
                setAlignment(Pos.CENTER);
            }
        };
    }

    public void loadData() {
        setTableLoading(true);
        Task<List<DichVu>> task = new Task<>() {
            @Override
            protected List<DichVu> call() {
                return dao.getAllDichVu();
            }
        };

        task.setOnSucceeded(e -> {
            List<DichVu> list = task.getValue();
            allData = list != null ? list : new ArrayList<>();
            setTableLoading(false);
            handleSearch();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
            allData = new ArrayList<>();
            dataList.clear();
            setTableLoading(false);
            showAlert(Alert.AlertType.ERROR, "Lỗi",
                    "Không thể tải dữ liệu dịch vụ: " + (ex != null ? ex.getMessage() : ""));
        });

        Thread loader = new Thread(task, "service-load");
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
        tableView.setPlaceholder(buildEmptyState(loading ? "Đang tải dữ liệu..." : "Chưa có dịch vụ nào."));
        if (loading) {
            dataList.clear();
        }
    }

    private void handleAdd() {
        Dialog<ButtonType> dialog = buildServiceDialog(
                "Thêm mới dịch vụ",
                "+",
                AppTheme.TEAL,
                "Thêm dịch vụ mới",
                "Nhập đầy đủ thông tin dịch vụ trước khi lưu.",
                null);
        dialog.showAndWait();
    }

    private void showEditDialog(DichVu dv) {
        Dialog<ButtonType> dialog = buildServiceDialog(
                "Cập nhật dịch vụ",
                "S",
                AppTheme.PRIMARY,
                "Cập nhật dịch vụ",
                "Điều chỉnh thông tin dịch vụ theo biểu mẫu bên dưới.",
                dv);
        dialog.showAndWait();
    }

    private Dialog<ButtonType> buildServiceDialog(String title, String iconText, String accent, String headerText,
            String subText, DichVu dv) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        VBox content = new VBox(12);
        content.setPadding(new Insets(22));
        content.setPrefWidth(470);
        content.setStyle("-fx-background-color: white;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconWrap = new StackPane();
        iconWrap.setPrefSize(42, 42);
        iconWrap.setStyle(
                "-fx-background-color: " + AppTheme.TEAL_LIGHT + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #cfeeea;" +
                        "-fx-border-radius: 14;");
        Label icon = new Label(iconText);
        icon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        icon.setTextFill(Color.web(accent));
        iconWrap.getChildren().add(icon);

        VBox heading = new VBox(3);
        Label lblHeader = new Label(headerText);
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblHeader.setTextFill(Color.web(AppTheme.TEXT));
        Label lblGuide = new Label(subText);
        lblGuide.setTextFill(Color.web(AppTheme.TEXT_SUB));
        lblGuide.setFont(Font.font("Segoe UI", 12));
        heading.getChildren().addAll(lblHeader, lblGuide);
        header.getChildren().addAll(iconWrap, heading);

        TextField txtMa = field(dv == null ? "" : dv.getMaDichVu());
        txtMa.setPromptText("Ví dụ: DV011");
        if (dv != null) {
            txtMa.setEditable(false);
            txtMa.setStyle(txtMa.getStyle() + "-fx-background-color: #f8fafc;");
        }

        TextField txtTen = field(dv == null ? "" : dv.getTenDichVu());
        txtTen.setPromptText("Ví dụ: Nước suối");

        TextField txtGia = field(dv == null ? "" : String.format(Locale.ROOT, "%.0f", dv.getDonGia()));
        txtGia.setPromptText("Ví dụ: 15000");

        ComboBox<String> cbTrangThai = new ComboBox<>(FXCollections.observableArrayList(
                "Đang kinh doanh",
                "Ngừng kinh doanh"));
        cbTrangThai.setValue(dv != null && dv.getTrangThai() != null ? dv.getTrangThai() : "Đang kinh doanh");
        cbTrangThai.setPrefHeight(40);
        cbTrangThai.setMaxWidth(Double.MAX_VALUE);
        setNormalStyle(cbTrangThai);

        Label errMa = errorLabel();
        Label errTen = errorLabel();
        Label errGia = errorLabel();
        Label errTT = errorLabel();

        content.getChildren().addAll(
                header,
                requiredLabel("Mã dịch vụ"), txtMa, errMa,
                requiredLabel("Tên dịch vụ"), txtTen, errTen,
                requiredLabel("Đơn giá"), txtGia, errGia,
                requiredLabel("Trạng thái"), cbTrangThai, errTT);

        dialog.getDialogPane().setContent(content);

        ButtonType positiveType = new ButtonType(dv == null ? "Thêm mới" : "Cập nhật", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(positiveType, cancelType);

        util.ModalUtils.setupDialogBehavior(dialog, cancelType, true, true, () -> {
            String ma = txtMa.getText().trim();
            String ten = txtTen.getText().trim();
            String giaText = txtGia.getText().trim();
            String trangThai = cbTrangThai.getValue();
            if (dv == null) {
                return !ma.isEmpty() || !ten.isEmpty() || !giaText.isEmpty();
            }
            double gia = -1;
            try {
                gia = parseMoney(giaText);
            } catch (Exception ignored) {
            }
            return !ma.equals(dv.getMaDichVu()) || !ten.equals(dv.getTenDichVu()) || gia != dv.getDonGia()
                    || !trangThai.equals(dv.getTrangThai());
        });

        Button positiveButton = (Button) dialog.getDialogPane().lookupButton(positiveType);
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelType);
        styleDialogPrimaryButton(positiveButton, accent);
        styleDialogSecondaryButton(cancelButton);

        positiveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            clearValidation(txtMa, errMa);
            clearValidation(txtTen, errTen);
            clearValidation(txtGia, errGia);
            clearValidation(cbTrangThai, errTT);

            boolean valid = true;
            String ma = txtMa.getText().trim().toUpperCase(Locale.ROOT);
            String ten = txtTen.getText().trim();
            String giaText = txtGia.getText().trim();
            String trangThai = cbTrangThai.getValue();

            if (ma.isEmpty()) {
                setValidationError(txtMa, errMa, "Vui lòng nhập mã dịch vụ.");
                valid = false;
            } else if (!ma.matches("^DV\\d{3,}$")) {
                setValidationError(txtMa, errMa, "Mã dịch vụ phải có dạng DV001, DV002...");
                valid = false;
            } else if (dv == null && dao.findByMa(ma) != null) {
                setValidationError(txtMa, errMa, "Mã dịch vụ đã tồn tại.");
                valid = false;
            }

            if (ten.isEmpty()) {
                setValidationError(txtTen, errTen, "Vui lòng nhập tên dịch vụ.");
                valid = false;
            }

            double gia = -1;
            if (giaText.isEmpty()) {
                setValidationError(txtGia, errGia, "Vui lòng nhập đơn giá.");
                valid = false;
            } else {
                try {
                    gia = parseMoney(giaText);
                    if (gia < 0) {
                        setValidationError(txtGia, errGia, "Đơn giá phải lớn hơn hoặc bằng 0.");
                        valid = false;
                    }
                } catch (Exception ex) {
                    setValidationError(txtGia, errGia, "Đơn giá phải là số hợp lệ.");
                    valid = false;
                }
            }

            if (trangThai == null || trangThai.trim().isEmpty()) {
                setValidationError(cbTrangThai, errTT, "Vui lòng chọn trạng thái.");
                valid = false;
            }

            if (!valid) {
                event.consume();
                return;
            }

            try {
                DichVu target = new DichVu(dv == null ? ma : dv.getMaDichVu(), ten, gia, trangThai);
                boolean saved = dv == null ? dao.create(target) : dao.update(target);
                if (saved) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công",
                            dv == null
                                    ? "Đã thêm dịch vụ " + target.getMaDichVu()
                                    : "Đã cập nhật dịch vụ " + target.getMaDichVu());
                    loadData();
                } else {
                    event.consume();
                    showAlert(Alert.AlertType.ERROR, "Lỗi",
                            dv == null ? "Không thể thêm dịch vụ." : "Không thể cập nhật dịch vụ.");
                }
            } catch (Exception ex) {
                event.consume();
                showAlert(Alert.AlertType.ERROR, "Lỗi", ex.getMessage());
            }
        });

        return dialog;
    }

    private void handleDelete(DichVu dv) {
        if (dv == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc chắn muốn xóa dịch vụ [" + dv.getTenDichVu() + "] không?");

        ButtonType btnYes = new ButtonType("Xóa dịch vụ", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);

        Button delBtn = (Button) confirm.getDialogPane().lookupButton(btnYes);
        Button cancelBtn = (Button) confirm.getDialogPane().lookupButton(btnNo);
        styleDialogPrimaryButton(delBtn, AppTheme.DANGER);
        styleDialogSecondaryButton(cancelBtn);

        confirm.showAndWait().ifPresent(type -> {
            if (type != btnYes) {
                return;
            }

            try {
                if (dao.hasUsage(dv.getMaDichVu())) {
                    showAlert(Alert.AlertType.WARNING, "Không thể xóa",
                            "Không thể xóa dịch vụ đã phát sinh dữ liệu. Có thể chuyển sang trạng thái Ngừng kinh doanh.");
                    return;
                }

                if (dao.delete(dv.getMaDichVu())) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa dịch vụ " + dv.getMaDichVu());
                    loadData();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa dịch vụ.");
                }
            } catch (IllegalStateException ex) {
                showAlert(Alert.AlertType.WARNING, "Không thể xóa",
                        "Không thể xóa dịch vụ đã phát sinh dữ liệu. Có thể chuyển sang trạng thái Ngừng kinh doanh.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", ex.getMessage());
            }
        });
    }

    private void handleSearch() {
        String kw = txtSearch != null && txtSearch.getText() != null
                ? txtSearch.getText().trim().toLowerCase(Locale.ROOT)
                : "";
        if (kw.isEmpty()) {
            dataList.setAll(allData);
            return;
        }

        dataList.setAll(allData.stream()
                .filter(dv -> has(dv.getMaDichVu(), kw)
                        || has(dv.getTenDichVu(), kw)
                        || has(dv.getTrangThai(), kw))
                .collect(Collectors.toList()));
    }

    private double parseMoney(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Đơn giá không được để trống.");
        }
        String cleaned = text.replace(".", "")
                .replace(",", "")
                .replace("VNĐ", "")
                .replace("VND", "")
                .trim();
        return Double.parseDouble(cleaned);
    }

    private String formatCurrency(double value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(LOCALE_VI);
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);
        return formatter.format(value) + " VNĐ";
    }

    private boolean has(String s, String kw) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(kw);
    }

    private HBox requiredLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        label.setTextFill(Color.web(AppTheme.TEXT_SUB));

        Label star = new Label("*");
        star.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        star.setTextFill(Color.web(AppTheme.DANGER));

        HBox box = new HBox(4, label, star);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Label errorLabel() {
        Label lbl = new Label("");
        lbl.setTextFill(Color.web(AppTheme.DANGER));
        lbl.setFont(Font.font("Segoe UI", 11));
        lbl.setManaged(false);
        lbl.setVisible(false);
        return lbl;
    }

    private TextField field(String value) {
        TextField field = new TextField(value);
        field.setPrefHeight(40);
        setNormalStyle(field);
        return field;
    }

    private Button createFilledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefHeight(44);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String base = "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 0 18;" +
                "-fx-effect: " + AppTheme.SHADOW_SM + ";";
        String hover = "-fx-background-color: derive(" + color + ", -12%);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 0 18;" +
                "-fx-effect: " + AppTheme.SHADOW_SM + ";";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Button createOutlineButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefHeight(44);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String base = "-fx-background-color: white;" +
                "-fx-text-fill: " + color + ";" +
                "-fx-border-color: " + AppTheme.BORDER + ";" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 0 18;";
        String hover = "-fx-background-color: #eef2f7;" +
                "-fx-text-fill: " + color + ";" +
                "-fx-border-color: " + AppTheme.BORDER + ";" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 0 18;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Button buildTrashButton() {
        Canvas normal = drawTrashIcon(14, Color.web(AppTheme.DANGER));
        Canvas hover = drawTrashIcon(14, Color.web("#b91c1c"));

        Button btn = new Button();
        btn.setGraphic(normal);
        btn.setMinSize(32, 32);
        btn.setPrefSize(32, 32);
        btn.setMaxSize(32, 32);

        String base = "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 0;";
        String hoverStyle = "-fx-background-color: #ffe4e6;" +
                "-fx-border-color: transparent;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 0;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(hoverStyle);
            btn.setGraphic(hover);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(base);
            btn.setGraphic(normal);
        });

        Tooltip tooltip = new Tooltip("Xóa dịch vụ");
        tooltip.setStyle("-fx-font-size: 12;");
        btn.setTooltip(tooltip);
        return btn;
    }

    private Canvas drawTrashIcon(double size, Color color) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setStroke(color);
        g.setLineWidth(1.6);

        double lidY = size * 0.28;
        double bodyTop = size * 0.36;
        double bodyBottom = size * 0.82;
        double left = size * 0.28;
        double right = size * 0.72;

        g.strokeLine(left, lidY, right, lidY);
        g.strokeLine(size * 0.42, size * 0.20, size * 0.58, size * 0.20);
        g.strokeLine(size * 0.46, size * 0.20, size * 0.46, lidY);
        g.strokeLine(size * 0.54, size * 0.20, size * 0.54, lidY);
        g.strokeRoundRect(left, bodyTop, right - left, bodyBottom - bodyTop, 3, 3);
        g.strokeLine(size * 0.42, bodyTop + 2, size * 0.42, bodyBottom - 2);
        g.strokeLine(size * 0.50, bodyTop + 2, size * 0.50, bodyBottom - 2);
        g.strokeLine(size * 0.58, bodyTop + 2, size * 0.58, bodyBottom - 2);
        return canvas;
    }

    private void styleDialogPrimaryButton(Button button, String color) {
        button.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 18;");
    }

    private void styleDialogSecondaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: #eef2f7;" +
                        "-fx-text-fill: " + AppTheme.TEXT_SUB + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + AppTheme.BORDER + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 18;");
    }

    private void setValidationError(TextField field, Label errorLabel, String message) {
        field.setStyle(
                "-fx-background-color: " + BG_ERR + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: " + BORDER_ERR + ";" +
                        "-fx-font-size: 14;" +
                        "-fx-padding: 0 12;");
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void setValidationError(ComboBox<?> comboBox, Label errorLabel, String message) {
        comboBox.setStyle(
                "-fx-background-color: " + BG_ERR + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: " + BORDER_ERR + ";" +
                        "-fx-padding: 0 12;");
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void clearValidation(TextField field, Label errorLabel) {
        setNormalStyle(field);
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private void clearValidation(ComboBox<?> comboBox, Label errorLabel) {
        setNormalStyle(comboBox);
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private void setNormalStyle(TextField field) {
        field.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: " + AppTheme.BORDER + ";" +
                        "-fx-font-size: 14;" +
                        "-fx-padding: 0 12;");
    }

    private void setNormalStyle(ComboBox<?> comboBox) {
        comboBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: " + AppTheme.BORDER + ";" +
                        "-fx-font-size: 14;" +
                        "-fx-padding: 0 12;");
    }

    private VBox buildEmptyState(String message) {
        Label icon = new Label("DV");
        icon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        icon.setTextFill(Color.web(AppTheme.PRIMARY));
        icon.setAlignment(Pos.CENTER);
        icon.setMinSize(44, 44);
        icon.setPrefSize(44, 44);
        icon.setMaxSize(44, 44);
        icon.setStyle(
                "-fx-background-color: #eff6ff;" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: #dbeafe;" +
                        "-fx-border-radius: 22;" +
                        "-fx-border-width: 1;");

        Label title = new Label(message);
        title.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        title.setTextFill(Color.web("#475569"));

        VBox box = new VBox(10, icon, title);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28, 20, 28, 20));
        return box;
    }

    private static String buildTableCss() {
        String css = ".service-table{" +
                "-fx-background-color:transparent;" +
                "-fx-padding:0;" +
                "-fx-table-cell-border-color:transparent;" +
                "}" +
                ".service-table .column-header-background{" +
                "-fx-background-color:#f8fafc;" +
                "}" +
                ".service-table .filler{" +
                "-fx-background-color:#f8fafc;" +
                "-fx-border-color:transparent transparent #e5e7eb transparent;" +
                "}" +
                ".service-table .column-header{" +
                "-fx-background-color:transparent;" +
                "-fx-border-color:transparent transparent #e5e7eb transparent;" +
                "-fx-border-width:0 0 1 0;" +
                "-fx-size:46;" +
                "}" +
                ".service-table .column-header .label{" +
                "-fx-text-fill:#334155;" +
                "-fx-font-weight:bold;" +
                "-fx-font-size:12px;" +
                "-fx-alignment:center;" +
                "-fx-text-alignment:center;" +
                "-fx-padding:0 16 0 16;" +
                "}" +
                ".service-table .virtual-flow{" +
                "-fx-background-color:transparent;" +
                "}" +
                ".service-table .table-row-cell{" +
                "-fx-background-color:white;" +
                "-fx-cell-size:48px;" +
                "-fx-padding:0 10 0 10;" +
                "-fx-border-color:transparent;" +
                "}" +
                ".service-table .table-row-cell:odd{" +
                "-fx-background-color:#fafbfd;" +
                "}" +
                ".service-table .table-row-cell:hover{" +
                "-fx-background-color:#f8fafc;" +
                "-fx-cursor:hand;" +
                "}" +
                ".service-table .table-row-cell:selected{" +
                "-fx-background-color:#ecfeff;" +
                "}" +
                ".service-table .table-row-cell:selected .table-cell{" +
                "-fx-text-fill:#0f766e;" +
                "}" +
                ".service-table .table-cell{" +
                "-fx-border-color:transparent transparent #f1f5f9 transparent;" +
                "-fx-border-width:0 0 1 0;" +
                "-fx-padding:0 16 0 16;" +
                "-fx-font-size:13px;" +
                "-fx-text-fill:#334155;" +
                "-fx-alignment:CENTER-LEFT;" +
                "}" +
                ".service-table .placeholder{" +
                "-fx-background-color:transparent;" +
                "}" +
                ".service-table .placeholder .label{" +
                "-fx-text-fill:#94a3b8;" +
                "-fx-font-size:14px;" +
                "}" +
                ".service-table .scroll-bar:vertical," +
                ".service-table .scroll-bar:horizontal{" +
                "-fx-background-color:transparent;" +
                "}" +
                ".service-table .scroll-bar:vertical{" +
                "-fx-pref-width:8;" +
                "}" +
                ".service-table .scroll-bar:horizontal{" +
                "-fx-pref-height:8;" +
                "}" +
                ".service-table .scroll-bar .thumb{" +
                "-fx-background-color:#cbd5e1;" +
                "-fx-background-radius:999;" +
                "}" +
                ".service-table .scroll-bar .thumb:hover{" +
                "-fx-background-color:#94a3b8;" +
                "}" +
                ".service-table .scroll-bar .track," +
                ".service-table .scroll-bar .increment-button," +
                ".service-table .scroll-bar .decrement-button{" +
                "-fx-background-color:transparent;" +
                "-fx-padding:0;" +
                "}" +
                ".service-table .corner{" +
                "-fx-background-color:white;" +
                "}";
        try {
            return "data:text/css," + URLEncoder.encode(css, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception ex) {
            return "";
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
