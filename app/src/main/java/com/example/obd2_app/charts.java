package com.example.obd2_app;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class charts extends AppCompatActivity {
    String name;
    int period;
    float time;
    ArrayList<Float> data;
    LineChart chart;
    TextView nameOfChart;

    private final int MAX_POINT_NUMBER = 5; //+2 stat,end

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        data = (ArrayList<Float>) getIntent().getSerializableExtra("data");
        period = getIntent().getIntExtra("period", 1000);
        name = getIntent().getStringExtra("name");

        chart = findViewById(R.id.chart);
        nameOfChart = findViewById(R.id.nameOfChart);

        nameOfChart.setText(name);
        createChart();
    }

    public void createChart()
    {
        List<Entry> entries = new ArrayList<>();
        time = 0;
        for(Float i:data){
            entries.add(new Entry(time, i));
            time += (float) (period/1000.0F); //ms->s
        }

        /*int step = data.size() / MAX_POINT_NUMBER;
        for (int i = 0 ; i<data.size() ; i += step){
            entries.add(new Entry(time, data.get(i)));
            time += (float) (period/1000.0F)*step; //ms->s
        }
        entries.add(new Entry(time, data.get(data.size()-1)));*/

        //scaling the data
        scaleDataXY(period, data);

        LineDataSet dataSet = new LineDataSet(entries, name); // add entries to dataset
        dataSet.setMode(LineDataSet.Mode.STEPPED);
        dataSet.setColor(Color.MAGENTA);
        LineData lineData = new LineData(dataSet);

        // enable touch gestures
        chart.setTouchEnabled(true);
        chart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setHighlightPerDragEnabled(true);

        chart.setData(lineData);
        chart.invalidate(); // refresh
    }

    void scaleDataXY(float period, ArrayList<Float> data){
        long number = (long) (period * data.size());
        long seconds = (number/1000);

        XAxis xAxis = chart.getXAxis();
        xAxis.setAxisMaximum(seconds);
        xAxis.setAxisMinimum(0);
        xAxis.setSpaceMin(seconds/MAX_POINT_NUMBER);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        Float [] floatData = data.toArray(new Float[0]);

        /*int i = 0;
        for(Float f:data){
            floatData[i] = f;
            i++;
        }*/

        Arrays.sort(floatData);
        float maxToInit = floatData[floatData.length-1] + 10;
        float minToInit = floatData[0] - 5;
        if(minToInit < 0 ) minToInit = 0;
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMaximum(maxToInit);
        yAxis.setAxisMinimum(minToInit);
    }
}