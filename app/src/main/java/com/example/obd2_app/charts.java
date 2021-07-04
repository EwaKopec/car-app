package com.example.obd2_app;

import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class charts extends AppCompatActivity
{
    private String  name;
    private int     period;

    private List<DataThread.CommandData> data;
    private LineChart chart;
    private TextView nameOfChart;//, xlabel, ylabel;

    private final int MAX_POINT_NUMBER = 5;

    private List<Float> entriesTimes = new ArrayList<>();

    // Creating date format
    private DateFormat simple = new java.text.SimpleDateFormat("dd MMM yyyy HH:mm:ss Z");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        data    = (List<DataThread.CommandData>) getIntent().getSerializableExtra("data");
        period  = data.get(0).period; //first, we assume for now that they are the same
        name    = data.get(0).commandName;

        chart       = findViewById(R.id.chart);
        nameOfChart = findViewById(R.id.nameOfChart);
        //xlabel      = findViewById(R.id.Xlabel);
        //ylabel      = findViewById(R.id.Ylabel);

        nameOfChart.setText(name);
        //ylabel.setText(name);
        createChart();
    }

    public void createChart()
    {
        List<LineDataSet> dataSets = new ArrayList<>();

        float min       = Float.MAX_VALUE;
        float max       = 0.0F;
        float maxTime   = 0;
        float minTime   = Float.MAX_VALUE;

        for (DataThread.CommandData dat : data)
        {
            List<Entry> entries   = new ArrayList<>();
            List<Float> dataFloat = new ArrayList<>();

            double sum            = 0.0;

            for (String i : dat.data) {
                dataFloat.add(FileUtils.findDigitis(i));
            }

            float time = 0;
            for(Float i : dataFloat){
                entries.add(new Entry(time, i));
                time += ((float)period/1000.0F); //ms->s

                if(min>i) min = i;
                if(max<i) max = i;
                sum += i;
            }
            if(maxTime < time)
                maxTime = time;
            if(minTime > time)
                minTime = time;
            //entriesTimes.add(time);

            // Will produce only bright / light colours:
            int color = ((int)(Math.random()*16777215)) | (0xFF << 24);

            LineDataSet dataSet = new LineDataSet(entries, simple.format(new Date(dat.startTime)) + " (AVG: "+(float)(sum/dataFloat.size())+dat.unit.getUnitSymbol()+" )"); // add entries to dataset
            dataSet.setDrawCircles(false);
            dataSet.setMode(LineDataSet.Mode.STEPPED);
            dataSet.setColor(color);
            dataSet.setLineWidth(3.0f);
            dataSet.setFillAlpha(255);
            dataSet.setFillColor(color);

            dataSets.add(dataSet);
        }

        //scaling the data
        scaleDataXY(maxTime, minTime, max, min, data.get(0).unit.unit);

        // enable touch gestures
        chart.setTouchEnabled(true);
        chart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setHighlightPerDragEnabled(true);

        Description description = chart.getDescription();
        // enable or disable the description
        description.setEnabled(false);
        // set the description text
        //description.setText("Zmiany parametru " + name + " w czasie");
        // set the position of the description on the screen
        //description.setPosition(15, 320);

        // create a data object with the data sets
        LineData lineData = new LineData();
        for (LineDataSet line : dataSets)
        {
            lineData.addDataSet(line);
        }

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();
        // modify the legend ...
        l.setForm(Legend.LegendForm.CIRCLE);
        l.setTextSize(8f);
        l.setTextColor(Color.BLACK);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setExtra(new int[]{Color.WHITE}, new String[]{""});
        l.setDrawInside(false);

        chart.setData(lineData);
        chart.invalidate(); // refresh
    }

    void scaleDataXY(float maxTime, float minTime, float max, float min, String unit)
    {
        XAxis xAxis = chart.getXAxis();
        xAxis.setAxisMaximum(maxTime);
        xAxis.setAxisMinimum(0);
        xAxis.setSpaceMin((minTime)/(float)MAX_POINT_NUMBER);
        xAxis.setSpaceMax((maxTime)/(float)MAX_POINT_NUMBER);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
            @Override
            public String getFormattedValue(float value) {
                long millis = TimeUnit.SECONDS.toMillis((long) value);
                return mFormat.format(new Date(millis-3600000)); //-1H
            }
        });

        float maxToInit = max + 10;
        float minToInit = min - 10;
        if(minToInit < 0 ) minToInit = 0;
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMaximum(maxToInit);
        yAxis.setAxisMinimum(minToInit);
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf(value)+unit;
            }
        });

        YAxis yAxisRight = chart.getAxisRight();
        yAxisRight.setEnabled(false);
    }
}