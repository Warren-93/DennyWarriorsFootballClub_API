package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.exception.ResourceNotFoundException;
import mark.warren93.dev.DennyWarriorsAPI.model.HistoryEntry;
import mark.warren93.dev.DennyWarriorsAPI.model.HistoryEntryType;
import mark.warren93.dev.DennyWarriorsAPI.repository.HistoryEntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class HistoryService {

    private final HistoryEntryRepository historyEntryRepository;

    public HistoryService(HistoryEntryRepository historyEntryRepository) {
        this.historyEntryRepository = historyEntryRepository;
    }

    public List<HistoryEntry> getEntries(HistoryEntryType type) {
        return historyEntryRepository.findAll().stream()
                .filter(entry -> type == null || type == entry.getType())
                .sorted(Comparator.comparingInt(HistoryEntry::getOrder)
                        .thenComparing(HistoryEntry::getYear, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    public HistoryEntry getEntryById(String id) {
        return historyEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("History entry not found: " + id));
    }

    public HistoryEntry createEntry(HistoryEntry entry) {
        entry.setId(null);
        entry.setCreatedAt(LocalDateTime.now());
        return historyEntryRepository.save(entry);
    }

    public HistoryEntry updateEntry(String id, HistoryEntry entry) {
        HistoryEntry existing = getEntryById(id);
        existing.setType(entry.getType());
        existing.setYear(entry.getYear());
        existing.setTitle(entry.getTitle());
        existing.setDescription(entry.getDescription());
        existing.setImageUrl(entry.getImageUrl());
        existing.setOrder(entry.getOrder());
        return historyEntryRepository.save(existing);
    }

    public void deleteEntry(String id) {
        HistoryEntry existing = getEntryById(id);
        historyEntryRepository.delete(existing);
    }
}
