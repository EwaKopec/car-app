package com.example.obd2_app;

import android.content.Context;
import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils
{
    public static final String TAG = "File-Utils";
    public static final String ABSOLUTE_PATH = context.getExternalFilesDir(null).getAbsolutePath();
    public static List<DataThread.CommandData> Load(Context context, ArrayList<String> files, String commandName)
    {
        List<DataThread.CommandData> newData = new ArrayList<>();

        for (String file : files)
        {
            File path = new File(ABSOLUTE_PATH+"/Graphs/"+file);
            FileInputStream stream = null;

            try {
                stream = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "Error: " + e.getMessage());
                continue;
            }

            try {
                byte b = 0;
                StringBuilder res = new StringBuilder();
                // -1 if the end of the stream is reached
                while (((b = (byte) stream.read()) > -1)) {
                    res.append((char) b);
                }

                String[] all = res.toString().split("\n");
                for (String dat : all)
                {
                    String[] data = dat.split(",");
                    if(data.length > 5) {
                        if (data[0].compareTo(commandName) == 0) {
                            DataThread.CommandData tmp = new DataThread.CommandData(data[0], Integer.valueOf(data[1]), Units.valueOf(data[2]));
                            tmp.startTime = Long.valueOf(data[3]);
                            tmp.stopTime  = Long.valueOf(data[4]);
                            for (int i = 5; i < data.length; i++) {
                                tmp.data.add(data[i]);
                            }
                            newData.add(tmp);
                        }
                    }
                }

            } catch (IOException e) {
                Log.d(TAG, "Error: " + e.getMessage());
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.d(TAG, "Error: " + e.getMessage());
                }
            }
        }

        return  newData;
    }

    public static boolean Save(Context context, DataThread thread, List<ObdCommand> commands)
    {
        final List<DataThread.CommandData> CommandList = new ArrayList<>(thread.getData());

        // Creating date format
        DateFormat simple = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z");

        File path = new File(ABSOLUTE_PATH+"/Graphs");
        if(!path.exists()) path.mkdir();
        File file = new File(path, simple.format(new Date(CommandList.get(0).startTime)));
        FileOutputStream stream = null;

        try {
            stream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "Error: " + e.getMessage());
            return false;
        }

        if(stream == null)
            return false;

        try {
            for (DataThread.CommandData data : CommandList) {
                if (data.commandName.compareTo(commands.get(0).getName()) == 0 || data.commandName.compareTo(commands.get(2).getName()) == 0 ||data.commandName.compareTo(commands.get(3).getName()) == 0 )
                {
                    stream.write(data.commandName.getBytes());
                    stream.write(",".getBytes());
                    stream.write(String.valueOf(data.period).getBytes());
                    stream.write(",".getBytes());
                    stream.write(data.unit.name().getBytes());
                    stream.write(",".getBytes());
                    stream.write(String.valueOf(data.startTime).getBytes());
                    stream.write(",".getBytes());
                    stream.write(String.valueOf(data.stopTime).getBytes());
                    stream.write(",".getBytes());
                    for (String S : data.data) {
                        stream.write(S.getBytes());
                        stream.write(",".getBytes());
                    }
                    stream.write("\n".getBytes());
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "Error: " + e.getMessage());
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                Log.d(TAG, "Error: " + e.getMessage());
            }
        }

        return true;
    }

    public static float findDigitis(String s)
    {
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(s);
        if(m.find()){
            return Float.parseFloat(m.group(0));
        }
        else return 0.0f;
    }
}
