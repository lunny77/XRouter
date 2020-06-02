package com.lunny.shoppingmodule;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.xrouter.annotation.Route;

@Route(path = "/shop/list")
public class ShoppingListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);
    }
}
