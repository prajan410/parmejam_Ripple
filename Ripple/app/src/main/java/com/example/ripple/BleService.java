package com.example.ripple;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.content.Intent;


public class BleService extends Service {

    private static final String TAG = "Ripple";
    public static final String SERVICE_UUID = "0000dead-0000-1000-8000-00805f9b34fb";
    private static final String CHANNEL_ID = "ripple_ble";

    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;
    private DatabaseService db;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private UploadService uploadService;
    public static final String ACTION_STOP_ADVERTISING = "STOP_ADVERTISING";

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseService(this);
        uploadService = new UploadService(db);
        startConnectivityMonitor();

        startForegroundNotification();

        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (btManager == null) {
            Log.w(TAG, "No Bluetooth manager — running in degraded mode");
            return;
        }

        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth not available or disabled");
            return;
        }

        advertiser = btAdapter.getBluetoothLeAdvertiser();
        scanner = btAdapter.getBluetoothLeScanner();

        if (scanner != null) {
            startScanning();
        } else {
            Log.w(TAG, "BLE scanner not available on this device");
        }
    }

    private void startConnectivityMonitor() {
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "Internet available — uploading pending packets");
                uploadService.uploadPendingPackets();
            }

            @Override
            public void onLost(Network network) {
                Log.d(TAG, "Internet lost — mesh relay mode active");
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);

        uploadService.uploadPendingPackets();
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_ADVERTISING.equals(intent.getAction())) {
            if (advertiser != null) advertiser.stopAdvertising(advertiseCallback);
            return START_STICKY;
        }
        if (intent != null && intent.hasExtra("packetId")) {
            SosPacket packet = new SosPacket(
                    intent.getStringExtra("packetId"),
                    intent.getStringExtra("userId"),
                    intent.getIntExtra("statusCode", 1),
                    intent.getDoubleExtra("lat", 0.0),
                    intent.getDoubleExtra("lng", 0.0),
                    intent.getLongExtra("timestamp", 0),
                    intent.getIntExtra("sequenceNumber", 0),
                    intent.getIntExtra("hopCount", 0),
                    false, false
            );

            db.upsertPacket(packet);
            uploadService.uploadPendingPackets();

            if (advertiser != null) {
                startAdvertising(packet);
                Log.d(TAG, "Advertising SOS packet: " + packet);
            } else {
                Log.w(TAG, "Advertiser null — no BLE hardware");
            }
        } else {
            Log.d(TAG, "BleService started without packet extras");
        }
        return START_STICKY;
    }


    public void startAdvertising(SosPacket packet) {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_UUID)))
                .addManufacturerData(0x0001, encodePacket(packet))
                .setIncludeDeviceName(false)
                .build();

        advertiser.startAdvertising(settings, data, advertiseCallback);
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "BLE advertising started");
        }
        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "BLE advertising failed: " + errorCode);
        }
    };

    private void startScanning() {
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_UUID)))
                .build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);

        scanner.startScan(filters, settings, scanCallback);
        Log.d(TAG, "BLE scanning started");
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanRecord record = result.getScanRecord();
            if (record == null) return;

            byte[] manufacturerData = record.getManufacturerSpecificData(0x0001);
            if (manufacturerData == null) return;

            SosPacket received = decodePacket(manufacturerData);
            if (received == null) return;

            Log.d(TAG, "Received SOS via BLE: " + received);

            int delay = new Random().nextInt(500) + 100;
            new android.os.Handler(getMainLooper()).postDelayed(() -> {
                received.incrementHop();
                db.upsertPacket(received);
                Intent broadcast = new Intent("SOS_RECEIVED");
                broadcast.putExtra("packet", received);
                sendBroadcast(broadcast);
                startAdvertising(received);
            }, delay);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scan failed: " + errorCode);
        }
    };

    private byte[] encodePacket(SosPacket packet) {
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.put((byte) packet.getStatusCode());
        buf.putInt((int)(packet.getLat() * 1e6));
        buf.putInt((int)(packet.getLng() * 1e6));
        buf.putShort((short) packet.getSequenceNumber());
        buf.put((byte) packet.getHopCount());
        return buf.array();
    }

    private SosPacket decodePacket(byte[] data) {
        if (data.length < 12) return null;
        try {
            ByteBuffer buf = ByteBuffer.wrap(data);
            int status   = buf.get() & 0xFF;
            double lat   = buf.getInt() / 1e6;
            double lng   = buf.getInt() / 1e6;
            int seq      = buf.getShort() & 0xFFFF;
            int hops     = buf.get() & 0xFF;

            return new SosPacket(
                    UUID.randomUUID().toString(),
                    "unknown",
                    status, lat, lng,
                    System.currentTimeMillis() / 1000,
                    seq, hops, false, false
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode BLE packet", e);
            return null;
        }
    }

    private void startForegroundNotification() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Ripple Mesh", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Ripple Active")
                .setContentText("Mesh network running — ready to relay SOS signals")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .build();

        try {
            startForeground(1, notification);
        } catch (Exception e) {
            Log.w(TAG, "startForeground not allowed, running without FGS", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scanner != null) scanner.stopScan(scanCallback);
        if (advertiser != null) advertiser.stopAdvertising(advertiseCallback);
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}
