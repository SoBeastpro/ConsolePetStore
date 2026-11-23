package ru.mirea.petstore.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petstore.domain.Reservation;
import ru.mirea.petstore.domain.ReservationNote;
import ru.mirea.petstore.domain.Role;
import ru.mirea.petstore.domain.UserAccount;
import ru.mirea.petstore.dto.NoteDtos.NoteRequest;
import ru.mirea.petstore.dto.NoteDtos.NoteResponse;
import ru.mirea.petstore.error.ConflictException;
import ru.mirea.petstore.error.NotFoundException;
import ru.mirea.petstore.repository.ReservationNoteRepository;
import ru.mirea.petstore.repository.ReservationRepository;
import ru.mirea.petstore.security.CurrentUserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NoteService {
    private final ReservationNoteRepository notes;
    private final ReservationRepository reservations;
    private final CurrentUserService currentUser;
    private final AuditService audit;

    public NoteService(ReservationNoteRepository notes, ReservationRepository reservations,
                       CurrentUserService currentUser, AuditService audit) {
        this.notes = notes;
        this.reservations = reservations;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID reservationId) {
        requireReservation(reservationId);
        return notes.findByReservationIdOrderByPinnedDescCreatedAtDesc(reservationId).stream()
                .map(NoteService::toResponse).toList();
    }

    @Transactional
    public NoteResponse create(UUID reservationId, NoteRequest request) {
        Reservation reservation = requireReservation(reservationId);
        UserAccount actor = currentUser.requireEntity();
        ReservationNote note = new ReservationNote(UUID.randomUUID(), reservation, actor, request.body().trim(), request.pinned());
        notes.save(note);
        audit.record("NOTE_CREATED", "RESERVATION_NOTE", note.getId(), "Добавлена заметка к заявке",
                Map.of("reservationId", reservationId));
        return toResponse(note);
    }

    @Transactional
    public NoteResponse update(UUID reservationId, UUID noteId, NoteRequest request) {
        ReservationNote note = requireNote(reservationId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        if (request.version() == null || request.version() != note.getVersion()) {
            throw new ConflictException("Заметка уже изменена. Обновите данные");
        }
        note.setBody(request.body().trim());
        note.setPinned(request.pinned());
        audit.record("NOTE_UPDATED", "RESERVATION_NOTE", noteId, "Изменена заметка", Map.of("reservationId", reservationId));
        return toResponse(note);
    }

    @Transactional
    public void delete(UUID reservationId, UUID noteId) {
        ReservationNote note = requireNote(reservationId, noteId);
        assertOwnerOrAdmin(note, currentUser.requireEntity());
        notes.delete(note);
        audit.record("NOTE_DELETED", "RESERVATION_NOTE", noteId, "Удалена заметка", Map.of("reservationId", reservationId));
    }

    private Reservation requireReservation(UUID id) {
        return reservations.findById(id).orElseThrow(() -> new NotFoundException("Заявка не найдена"));
    }

    private ReservationNote requireNote(UUID reservationId, UUID noteId) {
        ReservationNote note = notes.findById(noteId).orElseThrow(() -> new NotFoundException("Заметка не найдена"));
        if (!note.getReservation().getId().equals(reservationId)) throw new NotFoundException("Заметка не найдена");
        return note;
    }

    private static void assertOwnerOrAdmin(ReservationNote note, UserAccount actor) {
        if (!note.getAuthor().getId().equals(actor.getId()) && !actor.getRoles().contains(Role.ADMIN)) {
            throw new AccessDeniedException("Заметку может изменить автор или администратор");
        }
    }

    private static NoteResponse toResponse(ReservationNote note) {
        return new NoteResponse(note.getId(), note.getReservation().getId(), note.getAuthor().getId(),
                note.getAuthor().getDisplayName(), note.getBody(), note.isPinned(),
                note.getCreatedAt(), note.getUpdatedAt(), note.getVersion());
    }
}
