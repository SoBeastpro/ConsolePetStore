package ru.mirea.petstore.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.petstore.dto.ImportDtos.ImportMode;
import ru.mirea.petstore.dto.ImportDtos.ImportPreview;
import ru.mirea.petstore.dto.ImportDtos.ImportResult;
import ru.mirea.petstore.dto.PageResponse;
import ru.mirea.petstore.dto.ReservationDtos.BulkRequest;
import ru.mirea.petstore.dto.ReservationDtos.BulkResult;
import ru.mirea.petstore.dto.ReservationDtos.ReservationRequest;
import ru.mirea.petstore.dto.ReservationDtos.ReservationResponse;
import ru.mirea.petstore.service.CsvReservationService;
import ru.mirea.petstore.service.ReservationService;
import ru.mirea.petstore.web.PageableFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {
    private final ReservationService reservations;
    private final CsvReservationService csv;
    private final PageableFactory pages;

    public ReservationController(ReservationService reservations, CsvReservationService csv, PageableFactory pages) {
        this.reservations = reservations;
        this.csv = csv;
        this.pages = pages;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','KEEPER','ADMIN')")
    public PageResponse<ReservationResponse> list(@RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(defaultValue = "date,desc") String sort) {
        return PageResponse.from(reservations.search(q, pages.reservations(page, size, sort)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','KEEPER','ADMIN')")
    public ReservationResponse get(@PathVariable UUID id) {
        return reservations.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('KEEPER','ADMIN')")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(201).body(reservations.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('KEEPER','ADMIN')")
    public ReservationResponse update(@PathVariable UUID id, @Valid @RequestBody ReservationRequest request) {
        return reservations.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('KEEPER','ADMIN')")
    public ReservationResponse archive(@PathVariable UUID id) {
        return reservations.archive(id);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('KEEPER','ADMIN')")
    public ReservationResponse restore(@PathVariable UUID id) {
        return reservations.restore(id);
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> permanentDelete(@PathVariable UUID id,
                                                @RequestHeader(value = "X-Confirm-Delete", required = false) String confirmation) {
        reservations.permanentDelete(id, confirmation);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('KEEPER','ADMIN')")
    public BulkResult bulk(@Valid @RequestBody BulkRequest request) {
        return reservations.bulk(request);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('KEEPER','ADMIN')")
    public ImportPreview preview(@RequestPart("file") MultipartFile file,
                                 @RequestParam(defaultValue = "CREATE_ONLY") ImportMode mode) {
        return csv.preview(file, mode);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('KEEPER','ADMIN')")
    public ImportResult apply(@RequestPart("file") MultipartFile file,
                              @RequestParam(defaultValue = "CREATE_ONLY") ImportMode mode) {
        return csv.apply(file, mode);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('VIEWER','KEEPER','ADMIN')")
    public void export(@RequestParam(required = false) String q, HttpServletResponse response) throws IOException {
        String filename = "reservations-" + LocalDate.now() + ".csv";
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        csv.export(q, response.getOutputStream());
    }
}
