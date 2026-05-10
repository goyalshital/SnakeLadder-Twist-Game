package com.example.snakeladdertwistgame

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                SnakeLadderTwistApp()
            }
        }
    }
}

fun playSound(context: Context, soundResId: Int) {
    try {
        val mediaPlayer = MediaPlayer.create(context, soundResId)
        mediaPlayer.setOnCompletionListener { it.release() }
        mediaPlayer.start()
    } catch (e: Exception) { e.printStackTrace() }
}

@Composable
fun SnakeLadderTwistApp() {
    var currentScreen by remember { mutableStateOf("Home") }
    var selectedMode by remember { mutableStateOf("Easy") }
    var playerCount by remember { mutableIntStateOf(2) }

    if (currentScreen == "Home") {
        HomeScreen(onStart = { mode, count ->
            selectedMode = mode
            playerCount = count
            currentScreen = "Game"
        })
    } else {
        GameScreen(selectedMode, playerCount, onBack = { currentScreen = "Home" })
    }
}

@Composable
fun HomeScreen(onStart: (String, Int) -> Unit) {
    var tempMode by remember { mutableStateOf("Easy") }
    var tempCount by remember { mutableIntStateOf(2) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF1F8E9)).padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SNAKE & LADDER", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20), letterSpacing = 2.sp)
            Text("WITH A TWIST", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C), letterSpacing = 5.sp)
        }
        Spacer(modifier = Modifier.height(60.dp))
        Text("Select Players", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf(2, 3, 4).forEach { count ->
                Button(
                    onClick = { tempCount = count },
                    modifier = Modifier.padding(4.dp).weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (tempCount == count) Color(0xFF1976D2) else Color.White, contentColor = if (tempCount == count) Color.White else Color.Black),
                    elevation = ButtonDefaults.buttonElevation(4.dp),
                    border = if (tempCount != count) BorderStroke(1.dp, Color.LightGray) else null
                ) { Text("$count Players", fontSize = 11.sp) }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
        Text("Difficulty Level", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf("Easy", "Medium", "Hard").forEach { mode ->
                Button(
                    onClick = { tempMode = mode },
                    modifier = Modifier.padding(4.dp).weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (tempMode == mode) Color(0xFF388E3C) else Color.White, contentColor = if (tempMode == mode) Color.White else Color.Black),
                    elevation = ButtonDefaults.buttonElevation(4.dp),
                    border = if (tempMode != mode) BorderStroke(1.dp, Color.LightGray) else null
                ) { Text(mode, fontSize = 12.sp) }
            }
        }
        Spacer(modifier = Modifier.height(60.dp))
        Button(onClick = { onStart(tempMode, tempCount) }, modifier = Modifier.fillMaxWidth().height(65.dp), shape = RoundedCornerShape(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))) {
            Text("START GAME", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GameScreen(mode: String, playerCount: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var playerPositions by remember { mutableStateOf(List(playerCount) { 1 }) }
    val animatedPositions = remember { List(playerCount) { Animatable(1f) } }

    var currentPlayerIndex by remember { mutableIntStateOf(0) }
    val dCount = when(mode) { "Medium" -> 3; "Hard" -> 4; else -> 2 }
    var diceValues by remember { mutableStateOf(List(dCount) { 1 }) }
    var isRolling by remember { mutableStateOf(false) }
    var showChallenge by remember { mutableStateOf(false) }
    var userAnswer by remember { mutableStateOf("") }
    var isPaused by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(20) }
    var winnerName by remember { mutableStateOf<String?>(null) }
    var currentQuestion by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableIntStateOf(0) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackColor by remember { mutableStateOf(Color.Black) }
    val playerColors = listOf(Color(0xFF1976D2), Color(0xFFD32F2F), Color(0xFF388E3C), Color(0xFFF57C00))

    LaunchedEffect(isRolling) {
        if (isRolling && !isPaused) {
            playSound(context, R.raw.dice_roll)
            repeat(10) { diceValues = List(dCount) { Random.nextInt(1, 7) }; delay(80L) }
            val d = diceValues.sortedDescending()
            when(mode) {
                "Easy" -> {
                    val op = listOf("+", "-").random()
                    currentQuestion = if (op == "+") "${d[0]} + ${d[1]}" else "${d[0]} - ${d[1]}"
                    correctAnswer = if (op == "+") d[0] + d[1] else d[0] - d[1]
                }
                "Medium" -> { currentQuestion = "(${d[0]} + ${d[1]}) - ${d[2]}"; correctAnswer = (d[0] + d[1]) - d[2] }
                "Hard" -> { currentQuestion = "(${d[0]} + ${d[1]}) * (${d[2]} - ${d[3]})"; correctAnswer = (d[0] + d[1]) * (d[2] - d[3]) }
            }
            timeLeft = 20
            isRolling = false
            showChallenge = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()) // SIRF YE LINE ADD KI HAI SAFE REHNE KE LIYE
                .blur(if (showChallenge || isPaused) 15.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "SNAKE & LADDER", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                Text(text = "with a TWIST", fontSize = 16.sp, color = Color(0xFFB71C1C), modifier = Modifier.offset(y = (-4).dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(onClick = onBack, modifier = Modifier.height(38.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800020))) { Text("EXIT", fontSize = 11.sp) }
                Spacer(Modifier.width(16.dp))
                Button(onClick = { isPaused = true }, modifier = Modifier.height(38.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF002147))) { Text("PAUSE", fontSize = 11.sp) }
            }

            Spacer(modifier = Modifier.height(15.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PlayerUI(0, currentPlayerIndex, diceValues, playerColors, isRolling, showChallenge, isPaused) { isRolling = true }
                PlayerUI(1, currentPlayerIndex, diceValues, playerColors, isRolling, showChallenge, isPaused) { isRolling = true }
            }

            Spacer(modifier = Modifier.height(10.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth(0.98f).aspectRatio(1f).border(2.dp, Color.Black)) {
                val bSize = maxWidth
                val cellSize = bSize / 10
                Image(painter = painterResource(id = R.drawable.board100), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

                playerPositions.forEachIndexed { index, _ ->
                    val curVal = animatedPositions[index].value
                    val zeroPos = (curVal.toInt().coerceIn(1, 100)) - 1
                    val row = zeroPos / 10
                    val colInRow = zeroPos % 10
                    val actualCol = if (row % 2 == 1) (9 - colInRow) else colInRow
                    val xPos = cellSize * actualCol.toFloat()
                    val yPos = bSize - (cellSize * (row + 1).toFloat())

                    Box(
                        modifier = Modifier.size(cellSize * 0.8f).offset(x = xPos + (cellSize * 0.1f), y = yPos + (cellSize * 0.1f)).zIndex(10f + index),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.fillMaxSize(0.9f).offset(y = 2.dp).background(Color.Black.copy(0.2f), CircleShape))
                        Box(modifier = Modifier.fillMaxSize().background(brush = Brush.radialGradient(colors = listOf(playerColors[index].copy(alpha = 0.8f), playerColors[index])), shape = CircleShape).border(1.5.dp, Color.White, CircleShape))
                        Box(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.3f).offset(x = (-2).dp, y = (-4).dp).background(Color.White.copy(0.4f), CircleShape))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if(playerCount > 2) PlayerUI(2, currentPlayerIndex, diceValues, playerColors, isRolling, showChallenge, isPaused) { isRolling = true } else Spacer(Modifier.size(10.dp))
                if(playerCount > 3) PlayerUI(3, currentPlayerIndex, diceValues, playerColors, isRolling, showChallenge, isPaused) { isRolling = true } else Spacer(Modifier.size(10.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (showChallenge && !isPaused && feedbackMessage == null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.7f)).zIndex(100f), contentAlignment = Alignment.Center) {
                Card(modifier = Modifier.padding(15.dp).fillMaxWidth(0.94f),
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = playerColors[currentPlayerIndex].copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(15.dp).background(Color.White, RoundedCornerShape(15.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Time: ${timeLeft}s", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            IconButton(onClick = { isPaused = true }) { Icon(painter = painterResource(id = android.R.drawable.ic_media_pause), contentDescription = "Pause", tint = playerColors[currentPlayerIndex]) }
                        }
                        Text("Solve: $currentQuestion", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = playerColors[currentPlayerIndex], textAlign = TextAlign.Center, maxLines = 1)
                        OutlinedTextField(value = userAnswer, onValueChange = { userAnswer = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), placeholder = { Text("Your Answer") })
                        Button(onClick = {
                            val userIn = userAnswer.toIntOrNull()
                            val sIndex = currentPlayerIndex
                            val startPos = playerPositions[sIndex]
                            showChallenge = false
                            feedbackColor = playerColors[sIndex]

                            scope.launch {
                                if (userIn == correctAnswer) {
                                    feedbackMessage = if(correctAnswer == 0) "🌟 0 Bonus! +1 step" else if(correctAnswer > 20) "🔥 Super Bonus! +20 steps" else "🌟 Correct! Moving ${if(correctAnswer <= 0) 1 else if(correctAnswer > 20) 20 else correctAnswer} steps"
                                    playSound(context, if (correctAnswer == 0 || correctAnswer > 20) R.raw.bonus else R.raw.correct)
                                    val moveSteps = if (correctAnswer == 0) 1 else if (correctAnswer > 20) 20 else correctAnswer

                                    for (i in 1..moveSteps) {
                                        animatedPositions[sIndex].animateTo((startPos + i).toFloat(), tween(180, easing = LinearEasing))
                                    }

                                    var midPos = (startPos + moveSteps).coerceAtMost(100)
                                    val finalPos = when(midPos) {
                                        2->18; 11->31; 12->28; 22->40; 36->62; 41->59; 46->55; 70->94; 77->84; 85->97;
                                        21->15; 23->6; 29->7; 35->18; 47->32; 52->38; 71->34; 82->59; 95->78; 99->79;
                                        else -> midPos
                                    }

                                    if(finalPos != midPos) {
                                        delay(400)
                                        feedbackMessage = if(finalPos > midPos) "🪜 Up the Ladder!" else "🐍 Oh no, Snake!"
                                        if (finalPos > midPos) playSound(context, R.raw.ladder) else playSound(context, R.raw.snake)
                                        animatedPositions[sIndex].animateTo(finalPos.toFloat(), tween(900, easing = LinearOutSlowInEasing))
                                        midPos = finalPos
                                    }

                                    val newList = playerPositions.toMutableList()
                                    newList[sIndex] = midPos
                                    playerPositions = newList
                                    if (midPos == 100) { winnerName = "PLAYER ${sIndex + 1}"; playSound(context, R.raw.win) }
                                } else {
                                    playSound(context, R.raw.wrong)
                                    feedbackMessage = "🛑 Wrong! -3 steps penalty"
                                    val penaltyPos = (startPos - 3).coerceAtLeast(1)
                                    animatedPositions[sIndex].animateTo(penaltyPos.toFloat(), tween(500))
                                    val newList = playerPositions.toMutableList()
                                    newList[sIndex] = penaltyPos
                                    playerPositions = newList
                                }
                                delay(1800); feedbackMessage = null
                                userAnswer = ""; currentPlayerIndex = (sIndex + 1) % playerCount
                            }
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = playerColors[currentPlayerIndex])) { Text("SUBMIT") }
                    }
                }
            }
        }

        if (isPaused) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.8f)).zIndex(200f), contentAlignment = Alignment.Center) {
                Button(onClick = { isPaused = false }) { Text("RESUME GAME") }
            }
        }

        winnerName?.let { winner ->
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.9f)).zIndex(600f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆 CHAMPION!", color = Color(0xFFFBC02D), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                    Text("$winner WINS!", color = Color.White, fontSize = 24.sp)
                    Button(onClick = { onBack() }) { Text("PLAY AGAIN") }
                }
            }
        }

        feedbackMessage?.let { msg ->
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp).zIndex(500f), contentAlignment = Alignment.Center) {
                Surface(color = feedbackColor.copy(alpha = 0.9f), shape = RoundedCornerShape(50.dp)) {
                    Text(text = msg, color = Color.White, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PlayerUI(index: Int, activeIndex: Int, dice: List<Int>, colors: List<Color>, isRolling: Boolean, showChallenge: Boolean, isPaused: Boolean, onRoll: () -> Unit) {
    val isTurn = index == activeIndex
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(115.dp).padding(4.dp)) {
        Box(modifier = Modifier.size(56.dp).background(if (isTurn) colors[index].copy(alpha = 0.15f) else Color.Transparent, CircleShape).border(2.dp, if (isTurn) colors[index] else Color.LightGray.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(32.dp).background(brush = Brush.radialGradient(listOf(colors[index].copy(alpha = 0.7f), colors[index])), shape = CircleShape).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Text("P${index + 1}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }
        if (isTurn) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.03f)).padding(4.dp)) {
                dice.forEach { v ->
                    Card(modifier = Modifier.size(26.dp).padding(1.dp), shape = RoundedCornerShape(6.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(text = "$v", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = colors[index])
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRoll, enabled = !isRolling && !showChallenge && !isPaused, modifier = Modifier.height(36.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = colors[index], disabledContainerColor = Color.LightGray.copy(alpha = 0.3f)), elevation = ButtonDefaults.buttonElevation(8.dp)) {
                Text("ROLL", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        } else {
            Spacer(modifier = Modifier.height(85.dp))
        }
    }
}