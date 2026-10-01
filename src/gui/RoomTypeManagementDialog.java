package gui;

import dao.LoaiPhongDAO;
import dao.PhongDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import util.AppTheme;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RoomTypeManagementDialog {

    public record Result(boolean changed, PhongDAO.LoaiPhongItem preferredSelection) {
    }

    private static final String C_BG = AppTheme.BG_LIGHT;
    private static final String C_PRIMARY = AppTheme.PRIMARY;
    private static final String C_BORDER = AppTheme.BORDER;
    private static final String C_TEXT = AppTheme.TEXT;
    private static final String C_MUTED = AppTheme.MUTED;
    private static final String TABLE_CSS = buildTableCss();

    private final LoaiPhongDAO dao = new LoaiPhongDAO();
    private final Stage stage = new Stage();
    private final TableView<PhongDAO.LoaiPhongItem> tableView = new TableView<>();

    private TextField txtLoaiPhong;
    private TextField txtMaLoai;
    private PhongDAO.LoaiPhongItem originalSnapshot;
    private PhongDAO.LoaiPhongItem preferredSelection;
    private boolean changed;

    public RoomTypeManagementDialog(Window owner) {
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Quản lý loại phòng");
        stage.setResizable(false);
        stage.setScene(new Scene(buildRoot(), 860, 520));
        stage.setOnCloseRequest(event -> {
            if (!confirmCloseIfDirty()) {
                event.consume();
            }
        });
    }

    public Result showAndWait() {
        loadData();
        stage.showAndWait();
        return new Result(changed, preferredSelection);
    }

    private VBox buildRoot() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + C_BG + ";");
        root.getChildren().addAll(buildHeader(), buildContent(), buildFooter());
        return root;
    }

    private VBox buildHeader() {
        VBox header = new VBox(6);
        header.setPadding(new Insets(20, 22, 18, 22));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, " + AppTheme.TEAL_DARK + ", " + AppTheme.PRIMARY + ");"
        );

        Label title = new Label("Quản lý loại phòng");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Thêm mới hoặc cập nhật tên loại phòng và mã loại.");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web("#d9ecff"));

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private HBox buildContent() {
        configureTable();

        VBox leftCard = new VBox(12);
        leftCard.setPadding(new Insets(18));
        leftCard.setStyle(cardStyle());
        leftCard.setEffect(cardShadow());

        Label tableTitle = sectionTitle("Danh sách loại phòng");
        Label tableHint = helperText("Nhấn đúp vào một dòng để nạp dữ liệu sang form chỉnh sửa.");

        StackPane tableShell = new StackPane(tableView);
        tableShell.setPadding(new Insets(6));
        tableShell.setStyle(tableShellStyle());
        tableShell.setEffect(softShadow(10, 0.05, 3));
        VBox.setVgrow(tableShell, Priority.ALWAYS);

        leftCard.getChildren().addAll(tableTitle, tableHint, tableShell);

        txtLoaiPhong = styledField("Ví dụ: Standard");
        txtMaLoai = styledField("Ví dụ: LP01");

        Button btnThem = filledBtn("Thêm mới");
        btnThem.setMaxWidth(Double.MAX_VALUE);
        btnThem.setOnAction(e -> handleCreate());

        Button btnCapNhat = filledBtn("Cập nhật");
        btnCapNhat.setMaxWidth(Double.MAX_VALUE);
        btnCapNhat.setOnAction(e -> handleUpdate());

        Button btnLamMoi = outlineBtn("Làm mới");
        btnLamMoi.setMaxWidth(Double.MAX_VALUE);
        btnLamMoi.setOnAction(e -> resetForm());

        VBox rightCard = new VBox(14);
        rightCard.setPadding(new Insets(18));
        rightCard.setStyle(cardStyle());
        rightCard.setEffect(cardShadow());
        rightCard.getChildren().addAll(
                sectionTitle("Thông tin loại phòng"),
                formRow("Tên loại phòng", txtLoaiPhong),
                formRow("Mã loại", txtMaLoai),
                btnThem,
                btnCapNhat,
                btnLamMoi
        );
        VBox.setVgrow(rightCard, Priority.ALWAYS);

        HBox content = new HBox(16, leftCard, rightCard);
        content.setPadding(new Insets(18, 20, 18, 20));
        HBox.setHgrow(leftCard, Priority.ALWAYS);
        leftCard.setPrefWidth(540);
        rightCard.setPrefWidth(260);
        return content;
    }

    private HBox buildFooter() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = outlineBtn("Đóng");
        btnClose.setOnAction(e -> {
            if (confirmCloseIfDirty()) {
                stage.close();
            }
        });

        HBox footer = new HBox(10, spacer, btnClose);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(0, 20, 20, 20));
        return footer;
    }

    private void configureTable() {
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setPlaceholder(buildEmptyState());
        tableView.setFixedCellSize(50);
        tableView.setEditable(false);
        tableView.setFocusTraversable(false);
        if (!tableView.getStylesheets().contains(TABLE_CSS)) {
            tableView.getStylesheets().add(TABLE_CSS);
        }
        if (!tableView.getStyleClass().contains("room-type-table")) {
            tableView.getStyleClass().add("room-type-table");
        }
        tableView.setStyle("-fx-background-color: transparent;");

        TableColumn<PhongDAO.LoaiPhongItem, String> colLoai = new TableColumn<>("Loại phòng");
        colLoai.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLoaiPhong()));
        colLoai.setPrefWidth(300);
        colLoai.setResizable(false);
        colLoai.setReorderable(false);

        TableColumn<PhongDAO.LoaiPhongItem, String> colMa = new TableColumn<>("Mã loại");
        colMa.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMaLoai()));
        colMa.setPrefWidth(150);
        colMa.setResizable(false);
        colMa.setReorderable(false);
        colMa.setStyle("-fx-alignment: CENTER;");
        colMa.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                setAlignment(Pos.CENTER);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
                setTextFill(Color.web("#475569"));
            }
        });

        tableView.getColumns().setAll(colLoai, colMa);
        tableView.setRowFactory(tv -> {
            TableRow<PhongDAO.LoaiPhongItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    fillForm(row.getItem());
                }
            });
            return row;
        });
    }

    private void loadData() {
        List<PhongDAO.LoaiPhongItem> items = dao.getAllLoaiPhong();
        tableView.getItems().setAll(items);

        if (preferredSelection != null) {
            selectByCode(preferredSelection.getMaLoai());
            return;
        }
        if (originalSnapshot != null) {
            selectByCode(originalSnapshot.getMaLoai());
        }
    }

    private void handleCreate() {
        try {
            PhongDAO.LoaiPhongItem item = buildRoomTypeFromForm(null);
            if (dao.create(item)) {
                changed = true;
                preferredSelection = item;
                showInfo("Thành công", "Thêm loại phòng thành công.");
                originalSnapshot = null;
                loadData();
                selectByCode(item.getMaLoai());
            } else {
                showError("Lỗi", "Không thể thêm loại phòng. Vui lòng thử lại.");
            }
        } catch (IllegalArgumentException ex) {
            showWarning("Lỗi dữ liệu", ex.getMessage());
        } catch (RuntimeException ex) {
            showError("Lỗi", ex.getMessage());
        }
    }

    private void handleUpdate() {
        if (originalSnapshot == null) {
            showWarning("Thông báo", "Vui lòng chọn một loại phòng để cập nhật.");
            return;
        }

        try {
            PhongDAO.LoaiPhongItem updated = buildRoomTypeFromForm(originalSnapshot.getMaLoai());
            if (!hasChanges(originalSnapshot, updated)) {
                showInfo("Thông báo", "Không có thay đổi nào để cập nhật.");
                return;
            }

            if (dao.update(originalSnapshot.getMaLoai(), updated)) {
                changed = true;
                preferredSelection = updated;
                showInfo("Thành công", "Cập nhật loại phòng thành công.");
                originalSnapshot = updated;
                loadData();
                selectByCode(updated.getMaLoai());
            } else {
                showError("Lỗi", "Không thể cập nhật loại phòng. Vui lòng thử lại.");
            }
        } catch (IllegalArgumentException ex) {
            showWarning("Lỗi dữ liệu", ex.getMessage());
        } catch (RuntimeException ex) {
            showError("Lỗi", ex.getMessage());
        }
    }

    private PhongDAO.LoaiPhongItem buildRoomTypeFromForm(String excludeMaLoai) {
        String loaiPhong = normalizeRoomTypeName(txtLoaiPhong != null ? txtLoaiPhong.getText() : null);
        String maLoai = normalizeRoomTypeCode(txtMaLoai != null ? txtMaLoai.getText() : null);

        if (loaiPhong == null) {
            throw new IllegalArgumentException("Tên loại phòng không được để trống.");
        }
        if (maLoai == null) {
            throw new IllegalArgumentException("Mã loại không được để trống.");
        }
        if (dao.existsByLoaiPhong(loaiPhong, excludeMaLoai)) {
            throw new IllegalArgumentException("Tên loại phòng đã tồn tại.");
        }
        if (dao.existsByMaLoai(maLoai, excludeMaLoai)) {
            throw new IllegalArgumentException("Mã loại đã tồn tại. Vui lòng nhập mã khác.");
        }

        txtLoaiPhong.setText(loaiPhong);
        txtMaLoai.setText(maLoai);
        return new PhongDAO.LoaiPhongItem(loaiPhong, maLoai);
    }

    private void fillForm(PhongDAO.LoaiPhongItem item) {
        if (item == null) {
            return;
        }
        txtLoaiPhong.setText(item.getLoaiPhong());
        txtMaLoai.setText(item.getMaLoai());
        originalSnapshot = new PhongDAO.LoaiPhongItem(item.getLoaiPhong(), item.getMaLoai());
    }

    private void resetForm() {
        originalSnapshot = null;
        if (txtLoaiPhong != null) {
            txtLoaiPhong.clear();
        }
        if (txtMaLoai != null) {
            txtMaLoai.clear();
        }
        tableView.getSelectionModel().clearSelection();
    }

    private boolean confirmCloseIfDirty() {
        if (!isFormDirty()) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có thay đổi chưa lưu. Bạn vẫn muốn đóng popup?",
                ButtonType.YES, ButtonType.NO);
        alert.initOwner(stage);
        alert.setTitle("Xác nhận đóng");
        alert.setHeaderText(null);
        return alert.showAndWait().filter(result -> result == ButtonType.YES).isPresent();
    }

    private boolean isFormDirty() {
        String currentLoaiPhong = normalizeRoomTypeName(txtLoaiPhong != null ? txtLoaiPhong.getText() : null);
        String currentMaLoai = normalizeRoomTypeCode(txtMaLoai != null ? txtMaLoai.getText() : null);

        if (originalSnapshot == null) {
            return currentLoaiPhong != null || currentMaLoai != null;
        }

        return !equalsIgnoreCaseTrimmed(originalSnapshot.getLoaiPhong(), currentLoaiPhong)
                || !equalsIgnoreCaseTrimmed(originalSnapshot.getMaLoai(), currentMaLoai);
    }

    private boolean hasChanges(PhongDAO.LoaiPhongItem original, PhongDAO.LoaiPhongItem updated) {
        return !equalsIgnoreCaseTrimmed(original.getLoaiPhong(), updated.getLoaiPhong())
                || !equalsIgnoreCaseTrimmed(original.getMaLoai(), updated.getMaLoai());
    }

    private boolean equalsIgnoreCaseTrimmed(String left, String right) {
        String normalizedLeft = trimToNull(left);
        String normalizedRight = trimToNull(right);
        if (normalizedLeft == null || normalizedRight == null) {
            return Objects.equals(normalizedLeft, normalizedRight);
        }
        return normalizedLeft.equalsIgnoreCase(normalizedRight);
    }

    private void selectByCode(String maLoai) {
        if (maLoai == null) {
            return;
        }
        for (PhongDAO.LoaiPhongItem item : tableView.getItems()) {
            if (equalsIgnoreCaseTrimmed(item.getMaLoai(), maLoai)) {
                tableView.getSelectionModel().select(item);
                tableView.scrollTo(item);
                fillForm(item);
                break;
            }
        }
    }

    private String normalizeRoomTypeName(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (String part : trimmed.replaceAll("\\s+", " ").split(" ")) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            if (part.length() == 1) {
                builder.append(part.toUpperCase(Locale.ROOT));
                continue;
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            builder.append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private String normalizeRoomTypeCode(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private VBox formRow(String labelText, TextField field) {
        Label label = new Label(labelText);
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        label.setTextFill(Color.web(C_TEXT));
        return new VBox(6, label, field);
    }

    private Label helperText(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", 12));
        label.setTextFill(Color.web(C_MUTED));
        label.setWrapText(true);
        return label;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        label.setTextFill(Color.web(C_TEXT));
        return label;
    }

    private TextField styledField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(40);
        field.setStyle(
                "-fx-background-color: #f8fafc;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-padding: 0 12;" +
                        "-fx-font-size: 13;"
        );
        return field;
    }

    private Button filledBtn(String text) {
        Button button = new Button(text);
        button.setPrefHeight(40);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String base = "-fx-background-color:" + AppTheme.TEAL + ";-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;";
        String hover = "-fx-background-color:derive(" + AppTheme.TEAL + ",-14%);-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
        return button;
    }

    private Button outlineBtn(String text) {
        Button button = new Button(text);
        button.setPrefHeight(40);
        button.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        String base = "-fx-background-color:transparent;-fx-text-fill:" + C_PRIMARY + ";-fx-border-color:" + C_PRIMARY + ";-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:0 16;";
        String hover = "-fx-background-color:#f0f4f8;-fx-text-fill:" + C_PRIMARY + ";-fx-border-color:" + C_PRIMARY + ";-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;-fx-padding:0 16;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
        return button;
    }

    private String cardStyle() {
        return "-fx-background-color: white;" +
                "-fx-background-radius: 18;" +
                "-fx-border-radius: 18;" +
                "-fx-border-color: #e2e8f0;" +
                "-fx-border-width: 1;";
    }

    private String tableShellStyle() {
        return "-fx-background-color: white;" +
                "-fx-background-radius: 16;" +
                "-fx-border-radius: 16;" +
                "-fx-border-color: #edf2f7;" +
                "-fx-border-width: 1;" +
                "-fx-padding: 0;";
    }

    private DropShadow cardShadow() {
        return softShadow(14, 0.07, 4);
    }

    private DropShadow softShadow(int radius, double opacity, double offsetY) {
        DropShadow shadow = new DropShadow(radius, Color.rgb(15, 23, 42, opacity));
        shadow.setOffsetY(offsetY);
        return shadow;
    }

    private VBox buildEmptyState() {
        Label icon = new Label("LP");
        icon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        icon.setTextFill(Color.web(C_PRIMARY));
        icon.setAlignment(Pos.CENTER);
        icon.setMinSize(44, 44);
        icon.setPrefSize(44, 44);
        icon.setMaxSize(44, 44);
        icon.setStyle(
                "-fx-background-color: #eff6ff;" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: #dbeafe;" +
                        "-fx-border-radius: 22;" +
                        "-fx-border-width: 1;"
        );

        Label title = new Label("Chưa có loại phòng nào.");
        title.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        title.setTextFill(Color.web("#475569"));

        Label subtitle = new Label("Thêm loại phòng mới để sử dụng trong quản lý phòng.");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web(C_MUTED));
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(240);
        subtitle.setAlignment(Pos.CENTER);

        VBox box = new VBox(10, icon, title, subtitle);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28, 20, 28, 20));
        return box;
    }

    private static String buildTableCss() {
        String css =
                ".room-type-table{" +
                        "-fx-background-color:white;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:#e2e8f0;" +
                        "-fx-border-radius:16;" +
                        "-fx-border-width:1;" +
                        "-fx-padding:0;" +
                        "-fx-table-cell-border-color:transparent;" +
                        "}" +
                        ".room-type-table .column-header-background{" +
                        "-fx-background-color:#f8fafc;" +
                        "-fx-background-radius:16 16 0 0;" +
                        "}" +
                        ".room-type-table .filler{" +
                        "-fx-background-color:#f8fafc;" +
                        "-fx-border-color:transparent transparent #e5e7eb transparent;" +
                        "}" +
                        ".room-type-table .column-header{" +
                        "-fx-background-color:transparent;" +
                        "-fx-border-color:transparent transparent #e5e7eb transparent;" +
                        "-fx-border-width:0 0 1 0;" +
                        "-fx-size:50;" +
                        "}" +
                        ".room-type-table .column-header .label{" +
                        "-fx-text-fill:#334155;" +
                        "-fx-font-weight:bold;" +
                        "-fx-font-size:12px;" +
                        "-fx-alignment:center;" +
                        "-fx-text-alignment:center;" +
                        "-fx-padding:0 16 0 16;" +
                        "}" +
                        ".room-type-table .virtual-flow{" +
                        "-fx-background-color:transparent;" +
                        "}" +
                        ".room-type-table .table-row-cell{" +
                        "-fx-background-color:white;" +
                        "-fx-cell-size:50px;" +
                        "-fx-padding:0 10 0 10;" +
                        "-fx-border-color:transparent;" +
                        "}" +
                        ".room-type-table .table-row-cell:odd{" +
                        "-fx-background-color:#fafbfd;" +
                        "}" +
                        ".room-type-table .table-row-cell:hover{" +
                        "-fx-background-color:#f8fbff;" +
                        "-fx-cursor:hand;" +
                        "}" +
                        ".room-type-table .table-row-cell:selected{" +
                        "-fx-background-color:#ecfeff;" +
                        "}" +
                        ".room-type-table .table-row-cell:selected .table-cell{" +
                        "-fx-text-fill:#0f766e;" +
                        "-fx-font-weight:bold;" +
                        "}" +
                        ".room-type-table .table-cell{" +
                        "-fx-border-color:transparent transparent #eef2f7 transparent;" +
                        "-fx-border-width:0 0 1 0;" +
                        "-fx-padding:0 16 0 16;" +
                        "-fx-font-size:13px;" +
                        "-fx-text-fill:#334155;" +
                        "-fx-alignment:CENTER-LEFT;" +
                        "}" +
                        ".room-type-table .placeholder{" +
                        "-fx-background-color:transparent;" +
                        "}" +
                        ".room-type-table .placeholder .label{" +
                        "-fx-text-fill:#94a3b8;" +
                        "-fx-font-size:14px;" +
                        "}" +
                        ".room-type-table .scroll-bar:vertical," +
                        ".room-type-table .scroll-bar:horizontal{" +
                        "-fx-background-color:transparent;" +
                        "}" +
                        ".room-type-table .scroll-bar:vertical{" +
                        "-fx-pref-width:8;" +
                        "}" +
                        ".room-type-table .scroll-bar:horizontal{" +
                        "-fx-pref-height:8;" +
                        "}" +
                        ".room-type-table .scroll-bar .thumb{" +
                        "-fx-background-color:#cbd5e1;" +
                        "-fx-background-radius:999;" +
                        "}" +
                        ".room-type-table .scroll-bar .thumb:hover{" +
                        "-fx-background-color:#94a3b8;" +
                        "}" +
                        ".room-type-table .scroll-bar .track," +
                        ".room-type-table .scroll-bar .increment-button," +
                        ".room-type-table .scroll-bar .decrement-button{" +
                        "-fx-background-color:transparent;" +
                        "-fx-padding:0;" +
                        "}" +
                        ".room-type-table .corner{" +
                        "-fx-background-color:white;" +
                        "}";
        try {
            return "data:text/css," + URLEncoder.encode(css, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception ex) {
            return "";
        }
    }

    private void showInfo(String title, String message) {
        showMessage(Alert.AlertType.INFORMATION, title, message);
    }

    private void showWarning(String title, String message) {
        showMessage(Alert.AlertType.WARNING, title, message);
    }

    private void showError(String title, String message) {
        showMessage(Alert.AlertType.ERROR, title, message);
    }

    private void showMessage(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
