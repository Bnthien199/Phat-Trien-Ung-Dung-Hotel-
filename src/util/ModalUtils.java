package util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.util.function.Supplier;

public class ModalUtils {

    public static void setupModalBehavior(Stage stage, boolean closeOnEsc, boolean confirmOnClose,
            Supplier<Boolean> isDirty) {
        installStageFocusRecovery(stage);

        stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                if (closeOnEsc) {
                    attemptClose(stage, confirmOnClose, isDirty);
                }
            }
        });

        if (stage.getScene() != null) {
            stage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
                if (confirmOnClose && isDirty != null && isDirty.get()) {
                    if (!confirmClose(stage)) {
                        event.consume();
                    }
                }
            });
        }
    }

    public static void setupDialogBehavior(Dialog<?> dialog, ButtonType cancelType, boolean closeOnEsc,
            boolean confirmOnClose, Supplier<Boolean> isDirty) {
        javafx.scene.control.DialogPane pane = dialog.getDialogPane();
        pane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                installSceneFocusRecovery(newScene);
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        event.consume();
                        if (closeOnEsc) {
                            attemptClose(newScene.getWindow(), confirmOnClose, isDirty);
                        }
                    }
                });
                newScene.windowProperty().addListener((wObs, oldW, newW) -> {
                    if (newW != null) {
                        installWindowFocusRecovery(newW);
                        newW.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
                            if (confirmOnClose && isDirty != null && isDirty.get()) {
                                if (!confirmClose(newW)) {
                                    event.consume();
                                }
                            }
                        });
                    }
                });
            }
        });

        if (cancelType != null) {
            Node cancelBtn = dialog.getDialogPane().lookupButton(cancelType);
            if (cancelBtn != null) {
                cancelBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                    if (confirmOnClose && isDirty != null && isDirty.get()) {
                        Window w = dialog.getDialogPane().getScene() != null
                                ? dialog.getDialogPane().getScene().getWindow()
                                : null;
                        if (!confirmClose(w)) {
                            event.consume();
                        }
                    }
                });
            }
        }
    }

    public static void setupDialogBehavior(Dialog<?> dialog, Window owner, ButtonType cancelType, boolean closeOnEsc,
            boolean confirmOnClose, Supplier<Boolean> isDirty) {
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        } else {
            dialog.initModality(Modality.APPLICATION_MODAL);
        }
        setupDialogBehavior(dialog, cancelType, closeOnEsc, confirmOnClose, isDirty);
    }

    private static void attemptClose(Window window, boolean confirmOnClose, Supplier<Boolean> isDirty) {
        if (window == null) {
            return;
        }
        if (confirmOnClose && isDirty != null && isDirty.get()) {
            if (confirmClose(window)) {
                closeWindow(window);
            }
        } else {
            closeWindow(window);
        }
    }

    private static void closeWindow(Window window) {
        if (window instanceof Stage) {
            ((Stage) window).close();
        } else {
            window.hide();
        }
    }

    public static boolean confirmClose(Window owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("Xác nhận đóng");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc muốn đóng? Dữ liệu chưa lưu sẽ bị mất.");

        ButtonType btnYes = new ButtonType("Đóng", ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        return alert.showAndWait().orElse(btnNo) == btnYes;
    }

    private static void installStageFocusRecovery(Stage stage) {
        if (stage == null) {
            return;
        }

        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
            if (stage.getOwner() == null && stage.getModality() == Modality.APPLICATION_MODAL) {
                stage.setAlwaysOnTop(true);
            }
            Platform.runLater(() -> focusWindow(stage));
        });

        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> stage.setAlwaysOnTop(false));
        stage.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> focusWindow(stage));
        stage.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (focused) {
                Platform.runLater(() -> focusWindow(stage));
            }
        });

        if (stage.getScene() != null) {
            installSceneFocusRecovery(stage.getScene());
        } else {
            stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    installSceneFocusRecovery(newScene);
                }
            });
        }
    }

    private static void installWindowFocusRecovery(Window window) {
        if (window instanceof Stage stage) {
            installStageFocusRecovery(stage);
        }
    }

    private static void installSceneFocusRecovery(Scene scene) {
        if (scene == null) {
            return;
        }
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            Window window = scene.getWindow();
            if (window != null) {
                focusWindow(window);
            }
        });
    }

    private static void focusWindow(Window window) {
        if (window == null) {
            return;
        }
        if (window instanceof Stage stage) {
            stage.toFront();
        }
        window.requestFocus();
    }
}
