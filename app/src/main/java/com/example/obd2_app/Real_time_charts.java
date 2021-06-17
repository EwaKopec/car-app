package com.example.obd2_app;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.github.anastr.speedviewlib.Gauge;
import com.github.anastr.speedviewlib.Speedometer;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.DeviceCallback;


public class Real_time_charts extends AppCompatActivity {
    Speedometer speedometer, turnover;
    Gauge gauge;

    /*
    private Bluetooth bluetooth;
    private BluetoothDevice device;
    private BluetoothSocket socket = null;*/

    TextView textMSG;
    private ViewFlipper viewFlipper;
    final Handler myHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_charts);

        viewFlipper = findViewById(R.id.view_flipper);

        speedometer = findViewById(R.id.awesomeSpeedometer);
        turnover = findViewById(R.id.turnover);
        gauge = findViewById(R.id.gauge);
        textMSG = findViewById(R.id.text_msg);

        /*
        device = getIntent().getParcelableExtra("device");
        bluetooth = new Bluetooth(this);
        bluetooth.setCallbackOnUI(this);
        bluetooth.setDeviceCallback(deviceCallback); */

        customizeSpeedometer(speedometer);
        customizeTurnover(turnover);
        customizeGauge(gauge);

        /*
        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {UpdateGUI();}
        }, 0, 1000);  */
    }

    @Override
    protected void onStart() {
        super.onStart();
        //bluetooth.onStart();
        //bluetooth.connectToDevice(device);
        //Toast.makeText(Real_time_charts.this, "Connecting...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        /*
        if(bluetooth.isConnected()) {
            bluetooth.disconnect();
        }
        bluetooth.onStop();*/
    }

    void customizeSpeedometer(Speedometer s)
    {
        s.setBackgroundCircleColor(getResources().getColor(R.color.colorAccent));
        s.setSpeedTextColor(getResources().getColor(R.color.colorPrimaryDark));
        s.setIndicatorLightColor(getResources().getColor(R.color.colorYellow));
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
        s.setUnit("x1000RPM");
    }

    void customizeGauge(Gauge g)
    {
        g.setMinSpeed(60.0f);
        g.setMaxSpeed(120.0f);
        g.setUnit("°C");

    }

    public void onPreviousClick(View v) {
        viewFlipper.setInAnimation(this, R.anim.slide_in_right);
        viewFlipper.setOutAnimation(this, R.anim.slide_out_left);
        viewFlipper.showPrevious();
    }

    public void onNextClick(View v) {
        viewFlipper.setInAnimation(this, android.R.anim.slide_out_right);
        viewFlipper.setOutAnimation(this, android.R.anim.slide_in_left);
        viewFlipper.showNext();
    }

    /*
    private final DeviceCallback deviceCallback = new DeviceCallback() {
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
    }; */

    private static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");
    private static Pattern BUSINIT_PATTERN = Pattern.compile("(BUS INIT)|(BUSINIT)|(\\.)");
    private static Pattern SEARCHING_PATTERN = Pattern.compile("SEARCHING");
    private static Pattern DIGITS_LETTERS_PATTERN = Pattern.compile("([0-9A-F])+");

    protected String replaceAll(Pattern pattern, String input, String replacement) {
        return pattern.matcher(input).replaceAll(replacement);
    }

    protected String removeAll(Pattern pattern, String input) {
        return pattern.matcher(input).replaceAll("");
    }

    /*
    private void UpdateGUI() {
        myHandler.post(myRunnable);
    }

    final Runnable myRunnable = new Runnable() {
        public void run() {

            Long responseDelayInMs = 50L;

            if (socket != null && socket.isConnected())
            {
                try {
                    socket.getOutputStream().write(("01 05" + "\r").getBytes());
                    socket.getOutputStream().flush();
                    if (responseDelayInMs != null && responseDelayInMs > 0) {
                        Thread.sleep(responseDelayInMs);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                String rawData = null;
                int maxCount   = 100;        //Do testów, przy OBD można dać pow. 100

                try {
                    //reading data from input stream
                    byte b = 0;
                    int count = 0;
                    StringBuilder res = new StringBuilder();

                    // read until '>' arrives OR end of stream reached
                    char c;
                    // -1 if the end of the stream is reached
                    if(socket.getInputStream().available()>0) {

                        while (((b = (byte) socket.getInputStream().read()) > -1) && count < maxCount) {
                            c = (char) b;
                            count++;
                            if (c == '>') // read until '>' arrives
                            {
                                break;
                            }
                            res.append(c);
                        }

                        rawData = removeAll(SEARCHING_PATTERN, res.toString());
                        rawData = removeAll(WHITESPACE_PATTERN, rawData);//removes all [ \t\n\x0B\f\r]
                        rawData = removeAll(BUSINIT_PATTERN, rawData);

                        if (res != null && count > 0) {
                            textMSG.setText(rawData);
                            gauge.setSpeedAt(count);
                        }
                    }


                } catch (IOException e) {
                    Toast.makeText(Real_time_charts.this, "Error! -> "+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

        }
    };
    */

}