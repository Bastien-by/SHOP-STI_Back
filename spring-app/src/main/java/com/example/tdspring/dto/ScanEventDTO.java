package com.example.tdspring.dto;

/**
 * Payload poussé aux clients Angular via WebSocket (/topic/scan)
 * quand une valeur est lue sur la douchette câblée au PLC.
 */
public class ScanEventDTO {

    private boolean success;
    private String value;
    private long timestamp;

    public ScanEventDTO() {
    }

    public ScanEventDTO(boolean success, String value, long timestamp) {
        this.success = success;
        this.value = value;
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
