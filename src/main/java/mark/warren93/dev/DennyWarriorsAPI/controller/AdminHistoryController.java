package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.model.HistoryEntry;
import mark.warren93.dev.DennyWarriorsAPI.service.HistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/history")
public class AdminHistoryController {

    private final HistoryService historyService;

    public AdminHistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HistoryEntry createEntry(@RequestBody HistoryEntry entry) {
        return historyService.createEntry(entry);
    }

    @PutMapping("/{id}")
    public HistoryEntry updateEntry(@PathVariable String id, @RequestBody HistoryEntry entry) {
        return historyService.updateEntry(id, entry);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntry(@PathVariable String id) {
        historyService.deleteEntry(id);
    }
}
