package petstore.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import petstore.exception.BusinessException;
import petstore.exception.EntityNotFoundException;
import petstore.exception.Messages;
import petstore.fx.AppContext;
import petstore.fx.util.Dates;
import petstore.fx.util.Dialogs;
import petstore.model.Client;
import petstore.model.Reservation;
import petstore.model.ReservationStatus;
import petstore.service.ClientService;
import petstore.service.ReservationService;

import java.sql.SQLException;

public class ReservationFormController {
    private final ClientService clientService;
    private final ReservationService reservationService;
    private long id;
    private boolean editing;
    private boolean reloadRequested;

    @FXML
    private Label formTitle;
    @FXML
    private ComboBox<Client> clientBox;
    @FXML
    private TextField animalField;
    @FXML
    private DatePicker createdAtPicker;
    @FXML
    private Label statusLabel;
    @FXML
    private ComboBox<ReservationStatus> statusBox;
    @FXML
    private TextArea commentArea;

    public ReservationFormController(AppContext context) {
        this.clientService = context.clients();
        this.reservationService = context.reservations();
    }

    @FXML
    private void initialize() {
        Dates.apply(createdAtPicker);
        clientBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Client client) {
                if (client == null) {
                    return "";
                }
                return client.getFullName() + " · " + client.getPhone();
            }

            @Override
            public Client fromString(String text) {
                return null;
            }
        });
        statusBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ReservationStatus status) {
                return status == null ? "" : status.title();
            }

            @Override
            public ReservationStatus fromString(String text) {
                return null;
            }
        });
        try {
            clientBox.getItems().setAll(clientService.list());
        } catch (SQLException exception) {
            Dialogs.data(null, exception);
        }
    }

    public void edit(Reservation reservation) {
        editing = true;
        id = reservation.getId();
        formTitle.setText("Изменение заявки");
        animalField.setText(reservation.getAnimalName());
        createdAtPicker.setValue(reservation.getCreatedAt());
        commentArea.setText(reservation.getComment() == null ? "" : reservation.getComment());
        for (Client client : clientBox.getItems()) {
            if (client.getId() == reservation.getClientId()) {
                clientBox.getSelectionModel().select(client);
                break;
            }
        }
        statusBox.getItems().setAll(reservation.getStatus().allowedTargets());
        statusBox.getSelectionModel().select(reservation.getStatus());
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusBox.setVisible(true);
        statusBox.setManaged(true);
    }

    public boolean shouldReload() {
        return reloadRequested;
    }

    @FXML
    private void onSave() {
        clearMarks();
        try {
            Client client = clientBox.getValue();
            if (client == null) {
                throw new BusinessException(Messages.CLIENT_NOT_SELECTED);
            }
            String animal = animalField.getText();
            var createdAt = Dates.required(createdAtPicker);
            String comment = commentArea.getText();
            if (editing) {
                ReservationStatus status = statusBox.getValue();
                if (status == null) {
                    throw new BusinessException(Messages.STATUS_INVALID);
                }
                reservationService.update(id, client.getId(), animal, createdAt, status, comment);
            } else {
                reservationService.create(client.getId(), animal, createdAt, comment);
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
        setInvalid(clientBox, Messages.CLIENT_NOT_SELECTED.equals(message) || Messages.CLIENT_MISSING.equals(message));
        setInvalid(animalField, Messages.ANIMAL_NAME_REQUIRED.equals(message) || Messages.ANIMAL_NAME_LENGTH.equals(message));
        setInvalid(createdAtPicker, Messages.DATE_REQUIRED.equals(message) || Messages.DATE_INVALID.equals(message));
        setInvalid(commentArea, Messages.COMMENT_LENGTH.equals(message));
        setInvalid(statusBox, Messages.STATUS_INVALID.equals(message) || message.startsWith("Нельзя перевести заявку"));
    }

    private void clearMarks() {
        setInvalid(clientBox, false);
        setInvalid(animalField, false);
        setInvalid(createdAtPicker, false);
        setInvalid(commentArea, false);
        setInvalid(statusBox, false);
    }

    private void setInvalid(Node node, boolean invalid) {
        node.getStyleClass().remove("invalid");
        if (invalid) {
            node.getStyleClass().add("invalid");
        }
    }

    private void close() {
        ((Stage) animalField.getScene().getWindow()).close();
    }

    private Window window() {
        return animalField.getScene() == null ? null : animalField.getScene().getWindow();
    }
}
