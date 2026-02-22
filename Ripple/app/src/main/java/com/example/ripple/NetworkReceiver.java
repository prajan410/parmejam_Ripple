package com.example.ripple;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;

public class NetworkReceiver extends BroadcastReceiver {
    private DatabaseService db;

    public NetworkReceiver(DatabaseService db) {  // ADD CONSTRUCTOR
        this.db = db;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isOnline(context)) {
            UploadService uploadService = new UploadService(db);
            uploadService.uploadPendingPackets();
        }
    }

    private boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();
        return network != null;
    }
}
