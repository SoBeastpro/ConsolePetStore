package petstore.service;

import petstore.exception.BusinessException;
import petstore.exception.EntityNotFoundException;
import petstore.exception.Messages;
import petstore.model.Client;
import petstore.model.Reservation;
import petstore.model.ReservationQuery;
import petstore.model.ReservationStatus;
import petstore.model.ReservationView;
import petstore.model.SortDirection;
import petstore.model.SortField;
import petstore.repository.EntityRepository;
import petstore.repository.ReservationRepository;
import petstore.util.DateRanges;

import java.sql.SQLException;
import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
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

    public List<ReservationView> list(ReservationQuery query) throws SQLException {
        ReservationQuery safeQuery = query == null ? ReservationQuery.all() : query;
        DateRanges.check(safeQuery.from(), safeQuery.to());
        String needle = normalizeSearch(safeQuery.searchText());
        Map<Long, String> names = clientNames();
        List<ReservationView> rows = new ArrayList<>();
        for (Reservation item : reservationRepository.findAll()) {
            String clientName = names.getOrDefault(item.getClientId(), "");
            if (needle != null && !matches(item, clientName, needle)) {
                continue;
            }
            if (safeQuery.status() != null && item.getStatus() != safeQuery.status()) {
                continue;
            }
            if (safeQuery.from() != null && !inRange(item.getCreatedAt(), safeQuery.from(), safeQuery.to())) {
                continue;
            }
            rows.add(toView(item, clientName));
        }
        rows.sort(comparator(safeQuery.sortField(), safeQuery.sortDirection()));
        return rows;
    }

    public Reservation getById(long id) throws SQLException {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Messages.reservationNotFound(id)));
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
        update(id, clientId, animalName, createdAt, parseStatus(statusText), comment);
    }

    public void update(long id, long clientId, String animalName, LocalDate createdAt,
                       ReservationStatus status, String comment) throws SQLException {
        Reservation reservation = getById(id);
        if (status == null) {
            throw new BusinessException(Messages.STATUS_INVALID);
        }
        ReservationStatus current = reservation.getStatus();
        reservation.setClientId(clientId);
        reservation.setAnimalName(normalize(animalName));
        reservation.setCreatedAt(createdAt);
        reservation.setStatus(status);
        reservation.setComment(normalize(comment));
        validate(reservation);
        checkTransition(current, status);
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
        if (from == null || to == null) {
            throw new BusinessException(Messages.DATE_RANGE_INCOMPLETE);
        }
        if (from.isAfter(to)) {
            throw new BusinessException(Messages.DATE_RANGE_INVALID);
        }
        return reservationRepository.findAll().stream()
                .filter(item -> inRange(item.getCreatedAt(), from, to))
                .toList();
    }

    public List<Reservation> sortedByCreatedAt() throws SQLException {
        return reservationRepository.findAll().stream()
                .sorted(Comparator.comparing(Reservation::getCreatedAt).thenComparingLong(Reservation::getId))
                .toList();
    }

    public List<Reservation> sortedByAnimalName() throws SQLException {
        Collator collator = russianCollator();
        return reservationRepository.findAll().stream()
                .sorted(Comparator.comparing(Reservation::getAnimalName, Comparator.nullsFirst(collator))
                        .thenComparingLong(Reservation::getId))
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
            throw new BusinessException(Messages.STATUS_INVALID);
        }
        try {
            return ReservationStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(Messages.STATUS_INVALID);
        }
    }

    private void validate(Reservation reservation) {
        if (reservation.getAnimalName() == null || reservation.getAnimalName().isBlank()) {
            throw new BusinessException(Messages.ANIMAL_NAME_REQUIRED);
        }
        if (reservation.getAnimalName().length() > 100) {
            throw new BusinessException(Messages.ANIMAL_NAME_LENGTH);
        }
        if (reservation.getCreatedAt() == null) {
            throw new BusinessException(Messages.DATE_REQUIRED);
        }
        if (reservation.getComment() != null && reservation.getComment().length() > 500) {
            throw new BusinessException(Messages.COMMENT_LENGTH);
        }
        if (reservation.getStatus() == null) {
            throw new BusinessException(Messages.STATUS_INVALID);
        }
    }

    private void ensureClientExists(long clientId) throws SQLException {
        if (clientRepository.findById(clientId).isEmpty()) {
            throw new BusinessException(Messages.CLIENT_MISSING);
        }
    }

    private void checkTransition(ReservationStatus current, ReservationStatus next) {
        if (current.allowedTargets().contains(next)) {
            return;
        }
        throw new BusinessException(Messages.statusTransition(current.title(), next.title()));
    }

    private String requiredQuery(String query) {
        String needle = normalize(query).toLowerCase(Locale.forLanguageTag("ru"));
        if (needle.isEmpty()) {
            throw new BusinessException(Messages.SEARCH_EMPTY);
        }
        return needle;
    }

    private String normalizeSearch(String searchText) {
        if (searchText == null) {
            return null;
        }
        String needle = searchText.trim().toLowerCase(Locale.forLanguageTag("ru"));
        if (needle.isEmpty()) {
            throw new BusinessException(Messages.SEARCH_EMPTY);
        }
        return needle;
    }

    private boolean matches(Reservation item, String clientName, String needle) {
        Locale locale = Locale.forLanguageTag("ru");
        String animal = item.getAnimalName() == null ? "" : item.getAnimalName().toLowerCase(locale);
        String comment = item.getComment() == null ? "" : item.getComment().toLowerCase(locale);
        String client = clientName == null ? "" : clientName.toLowerCase(locale);
        return animal.contains(needle) || comment.contains(needle) || client.contains(needle);
    }

    private boolean inRange(LocalDate createdAt, LocalDate from, LocalDate to) {
        return !createdAt.isBefore(from) && !createdAt.isAfter(to);
    }

    private Comparator<ReservationView> comparator(SortField field, SortDirection direction) {
        if (field == null) {
            return Comparator.comparingLong(ReservationView::id);
        }
        Comparator<ReservationView> primary = switch (field) {
            case CREATED_AT -> Comparator.comparing(ReservationView::createdAt, Comparator.nullsFirst(Comparator.naturalOrder()));
            case ANIMAL_NAME -> Comparator.comparing(ReservationView::animalName, Comparator.nullsFirst(russianCollator()));
        };
        if (direction == SortDirection.DESC) {
            primary = primary.reversed();
        }
        return primary.thenComparingLong(ReservationView::id);
    }

    private ReservationView toView(Reservation item, String clientName) {
        String comment = item.getComment() == null ? "" : item.getComment();
        return new ReservationView(
                item.getId(),
                item.getClientId(),
                clientName,
                item.getAnimalName(),
                item.getStatus(),
                item.getCreatedAt(),
                comment
        );
    }

    private Map<Long, String> clientNames() throws SQLException {
        Map<Long, String> names = new HashMap<>();
        for (Client client : clientRepository.findAll()) {
            names.put(client.getId(), client.getFullName());
        }
        return names;
    }

    private Collator russianCollator() {
        Collator collator = Collator.getInstance(Locale.forLanguageTag("ru"));
        collator.setStrength(Collator.PRIMARY);
        return collator;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
