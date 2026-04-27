package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.dto.LeagueTableUpdateRequest;
import mark.warren93.dev.DennyWarriorsAPI.model.LeagueTable;
import mark.warren93.dev.DennyWarriorsAPI.service.LeagueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/league")
public class LeagueController {

    private final LeagueService leagueService;

    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @GetMapping("/table")
    public LeagueTable getLeagueTable() {
        return leagueService.getLeagueTable();
    }

    @PutMapping("/table")
    public LeagueTable updateLeagueTable(@RequestBody LeagueTableUpdateRequest request) {
        return leagueService.updateLeagueTable(request.rows());
    }
}
