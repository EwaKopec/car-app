package com.example.obd2_app;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.NonNumericResponseException;
import com.github.pires.obd.exceptions.ResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class TestActivity extends AppCompatActivity implements View.OnClickListener
{
    private TextView tData1,tData2,tData3,tData4;
    private TextView tName1,tName2,tName3,tName4;
    private TextView tTime1,tTime2,tTime3,tTime4;
    private TextView tTitle;
    private Button   bDisconnect, bStart, bStop, bReset;

    private BluetoothDevice device;

    private final Timer             myTimer   = new Timer();
    private final Handler           myHandler = new Handler();
    private       TestDataThread    myThread;
    private       long              myTimeDisconnector = System.currentTimeMillis();

    private final long              TIME_TO_STOP = 5000; //ms

    private  List<ObdCommand> commands = new ArrayList<>();
    private  List<Integer> periods     = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        tTitle= findViewById(R.id.t_t_title);
        tData1= findViewById(R.id.t_t_d1);
        tData2= findViewById(R.id.t_t_d2);
        tData3= findViewById(R.id.t_t_d3);
        tData4= findViewById(R.id.t_t_d4);
        tName1= findViewById(R.id.t_t_n1);
        tName2= findViewById(R.id.t_t_n2);
        tName3= findViewById(R.id.t_t_n3);
        tName4= findViewById(R.id.t_t_n4);
        tTime1= findViewById(R.id.t_t_t1);
        tTime2= findViewById(R.id.t_t_t2);
        tTime3= findViewById(R.id.t_t_t3);
        tTime4= findViewById(R.id.t_t_t4);
        bDisconnect= findViewById(R.id.t_b_disconnect);
        bDisconnect.setOnClickListener(this);
        bStart= findViewById(R.id.t_b_start);
        bStart.setOnClickListener(this);
        bStop= findViewById(R.id.t_b_stop);
        bStop.setOnClickListener(this);
        bReset= findViewById(R.id.t_b_reset);
        bReset.setOnClickListener(this);

        device = getIntent().getParcelableExtra("device");

        commands.add(new EngineCoolantTemperatureCommand());
        commands.add(new FuelLevelCommand());
        commands.add(new RPMCommand());
        commands.add(new SpeedCommand());
        periods.add(1000 );
        periods.add(10000);
        periods.add( 500 );
        periods.add( 500 );

        myThread = new TestDataThread(device, commands, periods);
        myThread.start();

        myTimer.schedule(new TimerTask() {
            @Override
            public void run() { UpdateGUI(); }
        }, 0, 1000);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.t_b_disconnect:
                this.finish();
                break;
            case R.id.t_b_start:
                myThread.startNewMeasurement();
                break;
            case R.id.t_b_stop:
                myThread.stopMeasurement();
                break;
            case R.id.t_b_reset:
                myThread.resetMeasurement(commands, periods);
                break;
        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Disconnected!", Toast.LENGTH_SHORT).show();
        myThread.turnOff();
    }

    /*
     *
     */
    private void UpdateGUI() {
        myHandler.post( new Runnable() {
            public void run() {

                myTimeDisconnector = myThread.getReadTime();
                if (System.currentTimeMillis() - myTimeDisconnector > TIME_TO_STOP) {
                    TestActivity.this.finish();
                }

                if (myThread.isMeasurementWorking())
                {
                    if (!myThread.isConnected()) {
                        tTitle.setText("Disconnected!");
                        tTitle.setTextColor(Color.RED);
                    } else {
                        tTitle.setText("OBD - TEST");
                        tTitle.setTextColor(Color.BLACK);
                    }

                    final List<TestDataThread.CommandData> CommandList = myThread.getData();
                    tData1.setText(CommandList.get(0).currentData);
                    tData2.setText(CommandList.get(1).currentData);
                    tData3.setText(CommandList.get(2).currentData);
                    tData4.setText(CommandList.get(3).currentData);
                    tName1.setText(CommandList.get(0).commandName);
                    tName2.setText(CommandList.get(1).commandName);
                    tName3.setText(CommandList.get(2).commandName);
                    tName4.setText(CommandList.get(3).commandName);
                    tTime1.setText(String.valueOf(System.currentTimeMillis() - CommandList.get(0).lastPickTime) + " | " + String.valueOf(System.currentTimeMillis() - CommandList.get(0).stopTime));
                    tTime2.setText(String.valueOf(System.currentTimeMillis() - CommandList.get(1).lastPickTime) + " | " + String.valueOf(System.currentTimeMillis() - CommandList.get(1).stopTime));
                    tTime3.setText(String.valueOf(System.currentTimeMillis() - CommandList.get(2).lastPickTime) + " | " + String.valueOf(System.currentTimeMillis() - CommandList.get(2).stopTime));
                    tTime4.setText(String.valueOf(System.currentTimeMillis() - CommandList.get(3).lastPickTime) + " | " + String.valueOf(System.currentTimeMillis() - CommandList.get(3).stopTime));
                }else{
                    tData1.setText("0");
                    tData2.setText("0");
                    tData3.setText("0");
                    tData4.setText("0");
                    tName1.setText("0");
                    tName2.setText("0");
                    tName3.setText("0");
                    tName4.setText("0");
                    tTime1.setText("0");
                    tTime2.setText("0");
                    tTime3.setText("0");
                    tTime4.setText("0");
                }
            }
        });
    }

    // Generate Chart example
    /* Generate Chart example

    void generateChart(int id_data)
    {
        final List<DataThread.CommandData> CommandList = new ArrayList<>(myThread.getData());
        List<String> data = null;
        int period = 0;
        String name;

        switch(id_data){
            case 0:
                name = "Engine Coolant Temperature";
                break;
            case 1:
                name = "Fuel";
                break;
            case 2:
                name = "RPM";
                break;
            case 3:
                name = "Speed";
                break;
            case 4:
                name = "Consumption";
                break;
            case 5:
                name = "Pressure of fuel";
                break;
            case 6:
                name = "Temperature of oil";
                break;
            default: name = "Null";
        }

        if (CommandList.size() > id_data) {
            data = new ArrayList<>(CommandList.get(id_data).data);
            period = CommandList.get(id_data).period;

            if (!data.isEmpty()) {
                List<Float> dataFloat = new ArrayList<>();
                for (String i : data) {
                    dataFloat.add(FileUtils.findDigitis(i));
                }
                makeChart(period, dataFloat, name);
            }
        }
    }

    void makeChart(int period, List<Float> data, String name) {
        Intent intent = new Intent(this, charts.class);
        intent.putExtra("name", name);
        intent.putExtra("period", period);
        intent.putExtra("data", (Serializable) data);
        startActivity(intent);
    }

    */

    /*
    *
    */

    class TestDataThread extends Thread
    {
        private final UUID myUUID           = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private final long LOOP_PERIOD      = 10; //ms
        private final long PICK_THRESHOLD   = LOOP_PERIOD/5; //ms
        private final int  TIMEOUT_COMMAND  = 250; //1/4ms 250 = 1000ms
        private final ObdProtocols PROTOCOL = ObdProtocols.AUTO;

        public final String ERROR_DATA_IN_SIZE = "Bad amount of data received ";

        private boolean isWorking       = true;
        private boolean isStopped       = false;
        private boolean wait            = false;
        private long    lastRead        = System.currentTimeMillis();
        private long    lastLoop        = System.currentTimeMillis();


        private BluetoothSocket   socket        = null;
        private List<ObdCommand>  commandIO     = new ArrayList<>();
        private List<CommandData> commandData   = new ArrayList<>();
        private List<String>      lastErrors    = new ArrayList<>();

        public TestDataThread(BluetoothDevice device, List<ObdCommand> commands, List<Integer> periods)
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
                commandData.add(new CommandData(com.getName(),periods.get(i++)));
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

            for (CommandData com : commandData){
                com.startTime = System.currentTimeMillis();
            }

            lastRead = System.currentTimeMillis();
            lastLoop = System.currentTimeMillis();
        }

        public void run()
        {
            while (isWorking)
            {
                while (wait) {
                    isStopped = true;
                    lastRead = System.currentTimeMillis();
                }

                if(System.currentTimeMillis() - lastLoop > LOOP_PERIOD)
                {
                    if (socket != null && socket.isConnected())
                    {
                        List<String> dataList = new ArrayList<>();

                        for (ObdCommand com : commandIO) {
                            try {
                                com.run(socket.getInputStream(), socket.getOutputStream());
                                dataList.add(com.getFormattedResult());
                            } catch (IndexOutOfBoundsException | NonNumericResponseException | ResponseException | IOException | InterruptedException e) {
                                System.out.println(e.getMessage());
                                lastErrors.add(e.getMessage());
                                dataList.add("-1");
                            }
                        }

                        int i = 0;
                        for (CommandData com : commandData) {
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
                        for (CommandData com : commandData) {
                            com.currentData = "-1";
                            if (System.currentTimeMillis() - com.stopTime >= com.period - PICK_THRESHOLD) {
                                com.data.add(com.currentData);
                                com.stopTime = System.currentTimeMillis();
                            }
                        }
                    }
                    lastLoop = System.currentTimeMillis();
                }

                isStopped = false;
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
            while (!isStopped) {;};
            for (CommandData com : commandData){
                com.clearData();
            }
            lastErrors    = new ArrayList<>();
            wait = false;
        }

        public void stopMeasurement()
        {
            wait = true;
        }

        public void resetMeasurement(List<ObdCommand> commands, List<Integer> periods)
        {
            wait        = true;
            while (!isStopped) {;};
            int i = 0;
            commandIO    = new ArrayList<>();
            commandData  = new ArrayList<>();
            lastErrors   = new ArrayList<>();
            for (ObdCommand com : commands){
                commandIO.add(com);
                commandData.add(new CommandData(com.getName(),periods.get(i++)));
            }
            for (CommandData com : commandData){
                com.startTime = System.currentTimeMillis();
            }
            wait = false;
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
            return !isStopped;
        }

        public List<CommandData> getData(){
            return commandData;
        }

        public CommandData getData(int index){
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