package com.lunny.xrouter;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.xrouter.XRouter;
import com.xrouter.annotation.Route;

@Route(path = "/main/launcher")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button_to_shop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XRouter.navigate("shopping/main");
            }
        });
    }
}
