package ru.mirea.petstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.petstore.domain.Reservation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID>, JpaSpecificationExecutor<Reservation> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
    Optional<Reservation> findByCodeIgnoreCase(String code);
    long countByArchivedFalse();
    long countByArchivedTrue();
    long countByArchivedFalseAndNeedsCareTrue();

    @Query("select coalesce(avg(r.depositAmount), 0) from Reservation r where r.archived = false")
    BigDecimal averageDepositForActive();

    @Query("select c.fullName, count(r) from Reservation r join r.client c " +
            "where r.archived = false group by c.fullName order by count(r) desc")
    List<Object[]> countActiveByClient();

    @Query("select r.status, count(r) from Reservation r where r.archived = false group by r.status order by count(r) desc")
    List<Object[]> countActiveByStatus();

    @Query("select r.priority, count(r) from Reservation r where r.archived = false group by r.priority order by count(r) desc")
    List<Object[]> countActiveByPriority();
}
