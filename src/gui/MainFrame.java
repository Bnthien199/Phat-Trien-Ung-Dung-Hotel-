package gui;

import entity.NhanVien;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainFrame extends JFrame {

    // ═══════════════════════════════════════════════════════════════
    // THEME COLORS — Teal / Emerald (đồng bộ với LoginFrame)
    // ═══════════════════════════════════════════════════════════════
    private static final String PRIMARY_COLOR = "#0f172a";
    private static final String SIDEBAR_TOP = "#162b4d";
    private static final String SIDEBAR_HOVER = "#1d4ed8";
    private static final String ACTIVE_COLOR = "#14f1d9";
    private static final String ACTIVE_BG = "#073b4c";
    private static final String BG_LIGHT = "#f6f8fb";
    private static final String GROUP_HEADER_BG = "rgba(255,255,255,0.06)";
    private static final String TEXT_DIM = "#9ca3af";
    private static final String TEXT_NORMAL = "#e5e7eb";

    private static final String FONT_STYLE = "-fx-font-size: 13.5px; -fx-font-family: 'Segoe UI'; ";
    private static final String COMMON_STYLE = "-fx-cursor: hand; -fx-background-radius: 8; -fx-alignment: center-left; ";

    private final JFXPanel jfxPanel = new JFXPanel();
    private final List<Button> menuButtons = new ArrayList<>();
    private final Map<String, StackPane> cardMap = new HashMap<>();
    private final Set<String> initializedCards = new HashSet<>();
    private final NhanVien loggedInNhanVien;

    private StackPane cardsContainer;

    private DashboardView dashboardView;
    private RoomView roomView;
    private RoomManagementView roomManagementView;
    private BookingView bookingView;
    private CheckoutView checkoutView;
    private CustomerView customerView;
    private BookingListView bookingListView;
    private CheckInView checkInView;
    private ServiceView serviceView;
    private PricingView pricingView;
    private InvoiceView invoiceView;
    private RevenueStatsView revenueStatsView;
    private RoomStatsView roomStatsView;
    private CustomerStatsView customerStatsView;
    private AccountView accountView;
    private ChangePasswordView changePasswordView;

    private Label lblClock;
    private Timeline clockTimeline;

    public MainFrame(NhanVien loggedInNhanVien) {
        this.loggedInNhanVien = loggedInNhanVien;
        setTitle("Hệ thống Quản Lý Khách Sạn — Nhóm 06");
        setSize(1400, 870);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(jfxPanel, BorderLayout.CENTER);

        Platform.runLater(() -> {
            try {
                javafx.scene.Parent root = createFxUI();
                Scene scene = new Scene(root, 1400, 870);
                scene.getStylesheets()
                        .add(Paths.get("src", "gui", "global-theme.css").toAbsolutePath().toUri().toString());
                jfxPanel.setScene(scene);
                startClockFx();
            } catch (Throwable t) {
                t.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(
                        this,
                        "Lỗi khởi tạo giao diện: " + t.getMessage(),
                        "Lỗi Hệ Thống",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // ─── Kiểm tra quyền Admin ────────────────────────────────────────────────

    private boolean isAdmin() {
        if (loggedInNhanVien == null)
            return false;
        String role = loggedInNhanVien.getVaiTro();
        if (role == null)
            return false;
        String r = role.trim().toLowerCase();
        return r.equals("admin") || r.equals("quản lý") || r.equals("quan ly") || r.equals("manager");
    }

    private javafx.scene.Parent createFxUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_LIGHT + ";");

        String role = loggedInNhanVien != null ? loggedInNhanVien.getVaiTro() : "User";
        String name = loggedInNhanVien != null ? loggedInNhanVien.getTenNhanVien() : "Nhân viên";

        // ═══════════════════════════════════════
        // SIDEBAR
        // ═══════════════════════════════════════
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(260);
        sidebar.setStyle("-fx-background-color: #111827;");
        sidebar.getStyleClass().add("sidebar");

        // ── Top: Logo + User info ──
        VBox top = new VBox(10);
        top.setPadding(new Insets(24, 20, 20, 20));
        top.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03);"
                + "-fx-background-radius: 0 0 24 24;"
                + "-fx-border-color: rgba(255, 255, 255, 0.06);"
                + "-fx-border-width: 0 0 1 0;");

        Label lblLogo = new Label("🏨  HOTEL SYSTEM");
        lblLogo.setTextFill(javafx.scene.paint.Color.WHITE);
        lblLogo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        Label lblRole = new Label("👤  " + name + "  ·  " + role);
        lblRole.setTextFill(javafx.scene.paint.Color.rgb(229, 231, 235));
        lblRole.setFont(Font.font("Segoe UI", 12));

        lblClock = new Label();
        lblClock.setTextFill(javafx.scene.paint.Color.WHITE);
        lblClock.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblClock.setStyle(
                "-fx-background-color: rgba(255,255,255,0.12); -fx-padding: 6 12; -fx-background-radius: 999;");

        top.getChildren().addAll(lblLogo, lblRole, lblClock);

        // ── Menu ──
        VBox menu = new VBox(2);
        menu.setPadding(new Insets(18, 12, 16, 12));

        menu.getChildren().add(createMenuButton("Trang chủ", "Dashboard"));
        menu.getChildren().add(createGroupSection("QUẢN LÝ ĐẶT PHÒNG", "🛏", true,
                new String[] { "Đặt phòng", "Booking" },
                new String[] { "Danh sách đặt phòng", "BookingList" },
                new String[] { "Nhận phòng", "CheckIn" },
                new String[] { "Trả phòng", "Checkout" }));
        menu.getChildren().add(createGroupSection("QUẢN LÝ KHÁCH SẠN", "🏢", true,
                new String[] { "Phòng", "Room" },
                new String[] { "Loại phòng", "RoomMgmt" },
                new String[] { "Dịch vụ", "Service" },
                new String[] { "Bảng giá", "Pricing" }));
        menu.getChildren().add(createGroupSection("KHÁCH HÀNG", "👥", true,
                new String[] { "Khách hàng", "Customer" }));
        menu.getChildren().add(createGroupSection("HÓA ĐƠN", "🧾", true,
                new String[] { "Hóa đơn", "Invoice" }));

        // ── BÁO CÁO — chỉ hiển thị cho Admin ──
        if (isAdmin()) {
            menu.getChildren().add(createGroupSection("BÁO CÁO", "📈", true,
                    new String[] { "Thống kê doanh thu", "RevenueStats" },
                    new String[] { "Thống kê phòng", "RoomStats" },
                    new String[] { "Thống kê khách hàng", "CustomerStats" }));
        }

        // ── HỆ THỐNG — Admin xem đầy đủ, Lễ tân chỉ xem Đổi mật khẩu ──
        if (isAdmin()) {
            menu.getChildren().add(createGroupSection("HỆ THỐNG", "⚙", true,
                    new String[] { "Tài khoản", "Account" },
                    new String[] { "Đổi mật khẩu", "ChangePassword" }));
        } else {
            menu.getChildren().add(createGroupSection("HỆ THỐNG", "⚙", true,
                    new String[] { "Đổi mật khẩu", "ChangePassword" }));
        }

        ScrollPane menuScroll = new ScrollPane(menu);
        menuScroll.setFitToWidth(true);
        menuScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        menuScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(menuScroll, Priority.ALWAYS);

        // ── Bottom: Logout ──
        VBox bottom = new VBox(16);
        bottom.setPadding(new Insets(12, 14, 18, 14));
        bottom.setAlignment(Pos.CENTER);

        Button btnLogout = new Button("Đăng xuất");
        btnLogout.getStyleClass().add("logout-button");
        btnLogout.setPrefSize(232, 42);
        btnLogout.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        
        Label logoutIcon = new Label("🚪");
        logoutIcon.setPrefWidth(24);
        logoutIcon.setAlignment(Pos.CENTER);
        logoutIcon.setFont(Font.font("Segoe UI Emoji", 14));
        btnLogout.setGraphic(logoutIcon);
        btnLogout.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        
        String logoutNormal = "-fx-background-color: rgba(239,68,68,0.22); -fx-text-fill: #ffffff;"
                + "-fx-cursor: hand; -fx-background-radius: 12; -fx-alignment: center;";
        String logoutHover = "-fx-background-color: #ef4444; -fx-text-fill: white;"
                + "-fx-cursor: hand; -fx-background-radius: 12; -fx-alignment: center;";
        btnLogout.setStyle(logoutNormal);
        btnLogout.setOnMouseEntered(e -> btnLogout.setStyle(logoutHover));
        btnLogout.setOnMouseExited(e -> btnLogout.setStyle(logoutNormal));
        btnLogout.setOnAction(e -> handleLogout());
        bottom.getChildren().add(btnLogout);

        sidebar.getChildren().addAll(top, menuScroll, bottom);

        // ═══════════════════════════════════════
        // CONTENT AREA
        // ═══════════════════════════════════════
        cardsContainer = new StackPane();
        cardsContainer.setStyle("-fx-background-color: " + BG_LIGHT + ";");
        initializeViews();

        root.setLeft(sidebar);
        root.setCenter(cardsContainer);
        showCard("Dashboard");
        return root;
    }

    // ─── Group Section with emoji icon ───────────────────────────────────────

    private VBox createGroupSection(String groupTitle, String emoji, boolean ignored, String[]... items) {
        VBox section = new VBox(0);

        // Sub items — mặc định thu gọn
        VBox subItems = new VBox(1);
        subItems.setPadding(new Insets(4, 0, 8, 14));
        subItems.setVisible(false);
        subItems.setManaged(false);

        for (String[] item : items) {
            Button btn = createMenuButton(item[0], item[1]);
            subItems.getChildren().add(btn);

            // Tự động thu gọn khi menu con mất trạng thái active và chuột nằm ngoài nhóm
            btn.getProperties().addListener((javafx.collections.MapChangeListener<Object, Object>) change -> {
                if ("state".equals(change.getKey()) && !btn.getProperties().containsKey("state")) {
                    if (!section.isHover()) {
                        subItems.setVisible(false);
                        subItems.setManaged(false);
                    }
                }
            });
        }

        // Header label
        Label header = new Label();
        header.setText(groupTitle);
        header.setPrefWidth(240);
        header.setMinHeight(38);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        header.setPadding(new Insets(0, 0, 0, 10));

        Label iconLabel = new Label(emoji);
        iconLabel.setPrefWidth(24);
        iconLabel.setAlignment(Pos.CENTER_LEFT);
        iconLabel.setFont(Font.font("Segoe UI Emoji", 13));
        iconLabel.setTextFill(Color.web("#2dd4bf")); // Soft teal color for main group icons!
        
        header.setGraphic(iconLabel);
        header.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);

        String headerNormal = "-fx-background-color: " + GROUP_HEADER_BG + "; "
                + "-fx-text-fill: " + TEXT_NORMAL + ";"
                + "-fx-background-radius: 10;";
        header.setStyle(headerNormal);

        section.getChildren().addAll(header, subItems);

        // Hover vào section thì tự sổ xuống
        section.setOnMouseEntered(e -> {
            subItems.setVisible(true);
            subItems.setManaged(true);
        });

        // Rời chuột khỏi section thì tự thu lại nếu không có con nào active
        section.setOnMouseExited(e -> {
            boolean hasActive = false;
            for (javafx.scene.Node node : subItems.getChildren()) {
                if (node instanceof Button btn && ACTIVE_COLOR.equals(btn.getProperties().get("state"))) {
                    hasActive = true;
                    break;
                }
            }

            if (!hasActive) {
                subItems.setVisible(false);
                subItems.setManaged(false);
            }
        });

        return section;
    }

    // ─── Views & Cards ───────────────────────────────────────────────────────

    private void initializeViews() {
        ensureCardInitialized("Dashboard");
    }

    private void ensureCardInitialized(String cardName) {
        if (initializedCards.contains(cardName))
            return;

        StackPane pane;
        try {
            pane = new StackPane(createViewNode(cardName));
        } catch (Throwable t) {
            t.printStackTrace();
            pane = createPlaceholderCard("⚠ " + cardName, "Khởi tạo view thất bại.");
        }

        pane.setVisible(false);
        pane.setManaged(false);
        cardsContainer.getChildren().add(pane);
        cardMap.put(cardName, pane);
        initializedCards.add(cardName);
    }

    private javafx.scene.Node createViewNode(String cardName) {
        return switch (cardName) {
            case "Dashboard" -> {
                if (dashboardView == null) {
                    dashboardView = new DashboardView();
                    dashboardView.setNavigator(this::showCard);
                }
                yield dashboardView.createView();
            }
            case "Room" -> {
                if (roomView == null) {
                    roomView = new RoomView(loggedInNhanVien);
                    roomView.setOnBookingRequest(room -> {
                        showCard("Booking");
                        if (bookingView != null)
                            bookingView.preSelectRoom(room);
                    });
                    roomView.setOnCheckoutRequest(room -> {
                        showCard("Checkout");
                        if (checkoutView != null) {
                            checkoutView.selectRoom(room.getMaPhong());
                        }
                    });
                }
                yield roomView.createView();
            }
            case "RoomMgmt" -> {
                if (roomManagementView == null)
                    roomManagementView = new RoomManagementView();
                yield roomManagementView.createView();
            }
            case "Booking" -> {
                if (bookingView == null) {
                    bookingView = new BookingView(loggedInNhanVien);
                    bookingView.setOnBookingSaved(() -> {
                        refreshAfterBooking();
                        showCard("BookingList");
                    });
                }
                yield bookingView.createView();
            }
            case "Checkout" -> {
                if (checkoutView == null) {
                    checkoutView = new CheckoutView(loggedInNhanVien);
                    checkoutView.setOnCheckoutSaved(this::refreshAfterCheckout);
                }
                yield checkoutView.createView();
            }
            case "Customer" -> {
                if (customerView == null) {
                    customerView = new CustomerView();
                    // Xử lý sự kiện bấm nút "Đặt phòng" trong màn hình Khách hàng
                    customerView.setOnBookingRequest(khachHang -> {
                        showCard("Booking"); // Mở panel Lập phiếu đặt phòng
                        if (bookingView != null) {
                            bookingView.preFillCustomer(khachHang); // Truyền dữ liệu
                        }
                    });
                }
                yield customerView.createView();
            }
            case "BookingList" -> {
                if (bookingListView == null) {
                    bookingListView = new BookingListView(loggedInNhanVien);
                    bookingListView.setOnBookingCancelled(this::refreshAfterBookingCancelled);
                }
                yield bookingListView.createView();
            }
            case "CheckIn" -> {
                if (checkInView == null) {
                    checkInView = new CheckInView();
                    checkInView.setOnCheckInSaved(this::refreshAfterCheckIn);
                }
                yield checkInView.createView();
            }
            case "Service" -> {
                if (serviceView == null)
                    serviceView = new ServiceView();
                yield serviceView.createView();
            }
            case "Pricing" -> {
                if (pricingView == null)
                    pricingView = new PricingView();
                yield pricingView.createView();
            }
            case "Invoice" -> {
                if (invoiceView == null)
                    invoiceView = new InvoiceView();
                yield invoiceView.createView();
            }
            case "RevenueStats" -> {
                if (revenueStatsView == null)
                    revenueStatsView = new RevenueStatsView();
                yield revenueStatsView.createView();
            }
            case "RoomStats" -> {
                if (roomStatsView == null)
                    roomStatsView = new RoomStatsView();
                yield roomStatsView.createView();
            }
            case "CustomerStats" -> {
                if (customerStatsView == null)
                    customerStatsView = new CustomerStatsView();
                yield customerStatsView.createView();
            }
            case "Account" -> {
                if (accountView == null)
                    accountView = new AccountView();
                yield accountView.createView();
            }
            case "ChangePassword" -> {
                if (changePasswordView == null)
                    changePasswordView = new ChangePasswordView(loggedInNhanVien);
                yield changePasswordView.createView();
            }
            default -> createPlaceholderCard("Không hỗ trợ", "Không tìm thấy view tương ứng.");
        };
    }

    private void showCard(String cardName) {
        ensureCardInitialized(cardName);

        cardMap.forEach((name, pane) -> {
            boolean active = name.equals(cardName);
            pane.setVisible(active);
            pane.setManaged(active);
        });

        try {
            switch (cardName) {
                case "Dashboard" -> {
                    if (dashboardView != null)
                        dashboardView.refreshData();
                }
                case "Room" -> {
                    if (roomView != null)
                        roomView.loadData();
                }
                case "RoomMgmt" -> {
                    if (roomManagementView != null)
                        roomManagementView.loadData();
                }
                case "Booking" -> {
                    if (bookingView != null) {
                        bookingView.loadPhongTrong();
                        bookingView.refreshBangGiaApDung();
                    }
                }
                case "BookingList" -> {
                    if (bookingListView != null) {
                        bookingListView.loadData();
                    }
                }
                case "CheckIn" -> {
                    if (checkInView != null)
                        checkInView.loadData();
                }
                case "Checkout" -> {
                    if (checkoutView != null)
                        checkoutView.loadData();
                }
                case "Customer" -> {
                    if (customerView != null)
                        customerView.loadData();
                }
                case "Service" -> {
                    if (serviceView != null)
                        serviceView.loadData();
                }
                case "Pricing" -> {
                    if (pricingView != null)
                        pricingView.loadData();
                }
                case "Invoice" -> {
                    if (invoiceView != null)
                        invoiceView.loadData();
                }
                case "RevenueStats" -> {
                    if (revenueStatsView != null)
                        revenueStatsView.loadData();
                }
                case "RoomStats" -> {
                    if (roomStatsView != null)
                        roomStatsView.loadData();
                }
                case "CustomerStats" -> {
                    if (customerStatsView != null)
                        customerStatsView.loadData();
                }
                case "Account" -> {
                    if (accountView != null)
                        accountView.loadData();
                }
                default -> {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Button btn : menuButtons) {
            if (cardName.equals(btn.getUserData())) {
                setButtonActive(btn);
            }
        }
    }

    private void refreshAfterBooking() {
        try {
            if (dashboardView != null)
                dashboardView.refreshData();
            if (roomView != null)
                roomView.loadData();
            if (roomManagementView != null)
                roomManagementView.loadData();
            if (bookingListView != null)
                bookingListView.loadData();
            if (checkInView != null)
                checkInView.loadData();
            if (checkoutView != null)
                checkoutView.loadData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshAfterCheckIn() {
        try {
            if (dashboardView != null)
                dashboardView.refreshData();
            if (roomView != null)
                roomView.loadData();
            if (roomManagementView != null)
                roomManagementView.loadData();
            if (bookingListView != null)
                bookingListView.loadData();
            if (checkInView != null)
                checkInView.loadData();
            if (checkoutView != null)
                checkoutView.loadData();
            if (roomStatsView != null)
                roomStatsView.loadData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshAfterBookingCancelled() {
        try {
            if (dashboardView != null)
                dashboardView.refreshData();
            if (roomView != null)
                roomView.loadData();
            if (roomManagementView != null)
                roomManagementView.loadData();
            if (bookingView != null) {
                bookingView.loadPhongTrong();
                bookingView.refreshBangGiaApDung();
            }
            if (bookingListView != null)
                bookingListView.loadData();
            if (checkInView != null)
                checkInView.loadData();
            if (checkoutView != null)
                checkoutView.loadData();
            if (roomStatsView != null)
                roomStatsView.loadData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshAfterCheckout() {
        try {
            if (dashboardView != null)
                dashboardView.refreshData();
            if (roomView != null)
                roomView.loadData();
            if (roomManagementView != null)
                roomManagementView.loadData();
            if (bookingListView != null)
                bookingListView.loadData();
            if (checkoutView != null)
                checkoutView.loadData();
            if (invoiceView != null)
                invoiceView.loadData();
            if (roomStatsView != null)
                roomStatsView.loadData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── Menu Buttons ────────────────────────────────────────────────────────

    private Button createMenuButton(String text, String cardName) {
        Button btn = new Button();
        btn.getStyleClass().add("sidebar-menu-item");
        btn.setPrefWidth(220);
        btn.setMinHeight(36);
        btn.setUserData(cardName);

        String cleanText = text.trim();
        if (cleanText.equals("Trang chủ")) {
            Label iconLabel = new Label("🏠");
            iconLabel.setPrefWidth(24);
            iconLabel.setAlignment(Pos.CENTER_LEFT);
            iconLabel.setFont(Font.font("Segoe UI Emoji", 13.5));
            iconLabel.setTextFill(Color.web("rgba(255,255,255,0.7)"));
            btn.setGraphic(iconLabel);
            btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            btn.setText("Trang chủ");
        } else {
            Label dotLabel = new Label("•");
            dotLabel.setPrefWidth(24);
            dotLabel.setAlignment(Pos.CENTER);
            dotLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            dotLabel.setTextFill(Color.web("rgba(255,255,255,0.3)"));
            btn.setGraphic(dotLabel);
            btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            btn.setText(cleanText);
        }

        String normalStyle = FONT_STYLE + COMMON_STYLE
                + "-fx-background-color: transparent; -fx-text-fill: " + TEXT_NORMAL
                + "; -fx-font-weight: normal; -fx-padding: 0 14 0 14;";
        btn.setStyle(normalStyle);

        btn.setOnMouseEntered(e -> {
            if (!ACTIVE_COLOR.equals(btn.getProperties().get("state"))) {
                btn.setStyle(FONT_STYLE + COMMON_STYLE
                        + "-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-font-weight: normal; -fx-padding: 0 14 0 14;");
                if (btn.getGraphic() instanceof Label lbl) {
                    if (btn.getText().equals("Trang chủ")) {
                        lbl.setTextFill(Color.web("white"));
                    } else {
                        lbl.setTextFill(Color.web("rgba(255,255,255,0.6)"));
                    }
                }
            }
        });
        btn.setOnMouseExited(e -> {
            if (!ACTIVE_COLOR.equals(btn.getProperties().get("state"))) {
                btn.setStyle(normalStyle);
                if (btn.getGraphic() instanceof Label lbl) {
                    if (btn.getText().equals("Trang chủ")) {
                        lbl.setTextFill(Color.web("rgba(255,255,255,0.7)"));
                    } else {
                        lbl.setTextFill(Color.web("rgba(255,255,255,0.3)"));
                    }
                }
            }
        });
        btn.setOnAction(e -> {
            setButtonActive(btn);
            showCard(cardName);
        });

        menuButtons.add(btn);
        return btn;
    }

    private void setButtonActive(Button selectedBtn) {
        for (Button btn : menuButtons) {
            if (btn == selectedBtn) {
                btn.setStyle(FONT_STYLE + COMMON_STYLE
                        + "-fx-background-color: " + ACTIVE_BG + "; -fx-text-fill: " + ACTIVE_COLOR + "; -fx-font-weight: bold;"
                        + "-fx-border-color: " + ACTIVE_COLOR
                        + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 0 8 8 0; -fx-padding: 0 14 0 14;");
                btn.getProperties().put("state", ACTIVE_COLOR);
                if (btn.getGraphic() instanceof Label lbl) {
                    lbl.setTextFill(Color.web(ACTIVE_COLOR));
                }

                // Mở nhóm chứa button đang active
                javafx.scene.Parent parent = btn.getParent();
                if (parent instanceof VBox parentBox) {
                    // subItems VBox nằm trong section VBox
                    parentBox.setVisible(true);
                    parentBox.setManaged(true);
                }
            } else {
                btn.setStyle(FONT_STYLE + COMMON_STYLE
                        + "-fx-background-color: transparent; -fx-text-fill: " + TEXT_NORMAL
                        + "; -fx-font-weight: normal; -fx-padding: 0 14 0 14;");
                btn.getProperties().remove("state");
                if (btn.getGraphic() instanceof Label lbl) {
                    if (btn.getText().equals("Trang chủ")) {
                        lbl.setTextFill(Color.web("rgba(255,255,255,0.7)"));
                    } else {
                        lbl.setTextFill(Color.web("rgba(255,255,255,0.3)"));
                    }
                }
            }
        }
    }

    // ─── Logout ──────────────────────────────────────────────────────────────

    private void handleLogout() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle("");

        // ── Overlay backdrop ──────────────────────────────────────────────
        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane();
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.52);");

        // ── Dialog card ───────────────────────────────────────────────────
        VBox card = new VBox(20);
        card.setMaxWidth(380);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 36 32 28 32;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.22), 32, 0, 0, 8);");

        // Icon + Title (no border on icon)
        Label iconLbl = new Label("\uD83D\uDEAA");
        iconLbl.setFont(Font.font("Segoe UI Emoji", 38));

        Label titleLbl = new Label("Đăng xuất");
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 21));
        titleLbl.setTextFill(Color.web("#0f172a"));

        VBox titleBox = new VBox(6, iconLbl, titleLbl);
        titleBox.setAlignment(Pos.CENTER);

        // Message
        Label msgLbl = new Label("Bạn có chắc chắn muốn đăng xuất\nkhỏi hệ thống không?");
        msgLbl.setTextFill(Color.web("#64748b"));
        msgLbl.setFont(Font.font("Segoe UI", 14));
        msgLbl.setTextAlignment(TextAlignment.CENTER);
        msgLbl.setStyle("-fx-alignment: center;");

        // Separator — xám rất nhạt, không viền đen
        Region sep = new Region();
        sep.setStyle("-fx-background-color: #f1f5f9;");
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);

        // Buttons — nằm ngang, không viền đen
        Button btnCancel = new Button("Thoát");
        btnCancel.setPrefHeight(44);
        btnCancel.setPrefWidth(140);
        btnCancel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        String nBase = "-fx-background-color:#f1f5f9;-fx-text-fill:#374151;-fx-background-radius:12;-fx-cursor:hand;";
        String nHover = "-fx-background-color:#e2e8f0;-fx-text-fill:#0f172a;-fx-background-radius:12;-fx-cursor:hand;";
        btnCancel.setStyle(nBase);
        btnCancel.setOnMouseEntered(e -> btnCancel.setStyle(nHover));
        btnCancel.setOnMouseExited(e -> btnCancel.setStyle(nBase));

        Button btnConfirm = new Button("\uD83D\uDEAA  Đăng xuất");
        btnConfirm.setPrefHeight(44);
        btnConfirm.setPrefWidth(140);
        btnConfirm.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        String cBase = "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-background-radius:12;-fx-cursor:hand;";
        String cHover = "-fx-background-color:#b91c1c;-fx-text-fill:white;-fx-background-radius:12;-fx-cursor:hand;";
        btnConfirm.setStyle(cBase);
        btnConfirm.setOnMouseEntered(e -> btnConfirm.setStyle(cHover));
        btnConfirm.setOnMouseExited(e -> btnConfirm.setStyle(cBase));

        // HBox — 2 nút nằm ngang, canh giữa, cách nhau 12px
        javafx.scene.layout.HBox btnGroup = new javafx.scene.layout.HBox(12, btnCancel, btnConfirm);
        btnGroup.setAlignment(Pos.CENTER);

        card.getChildren().addAll(titleBox, msgLbl, sep, btnGroup);
        javafx.scene.layout.StackPane.setAlignment(card, Pos.CENTER);
        overlay.getChildren().add(card);

        // ── Actions ───────────────────────────────────────────────────────
        btnConfirm.setOnAction(e -> {
            dialog.close();
            Platform.runLater(() -> {
                Stage stage = new Stage();
                LoginFrame login = new LoginFrame();
                Scene scene = login.createScene(stage);
                stage.setScene(scene);
                stage.show();
                SwingUtilities.invokeLater(this::dispose);
            });
        });
        btnCancel.setOnAction(e -> dialog.close());

        Scene dialogScene = new Scene(overlay, 480, 340);
        dialogScene.setFill(Color.TRANSPARENT);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    // ─── Placeholder ─────────────────────────────────────────────────────────

    private StackPane createPlaceholderCard(String title, String subtitle) {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        Label lbl = new Label(title);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lbl.setTextFill(javafx.scene.paint.Color.web("#94a3b8"));
        Label lbl2 = new Label(subtitle);
        lbl2.setFont(Font.font("Segoe UI", 16));
        lbl2.setTextFill(javafx.scene.paint.Color.web("#cbd5e1"));
        box.getChildren().addAll(lbl, lbl2);
        StackPane sp = new StackPane(box);
        sp.setStyle("-fx-background-color: " + BG_LIGHT + ";");
        return sp;
    }

    // ─── Clock ───────────────────────────────────────────────────────────────

    private void startClockFx() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss  dd/MM/yyyy");
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (lblClock != null) {
                lblClock.setText("🕐  " + sdf.format(new Date()));
            }
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }
}
