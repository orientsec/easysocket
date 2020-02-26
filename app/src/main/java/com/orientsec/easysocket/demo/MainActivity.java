package com.orientsec.easysocket.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.orientsec.easysocket.demo.client.Client;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private int errorTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.text);
        findViewById(R.id.button).setOnClickListener(v -> Client.getInstance().request("hello")
                .subscribe(s -> textView.setText(s),
                        e -> {
                            e.printStackTrace();
                            errorTimes++;
                            textView.setText("error:" + errorTimes);
                        }));
    }
}
