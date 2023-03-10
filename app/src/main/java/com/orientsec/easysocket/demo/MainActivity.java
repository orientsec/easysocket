package com.orientsec.easysocket.demo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.orientsec.easysocket.demo.client.Client;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private int errorTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.text);
        Button button = findViewById(R.id.button);
        button.setText("点我");
        button.setOnClickListener(v -> Client.getInstance().request("hello")
                .subscribe(s -> textView.setText(s),
                        e -> {
                            e.printStackTrace();
                            errorTimes++;
                            textView.setText("error:" + errorTimes);
                        }));
    }
}
