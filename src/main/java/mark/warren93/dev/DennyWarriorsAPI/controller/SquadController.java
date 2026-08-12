package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.dto.ApiListResponse;
import mark.warren93.dev.DennyWarriorsAPI.dto.PlayerResponse;
import mark.warren93.dev.DennyWarriorsAPI.service.SquadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/squad")
public class SquadController {

    private final SquadService squadService;

    public SquadController(SquadService squadService) {
        this.squadService = squadService;
    }

    @GetMapping
    public ApiListResponse<PlayerResponse> getSquad(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String position) {
        var players = squadService.getSquad(limit, position).stream()
                .map(PlayerResponse::from)
                .toList();
        return ApiListResponse.of(players);
    }

    @GetMapping("/{id}")
    public PlayerResponse getPlayerById(@PathVariable String id) {
        return PlayerResponse.from(squadService.getPlayerById(id));
    }
}
