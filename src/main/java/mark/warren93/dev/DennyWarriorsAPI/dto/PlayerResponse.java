package mark.warren93.dev.DennyWarriorsAPI.dto;

import mark.warren93.dev.DennyWarriorsAPI.model.Player;

import java.util.List;
import java.util.stream.Stream;

public record PlayerResponse(
        String id,
        String name,
        Integer number,
        String position,
        Integer age,
        String nationality,
        String profileImage,
        String infoCard,
        Integer goals,
        Integer assists,
        Integer appearances,
        String bio,
        boolean captain,
        List<String> sponsorLogos) {

    public static PlayerResponse from(Player player) {
        String name = (nullToEmpty(player.getPlayerFirstName()) + " " + nullToEmpty(player.getPlayerSurename())).trim();
        List<String> sponsorLogos = Stream.of(player.getSponsorLogo1(), player.getSponsorLogo2(), player.getSponsorLogo3())
                .filter(url -> url != null && !url.isBlank())
                .toList();

        return new PlayerResponse(
                player.getId(),
                name.isEmpty() ? "Unnamed Player" : name,
                player.getPlayerNumber(),
                player.getPosition(),
                player.getPlayerAge(),
                player.getPlayerNationality(),
                player.getPlayerProfileImage(),
                player.getPlayerInfoCard(),
                player.getGoals(),
                player.getAssists(),
                player.getAppearances(),
                player.getBio(),
                player.isCaptain(),
                sponsorLogos);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
