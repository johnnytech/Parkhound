package com.parkhound.spuploader;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;

import com.google.android.gms.maps.model.LatLng;


public class MainActivity extends FragmentActivity {
    static final LatLng melbourne = new LatLng(-37.815, 144.966);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentTabHost tabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
        tabHost.setup(this, getSupportFragmentManager(), R.id.realTabContent);
        tabHost.addTab(tabHost.newTabSpec("LINE").setIndicator("LINE"), LineFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec("METER").setIndicator("METER"), MeterFragment.class, null);
        tabHost.addTab(tabHost.newTabSpec("CARPARK").setIndicator("CARPARK"), CarparkFragment.class, null);
    }

    public String getLineData(){
        return "Line";
    }
    public String getMeterData(){
        return "Meter";
    }
    public String getCarparkData(){
        return "Carpark";
    }
}
