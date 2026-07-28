@AndroidEntryPoint
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
    val density = LocalDensity.current
    var score by remember { mutableStateOf(0) }
    var centerOffset by remember { mutableStateOf(Offset.Zero) }
    val constraints = remember { mutableStateOf<Constraints?>(null) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { constraints.value = it }
    ) {
        val maxWidth = constraints.value?.maxWidth?.roundToInt() ?: 0
        val maxHeight = constraints.value?.maxHeight?.roundToInt() ?: 0
        val circleDiameter = (50.dp * density).roundToPx() * 2 // Actually radius 50.dp => diameter 100.dp
        // Ensure we have size > 0
        if (maxWidth > 0 && maxHeight > 0) {
            LaunchedEffect(Unit) {
                delay(1500)
                val randomX = (0..(maxWidth - circleDiameter)).random()
                val randomY = (0..(maxHeight - circleDiameter)).random()
                centerOffset = Offset(randomX.toFloat(), randomY.toFloat())
            }
        }
        // Draw circle at offset
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color.Red, shape = CircleShape)
                .offset(
                    x = centerOffset.x.dp,
                    y = centerOffset.y.dp
                )
                .pointerInput(Unit) {
                    detectTapGestures { tapPos ->
                        // tapPos is Offset in px
                        val centerPx = centerOffset * density
                        val radiusPx = (50.dp * density).roundToPx()
                        if ((tapPos - centerPx).distance <= radiusPx) {
                            score++
                            // generate new position immediately
                            val randomX = (0..(maxWidth - circleDiameter)).random()
                            val randomY = (0..(maxHeight - circleDiameter)).random()
                            centerOffset = Offset(randomX.toFloat(), randomY.toFloat())
                        }
                    }
                }
        )
        // Score text
        Text(
            text = "Score: $score",
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            fontSize = 20.sp,
            color = Color.Black
        )
    }
}