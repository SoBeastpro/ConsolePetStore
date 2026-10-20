package petstore.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DetailsController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label bodyLabel;

    public void show(String title, String body) {
        titleLabel.setText(title);
        bodyLabel.setText(body);
    }

    @FXML
    private void onClose() {
        ((Stage) titleLabel.getScene().getWindow()).close();
    }
}
