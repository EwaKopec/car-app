package com.example.obd2_app;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import com.github.anastr.speedviewlib.Gauge;
import com.github.anastr.speedviewlib.Speedometer;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Pattern;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.DeviceCallback;


public class Real_time_charts extends AppCompatActivity
{
    Speedometer speedometer, turnover;
    Gauge gauge;

    private Bluetooth bluetooth;
    private BluetoothDevice device;
    private BluetoothSocket socket = null;

    TextView textMSG;
    final Handler myHandler = new Handler();
    PrimeThread p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_charts);

        speedometer = findViewById(R.id.awesomeSpeedometer);
        turnover = findViewById(R.id.turnover);
        gauge = findViewById(R.id.gauge);
        textMSG = findViewById(R.id.text_msg);

        device = getIntent().getParcelableExtra("device");
        //bluetooth = new Bluetooth(this);
        //bluetooth.setCallbackOnUI(this);
        //bluetooth.setDeviceCallback(deviceCallback);

        customizeSpeedometer(speedometer);
        customizeTurnover(turnover);
        customizeGauge(gauge);

        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {UpdateGUI();}
        }, 0, 500);

        p = new PrimeThread(Real_time_charts.this);
        p.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //bluetooth.onStart();
        //bluetooth.connectToDevice(device);
        Toast.makeText(Real_time_charts.this, "Connecting...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //if(bluetooth.isConnected()) {
            //bluetooth.disconnect();
        //}
        //bluetooth.onStop();
    }

    void customizeSpeedometer(Speedometer s)
    {
        s.setBackgroundCircleColor(Color.WHITE);
        s.setSpeedTextColor(Color.GREEN);
        s.setIndicatorLightColor(Color.MAGENTA);
        s.setStartDegree(0);
        s.setEndDegree(250);
    }

    void customizeTurnover(Speedometer s)
    {
        s.setBackgroundCircleColor(Color.WHITE);
        s.setSpeedTextColor(Color.GREEN);
        s.setIndicatorLightColor(Color.MAGENTA);
        s.setStartDegree(0);
        s.setEndDegree(10);
    }

    void customizeGauge(Gauge g)
    {
        g.setMinSpeed(60.0f);
        g.setMaxSpeed(120.0f);
    }

    private DeviceCallback deviceCallback = new DeviceCallback() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            Toast.makeText(Real_time_charts.this, "Connected !", Toast.LENGTH_SHORT).show();
            socket = bluetooth.getSocket();
            p = new PrimeThread(Real_time_charts.this);
            p.start();
            /*if (socket != null && socket.isConnected())
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
            }*/
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, String message) {
            Toast.makeText(Real_time_charts.this, "Device disconnected !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onMessage(byte[] message) {
            String str = new String(message);
            Toast.makeText(Real_time_charts.this, "Message -> "+str, Toast.LENGTH_LONG).show();
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


            socket = p.getSocket();

            if (socket != null && socket.isConnected())
            {
                //socket.getOutputStream().write(("01 05" + "\r").getBytes());
                //socket.getOutputStream().flush();

                textMSG.setText(p.getData());
                gauge.setSpeedAt(p.getData().length());
            }

        }
    };

    static class PrimeThread extends Thread
    {
        private static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");
        private static Pattern BUSINIT_PATTERN = Pattern.compile("(BUS INIT)|(BUSINIT)|(\\.)");
        private static Pattern SEARCHING_PATTERN = Pattern.compile("SEARCHING");
        private static Pattern DIGITS_LETTERS_PATTERN = Pattern.compile("([0-9A-F])+");

        Real_time_charts main;
        BluetoothSocket socket;
        String rawData = "...";
        protected ArrayList<Integer> buffer = null;
        private float temperature = 0.0f;

        long lastPick = 0;
        String tmp;

        BluetoothAdapter myBluetooth;
        static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        PrimeThread(Real_time_charts activity){
            main = activity;
            this.buffer = new ArrayList<>();

            myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
            BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(main.device.getAddress());//connects to the device's address and checks if it's available
            try {
                socket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
            } catch (IOException e) {
                e.printStackTrace();
            }
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            try {
                socket.connect();//start connection
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            while(true)
            {
                if (socket != null && socket.isConnected())
                {
                    try {
                        EngineCoolantTemperatureCommand com = new EngineCoolantTemperatureCommand ();
                        com.run(socket.getInputStream(), socket.getOutputStream());
                        tmp = com.getCalculatedResult();
                        lastPick = System.currentTimeMillis();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    /*
                    try {
                        //reading data from input stream
                        // read until '>' arrives OR end of stream reached
                        StringBuilder res = new StringBuilder();
                        byte b = 0;
                        char c;

                        // -1 if the end of the stream is reached
                        while ((b = (byte) socket.getInputStream().read()) > -1) {
                            c = (char) b;
                            if (c == '>') // read until '>' arrives
                            {
                                break;
                            }
                            res.append(c);
                        }

                        rawData = removeAll(SEARCHING_PATTERN, res.toString());
                        rawData = removeAll(WHITESPACE_PATTERN, rawData);//removes all [ \t\n\x0B\f\r]
                        rawData = removeAll(BUSINIT_PATTERN, rawData);

                        if (rawData.length() > 6) {
                            rawData = rawData.substring(rawData.length()-6,rawData.length());
                        }

                        if(rawData.length() > 0) {
                            fillBuffer();
                            performCalculations();
                            lastPick = System.currentTimeMillis();
                        }

                    } catch (IOException e) {
                        Toast.makeText(main, "Error! -> " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    */

                }
            }
        }

        public String getData(){
            return  tmp+"'F | TIME: "+String.valueOf(System.currentTimeMillis()-lastPick); //String.valueOf(temperature)+"'F | TIME: "+String.valueOf(System.currentTimeMillis()-lastPick);
        }

        public BluetoothSocket getSocket(){
            return  socket;
        }

        protected void fillBuffer() {
            rawData = rawData.replaceAll("\\s", ""); //removes all [ \t\n\x0B\f\r]
            rawData = rawData.replaceAll("(BUS INIT)|(BUSINIT)|(\\.)", "");

            if (rawData.matches("([0-9A-F])+")) {
                //throw new NonNumericResponseException(rawData);
                // read string each two chars
                buffer.clear();
                int begin = 0;
                int end = 2;
                while (end <= rawData.length()) {
                    buffer.add(Integer.decode("0x" + rawData.substring(begin, end)));
                    begin = end;
                    end += 2;
                }
            }
        }

        protected void performCalculations() {
            // ignore first two bytes [hh hh] of the response
            temperature = buffer.get(2) - 40;
        }

        protected String replaceAll(Pattern pattern, String input, String replacement) {
            return pattern.matcher(input).replaceAll(replacement);
        }

        protected String removeAll(Pattern pattern, String input) {
            return pattern.matcher(input).replaceAll("");
        }

    }

}