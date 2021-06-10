package com.example.obd2_app;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import com.github.anastr.speedviewlib.Gauge;
import com.github.anastr.speedviewlib.Speedometer;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.DeviceCallback;


public class Real_time_charts extends AppCompatActivity {
    Speedometer speedometer, turnover;
    Gauge gauge;

    private Bluetooth bluetooth;
    private BluetoothDevice device;
    private BluetoothSocket socket = null;

    TextView textMSG;
    final Handler myHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_charts);

        speedometer = findViewById(R.id.awesomeSpeedometer);
        turnover = findViewById(R.id.turnover);
        gauge = findViewById(R.id.gauge);
        textMSG = findViewById(R.id.text_msg);
        gauge.setMaxSpeed(300.0f);

        device = getIntent().getParcelableExtra("device");
        bluetooth = new Bluetooth(this);
        bluetooth.setCallbackOnUI(this);
        bluetooth.setDeviceCallback(deviceCallback);

        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {UpdateGUI();}
        }, 0, 1000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetooth.onStart();
        bluetooth.connectToDevice(device);
        Toast.makeText(Real_time_charts.this, "Connecting...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(bluetooth.isConnected()) {
            bluetooth.disconnect();
        }
        bluetooth.onStop();
    }

    void customizeSpeedometer(Speedometer s)
    {
        //s.setBackgroundCircleColor(2);
        s.setEndDegree(250);

    }

    void customizeTurnover(Speedometer s)
    {

    }

    private DeviceCallback deviceCallback = new DeviceCallback() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            Toast.makeText(Real_time_charts.this, "Connected !", Toast.LENGTH_SHORT).show();
            socket = bluetooth.getSocket();
            if (socket != null && socket.isConnected())
            {
                try {
                    //new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                    //new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                    //new TimeoutCommand(125).run(socket.getInputStream(), socket.getOutputStream());
                    //new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
                    //new AmbientAirTemperatureCommand().run(socket.getInputStream(), socket.getOutputStream());
                    //socket.getOutputStream().write(("01 46" + "\r").getBytes());

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, String message) {
            Toast.makeText(Real_time_charts.this, "Device disconnected !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onMessage(byte[] message) {
            //String str = new String(message);
            //Toast.makeText(Real_time_charts.this, "Message -> "+str, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(int errorCode) {
            Toast.makeText(Real_time_charts.this, "Error! -> "+errorCode, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnectError(final BluetoothDevice device, String message) {
            Toast.makeText(Real_time_charts.this, "Could not connect, next try in 3 sec...", Toast.LENGTH_SHORT).show();
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
                try {
                    socket.getOutputStream().write(("01 05" + "\r").getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                byte[] buffer = new byte[512];  // buffer (our data)
                int bytesCount; // amount of read bytes

                try {
                    //reading data from input stream
                    bytesCount = socket.getInputStream().read(buffer);

                    if (buffer != null && bytesCount > 0) {
                        textMSG.setText(new String(buffer, 0, bytesCount, StandardCharsets.US_ASCII));
                        gauge.setSpeedAt(bytesCount);
                    }
                } catch (IOException e) {
                    //..
                }
            }

        }
    };

}