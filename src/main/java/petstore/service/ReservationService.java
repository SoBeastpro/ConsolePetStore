package petstore.service;

import petstore.exception.BusinessException;
import petstore.exception.EntityNotFoundException;
import petstore.model.Client;
import petstore.model.Reservation;
import petstore.model.ReservationStatus;
import petstore.repository.EntityRepository;
import petstore.repository.ReservationRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ReservationService {
    private final EntityRepository<Client> clientRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(EntityRepository<Client> clientRepository, ReservationRepository reservationRepository) {
        this.clientRepository = clientRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<Reservation> getAll() throws SQLException {
        return reservationRepository.findAll();
    }

    public Reservation getById(long id) throws SQLException {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заявка с ID " + id + " не найдена."));
    }

    public long create(long clientId, String animalName, LocalDate createdAt, String comment) throws SQLException {
        Reservation reservation = new Reservation(
                clientId,
                normalize(animalName),
                ReservationStatus.CREATED,
                createdAt,
                normalize(comment)
        );
        validate(reservation);
        ensureClientExists(clientId);
        return reservationRepository.insert(reservation);
    }

    public void update(long id, long clientId, String animalName, LocalDate createdAt,
                       String statusText, String comment) throws SQLException {
        Reservation reservation = getById(id);
        ReservationStatus status = parseStatus(statusText);
        checkTransition(reservation.getStatus(), status);
        reservation.setClientId(clientId);
        reservation.setAnimalName(normalize(animalName));
        reservation.setCreatedAt(createdAt);
        reservation.setStatus(status);
        reservation.setComment(normalize(comment));
        validate(reservation);
        ensureClientExists(clientId);
        reservationRepository.update(reservation);
    }

    public void delete(long id) throws SQLException {
        getById(id);
        reservationRepository.delete(id);
    }

    public List<Reservation> searchByAnimalName(String query) throws SQLException {
        String needle = requiredQuery(query);
        return reservationRepository.findAll().stream()
                .filter(item -> item.getAnimalName().toLowerCase(Locale.forLanguageTag("ru")).contains(needle))
                .toList();
    }

    public List<Reservation> searchByClientName(String query) throws SQLException {
        String needle = requiredQuery(query);
        Map<Long, String> names = clientNames();
        return reservationRepository.findAll().stream()
                .filter(item -> names.getOrDefault(item.getClientId(), "")
                        .toLowerCase(Locale.forLanguageTag("ru"))
                        .contains(needle))
                .toList();
    }

    public List<Reservation> filterByStatus(String statusText) throws SQLException {
        ReservationStatus status = parseStatus(statusText);
        return reservationRepository.findAll().stream()
                .filter(item -> item.getStatus() == status)
                .toList();
    }

    public List<Reservation> filterByCreatedAt(LocalDate from, LocalDate to) throws SQLException {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("Нельзя выполнить фильтрацию: некорректный диапазон дат.");
        }
        return reservationRepository.findAll().stream()
                .filter(item -> !item.getCreatedAt().isBefore(from) && !item.getCreatedAt().isAfter(to))
                .toList();
    }

    public List<Reservation> sortedByCreatedAt() throws SQLException {
        return reservationRepository.findAll().stream()
                .sorted(Comparator.comparing(Reservation::getCreatedAt))
                .toList();
    }

    public List<Reservation> sortedByAnimalName() throws SQLException {
        return reservationRepository.findAll().stream()
                .sorted(Comparator.comparing(Reservation::getAnimalName))
                .toList();
    }

    public long count() throws SQLException {
        return reservationRepository.findAll().size();
    }

    public Map<ReservationStatus, Long> countByStatus() throws SQLException {
        return reservationRepository.findAll().stream()
                .collect(Collectors.groupingBy(Reservation::getStatus, Collectors.counting()));
    }

    public ReservationStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Нельзя установить некорректный статус.");
        }
        try {
            return ReservationStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Нельзя установить некорректный статус.");
        }
    }

    private void validate(Reservation reservation) {
        if (reservation.getAnimalName() == null || reservation.getAnimalName().isBlank()
                || reservation.getCreatedAt() == null) {
            throw new BusinessException("Нельзя сохранить заявку без обязательных полей: кличка и дата создания.");
        }
        if (reservation.getStatus() == null) {
            throw new BusinessException("Нельзя установить некорректный статус.");
        }
        if (reservation.getAnimalName().length() > 100
                || (reservation.getComment() != null && reservation.getComment().length() > 500)) {
            throw new BusinessException("Нельзя сохранить заявку: некорректная длина поля.");
        }
    }

    private void ensureClientExists(long clientId) throws SQLException {
        if (clientRepository.findById(clientId).isEmpty()) {
            throw new BusinessException("Нельзя указать несуществующего клиента.");
        }
    }

    private void checkTransition(ReservationStatus current, ReservationStatus next) {
        if (current == next) {
            return;
        }
        boolean allowed = switch (current) {
            case CREATED -> next == ReservationStatus.CONFIRMED || next == ReservationStatus.CANCELLED;
            case CONFIRMED -> next == ReservationStatus.COMPLETED || next == ReservationStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new BusinessException("Нельзя перевести заявку из статуса " + current + " в " + next + ".");
        }
    }

    private String requiredQuery(String query) {
        String needle = normalize(query).toLowerCase(Locale.forLanguageTag("ru"));
        if (needle.isEmpty()) {
            throw new BusinessException("Нельзя выполнить поиск: пустая строка.");
        }
        return needle;
    }

    private Map<Long, String> clientNames() throws SQLException {
        Map<Long, String> names = new HashMap<>();
        for (Client client : clientRepository.findAll()) {
            names.put(client.getId(), client.getFullName());
        }
        return names;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
