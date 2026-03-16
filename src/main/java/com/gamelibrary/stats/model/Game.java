 package com.gamelibrary.stats.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "games")
@Data
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String genre;
    private String status;

    private String ageRating;

    @Column(length = 1000)
    private String developers;

    @Column(length = 1000)
    private String publishers;

    @Column(length = 1000)
    private String genres;

    private java.time.LocalDateTime lastPlayed;

    private String completionStatus;

    private Integer timePlayed;

    private Double communityScore;

    @Column(length = 500)
    private String coverUrl;

    @Column(name = "purchase_price")
    private Double purchasePrice;

    // Relationship to Launcher (One-to-Many side)
    @ManyToOne
    @JoinColumn(name = "launcher_id")
    private Launcher launcher;

    // Relationship to User (Security requirement)
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

}

