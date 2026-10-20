package petstore.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petstore.exception.Messages;
import petstore.fx.util.Dialogs;
import petstore.ui.SqlErrors;
import petstore.util.DatabaseManager;

import java.sql.SQLException;

public class PetStoreFxApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(PetStoreFxApp.class);

    public static void main(String[] args) {
        System.setProperty("log4j2.statusLoggerLevel", "OFF");
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            DatabaseManager.init();
        } catch (SQLException exception) {
            log.error("Подключение к базе не удалось", exception);
            String message = SqlErrors.isConnectionError(exception) ? Messages.DB_CONNECTION : Messages.DATA_ACCESS;
            Dialogs.error(null, message);
            return;
        }
        Fx.Loaded<petstore.fx.controller.MainController> view = Fx.load("/petstore/fx/main-view.fxml", new AppContext());
        Scene scene = new Scene(view.root(), 1180, 760);
        stage.setTitle("Питомник животных");
        stage.setMinWidth(1020);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }
}
