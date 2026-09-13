package de.goafestival.webapp.repository;

import de.goafestival.webapp.domain.FaqEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqEntryRepository extends JpaRepository<FaqEntry, Long> {

    List<FaqEntry> findByEditionIdOrderBySortOrderAsc(Long editionId);
}
