package de.goafestival.webapp.repository;

import de.goafestival.webapp.domain.Band;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BandRepository extends JpaRepository<Band, Long> {

    List<Band> findByEditionIdOrderByPerformanceAtAsc(Long editionId);

    @Query("select b from Band b join fetch b.edition left join fetch b.galleryImages where b.id = :id")
    Optional<Band> findByIdWithEdition(Long id);

    @Query("select distinct b from Band b left join fetch b.galleryImages where b.id = :id")
    Optional<Band> findByIdWithGallery(Long id);
}
