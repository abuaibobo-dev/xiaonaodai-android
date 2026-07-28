package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.align
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.clickable
import androidx.compose.ui.graphics.shape.CircleShape

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GameScreen()
            }
        }
    }

    @Composable
    fun GameScreen() {
        var score by remember { mutableStateOf(0) }
        var x by remember { mutableStateOf(0) }
        var y by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                x = ((Math.random() * 400) - 200).toInt()
                y = ((Math.random() * 400) - 200).toInt()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .offset(x = x.dp, y = y.dp)
                        .size(80.dp)
                        .background(Color.Blue, shape = CircleShape)
                        .clickable {
                            score++
                            x = ((Math.random() * 400) - 200).toInt()
                            y = ((Math.random() * 400) - 200).toInt()
                        }
                )
            }
        }
    }
}