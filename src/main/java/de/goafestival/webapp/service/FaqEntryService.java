package de.goafestival.webapp.service;

import de.goafestival.webapp.domain.Edition;
import de.goafestival.webapp.domain.FaqEntry;
import de.goafestival.webapp.dto.FaqEntryForm;
import de.goafestival.webapp.repository.FaqEntryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class FaqEntryService {

    private final FaqEntryRepository faqEntryRepository;
    private final EditionService editionService;

    public FaqEntryService(FaqEntryRepository faqEntryRepository, EditionService editionService) {
        this.faqEntryRepository = faqEntryRepository;
        this.editionService = editionService;
    }

    public FaqEntry getByIdOrThrow(Long id) {
        return faqEntryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("FAQ-Eintrag " + id + " wurde nicht gefunden."));
    }

    public List<FaqEntry> findByEdition(Long editionId) {
        return faqEntryRepository.findByEditionIdOrderBySortOrderAsc(editionId);
    }

    public FaqEntry create(FaqEntryForm form) {
        FaqEntry entry = new FaqEntry();
        Edition edition = editionService.getByIdOrThrow(form.getEditionId());
        entry.setEdition(edition);
        applyForm(entry, form);
        return faqEntryRepository.save(entry);
    }

    public FaqEntry update(Long id, FaqEntryForm form) {
        FaqEntry entry = getByIdOrThrow(id);
        applyForm(entry, form);
        return faqEntryRepository.save(entry);
    }

    public void delete(Long id) {
        faqEntryRepository.delete(getByIdOrThrow(id));
    }

    /** Appends a copy of every FAQ entry from another edition after this edition's existing ones. */
    public void copyAllFromEdition(Long targetEditionId, Long sourceEditionId) {
        Edition target = editionService.getByIdOrThrow(targetEditionId);
        List<FaqEntry> existing = findByEdition(targetEditionId);
        int nextOrder = existing.stream().mapToInt(FaqEntry::getSortOrder).max().orElse(-1) + 1;
        for (FaqEntry source : findByEdition(sourceEditionId)) {
            FaqEntry copy = new FaqEntry();
            copy.setEdition(target);
            copy.setQuestion(source.getQuestion());
            copy.setAnswer(source.getAnswer());
            copy.setSortOrder(nextOrder++);
            faqEntryRepository.save(copy);
        }
    }

    private void applyForm(FaqEntry entry, FaqEntryForm form) {
        entry.setQuestion(form.getQuestion());
        entry.setAnswer(form.getAnswer());
        entry.setSortOrder(form.getSortOrder());
    }
}
