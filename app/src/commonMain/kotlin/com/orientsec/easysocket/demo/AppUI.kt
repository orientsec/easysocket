package com.orientsec.easysocket.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orientsec.easysocket.demo.client.Client
import com.orientsec.easysocket.task.DefaultCallback
import com.orientsec.easysocket.utils.Platform

@Composable
fun AppUI() {
    var text by remember { mutableStateOf("Ready") }
    var errorTimes by remember { mutableStateOf(0) }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = text)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                Client.request("hello", object : DefaultCallback<String>() {
                    override fun onSuccess(res: String) {
                        text = res
                    }

                    override fun onFailure(t: Throwable) {
                        errorTimes++
                        text = "error: ${t.message}\n error counts:$errorTimes"
                        Platform.log(Platform.LogLevel.ERROR, "AppUI", "onFailure: ", t)
                    }
                })
            }) {
                Text("点我")
            }
        }
    }
}
