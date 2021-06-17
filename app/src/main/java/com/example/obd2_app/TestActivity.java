package com.example.obd2_app;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class TestActivity extends AppCompatActivity implements View.OnClickListener
{
    private TextView tData1,tData2,tData3,tData4;
    private TextView tName1,tName2,tName3,tName4;
    private TextView tTime1,tTime2,tTime3,tTime4;
    private TextView tTitle;
    private Button   bDisconnect;

    private BluetoothDevice device;

    private final Timer         myTimer   = new Timer();
    private final Handler       myHandler = new Handler();
    private       DataThread    myThread;
    private       long          myTimeDisconnector = System.currentTimeMillis();

    private final long          TIME_TO_STOP = 5000; //ms

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

        device = getIntent().getParcelableExtra("device");

        myThread = new DataThread(device);
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
        Toast.makeText(this, "Disconnected!", Toast.LENGTH_SHORT).show();
        myThread.turnOff();
    }

    /*
     *
     */
    private void UpdateGUI() {
        myHandler.post( new Runnable() {
            public void run()
            {
                myTimeDisconnector = myThread.getLastReadTime();

                if(System.currentTimeMillis()-myTimeDisconnector > TIME_TO_STOP)
                {
                    TestActivity.this.finish();
                }

                if(!myThread.getSocket().isConnected()) {
                    tTitle.setText("Disconnected!");
                    tTitle.setTextColor(Color.RED);
                }else{
                    tTitle.setText("OBD - TEST");
                    tTitle.setTextColor(Color.BLACK);
                }

                final DataThread.CommandData CommandList[] = myThread.getData();

                tData1.setText(CommandList[0].data);
                tData2.setText(CommandList[1].data);
                tData3.setText(CommandList[2].data);
                tData4.setText(CommandList[3].data);
                tName1.setText(CommandList[0].command.getName());
                tName2.setText(CommandList[1].command.getName());
                tName3.setText(CommandList[2].command.getName());
                tName4.setText(CommandList[3].command.getName());
                tTime1.setText( String.valueOf(System.currentTimeMillis()-CommandList[0].time) );
                tTime2.setText( String.valueOf(System.currentTimeMillis()-CommandList[1].time) );
                tTime3.setText( String.valueOf(System.currentTimeMillis()-CommandList[2].time) );
                tTime4.setText( String.valueOf(System.currentTimeMillis()-CommandList[3].time) );
            }
        });
    }

    /*
    *
    */
    class DataThread extends Thread
    {
        boolean isWorking = true;
        long    lastRead  = System.currentTimeMillis();
        long    loop      = System.currentTimeMillis();

        private final long PERIOD = 500; //ms

        final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        BluetoothSocket socket;

        private final CommandData CommandList[] =
                {
                        new CommandData(new EngineCoolantTemperatureCommand()),
                        new CommandData(new OilTempCommand()),
                        new CommandData(new RPMCommand()),
                        new CommandData(new SpeedCommand())
                };

        public DataThread(BluetoothDevice device)
        {
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }


            if (socket != null && socket.isConnected()) {
                try {
                    new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                    new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                    new TimeoutCommand(250).run(socket.getInputStream(), socket.getOutputStream());
                    new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
                }catch (IOException | InterruptedException e) {
                    //e.printStackTrace();
                    System.out.println(e.getMessage());
                }
            }
        }

        public void run()
        {
            while (isWorking)
            {
                if(System.currentTimeMillis() - loop > PERIOD)
                {

                    if (socket != null && socket.isConnected()) {
                        for (CommandData com : CommandList) {
                            try {
                                com.command.run(socket.getInputStream(), socket.getOutputStream());
                                com.data = com.command.getCalculatedResult();
                                com.time = System.currentTimeMillis();
                            } catch (IOException | InterruptedException e) {
                                //e.printStackTrace();
                                System.out.println(e.getMessage());
                            }
                        }
                        lastRead = System.currentTimeMillis();

                    } else {
                        for (CommandData com : CommandList) {
                            com.data = "OFF";
                            com.time = System.currentTimeMillis();
                        }
                    }

                    loop = System.currentTimeMillis();
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

        public BluetoothSocket getSocket(){
            return  socket;
        }

        public CommandData[] getData(){
            return CommandList;
        }


        public long getLastReadTime(){
            return lastRead;
        }


        public void turnOff(){
            isWorking = false;
        }

        class CommandData
        {
            public ObdCommand   command;
            public String       data;
            public long         time;

            CommandData(ObdCommand command)
            {
                this.command = command;
                data = "";
                time = 0L;
            }
        }

    }



}