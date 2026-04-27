package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.model.LeagueTable;
import mark.warren93.dev.DennyWarriorsAPI.model.LeagueTableRow;
import mark.warren93.dev.DennyWarriorsAPI.repository.LeagueTableRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LeagueService {

    private static final String CURRENT_TABLE_KEY = "current";

    private final LeagueTableRepository leagueTableRepository;

    public LeagueService(LeagueTableRepository leagueTableRepository) {
        this.leagueTableRepository = leagueTableRepository;
    }

    public LeagueTable getLeagueTable() {
        return leagueTableRepository.findByKey(CURRENT_TABLE_KEY)
                .orElseGet(this::createDefaultTable);
    }

    public LeagueTable updateLeagueTable(List<LeagueTableRow> rows) {
        LeagueTable table = leagueTableRepository.findByKey(CURRENT_TABLE_KEY)
                .orElseGet(this::createDefaultTable);
        table.setRows(rows == null ? new ArrayList<>() : rows);
        table.setUpdatedAt(LocalDateTime.now());
        return leagueTableRepository.save(table);
    }

    private LeagueTable createDefaultTable() {
        LeagueTable table = new LeagueTable();
        table.setKey(CURRENT_TABLE_KEY);
        table.setLeagueName("Denny Warriors League Table");
        table.setSeason("current");
        table.setRows(new ArrayList<>());
        table.setUpdatedAt(LocalDateTime.now());
        return leagueTableRepository.save(table);
    }
}
