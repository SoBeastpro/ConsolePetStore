package petstore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petstore.exception.BusinessException;
import petstore.exception.Messages;
import petstore.model.Client;
import petstore.model.ReservationView;
import petstore.util.ExcelExporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ExportService {
    private static final Logger log = LoggerFactory.getLogger(ExportService.class);
    private final ExcelExporter exporter = new ExcelExporter();

    public void exportXlsx(Path file, List<Client> clients, List<ReservationView> reservations) {
        try {
            exporter.exportCurrent(file, clients, reservations);
        } catch (IOException | RuntimeException exception) {
            log.error("Не удалось записать файл {}", file, exception);
            throw new BusinessException(Messages.EXPORT_FAILED);
        }
    }
}
