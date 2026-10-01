package gui;

import dao.NhanVienDAO;
import entity.NhanVien;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ChangePasswordView {

    private static final String C_BG = util.AppTheme.BG_LIGHT;
    private static final String C_SURFACE = util.AppTheme.SURFACE;
    private static final String C_PRIMARY = util.AppTheme.PRIMARY;
    private static final String C_ACCENT = util.AppTheme.TEAL_DARK;
    private static final String C_SUCCESS = util.AppTheme.SUCCESS;
    private static final String C_DANGER = util.AppTheme.DANGER;
    private static final String C_TEXT = util.AppTheme.TEXT;
    private static final String C_MUTED = util.AppTheme.MUTED;
    private static final String C_BORDER = util.AppTheme.BORDER;

    private final NhanVien currentUser;
    private final NhanVienDAO nhanVienDAO = new NhanVienDAO();

    private PasswordField txtOldPass;
    private PasswordField txtNewPass;
    private PasswordField txtConfirm;
    private Label lblStatus;

    public ChangePasswordView(NhanVien currentUser) {
        this.currentUser = currentUser;
    }

    public Node createView() {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: " + C_BG + ";");
        root.setAlignment(Pos.CENTER);

        VBox card = new VBox(24);
        card.setPadding(new Insets(40, 48, 44, 48));
        card.setMaxWidth(520);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle(
                "-fx-background-color: " + C_SURFACE + ";"
                        + "-fx-background-radius: 16;"
                        + "-fx-border-color: " + C_BORDER + "88;"
                        + "-fx-border-radius: 16;"
                        + "-fx-border-width: 1;"
        );
        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow(12, Color.rgb(0, 0, 0, 0.04));
        shadow.setOffsetY(6);
        card.setEffect(shadow);

        HBox headerBox = new HBox(16);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.shape.Circle avatar = new javafx.scene.shape.Circle(24);
        avatar.setFill(Color.web(util.AppTheme.TEAL_LIGHT));
        Label icon = new Label("🔒");
        icon.setFont(Font.font(20));
        icon.setTextFill(Color.web(C_PRIMARY));
        StackPane iconWrap = new StackPane(avatar, icon);

        VBox titleTexts = new VBox(4);
        Label lblTitle = new Label("Đổi mật khẩu");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblTitle.setTextFill(Color.web(C_TEXT));

        Label lblSub = new Label("Cập nhật mật khẩu để bảo vệ tài khoản");
        lblSub.setFont(Font.font("Segoe UI", 13));
        lblSub.setTextFill(Color.web(C_MUTED));
        titleTexts.getChildren().addAll(lblTitle, lblSub);
        headerBox.getChildren().addAll(iconWrap, titleTexts);

        VBox form = new VBox(16);

        String inputBase = "-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 0 14; -fx-font-size: 14;";
        String inputFocus = "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: " + C_ACCENT + "; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 0 14; -fx-font-size: 14;";

        VBox boxOld = new VBox(6);
        Label lblOld = new Label("Mật khẩu hiện tại");
        lblOld.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        lblOld.setTextFill(Color.web(C_TEXT));
        txtOldPass = new PasswordField();
        txtOldPass.setPromptText("Nhập mật khẩu hiện tại");
        txtOldPass.setPrefHeight(42);
        txtOldPass.setMaxWidth(Double.MAX_VALUE);
        txtOldPass.setStyle(inputBase);
        txtOldPass.focusedProperty().addListener((o, ov, nv) -> txtOldPass.setStyle(nv ? inputFocus : inputBase));
        boxOld.getChildren().addAll(lblOld, txtOldPass);

        VBox boxNew = new VBox(6);
        Label lblNew = new Label("Mật khẩu mới");
        lblNew.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        lblNew.setTextFill(Color.web(C_TEXT));
        txtNewPass = new PasswordField();
        txtNewPass.setPromptText("Nhập mật khẩu mới");
        txtNewPass.setPrefHeight(42);
        txtNewPass.setMaxWidth(Double.MAX_VALUE);
        txtNewPass.setStyle(inputBase);
        txtNewPass.focusedProperty().addListener((o, ov, nv) -> txtNewPass.setStyle(nv ? inputFocus : inputBase));
        boxNew.getChildren().addAll(lblNew, txtNewPass);

        VBox boxConfirm = new VBox(6);
        Label lblConfirm = new Label("Xác nhận mật khẩu mới");
        lblConfirm.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        lblConfirm.setTextFill(Color.web(C_TEXT));
        txtConfirm = new PasswordField();
        txtConfirm.setPromptText("Nhập lại mật khẩu mới");
        txtConfirm.setPrefHeight(42);
        txtConfirm.setMaxWidth(Double.MAX_VALUE);
        txtConfirm.setStyle(inputBase);
        txtConfirm.focusedProperty().addListener((o, ov, nv) -> txtConfirm.setStyle(nv ? inputFocus : inputBase));
        boxConfirm.getChildren().addAll(lblConfirm, txtConfirm);

        form.getChildren().addAll(boxOld, boxNew, boxConfirm);

        HBox hintBox = new HBox(8);
        hintBox.setAlignment(Pos.CENTER_LEFT);
        hintBox.setPadding(new Insets(12, 16, 12, 16));
        hintBox.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 8; -fx-border-color: #bbf7d0; -fx-border-radius: 8;");

        Label lblHintIcon = new Label("💡");
        Label lblRules = new Label("Mật khẩu phải có ít nhất 6 ký tự và trùng khớp xác nhận.");
        lblRules.setFont(Font.font("Segoe UI", 13));
        lblRules.setTextFill(Color.web(C_SUCCESS));
        lblRules.setWrapText(true);
        hintBox.getChildren().addAll(lblHintIcon, lblRules);

        lblStatus = new Label();
        lblStatus.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        lblStatus.setWrapText(true);
        lblStatus.setPadding(new Insets(0, 0, 4, 0));

        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(16, 0, 0, 0));

        Button btnClear = new Button("Xóa tất cả");
        btnClear.setPrefHeight(42);
        btnClear.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        btnClear.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: " + C_TEXT + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 20;");
        btnClear.setOnMouseEntered(e -> btnClear.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: " + C_TEXT + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 20;"));
        btnClear.setOnMouseExited(e -> btnClear.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: " + C_TEXT + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 20;"));
        btnClear.setOnAction(e -> clearAllFields());

        Button btnChange = new Button("Đổi mật khẩu");
        btnChange.setPrefHeight(42);
        btnChange.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        btnChange.setStyle("-fx-background-color: " + util.AppTheme.TEAL + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 28;");
        btnChange.setOnMouseEntered(e -> btnChange.setStyle("-fx-background-color: " + util.AppTheme.TEAL_DARK + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 28;"));
        btnChange.setOnMouseExited(e -> btnChange.setStyle("-fx-background-color: " + util.AppTheme.TEAL + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 28;"));
        btnChange.setOnAction(e -> handleChangePassword());

        btnBox.getChildren().addAll(btnClear, btnChange);
        card.getChildren().addAll(headerBox, form, hintBox, lblStatus, btnBox);

        root.getChildren().add(card);
        VBox.setVgrow(root, Priority.ALWAYS);
        return root;
    }

    private void handleChangePassword() {
        String oldPass = txtOldPass.getText().trim();
        String newPass = txtNewPass.getText().trim();
        String confirm = txtConfirm.getText().trim();

        if (currentUser == null || currentUser.getMaNhanVien() == null || currentUser.getMaNhanVien().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không xác định được tài khoản đang đăng nhập.");
            setStatus("Không xác định được tài khoản đang đăng nhập.", C_DANGER);
            return;
        }
        if (oldPass.isEmpty()) {
            showValidationError("Vui lòng nhập mật khẩu hiện tại.", txtOldPass);
            return;
        }
        if (newPass.isEmpty()) {
            showValidationError("Vui lòng nhập mật khẩu mới.", txtNewPass);
            return;
        }
        if (confirm.isEmpty()) {
            showValidationError("Vui lòng nhập xác nhận mật khẩu mới.", txtConfirm);
            return;
        }
        if (newPass.length() < 6) {
            showValidationError("Mật khẩu mới phải có ít nhất 6 ký tự.", txtNewPass);
            return;
        }
        if (oldPass.equals(newPass)) {
            showValidationError("Mật khẩu mới không được trùng với mật khẩu hiện tại.", txtNewPass);
            return;
        }
        if (!newPass.equals(confirm)) {
            showValidationError("Mật khẩu xác nhận không khớp.", txtConfirm);
            return;
        }
        if (!nhanVienDAO.verifyPassword(currentUser.getMaNhanVien(), oldPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu hiện tại không đúng.");
            setStatus("Mật khẩu hiện tại không đúng.", C_DANGER);
            txtOldPass.requestFocus();
            txtOldPass.selectAll();
            return;
        }

        boolean updated = nhanVienDAO.updatePassword(currentUser.getMaNhanVien(), newPass);
        if (!updated) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật mật khẩu mới.");
            setStatus("Không thể cập nhật mật khẩu mới.", C_DANGER);
            return;
        }

        currentUser.setMatKhau(newPass);
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đổi mật khẩu thành công.");
        clearAllFields();
        setStatus("Đổi mật khẩu thành công.", C_SUCCESS);
    }

    private void clearAllFields() {
        txtOldPass.clear();
        txtNewPass.clear();
        txtConfirm.clear();
        lblStatus.setText("");
        txtOldPass.requestFocus();
    }

    private void showValidationError(String message, PasswordField field) {
        setStatus(message, C_DANGER);
        showAlert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", message);
        field.requestFocus();
    }

    private void setStatus(String msg, String hexColor) {
        lblStatus.setText(msg);
        lblStatus.setTextFill(Color.web(hexColor));
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
