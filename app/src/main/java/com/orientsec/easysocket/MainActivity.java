package com.orientsec.easysocket;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private int errorTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.text);
        findViewById(R.id.button).setOnClickListener(v -> Client.getInstance().request("hello")
                .subscribe(s -> {
                    ConnectionInfo info = Client.getInstance().connection.getConnectionInfo();
                    textView.setText("from host:" + info.getHost() + " port:" + info.getPort() + "\n" + s);
                }, e -> {
                    errorTimes++;
                    textView.setText("error:" + errorTimes);
                }));
    }
}
