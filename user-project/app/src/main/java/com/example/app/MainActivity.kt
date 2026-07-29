package com.example.app
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItemsHorizontal
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext


/**
 * 贪吃蛇游戏主 Activity
 * 使用 Jetpack Compose 构建 UI，Hilt 进行依赖注入
 */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SnakeGameScreen()
            }
        }
    }
}

/**
 * 游戏状态数据类
 */
data class GameState(
    val snake: List<Offset> = listOf(Offset(5f, 5f)),
    val food: Offset = Offset(10f, 10f),
    val direction: Direction = Direction.RIGHT,
    val isGameOver: Boolean = false,
    val score: Int = 0
)

/**
 * 方向枚举
 */
enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

/**
 * 游戏主屏幕 Composable
 */
@Composable
fun SnakeGameScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 游戏状态
    var gameState by remember { mutableStateOf(GameState()) }
    var isRunning by remember { mutableStateOf(false) }

    // 网格尺寸
    val gridSize = 20
    val cellSize = 16.dp

    // 游戏循环
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isRunning && !gameState.isGameOver) {
                delay(200L) // 游戏速度
                gameState = updateGameState(gameState, gridSize)
            }
        }
    }

    // 触摸事件处理
    val onSwipe: (Direction) -> Unit = { newDirection ->
        if (!isRunning) {
            isRunning = true
        }
        // 防止反向移动
        val currentDirection = gameState.direction
        if (!isOppositeDirection(currentDirection, newDirection)) {
            gameState = gameState.copy(direction = newDirection)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B2F))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 分数显示
        Text(
            text = "分数: ${gameState.score}",
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 游戏画布
        Box(
            modifier = Modifier
                .size(gridSize * cellSize)
                .background(Color(0xFF162447))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // 根据点击位置判断方向
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY

                        val newDirection = when {
                            kotlin.math.abs(dx) > kotlin.math.abs(dy) -> {
                                if (dx > 0) Direction.RIGHT else Direction.LEFT
                            }
                            else -> {
                                if (dy > 0) Direction.DOWN else Direction.UP
                            }
                        }
                        onSwipe(newDirection)
                    }
                }
        ) {
            // 绘制游戏网格
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellWidth = size.width / gridSize
                val cellHeight = size.height / gridSize

                // 绘制网格线
                for (i in 0..gridSize) {
                    drawLine(
                        color = Color(0xFF1F4068),
                        start = Offset(i * cellWidth, 0f),
                        end = Offset(i * cellWidth, size.height),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0xFF1F4068),
                        start = Offset(0f, i * cellHeight),
                        end = Offset(size.width, i * cellHeight),
                        strokeWidth = 1f
                    )
                }

                // 绘制蛇
                gameState.snake.forEachIndexed { index, segment ->
                    drawRect(
                        color = if (index == 0) Color(0xFF00FF00) else Color(0xFF00CC00),
                        topLeft = Offset(
                            segment.x * cellWidth + 1f,
                            segment.y * cellHeight + 1f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            cellWidth - 2f,
                            cellHeight - 2f
                        )
                    )
                }

                // 绘制食物
                drawCircle(
                    color = Color(0xFFFF4444),
                    radius = cellWidth / 2f - 2f,
                    center = Offset(
                        gameState.food.x * cellWidth + cellWidth / 2f,
                        gameState.food.y * cellHeight + cellHeight / 2f
                    )
                )
            }
        }

        // 游戏结束提示
        if (gameState.isGameOver) {
            Text(
                text = "游戏结束!",
                color = Color.Red,
                fontSize = 32.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
            Button(
                onClick = {
                    gameState = GameState()
                    isRunning = false
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("重新开始")
            }
        } else if (!isRunning) {
            Text(
                text = "点击屏幕开始游戏",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/**
 * 更新游戏状态
 */
private fun updateGameState(currentState: GameState, gridSize: Int): GameState {
    if (currentState.isGameOver) return currentState

    val snake = currentState.snake.toMutableList()
    val head = snake.first()
    val direction = currentState.direction

    // 计算新头部位置
    val newHead = when (direction) {
        Direction.UP -> Offset(head.x, head.y - 1f)
        Direction.DOWN -> Offset(head.x, head.y + 1f)
        Direction.LEFT -> Offset(head.x - 1f, head.y)
        Direction.RIGHT -> Offset(head.x + 1f, head.y)
    }

    // 检查是否撞墙
    if (newHead.x < 0 || newHead.x >= gridSize ||
        newHead.y < 0 || newHead.y >= gridSize
    ) {
        return currentState.copy(isGameOver = true)
    }

    // 检查是否撞到自己
    if (snake.contains(newHead)) {
        return currentState.copy(isGameOver = true)
    }

    // 添加新头部
    snake.add(0, newHead)

    // 检查是否吃到食物
    var newFood = currentState.food
    var newScore = currentState.score
    if (newHead == currentState.food) {
        newScore++
        // 生成新食物
        newFood = generateFood(snake, gridSize)
    } else {
        // 移除尾部
        snake.removeAt(snake.size - 1)
    }

    return currentState.copy(
        snake = snake,
        food = newFood,
        score = newScore
    )
}

/**
 * 生成新食物位置
 */
private fun generateFood(snake: List<Offset>, gridSize: Int): Offset {
    val occupied = snake.toSet()
    val available = mutableListOf<Offset>()

    for (x in 0 until gridSize) {
        for (y in 0 until gridSize) {
            val pos = Offset(x.toFloat(), y.toFloat())
            if (pos !in occupied) {
                available.add(pos)
            }
        }
    }

    return if (available.isNotEmpty()) {
        available.random()
    } else {
        // 所有格子都被占满，游戏胜利
        Offset(-1f, -1f)
    }
}

/**
 * 检查是否为相反方向
 */
private fun isOppositeDirection(current: Direction, new: Direction): Boolean {
    return when (current) {
        Direction.UP -> new == Direction.DOWN
        Direction.DOWN -> new == Direction.UP
        Direction.LEFT -> new == Direction.RIGHT
        Direction.RIGHT -> new == Direction.LEFT
    }
}