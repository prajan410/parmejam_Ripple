
package com.example.ripple;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SosPacketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPacket(SosPacket packet);

    @Query("SELECT * FROM packets WHERE timestamp > :cutoff")
    List<SosPacket>getActivePackets(long cutoff);

    @Query("SELECT * FROM packets WHERE packetId = :packetId LIMIT 1")
    SosPacket getPacketById(String packetId);

    @Query("DELETE FROM packets WHERE timestamp < :cutoff")
    void pruneOldPackets(long cutoff);

    @Query("SELECT * FROM packets")
    List<SosPacket> getAllPackets();

    @Query("SELECT * FROM packets WHERE uploaded = 0")
    List<SosPacket> getPendingUploadPackets();

    @Query("UPDATE packets SET uploaded = 1 WHERE packetId = :packetId")
    void markAsUploaded(String packetId);


}
