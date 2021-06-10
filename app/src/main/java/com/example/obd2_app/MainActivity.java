package com.example.obd2_app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.BluetoothCallback;
import me.aflak.bluetooth.interfaces.DeviceCallback;
import me.aflak.bluetooth.interfaces.DiscoveryCallback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ListView.OnItemClickListener
{
    private BluetoothAdapter bluetoothAdapter;
    private Bluetooth bluetooth;
    private List<BluetoothDevice> pairedDevices;
    private List<BluetoothDevice> scannedDevices;
    private List<BluetoothDevice> AllDevices = new ArrayList<>();

    private BluetoothSocket socket = null;

    ListView lv;
    Button onButton, offButton, visibleButton, listButton;
    TextView textMSG;
    final Handler myHandler = new Handler();
    String msg = "";

    private boolean scanning = false;

    private static final String  AddressList[] =
            {
                    //...
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lv = findViewById(R.id.listView);
        onButton = findViewById(R.id.onbutton);
        offButton = findViewById(R.id.offbutton);
        visibleButton = findViewById(R.id.visiblebutton);
        listButton = findViewById(R.id.listbutton);
        textMSG = findViewById(R.id.text_msg);
        onButton.setOnClickListener(this);
        offButton.setOnClickListener(this);
        visibleButton.setOnClickListener(this);
        listButton.setOnClickListener(this);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(MainActivity.this, "Device doesn't support Bluetooth !", Toast.LENGTH_SHORT).show();

        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        // bluetooth lib
        bluetooth = new Bluetooth(this);
        bluetooth.setCallbackOnUI(this);
        bluetooth.setBluetoothCallback(bluetoothCallback);
        bluetooth.setDiscoveryCallback(discoveryCallback);
        bluetooth.setDeviceCallback(deviceCallback);

        scannedDevices = new ArrayList<>();
        for (String address : AddressList)
        {
            if(BluetoothAdapter.checkBluetoothAddress(address))
            {
                BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
                scannedDevices.add(device);
            }
        }

        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {UpdateGUI();}
        }, 0, 250);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetooth.onStart();
        if(bluetooth.isEnabled()){
            //...
        } else {
            bluetooth.showEnableDialog(MainActivity.this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bluetooth.onActivityResult(requestCode, resultCode);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(bluetooth.isConnected()) {
            bluetooth.disconnect();
        }
        bluetooth.onStop();
    }

    private BluetoothCallback bluetoothCallback = new BluetoothCallback() {
        @Override
        public void onBluetoothTurningOn() {
            Toast.makeText(MainActivity.this, "Bluetooth TurningOn !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBluetoothOn() {
            Toast.makeText(MainActivity.this, "Bluetooth On !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBluetoothTurningOff() {
            Toast.makeText(MainActivity.this, "Bluetooth TurningOff !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBluetoothOff() {
            Toast.makeText(MainActivity.this, "Bluetooth Off !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onUserDeniedActivation() {
            Toast.makeText(MainActivity.this, "I need to activate bluetooth...", Toast.LENGTH_SHORT).show();
        }
    };

    private DiscoveryCallback discoveryCallback = new DiscoveryCallback() {
        @Override
        public void onDiscoveryStarted() {
            Toast.makeText(MainActivity.this, "Discovery Started !", Toast.LENGTH_SHORT).show();
            scannedDevices = new ArrayList<>();
            for (String address : AddressList)
            {
                if(BluetoothAdapter.checkBluetoothAddress(address))
                {
                    BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
                    scannedDevices.add(device);
                }
            }
            scanning = true;
        }

        @Override
        public void onDiscoveryFinished() {
            Toast.makeText(MainActivity.this, "Discovery Finished !", Toast.LENGTH_SHORT).show();
            scanning = false;
        }

        @Override
        public void onDeviceFound(BluetoothDevice device) {
            Toast.makeText(MainActivity.this, "Device Found !", Toast.LENGTH_SHORT).show();
            scannedDevices.add(device);
            updateList();
        }

        @Override
        public void onDevicePaired(BluetoothDevice device) {
            Toast.makeText(MainActivity.this, "Paired !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDeviceUnpaired(BluetoothDevice device) {
            Toast.makeText(MainActivity.this, "Unpaired !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(int errorCode) {
            Toast.makeText(MainActivity.this, "Error! (DiscoveryCallback) -> "+String.valueOf(errorCode), Toast.LENGTH_SHORT).show();
        }
    };

    public void updateList(){
        AllDevices = new ArrayList<>();
        pairedDevices = new ArrayList<>();
        for (BluetoothDevice bt : bluetoothAdapter.getBondedDevices()){
            pairedDevices.add(bt);
            AllDevices.add(bt);
        }
        for (BluetoothDevice bt : scannedDevices) AllDevices.add(bt);

        ArrayList list = new ArrayList();
        for(BluetoothDevice bt : AllDevices) list.add(bt.getName());
        Toast.makeText(getApplicationContext(), "Showing Devices",Toast.LENGTH_SHORT).show();
        final ArrayAdapter adapter = new  ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
    }

    public void on(View v){
        bluetooth.enable();
        Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        /*if (!bluetoothAdapter.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }*/
    }

    public void off(View v){
        bluetooth.disable();
        //bluetoothAdapter.disable();
        Toast.makeText(getApplicationContext(), "Turned off" , Toast.LENGTH_LONG).show();
    }

    public  void visible(View v){
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }

    public void list(View v){
        bluetooth.startScanning();
        updateList();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.onbutton:
                on(v);
                break;
            case R.id.offbutton:
               off(v);
                break;
            case R.id.listbutton:
                list(v);
                break;
            case R.id.visiblebutton:
                visible(v);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        Toast.makeText(getApplicationContext(), "Try to connect!",Toast.LENGTH_SHORT).show();

        if(bluetooth.isEnabled()) {
            if (scanning) {
                bluetooth.stopScanning();
                scanning = false;
            }

            BluetoothDevice selectedDevice = AllDevices.get(position);

            boolean paired = false;

            for(BluetoothDevice bt : pairedDevices){
                if(bt.getAddress() == selectedDevice.getAddress()){
                    paired = true;
                    break;
                }
            }

            if(!paired)
                bluetooth.pair(selectedDevice);


            if(!bluetooth.getPairedDevices().isEmpty()) {
                Intent intent = new Intent(this, Real_time_charts.class);
                intent.putExtra("device", selectedDevice);
                startActivity(intent);
                bluetooth.connectToDevice(selectedDevice);
            }

        }
    }

    private DeviceCallback deviceCallback = new DeviceCallback() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            Toast.makeText(MainActivity.this, "Connected !", Toast.LENGTH_SHORT).show();
            socket = bluetooth.getSocket();
            if (socket != null && socket.isConnected())
            {
                try {
                    //new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                    //new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                    //new TimeoutCommand(125).run(socket.getInputStream(), socket.getOutputStream());
                    //new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
                    //new AmbientAirTemperatureCommand().run(socket.getInputStream(), socket.getOutputStream());
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, String message) {
            Toast.makeText(MainActivity.this, "Device disconnected !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onMessage(byte[] message) {
            String str = new String(message);
            Toast.makeText(MainActivity.this, "Message -> "+str, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(int errorCode) {
            Toast.makeText(MainActivity.this, "Error! -> "+errorCode, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnectError(final BluetoothDevice device, String message) {
            Toast.makeText(MainActivity.this, "Could not connect, next try in 3 sec...", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetooth.connectToDevice(device);
                }
            }, 3000);
        }
    };

    private void UpdateGUI() {
        myHandler.post(myRunnable);
    }

    final Runnable myRunnable = new Runnable() {
        public void run() {

            if (socket != null && socket.isConnected())
            {
                byte[] buffer = new byte[512];  // buffer (our data)
                int bytesCount; // amount of read bytes

                try {
                    //reading data from input stream
                    bytesCount = socket.getInputStream().read(buffer);

                    if (buffer != null && bytesCount > 0) {
                        textMSG.setText(new String(buffer, 0, bytesCount, StandardCharsets.US_ASCII));
                    }
                } catch (IOException e) {
                    //..
                }
            }

        }
    };

}
