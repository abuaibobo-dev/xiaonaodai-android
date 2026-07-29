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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * 一个简单的打地鼠小游戏
 * 使用 Jetpack Compose 构建 UI，Hilt 进行依赖注入
 */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WhackAMoleGame()
            }
        }
    }
}

/**
 * 游戏状态数据类
 */
data class GameState(
    val moles: List<Mole> = List(9) { Mole(id = it) },
    val score: Int = 0,
    val gameTime: Int = 30, // 游戏总时长（秒）
    val isPlaying: Boolean = false
)

/**
 * 地鼠数据类
 */
data class Mole(
    val id: Int,
    val isVisible: Boolean = false,
    val position: Offset = Offset.Zero
)

/**
 * 游戏主界面
 */
@Composable
fun WhackAMoleGame() {
    var gameState by remember { mutableStateOf(GameState()) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var isGameRunning by remember { mutableStateOf(false) }

    // 游戏计时器
    LaunchedEffect(isGameRunning) {
        if (isGameRunning) {
            timeLeft = 30
            while (timeLeft > 0 && isGameRunning) {
                kotlinx.coroutines.delay(1000)
                timeLeft--
            }
            if (timeLeft == 0) {
                isGameRunning = false
            }
        }
    }

    // 地鼠出现逻辑
    LaunchedEffect(isGameRunning) {
        if (isGameRunning) {
            while (isGameRunning) {
                kotlinx.coroutines.delay(Random.nextLong(500, 1500))
                if (isGameRunning) {
                    val randomIndex = Random.nextInt(9)
                    gameState = gameState.copy(
                        moles = gameState.moles.mapIndexed { index, mole ->
                            if (index == randomIndex) mole.copy(isVisible = true)
                            else mole
                        }
                    )
                    // 地鼠出现一段时间后消失
                    kotlinx.coroutines.delay(800)
                    gameState = gameState.copy(
                        moles = gameState.moles.mapIndexed { index, mole ->
                            if (index == randomIndex) mole.copy(isVisible = false)
                            else mole
                        }
                    )
                }
            }
        }
    }

    // 动画值
    val scoreAnim by animateFloatAsState(
        targetValue = gameState.score.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "score"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4CAF50))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = "打地鼠",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 分数和计时器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "分数: ${scoreAnim.toInt()}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "时间: ${timeLeft}s",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (timeLeft <= 10) Color.Red else Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 游戏网格
        GameGrid(
            moles = gameState.moles,
            onMoleClick = { moleId ->
                if (isGameRunning) {
                    val clickedMole = gameState.moles.find { it.id == moleId }
                    if (clickedMole?.isVisible == true) {
                        gameState = gameState.copy(
                            score = gameState.score + 10,
                            moles = gameState.moles.map {
                                if (it.id == moleId) it.copy(isVisible = false)
                                else it
                            }
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 开始/重置按钮
        Button(
            onClick = {
                if (isGameRunning) {
                    // 重置游戏
                    isGameRunning = false
                    gameState = GameState()
                    timeLeft = 30
                } else {
                    // 开始新游戏
                    gameState = GameState(score = 0)
                    isGameRunning = true
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGameRunning) Color.Red else Color(0xFF2196F3)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (isGameRunning) "重置游戏" else "开始游戏",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * 游戏网格组件
 */
@Composable
fun GameGrid(
    moles: List<Mole>,
    onMoleClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0..2) {
                    val index = row * 3 + col
                    val mole = moles.getOrNull(index) ?: return
                    MoleCell(
                        mole = mole,
                        onClick = { onMoleClick(mole.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 单个地鼠格子组件
 */
@Composable
fun MoleCell(
    mole: Mole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 地鼠出现动画
    val scale by animateFloatAsState(
        targetValue = if (mole.isVisible) 1f else 0.8f,
        animationSpec = tween(durationMillis = 200),
        label = "moleScale"
    )

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF8B4513) // 棕色地洞
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (mole.isVisible) {
                // 地鼠
                Canvas(
                    modifier = Modifier
                        .fillMaxSize(scale)
                        .padding(8.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // 地鼠身体（圆形）
                    drawCircle(
                        color = Color(0xFF8B4513),
                        radius = canvasWidth * 0.4f,
                        center = Offset(canvasWidth / 2, canvasHeight * 0.6f)
                    )

                    // 地鼠头部
                    drawCircle(
                        color = Color(0xFFA0522D),
                        radius = canvasWidth * 0.3f,
                        center = Offset(canvasWidth / 2, canvasHeight * 0.35f)
                    )

                    // 眼睛
                    drawCircle(
                        color = Color.Black,
                        radius = canvasWidth * 0.05f,
                        center = Offset(canvasWidth * 0.4f, canvasHeight * 0.3f)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = canvasWidth * 0.05f,
                        center = Offset(canvasWidth * 0.6f, canvasHeight * 0.3f)
                    )

                    // 鼻子
                    drawCircle(
                        color = Color(0xFFFF69B4),
                        radius = canvasWidth * 0.04f,
                        center = Offset(canvasWidth / 2, canvasHeight * 0.38f)
                    )
                }
            } else {
                // 空的地洞
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // 地洞椭圆
                    drawOval(
                        color = Color(0xFF3E2723),
                        topLeft = Offset(canvasWidth * 0.1f, canvasHeight * 0.6f),
                        size = androidx.compose.ui.geometry.Size(
                            canvasWidth * 0.8f,
                            canvasHeight * 0.3f
                        )
                    )
                }
            }
        }
    }
}