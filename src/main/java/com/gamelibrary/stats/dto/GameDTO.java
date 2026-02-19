package com.gamelibrary.stats.dto;

public class GameDTO {
    private Long id;
    private String title;
    private String genre;
    private String status;
    private String launcherName;
    private Double purchasePrice;

    public GameDTO() {}

    public GameDTO(Long id, String title, String genre, String status, String launcherName, Double purchasePrice) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.status = status;
        this.launcherName = launcherName;
        this.purchasePrice = purchasePrice;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public String getStatus() { return status; }
    public String getLauncherName() { return launcherName; }
    public Double getPurchasePrice() { return purchasePrice; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setStatus(String status) { this.status = status; }
    public void setLauncherName(String launcherName) { this.launcherName = launcherName; }
    public void setPurchasePrice(Double purchasePrice) { this.purchasePrice = purchasePrice; }
}
