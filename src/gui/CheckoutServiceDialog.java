package gui;

import dao.DichVuDAO;
import entity.DichVu;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class CheckoutServiceDialog {
    private final DichVuDAO dichVuDAO = new DichVuDAO();
    private final Window owner;

    public CheckoutServiceDialog(Window owner) {
        this.owner = owner;
    }

    /**
     * Opens the dialog. onServiceAdded callback is invoked each time the user clicks "Thêm"
     * for a service; the dialog stays open so the user can pick multiple services.
     * The dialog closes only when the user clicks "Đóng".
     */
    public void show(String roomId, Consumer<CheckoutServiceItem> onServiceAdded) {
        List<DichVu> services = new ArrayList<>(dichVuDAO.getActiveDichVu());
        if (services.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Không có dịch vụ khả dụng.", ButtonType.OK).showAndWait();
            return;
        }

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Thêm dịch vụ cho phòng " + roomId);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("checkout-dialog");
        root.setPadding(new Insets(18));

        Label title = new Label("THÊM DỊCH VỤ");
        title.getStyleClass().add("checkout-title");

        Label subtitle = new Label("Đang thêm dịch vụ cho phòng: " + roomId
                + ". Chọn dịch vụ và số lượng, nhấn \"Thêm\" để ghi nhận. Nhấn \"Đóng\" sau khi hoàn tất.");
        subtitle.getStyleClass().add("checkout-subtitle");
        subtitle.setWrapText(true);

        VBox header = new VBox(4, title, subtitle);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Tìm theo tên dịch vụ...");
        txtSearch.getStyleClass().add("checkout-search");
        txtSearch.setPrefHeight(38);

        VBox serviceList = new VBox(12);
        ScrollPane scrollPane = new ScrollPane(serviceList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(420);
        scrollPane.getStyleClass().addAll("checkout-scroll", "clean-scroll");

        Runnable render = () -> {
            serviceList.getChildren().clear();
            String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);

            for (DichVu dichVu : services) {
                if (!keyword.isBlank()
                        && (dichVu.getTenDichVu() == null
                        || !dichVu.getTenDichVu().toLowerCase(Locale.ROOT).contains(keyword))) {
                    continue;
                }

                Label lblName = new Label(dichVu.getTenDichVu());
                lblName.getStyleClass().add("checkout-card-title");

                Label lblMeta = new Label("Mã: " + dichVu.getMaDichVu() + " | Đơn giá: "
                        + String.format("%,.0f VNĐ / lượt", dichVu.getDonGia()));
                lblMeta.getStyleClass().add("checkout-card-meta");

                Label lblPrice = new Label(String.format("%,.0f VNĐ / lượt", dichVu.getDonGia()));
                lblPrice.getStyleClass().add("checkout-card-value");

                Spinner<Integer> spQty = new Spinner<>();
                spQty.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
                spQty.setEditable(true);
                spQty.setPrefWidth(88);
                spQty.getStyleClass().add("checkout-spinner");

                Label lblQty = new Label("Số lượng");
                lblQty.getStyleClass().add("checkout-card-meta");

                Label lblFeedback = new Label();
                lblFeedback.getStyleClass().add("checkout-card-meta");
                lblFeedback.setStyle("-fx-text-fill: #2ecc71;");

                Button btnAdd = new Button("Thêm");
                btnAdd.getStyleClass().add("btn-primary");
                btnAdd.setPrefHeight(36);
                btnAdd.setOnAction(e -> {
                    int qty = spQty.getValue();
                    if (qty <= 0) {
                        new Alert(Alert.AlertType.WARNING, "Số lượng phải lớn hơn 0.", ButtonType.OK).showAndWait();
                        return;
                    }
                    CheckoutServiceItem item = new CheckoutServiceItem(dichVu, roomId, qty, dichVu.getDonGia());
                    try {
                        onServiceAdded.accept(item);
                        lblFeedback.setText("✓ Đã thêm " + qty + " x " + dichVu.getTenDichVu());
                        spQty.getValueFactory().setValue(1);
                    } catch (RuntimeException ex) {
                        new Alert(Alert.AlertType.ERROR,
                                "Lỗi khi thêm dịch vụ: " + ex.getMessage(), ButtonType.OK).showAndWait();
                    }
                });

                VBox left = new VBox(6, lblName, lblMeta, lblPrice, lblFeedback);
                HBox.setHgrow(left, Priority.ALWAYS);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                VBox right = new VBox(8, lblQty, new HBox(8, spQty, btnAdd));
                right.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(16, left, spacer, right);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("checkout-service-card");
                row.setPadding(new Insets(14));
                serviceList.getChildren().add(row);
            }

            if (serviceList.getChildren().isEmpty()) {
                Label empty = new Label("Không tìm thấy dịch vụ phù hợp.");
                empty.getStyleClass().add("checkout-empty-label");
                serviceList.getChildren().add(empty);
            }
        };

        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> render.run());
        render.run();

        Button btnClose = new Button("Đóng");
        btnClose.getStyleClass().add("btn-secondary");
        btnClose.setPrefHeight(36);
        btnClose.setOnAction(e -> stage.close());

        HBox footer = new HBox(btnClose);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 0, 0, 0));

        VBox center = new VBox(12, txtSearch, scrollPane);
        root.setTop(header);
        root.setCenter(center);
        root.setBottom(footer);

        Scene scene = new Scene(root, 820, 600);
        scene.getStylesheets().addAll(
                Paths.get("src", "gui", "global-theme.css").toAbsolutePath().toUri().toString(),
                Paths.get("src", "gui", "checkout-flow.css").toAbsolutePath().toUri().toString()
        );
        stage.setScene(scene);
        stage.showAndWait();
    }
}
