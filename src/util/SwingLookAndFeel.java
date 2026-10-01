package util;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

public final class SwingLookAndFeel {
    private static boolean configured;

    private SwingLookAndFeel() {
    }

    public static void configure() {
        if (configured) {
            return;
        }

        configured = true;
        try {
            if (!trySetFlatLaf()) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception ex) {
            System.err.println("Could not configure Swing look and feel: " + ex.getMessage());
        }
    }

    private static boolean trySetFlatLaf() {
        String[] flatLafClasses = {
            "com.formdev.flatlaf.FlatLightLaf",
            "com.formdev.flatlaf.FlatIntelliJLaf"
        };

        for (String className : flatLafClasses) {
            try {
                Class<?> lafClass = Class.forName(className);
                Object lookAndFeel = lafClass.getDeclaredConstructor().newInstance();
                if (lookAndFeel instanceof LookAndFeel) {
                    UIManager.setLookAndFeel((LookAndFeel) lookAndFeel);
                    return true;
                }
            } catch (ClassNotFoundException ignored) {
                // FlatLaf is optional; fall back to the system look and feel.
            } catch (Exception ex) {
                System.err.println("Could not use FlatLaf: " + ex.getMessage());
                return false;
            }
        }

        return false;
    }
}
