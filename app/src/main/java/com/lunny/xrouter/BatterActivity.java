package com.lunny.xrouter;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.xrouter.annotation.Route;

@Route(path = "/main/batter")
public class BatterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batter);
    }
}
