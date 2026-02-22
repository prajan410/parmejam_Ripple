package com.example.ripple;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseService {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SosPacketDao dao;

    public DatabaseService(Context context) {
        this.dao = RippleDatabase.getInstance(context).sosPacketDao();
    }

    public void upsertPacket(SosPacket incoming) {
        executor.execute(() -> {
            SosPacket existing = dao.getPacketById(incoming.getPacketId());
            if (existing == null || incoming.isNewerThan(existing)) {
                dao.insertPacket(incoming);
            }
        });
    }

    public void getActivePackets(Callback<List<SosPacket>> callback) {
        executor.execute(() -> {
            long cutoff = System.currentTimeMillis() / 1000 - 86400;
            List<SosPacket> packets = dao.getActivePackets(cutoff);
            callback.onResult(packets);
        });
    }

    public void pruneOldPackets() {
        executor.execute(() -> {
            long cutoff = System.currentTimeMillis() / 1000 - 86400;
            dao.pruneOldPackets(cutoff);
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    public void resolvePacket(String packetId) {
        executor.execute(() -> {
            Log.d("DatabaseService", "Resolving packet: " + packetId);
            SosPacket packet = dao.getPacketById(packetId);
            Log.d("DatabaseService", "Found packet: " + (packet != null));
            if (packet != null) {
                packet.setResolved(true);
                packet.setUploaded(false);
                dao.insertPacket(packet);
                UploadService uploadService = new UploadService(this);
                uploadService.uploadPendingPackets();
                Log.d("DatabaseService", "Marked resolved: " + packetId);
            } else {
                Log.w("DatabaseService", "No packet found for ID: " + packetId);
            }
        });
    }




    public void getPendingPackets(Callback<List<SosPacket>> callback) {
        executor.execute(() -> {
            List<SosPacket> packets = dao.getPendingUploadPackets();
            callback.onResult(packets);
        });
    }

    public void markUploaded(String packetId) {
        executor.execute(() -> dao.markAsUploaded(packetId));
    }
}
