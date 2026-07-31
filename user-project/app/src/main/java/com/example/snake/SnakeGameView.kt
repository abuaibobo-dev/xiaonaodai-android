package com.example.snake

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View

class SnakeGameView(context: Context) : View(context) {

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    // Snake body segments (Rectangles)
    private val snakeBody = mutableListOf<Rect>()
    // Direction: 0=RIGHT, 1=DOWN, 2=LEFT, 3=UP
    private var direction = 0
    private var nextDirection = 0

    // Food position
    private var foodRect: Rect? = null

    // Game state
    private var score = 0
    private var isGameOver = false
    private var isPaused = false

    // Dimensions
    private var viewWidth = 0
    private var viewHeight = 0
    private var cellSize = 0
    private val snakeColor = Color.GREEN
    private val foodColor = Color.RED
    private val gameOverColor = Color.RED
    private val scoreColor = Color.WHITE

    // Paint objects for drawing
    private val snakePaint = Paint()
    private val foodPaint = Paint()
    private val gameOverPaint = Paint()
    private val scorePaint = Paint()

    init {
        snakePaint.color = snakeColor
        snakePaint.isAntiAlias = true

        foodPaint.color = foodColor
        foodPaint.isAntiAlias = true

        gameOverPaint.color = gameOverColor
        gameOverPaint.isAntiAlias = true
        gameOverPaint.textSize = 60f
        gameOverPaint.isFakeBoldText = true

        scorePaint.color = scoreColor
        scorePaint.isAntiAlias = true
        scorePaint.textSize = 40f
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        viewWidth = width
        viewHeight = height
        // Calculate best square size (take the smaller of the two directions)
        cellSize = minOf(width / 20, height / 20)
        // Initialize game
        resetGame()
    }

    private fun resetGame() {
        if (cellSize <= 0) return
        // Reset snake to center
        snakeBody.clear()
        val centerX = (viewWidth - cellSize) / 2
        val centerY = (viewHeight - cellSize) / 2
        snakeBody.add(Rect(centerX, centerY, centerX + cellSize, centerY + cellSize))
        snakeBody.add(Rect(centerX - cellSize, centerY, centerX, centerY + cellSize))
        snakeBody.add(Rect(centerX - 2 * cellSize, centerY, centerX - cellSize, centerY + cellSize))

        direction = 0
        nextDirection = 0
        score = 0
        isGameOver = false
        isPaused = false

        // Generate initial food
        generateFood()

        // Start game loop
        startGameLoop()
    }

    private fun generateFood() {
        if (cellSize <= 0 || viewWidth <= cellSize || viewHeight <= cellSize) return
        val maxX = (viewWidth - cellSize) / cellSize
        val maxY = (viewHeight - cellSize) / cellSize
        if (maxX <= 0 || maxY <= 0) return

        do {
            val foodX = (Math.random() * maxX).toInt() * cellSize
            val foodY = (Math.random() * maxY).toInt() * cellSize
            foodRect = Rect(foodX, foodY, foodX + cellSize, foodY + cellSize)
        } while (snakeBody.contains(foodRect))
    }

    private fun startGameLoop() {
        runnable?.let { handler.removeCallbacks(it) }
        runnable = object : Runnable {
            override fun run() {
                if (!isGameOver && !isPaused) {
                    moveSnake()
                    checkCollisions()
                    invalidate() // Redraw
                }
                handler.postDelayed(this, 200) // 200ms interval
            }
        }
        handler.post(runnable!!)
    }

    private fun startLoopIfNeeded() {
        if (!isGameOver && cellSize > 0) {
            startGameLoop()
        }
    }

    private fun moveSnake() {
        direction = nextDirection

        // Calculate new head position
        val head = snakeBody[0]
        val newHead = Rect(head)

        when (direction) {
            0 -> { // RIGHT
                newHead.left += cellSize
                newHead.right += cellSize
            }
            1 -> { // DOWN
                newHead.top += cellSize
                newHead.bottom += cellSize
            }
            2 -> { // LEFT
                newHead.left -= cellSize
                newHead.right -= cellSize
            }
            3 -> { // UP
                newHead.top -= cellSize
                newHead.bottom -= cellSize
            }
        }

        // Add new head
        snakeBody.add(0, newHead)

        // Check if snake ate food
        if (foodRect != null && newHead.intersect(foodRect!!)) {
            score++
            generateFood()
            // Don't remove tail when eating food
        } else {
            // Remove tail
            snakeBody.removeAt(snakeBody.size - 1)
        }
    }

    private fun checkCollisions() {
        val head = snakeBody[0]

        // Check wall collision
        if (head.left < 0 || head.right > viewWidth || head.top < 0 || head.bottom > viewHeight) {
            isGameOver = true
            return
        }

        // Check self collision (except tail)
        for (i in 1 until snakeBody.size - 1) {
            if (head.equals(snakeBody[i])) {
                isGameOver = true
                return
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Clear canvas
        canvas.drawColor(Color.BLACK)

        // Draw snake
        for (segment in snakeBody) {
            canvas.drawRect(segment, snakePaint)
        }

        // Draw food
        foodRect?.let {
            canvas.drawRect(it, foodPaint)
        }

        // Draw score
        canvas.drawText("Score: $score", 50f, 80f, scorePaint)

        // Draw game over message
        if (isGameOver) {
            canvas.drawText("Game Over", (viewWidth / 2 - 200).toFloat(), (viewHeight / 2).toFloat(), gameOverPaint)
            canvas.drawText("Tap to restart", (viewWidth / 2 - 150).toFloat(), (viewHeight / 2 + 100).toFloat(), scorePaint)
        } else if (isPaused) {
            canvas.drawText("Paused", (viewWidth / 2 - 100).toFloat(), (viewHeight / 2).toFloat(), gameOverPaint)
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
        startLoopIfNeeded()
    }

    fun restart() {
        resetGame()
    }

    fun getScore(): Int = score
}
