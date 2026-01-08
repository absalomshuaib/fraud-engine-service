package com.fraud.engine.server.fraudEngineService.entity;

import lombok.Data;

import java.time.Instant;
@Data
public class Location {
    private Double latitude;
    private Double longitude;

    private Instant timestamp;

    public Location(Double latitude, Double longitude, Instant timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
