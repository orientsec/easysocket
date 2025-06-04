package com.orientsec.easysocket.demo;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.orientsec.easysocket.demo.client.Client;
import com.orientsec.easysocket.task.DefaultCallback;

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
        button.setOnClickListener(v ->
                Client.getInstance().request("hello", new Callback()));
    }

    class Callback extends DefaultCallback<String> {
        @Override
        public void onSuccess(@NonNull String res) {
            textView.setText(res);
        }

        @Override
        public void onFailure(@NonNull Throwable t) {
            errorTimes++;
            String msg = "error: " + t.getMessage() + "\n error counts:" + errorTimes;
            textView.setText(msg);
            Log.e("MainActivity", "onFailure: ", t);
        }
    }
}
