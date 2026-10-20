package petstore.fx.util;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;
import petstore.exception.BusinessException;
import petstore.exception.Messages;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class Dates {
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private Dates() {
    }

    public static void apply(DatePicker picker) {
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : FORMAT.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                return LocalDate.parse(text.trim(), FORMAT);
            }
        });
        picker.setPromptText("ГГГГ-ММ-ДД");
    }

    public static LocalDate optional(DatePicker picker) {
        return parse(picker, false);
    }

    public static LocalDate required(DatePicker picker) {
        return parse(picker, true);
    }

    private static LocalDate parse(DatePicker picker, boolean required) {
        String text = picker.getEditor().getText();
        if (text == null || text.isBlank()) {
            if (required) {
                throw new BusinessException(Messages.DATE_REQUIRED);
            }
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), FORMAT);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(Messages.DATE_INVALID);
        }
    }
}
