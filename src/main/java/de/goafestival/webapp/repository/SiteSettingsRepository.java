package de.goafestival.webapp.repository;

import de.goafestival.webapp.domain.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings, Long> {
}
