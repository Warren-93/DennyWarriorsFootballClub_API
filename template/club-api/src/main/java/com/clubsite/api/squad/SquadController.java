package com.clubsite.api.squad;

import com.clubsite.api.common.ApiListResponse;
import com.clubsite.api.squad.PlayerDtos.PlayerRequest;
import com.clubsite.api.squad.PlayerDtos.PlayerResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SquadController {

    private final SquadService squadService;

    public SquadController(SquadService squadService) {
        this.squadService = squadService;
    }

    // --- Public ------------------------------------------------------------

    @GetMapping({"/squad", "/admin/squad"})
    public ApiListResponse<PlayerResponse> list(@RequestParam(required = false) String position) {
        return ApiListResponse.of(squadService.list(position).stream().map(PlayerResponse::from).toList());
    }

    @GetMapping("/squad/{id}")
    public PlayerResponse get(@PathVariable String id) {
        return PlayerResponse.from(squadService.getById(id));
    }

    // --- Admin -------------------------------------------------------------

    @PostMapping("/admin/squad")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse create(@Valid @RequestBody PlayerRequest request) {
        return PlayerResponse.from(squadService.create(request));
    }

    @PutMapping("/admin/squad/{id}")
    public PlayerResponse update(@PathVariable String id, @Valid @RequestBody PlayerRequest request) {
        return PlayerResponse.from(squadService.update(id, request));
    }

    @DeleteMapping("/admin/squad/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        squadService.delete(id);
    }
}
