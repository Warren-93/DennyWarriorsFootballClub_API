package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.exception.ResourceNotFoundException;
import mark.warren93.dev.DennyWarriorsAPI.model.Player;
import mark.warren93.dev.DennyWarriorsAPI.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class SquadService {

    private final PlayerRepository playerRepository;

    public SquadService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public List<Player> getSquad(Integer limit, String position) {

        Stream<Player> stream = playerRepository.findAll().stream()
                .filter(player -> isBlank(position) || equalsIgnoreCase(player.getPosition(), position))
                .sorted(Comparator.comparing(Player::getPlayerNumber, Comparator.nullsLast(Integer::compareTo)));

        if (limit != null && limit > 0) {
            stream = stream.limit(limit);
        }
        return stream.toList();
    }

    public Player getPlayerById(String id) {
        return playerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }

    public Player createPlayer(Player player) {
        LocalDateTime now = LocalDateTime.now();
        player.setId(null);
        player.setCreatedAt(now);
        player.setUpdatedAt(now);
        return playerRepository.save(player);
    }

    public Player updatePlayer(String id, Player player) {
        Player existing = getPlayerById(id);
        existing.setPlayerId(player.getPlayerId());
        existing.setPlayerNumber(player.getPlayerNumber());
        existing.setPlayerFirstName(player.getPlayerFirstName());
        existing.setPlayerSurename(player.getPlayerSurename());
        existing.setPlayerProfileImage(player.getPlayerProfileImage());
        existing.setPlayerInfoCard(player.getPlayerInfoCard());
        existing.setPosition(player.getPosition());
        existing.setGoals(player.getGoals());
        existing.setAssists(player.getAssists());
        existing.setAppearances(player.getAppearances());
        existing.setBio(player.getBio());
        existing.setCaptain(player.isCaptain());
        existing.setSponsorLogo1(player.getSponsorLogo1());
        existing.setSponsorLogo2(player.getSponsorLogo2());
        existing.setSponsorLogo3(player.getSponsorLogo3());
        existing.setUpdatedAt(LocalDateTime.now());
        return playerRepository.save(existing);
    }

    public void deletePlayer(String id) {
        Player existing = getPlayerById(id);
        playerRepository.delete(existing);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return Objects.equals(normalise(left), normalise(right));
    }

    private String normalise(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
