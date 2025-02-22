package com.droiduino.bluetoothconn;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import static android.content.ContentValues.TAG;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

public class MainActivity extends AppCompatActivity {

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTION_DISCONNECTED = 3; // used in bluetooth handler to handle disconnection

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration config = getResources().getConfiguration();
        if (config.smallestScreenWidthDp >= 600) {
            setContentView(R.layout.activity_main);
        } else {
            setContentView(R.layout.activity_main_phone);
        }

        // UI Initialization
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        /** RecyclerView **/
        // Display paired device using recyclerView

        ArrayList<PinSwitchModel> switches;

        if (PinSwitchAdapter.pinSwitches != null){
            switches = PinSwitchAdapter.pinSwitches;
        }
        else {
            switches = new ArrayList<>();
        }

        final ArrayList<PinSwitchModel> pinSwitchList = switches;
        final PinSwitchAdapter pinSwitchAdapter = new PinSwitchAdapter(this, pinSwitchList);

        final RecyclerView recyclerView = findViewById(R.id.pinSwitchRecyclerView);
        recyclerView.setAdapter(pinSwitchAdapter);
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.SPACE_AROUND);
        recyclerView.setLayoutManager(layoutManager);

        ItemTouchHelper.Callback callback =
                new MyItemTouchHelperCallback(pinSwitchAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        final Button buttonAddPinSwitch = findViewById(R.id.buttonAddPinSwitch);
        buttonAddPinSwitch.setEnabled(false);

        final EditText addPinNumberEditText = findViewById(R.id.addPinNumberEditText);
        addPinNumberEditText.setEnabled(false);

        final EditText editTextSwitchName = findViewById(R.id.editTextSwitchName);
        editTextSwitchName.setEnabled(false);

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null){
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progress and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);
            buttonAddPinSwitch.setEnabled(false);
            addPinNumberEditText.setEnabled(false);
            editTextSwitchName.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
            createConnectThread.start();
        }

        /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setText("Disconnect");
                                buttonConnect.setEnabled(true);
                                buttonAddPinSwitch.setEnabled(true);
                                addPinNumberEditText.setEnabled(true);
                                editTextSwitchName.setEnabled(true);
                                pinSwitchAdapter.enable();
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setText("Connect");
                                buttonConnect.setEnabled(true);
                                buttonAddPinSwitch.setEnabled(false);
                                addPinNumberEditText.setEnabled(false);
                                editTextSwitchName.setEnabled(false);
                                pinSwitchAdapter.disable();
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        if (arduinoMsg.startsWith("Message:")) {
                            Toast.makeText(MainActivity.this, arduinoMsg,
                                    Toast.LENGTH_LONG).show();
                        }
                        break;

                    case CONNECTION_DISCONNECTED:
                        buttonConnect.setText("Connect");
                        buttonConnect.setEnabled(true);
                        buttonAddPinSwitch.setEnabled(false);
                        addPinNumberEditText.setEnabled(false);
                        editTextSwitchName.setEnabled(false);
                        pinSwitchAdapter.disable();
                        toolbar.setSubtitle(R.string.app_substring);
                        Log.e("Status", msg.obj.toString());
                        closeStream();
                        break;
                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String btnText = buttonConnect.getText().toString();
                if (btnText.equals("Connect")) {
                    // Move to adapter list
                    Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                    startActivity(intent);
                }
                else if (btnText.equals("Disconnect")){
                    closeStream();
                    buttonConnect.setText("Connect");
                }
            }
        });

        // Button to ON/OFF LED on Arduino Board
        buttonAddPinSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pinValue = addPinNumberEditText.getText().toString();
                String pinName = editTextSwitchName.getText().toString();

                if (isNullOrWhitespace(pinValue)){
                    Toast.makeText(MainActivity.this, "Invalid Pin Number", Toast.LENGTH_LONG);
                    return;
                }

                int pin = Integer.parseInt(pinValue);

                if (isNullOrWhitespace(pinName)) {
                    pinSwitchAdapter.addItem(new PinSwitchModel(pin));
                }
                else {
                    pinSwitchAdapter.addItem(new PinSwitchModel(pinName, pin));
                }

                // reinitialize
                editTextSwitchName.setText(null);
                recyclerView.scrollToPosition(pinSwitchAdapter.getItemCount() - 1);
            }
        });
    }

    private boolean isNullOrWhitespace(String s){
        return s == null || s.trim().length() == 0;
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            //TODO :: Edit UI when activity and bluetooth connection are closed
            // Add a disconnect button.
            // Add event listener for disconnection.
            // Disable UI when disconnected.
            // Add remove/relocate option for switches (Press and hold on pin number).

            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();

                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread();
            connectedThread.run();
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread() {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (mmSocket != null && mmSocket.isConnected()) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message",readMessage);
                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    // Connection closed, disable UI
                    handler.obtainMessage(CONNECTION_DISCONNECTED, "Current connection closed.").sendToTarget();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        closeStream();

        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    public void closeStream(){
        try {
            if (mmSocket != null && mmSocket.isConnected()) {
                mmSocket.close();
            }
        } catch (IOException e) {
            Log.e("Error", "closeStream() failed", e);
        }
    }
}
