package petstore.support;

import petstore.exception.EntityNotFoundException;
import petstore.exception.Messages;
import petstore.model.Reservation;
import petstore.repository.ReservationRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MemoryReservationRepository implements ReservationRepository {
    private final Map<Long, Reservation> rows = new LinkedHashMap<>();
    private long sequence = 1;

    @Override
    public List<Reservation> findAll() {
        return rows.values().stream()
                .sorted(Comparator.comparingLong(Reservation::getId))
                .map(this::copy)
                .toList();
    }

    @Override
    public Optional<Reservation> findById(long id) {
        Reservation reservation = rows.get(id);
        return reservation == null ? Optional.empty() : Optional.of(copy(reservation));
    }

    @Override
    public long insert(Reservation reservation) {
        long id = sequence++;
        reservation.setId(id);
        rows.put(id, copy(reservation));
        return id;
    }

    @Override
    public void update(Reservation reservation) {
        if (!rows.containsKey(reservation.getId())) {
            throw new EntityNotFoundException(Messages.reservationNotFound(reservation.getId()));
        }
        rows.put(reservation.getId(), copy(reservation));
    }

    @Override
    public void delete(long id) {
        if (rows.remove(id) == null) {
            throw new EntityNotFoundException(Messages.reservationNotFound(id));
        }
    }

    @Override
    public int countByClientId(long clientId) {
        return (int) rows.values().stream().filter(item -> item.getClientId() == clientId).count();
    }

    private Reservation copy(Reservation reservation) {
        return new Reservation(
                reservation.getId(),
                reservation.getClientId(),
                reservation.getAnimalName(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getComment()
        );
    }
}
