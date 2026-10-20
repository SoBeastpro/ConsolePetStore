package petstore.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import petstore.exception.BusinessException;
import petstore.exception.EntityNotFoundException;
import petstore.exception.Messages;
import petstore.fx.AppContext;
import petstore.fx.util.Dialogs;
import petstore.model.Client;
import petstore.service.ClientService;

import java.sql.SQLException;

public class ClientFormController {
    private final ClientService clientService;
    private long id;
    private boolean editing;
    private boolean reloadRequested;

    @FXML
    private Label formTitle;
    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;

    public ClientFormController(AppContext context) {
        this.clientService = context.clients();
    }

    public void edit(Client client) {
        editing = true;
        id = client.getId();
        formTitle.setText("Изменение клиента");
        nameField.setText(client.getFullName());
        phoneField.setText(client.getPhone());
        emailField.setText(client.getEmail());
    }

    public boolean shouldReload() {
        return reloadRequested;
    }

    @FXML
    private void onSave() {
        clearMarks();
        try {
            if (editing) {
                clientService.update(id, nameField.getText(), phoneField.getText(), emailField.getText());
            } else {
                clientService.create(nameField.getText(), phoneField.getText(), emailField.getText());
            }
            reloadRequested = true;
            close();
        } catch (BusinessException exception) {
            mark(exception.getMessage());
            Dialogs.error(window(), exception.getMessage());
        } catch (EntityNotFoundException exception) {
            Dialogs.error(window(), exception.getMessage());
            reloadRequested = true;
            close();
        } catch (SQLException exception) {
            Dialogs.data(window(), exception);
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void mark(String message) {
        setInvalid(nameField, Messages.CLIENT_NAME_REQUIRED.equals(message) || Messages.CLIENT_NAME_LENGTH.equals(message));
        boolean phone = Messages.CLIENT_PHONE_REQUIRED.equals(message)
                || Messages.CLIENT_PHONE_LENGTH.equals(message)
                || Messages.CLIENT_PHONE_DUPLICATE.equals(message);
        boolean email = Messages.CLIENT_EMAIL_REQUIRED.equals(message)
                || Messages.CLIENT_EMAIL_LENGTH.equals(message)
                || Messages.CLIENT_EMAIL_DUPLICATE.equals(message);
        if (Messages.CLIENT_DUPLICATE.equals(message)) {
            phone = true;
            email = true;
        }
        setInvalid(phoneField, phone);
        setInvalid(emailField, email);
    }

    private void clearMarks() {
        setInvalid(nameField, false);
        setInvalid(phoneField, false);
        setInvalid(emailField, false);
    }

    private void setInvalid(Node node, boolean invalid) {
        node.getStyleClass().remove("invalid");
        if (invalid) {
            node.getStyleClass().add("invalid");
        }
    }

    private void close() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    private Window window() {
        return nameField.getScene() == null ? null : nameField.getScene().getWindow();
    }
}
