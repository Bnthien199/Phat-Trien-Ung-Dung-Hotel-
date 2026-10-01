package util;

import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

import java.util.function.UnaryOperator;

public final class TextInputSafeUtils {

    private TextInputSafeUtils() {
    }

    public static int clampIndex(int index, int length) {
        return Math.max(0, Math.min(index, Math.max(0, length)));
    }

    public static void safeSelectRange(TextInputControl control, int start, int end) {
        if (control == null) {
            return;
        }
        String text = control.getText();
        int length = text == null ? 0 : text.length();
        int safeStart = clampIndex(Math.min(start, end), length);
        int safeEnd = clampIndex(Math.max(start, end), length);
        control.selectRange(safeStart, safeEnd);
    }

    public static void safePositionCaret(TextInputControl control, int caretPosition) {
        safeSelectRange(control, caretPosition, caretPosition);
    }

    public static void safeSetText(TextInputControl control, String text) {
        if (control == null) {
            return;
        }
        String currentText = control.getText();
        String safeCurrent = currentText == null ? "" : currentText;
        String safeText = text == null ? "" : text;
        int caret = clampIndex(control.getCaretPosition(), safeCurrent.length());
        control.setText(safeText);
        safePositionCaret(control, clampIndex(caret, safeText.length()));
    }

    public static TextFormatter<String> filteringFormatter(UnaryOperator<String> sanitizer) {
        return new TextFormatter<>(change -> {
            String currentText = change.getControlText() == null ? "" : change.getControlText();
            int currentLength = currentText.length();

            int start = clampIndex(Math.min(change.getRangeStart(), change.getRangeEnd()), currentLength);
            int end = clampIndex(Math.max(change.getRangeStart(), change.getRangeEnd()), currentLength);
            if (start != change.getRangeStart() || end != change.getRangeEnd()) {
                change.setRange(start, end);
            }

            if (!change.isContentChange()) {
                int anchor = clampIndex(change.getAnchor(), currentLength);
                int caret = clampIndex(change.getCaretPosition(), currentLength);
                change.selectRange(Math.min(anchor, caret), Math.max(anchor, caret));
                return change;
            }

            String insertedText = change.getText() == null ? "" : change.getText();
            String candidate = currentText.substring(0, start) + insertedText + currentText.substring(end);
            String sanitized = sanitizer.apply(candidate);
            if (sanitized == null) {
                return null;
            }

            if (!sanitized.equals(candidate)) {
                change.setRange(0, currentLength);
                change.setText(sanitized);
                int caret = clampIndex(start + insertedText.length(), sanitized.length());
                change.selectRange(caret, caret);
                return change;
            }

            int nextLength = candidate.length();
            int anchor = clampIndex(change.getAnchor(), nextLength);
            int caret = clampIndex(change.getCaretPosition(), nextLength);
            change.selectRange(Math.min(anchor, caret), Math.max(anchor, caret));
            return change;
        });
    }
}
