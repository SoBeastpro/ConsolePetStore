package ru.mirea.petstore.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.petstore.config.AppProperties;
import ru.mirea.petstore.domain.Reservation;
import ru.mirea.petstore.domain.ReservationStatus;
import ru.mirea.petstore.domain.ShelterClient;
import ru.mirea.petstore.domain.UserAccount;
import ru.mirea.petstore.dto.ImportDtos.ImportMode;
import ru.mirea.petstore.dto.ImportDtos.ImportPreview;
import ru.mirea.petstore.dto.ImportDtos.ImportResult;
import ru.mirea.petstore.dto.ImportDtos.RowIssue;
import ru.mirea.petstore.dto.ReservationDtos.ReservationRequest;
import ru.mirea.petstore.dto.ReservationDtos.ReservationResponse;
import ru.mirea.petstore.error.BadRequestException;
import ru.mirea.petstore.filter.ReservationSpecifications;
import ru.mirea.petstore.filter.SmartFilterParser;
import ru.mirea.petstore.repository.ReservationRepository;
import ru.mirea.petstore.repository.ShelterClientRepository;
import ru.mirea.petstore.security.CurrentUserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CsvReservationService {
    private static final String[] HEADERS = {
            "code", "animalName", "comment", "reservationDate", "client",
            "status", "depositAmount", "needsCare", "tags"
    };

    private final ReservationRepository reservations;
    private final ShelterClientRepository clients;
    private final ReservationService reservationService;
    private final CurrentUserService currentUser;
    private final SmartFilterParser parser;
    private final ReservationMapper mapper;
    private final Validator validator;
    private final AppProperties properties;
    private final AuditService audit;

    public CsvReservationService(ReservationRepository reservations, ShelterClientRepository clients,
                                 ReservationService reservationService, CurrentUserService currentUser,
                                 SmartFilterParser parser, ReservationMapper mapper, Validator validator,
                                 AppProperties properties, AuditService audit) {
        this.reservations = reservations;
        this.clients = clients;
        this.reservationService = reservationService;
        this.currentUser = currentUser;
        this.parser = parser;
        this.mapper = mapper;
        this.validator = validator;
        this.properties = properties;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public ImportPreview preview(MultipartFile file, ImportMode mode) {
        ParsedFile parsed = parse(file, mode == null ? ImportMode.CREATE_ONLY : mode);
        return new ImportPreview(parsed.totalRows(), parsed.rows().size(), parsed.issues().size(), parsed.issues());
    }

    @Transactional
    public ImportResult apply(MultipartFile file, ImportMode mode) {
        ImportMode effectiveMode = mode == null ? ImportMode.CREATE_ONLY : mode;
        ParsedFile parsed = parse(file, effectiveMode);
        UserAccount actor = currentUser.requireEntity();
        int created = 0;
        int updated = 0;
        int skipped = parsed.issues().size();
        List<RowIssue> runtimeIssues = new ArrayList<>(parsed.issues());
        for (ParsedRow row : parsed.rows()) {
            try {
                Reservation existing = reservations.findByCodeIgnoreCase(row.request().code()).orElse(null);
                if (existing != null && effectiveMode == ImportMode.CREATE_ONLY) {
                    runtimeIssues.add(new RowIssue(row.rowNumber(), "code", "Запись уже существует"));
                    skipped++;
                    continue;
                }
                reservationService.upsertFromImport(existing, row.request(), actor);
                if (existing == null) created++;
                else updated++;
            } catch (RuntimeException ex) {
                runtimeIssues.add(new RowIssue(row.rowNumber(), "row", safeMessage(ex)));
                skipped++;
            }
        }
        audit.record("RESERVATION_CSV_IMPORTED", "RESERVATION", null, "Импортирован CSV заявок",
                Map.of("created", created, "updated", updated, "skipped", skipped, "mode", effectiveMode.name()));
        return new ImportResult(parsed.totalRows(), created, updated, skipped, List.copyOf(runtimeIssues));
    }

    @Transactional(readOnly = true)
    public void export(String query, OutputStream output) throws IOException {
        output.write("\ufeff".getBytes(StandardCharsets.UTF_8));
        try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).get())) {
            int page = 0;
            while (true) {
                var result = reservations.findAll(ReservationSpecifications.from(parser.parse(query)),
                        PageRequest.of(page, 500, Sort.by("reservationDate").descending()));
                for (Reservation reservation : result.getContent()) {
                    ReservationResponse dto = mapper.toResponse(reservation);
                    printer.printRecord(
                            safeCell(dto.code()), safeCell(dto.animalName()), safeCell(dto.comment()),
                            dto.reservationDate(), safeCell(dto.clientCode()), dto.status(),
                            dto.depositAmount(), dto.needsCare(), safeCell(String.join("|", dto.tags())));
                }
                if (result.isLast()) break;
                page++;
            }
            printer.flush();
        }
        audit.record("RESERVATION_CSV_EXPORTED", "RESERVATION", null, "Экспортирована выборка заявок",
                Map.of("query", query == null ? "" : query));
    }

    private ParsedFile parse(MultipartFile file, ImportMode mode) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Выберите непустой CSV-файл");
        List<ParsedRow> validRows = new ArrayList<>();
        List<RowIssue> issues = new ArrayList<>();
        Set<String> codesInFile = new LinkedHashSet<>();
        int total = 0;
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true).setTrim(true).get();
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csv = CSVParser.parse(reader, format)) {
            validateHeaders(csv.getHeaderMap());
            for (CSVRecord record : csv) {
                total++;
                long rowNumber = record.getRecordNumber() + 1;
                if (total > properties.importLimits().maxRows()) {
                    throw new BadRequestException("CSV содержит больше " + properties.importLimits().maxRows() + " строк");
                }
                try {
                    ParsedRow row = toRow(record, rowNumber);
                    Set<ConstraintViolation<ReservationRequest>> violations = validator.validate(row.request());
                    if (!violations.isEmpty()) {
                        for (ConstraintViolation<ReservationRequest> violation : violations) {
                            issues.add(new RowIssue(rowNumber, violation.getPropertyPath().toString(), violation.getMessage()));
                        }
                        continue;
                    }
                    String key = row.request().code().toLowerCase(Locale.ROOT);
                    if (!codesInFile.add(key)) {
                        issues.add(new RowIssue(rowNumber, "code", "Дубликат code внутри файла"));
                        continue;
                    }
                    if (mode == ImportMode.CREATE_ONLY && reservations.existsByCodeIgnoreCase(row.request().code())) {
                        issues.add(new RowIssue(rowNumber, "code", "Запись уже существует"));
                        continue;
                    }
                    validRows.add(row);
                } catch (RuntimeException ex) {
                    issues.add(new RowIssue(rowNumber, "row", safeMessage(ex)));
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("Не удалось прочитать CSV в UTF-8");
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Некорректная структура CSV: " + ex.getMessage());
        }
        return new ParsedFile(total, List.copyOf(validRows), List.copyOf(issues));
    }

    private ParsedRow toRow(CSVRecord record, long rowNumber) {
        String clientCode = required(record, "client");
        ShelterClient client = clients.findByCodeIgnoreCase(clientCode)
                .orElseThrow(() -> new BadRequestException("Клиент не найден: " + clientCode));
        if (!client.isActive()) throw new BadRequestException("Клиент неактивен: " + clientCode);
        ReservationRequest request = new ReservationRequest(
                required(record, "code"),
                required(record, "animalName"),
                nullable(record, "comment"),
                parseDate(required(record, "reservationDate")),
                client.getId(),
                parseStatus(required(record, "status")),
                parseDeposit(required(record, "depositAmount")),
                parseBoolean(nullable(record, "needsCare")),
                parseTags(nullable(record, "tags")),
                null);
        return new ParsedRow(rowNumber, request);
    }

    private static void validateHeaders(Map<String, Integer> actual) {
        for (String header : HEADERS) {
            if (!actual.containsKey(header)) throw new BadRequestException("Отсутствует колонка: " + header);
        }
    }

    private static String required(CSVRecord record, String field) {
        String value = nullable(record, field);
        if (value == null) throw new BadRequestException("Не заполнено поле " + field);
        return value;
    }

    private static String nullable(CSVRecord record, String field) {
        String value = record.get(field);
        if (value == null) return null;
        value = value.replace("\ufeff", "").trim();
        return value.isEmpty() ? null : value;
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Дата должна иметь формат YYYY-MM-DD");
        }
    }

    private static BigDecimal parseDeposit(String value) {
        try {
            return new BigDecimal(value.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Некорректный depositAmount");
        }
    }

    private static boolean parseBoolean(String value) {
        if (value == null) return false;
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "да" -> true;
            case "false", "0", "no", "нет" -> false;
            default -> throw new BadRequestException("needsCare принимает true/false или да/нет");
        };
    }

    private static ReservationStatus parseStatus(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "created", "создана" -> ReservationStatus.CREATED;
            case "confirmed", "подтверждена" -> ReservationStatus.CONFIRMED;
            case "completed", "завершена" -> ReservationStatus.COMPLETED;
            case "cancelled", "canceled", "отменена" -> ReservationStatus.CANCELLED;
            default -> throw new BadRequestException("Неизвестный status: " + value);
        };
    }

    private static Set<String> parseTags(String value) {
        if (value == null) return Set.of();
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        Arrays.stream(value.split("\\|")).map(String::trim).filter(v -> !v.isEmpty())
                .map(v -> v.toLowerCase(Locale.ROOT)).forEach(tags::add);
        return tags;
    }

    private static String safeCell(Object value) {
        if (value == null) return "";
        String cell = String.valueOf(value);
        if (!cell.isEmpty() && "=+-@\t\r".indexOf(cell.charAt(0)) >= 0) return "'" + cell;
        return cell;
    }

    private static String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "Некорректная строка" : message;
    }

    private record ParsedRow(long rowNumber, ReservationRequest request) {
    }

    private record ParsedFile(int totalRows, List<ParsedRow> rows, List<RowIssue> issues) {
    }
}
