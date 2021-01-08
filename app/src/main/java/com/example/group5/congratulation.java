package com.example.group5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class congratulation extends AppCompatActivity {
    String events = "";
    TextView display;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.congratulation);
        Intent intent = getIntent();
        Bundle extra = intent.getBundleExtra("bundle");
        ArrayList<String> events_pass = (ArrayList<String>) extra.getSerializable("events_pass");
        for(int i = 0; i<events_pass.size(); ++i){
            events += events_pass.get(i);
        }
        display = findViewById(R.id.events_pass);
        display.setText(events.toString());
    }
}