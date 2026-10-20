package petstore.util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import petstore.model.Client;
import petstore.model.Reservation;
import petstore.model.ReservationView;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelExporter {
    public void exportCurrent(Path file, List<Client> clients, List<ReservationView> reservations) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            writeClients(workbook.createSheet("Клиенты"), clients);
            writeCurrentReservations(workbook.createSheet("Заявки"), reservations);
            try (OutputStream output = Files.newOutputStream(file)) {
                workbook.write(output);
            }
        }
    }

    public void export(List<Client> clients, List<Reservation> reservations, Path file) throws IOException {
        Map<Long, String> names = new HashMap<>();
        for (Client client : clients) {
            names.put(client.getId(), client.getFullName());
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            writeClients(workbook.createSheet("Клиенты"), clients);
            writeReservations(workbook.createSheet("Заявки"), reservations, names);
            try (OutputStream output = Files.newOutputStream(file)) {
                workbook.write(output);
            }
        }
    }

    private void writeClients(Sheet sheet, List<Client> clients) {
        header(sheet.createRow(0), "ID", "ФИО", "Телефон", "Email");
        int rowIndex = 1;
        for (Client client : clients) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(client.getId());
            row.createCell(1).setCellValue(client.getFullName());
            row.createCell(2).setCellValue(client.getPhone());
            row.createCell(3).setCellValue(client.getEmail());
        }
        setWidths(sheet, 12, 28, 18, 28);
    }

    private void writeCurrentReservations(Sheet sheet, List<ReservationView> reservations) {
        header(sheet.createRow(0), "ID", "Клиент", "Кличка", "Статус", "Дата создания", "Комментарий");
        int rowIndex = 1;
        for (ReservationView reservation : reservations) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(reservation.id());
            row.createCell(1).setCellValue(reservation.clientName() == null ? "" : reservation.clientName());
            row.createCell(2).setCellValue(reservation.animalName());
            row.createCell(3).setCellValue(reservation.status().title());
            row.createCell(4).setCellValue(reservation.createdAt().toString());
            row.createCell(5).setCellValue(reservation.comment() == null ? "" : reservation.comment());
        }
        setWidths(sheet, 12, 28, 18, 16, 16, 40);
    }

    private void writeReservations(Sheet sheet, List<Reservation> reservations, Map<Long, String> names) {
        header(sheet.createRow(0), "ID", "ID клиента", "Клиент", "Кличка", "Статус", "Дата создания", "Комментарий");
        int rowIndex = 1;
        for (Reservation reservation : reservations) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(reservation.getId());
            row.createCell(1).setCellValue(reservation.getClientId());
            row.createCell(2).setCellValue(names.getOrDefault(reservation.getClientId(), ""));
            row.createCell(3).setCellValue(reservation.getAnimalName());
            row.createCell(4).setCellValue(reservation.getStatus().name());
            row.createCell(5).setCellValue(reservation.getCreatedAt().toString());
            row.createCell(6).setCellValue(reservation.getComment() == null ? "" : reservation.getComment());
        }
        setWidths(sheet, 12, 14, 28, 18, 16, 16, 32);
    }

    private void header(Row row, String... titles) {
        for (int i = 0; i < titles.length; i++) {
            row.createCell(i).setCellValue(titles[i]);
        }
    }

    private void setWidths(Sheet sheet, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }
}
