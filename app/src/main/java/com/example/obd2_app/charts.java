package com.example.obd2_app;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class charts extends AppCompatActivity {
    String name;
    float period;
    ArrayList<Float> data;
    LineChart chart;
    TextView nameOfChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        data = (ArrayList<Float>) getIntent().getSerializableExtra("data");
        period = getIntent().getIntExtra("period", 1);
        name = getIntent().getStringExtra("name");

        chart = findViewById(R.id.chart);
        nameOfChart = findViewById(R.id.nameOfChart);

        nameOfChart.setText(name);
        createChart();
    }

    public void createChart()
    {
        List<Entry> entries = new ArrayList<>();
        period /= period/1000;
        for(Float i:data){
            entries.add(new Entry(period, i));
            period += period;
        }

        //scaling the data
        scaleDataXY(period, data);

        LineDataSet dataSet = new LineDataSet(entries, name); // add entries to dataset
        dataSet.setColor(Color.MAGENTA);
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate(); // refresh
    }

    void scaleDataXY(float period, ArrayList<Float> data){
        long number = (long) (period * data.size());
        long seconds = number/1000;

        XAxis xAxis = chart.getXAxis();
        xAxis.setAxisMaximum(seconds);
        xAxis.setSpaceMin(seconds/1000);
        xAxis.setSpaceMax(seconds/100);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        Float [] floatData = new Float[data.size()];
        int i = 0;
        for(Float f:data){
            floatData[i] = f;
            i++;
        }

        Arrays.sort(floatData);
        float maxToInit = floatData[floatData.length-1] + 10;
        float minToInit = floatData[0] - 5;
        if(minToInit < 0 ) minToInit = 0;
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMaximum(maxToInit);
        yAxis.setAxisMinimum(minToInit);

    }
}