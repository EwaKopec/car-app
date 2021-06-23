package com.example.obd2_app;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.anastr.speedviewlib.Speedometer;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.pressure.FuelPressureCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.NonNumericResponseException;
import com.github.pires.obd.exceptions.ResponseException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Real_time_charts extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener
{
    Speedometer speedometer, turnover;
    TextView tempTV, fuelTV, oilTempTV, fuelPressureTV, consumptionTV;
    Button menuButton;

    private BluetoothDevice device;

    private final Timer         myTimer   = new Timer();
    private final Handler       myHandler = new Handler();
    private Real_time_charts.DataThread myThread;
    private       long          myTimeDisconnector = System.currentTimeMillis();

    private final long          TIME_TO_STOP = 5000; //ms

    private final List<ObdCommand> commands = new ArrayList<>();
    private final List<Integer> periods     = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_charts);

        speedometer = findViewById(R.id.awesomeSpeedometer);
        turnover = findViewById(R.id.turnover);
        fuelTV = findViewById(R.id.fuelTV);
        tempTV = findViewById(R.id.tempTV);
        fuelPressureTV = findViewById(R.id.fuelpressureTV);
        oilTempTV = findViewById(R.id.oilTemp);
        consumptionTV = findViewById(R.id.consumptionTV);
        menuButton = findViewById(R.id.menu);

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

        myThread = new Real_time_charts.DataThread(device, commands, periods);
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

    float findDigitis(String s)
    {
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(s);
        if(m.find()){
            return Float.parseFloat(m.group(0));
        }
        else return 0.0f;
    }

    private void UpdateGUI() {
        myHandler.post( new Runnable() {
            @SuppressLint({"SetTextI18n", "DefaultLocale"})
            public void run() {

                myTimeDisconnector = myThread.getReadTime();
                if (System.currentTimeMillis() - myTimeDisconnector > TIME_TO_STOP) {
                    Real_time_charts.this.finish();
                }

                if (myThread.isMeasurementWorking() && myThread.isConnected())
                {
                    final List<Real_time_charts.DataThread.CommandData> CommandList = myThread.getData();
                    if(!CommandList.isEmpty()) {
                        String speed, rmp, fuel, temp, oilTemp, fuelPressure, consumption;
                        speed = CommandList.get(3).currentData;
                        rmp = CommandList.get(2).currentData;

                        fuel = String.format("%.1f%s", Float.valueOf(CommandList.get(1).currentData.isEmpty()?"0":CommandList.get(1).currentData)," %");
                        temp = String.format("%.1f%s", Float.valueOf(CommandList.get(0).currentData.isEmpty()?"0":CommandList.get(0).currentData)," °C");
                        oilTemp = String.format("%.1f%s", Float.valueOf(CommandList.get(6).currentData.isEmpty()?"0":CommandList.get(6).currentData), " °C");
                        fuelPressure = String.format("%.1f%s", Float.valueOf(CommandList.get(5).currentData.isEmpty()?"0":CommandList.get(5).currentData), " Bar");
                        consumption = String.format("%.1f%s", Float.valueOf(CommandList.get(4).currentData.isEmpty()?"0":CommandList.get(4).currentData), " l/km");

                        speedometer.speedTo(findDigitis(speed));
                        turnover.speedTo(findDigitis(rmp)/1000.0F);
                        tempTV.setText(temp);
                        fuelTV.setText(fuel);
                        oilTempTV.setText(oilTemp);
                        fuelPressureTV.setText(fuelPressure);
                        consumptionTV.setText(consumption);
                    }

                }else{
                    speedometer.speedTo(0.0f);
                    turnover.speedTo(0.0f);
                    tempTV.setText("0 °C");
                    fuelTV.setText("0 %");
                    oilTempTV.setText("0 °C");
                    fuelPressureTV.setText("0 Bar");
                    consumptionTV.setText("0 l/km");
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
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.popup_menu);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()){
            case R.id.start_item:
                myThread.startNewMeasurement();
                return true;
            case R.id.stop_item:
                myThread.stopMeasurement();
                return true;

            case R.id.fuel_item:
                ////charts
                generateChart(1);
                return true;

            case R.id.speed_item:
                ////charts
                generateChart(3);
                return true;

            case R.id.addidional_item:
                ////charts - oil temp
                generateChart(6);
                return true;

            default: return false;
        }
    }

    void generateChart(int id_data)
    {
        final List<Real_time_charts.DataThread.CommandData> CommandList = new ArrayList<>(myThread.getData());
        List<String> data;
        int period;
        String name;
        switch(id_data){
            case 0:
               data = CommandList.get(0).data;
               name = "Temperatura płynu chłodniczego";
               period = CommandList.get(0).period; break;
            case 1:
               data = CommandList.get(1).data;
               name = "Poziom paliwa";
               period = CommandList.get(1).period; break;
            case 2:
               data = CommandList.get(2).data;
               name = "Obroty";
               period = CommandList.get(2).period; break;
            case 3:
               data = CommandList.get(3).data;
               name = "Prędkość";
               period = CommandList.get(3).period; break;
            case 4:
                data = CommandList.get(4).data;
                name = "Zużycie paliwa";
                period = CommandList.get(4).period; break;
            case 5:
                data = CommandList.get(5).data;
                name = "Ciśnienie paliwa";
                period = CommandList.get(5).period; break;
            case 6:
                data = CommandList.get(6).data;
                name = "Temperatura oleju";
                period = CommandList.get(6).period; break;
            default: data = null; period = 0; name = null;
        }

        if(!data.isEmpty()){
            List<Float> dataFloat = new ArrayList<>();
            for(String i:data)
            {
                dataFloat.add(findDigitis(i));
            }
            makeChart(period, dataFloat, name);
        }
    }

    void makeChart(int period, List<Float> data, String name) {
        Intent intent = new Intent(this, charts.class);
        intent.putExtra("name", name);
        intent.putExtra("period", period);
        intent.putExtra("data", (Serializable) data);
        startActivity(intent);
    }

    class DataThread extends Thread
    {
        private final UUID myUUID           = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private final long LOOP_PERIOD      = 10; //ms
        private final long WAIT_TIME        = 1000; //ms
        private final long PICK_THRESHOLD   = LOOP_PERIOD/5; //ms
        private final int  TIMEOUT_COMMAND  = 250; //1/4ms 250 = 1000ms
        private final ObdProtocols PROTOCOL = ObdProtocols.AUTO;

        private boolean isWorking       = true;
        private boolean stop            = false;
        private boolean wait            = false;
        private long    lastRead;
        private long    lastLoop;


        private BluetoothSocket   socket        = null;
        private List<ObdCommand> commandIO      = new ArrayList<>();
        private List<String>      lastErrors    = new ArrayList<>();
        private List<Real_time_charts.DataThread.CommandData> commandData   = new ArrayList<>();

        public DataThread(BluetoothDevice device, List<ObdCommand> commands, List<Integer> periods)
        {
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(myUUID);
                socket.connect();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                lastErrors.add(e.getMessage());
            }

            int i = 0;
            for (ObdCommand com : commands){
                commandIO.add(com);
                commandData.add(new Real_time_charts.DataThread.CommandData(com.getName(),periods.get(i++)));
            }

            if (socket != null && socket.isConnected())
            {
                try {
                    new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                    new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                    new TimeoutCommand(TIMEOUT_COMMAND).run(socket.getInputStream(), socket.getOutputStream());
                    new SelectProtocolCommand(PROTOCOL).run(socket.getInputStream(), socket.getOutputStream());
                }catch ( ResponseException | IOException | InterruptedException e) {
                    System.out.println(e.getMessage());
                    lastErrors.add(e.getMessage());
                }
            }

            for (Real_time_charts.DataThread.CommandData com : commandData){
                com.startTime = System.currentTimeMillis();
            }

            lastRead = System.currentTimeMillis();
            lastLoop = System.currentTimeMillis();
        }

        public void run()
        {
            while (isWorking)
            {
                while (stop) {
                    lastRead = System.currentTimeMillis();
                }

                while (wait) {
                    lastRead = System.currentTimeMillis();
                    if(System.currentTimeMillis() - lastLoop > WAIT_TIME) {
                        wait = false;
                        break;
                    }
                }

                if(System.currentTimeMillis() - lastLoop > LOOP_PERIOD)
                {
                    if (socket != null && socket.isConnected())
                    {
                        List<String> dataList = new ArrayList<>();

                        for (ObdCommand com : commandIO) {
                            try {
                                com.run(socket.getInputStream(), socket.getOutputStream());
                                dataList.add(com.getCalculatedResult()); //Formatted->Calculated
                            } catch (IndexOutOfBoundsException | NonNumericResponseException | ResponseException | IOException | InterruptedException e) {
                                System.out.println(e.getMessage());
                                lastErrors.add(e.getMessage());
                                dataList.add("-1");
                            }
                        }

                        int i = 0;
                        for (Real_time_charts.DataThread.CommandData com : commandData) {
                            com.currentData = dataList.get(i);
                            com.lastPickTime = System.currentTimeMillis();
                            if (com.lastPickTime - com.stopTime >= com.period - PICK_THRESHOLD) {
                                com.data.add(com.currentData);
                                com.stopTime = com.lastPickTime;
                            }
                            i++;
                        }

                        lastRead = System.currentTimeMillis();
                    }
                    else {
                        for (Real_time_charts.DataThread.CommandData com : commandData) {
                            com.currentData = "-1";
                            if (System.currentTimeMillis() - com.stopTime >= com.period - PICK_THRESHOLD) {
                                com.data.add(com.currentData);
                                com.stopTime = System.currentTimeMillis();
                            }
                        }
                    }
                    lastLoop = System.currentTimeMillis();
                }

            }

            if (socket != null && socket.isConnected()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        public void startNewMeasurement()
        {
            wait        = true;

            for (Real_time_charts.DataThread.CommandData com : commandData){
                com.clearData();
            }
            lastErrors      = new ArrayList<>();

            stop            = false;
        }

        public void stopMeasurement()
        {
            stop = true;
        }

        public void resetMeasurement(List<ObdCommand> commands, List<Integer> periods)
        {
            wait        = true;

            int i = 0;
            commandIO    = new ArrayList<>();
            commandData  = new ArrayList<>();
            lastErrors   = new ArrayList<>();
            for (ObdCommand com : commands){
                commandIO.add(com);
                commandData.add(new Real_time_charts.DataThread.CommandData(com.getName(),periods.get(i++)));
            }
            for (Real_time_charts.DataThread.CommandData com : commandData){
                com.startTime = System.currentTimeMillis();
            }

            stop = false;
        }

        public BluetoothSocket getSocket(){
            return  socket;
        }

        public boolean isConnected() {
            return socket.isConnected();
        }

        public boolean isThreadLive() {
            return isWorking;
        }

        public boolean isMeasurementWorking() {
            return !wait&&!stop;
        }

        public List<Real_time_charts.DataThread.CommandData> getData(){
            return commandData;
        }

        public Real_time_charts.DataThread.CommandData getData(int index){
            return commandData.get(index);
        }

        public long getReadTime(){
            return lastRead;
        }

        public void turnOff(){
            isWorking = false;
        }

        public List<String> getErrors(){
            return lastErrors;
        }

        public String getLastError(){
            return lastErrors.remove(lastErrors.size()-1);
        }

        class CommandData
        {
            public String           commandName;
            public List<String>     data;
            public String           currentData;
            public int              period;             //ms
            public long             startTime;
            public long             stopTime;
            public long             lastPickTime;

            CommandData(String command, int period)
            {
                this.data           = new ArrayList<>();
                this.commandName    = command;
                this.startTime      = System.currentTimeMillis();
                this.stopTime       = System.currentTimeMillis();
                this.period         = period;
                this.currentData    = "";
                this.lastPickTime   = System.currentTimeMillis();
            }

            public void clearData(){
                this.data           = new ArrayList<>();
                this.startTime      = System.currentTimeMillis();
                this.stopTime       = System.currentTimeMillis();
                this.currentData    = "";
                this.lastPickTime   = System.currentTimeMillis();
            }
        }
    }
}