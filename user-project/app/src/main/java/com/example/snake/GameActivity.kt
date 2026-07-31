package com.example.snake

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {

    private lateinit var snakeGameView: SnakeGameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        snakeGameView = findViewById(R.id.snake_game_view)

        val restartButton: Button = findViewById(R.id.restart_button)
        restartButton.setOnClickListener {
            snakeGameView.restart()
        }
    }

    override fun onResume() {
        super.onResume()
        snakeGameView.resume()
    }

    override fun onPause() {
        super.onPause()
        snakeGameView.pause()
    }
}
