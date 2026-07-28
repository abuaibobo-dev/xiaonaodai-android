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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background.background
import androidx.compose.foundation.clickable.clickable
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen() {
    var score by remember { mutableStateOf(0) }
    var xOffset by remember { mutableStateOf(0f) }
    var yOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500L)
            xOffset = (Random.nextFloat() * 300f - 150f)
            yOffset = (Random.nextFloat() * 300f - 150f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color.Red, shape = CircleShape)
                .offset { xOffset dp to yOffset dp }
                .clickable {
                    score++
                    xOffset = (Random.nextFloat() * 300f - 150f)
                    yOffset = (Random.nextFloat() * 300f - 150f)
                }
        )
        Text(
            text = "Score: $score",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            color = Color.Black,
            fontSize = 20.sp
        )
    }
}