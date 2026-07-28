package com.example.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.mutableStateFlow
import androidx.lifecycle.StateFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.app.ui.theme.AppTheme

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
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Whack-a-Mole") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Score: ${uiState.score}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(alignment = Alignment.End)
                    .padding(bottom = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(uiState.boardSize),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(uiState.boardSize * uiState.boardSize) { index ->
                    val isMole = index == uiState.molePosition
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(
                                if (isMole) Color.Green else Color.LightGray
                            )
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                            .clickable { viewModel.onTileClicked(index) }
                    )
                }
            }
        }
    }
}

@HiltViewModel
class GameViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var job: Job? = null
    private val boardSize = 3
    private val totalTiles = boardSize * boardSize

    init {
        startGame()
    }

    private fun startGame() {
        job?.cancel()
        job = viewModelScope.launch {
            while (isActive) {
                delay(1000L) // change mole every second
                val newPos = (0 until totalTiles).random()
                _uiState.update { it.copy(molePosition = newPos) }
            }
        }
    }

    fun onTileClicked(index: Int) {
        _uiState.update { current ->
            val newScore = if (index == current.molePosition) current.score + 1 else current.score
            current.copy(score = newScore)
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}

data class GameUiState(
    val score: Int = 0,
    val molePosition: Int = 0,
    val boardSize: Int = 3
)