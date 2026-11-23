package ru.mirea.petstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.petstore.domain.ReservationNote;

import java.util.List;
import java.util.UUID;

public interface ReservationNoteRepository extends JpaRepository<ReservationNote, UUID> {
    List<ReservationNote> findByReservationIdOrderByPinnedDescCreatedAtDesc(UUID reservationId);
    void deleteByReservationId(UUID reservationId);
}
