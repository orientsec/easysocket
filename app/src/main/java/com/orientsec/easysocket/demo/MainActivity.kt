package com.orientsec.easysocket.demo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.orientsec.easysocket.demo.client.Client
import com.orientsec.easysocket.task.DefaultCallback

class MainActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private var errorTimes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.text)
        val button = findViewById<Button>(R.id.button)
        button.text = "点我"
        button.setOnClickListener {
            Client.request("hello", Callback())
        }
    }

    inner class Callback : DefaultCallback<String>() {
        override fun onSuccess(res: String) {
            textView.text = res
        }

        override fun onFailure(t: Throwable) {
            errorTimes++
            val msg = "error: ${t.message}\n error counts:$errorTimes"
            textView.text = msg
            Log.e("MainActivity", "onFailure: ", t)
        }
    }
}
