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
 * 一个简单的打地鼠小游戏，使用 Jetpack Compose 构建 UI，Hilt 注入依赖。
 * 游戏逻辑：点击随机出现的地鼠获得分数，游戏时长 30 秒。
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
 * 游戏主界面 Composable
 */
@Composable
fun WhackAMoleGame() {
    // 游戏状态
    var score by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(30) }
    var isGameRunning by remember { mutableStateOf(false) }
    // 地鼠位置 (0-8 代表 3x3 网格)
    var molePosition by remember { mutableStateOf(-1) }
    // 动画值，用于地鼠出现/消失的缩放效果
    val moleScale = remember { Animatable(0f) }

    // 游戏循环：倒计时和地鼠随机出现
    LaunchedEffect(isGameRunning) {
        if (isGameRunning) {
            // 倒计时循环
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            // 游戏结束
            isGameRunning = false
            molePosition = -1
            moleScale.snapTo(0f)
        }
    }

    // 地鼠出现循环（仅在游戏运行时）
    LaunchedEffect(isGameRunning, timeLeft) {
        if (isGameRunning && timeLeft > 0) {
            while (timeLeft > 0) {
                // 随机延迟 0.5-1.5 秒
                delay(Random.nextLong(500, 1500))
                if (!isGameRunning || timeLeft <= 0) break

                // 显示地鼠
                molePosition = Random.nextInt(9)
                // 缩放动画：从 0 到 1
                moleScale.snapTo(0f)
                moleScale.animateTo(1f, animationSpec = tween(200))

                // 地鼠停留 0.8-1.2 秒
                delay(Random.nextLong(800, 1200))
                if (!isGameRunning || timeLeft <= 0) break

                // 隐藏地鼠
                moleScale.animateTo(0f, animationSpec = tween(200))
                molePosition = -1
            }
        }
    }

    // UI 布局
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

        // 分数和倒计时
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "分数: $score",
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "时间: ${timeLeft}s",
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3x3 游戏网格
        GameGrid(
            molePosition = molePosition,
            moleScale = moleScale.value,
            enabled = isGameRunning,
            onMoleClicked = {
                if (isGameRunning && molePosition != -1) {
                    score++
                    // 点击后立即隐藏地鼠
                    molePosition = -1
                    moleScale.snapTo(0f)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 开始/重新开始按钮
        Button(
            onClick = {
                if (!isGameRunning) {
                    // 重置游戏
                    score = 0
                    timeLeft = 30
                    molePosition = -1
                    isGameRunning = true
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800)
            ),
            modifier = Modifier
                .width(200.dp)
                .height(50.dp)
        ) {
            Text(
                text = if (isGameRunning) "游戏进行中..." else "开始游戏",
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}

/**
 * 3x3 游戏网格 Composable
 *
 * @param molePosition 当前地鼠位置 (0-8)，-1 表示没有地鼠
 * @param moleScale 地鼠缩放比例，用于动画
 * @param enabled 是否可点击
 * @param onMoleClicked 点击地鼠时的回调
 */
@Composable
fun GameGrid(
    molePosition: Int,
    moleScale: Float,
    enabled: Boolean,
    onMoleClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .height(300.dp)
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                for (col in 0..2) {
                    val index = row * 3 + col
                    val isMole = molePosition == index

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .background(
                                color = Color(0xFF8B4513),
                                shape = MaterialTheme.shapes.medium
                            )
                            .pointerInput(enabled && isMole) {
                                if (enabled && isMole) {
                                    detectTapGestures {
                                        onMoleClicked()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMole) {
                            // 使用 Canvas 绘制地鼠（简单圆形表示）
                            Canvas(
                                modifier = Modifier
                                    .size(60.dp)
                                    .scale(moleScale)
                            ) {
                                // 地鼠身体（棕色圆形）
                                drawCircle(
                                    color = Color(0xFFD2691E),
                                    radius = size.minDimension / 2
                                )
                                // 地鼠眼睛（白色小圆）
                                drawCircle(
                                    color = Color.White,
                                    radius = size.minDimension / 8,
                                    center = Offset(
                                        x = size.width / 2 - size.width / 6,
                                        y = size.height / 2 - size.height / 6
                                    )
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = size.minDimension / 8,
                                    center = Offset(
                                        x = size.width / 2 + size.width / 6,
                                        y = size.height / 2 - size.height / 6
                                    )
                                )
                                // 瞳孔（黑色小圆）
                                drawCircle(
                                    color = Color.Black,
                                    radius = size.minDimension / 16,
                                    center = Offset(
                                        x = size.width / 2 - size.width / 6,
                                        y = size.height / 2 - size.height / 6
                                    )
                                )
                                drawCircle(
                                    color = Color.Black,
                                    radius = size.minDimension / 16,
                                    center = Offset(
                                        x = size.width / 2 + size.width / 6,
                                        y = size.height / 2 - size.height / 6
                                    )
                                )
                                // 鼻子（红色小圆）
                                drawCircle(
                                    color = Color.Red,
                                    radius = size.minDimension / 12,
                                    center = Offset(
                                        x = size.width / 2,
                                        y = size.height / 2 + size.height / 10
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 扩展函数：为 Modifier 添加缩放动画支持
 */
private fun Modifier.scale(scale: Float): Modifier {
    return this.then(
        androidx.compose.ui.draw.scale(scale, scale)
    )
}