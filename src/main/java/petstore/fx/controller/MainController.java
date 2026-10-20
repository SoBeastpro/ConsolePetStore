package petstore.fx.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import petstore.exception.BusinessException;
import petstore.exception.EntityNotFoundException;
import petstore.exception.Messages;
import petstore.fx.AppContext;
import petstore.fx.Fx;
import petstore.fx.util.Dates;
import petstore.fx.util.Dialogs;
import petstore.model.Client;
import petstore.model.Reservation;
import petstore.model.ReservationQuery;
import petstore.model.ReservationStatus;
import petstore.model.ReservationView;
import petstore.model.SortDirection;
import petstore.model.SortField;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class MainController {
    private final AppContext context;
    private final ObservableList<ReservationView> reservationRows = FXCollections.observableArrayList();
    private final ObservableList<Client> clientRows = FXCollections.observableArrayList();
    private boolean suppress;

    @FXML
    private Label statusLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private DatePicker fromDate;
    @FXML
    private DatePicker toDate;
    @FXML
    private ComboBox<String> sortField;
    @FXML
    private ComboBox<String> sortDirection;
    @FXML
    private TableView<ReservationView> reservationTable;
    @FXML
    private TableColumn<ReservationView, String> animalColumn;
    @FXML
    private TableColumn<ReservationView, String> clientColumn;
    @FXML
    private TableColumn<ReservationView, String> statusColumn;
    @FXML
    private TableColumn<ReservationView, String> createdColumn;
    @FXML
    private TableColumn<ReservationView, String> commentColumn;
    @FXML
    private Label reservationCountLabel;
    @FXML
    private TableView<Client> clientTable;
    @FXML
    private TableColumn<Client, String> clientNameColumn;
    @FXML
    private TableColumn<Client, String> phoneColumn;
    @FXML
    private TableColumn<Client, String> emailColumn;
    @FXML
    private Label clientCountLabel;

    public MainController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        Dates.apply(fromDate);
        Dates.apply(toDate);
        configureReservationTable();
        configureClientTable();
        configureFilters();
        reloadAll();
    }

    @FXML
    private void onRefresh() {
        statusLabel.setText("");
        reloadAll();
    }

    @FXML
    private void onSearch() {
        if (suppress) {
            return;
        }
        reloadReservations();
    }

    @FXML
    private void onReset() {
        suppress = true;
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        fromDate.setValue(null);
        fromDate.getEditor().clear();
        toDate.setValue(null);
        toDate.getEditor().clear();
        sortField.getSelectionModel().selectFirst();
        sortDirection.getSelectionModel().selectFirst();
        setInvalid(fromDate, false);
        setInvalid(toDate, false);
        suppress = false;
        reloadReservations();
    }

    @FXML
    private void onAddReservation() {
        ReservationFormController form = open("Новая заявка", "/petstore/fx/reservation-form.fxml", controller -> {
        });
        if (form.shouldReload()) {
            reloadReservations();
        }
    }

    @FXML
    private void onEditReservation() {
        ReservationView selected = selectedReservation();
        if (selected == null) {
            return;
        }
        try {
            Reservation fresh = context.reservations().getById(selected.id());
            ReservationFormController form = open("Изменение заявки", "/petstore/fx/reservation-form.fxml",
                    controller -> controller.edit(fresh));
            if (form.shouldReload()) {
                reloadReservations();
            }
        } catch (EntityNotFoundException exception) {
            Dialogs.error(window(), exception.getMessage());
            reloadReservations();
        } catch (SQLException exception) {
            Dialogs.data(window(), exception);
        }
    }

    @FXML
    private void onViewReservation() {
        ReservationView selected = selectedReservation();
        if (selected == null) {
            return;
        }
        try {
            Reservation fresh = context.reservations().getById(selected.id());
            String clientName = selected.clientName();
            try {
                clientName = context.clients().getById(fresh.getClientId()).getFullName();
            } catch (EntityNotFoundException exception) {
                clientName = selected.clientName();
            }
            String comment = fresh.getComment() == null || fresh.getComment().isBlank() ? "—" : fresh.getComment();
            String body = "Кличка: " + fresh.getAnimalName()
                    + "\nКлиент: " + clientName
                    + "\nСтатус: " + fresh.getStatus().title()
                    + "\nДата создания: " + fresh.getCreatedAt()
                    + "\nКомментарий: " + comment;
            showDetails("Заявка", body);
        } catch (EntityNotFoundException exception) {
            Dialogs.error(window(), exception.getMessage());
            reloadReservations();
        } catch (SQLException exception) {
            Dialogs.data(window(), exception);
        }
    }

    @FXML
    private void onDeleteReservation() {
        ReservationView selected = selectedReservation();
        if (selected == null) {
            return;
        }
        if (!Dialogs.confirm(window(), "Удалить заявку на животное «" + selected.animalName() + "»?")) {
            return;
        }
        try {
            context.reservations().delete(selected.id());
            statusLabel.setText("");
            reloadReservations();
        } catch (EntityNotFoundException exception) {
            Dialogs.error(window(), exception.getMessage());
            reloadReservations();
        } catch (SQLException exception) {
            Dialogs.data(window(), exception);
        }
    }

    @FXML
    private void onAddClient() {
        ClientFormController form = open("Новый клиент", "/petstore/fx/client-form.fxml", controller -> {
        });
        if (form.shouldReload()) {
            reloadAll();
        }
    }

    @FXML
    private void onEditClient() {
        Client selected = selectedClient();
        if (selected == null) {
            return;
        }
        try {
            Client fresh = context.clients().getById(selected.getId());
            ClientFormController form = open("Изменение клиента", "/petstore/fx/client-form.fxml",
                    controller -> controller.edit(fresh));
            if (form.shouldReload()) {
                reloadAll();
            }
        } catch (EntityNotFoundException exception) {
            Dialogs.error(window(), exception.getMessage());
            reloadAll();
        } catch (SQLException exception) {
            Dialogs.data(window(), exception);
        }
    }

    @FXML
    private void onViewClient() {
        Client selected = selectedClient();
        if (selected == null) {
            return;
        }
        try {
            Client fresh = context.clients().getById(selected.getId());
            String body = "ФИО: " + fresh.getFullName()
                    + "\nТелефон: " + fresh.getPhone()
                    + "\nEmail: " + fresh.getEmail();
            showDetails("Клиент", body);
        } catch (EntityNotFoundException exception) {
            Dialogs.error(window(), exception.getMessage());
            reloadAll();
        } catch (SQLException exception) {
            Dialogs.data(window(), exception);
        }
    }

    @FXML
    private void onDeleteClient() {
        Client selected = selectedClient();
        if (selected == null) {
            return;
        }
        if (!Dialogs.confirm(window(), "Удалить клиента «" + selected.getFullName() + "»?")) {
            return;
        }
        try {
            context.clients().delete(selected.getId());
            statusLabel.setText("");
            reloadAll();
        } catch (BusinessException | EntityNotFoundException exception) {
            Dialogs.error(window(), exception.getMessage());
            reloadAll();
        } catch (SQLException exception) {
            Dialogs.data(window(), exception);
        }
    }

    @FXML
    private void onStatistics() {
        Fx.Loaded<StatisticsController> loaded = Fx.load("/petstore/fx/statistics-view.fxml", context);
        Stage stage = Fx.dialog(window(), "Статистика", loaded.root());
        stage.showAndWait();
    }

    @FXML
    private void onExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт в Excel");
        chooser.setInitialFileName("petstore.xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Книга Excel (*.xlsx)", "*.xlsx"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        String path = file.getAbsolutePath();
        if (!path.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            file = new File(path + ".xlsx");
        }
        try {
            context.export().exportXlsx(file.toPath(), List.copyOf(clientRows), List.copyOf(reservationRows));
            statusLabel.setText("Экспорт сохранён: " + file.getName());
        } catch (BusinessException exception) {
            Dialogs.error(window(), exception.getMessage());
        }
    }

    private void configureFilters() {
        statusFilter.getItems().setAll("Все статусы", "Создана", "Подтверждена", "Завершена", "Отменена");
        statusFilter.getSelectionModel().selectFirst();
        sortField.getItems().setAll("По умолчанию", "Дата создания", "Кличка");
        sortField.getSelectionModel().selectFirst();
        sortDirection.getItems().setAll("По возрастанию", "По убыванию");
        sortDirection.getSelectionModel().selectFirst();
        sortDirection.setDisable(true);
        sortField.setOnAction(event -> {
            boolean defaultOrder = sortField.getValue() == null || "По умолчанию".equals(sortField.getValue());
            sortDirection.setDisable(defaultOrder);
            onSearch();
        });
        sortDirection.setOnAction(event -> onSearch());
        statusFilter.setOnAction(event -> onSearch());
        searchField.setOnAction(event -> onSearch());
    }

    private void configureReservationTable() {
        animalColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().animalName()));
        clientColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().clientName()));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().status().title()));
        createdColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().createdAt())));
        commentColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().comment()));
        commentColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setTooltip(null);
                    return;
                }
                setText(item);
                setTooltip(item.isBlank() ? null : new Tooltip(item));
            }
        });
        reservationTable.getColumns().forEach(column -> column.setSortable(false));
        reservationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        reservationTable.setItems(reservationRows);
        reservationTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && reservationTable.getSelectionModel().getSelectedItem() != null) {
                onEditReservation();
            }
        });
    }

    private void configureClientTable() {
        clientNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getFullName()));
        phoneColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getPhone()));
        emailColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getEmail()));
        clientTable.getColumns().forEach(column -> column.setSortable(false));
        clientTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        clientTable.setItems(clientRows);
        clientTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && clientTable.getSelectionModel().getSelectedItem() != null) {
                onEditClient();
            }
        });
    }

    private void reloadAll() {
        reloadClients();
        reloadReservations();
    }

    private void reloadClients() {
        try {
            clientRows.setAll(context.clients().list());
            clientCountLabel.setText("Клиентов: " + clientRows.size());
        } catch (SQLException exception) {
            Dialogs.data(window(), exception);
        }
    }

    private void reloadReservations() {
        try {
            setInvalid(fromDate, false);
            setInvalid(toDate, false);
            reservationRows.setAll(context.reservations().list(currentQuery()));
            reservationCountLabel.setText("Заявок: " + reservationRows.size());
        } catch (BusinessException exception) {
            boolean dateError = Messages.DATE_INVALID.equals(exception.getMessage())
                    || Messages.DATE_RANGE_INCOMPLETE.equals(exception.getMessage())
                    || Messages.DATE_RANGE_INVALID.equals(exception.getMessage());
            setInvalid(fromDate, dateError);
            setInvalid(toDate, dateError);
            Dialogs.error(window(), exception.getMessage());
        } catch (SQLException exception) {
            Dialogs.data(window(), exception);
        }
    }

    private ReservationQuery currentQuery() {
        String text = searchField.getText() == null ? "" : searchField.getText().trim();
        String search = text.isEmpty() ? null : text;
        return new ReservationQuery(search, selectedStatus(), Dates.optional(fromDate), Dates.optional(toDate),
                selectedSortField(), selectedSortDirection());
    }

    private ReservationStatus selectedStatus() {
        return switch (statusFilter.getValue() == null ? "" : statusFilter.getValue()) {
            case "Создана" -> ReservationStatus.CREATED;
            case "Подтверждена" -> ReservationStatus.CONFIRMED;
            case "Завершена" -> ReservationStatus.COMPLETED;
            case "Отменена" -> ReservationStatus.CANCELLED;
            default -> null;
        };
    }

    private SortField selectedSortField() {
        return switch (sortField.getValue() == null ? "" : sortField.getValue()) {
            case "Дата создания" -> SortField.CREATED_AT;
            case "Кличка" -> SortField.ANIMAL_NAME;
            default -> null;
        };
    }

    private SortDirection selectedSortDirection() {
        if (selectedSortField() == null) {
            return null;
        }
        return "По убыванию".equals(sortDirection.getValue()) ? SortDirection.DESC : SortDirection.ASC;
    }

    private ReservationView selectedReservation() {
        ReservationView selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error(window(), Messages.ROW_NOT_SELECTED);
        }
        return selected;
    }

    private Client selectedClient() {
        Client selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error(window(), Messages.ROW_NOT_SELECTED);
        }
        return selected;
    }

    private void showDetails(String title, String body) {
        Fx.Loaded<DetailsController> loaded = Fx.load("/petstore/fx/details-view.fxml", context);
        loaded.controller().show(title, body);
        Fx.dialog(window(), title, loaded.root()).showAndWait();
    }

    private <T> T open(String title, String fxml, Consumer<T> prepare) {
        Fx.Loaded<T> loaded = Fx.load(fxml, context);
        prepare.accept(loaded.controller());
        Fx.dialog(window(), title, loaded.root()).showAndWait();
        return loaded.controller();
    }

    private void setInvalid(javafx.scene.Node node, boolean invalid) {
        node.getStyleClass().remove("invalid");
        if (invalid) {
            node.getStyleClass().add("invalid");
        }
    }

    private Window window() {
        if (reservationTable == null || reservationTable.getScene() == null) {
            return null;
        }
        return reservationTable.getScene().getWindow();
    }
}
