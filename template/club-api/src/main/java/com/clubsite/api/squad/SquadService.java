package com.clubsite.api.squad;

import com.clubsite.api.common.ApiException;
import com.clubsite.api.common.Text;
import com.clubsite.api.squad.PlayerDtos.PlayerRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SquadService {

    /** Squad number first (unnumbered players last), then surname. */
    private static final Comparator<Player> SQUAD_ORDER =
            Comparator.comparing(Player::getNumber, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(Player::getLastName, Comparator.nullsLast(String::compareToIgnoreCase));

    private final PlayerRepository repository;

    public SquadService(PlayerRepository repository) {
        this.repository = repository;
    }

    /**
     * Sorted in memory: a squad is a few dozen players, and MongoDB sorts
     * nulls first, which would put unnumbered players at the top.
     */
    public List<Player> list(String position) {
        return repository.findAll().stream()
                .filter(p -> Text.isBlank(position) || Text.sameText(p.getPosition(), position))
                .sorted(SQUAD_ORDER)
                .toList();
    }

    public Player getById(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Player", id));
    }

    public Player create(PlayerRequest request) {
        Player player = new Player();
        player.setCreatedAt(Instant.now());
        return save(player, request);
    }

    public Player update(String id, PlayerRequest request) {
        return save(getById(id), request);
    }

    public void delete(String id) {
        repository.delete(getById(id));
    }

    private Player save(Player player, PlayerRequest r) {
        player.setFirstName(r.firstName().trim());
        player.setLastName(r.lastName().trim());
        player.setNumber(r.number());
        player.setPosition(r.position().trim());
        player.setNationality(r.nationality());
        player.setProfileImageUrl(r.profileImageUrl());
        player.setInfoCardImageUrl(r.infoCardImageUrl());
        player.setAppearances(r.appearances());
        player.setGoals(r.goals());
        player.setAssists(r.assists());
        player.setBio(r.bio());
        player.setCaptain(r.captain());
        player.setSponsorLogoUrls(r.sponsorLogoUrls() == null ? new ArrayList<>() : new ArrayList<>(r.sponsorLogoUrls()));
        player.setUpdatedAt(Instant.now());
        return repository.save(player);
    }
}
