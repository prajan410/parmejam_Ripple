package com.example.ripple;

import java.io.Serializable;
import java.util.UUID;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;

@Entity(tableName = "packets")
public class SosPacket implements Serializable {

    public static final int STATUS_CRITICAL = 1;
    public static final int STATUS_INJURED  = 2;
    public static final int STATUS_HELP     = 3;

    @PrimaryKey
    @NonNull
    private String packetId;
    @ColumnInfo(name = "user_id")
    private String userId;
    @ColumnInfo(name = "status_code")
    private int statusCode;
    private double lat;
    private double lng;
    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "sequence_number")
    private int sequenceNumber;
    @ColumnInfo(name = "hop_count")
    private int hopCount;

    @ColumnInfo(name = "uploaded")
    private boolean uploaded;
    @ColumnInfo(name = "resolved")
    private boolean resolved = false;


    @Ignore
    public SosPacket(String userId, int statusCode, double lat, double lng) {
        this.packetId      = UUID.randomUUID().toString();
        this.userId        = userId;
        this.statusCode    = statusCode;
        this.lat           = lat;
        this.lng           = lng;
        this.timestamp     = System.currentTimeMillis() / 1000;
        this.sequenceNumber = 0;
        this.hopCount      = 0;
        this.uploaded = false;
    }

    public SosPacket(String packetId, String userId, int statusCode,
                     double lat, double lng, long timestamp,
                     int sequenceNumber, int hopCount, boolean uploaded, boolean resolved) {
        this.packetId       = packetId;
        this.userId         = userId;
        this.statusCode     = statusCode;
        this.lat            = lat;
        this.lng            = lng;
        this.timestamp      = timestamp;
        this.sequenceNumber = sequenceNumber;
        this.hopCount       = hopCount;
        this.uploaded = uploaded;
        this.resolved = resolved;
    }

    public void updateLocation(double newLat, double newLng) {
        this.lat = newLat;
        this.lng = newLng;
        this.sequenceNumber++;
        this.timestamp = System.currentTimeMillis() / 1000;
    }

    public boolean isExpired() {
        long now = System.currentTimeMillis() / 1000;
        return (now - timestamp) > 86400;
    }

    public void incrementHop() {
        this.hopCount++;
    }

    public boolean isNewerThan(SosPacket other) {
        return this.sequenceNumber > other.sequenceNumber;
    }

    public String getPacketId()      { return packetId; }
    public String getUserId()        { return userId; }
    public int getStatusCode()       { return statusCode; }
    public double getLat()           { return lat; }
    public double getLng()           { return lng; }
    public long getTimestamp()       { return timestamp; }
    public int getSequenceNumber()   { return sequenceNumber; }
    public int getHopCount()         { return hopCount; }
    public boolean isUploaded() { return uploaded; }
    public boolean isResolved() { return resolved; }


    public void setPacketId(String packetId)          { this.packetId = packetId; }
    public void setUserId(String userId)              { this.userId = userId; }
    public void setStatusCode(int statusCode)         { this.statusCode = statusCode; }
    public void setLat(double lat)                    { this.lat = lat; }
    public void setLng(double lng)                    { this.lng = lng; }
    public void setTimestamp(long timestamp)          { this.timestamp = timestamp; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public void setHopCount(int hopCount)             { this.hopCount = hopCount; }
    public void setUploaded(boolean uploaded) { this.uploaded = uploaded; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }


    @Override
    public String toString() {
        return "SosPacket{" +
                "id=" + packetId +
                ", status=" + statusCode +
                ", lat=" + lat +
                ", lng=" + lng +
                ", hops=" + hopCount +
                ", seq=" + sequenceNumber +
                "}";
    }
}
