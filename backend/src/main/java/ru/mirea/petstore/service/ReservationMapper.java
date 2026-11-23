package ru.mirea.petstore.service;

import org.springframework.stereotype.Component;
import ru.mirea.petstore.domain.Reservation;
import ru.mirea.petstore.dto.ReservationDtos.ReservationResponse;

import java.util.Set;

@Component
public class ReservationMapper {
    public ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getCode(),
                reservation.getAnimalName(),
                reservation.getComment(),
                reservation.getReservationDate(),
                reservation.getClient() == null ? null : reservation.getClient().getId(),
                reservation.getClient() == null ? null : reservation.getClient().getCode(),
                reservation.getClient() == null ? null : reservation.getClient().getFullName(),
                reservation.getStatus(),
                reservation.getDepositAmount(),
                reservation.isNeedsCare(),
                reservation.getPriority(),
                reservation.isArchived(),
                reservation.getArchivedAt(),
                Set.copyOf(reservation.getTags()),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt(),
                reservation.getVersion());
    }
}
