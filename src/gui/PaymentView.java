package gui;

import dao.ChiTietPhieuDatDAO;
import dao.HoaDonDAO;
import dao.PhieuDatPhongDAO;
import entity.ChiTietPhieuDat;
import entity.HoaDon;
import entity.PhieuDatPhong;
import entity.NhanVien;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import util.AppTheme;
import util.BookingStatus;
import util.PricingService;
import util.StatusUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentView {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObservableList<PhieuDatPhong> bookingList = FXCollections.observableArrayList();
    private final PhieuDatPhongDAO phieuDatPhongDAO = new PhieuDatPhongDAO();
    private final ChiTietPhieuDatDAO chiTietPhieuDatDAO = new ChiTietPhieuDatDAO();
    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private final PricingService pricingService = new PricingService();

    private final TableView<PhieuDatPhong> tableBooking = new TableView<>();
    private final List<PhieuDatPhong> allData = new ArrayList<>();
    private TextField txtSearch;
    private TextArea txtSummary;
    private Button btnPay;
    private HoaDon currentHoaDon;
    private double currentRoomTotal;
    private double currentServiceTotal;
    private List<ChiTietPhieuDat> currentCheckedInRooms = new ArrayList<>();

    public Node createView() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + AppTheme.BG_LIGHT + ";");

        Label title = new Label("THANH TOÁN");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        title.setTextFill(Color.web(AppTheme.TEXT));

        txtSearch = new TextField();
        txtSearch.setPromptText("Tìm theo mã phiếu hoặc khách hàng...");
        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> filterData());

        configureTable();
        tableBooking.setItems(bookingList);

        txtSummary = new TextArea();
        txtSummary.setEditable(false);
        txtSummary.setPrefRowCount(14);

        Button btnCalc = new Button("Tính tiền");
        btnCalc.setTextFill(Color.WHITE);
        btnCalc.setStyle("-fx-background-color: " + AppTheme.INFO + "; -fx-background-radius: 8;");
        btnCalc.setOnAction(e -> handleCalculate());

        btnPay = new Button("Xác nhận thanh toán");
        btnPay.setTextFill(Color.WHITE);
        btnPay.setStyle("-fx-background-color: " + AppTheme.PRIMARY + "; -fx-background-radius: 8;");
        btnPay.setDisable(true);
        btnPay.setOnAction(e -> handlePayment());

        HBox actionBar = new HBox(10, btnCalc, btnPay);
        HBox content = new HBox(18, new VBox(10, txtSearch, tableBooking), new VBox(10, txtSummary, actionBar));
        HBox.setHgrow(tableBooking, Priority.ALWAYS);
        HBox.setHgrow(txtSummary, Priority.ALWAYS);
        VBox.setVgrow(tableBooking, Priority.ALWAYS);

        root.getChildren().addAll(title, content);
        loadData();
        return root;
    }

    public void loadData() {
        allData.clear();
        allData.addAll(phieuDatPhongDAO.getAllPhieuDatPhong().stream()
                .filter(p -> StatusUtils.isBookingStatus(p.getTrangThai(), BookingStatus.DA_NHAN_PHONG))
                .collect(Collectors.toList()));
        filterData();
    }

    private void configureTable() {
        tableBooking.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<PhieuDatPhong, String> colMa = new TableColumn<>("Mã phiếu");
        colMa.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getMaDatPhong()));

        TableColumn<PhieuDatPhong, String> colKhach = new TableColumn<>("Khách hàng");
        colKhach.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getKhachHang() != null ? d.getValue().getKhachHang().getTenKhachHang() : "Khách lẻ"));

        TableColumn<PhieuDatPhong, String> colPhong = new TableColumn<>("Phòng");
        colPhong.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDsMaPhong()));

        TableColumn<PhieuDatPhong, String> colSoLuong = new TableColumn<>("SL phòng");
        colSoLuong.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.valueOf(d.getValue().getSoLuongPhong())));

        tableBooking.getColumns().setAll(colMa, colKhach, colPhong, colSoLuong);
    }

    private void filterData() {
        String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();
        bookingList.setAll(allData.stream()
                .filter(p -> keyword.isBlank()
                        || p.getMaDatPhong().toLowerCase().contains(keyword)
                        || (p.getKhachHang() != null && p.getKhachHang().getTenKhachHang().toLowerCase().contains(keyword)))
                .collect(Collectors.toList()));
    }

    private void handleCalculate() {
        PhieuDatPhong selected = tableBooking.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng chọn phiếu.");
            return;
        }

        currentCheckedInRooms = chiTietPhieuDatDAO.getDSChiTietByMaPhieuAndStatuses(
                selected.getMaDatPhong(),
                ChiTietPhieuDat.TRANG_THAI_DA_NHAN
        );
        if (currentCheckedInRooms.isEmpty()) {
            currentRoomTotal = 0;
            currentServiceTotal = 0;
            currentHoaDon = hoaDonDAO.getHoaDonByMaPhieu(selected.getMaDatPhong());
            txtSummary.setText("Phiếu " + selected.getMaDatPhong() + " hiện chưa có phòng nào đã nhận phòng.");
            btnPay.setDisable(true);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        currentRoomTotal = currentCheckedInRooms.stream()
                .mapToDouble(detail -> pricingService.calculateRoomCharge(detail, now))
                .sum();
        currentHoaDon = hoaDonDAO.getHoaDonByMaPhieu(selected.getMaDatPhong());
        currentServiceTotal = currentHoaDon != null ? currentHoaDon.getTongTienDichVu() : 0;

        StringBuilder builder = new StringBuilder();
        builder.append("Phiếu: ").append(selected.getMaDatPhong()).append('\n');
        builder.append("Khách hàng: ").append(selected.getKhachHang() != null ? selected.getKhachHang().getTenKhachHang() : "Khách lẻ").append('\n');
        builder.append("Số phòng đã nhận: ").append(currentCheckedInRooms.size()).append("\n\n");

        for (ChiTietPhieuDat detail : currentCheckedInRooms) {
            builder.append(detail.getPhong().getMaPhong()).append(" | ");
            builder.append(detail.getNgayNhan() != null ? detail.getNgayNhan().format(DATE_TIME_FORMAT) : "--").append(" | ");
            builder.append(String.format("%,.0f VNĐ", pricingService.calculateRoomCharge(detail, now))).append('\n');
        }

        builder.append("\nTổng tiền phòng: ").append(String.format("%,.0f VNĐ", currentRoomTotal));
        builder.append("\nTổng tiền dịch vụ: ").append(String.format("%,.0f VNĐ", currentServiceTotal));
        builder.append("\nTạm tính hóa đơn: ").append(String.format("%,.0f VNĐ", currentRoomTotal + currentServiceTotal));
        txtSummary.setText(builder.toString());
        btnPay.setDisable(false);
    }

    private void handlePayment() {
        PhieuDatPhong selected = tableBooking.getSelectionModel().getSelectedItem();
        if (selected == null || currentCheckedInRooms.isEmpty()) {
            return;
        }

        HoaDon hd = currentHoaDon;
        if (hd == null) {
            hd = new HoaDon(
                    hoaDonDAO.generateNextId(),
                    LocalDateTime.now(),
                    0.08,
                    currentRoomTotal,
                    currentServiceTotal,
                    selected,
                    new NhanVien("NV001")
            );
            hoaDonDAO.create(hd);
        } else {
            hoaDonDAO.updateTotalsAbsolute(hd.getMaHoaDon(), currentRoomTotal, currentServiceTotal);
        }

        btnPay.setDisable(true);
        loadData();
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật hóa đơn cho các phòng đã nhận phòng.");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
