package com.example.obd2_app;

public enum Units
{
    TEMPERATURE("°C"),
    PERCENT("%"),
    CONSUMPTION("l/100km"),
    PRESSURE("Bar"),
    RPM("r/min"),
    VELOCITY("km/h");

    String unit;

    Units(String s){
        unit = s;
    }

}
