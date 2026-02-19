package com.gamelibrary.stats.dto;

public class GameDTO {
    private Long id;

    private String title;
    private String platform;
    private Double playTimeHours;
    private String completionStatus;
    private Double purchasePrice;
    private String genre;
    private String coverUrl;


    public GameDTO() {}

    public GameDTO(Long id,
                   String title,
                   String platform,
                   Double playTimeHours,
                   String completionStatus,
                   Double purchasePrice,
                   String genre) {
        this.id = id;
        this.title = title;
        this.platform = platform;
        this.playTimeHours = playTimeHours;
        this.completionStatus = completionStatus;
        this.purchasePrice = purchasePrice;
        this.genre = genre;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getPlatform() { return platform; }
    public Double getPlayTimeHours() { return playTimeHours; }
    public String getCompletionStatus() { return completionStatus; }
    public Double getPurchasePrice() { return purchasePrice; }
    public String getGenre() { return genre; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setPlatform(String platform) { this.platform = platform; }
    public void setPlayTimeHours(Double playTimeHours) { this.playTimeHours = playTimeHours; }
    public void setCompletionStatus(String completionStatus) { this.completionStatus = completionStatus; }
    public void setPurchasePrice(Double purchasePrice) { this.purchasePrice = purchasePrice; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}
