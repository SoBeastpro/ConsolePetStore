package ru.mirea.petstore.filter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.petstore.domain.Reservation;
import ru.mirea.petstore.domain.ShelterClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Параметризованная JPA Specification. Значения фильтра передаются как параметры Criteria API. */
public final class ReservationSpecifications {
    private ReservationSpecifications() {
    }

    public static Specification<Reservation> from(ReservationFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (filter.archived() != null) predicates.add(cb.equal(root.get("archived"), filter.archived()));
            if (filter.status() != null) predicates.add(cb.equal(root.get("status"), filter.status()));
            if (filter.needsCare() != null) predicates.add(cb.equal(root.get("needsCare"), filter.needsCare()));
            if (filter.priority() != null) predicates.add(cb.equal(root.get("priority"), filter.priority()));

            Join<Reservation, ShelterClient> clientJoin = root.join("client", JoinType.LEFT);
            if (filter.client() != null) {
                String like = "%" + escape(filter.client()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(clientJoin.get("code")), like, '\\'),
                        cb.like(cb.lower(clientJoin.get("fullName")), like, '\\')));
            }
            if (filter.tag() != null) {
                Join<Reservation, String> tagJoin = root.joinSet("tags", JoinType.LEFT);
                predicates.add(cb.equal(cb.lower(tagJoin), filter.tag().toLowerCase(Locale.ROOT)));
            }
            if (filter.freeText() != null && !filter.freeText().isBlank()) {
                String like = "%" + escape(filter.freeText()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("animalName")), like, '\\'),
                        cb.like(cb.lower(root.get("comment")), like, '\\'),
                        cb.like(cb.lower(root.get("code")), like, '\\'),
                        cb.like(cb.lower(clientJoin.get("fullName")), like, '\\'),
                        cb.like(cb.lower(clientJoin.get("code")), like, '\\')));
            }
            filter.deposits().forEach(constraint -> predicates.add(switch (constraint.operator()) {
                case LT -> cb.lessThan(root.get("depositAmount"), constraint.value());
                case LTE -> cb.lessThanOrEqualTo(root.get("depositAmount"), constraint.value());
                case EQ -> cb.equal(root.get("depositAmount"), constraint.value());
                case GTE -> cb.greaterThanOrEqualTo(root.get("depositAmount"), constraint.value());
                case GT -> cb.greaterThan(root.get("depositAmount"), constraint.value());
            }));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escape(String value) {
        return value.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
