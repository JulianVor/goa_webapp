package de.goafestival.webapp.service;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.domain.FanMediaType;
import de.goafestival.webapp.domain.FanUpload;
import de.goafestival.webapp.repository.FanUploadRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class FanUploadService {

    private final FanUploadRepository fanUploadRepository;
    private final FanUploadStorageService storageService;

    public FanUploadService(FanUploadRepository fanUploadRepository, FanUploadStorageService storageService) {
        this.fanUploadRepository = fanUploadRepository;
        this.storageService = storageService;
    }

    public List<FanUpload> findByEdition(Long editionId) {
        return fanUploadRepository.findByEditionIdOrderByUploadedAtDesc(editionId);
    }

    public List<FanUpload> findAll() {
        return fanUploadRepository.findAllByOrderByUploadedAtDesc();
    }

    public FanUpload getByIdOrThrow(Long id) {
        return fanUploadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Upload " + id + " wurde nicht gefunden."));
    }

    /**
     * Stores every file that's actually a photo or video, silently skipping anything
     * else (wrong type, or an empty/missing form slot) rather than failing the whole
     * submission over one bad file. Returns how many were stored.
     */
    public int storeAll(Edition edition, List<MultipartFile> files) {
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            FanMediaType mediaType = classify(file.getContentType());
            if (mediaType == null) {
                continue;
            }
            String storedFilename = storageService.store(file);
            if (storedFilename == null) {
                continue;
            }
            FanUpload upload = new FanUpload();
            upload.setEdition(edition);
            upload.setMediaType(mediaType);
            upload.setOriginalFilename(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : storedFilename);
            upload.setStoredPath(storedFilename);
            upload.setContentType(file.getContentType());
            upload.setFileSizeBytes(file.getSize());
            fanUploadRepository.save(upload);
            count++;
        }
        return count;
    }

    public void delete(Long id) {
        FanUpload upload = getByIdOrThrow(id);
        storageService.delete(upload.getStoredPath());
        fanUploadRepository.delete(upload);
    }

    /** Deletes every upload (file + row) belonging to an edition - call before deleting the edition itself. */
    public void deleteForEdition(Long editionId) {
        List<FanUpload> uploads = fanUploadRepository.findByEditionIdOrderByUploadedAtDesc(editionId);
        uploads.forEach(upload -> storageService.delete(upload.getStoredPath()));
        fanUploadRepository.deleteAll(uploads);
    }

    private FanMediaType classify(String contentType) {
        if (contentType == null) {
            return null;
        }
        if (contentType.startsWith("image/")) {
            return FanMediaType.PHOTO;
        }
        if (contentType.startsWith("video/")) {
            return FanMediaType.VIDEO;
        }
        return null;
    }
}
