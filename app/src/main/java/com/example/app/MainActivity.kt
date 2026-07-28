package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.hypot
import kotlin.math.random

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                GameScreen()
            }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme.light(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC6)
        ),
        typography = Typography(),
        content = content
    )
}

@Composable
fun GameScreen() {
    var score by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(30) }
    var gameRunning by remember { mutableStateOf(false) }
    var circleX by remember { mutableStateOf(0f) }
    var circleY by remember { mutableStateOf(0f) }

    // Countdown timer
    LaunchedEffect(Unit) {
        if (!gameRunning) return@LaunchedEffect
        val startTime = System.currentTimeMillis()
        while (timeLeft > 0 && gameRunning) {
            delay(1000)
            timeLeft = (timeLeft - 1).coerceAtLeast(0)
        }
        if (timeLeft == 0) {
            gameRunning = false
        }
    }

    // Move circle periodically while game is running
    LaunchedEffect(gameRunning) {
        if (!gameRunning) return@LaunchedEffect
        while (gameRunning && timeLeft > 0) {
            delay(800)
            circleX = random(0f..0.8f)
            circleY = random(0f..0.6f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    if (!gameRunning) {
                        // Start a new game
                        gameRunning = true
                        timeLeft = 30
                        score = 0
                        circleX = random(0f..0.8f)
                        circleY = random(0f..0.6f)
                    } else {
                        // Check if tap hit the circle
                        val width = size.width
                        val height = size.height
                        val cx = circleX * width
                        val cy = circleY * height
                        val radius = 80.dp.toPx()
                        val distance = hypot(pos.x - cx, pos.y - cy)
                        if (distance <= radius) {
                            score++
                            circleX = random(0f..0.8f)
                            circleY = random(0f..0.6f)
                        }
                    }
                }
            }
    ) {
        if (!gameRunning) {
            Text(
                text = "Tap to Start",
                fontSize = 24.sp,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Moving circle
            val circleModifier = Modifier
                .size(160.dp)
                .offset {
                    IntOffset(
                        (circleX * (size.width - 160.dp.toPx())).toInt(),
                        (circleY * (size.height - 160.dp.toPx())).toInt()
                    )
                }
                .background(Color.Red, shape = CircleShape)

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = circleModifier)

                // Score and timer
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Score: $score",
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "Time: $timeLeft s",
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                }

                // Game over overlay
                if (timeLeft == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { gameRunning = false }
                    ) {
                        Text(
                            text = "Game Over! Score: $score\nTap to restart",
                            fontSize = 20.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}