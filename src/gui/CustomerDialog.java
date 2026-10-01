package gui;

import entity.KhachHang;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import util.AppTheme;
import util.TextInputSafeUtils;

public class CustomerDialog {

    // ── palette ──────────────────────────────────────────────────────
    private static final String C_PRIMARY = AppTheme.PRIMARY;
    private static final String C_ACCENT = AppTheme.TEAL_DARK;
    private static final String C_SUCCESS = AppTheme.SUCCESS; // #0f766e
    private static final String C_DANGER = AppTheme.DANGER; // #dc2626
    private static final String C_BORDER = AppTheme.BORDER; // #d7deea
    private static final String C_BG = AppTheme.BG_LIGHT; // #f4f7fb
    private static final String C_TEXT = AppTheme.TEXT; // #233044
    private static final String C_MUTED = AppTheme.MUTED; // #64748b

    private final Stage stage;
    private final KhachHang original;
    private final boolean isEditMode;
    private final KhachHangService service;
    private final Runnable onSuccess;
    private KhachHang savedCustomer;

    private VBox root;
    private TextField txtTen;
    private TextField txtSdt;
    private TextField txtDiaChi;
    private TextField txtSoGiayTo;
    private ComboBox<String> cbGioiTinh;
    private ComboBox<String> cbLoaiGiayTo;
    private ComboBox<String> cbTrangThai;

    public CustomerDialog(KhachHang khachHang, KhachHangService service, Runnable onSuccess) {
        this(null, khachHang, service, onSuccess);
    }

    public CustomerDialog(Window owner, KhachHang khachHang, KhachHangService service, Runnable onSuccess) {
        this.original = khachHang;
        this.service = service;
        this.onSuccess = onSuccess;
        this.isEditMode = (khachHang != null);

        stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setResizable(false);
        stage.setTitle(isEditMode ? "Chi tiết khách hàng" : "Thêm khách hàng mới");

        root = buildUI();
        stage.setScene(new Scene(root, 440, isEditMode ? 650 : 610));

        // fade + scale animation khi mở
        root.setOpacity(0);
        root.setScaleX(0.95);
        root.setScaleY(0.95);
        stage.setOnShown(e -> {
            stage.toFront();
            stage.requestFocus();
            FadeTransition fade = new FadeTransition(Duration.millis(180), root);
            fade.setFromValue(0);
            fade.setToValue(1);
            ScaleTransition scale = new ScaleTransition(Duration.millis(180), root);
            scale.setFromX(0.95);
            scale.setToX(1.0);
            scale.setFromY(0.95);
            scale.setToY(1.0);
            new ParallelTransition(fade, scale).play();
            Platform.runLater(() -> txtTen.requestFocus());
        });

        util.ModalUtils.setupModalBehavior(stage, true, true, this::isDirty);
    }

    public void show() {
        stage.showAndWait();
    }

    public KhachHang showAndGetCustomer() {
        show();
        return savedCustomer;
    }

    public void setInitialPhone(String phone) {
        if (!isEditMode && txtSdt != null) {
            txtSdt.setText(phone == null ? "" : phone.trim());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // BUILD UI
    // ═════════════════════════════════════════════════════════════════

    private VBox buildUI() {
        VBox ui = new VBox(0);
        ui.setStyle("-fx-background-color: " + C_BG + ";");
        ui.getChildren().addAll(buildHeader(), buildFormCard(), buildFooter());
        return ui;
    }

    // ─── Header ──────────────────────────────────────────────────────
    private StackPane buildHeader() {
        // gradient strip
        VBox strip = new VBox(4);
        strip.setPadding(new Insets(22, 24, 22, 24));
        strip.setStyle(
                "-fx-background-color: linear-gradient(to right, " + AppTheme.TEAL_DARK + ", " + AppTheme.PRIMARY
                        + ");");

        String emoji = "👤";
        Label lblIcon = new Label(emoji);
        lblIcon.setFont(Font.font(20));

        Label lblTitle = new Label(isEditMode ? "Chỉnh sửa khách hàng" : "Thêm khách hàng mới");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        lblTitle.setTextFill(Color.WHITE);

        Label lblSub = new Label(isEditMode
                ? "Mã KH: " + original.getMaKhachHang()
                : "Điền đầy đủ thông tin rồi bấm lưu ");
        lblSub.setFont(Font.font("Segoe UI", 12));
        lblSub.setTextFill(Color.web("#a8c4e0"));

        HBox titleRow = new HBox(10, lblIcon, new VBox(2, lblTitle, lblSub));
        titleRow.setAlignment(Pos.CENTER_LEFT);
        strip.getChildren().add(titleRow);

        // trash icon — chỉ hiện ở edit mode
        if (isEditMode) {
            Button btnAction = buildHeaderActions();
            StackPane.setAlignment(btnAction, Pos.TOP_RIGHT);
            StackPane.setMargin(btnAction, new Insets(10, 10, 0, 0));
            return new StackPane(strip, btnAction);
        }
        return new StackPane(strip);
    }

    // ─── Form card ───────────────────────────────────────────────────
    private VBox buildFormCard() {
        cbLoaiGiayTo = styledCombo(KhachHangService.GIAY_TO_CCCD, KhachHangService.GIAY_TO_HO_CHIEU);
        txtSoGiayTo = styledField("");
        installDocumentTextFormatter();
        txtTen = styledField("Nguyễn Văn A");
        txtTen.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                txtTen.setText(capitalizeWords(txtTen.getText()));
            }
        });
        txtSdt = styledField("0912 345 678");
        txtDiaChi = styledField("Số nhà, đường, quận, tỉnh/thành phố");
        cbGioiTinh = styledCombo("Nam", "Nữ");
        cbTrangThai = styledCombo(KhachHang.TRANG_THAI_DANG_HOAT_DONG, KhachHang.TRANG_THAI_VO_HIEU_HOA);
        cbTrangThai.setValue(KhachHang.TRANG_THAI_DANG_HOAT_DONG);

        if (isEditMode) {
            txtTen.setText(safe(original.getTenKhachHang()));
            txtSdt.setText(safe(original.getSoDienThoai()));
            txtDiaChi.setText(safe(original.getDiaChi()).trim());
            if (original.getLoaiGiayTo() != null && !original.getLoaiGiayTo().isBlank()) {
                cbLoaiGiayTo.setValue(original.getLoaiGiayTo());
            }
            txtSoGiayTo.setText(safe(original.getSoGiayTo()));
            syncDocumentPromptAndInput();
            cbGioiTinh.setValue(original.isGioiTinh() ? "Nam" : "Nữ");
            cbTrangThai.setValue(original.getTrangThai());
        }

        syncDocumentPromptAndInput();
        cbLoaiGiayTo.setOnAction(e -> syncDocumentPromptAndInput());

        VBox card = new VBox(16);
        card.setPadding(new Insets(24, 24, 20, 24));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-width: 1;");
        DropShadow sh = new DropShadow(10, Color.rgb(0, 0, 0, 0.07));
        sh.setOffsetY(3);
        card.setEffect(sh);

        card.getChildren().addAll(
                formRow("Họ và tên *", txtTen),
                formRow("Số điện thoại *", txtSdt),
                formRow("Địa chỉ", txtDiaChi),
                formRow("Giới tính", cbGioiTinh),
                formRow("Trạng thái", cbTrangThai));

        card.getChildren().add(2, formRow("Giấy tờ  *", cbLoaiGiayTo));
        card.getChildren().add(3, formRow("Số CCCD / Hộ chiếu *", txtSoGiayTo));

        VBox.setVgrow(card, Priority.ALWAYS);

        VBox wrap = new VBox(card);
        wrap.setPadding(new Insets(16, 16, 12, 16));
        VBox.setVgrow(wrap, Priority.ALWAYS);
        return wrap;
    }

    // ─── Footer buttons ───────────────────────────────────────────────
    private HBox buildFooter() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(4, 24, 20, 24));

        if (!isEditMode) {
            Button btnHuy = ghostBtn("Hủy", 90);
            Button btnLuu = primaryBtn("Lưu", AppTheme.TEAL, 110);
            btnHuy.setOnAction(e -> {
                if (isDirty() && !util.ModalUtils.confirmClose(stage)) {
                    return;
                }
                stage.close();
            });
            btnLuu.setOnAction(e -> handleCreate());
            bar.getChildren().addAll(btnHuy, btnLuu);
        } else {
            Button btnCapNhat = primaryBtn("👤  Cập nhật", AppTheme.TEAL, 160);
            btnCapNhat.setOnAction(e -> handleUpdate());
            bar.getChildren().add(btnCapNhat);
        }
        return bar;
    }

    private boolean isDirty() {
        String ten = txtTen.getText().trim();
        String sdt = txtSdt.getText().trim();
        String diaChi = txtDiaChi.getText().trim();
        String loaiGiayTo = cbLoaiGiayTo.getValue();
        String soGiayTo = txtSoGiayTo.getText().trim();
        String trangThai = cbTrangThai.getValue();
        boolean gt = "Nam".equals(cbGioiTinh.getValue());

        if (!isEditMode) {
            return !ten.isEmpty() || !sdt.isEmpty() || !diaChi.isEmpty() || !soGiayTo.isEmpty()
                    || !KhachHang.TRANG_THAI_DANG_HOAT_DONG.equals(trangThai);
        }

        KhachHang updated = new KhachHang(
                original.getMaKhachHang(), ten,
                diaChi.isEmpty() ? " " : diaChi, gt, sdt, loaiGiayTo, soGiayTo, trangThai);
        return service.hasChanges(original, updated);
    }

    // ═════════════════════════════════════════════════════════════════
    // BUSINESS LOGIC (không đổi)
    // ═════════════════════════════════════════════════════════════════

    private void handleCreate() {
        String ten = txtTen.getText().trim();
        String sdt = txtSdt.getText().trim();
        String diaChi = txtDiaChi.getText().trim();
        String loaiGiayTo = service.normalizeLoaiGiayTo(cbLoaiGiayTo.getValue());
        String soGiayTo = txtSoGiayTo.getText().trim();
        String trangThai = cbTrangThai.getValue();
        boolean gt = "Nam".equals(cbGioiTinh.getValue());

        String e1 = service.validateTen(txtTen.getText());
        if (e1 != null) {
            showError(e1);
            txtTen.requestFocus();
            return;
        }
        String e2 = service.validateSdt(sdt);
        if (e2 != null) {
            showError(e2);
            txtSdt.requestFocus();
            return;
        }
        String e3 = service.validateSoGiayTo(loaiGiayTo, soGiayTo);
        if (e3 != null) {
            showError(e3);
            txtSoGiayTo.requestFocus();
            return;
        }

        try {
            String newId = service.generateNextId();
            KhachHang kh = new KhachHang();
            kh.setMaKhachHang(newId);
            kh.setTenKhachHang(ten);
            kh.setDiaChi(diaChi.isEmpty() ? " " : diaChi);
            kh.setSoDienThoai(sdt);
            kh.setGioiTinh(gt);
            kh.setLoaiGiayTo(loaiGiayTo);
            kh.setSoGiayTo(soGiayTo);
            kh.setTrangThai(trangThai);
            if (service.create(kh)) {
                savedCustomer = kh;
                showInfo("Thành công", "Đã thêm khách hàng mới!\nMã KH: " + newId + "\nTên: " + ten);
                if (onSuccess != null) {
                    onSuccess.run();
                }
                stage.close();
            } else {
                showError("Không thể thêm. Số điện thoại có thể đã tồn tại.");
            }
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (RuntimeException ex) {
            if (!handleDuplicateCustomerOnCreate(sdt, soGiayTo, ex)) {
                showError(ex.getMessage());
            }
        }
    }

    private void handleUpdate() {
        String ten = txtTen.getText().trim();
        String sdt = txtSdt.getText().trim();
        String diaChi = txtDiaChi.getText().trim();
        String loaiGiayTo = service.normalizeLoaiGiayTo(cbLoaiGiayTo.getValue());
        String soGiayTo = txtSoGiayTo.getText().trim();
        String trangThai = cbTrangThai.getValue();
        boolean gt = "Nam".equals(cbGioiTinh.getValue());

        String e1 = service.validateTen(txtTen.getText());
        if (e1 != null) {
            showError(e1);
            txtTen.requestFocus();
            return;
        }
        String e2 = service.validateSdt(sdt);
        if (e2 != null) {
            showError(e2);
            txtSdt.requestFocus();
            return;
        }
        String e3 = service.validateSoGiayTo(loaiGiayTo, soGiayTo);
        if (e3 != null) {
            showError(e3);
            txtSoGiayTo.requestFocus();
            return;
        }

        KhachHang updated = new KhachHang(
                original.getMaKhachHang(), ten,
                diaChi.isEmpty() ? " " : diaChi, gt, sdt, loaiGiayTo, soGiayTo, trangThai);

        if (service.existsByPhoneExceptCustomerId(sdt, original.getMaKhachHang())) {
            showError("So dien thoai nay da duoc dang ky cho khach hang khac.");
            txtSdt.requestFocus();
            return;
        }
        if (service.existsByDocumentExceptCustomerId(soGiayTo, original.getMaKhachHang())) {
            showError("So CCCD / Ho chieu nay da duoc dang ky cho khach hang khac.");
            txtSoGiayTo.requestFocus();
            return;
        }

        if (!service.hasChanges(original, updated)) {
            showInfo("Thông báo", "Không có thay đổi nào để cập nhật.");
            return;
        }
        try {
            if (service.update(updated)) {
                savedCustomer = updated;
                showInfo("Thành công", "Đã cập nhật thông tin khách hàng!");
                if (onSuccess != null) {
                    onSuccess.run();
                }
                stage.close();
            } else {
                showError("Cập nhật thất bại. Vui lòng thử lại.");
            }
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (RuntimeException ex) {
            if (!handleDuplicateCustomerOnUpdate(sdt, soGiayTo, ex)) {
                showError(ex.getMessage());
            }
        }
    }

    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(stage);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa khách hàng: " + original.getTenKhachHang());
        confirm.setContentText("Bạn có chắc chắn muốn xóa?\nHành động này không thể hoàn tác.");
        ButtonType yes = new ButtonType("Có, xóa", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("Không", ButtonBar.ButtonData.NO);
        confirm.getButtonTypes().setAll(yes, no);
        confirm.showAndWait().ifPresent(r -> {
            if (r == yes) {
                if (service.delete(original.getMaKhachHang())) {
                    savedCustomer = null;
                    showInfo("Đã xóa", "Đã xóa khách hàng: " + original.getTenKhachHang());
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    stage.close();
                } else {
                    showError("Không thể xóa. Khách hàng có thể đang liên kết với phiếu đặt phòng.");
                }
            }
        });
    }

    private void handleDeactivate() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(stage);
        confirm.setTitle("Xác nhận vô hiệu hóa");
        confirm.setHeaderText("Khách hàng này đã phát sinh hóa đơn nên không thể xóa.");
        confirm.setContentText(
                "Bạn có chắc chắn muốn vô hiệu hóa khách hàng thay thế?\n(Khách hàng vô hiệu hóa sẽ không thể tạo phiếu đặt phòng mới)");
        ButtonType yes = new ButtonType("Vô hiệu hóa", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("Hủy", ButtonBar.ButtonData.NO);
        confirm.getButtonTypes().setAll(yes, no);
        confirm.showAndWait().ifPresent(r -> {
            if (r == yes) {
                if (service.updateTrangThai(original.getMaKhachHang(), false)) {
                    original.setDangHoatDong(false);
                    if (cbTrangThai != null) {
                        cbTrangThai.setValue(original.getTrangThai());
                    }
                    savedCustomer = original;
                    showInfo("Thành công", "Đã vô hiệu hóa khách hàng: " + original.getTenKhachHang());
                    if (onSuccess != null)
                        onSuccess.run();
                    stage.close();
                } else {
                    showError("Vô hiệu hóa thất bại. Vui lòng thử lại.");
                }
            }
        });
    }

    private void handleActivate() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(stage);
        confirm.setTitle("Kích hoạt lại khách hàng");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có muốn kích hoạt lại khách hàng này để có thể tiếp tục tạo phiếu đặt phòng?");
        ButtonType yes = new ButtonType("Kích hoạt", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("Hủy", ButtonBar.ButtonData.NO);
        confirm.getButtonTypes().setAll(yes, no);
        confirm.showAndWait().ifPresent(r -> {
            if (r == yes) {
                if (service.updateTrangThai(original.getMaKhachHang(), true)) {
                    original.setDangHoatDong(true);
                    if (cbTrangThai != null) {
                        cbTrangThai.setValue(original.getTrangThai());
                    }
                    savedCustomer = original;
                    showInfo("Thành công", "Đã kích hoạt lại khách hàng: " + original.getTenKhachHang());
                    if (onSuccess != null)
                        onSuccess.run();
                    stage.close();
                } else {
                    showError("Kích hoạt thất bại. Vui lòng thử lại.");
                }
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════
    // UI HELPERS
    // ═════════════════════════════════════════════════════════════════

    private boolean handleDuplicateCustomerOnCreate(String sdt, String soGiayTo, RuntimeException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        KhachHang existing = null;

        if (message.contains("UQ_KhachHang_GiayTo")) {
            existing = service.findByPhoneOrDocument(soGiayTo);
        } else if (message.contains("soDienThoai")
                || message.contains("UQ_KhachHang_Sdt")
                || message.contains("UQ_KhachHang_SoDienThoai")) {
            existing = service.findByPhoneOrDocument(sdt);
        } else {
            existing = service.findByPhoneOrDocument(soGiayTo);
            if (existing == null) {
                existing = service.findByPhoneOrDocument(sdt);
            }
        }

        if (existing == null) {
            return false;
        }

        final KhachHang matchedCustomer = existing;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(stage);
        confirm.setTitle("Khách hàng đã tồn tại");
        confirm.setHeaderText("Thông tin này đã thuộc về một khách hàng có sẵn.");
        confirm.setContentText("Mã KH: " + safe(matchedCustomer.getMaKhachHang())
                + "\nTên: " + safe(matchedCustomer.getTenKhachHang())
                + "\nSĐT: " + safe(matchedCustomer.getSoDienThoai())
                + "\nBạn có muốn dùng khách hàng này không?");
        ButtonType useExisting = new ButtonType("Dùng khách này", ButtonBar.ButtonData.YES);
        ButtonType keepEditing = new ButtonType("Ở lại chỉnh sửa", ButtonBar.ButtonData.NO);
        confirm.getButtonTypes().setAll(useExisting, keepEditing);
        confirm.showAndWait().ifPresent(result -> {
            if (result == useExisting) {
                savedCustomer = matchedCustomer;
                stage.close();
            }
        });
        return true;
    }

    private boolean handleDuplicateCustomerOnUpdate(String sdt, String soGiayTo, RuntimeException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        String originalId = original == null ? "" : safe(original.getMaKhachHang());

        if (message.contains("UQ_KhachHang_GiayTo")
                || message.toLowerCase().contains("sogiayto")
                || message.toLowerCase().contains("so giay to")) {
            KhachHang existing = service.findByPhoneOrDocument(soGiayTo);
            if (existing != null && !safe(existing.getMaKhachHang()).equals(originalId)) {
                showError("So CCCD / Ho chieu nay da duoc dang ky cho khach hang khac.");
                txtSoGiayTo.requestFocus();
                return true;
            }
        }

        if (message.contains("soDienThoai")
                || message.contains("UQ_KhachHang_Sdt")
                || message.contains("UQ_KhachHang_SoDienThoai")) {
            KhachHang existing = service.findByPhoneOrDocument(sdt);
            if (existing != null && !safe(existing.getMaKhachHang()).equals(originalId)) {
                showError("So dien thoai nay da duoc dang ky cho khach hang khac.");
                txtSdt.requestFocus();
                return true;
            }
        }

        return false;
    }

    private Button buildHeaderActions() {
        boolean isActive = original.isDangHoatDong();
        boolean hasInvoice = service.hasInvoice(original.getMaKhachHang());

        Button btn = new Button();
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        if (!isActive) {
            btn.setText("Kích hoạt lại");
            btn.setStyle(
                    "-fx-background-color: #ecfdf5; -fx-text-fill: #059669; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 12;");
            btn.setOnAction(e -> handleActivate());
        } else if (hasInvoice) {
            btn.setText("Vô hiệu hóa");
            btn.setStyle(
                    "-fx-background-color: #fff7ed; -fx-text-fill: #d97706; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 12;");
            btn.setOnAction(e -> handleDeactivate());
        } else {
            btn.setText("Xóa");
            btn.setStyle(
                    "-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 12;");
            btn.setOnAction(e -> handleDelete());
        }
        return btn;
    }

    private void syncDocumentPromptAndInput() {
        if (txtSoGiayTo == null || cbLoaiGiayTo == null) {
            return;
        }
        boolean isCccd = KhachHangService.GIAY_TO_CCCD.equals(cbLoaiGiayTo.getValue());
        txtSoGiayTo.setPromptText(isCccd ? "Nhập số CCCD..." : "Nhập số hộ chiếu...");
        String sanitized = sanitizeDocumentInput(txtSoGiayTo.getText());
        if (!sanitized.equals(txtSoGiayTo.getText())) {
            TextInputSafeUtils.safeSetText(txtSoGiayTo, sanitized);
        }
    }

    private void installDocumentTextFormatter() {
        if (txtSoGiayTo == null) {
            return;
        }
        txtSoGiayTo.setTextFormatter(TextInputSafeUtils.filteringFormatter(this::sanitizeDocumentInput));
    }

    private String sanitizeDocumentInput(String value) {
        String safeValue = value == null ? "" : value;
        boolean isCccd = cbLoaiGiayTo != null
                && KhachHangService.GIAY_TO_CCCD.equals(cbLoaiGiayTo.getValue());
        String sanitized = isCccd
                ? safeValue.replaceAll("\\D", "")
                : safeValue.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return sanitized.length() > 12 ? sanitized.substring(0, 12) : sanitized;
    }

    private String capitalizeWords(String text) {
        if (text == null || text.trim().isEmpty())
            return text;
        String[] words = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(42);
        tf.setMaxWidth(Double.MAX_VALUE);
        String base = "-fx-background-color: #f8fafc;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: " + C_BORDER + ";" +
                "-fx-border-radius: 8;" +
                "-fx-border-width: 1.5;" +
                "-fx-padding: 0 12;" +
                "-fx-font-size: 13;";
        String focused = "-fx-background-color: white;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: " + C_ACCENT + ";" +
                "-fx-border-radius: 8;" +
                "-fx-border-width: 1.5;" +
                "-fx-padding: 0 12;" +
                "-fx-font-size: 13;";
        tf.setStyle(base);
        tf.focusedProperty().addListener((obs, o, focused2) -> tf.setStyle(focused2 ? focused : base));
        return tf;
    }

    private ComboBox<String> styledCombo(String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setValue(items[0]);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setPrefHeight(42);
        cb.setStyle(
                "-fx-background-color: #f8fafc;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-font-size: 13;");
        return cb;
    }

    private VBox formRow(String labelText, Control field) {
        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        lbl.setTextFill(Color.web(C_TEXT));
        VBox row = new VBox(6, lbl, field);
        VBox.setVgrow(field, Priority.NEVER);
        return row;
    }

    private Button primaryBtn(String text, String bg, double w) {
        Button btn = new Button(text);
        btn.setPrefWidth(w);
        btn.setPrefHeight(42);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String base = "-fx-background-color:" + bg + ";-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;";
        String hover = "-fx-background-color:derive(" + bg
                + ",-14%);-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(13,148,136,0.20),10,0,0,3);";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Button ghostBtn(String text, double w) {
        Button btn = new Button(text);
        btn.setPrefWidth(w);
        btn.setPrefHeight(42);
        btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        String base = "-fx-background-color:#f8fafc;-fx-text-fill:" + C_MUTED + ";-fx-border-color:" + C_BORDER
                + ";-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;";
        String hover = "-fx-background-color:#e2e8f0;-fx-text-fill:" + C_TEXT
                + ";-fx-border-color:#cbd5e1;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.initOwner(stage);
        a.setTitle("Lỗi");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.initOwner(stage);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
