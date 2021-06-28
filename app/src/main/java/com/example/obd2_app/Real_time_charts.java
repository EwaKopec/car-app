package com.example.obd2_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.anastr.speedviewlib.Speedometer;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.pressure.FuelPressureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class Real_time_charts extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener
{
    Speedometer speedometer, turnover;
    TextView tempTV, fuelTV, oilTempTV, fuelPressureTV, consumptionTV, timeTV;
    ImageView recImage;
    Button menuButton;

    private static final int STORAGE_PERMISSION_CODE = 101;

    private BluetoothDevice device;

    private final  Timer         myTimer   = new Timer();
    private final  Handler       myHandler = new Handler();
    private static DataThread    myThread;
    private        long          myTimeDisconnector = System.currentTimeMillis();
    private        long          myTimeMeasurement  = 0;

    private final  SimpleDateFormat mFormat   = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
    private final  long          TIME_TO_STOP = 5000; //ms

    private final List<ObdCommand> commands = new ArrayList<>();
    private final List<Integer> periods     = new ArrayList<>();
    private final List<Units>   units       = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_charts);

        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);

        speedometer = findViewById(R.id.awesomeSpeedometer);
        turnover = findViewById(R.id.turnover);
        fuelTV = findViewById(R.id.fuelTV);
        tempTV = findViewById(R.id.tempTV);
        fuelPressureTV = findViewById(R.id.fuelpressureTV);
        oilTempTV = findViewById(R.id.oilTemp);
        consumptionTV = findViewById(R.id.consumptionTV);
        timeTV = findViewById(R.id.timeTV);
        menuButton = findViewById(R.id.menu);

        recImage = findViewById(R.id.imageView_rec);
        recImage.setImageResource(R.drawable.ic_sharp_rec_off);
        recImage.setColorFilter(Color.RED);

        customizeTurnover(turnover);
        customizeSpeedometer(speedometer);

        device = getIntent().getParcelableExtra("device");

        commands.add(new EngineCoolantTemperatureCommand());
        commands.add(new FuelLevelCommand());
        commands.add(new RPMCommand());
        commands.add(new SpeedCommand());
        commands.add(new ConsumptionRateCommand());
        commands.add(new FuelPressureCommand());
        commands.add(new OilTempCommand());

        periods.add(1000);
        periods.add(1000);
        periods.add( 500 );
        periods.add( 500 );
        periods.add( 500 );
        periods.add( 500 );
        periods.add( 500 );

        units.add(Units.TEMPERATURE);
        units.add(Units.PERCENT);
        units.add(Units.RPM);
        units.add(Units.VELOCITY);
        units.add(Units.CONSUMPTION);
        units.add(Units.PRESSURE);
        units.add(Units.TEMPERATURE);

        myThread = new DataThread(device, commands, periods, units);
        myThread.start();

        myTimer.schedule(new TimerTask() {
            @Override
            public void run() { UpdateGUI(); }
        }, 0, 1000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Toast.makeText(Real_time_charts.this, "Connecting...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Disconnected!", Toast.LENGTH_SHORT).show();
        myThread.turnOff();
    }

    // Function to check and request permission.
    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(Real_time_charts.this, permission) == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(Real_time_charts.this, new String[] { permission }, requestCode);
        }
        else {
            Toast.makeText(Real_time_charts.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void UpdateGUI() {
        myHandler.post( new Runnable() {
            @SuppressLint({"SetTextI18n", "DefaultLocale"})
            public void run() {

                myTimeDisconnector = myThread.getReadTime();
                if (System.currentTimeMillis() - myTimeDisconnector > TIME_TO_STOP) {
                    Real_time_charts.this.finish();
                }

                timeTV.setText("00:00:00");

                if (myThread.isDataReadingWorking())
                {
                    final List<DataThread.CommandData> CommandList = myThread.getData();
                    if(!CommandList.isEmpty()) {
                        String speed, rmp, fuel, temp, oilTemp, fuelPressure, consumption;
                        speed = CommandList.get(3).currentData;
                        rmp = CommandList.get(2).currentData;

                        fuel            = CommandList.get(1).currentData.isEmpty()?"-1":CommandList.get(1).currentData;
                        temp            = CommandList.get(0).currentData.isEmpty()?"-1":CommandList.get(0).currentData;
                        oilTemp         = CommandList.get(6).currentData.isEmpty()?"-1":CommandList.get(6).currentData;
                        fuelPressure    = CommandList.get(5).currentData.isEmpty()?"-1":CommandList.get(5).currentData;
                        consumption     = CommandList.get(4).currentData.isEmpty()?"-1":CommandList.get(4).currentData;

                        fuel            = (fuel.compareTo("-1")==0)?"NaN":String.format("%.1f%s", Float.valueOf(CommandList.get(1).currentData)," %");
                        temp            = (temp.compareTo("-1")==0)?"NaN":String.format("%.1f%s", Float.valueOf(CommandList.get(0).currentData)," °C");
                        oilTemp         = (oilTemp.compareTo("-1")==0)?"NaN":String.format("%.1f%s", Float.valueOf(CommandList.get(6).currentData)," °C");
                        fuelPressure    = (fuelPressure.compareTo("-1")==0)?"NaN":String.format("%.1f%s", Float.valueOf(CommandList.get(5).currentData)," Bar");
                        consumption     = (consumption.compareTo("-1")==0)?"NaN":String.format("%.1f%s", Float.valueOf(CommandList.get(4).currentData)," l/100km");

                        //fuel = String.format("%.1f%s", Float.valueOf(CommandList.get(1).currentData.isEmpty()?"0":CommandList.get(1).currentData)," %");
                        //temp = String.format("%.1f%s", Float.valueOf(CommandList.get(0).currentData.isEmpty()?"0":CommandList.get(0).currentData)," °C");
                        //oilTemp = String.format("%.1f%s", Float.valueOf(CommandList.get(6).currentData.isEmpty()?"0":CommandList.get(6).currentData), " °C");
                        //fuelPressure = String.format("%.1f%s", Float.valueOf(CommandList.get(5).currentData.isEmpty()?"0":CommandList.get(5).currentData), " Bar");
                        //consumption = String.format("%.1f%s", Float.valueOf(CommandList.get(4).currentData.isEmpty()?"0":CommandList.get(4).currentData), " l/100km");

                        speedometer.speedTo(FileUtils.findDigitis(speed));
                        turnover.speedTo(FileUtils.findDigitis(rmp)/1000.0F);
                        tempTV.setText(temp);
                        fuelTV.setText(fuel);
                        oilTempTV.setText(oilTemp);
                        fuelPressureTV.setText(fuelPressure);
                        consumptionTV.setText(consumption);

                        if(myThread.isMeasurementWorking()){
                            timeTV.setText(mFormat.format(new Date( (System.currentTimeMillis()-myTimeMeasurement) - 3600000 + 1000))); //-1H
                        }
                    }

                }else {
                    speedometer.speedTo(0.0f);
                    turnover.speedTo(0.0f);
                    tempTV.setText("NaN");
                    fuelTV.setText("NaN");
                    oilTempTV.setText("NaN");
                    fuelPressureTV.setText("NaN");
                    consumptionTV.setText("NaN");
                }
            }
        });
    }

    void customizeSpeedometer(Speedometer s)
    {
        s.setMaxSpeed(250.0f);
        s.setMinSpeed(0.0f);
        s.setUnit("km/h");
        s.setWithTremble(false);
    }

    void customizeTurnover(Speedometer s)
    {
        s.setMaxSpeed(10.0f);
        s.setMinSpeed(0.0f);
        s.setUnit("x1000RPM");
        s.setWithTremble(false);
    }

    public void onMenuClick(View view) {
        Context wrapper = new ContextThemeWrapper(this, R.style.ListFont);
        PopupMenu popupMenu = new PopupMenu(wrapper, view);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.popup_menu);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()){
            case R.id.start_item:
                startMeasurement();
                return true;
            case R.id.stop_item:
                stopMeasurement();
                return true;
            case R.id.graphs_item:
                Intent intent = new Intent(this, Graph_selection.class);
                startActivity(intent);
                return true;
            case R.id.link_item:
                openBrowser();
            default: return false;
        }
    }

    private void openBrowser() {
        Uri uri = Uri.parse("https://www.outilsobdfacile.com/vehicle-list-compatible-obd2.php");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private void startMeasurement(){
        myThread.startNewMeasurement();
        recImage.setImageResource(R.drawable.ic_sharp_rec);
        recImage.setColorFilter(Color.WHITE);
        Animation animation = new AlphaAnimation(1, 0.5F); //to change visibility from visible to invisible
        animation.setDuration(2500); //1 second duration for each animation cycle
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE); //repeating indefinitely
        animation.setRepeatMode(Animation.REVERSE); //animation will start from end point once ended.
        recImage.startAnimation(animation); //to start animation
        myTimeMeasurement = System.currentTimeMillis();
    }

    private void stopMeasurement(){
        myThread.stopMeasurement();
        recImage.setImageResource(R.drawable.ic_sharp_rec_off);
        recImage.setColorFilter(Color.RED);
        recImage.clearAnimation();
        FileUtils.Save(this, myThread, commands);
    }

    public DataThread getMyThread(){
        return myThread;
    }
}