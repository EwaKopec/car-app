package com.example.obd2_app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph_selection extends AppCompatActivity implements View.OnClickListener, ListView.OnItemClickListener, RadioGroup.OnCheckedChangeListener{

     private RadioGroup    radio           ;
     private ListView      listView        ;
     private Button        buttonDisplay   ;
     private Button        buttonReset     ;
     private Button        buttonClear     ;

     private int                    selectedGraph   = 0;
     private Map<Integer, String>   selectedFile    = new HashMap<>();

     private final Real_time_charts parent          = (Real_time_charts) this.getParent();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph_selection);

        radio           = (RadioGroup) findViewById(R.id.g_radioGroup);
        listView        = (ListView) findViewById(R.id.g_listView);
        buttonDisplay   = (Button) findViewById(R.id.g_displaybutton);
        buttonReset     = (Button) findViewById(R.id.g_resetselected);
        buttonClear     = (Button) findViewById(R.id.g_clearButton);

        radio.setOnCheckedChangeListener(this::onCheckedChanged);
        listView.setOnItemClickListener(this);
        buttonDisplay.setOnClickListener(this);
        buttonReset.setOnClickListener(this);
        buttonClear.setOnClickListener(this);

        updateList();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.g_displaybutton:
                displayChart();
                break;
            case R.id.g_resetselected:
                updateList();
                break;
            case R.id.g_clearButton:
                cleanData(v);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        if(!selectedFile.containsKey(position)){
            view.setBackgroundColor(Color.LTGRAY);
            selectedFile.put(position,parent.getItemAtPosition(position).toString());
        }else{
            view.setBackgroundColor(Color.WHITE);
            selectedFile.remove(position);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId)
    {
        View radioButton    = radio.findViewById(checkedId);
        int index           = radio.indexOfChild(radioButton);
        switch (index) {
            case 0:
            case 1:
            case 2:
                selectedGraph = index;
                break;
            default:
        }
    }

    public void updateList()
    {
        ArrayList list = new ArrayList();
        selectedFile.clear();

        String path = getExternalFilesDir(null).getAbsolutePath()+"/Graphs";
        Log.d("Graph-Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();

        if(files != null)
        {
            Log.d("Graph-Files", "Size: " + files.length);
            for (int i = 0; i < files.length; i++) {
                Log.d("Graph-Files", "FileName:" + files[i].getName());
                list.add(files[i].getName());
            }

            Collections.sort(list);

            final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(this);
        }
    }

    void displayChart()
    {
        String command  = "";
        switch (selectedGraph)
        {
            case 0:
                command = new SpeedCommand().getName();
                break;
            case 1:
                command = new RPMCommand().getName();
                break;
            case 2:
                command = new EngineCoolantTemperatureCommand().getName();
                break;
            default:
        }

        List<DataThread.CommandData> data = FileUtils.Load(this,  new ArrayList<String>(selectedFile.values()) , command);

        if(data != null && !data.isEmpty()){
            Intent intent = new Intent(this, charts.class);
            intent.putExtra("data", (Serializable) data);
            startActivity(intent);
        }

    }

    private void cleanData(View view)
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Are you sure you want to delete "+selectedFile.size()+" files?");
                alertDialogBuilder.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                deleteFileData();
                                updateList();
                                return;
                            }
                        });

        alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateList();
                return;
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void deleteFileData()
    {
        String path = getExternalFilesDir(null).getAbsolutePath()+"/Graphs";
        Log.d("Graph-Files", "Delete!");
        Log.d("Graph-Files", "Path: " + path);

        if(!selectedFile.isEmpty())
        {
            for (String file : selectedFile.values())
            {
                Log.d("Graph-Files", "Delete File:" + file);
                File tmp = new File(path,file);
                if(tmp.exists())
                    tmp.delete();
            }
            selectedFile.clear();
        }
    }

}