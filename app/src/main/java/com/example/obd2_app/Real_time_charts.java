package com.example.obd2_app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.github.anastr.speedviewlib.Speedometer;

public class Real_time_charts extends AppCompatActivity {
    Speedometer speedometer, turnover;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_charts);

      speedometer = findViewById(R.id.awesomeSpeedometer);
      turnover = findViewById(R.id.turnover);
    }

    void customizeSpeedometer(Speedometer s)
    {
        //s.setBackgroundCircleColor(2);
        s.setEndDegree(250);

    }

    void customizeTurnover(Speedometer s)
    {

    }
}