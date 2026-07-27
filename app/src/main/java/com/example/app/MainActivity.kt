package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableStateFlow
import androidx.lifecycle.StateFlow
import dagger.assisted.Assisted
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.app.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen()
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel()
) {
    val score by viewModel.score.collectAsState()
    val targetVisible by viewModel.targetVisible.collectAsState()
    val targetPosition by viewModel.targetPosition.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Score display
        Text(
            text = "Score: $score",
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Target (a circle) that appears/disappears at random position
        if (targetVisible) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(
                        x = targetPosition.x.dp,
                        y = targetPosition.y.dp
                    )
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .clickable { viewModel.onTargetClicked() }
            )
        }

        // Reset button at bottom
        Button(
            onClick = { viewModel.resetGame() },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) {
            Text("Reset")
        }
    }
}

data class Position(val x: Int, val y: Int)

@HiltViewModel
class GameViewModel @Inject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _targetVisible = MutableStateFlow(false)
    val targetVisible: StateFlow<Boolean> = _targetVisible.asStateFlow()

    private val _targetPosition = MutableStateFlow(Position(0, 0))
    val targetPosition: StateFlow<Position> = _targetPosition.asStateFlow()

    private val random = java.util.Random()
    // These dimensions are placeholders; in a real app you'd get them from window metrics.
    private val viewWidth = 1080
    private val viewHeight = 1920

    init {
        startNewTarget()
    }

    fun onTargetClicked() {
        _score.value += 1
        startNewTarget()
    }

    private fun startNewTarget() {
        _targetVisible.value = true
        val x = random.nextInt((viewWidth - 80).coerceAtLeast(0))
        val y = random.nextInt((viewHeight - 80).coerceAtLeast(0))
        _targetPosition.value = Position(x, y)

        // Hide after a short delay if not clicked
        viewModelScope.launch {
            delay(2000)
            if (_targetVisible.value) {
                _targetVisible.value = false
                // after hiding, show again after a short pause
                delay(500)
                startNewTarget()
            }
        }
    }

    fun resetGame() {
        viewModelScope.clear()
        _score.value = 0
        _targetVisible.value = false
        startNewTarget()
    }
}