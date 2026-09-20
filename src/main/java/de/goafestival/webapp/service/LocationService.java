package de.goafestival.webapp.service;

import de.goafestival.webapp.domain.Location;
import de.goafestival.webapp.dto.LocationForm;
import de.goafestival.webapp.repository.EditionRepository;
import de.goafestival.webapp.repository.LocationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class LocationService {

    private final LocationRepository locationRepository;
    private final EditionRepository editionRepository;

    public LocationService(LocationRepository locationRepository, EditionRepository editionRepository) {
        this.locationRepository = locationRepository;
        this.editionRepository = editionRepository;
    }

    public List<Location> findAllOrdered() {
        return locationRepository.findAllByOrderByNameAsc();
    }

    public Location getByIdOrThrow(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location " + id + " wurde nicht gefunden."));
    }

    public Location create(LocationForm form) {
        Location location = new Location();
        applyForm(location, form);
        return locationRepository.save(location);
    }

    public Location update(Long id, LocationForm form) {
        Location location = getByIdOrThrow(id);
        applyForm(location, form);
        return locationRepository.save(location);
    }

    /** Refuses to delete a location that's still used by at least one edition. */
    public void delete(Long id) {
        Location location = getByIdOrThrow(id);
        if (editionRepository.existsByLocation(location)) {
            throw new IllegalStateException(
                    "Location \"" + location.getName() + "\" wird noch von mindestens einer Ausgabe verwendet und kann nicht gelöscht werden.");
        }
        locationRepository.delete(location);
    }

    private void applyForm(Location location, LocationForm form) {
        location.setName(form.getName());
        location.setStreet(form.getStreet());
        location.setZipCity(form.getZipCity());
    }
}
