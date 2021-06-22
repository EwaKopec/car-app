package com.example.obd2_app;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class charts extends AppCompatActivity {
    String name;
    int period;
    List<Float> data;
    LineChart chart;
    TextView nameOfChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        data = (List<Float>) getIntent().getSerializableExtra("data");
        period = getIntent().getIntExtra("period", 1);
        name = getIntent().getStringExtra("name");

        chart = findViewById(R.id.chart);
        nameOfChart = findViewById(R.id.nameOfChart);

        nameOfChart.setText(name);
        createChart();
    }

    public void createChart()
    {
        List<Entry> entries = new ArrayList<Entry>();
        for(Float i:data){
            entries.add(new Entry(period, i));
            period += period;
        }

        LineDataSet dataSet = new LineDataSet(entries, "name"); // add entries to dataset
        dataSet.setColor(Color.MAGENTA);
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate(); // refresh
    }
}