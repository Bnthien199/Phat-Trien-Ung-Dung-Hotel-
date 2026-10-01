package gui;

import connectDB.ConnectDB;
import dao.NhanVienDAO;
import entity.NhanVien;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import javax.swing.SwingUtilities;
import java.nio.file.Paths;
import java.sql.SQLException;

public class LoginFrame {

    private final TextField txtUsername = new TextField();
    private final PasswordField txtPassword = new PasswordField();
    private final VBox usernameBox = new VBox(6);
    private final VBox passwordBox = new VBox(6);
    private final Label messageLabel = new Label();
    private final Label loginButtonIcon = new Label("\u2192");
    private final Button btnLogin = new Button("\u0110\u0103ng nh\u1eadp");

    public Scene createScene(Stage primaryStage) {
        HBox root = new HBox();
        root.getStyleClass().add("login-root");

        StackPane leftPanel = buildLeftPanel();
        VBox rightPanel = buildRightPanel(primaryStage);

        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        root.getChildren().addAll(leftPanel, rightPanel);

        Scene scene = new Scene(root, 1080, 680);
        scene.getStylesheets().addAll(
                stylesheetUri("global-theme.css"),
                stylesheetUri("login-view.css")
        );

        primaryStage.setOnShown(ev -> txtUsername.requestFocus());

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                txtUsername.clear();
                txtPassword.clear();
                clearMessage();
                clearFieldState(txtUsername, usernameBox);
                clearFieldState(txtPassword, passwordBox);
                txtUsername.requestFocus();
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.ENTER) {
                handleLogin(primaryStage);
                e.consume();
            }
        });

        return scene;
    }

    // ── LEFT PANEL ───────────────────────────────────────────────────────────

    private StackPane buildLeftPanel() {
        StackPane panel = new StackPane();
        panel.getStyleClass().add("login-left");
        panel.setMinWidth(380);
        panel.setPrefWidth(480);

        Canvas illustration = createHotelIllustration();
        panel.getChildren().add(illustration);

        panel.widthProperty().addListener((obs, oldW, newW) -> {
            illustration.setWidth(newW.doubleValue());
            drawHotelScene(illustration.getGraphicsContext2D(),
                    newW.doubleValue(), illustration.getHeight());
        });
        panel.heightProperty().addListener((obs, oldH, newH) -> {
            illustration.setHeight(newH.doubleValue());
            drawHotelScene(illustration.getGraphicsContext2D(),
                    illustration.getWidth(), newH.doubleValue());
        });

        return panel;
    }

    private Canvas createHotelIllustration() {
        Canvas canvas = new Canvas(480, 680);
        drawHotelScene(canvas.getGraphicsContext2D(), 480, 680);
        return canvas;
    }

    private void drawHotelScene(GraphicsContext gc, double W, double H) {
        gc.clearRect(0, 0, W, H);

        double cx = W / 2.0;

        // ── Atmospheric glow blobs ──────────────────────────────────────────
        RadialGradient glowTop = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 255, 255, 0.09)),
                new Stop(1, Color.TRANSPARENT));
        gc.setFill(glowTop);
        gc.fillOval(cx - 220, -60, 360, 320);

        RadialGradient glowBot = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 255, 255, 0.07)),
                new Stop(1, Color.TRANSPARENT));
        gc.setFill(glowBot);
        gc.fillOval(cx + 20, H - 280, 300, 300);

        // ── Stars ───────────────────────────────────────────────────────────
        double[][] stars = {
            {0.09, 0.085}, {0.21, 0.042}, {0.34, 0.071}, {0.44, 0.026},
            {0.60, 0.062}, {0.73, 0.100}, {0.88, 0.047}, {0.11, 0.188},
            {0.84, 0.158}, {0.54, 0.115}, {0.28, 0.144}, {0.69, 0.037},
            {0.39, 0.194}, {0.15, 0.287}, {0.91, 0.242}
        };
        for (double[] s : stars) {
            double sx = s[0] * W, sy = s[1] * H;
            double r = ((sx + sy) % 7 < 3) ? 2.2 : 1.5;
            gc.setFill(Color.rgb(255, 255, 255, 0.82));
            gc.fillOval(sx - r, sy - r, r * 2, r * 2);
        }

        // ── Moon crescent ────────────────────────────────────────────────────
        double mx = W * 0.78, my = H * 0.08;
        gc.setFill(Color.rgb(255, 255, 255, 0.92));
        gc.fillOval(mx, my, 56, 56);
        gc.setFill(Color.rgb(22, 43, 77, 1.0)); // matches top of new navy gradient
        gc.fillOval(mx + 13, my - 7, 48, 48);

        // ── Building base coordinates (scaled to panel width) ───────────────
        double bCx   = cx;
        double gndY  = H * 0.775;                     // ground line y
        double tH    = H * 0.545;                     // main tower height
        double tW    = W * 0.245;                     // main tower width
        double tX    = bCx - tW / 2.0;
        double tY    = gndY - tH;

        double wgW   = tW * 0.80;                     // wing width
        double wgH   = tH * 0.72;                     // wing height
        double lWgX  = tX - wgW - 2;
        double rWgX  = tX + tW + 2;
        double wgY   = gndY - wgH;

        double ptW   = tW * 0.60;                     // penthouse width
        double ptH   = tH * 0.07;                     // penthouse height
        double ptX   = bCx - ptW / 2.0;
        double ptY   = tY - ptH;

        // ── Drop shadows ────────────────────────────────────────────────────
        gc.setFill(Color.rgb(0, 0, 0, 0.14));
        gc.fillRoundRect(lWgX + 4, wgY + 4, wgW, wgH, 6, 6);
        gc.fillRoundRect(rWgX + 4, wgY + 4, wgW, wgH, 6, 6);
        gc.fillRoundRect(tX + 5, tY + 5, tW, tH, 6, 6);

        // ── Wings ────────────────────────────────────────────────────────────
        gc.setFill(Color.rgb(255, 255, 255, 0.11));
        gc.setStroke(Color.rgb(255, 255, 255, 0.65));
        gc.setLineWidth(1.8);
        gc.fillRoundRect(lWgX, wgY, wgW, wgH, 6, 6);
        gc.strokeRoundRect(lWgX, wgY, wgW, wgH, 6, 6);
        gc.fillRoundRect(rWgX, wgY, wgW, wgH, 6, 6);
        gc.strokeRoundRect(rWgX, wgY, wgW, wgH, 6, 6);

        // ── Main tower ───────────────────────────────────────────────────────
        gc.setFill(Color.rgb(255, 255, 255, 0.16));
        gc.setStroke(Color.rgb(255, 255, 255, 0.82));
        gc.setLineWidth(2.0);
        gc.fillRoundRect(tX, tY, tW, tH, 6, 6);
        gc.strokeRoundRect(tX, tY, tW, tH, 6, 6);

        // ── Penthouse ────────────────────────────────────────────────────────
        gc.setFill(Color.rgb(255, 255, 255, 0.13));
        gc.setStroke(Color.rgb(255, 255, 255, 0.70));
        gc.setLineWidth(1.8);
        gc.fillRoundRect(ptX, ptY, ptW, ptH, 4, 4);
        gc.strokeRoundRect(ptX, ptY, ptW, ptH, 4, 4);

        // ── Windows — main tower (3 cols × 5 rows) ──────────────────────────
        int[][] towerLit = {{1,0,1},{0,1,1},{1,1,0},{1,0,1},{0,1,0}};
        double winW = tW * 0.19, winH = tH * 0.058;
        double winGapX = (tW - 3 * winW) / 4.0;
        double winGapY = tH * 0.10;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 3; col++) {
                double wx = tX + winGapX + col * (winW + winGapX);
                double wy = tY + winGapY + row * (winH + winGapY * 0.75);
                boolean lit = towerLit[row][col] == 1;
                gc.setFill(lit ? Color.rgb(255, 228, 120, 0.68) : Color.rgb(255, 255, 255, 0.08));
                gc.fillRoundRect(wx, wy, winW, winH, 3, 3);
                gc.setStroke(Color.rgb(255, 255, 255, lit ? 0.55 : 0.30));
                gc.setLineWidth(1.0);
                gc.strokeRoundRect(wx, wy, winW, winH, 3, 3);
            }
        }

        // ── Windows — wing helper lambda ─────────────────────────────────────
        boolean[][] leftLit  = {{true,false},{false,true},{true,true},{false,true}};
        boolean[][] rightLit = {{false,true},{true,false},{true,false},{false,false}};
        drawWingWindows(gc, lWgX, wgY, wgW, wgH, leftLit);
        drawWingWindows(gc, rWgX, wgY, wgW, wgH, rightLit);

        // ── Penthouse window ─────────────────────────────────────────────────
        double pwW = ptW * 0.30, pwH = ptH * 0.60;
        gc.setFill(Color.rgb(255, 228, 120, 0.62));
        gc.fillRoundRect(bCx - pwW / 2, ptY + ptH * 0.20, pwW, pwH, 3, 3);
        gc.setStroke(Color.rgb(255, 255, 255, 0.65));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(bCx - pwW / 2, ptY + ptH * 0.20, pwW, pwH, 3, 3);

        // ── Entrance door (arch) ─────────────────────────────────────────────
        double dW = tW * 0.38, dH = tH * 0.105;
        double dX = bCx - dW / 2, dY = gndY - dH;
        double arcR = dW * 0.50;
        gc.setFill(Color.rgb(255, 255, 255, 0.18));
        gc.fillRoundRect(dX, dY, dW, dH, arcR, arcR);
        gc.setStroke(Color.rgb(255, 255, 255, 0.82));
        gc.setLineWidth(1.6);
        gc.strokeRoundRect(dX, dY, dW, dH, arcR, arcR);
        gc.strokeLine(bCx, dY + dH * 0.28, bCx, gndY);
        gc.setStroke(Color.rgb(255, 255, 255, 0.55));
        gc.setLineWidth(1.0);
        gc.strokeLine(dX, dY + dH * 0.45, dX + dW, dY + dH * 0.45);

        // ── Bushes / trees ───────────────────────────────────────────────────
        gc.setFill(Color.rgb(255, 255, 255, 0.18));
        double bBaseY = gndY - 10;
        // left cluster
        gc.fillOval(lWgX - 20, bBaseY - 26, 46, 28);
        gc.fillOval(lWgX - 10, bBaseY - 38, 36, 28);
        gc.fillOval(lWgX + 10, bBaseY - 28, 28, 24);
        // right cluster
        gc.fillOval(rWgX + wgW - 26, bBaseY - 26, 46, 28);
        gc.fillOval(rWgX + wgW - 22, bBaseY - 38, 36, 28);
        gc.fillOval(rWgX + wgW - 38, bBaseY - 28, 28, 24);

        // ── Ground line ──────────────────────────────────────────────────────
        gc.setStroke(Color.rgb(255, 255, 255, 0.42));
        gc.setLineWidth(2.0);
        gc.strokeLine(W * 0.05, gndY, W * 0.95, gndY);

        // ── Ground shadow strip ──────────────────────────────────────────────
        gc.setFill(Color.rgb(0, 0, 0, 0.12));
        gc.fillRect(W * 0.05, gndY, W * 0.90, 30);

        // ── "HOTEL" text above penthouse ─────────────────────────────────────
        gc.setFill(Color.rgb(255, 255, 255, 0.88));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("HOTEL", bCx, ptY - 6);
    }

    private void drawWingWindows(GraphicsContext gc, double wx0, double wy0,
                                 double wgW, double wgH, boolean[][] lit) {
        double winW = wgW * 0.26, winH = wgH * 0.076;
        double gapX = (wgW - 2 * winW) / 3.0;
        double gapY = wgH * 0.10;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 2; col++) {
                double x = wx0 + gapX + col * (winW + gapX);
                double y = wy0 + gapY + row * (winH + gapY * 0.85);
                gc.setFill(lit[row][col]
                        ? Color.rgb(255, 228, 120, 0.58)
                        : Color.rgb(255, 255, 255, 0.07));
                gc.fillRoundRect(x, y, winW, winH, 2, 2);
                gc.setStroke(Color.rgb(255, 255, 255, 0.38));
                gc.setLineWidth(1.0);
                gc.strokeRoundRect(x, y, winW, winH, 2, 2);
            }
        }
    }

    // ── RIGHT PANEL ──────────────────────────────────────────────────────────

    private VBox buildRightPanel(Stage primaryStage) {
        VBox panel = new VBox();
        panel.getStyleClass().add("login-right");
        panel.setAlignment(Pos.CENTER);
        panel.setPrefWidth(600);

        VBox form = buildLoginForm(primaryStage);
        form.setMaxWidth(360);
        panel.getChildren().add(form);
        return panel;
    }

    private VBox buildLoginForm(Stage primaryStage) {
        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER_LEFT);

        // ── Logo section ─────────────────────────────────────────────────────
        VBox logoSection = new VBox(6);
        logoSection.setAlignment(Pos.CENTER);

        Label logoIcon = new Label("\uD83C\uDFE8");
        logoIcon.getStyleClass().add("login-logo-icon");

        Label logoText = new Label("HOTEL");
        logoText.getStyleClass().add("login-logo-text");

        Label tagline = new Label("H\u1ec7 th\u1ed1ng qu\u1ea3n l\u00fd kh\u00e1ch s\u1ea1n");
        tagline.getStyleClass().add("login-tagline");

        logoSection.getChildren().addAll(logoIcon, logoText, tagline);

        // ── Divider ───────────────────────────────────────────────────────────
        Region divider = new Region();
        divider.getStyleClass().add("login-divider");
        divider.setMaxWidth(Double.MAX_VALUE);

        // ── Username field ────────────────────────────────────────────────────
        usernameBox.getChildren().addAll(
                createFieldLabel("T\u00ean \u0111\u0103ng nh\u1eadp"),
                createInputRow("\uD83D\uDC64", txtUsername, "Nh\u1eadp t\u00ean \u0111\u0103ng nh\u1eadp")
        );

        // ── Password field ────────────────────────────────────────────────────
        passwordBox.getChildren().addAll(
                createFieldLabel("M\u1eadt kh\u1ea9u"),
                createInputRow("\uD83D\uDD12", txtPassword, "Nh\u1eadp m\u1eadt kh\u1ea9u")
        );

        VBox fieldsGroup = new VBox(14, usernameBox, passwordBox);

        txtUsername.setOnAction(e -> txtPassword.requestFocus());
        txtPassword.setOnAction(e -> handleLogin(primaryStage));

        setupFieldValidation(txtUsername, usernameBox);
        setupFieldValidation(txtPassword, passwordBox);

        // ── Message label ─────────────────────────────────────────────────────
        messageLabel.setManaged(false);
        messageLabel.setVisible(false);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        messageLabel.getStyleClass().addAll("login-message", "login-message-info");

        // ── Login button ──────────────────────────────────────────────────────
        btnLogin.getStyleClass().add("login-submit");
        btnLogin.setGraphic(loginButtonIcon);
        btnLogin.setGraphicTextGap(10);
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(48);
        btnLogin.setOnAction(e -> handleLogin(primaryStage));

        // ── Hint ──────────────────────────────────────────────────────────────
        Label hint = new Label("Enter chuy\u1ec3n \u00f4  \u00b7  Ctrl+Enter \u0111\u0103ng nh\u1eadp  \u00b7  Esc x\u00f3a");
        hint.getStyleClass().add("login-footer-note");
        hint.setMaxWidth(Double.MAX_VALUE);

        form.getChildren().addAll(logoSection, divider, fieldsGroup, messageLabel, btnLogin, hint);
        return form;
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("login-form-label");
        return label;
    }

    private HBox createInputRow(String icon, TextInputControl control, String prompt) {
        HBox row = new HBox(10);
        row.getStyleClass().add("login-field-wrap");
        row.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("login-field-icon");

        control.setPromptText(prompt);
        HBox.setHgrow(control, Priority.ALWAYS);

        row.getChildren().addAll(iconLabel, control);
        return row;
    }

    private void setupFieldValidation(TextInputControl control, VBox box) {
        control.textProperty().addListener((obs, o, n) -> clearFieldState(control, box));
        control.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused && !control.getText().trim().isEmpty()) {
                clearFieldState(control, box);
            }
        });
    }

    // ── Login logic (unchanged) ───────────────────────────────────────────────

    private void handleLogin(Stage primaryStage) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        clearMessage();
        clearFieldState(txtUsername, usernameBox);
        clearFieldState(txtPassword, passwordBox);

        if (username.isEmpty() || password.isEmpty()) {
            if (username.isEmpty()) markFieldError(txtUsername, usernameBox);
            if (password.isEmpty()) markFieldError(txtPassword, passwordBox);
            showMessage("Vui l\u00f2ng nh\u1eadp \u0111\u1ea7y \u0111\u1ee7 t\u00ean \u0111\u0103ng nh\u1eadp v\u00e0 m\u1eadt kh\u1ea9u.", true);
            if (username.isEmpty()) txtUsername.requestFocus();
            else txtPassword.requestFocus();
            return;
        }

        setLoadingState(true);
        try {
            ConnectDB.getInstance().connect();

            NhanVienDAO nvDao = new NhanVienDAO();
            NhanVien nv = nvDao.getNhanVienByUsername(username);

            if (nv == null) {
                markFieldError(txtUsername, usernameBox);
                showMessage("T\u00e0i kho\u1ea3n kh\u00f4ng t\u1ed3n t\u1ea1i trong h\u1ec7 th\u1ed1ng.", true);
                txtUsername.requestFocus();
                return;
            }

            String dbPass = nv.getMatKhau();
            boolean passwordOk = dbPass != null && dbPass.equals(password);
            if (!passwordOk) {
                markFieldError(txtPassword, passwordBox);
                showMessage("M\u1eadt kh\u1ea9u kh\u00f4ng ch\u00ednh x\u00e1c.", true);
                txtPassword.requestFocus();
                txtPassword.selectAll();
                return;
            }

            showMessage("\u0110\u0103ng nh\u1eadp th\u00e0nh c\u00f4ng. \u0110ang m\u1edf giao di\u1ec7n qu\u1ea3n tr\u1ecb...", false);
            final NhanVien loggedIn = nv;
            SwingUtilities.invokeLater(() -> new MainFrame(loggedIn).setVisible(true));
            primaryStage.close();
        } catch (SQLException ex) {
            showMessage("Kh\u00f4ng th\u1ec3 k\u1ebft n\u1ed1i \u0111\u1ebfn database. Ki\u1ec3m tra l\u1ea1i SQL Server v\u00e0 t\u00e0i kho\u1ea3n \u0111ang d\u00f9ng.", true);
            ex.printStackTrace();
        } finally {
            setLoadingState(false);
        }
    }

    private void setLoadingState(boolean loading) {
        btnLogin.setDisable(loading);
        btnLogin.setText(loading ? "\u0110ang x\u00e1c th\u1ef1c..." : "\u0110\u0103ng nh\u1eadp");
        loginButtonIcon.setText(loading ? "\u23F3" : "\u2192");
    }

    private void showMessage(String message, boolean error) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("login-message-error", "login-message-info");
        messageLabel.getStyleClass().add(error ? "login-message-error" : "login-message-info");
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
    }

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    private void markFieldError(TextInputControl control, VBox box) {
        if (box.getChildren().size() > 1 && box.getChildren().get(1) instanceof HBox row) {
            if (!row.getStyleClass().contains("login-field-wrap-error")) {
                row.getStyleClass().add("login-field-wrap-error");
            }
        }
        control.requestFocus();
    }

    private void clearFieldState(TextInputControl control, VBox box) {
        if (box.getChildren().size() > 1 && box.getChildren().get(1) instanceof HBox row) {
            row.getStyleClass().remove("login-field-wrap-error");
        }
    }

    private String stylesheetUri(String fileName) {
        return Paths.get("src", "gui", fileName).toAbsolutePath().toUri().toString();
    }
}
