package de.goafestival.webapp.repository;

import de.goafestival.webapp.domain.Edition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EditionRepository extends JpaRepository<Edition, Long> {

    Optional<Edition> findByCurrentTrue();

    Optional<Edition> findByYear(Integer year);

    List<Edition> findAllByOrderByYearDesc();
}
