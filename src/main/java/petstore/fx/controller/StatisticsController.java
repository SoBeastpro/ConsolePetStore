package petstore.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import petstore.exception.BusinessException;
import petstore.fx.AppContext;
import petstore.fx.util.Dates;
import petstore.fx.util.Dialogs;
import petstore.model.StatisticsReport;
import petstore.service.StatisticsService;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

public class StatisticsController {
    private final StatisticsService statisticsService;

    @FXML
    private DatePicker fromDate;
    @FXML
    private DatePicker toDate;
    @FXML
    private Label clientsValue;
    @FXML
    private Label reservationsValue;
    @FXML
    private Label createdValue;
    @FXML
    private Label confirmedValue;
    @FXML
    private Label completedValue;
    @FXML
    private Label cancelledValue;
    @FXML
    private Label shareValue;
    @FXML
    private Label earliestValue;
    @FXML
    private Label latestValue;
    @FXML
    private VBox periodCard;
    @FXML
    private Label periodValue;

    public StatisticsController(AppContext context) {
        this.statisticsService = context.statistics();
    }

    @FXML
    private void initialize() {
        Dates.apply(fromDate);
        Dates.apply(toDate);
        onCalculate();
    }

    @FXML
    private void onCalculate() {
        try {
            LocalDate from = Dates.optional(fromDate);
            LocalDate to = Dates.optional(toDate);
            show(statisticsService.report(from, to));
        } catch (BusinessException exception) {
            Dialogs.error(window(), exception.getMessage());
        } catch (SQLException exception) {
            Dialogs.data(window(), exception);
        }
    }

    @FXML
    private void onClose() {
        ((Stage) clientsValue.getScene().getWindow()).close();
    }

    private void show(StatisticsReport report) {
        clientsValue.setText(Long.toString(report.clientCount()));
        reservationsValue.setText(Long.toString(report.reservationCount()));
        createdValue.setText(Long.toString(report.createdCount()));
        confirmedValue.setText(Long.toString(report.confirmedCount()));
        completedValue.setText(Long.toString(report.completedCount()));
        cancelledValue.setText(Long.toString(report.cancelledCount()));
        shareValue.setText(shareText(report));
        earliestValue.setText(report.earliestCreatedAt() == null ? "—" : report.earliestCreatedAt().toString());
        latestValue.setText(report.latestCreatedAt() == null ? "—" : report.latestCreatedAt().toString());
        boolean hasPeriod = report.reservationsInPeriod() != null;
        periodCard.setVisible(hasPeriod);
        periodCard.setManaged(hasPeriod);
        if (hasPeriod) {
            periodValue.setText(Long.toString(report.reservationsInPeriod()));
        }
    }

    private String shareText(StatisticsReport report) {
        if (report.reservationCount() == 0) {
            return "0";
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ru"));
        DecimalFormat format = new DecimalFormat("0.0", symbols);
        return format.format(report.completedShare()) + "%";
    }

    private javafx.stage.Window window() {
        if (clientsValue.getScene() == null) {
            return null;
        }
        return clientsValue.getScene().getWindow();
    }
}
