package com.lunny.shoppingmodule;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.xrouter.annotation.Route;

@Route(path = "shopping/main")
public class ShoppingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping);
    }
}
