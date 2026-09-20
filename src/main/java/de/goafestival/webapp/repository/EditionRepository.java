package de.goafestival.webapp.repository;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.domain.EditionType;
import de.goafestival.webapp.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EditionRepository extends JpaRepository<Edition, Long> {

    Optional<Edition> findByCurrentTrue();

    Optional<Edition> findByYearAndType(Integer year, EditionType type);

    List<Edition> findAllByOrderByYearDesc();

    List<Edition> findByTypeOrderByStartDateDesc(EditionType type);

    boolean existsByLocation(Location location);
}
