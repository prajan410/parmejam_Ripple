package com.example.ripple;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadService {

    private static final String TAG = "Ripple";
    private final DatabaseService db;
    private final FirebaseFirestore firestore;
    private boolean isUploading = false;

    public UploadService(DatabaseService db) {
        this.db = db;
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void uploadPendingPackets() {
        if (isUploading) return;
        isUploading = true;

        db.getPendingPackets(packets -> {
            if (packets.isEmpty()) {
                Log.d(TAG, "No pending packets to upload");
                return;
            }
            Log.d(TAG, "Uploading " + packets.size() + " pending packets");
            for (SosPacket packet : packets) {
                uploadPacket(packet);
            }
        });
    }

    private void uploadPacket(SosPacket packet) {
        Map<String, Object> data = new HashMap<>();
        data.put("packetId",       packet.getPacketId());
        data.put("userId",         packet.getUserId());
        data.put("statusCode",     packet.getStatusCode());
        data.put("lat",            packet.getLat());
        data.put("lng",            packet.getLng());
        data.put("timestamp",      packet.getTimestamp());
        data.put("hopCount",       packet.getHopCount());
        data.put("sequenceNumber", packet.getSequenceNumber());
        data.put("resolved", packet.isResolved());


        firestore.collection("sos_signals")
                .document(packet.getPacketId())
                .set(data)
                .addOnSuccessListener(unused -> {
                    db.markUploaded(packet.getPacketId());
                    Log.d(TAG, "Uploaded to Firestore: " + packet.getPacketId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload error for packet " + packet.getPacketId(), e);
                });
    }
}
