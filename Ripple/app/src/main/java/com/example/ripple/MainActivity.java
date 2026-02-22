package com.example.ripple;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.BroadcastReceiver;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int BT_PERMISSION_CODE = 100;

    private int selectedStatus = SosPacket.STATUS_CRITICAL;
    private boolean isEmergencyActive = false;

    private Button btnCritical, btnInjured, btnHelp, btnSOS;
    private TextView statusText, broadcastText;
    private LinearLayout rootLayout;
    private DatabaseService db;
    private String activePacketId;
    private ListView receivedList;
    private List<SosPacket> receivedPackets = new ArrayList<>();
    private ReceivedAdapter adapter;
    private BroadcastReceiver packetReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent serviceIntent = new Intent(this, BleService.class);
        startService(serviceIntent);

        db = new DatabaseService(this);

        NetworkReceiver networkReceiver = new NetworkReceiver(db);
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
                Context.RECEIVER_NOT_EXPORTED);

        rootLayout    = findViewById(R.id.rootLayout);
        btnCritical   = findViewById(R.id.btnCritical);
        btnInjured    = findViewById(R.id.btnInjured);
        btnHelp       = findViewById(R.id.btnHelp);
        btnSOS        = findViewById(R.id.btnSOS);
        statusText    = findViewById(R.id.statusText);
        broadcastText = findViewById(R.id.broadcastText);

        receivedList = findViewById(R.id.receivedList);
        adapter = new ReceivedAdapter(this, receivedPackets);
        receivedList.setAdapter(adapter);



        this.packetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("SOS_RECEIVED".equals(intent.getAction())) {
                    SosPacket packet = (SosPacket) intent.getSerializableExtra("packet");
                    if (packet != null) {
                        boolean alreadyExists = false;
                        for (SosPacket existing : receivedPackets) {
                            if (existing.getPacketId().equals(packet.getPacketId())) {
                                alreadyExists = true;
                                break;
                            }
                        }
                        if (!alreadyExists) {
                            receivedPackets.add(0, packet);
                            adapter.notifyDataSetChanged();

                            db.upsertPacket(packet);
                            UploadService uploadService = new UploadService(db);
                            uploadService.uploadPendingPackets();

                            runOnUiThread(() -> {
                                statusText.setText("Relayed " + receivedPackets.size() + " SOS signals");
                            });
                        }
                    }
                }
            }
        };

        registerReceiver(packetReceiver, new IntentFilter("SOS_RECEIVED"),
                Context.RECEIVER_NOT_EXPORTED);

        btnCritical.setOnClickListener(v -> selectStatus(SosPacket.STATUS_CRITICAL));
        btnInjured.setOnClickListener(v -> selectStatus(SosPacket.STATUS_INJURED));
        btnHelp.setOnClickListener(v -> selectStatus(SosPacket.STATUS_HELP));

        btnSOS.setOnClickListener(v -> {
            if (!isEmergencyActive) {
                activateSOS();
            } else {
                deactivateSOS();
            }
        });

        updateStatusButtons();
        requestBluetoothPermissions();
    }

    private void selectStatus(int status) {
        selectedStatus = status;
        updateStatusButtons();
    }

    private void updateStatusButtons() {
        btnCritical.setAlpha(selectedStatus == SosPacket.STATUS_CRITICAL ? 1.0f : 0.3f);
        btnInjured.setAlpha(selectedStatus == SosPacket.STATUS_INJURED  ? 1.0f : 0.3f);
        btnHelp.setAlpha(selectedStatus == SosPacket.STATUS_HELP        ? 1.0f : 0.3f);
    }

    private void activateSOS() {
        isEmergencyActive = true;

        rootLayout.setBackgroundColor(Color.parseColor("#3a0000"));
        btnSOS.setText("LOCATING...");
        btnSOS.setTextSize(20);
        btnSOS.setAlpha(0.7f);
        statusText.setText("Getting your location...");

        LocationService locationService = new LocationService(this);
        locationService.getLastLocation((lat, lng) -> {
            runOnUiThread(() -> {
                if (!isEmergencyActive) return;
                String deviceId = android.os.Build.MODEL + "_" + android.provider.Settings.Secure.getString(
                        getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID);

                SosPacket packet = new SosPacket(deviceId, selectedStatus, lat, lng);
                db.upsertPacket(packet);
                activePacketId = packet.getPacketId();
                Log.d("MainActivity", "Set activePacketId: " + activePacketId);

                btnSOS.setText("BROADCASTING");
                statusText.setText("SOS active — signal relaying through mesh");
                broadcastText.setText("📡 Relaying from " +
                        String.format("%.4f, %.4f", lat, lng));

                Intent serviceIntent = new Intent(this, BleService.class);
                serviceIntent.putExtra("packetId", packet.getPacketId());
                serviceIntent.putExtra("userId", packet.getUserId());
                serviceIntent.putExtra("statusCode", packet.getStatusCode());
                serviceIntent.putExtra("lat", packet.getLat());
                serviceIntent.putExtra("lng", packet.getLng());
                serviceIntent.putExtra("timestamp", packet.getTimestamp());
                serviceIntent.putExtra("sequenceNumber", packet.getSequenceNumber());
                serviceIntent.putExtra("hopCount", packet.getHopCount());
                ContextCompat.startForegroundService(this, serviceIntent);
                UploadService uploadService = new UploadService(db);
                uploadService.uploadPendingPackets();
            });
        });
    }

    private void deactivateSOS() {
        isEmergencyActive = false;

        if (activePacketId != null) {
            Log.d("MainActivity", "Calling resolvePacket for: " + activePacketId);
            db.resolvePacket(activePacketId);
            Intent stopIntent = new Intent(this, BleService.class);
            stopIntent.setAction(BleService.ACTION_STOP_ADVERTISING);
            stopIntent.putExtra("packetId", activePacketId);
            startService(stopIntent);
            activePacketId = null;
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }

        Intent stopAdvert = new Intent(this, BleService.class);
        stopAdvert.setAction(BleService.ACTION_STOP_ADVERTISING);
        startService(stopAdvert);

        rootLayout.setBackgroundColor(Color.parseColor("#1a1a1a"));
        btnSOS.setText("SOS");
        btnSOS.setTextSize(48);
        btnSOS.setAlpha(1.0f);
        statusText.setText("Mesh network ready");
        broadcastText.setText("");
    }



    private void requestBluetoothPermissions() {
        String[] permissions = {
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_FINE_LOCATION
        };

        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, BT_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (packetReceiver != null) {
            unregisterReceiver(packetReceiver);
        }
    }
}
