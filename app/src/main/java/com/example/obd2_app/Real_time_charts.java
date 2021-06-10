package com.example.obd2_app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.github.anastr.speedviewlib.Speedometer;

public class Real_time_charts extends AppCompatActivity {
    Speedometer speedometer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_charts);

      speedometer = findViewById(R.id.awesomeSpeedometer);
    }
}