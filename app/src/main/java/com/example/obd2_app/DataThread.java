package com.example.obd2_app;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.NonNumericResponseException;
import com.github.pires.obd.exceptions.ResponseException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataThread extends Thread
{
    private final UUID myUUID           = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final long LOOP_PERIOD      = 5; //ms
    private final long WAIT_TIME        = 1000; //ms
    private final long PICK_THRESHOLD   = 10; //ms
    private final int  TIMEOUT_COMMAND  = 125; //1/4ms 250 = 1000ms
    private final ObdProtocols PROTOCOL = ObdProtocols.AUTO;

    private boolean isWorking       = true;
    private boolean stop            = true;
    private boolean wait            = false;
    private long    lastRead;
    private long    lastLoop;


    private BluetoothSocket socket        = null;
    private List<ObdCommand> commandIO    = new ArrayList<>();
    //private List<String>      lastErrors    = new ArrayList<>();
    private List<DataThread.CommandData> commandData   = new ArrayList<>();

    public DataThread(BluetoothDevice device, List<ObdCommand> commands, List<Integer> periods, List<Units> units)
    {
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(myUUID);
            socket.connect();
        } catch (IOException e) {
            Log.d("DataThread", "Error: " + e.getMessage());
        }

        int i = 0;
        for (ObdCommand com : commands){
            commandIO.add(com);
            commandData.add(new DataThread.CommandData(com.getName(),periods.get(i), units.get(i++)));
        }

        if (socket != null && socket.isConnected())
        {
            try {
                new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                new TimeoutCommand(TIMEOUT_COMMAND).run(socket.getInputStream(), socket.getOutputStream());
                new SelectProtocolCommand(PROTOCOL).run(socket.getInputStream(), socket.getOutputStream());
            }catch ( ResponseException | IOException | InterruptedException e) {
                Log.d("DataThread", "Error: " + e.getMessage());
            }
        }

        for (DataThread.CommandData com : commandData){
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
                            Log.d("DataThread", "Error: " + e.getMessage());
                            dataList.add("-1");
                        }
                    }

                    int i = 0;
                    for (DataThread.CommandData com : commandData) {
                        com.currentData = dataList.get(i);
                        com.lastPickTime = System.currentTimeMillis();
                        if ( (com.lastPickTime - com.stopTime) >= (com.period - PICK_THRESHOLD) && !stop) {
                            com.data.add(com.currentData);
                            com.stopTime = com.startTime + com.period*com.data.size();
                        }
                        i++;
                    }

                    lastRead = System.currentTimeMillis();
                }
                else {
                    for (DataThread.CommandData com : commandData) {
                        com.currentData = "-1";
                        if ( (System.currentTimeMillis() - com.stopTime) >= (com.period - PICK_THRESHOLD) && !stop) {
                            com.data.add(com.currentData);
                            com.stopTime = System.currentTimeMillis();
                        }
                    }
                }

                lastLoop = System.currentTimeMillis();
            }
        }

        if (socket != null && socket.isConnected())
        {
            try {
                socket.close();
            } catch (IOException e) {
                Log.d("DataThread", "Error: " + e.getMessage());
            }
        }
    }

    public void startNewMeasurement()
    {
        if(!stop)
            return;

        //lastErrors      = new ArrayList<>();
        for (DataThread.CommandData com : commandData){
            com.clearData();
        }

        stop            = false;
    }

    public void stopMeasurement()
    {
        stop = true;
    }

    public void resetMeasurement(List<ObdCommand> commands, List<Integer> periods, List<Units> units)
    {
        wait        = true;

        int i = 0;
        commandIO    = new ArrayList<>();
        commandData  = new ArrayList<>();
        //lastErrors   = new ArrayList<>();
        for (ObdCommand com : commands){
            commandIO.add(com);
            commandData.add(new DataThread.CommandData(com.getName(),periods.get(i), units.get(i++)));
        }
        for (DataThread.CommandData com : commandData){
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

    public boolean isDataReadingWorking() {
        return !wait&&isConnected();
    }

    public List<DataThread.CommandData> getData(){
        return commandData;
    }

    public DataThread.CommandData getData(int index){
        return commandData.get(index);
    }

    public long getReadTime(){
        return lastRead;
    }

    public void turnOff(){
        isWorking = false;
    }

    /*
    public List<String> getErrors(){
        return lastErrors;
    }

    public String getLastError(){
        return lastErrors.remove(lastErrors.size()-1);
    }
    */

    public static class CommandData implements Serializable
    {
        public String           commandName;
        public List<String>     data;
        public String           currentData;
        public Units            unit;
        public int              period;             //ms
        public long             startTime;
        public long             stopTime;
        public long             lastPickTime;

        CommandData(String command, int period, Units unit)
        {
            this.data           = new ArrayList<>();
            this.commandName    = command;
            this.startTime      = System.currentTimeMillis();
            this.stopTime       = System.currentTimeMillis();
            this.period         = period;
            this.currentData    = "";
            this.unit           = unit;
            this.lastPickTime   = System.currentTimeMillis();
        }

        public void clearData(){
            this.data           = new ArrayList<>();
            this.startTime      = System.currentTimeMillis();
            this.stopTime       = System.currentTimeMillis();
        }
    }
}
