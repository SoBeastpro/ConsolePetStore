package petstore.fx.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petstore.exception.Messages;
import petstore.fx.Fx;
import petstore.ui.SqlErrors;

import java.sql.SQLException;

public final class Dialogs {
    private static final Logger log = LoggerFactory.getLogger(Dialogs.class);

    private Dialogs() {
    }

    public static void error(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        prepare(alert, owner);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void data(Window owner, SQLException exception) {
        log.error("Ошибка доступа к данным", exception);
        String message = SqlErrors.isConnectionError(exception) ? Messages.DB_CONNECTION : Messages.DATA_ACCESS;
        error(owner, message);
    }

    public static boolean confirm(Window owner, String question) {
        ButtonType yes = new ButtonType("Да", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Нет", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", yes, no);
        prepare(alert, owner);
        alert.setTitle("Подтверждение");
        alert.setHeaderText(question);
        alert.setContentText(null);
        Button yesButton = (Button) alert.getDialogPane().lookupButton(yes);
        Button noButton = (Button) alert.getDialogPane().lookupButton(no);
        yesButton.setDefaultButton(false);
        noButton.setDefaultButton(true);
        return alert.showAndWait().orElse(no) == yes;
    }

    private static void prepare(Alert alert, Window owner) {
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.getDialogPane().getStylesheets().add(Fx.stylesheet());
        alert.getDialogPane().setMinWidth(420);
    }
}
