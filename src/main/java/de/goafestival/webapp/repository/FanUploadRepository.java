package de.goafestival.webapp.repository;

import de.goafestival.webapp.domain.FanUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FanUploadRepository extends JpaRepository<FanUpload, Long> {

    // join fetch the edition explicitly - the admin listing always displays its title/year,
    // and the lazy association would otherwise throw outside the (already-closed) service
    // transaction (open-in-view is off).
    @Query("select u from FanUpload u join fetch u.edition where u.edition.id = :editionId order by u.uploadedAt desc")
    List<FanUpload> findByEditionIdOrderByUploadedAtDesc(@Param("editionId") Long editionId);

    @Query("select u from FanUpload u join fetch u.edition order by u.uploadedAt desc")
    List<FanUpload> findAllByOrderByUploadedAtDesc();
}
