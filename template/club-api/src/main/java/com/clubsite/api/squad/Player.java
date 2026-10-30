package com.clubsite.api.squad;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A squad member. Positions are free text so the same model serves any
 * sport; the frontend's club.config.js lists the positions it offers.
 */
@Data
@NoArgsConstructor
@Document(collection = "players")
public class Player {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private Integer number;
    private String position;
    private String nationality;
    private String profileImageUrl;
    private String infoCardImageUrl;
    private Integer appearances;
    private Integer goals;
    private Integer assists;
    private String bio;
    private boolean captain;
    /** Up to three sponsor logos shown on the player's card. */
    private List<String> sponsorLogoUrls = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
