package com.example.whackamole

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import kotlin.random.Random

class GameActivity : AppCompatActivity() {
    private lateinit var gridLayout: GridLayout
    private lateinit var scoreText: TextView
    private lateinit var timeText: TextView
    private lateinit var startButton: Button
    private val buttons = mutableListOf<Button>()
    private var score = 0
    private var timeLeft = 30
    private var isRunning = false
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        
        gridLayout = findViewById(R.id.gridLayout)
        scoreText = findViewById(R.id.scoreText)
        timeText = findViewById(R.id.timeText)
        startButton = findViewById(R.id.startButton)
        
        // 创建3x3网格
        for (i in 0 until 9) {
            val btn = Button(this)
            btn.text = ""
            btn.setBackgroundColor(0xFF4CAF50.toInt())
            btn.textSize = 24f
            btn.setOnClickListener { onMoleClick(it as Button) }
            buttons.add(btn)
            
            val params = GridLayout.LayoutParams()
            params.width = 200
            params.height = 200
            params.setMargins(8, 8, 8, 8)
            gridLayout.addView(btn, params)
        }
        
        startButton.setOnClickListener { startGame() }
    }
    
    private fun startGame() {
        if (isRunning) return
        isRunning = true
        score = 0
        timeLeft = 30
        scoreText.text = "得分: 0"
        timeText.text = "时间: 30"
        startButton.isEnabled = false
        
        job = CoroutineScope(Dispatchers.Main).launch {
            // 倒计时
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
                timeText.text = "时间: $timeLeft"
            }
            endGame()
        }
        
        // 地鼠出现循环
        CoroutineScope(Dispatchers.Main).launch {
            while (isRunning) {
                showRandomMole()
                delay(Random.nextLong(600, 1500))
            }
        }
    }
    
    private fun showRandomMole() {
        if (!isRunning) return
        // 清除所有地鼠
        buttons.forEach { it.text = "" }
        // 随机显示1-2个地鼠
        val count = Random.nextInt(1, 3)
        repeat(count) {
            val idx = Random.nextInt(9)
            buttons[idx].text = "🐹"
        }
    }
    
    private fun onMoleClick(btn: Button) {
        if (!isRunning) return
        if (btn.text == "🐹") {
            score += 10
            scoreText.text = "得分: $score"
            btn.text = ""
        }
    }
    
    private fun endGame() {
        isRunning = false
        startButton.isEnabled = true
        buttons.forEach { it.text = "" }
        Toast.makeText(this, "游戏结束！得分: $score", Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        job?.cancel()
    }
}