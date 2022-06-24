package com.droiduino.bluetoothconn;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PinSwitchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    public static ArrayList<PinSwitchModel> pinSwitches;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        Switch pinSwitch;
        TextView pinTextView;

        public ViewHolder(View v) {
            super(v);
            pinSwitch = v.findViewById(R.id.pinSwitch);
            pinTextView = v.findViewById((R.id.pinTextView));
        }
    }
    public PinSwitchAdapter(){};

    public PinSwitchAdapter(Context context, ArrayList<PinSwitchModel> pinSwitches) {
        this.context = context;
        this.pinSwitches = pinSwitches;
    }

    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.pin_switch_layout, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    private String getCommandText(int pinNumber, Boolean turnOn){
        return (turnOn ? "on" : "off") + "," + pinNumber;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        ViewHolder itemHolder = (ViewHolder) holder;
        final PinSwitchModel model = (PinSwitchModel) pinSwitches.get(position);

        itemHolder.pinSwitch.setText(model.getName());
        itemHolder.pinSwitch.setChecked(model.getIsChecked());
        itemHolder.pinSwitch.setEnabled(model.getIsEnabled());
        itemHolder.pinTextView.setText(Integer.toString(model.getPinNumber()));

        // When a device is selected
        itemHolder.pinSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    // Send command to Arduino board
                    model.setIsChecked(isChecked);
                    int pinNumber = model.getPinNumber();
                    String request = getCommandText(pinNumber, isChecked);
                    MainActivity.connectedThread.write(request);
                }
                catch (Exception e){
                    int i = 0;
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        int dataCount = pinSwitches.size();
        return dataCount;
    }

    public void addItem(PinSwitchModel item){
        int position = this.pinSwitches.size();
        this.pinSwitches.add(position, item);
        notifyItemInserted(position);
    }

    public void swap(Integer fromPosition, Integer toPosition) {
        Collections.swap(pinSwitches, fromPosition, toPosition);
        //notifyItemMoved(fromPosition, toPosition);
        notifyDataSetChanged();
    }
}
