package gui;

import javafx.application.Application;
import util.SwingLookAndFeel;

public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        SwingLookAndFeel.configure();
        Application.launch(StartUpGui.class, args);
    }
}
