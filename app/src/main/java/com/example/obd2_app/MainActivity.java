package com.example.obd2_app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.BluetoothCallback;
import me.aflak.bluetooth.interfaces.DiscoveryCallback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ListView.OnItemClickListener
{
    private BluetoothAdapter        bluetoothAdapter;
    private Bluetooth               bluetooth;
    private List<BluetoothDevice>   pairedDevices;

    ListView lv;
    Button onButton, offButton, visibleButton, listButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lv              = findViewById(R.id.listView);
        onButton        = findViewById(R.id.onbutton);
        offButton       = findViewById(R.id.offbutton);
        visibleButton   = findViewById(R.id.visiblebutton);
        listButton      = findViewById(R.id.listbutton);

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetooth.onStart();
        if(!bluetooth.isEnabled())
            bluetooth.showEnableDialog(MainActivity.this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(bluetooth.isConnected()) {
            bluetooth.disconnect();
        }
        bluetooth.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bluetooth.onActivityResult(requestCode, resultCode);
    }

    private BluetoothCallback bluetoothCallback = new BluetoothCallback() {
        @Override
        public void onBluetoothTurningOn() {
            Toast.makeText(MainActivity.this, "Bluetooth Turning On !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBluetoothOn() {
            Toast.makeText(MainActivity.this, "Bluetooth On !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBluetoothTurningOff() {
            Toast.makeText(MainActivity.this, "Bluetooth Turning Off !", Toast.LENGTH_SHORT).show();
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
        }

        @Override
        public void onDiscoveryFinished() {
            Toast.makeText(MainActivity.this, "Discovery Finished !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDeviceFound(BluetoothDevice device) {
            Toast.makeText(MainActivity.this, "Device Found !", Toast.LENGTH_SHORT).show();
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
        pairedDevices = new ArrayList<>();
        ArrayList list = new ArrayList();
        for (BluetoothDevice bt : bluetoothAdapter.getBondedDevices()){
            pairedDevices.add(bt);
            list.add(bt.getName());
        }

        final ArrayAdapter adapter = new  ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.onbutton:
                if (!bluetooth.isEnabled()) {
                    bluetooth.enable();
                }
                break;
            case R.id.offbutton:
                bluetooth.disable();
                break;
            case R.id.listbutton:
                updateList();
                break;
            case R.id.visiblebutton:
                Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityForResult(getVisible, 0);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        Toast.makeText(getApplicationContext(), "Try to connect!",Toast.LENGTH_SHORT).show();

        if(bluetooth.isEnabled()) {

            BluetoothDevice selectedDevice = pairedDevices.get(position);

            if(!bluetooth.getPairedDevices().isEmpty() && selectedDevice != null) {
                Intent intent = new Intent(this, TestActivity.class);
                intent.putExtra("device", selectedDevice);
                startActivity(intent);
            }

        }
    }

}
