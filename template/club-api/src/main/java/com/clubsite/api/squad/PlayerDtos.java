package com.clubsite.api.squad;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class PlayerDtos {

    private PlayerDtos() {
    }

    public record PlayerRequest(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName,
            @Min(0) Integer number,
            @NotBlank @Size(max = 40) String position,
            @Size(max = 60) String nationality,
            String profileImageUrl,
            String infoCardImageUrl,
            @Min(0) Integer appearances,
            @Min(0) Integer goals,
            @Min(0) Integer assists,
            @Size(max = 2000) String bio,
            boolean captain,
            @Size(max = 3) List<@NotBlank String> sponsorLogoUrls) {
    }

    /** Same fields as the model plus a ready-to-display name. */
    public record PlayerResponse(
            String id,
            String name,
            String firstName,
            String lastName,
            Integer number,
            String position,
            String nationality,
            String profileImageUrl,
            String infoCardImageUrl,
            Integer appearances,
            Integer goals,
            Integer assists,
            String bio,
            boolean captain,
            List<String> sponsorLogoUrls) {

        public static PlayerResponse from(Player p) {
            String name = ((p.getFirstName() == null ? "" : p.getFirstName()) + " "
                    + (p.getLastName() == null ? "" : p.getLastName())).trim();
            return new PlayerResponse(
                    p.getId(),
                    name,
                    p.getFirstName(),
                    p.getLastName(),
                    p.getNumber(),
                    p.getPosition(),
                    p.getNationality(),
                    p.getProfileImageUrl(),
                    p.getInfoCardImageUrl(),
                    p.getAppearances(),
                    p.getGoals(),
                    p.getAssists(),
                    p.getBio(),
                    p.isCaptain(),
                    p.getSponsorLogoUrls() == null ? List.of() : p.getSponsorLogoUrls());
        }
    }
}
