package ru.mirea.petstore.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.petstore.dto.NoteDtos.NoteRequest;
import ru.mirea.petstore.dto.NoteDtos.NoteResponse;
import ru.mirea.petstore.service.NoteService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations/{reservationId}/notes")
@SecurityRequirement(name = "bearerAuth")
public class NoteController {
    private final NoteService notes;

    public NoteController(NoteService notes) {
        this.notes = notes;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','KEEPER','ADMIN')")
    public List<NoteResponse> list(@PathVariable UUID reservationId) {
        return notes.list(reservationId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('KEEPER','ADMIN')")
    public ResponseEntity<NoteResponse> create(@PathVariable UUID reservationId, @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(201).body(notes.create(reservationId, request));
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("hasAnyRole('KEEPER','ADMIN')")
    public NoteResponse update(@PathVariable UUID reservationId, @PathVariable UUID noteId,
                               @Valid @RequestBody NoteRequest request) {
        return notes.update(reservationId, noteId, request);
    }

    @DeleteMapping("/{noteId}")
    @PreAuthorize("hasAnyRole('KEEPER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID reservationId, @PathVariable UUID noteId) {
        notes.delete(reservationId, noteId);
        return ResponseEntity.noContent().build();
    }
}
