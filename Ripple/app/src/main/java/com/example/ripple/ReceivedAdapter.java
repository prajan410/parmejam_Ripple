package com.example.ripple;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReceivedAdapter extends BaseAdapter {

    private final Context context;
    private final List<SosPacket> packets;

    public ReceivedAdapter(Context context, List<SosPacket> packets) {
        this.context = context;
        this.packets = packets;
    }

    @Override
    public int getCount() {
        return packets.size();
    }

    @Override
    public SosPacket getItem(int position) {
        return packets.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    android.R.layout.simple_list_item_2, parent, false);
        }

        SosPacket packet = getItem(position);
        TextView line1 = convertView.findViewById(android.R.id.text1);
        TextView line2 = convertView.findViewById(android.R.id.text2);

        line1.setTextColor(0xFF00FF00); // green text
        line2.setTextColor(0xFFFFFFFF); // white text

        line1.setText(packet.getUserId() + " (" + packet.getHopCount() + " hops)");
        line2.setText(String.format(Locale.US, "%.4f, %.4f",
                packet.getLat(), packet.getLng()));

        return convertView;
    }
}
