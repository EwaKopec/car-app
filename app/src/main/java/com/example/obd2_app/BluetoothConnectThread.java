package com.example.obd2_app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectThread extends Thread {
    private final BluetoothSocket socket;
    private final BluetoothDevice device;

    private final static String DEFAULT_UUID = "00001101-0000-1000-8000-00805f9b34fb";

    public BluetoothSocket getSocket() {
        return socket;
    }

    public BluetoothConnectThread(BluetoothDevice device) {
        BluetoothSocket tmp = null;
        this.device = device;

        try {
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(DEFAULT_UUID)); //this.device.getUuids()[0].getUuid()
        } catch (IOException e) {
            Log.e("BtConnError", "Socket's create() method failed", e);
        }

        this.socket = tmp;
    }

    public void run() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.cancelDiscovery();

        try {
            socket.connect();
        } catch (IOException connException) {
            Log.e("BtOpenSocketError", "Could not open the client socket", connException);
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.e("BtCloseSocketError", "Could not close the client socket", closeException);
            }
            return;
        }
    }

    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e("BtCancelSocketError", "Could not close the client socket", e);
        }
    }
}
