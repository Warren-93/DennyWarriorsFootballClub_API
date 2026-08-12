package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.dto.ApiListResponse;
import mark.warren93.dev.DennyWarriorsAPI.model.Player;
import mark.warren93.dev.DennyWarriorsAPI.service.SquadService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/squad")
public class AdminSquadController {

    private final SquadService squadService;

    public AdminSquadController(SquadService squadService) {
        this.squadService = squadService;
    }

    /** Raw model fields (not the public PlayerResponse aliases) — for the admin editor form. */
    @GetMapping
    public ApiListResponse<Player> getAllPlayers() {
        return ApiListResponse.of(squadService.getSquad(null, null));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Player createPlayer(@RequestBody Player player) {
        return squadService.createPlayer(player);
    }

    @PutMapping("/{id}")
    public Player updatePlayer(@PathVariable String id, @RequestBody Player player) {
        return squadService.updatePlayer(id, player);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlayer(@PathVariable String id) {
        squadService.deletePlayer(id);
    }
}
