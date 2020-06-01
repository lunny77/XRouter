package com.lunny.usermodule;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.xrouter.annotation.Route;

@Route(path = "user/user")
public class UserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
    }
}
