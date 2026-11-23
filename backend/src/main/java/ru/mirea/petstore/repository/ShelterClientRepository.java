package ru.mirea.petstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.petstore.domain.ShelterClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShelterClientRepository extends JpaRepository<ShelterClient, UUID> {
    List<ShelterClient> findAllByOrderByFullNameAsc();
    List<ShelterClient> findByActiveTrueOrderByFullNameAsc();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByPhone(String phone);
    boolean existsByEmailIgnoreCase(String email);
    Optional<ShelterClient> findByCodeIgnoreCase(String code);
}
