package com.example.myapplication

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

private val PracticeInk = Color(0xFF202B35)
private val PracticeMuted = Color(0xFF78838A)
private val PracticeGreen = Color(0xFF277A60)
private val PracticeMint = Color(0xFFE2F1E9)
private val PracticePaper = Color(0xFFF7F8F4)
private val PracticeCoral = Color(0xFFDE7358)
internal const val DAILY_CHALLENGE_SIZE = 5

private enum class PracticeMode(val title: String, val icon: String) {
    GUESS("Đoán", "♧"),
    FLASHCARD("Flashcard", "◉"),
    QUIZ("Trắc nghiệm", "?"),
    REVERSE("Đảo chiều", "↔"),
    REPEAT("Lặp lại", "↻"),
    LISTENING("Nghe & gõ", "♫"),
    DAILY("Thử thách ngày", "✦")
}

internal data class PracticeChoice(val title: String, val detail: String = "")

internal fun createDailyChallengeWords(words: List<Word>, dateKey: String): List<Word> =
    words.shuffled(Random(dateKey.hashCode().toLong())).take(DAILY_CHALLENGE_SIZE)

@Composable
fun PracticeScreen(
    database: UserDatabase,
    userId: Long,
    startMode: String = PracticeMode.GUESS.name,
    onCorrectAnswer: () -> Unit,
    onActivityChanged: () -> Unit,
    onDailyChallengeCompleted: (Int, Int) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allWords = remember { lessons.flatMap { it.vocabulary } }
    var reviewStates by remember(userId) { mutableStateOf(database.getWordReviewStates(userId)) }
    var selectedMode by rememberSaveable(userId, startMode) { mutableStateOf(startMode) }
    var cardIndex by rememberSaveable(userId) { mutableIntStateOf(0) }
    var sessionCount by rememberSaveable(userId) { mutableIntStateOf(0) }
    var sessionTarget by rememberSaveable(userId, startMode) { mutableIntStateOf(if (startMode == PracticeMode.DAILY.name) DAILY_CHALLENGE_SIZE else 10) }
    var sessionSize by rememberSaveable(userId) { mutableIntStateOf(10) }
    var sessionCorrect by rememberSaveable(userId) { mutableIntStateOf(0) }
    var sessionFinished by rememberSaveable(userId) { mutableStateOf(false) }
    var autoSpeak by rememberSaveable(userId) { mutableStateOf(false) }
    var showSettings by rememberSaveable(userId) { mutableStateOf(false) }
    var showSchedule by rememberSaveable(userId) { mutableStateOf(false) }
    var typedAnswer by rememberSaveable(userId) { mutableStateOf("") }
    var hintVisible by rememberSaveable(userId) { mutableStateOf(false) }
    var answerRevealed by rememberSaveable(userId) { mutableStateOf(false) }
    var checked by rememberSaveable(userId) { mutableStateOf(false) }
    var wasCorrect by rememberSaveable(userId) { mutableStateOf(false) }
    var selectedChoice by rememberSaveable(userId) { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()
    val speaker = rememberSpeechController()
    val mode = PracticeMode.valueOf(selectedMode)
    val now = System.currentTimeMillis()
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
    val dueWords = allWords.filter { (reviewStates[it.id]?.dueAtMillis ?: 0L) <= now }
    val deck = when (mode) {
        PracticeMode.REPEAT -> dueWords
        PracticeMode.DAILY -> createDailyChallengeWords(allWords, todayKey)
        else -> allWords
    }
    val word = deck.getOrNull(cardIndex.coerceAtMost((deck.size - 1).coerceAtLeast(0)))
    val state = word?.let { reviewStates[it.id] ?: WordReviewState() } ?: WordReviewState()
    val choices = remember(userId, selectedMode, word?.id) {
        if (word != null && (mode == PracticeMode.QUIZ || mode == PracticeMode.REVERSE || mode == PracticeMode.DAILY)) {
            createPracticeChoices(word, allWords, reverse = mode == PracticeMode.REVERSE)
        } else {
            emptyList()
        }
    }
    LaunchedEffect(word?.id, autoSpeak) {
        if ((autoSpeak || mode == PracticeMode.LISTENING) && word != null) speaker.speak(word.english)
    }
    LaunchedEffect(userId) {
        while (true) {
            delay(60_000L)
            withContext(Dispatchers.IO) { database.recordStudySeconds(userId, 60L) }
            onActivityChanged()
        }
    }

    fun moveToNext() {
        sessionCount++
        typedAnswer = ""
        hintVisible = false
        answerRevealed = false
        checked = false
        wasCorrect = false
        selectedChoice = -1
        if (sessionCount >= sessionTarget) {
            sessionFinished = true
            if (mode == PracticeMode.DAILY) {
                val finalCorrect = sessionCorrect
                val finalTotal = sessionTarget
                scope.launch(Dispatchers.IO) {
                    database.saveDailyChallengeResult(userId, finalCorrect, finalTotal)
                    withContext(Dispatchers.Main) { onDailyChallengeCompleted(finalCorrect, finalTotal) }
                }
            }
            return
        }
        cardIndex = when {
            deck.isEmpty() -> 0
            mode == PracticeMode.REPEAT -> cardIndex.coerceAtMost(deck.lastIndex)
            deck.size > 1 -> (cardIndex + 1) % deck.size
            else -> 0
        }
    }

    fun recordAnswer(remembered: Boolean) {
        val currentWord = word ?: return
        if (remembered) {
            sessionCorrect++
            onCorrectAnswer()
        }
        scope.launch {
            val updated = withContext(Dispatchers.IO) {
                database.recordWordReview(userId, currentWord.id, remembered)
            }
            reviewStates = reviewStates + (currentWord.id to updated)
            onActivityChanged()
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ôn tập tổng hợp", color = PracticeInk, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Luyện linh hoạt · ${allWords.size} từ trong lộ trình", color = PracticeMuted, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Surface(color = PracticeMint, shape = RoundedCornerShape(6.dp)) {
                    Text("✓ $sessionCorrect", color = PracticeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                }
                Surface(color = Color.White, shape = CircleShape, modifier = Modifier.size(36.dp).semantics { contentDescription = "Mở lịch ôn tập" }.clickable { showSchedule = true }) {
                    Box(contentAlignment = Alignment.Center) { Text("▦", color = PracticeGreen, fontSize = 17.sp) }
                }
                Surface(color = Color.White, shape = CircleShape, modifier = Modifier.size(36.dp).semantics { contentDescription = "Cài đặt luyện tập" }.clickable { showSettings = true }) {
                    Box(contentAlignment = Alignment.Center) { Text("⚙", color = PracticeInk, fontSize = 17.sp) }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PracticeMode.values().forEach { item ->
                PracticeModeChip(item, selectedMode == item.name) {
                    selectedMode = item.name
                    cardIndex = 0
                        sessionCount = 0
                        sessionTarget = when (item) {
                            PracticeMode.REPEAT -> minOf(sessionSize, dueWords.size).coerceAtLeast(1)
                            PracticeMode.DAILY -> minOf(DAILY_CHALLENGE_SIZE, allWords.size)
                            else -> minOf(sessionSize, allWords.size)
                        }
                        sessionCorrect = 0
                        sessionFinished = false
                    typedAnswer = ""
                    hintVisible = false
                    answerRevealed = false
                    checked = false
                    selectedChoice = -1
                }
            }
        }
        Spacer(Modifier.height(15.dp))

        Card(shape = RoundedCornerShape(9.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE5EAE4))) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TIẾN ĐỘ ÔN TẬP", color = PracticeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.weight(1f))
                    Text("$sessionCount / $sessionTarget thẻ", color = PracticeMuted, fontSize = 10.sp)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (sessionCount / sessionTarget.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = PracticeGreen,
                    trackColor = PracticeMint
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (sessionFinished) {
            PracticeRoundSummary(
                mode = mode,
                correctAnswers = sessionCorrect,
                totalQuestions = sessionTarget,
                onContinue = {
                    sessionCount = 0
                    sessionCorrect = 0
                    cardIndex = 0
                    sessionTarget = when (mode) {
                        PracticeMode.REPEAT -> minOf(sessionSize, dueWords.size).coerceAtLeast(1)
                        PracticeMode.DAILY -> minOf(DAILY_CHALLENGE_SIZE, allWords.size)
                        else -> minOf(sessionSize, allWords.size)
                    }
                    sessionFinished = false
                },
                onExit = onExit
            )
        } else if (word == null) {
            EmptyPracticeState(mode = mode, dueCount = dueWords.size, onExit = onExit)
        } else {
            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE1E6E1))) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${mode.icon}  ${mode.title.uppercase()}", color = PracticeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                        Spacer(Modifier.weight(1f))
                        Text("Đã ôn ${state.reviewCount} lần", color = PracticeMuted, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(15.dp))
                    Surface(color = PracticeMint, shape = CircleShape, modifier = Modifier.size(54.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(word.imageCue, fontSize = 27.sp) }
                    }
                    Spacer(Modifier.height(12.dp))

                    when (mode) {
                        PracticeMode.GUESS, PracticeMode.REPEAT -> GuessPrompt(
                            word = word,
                            hintVisible = hintVisible,
                            answer = typedAnswer,
                            checked = checked,
                            onAnswerChange = { typedAnswer = it },
                            onHint = { hintVisible = true },
                            onSpeak = { speaker.speak(word.english) }
                        )
                        PracticeMode.LISTENING -> ListeningPrompt(
                            word = word,
                            answer = typedAnswer,
                            checked = checked,
                            hintVisible = hintVisible,
                            onAnswerChange = { typedAnswer = it },
                            onSpeak = { speaker.speak(word.english) },
                            onHint = { hintVisible = true }
                        )
                        PracticeMode.FLASHCARD -> FlashcardPrompt(
                            word = word,
                            revealed = answerRevealed,
                            onReveal = { answerRevealed = !answerRevealed },
                            onSpeak = { speaker.speak(word.english) }
                        )
                        PracticeMode.QUIZ, PracticeMode.REVERSE, PracticeMode.DAILY -> MultipleChoicePrompt(
                            word = word,
                            mode = mode,
                            options = choices,
                            selectedChoice = selectedChoice,
                            checked = checked,
                            wasCorrect = wasCorrect,
                            onSelect = { selectedChoice = it },
                            onSpeak = { speaker.speak(word.english) }
                        )
                    }

                    if (checked) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (wasCorrect) "Chính xác! Từ này đã được lên lịch ôn tiếp." else "Chưa đúng. Đáp án: ${word.english}",
                            color = if (wasCorrect) PracticeGreen else PracticeCoral,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(13.dp))
                    PracticeActions(
                        mode = mode,
                        checked = checked,
                        answerRevealed = answerRevealed,
                        typedAnswer = typedAnswer,
                        selectedChoice = selectedChoice,
                        onDontKnow = {
                            if (!checked) {
                                checked = true
                                wasCorrect = false
                                recordAnswer(false)
                            } else {
                                moveToNext()
                            }
                        },
                        onCheck = {
                            if (mode == PracticeMode.FLASHCARD) {
                                if (checked) {
                                    moveToNext()
                                } else if (answerRevealed) {
                                    recordAnswer(true)
                                    moveToNext()
                                } else answerRevealed = true
                            } else if (checked) {
                                moveToNext()
                            } else {
                                wasCorrect = when (mode) {
                                    PracticeMode.GUESS, PracticeMode.REPEAT -> isPracticeAnswerCorrect(typedAnswer, word.english)
                                    PracticeMode.QUIZ -> selectedChoice >= 0 && choices.getOrNull(selectedChoice)?.title == word.english
                                    PracticeMode.REVERSE -> selectedChoice >= 0 && choices.getOrNull(selectedChoice)?.title == word.vietnamese
                                    PracticeMode.DAILY -> selectedChoice >= 0 && choices.getOrNull(selectedChoice)?.title == word.english
                                    PracticeMode.LISTENING -> isPracticeAnswerCorrect(typedAnswer, word.english)
                                    PracticeMode.FLASHCARD -> answerRevealed
                                }
                                checked = true
                                recordAnswer(wasCorrect)
                            }
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("← Thoát luyện tập", modifier = Modifier.clickable(onClick = onExit).padding(vertical = 8.dp), color = PracticeMuted, fontSize = 12.sp)
            Text("⏱  Học ngắn, ôn đều", color = PracticeMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
    if (showSettings) {
        PracticeSettingsDialog(
            sessionSize = sessionSize,
            autoSpeak = autoSpeak,
            onSessionSizeChange = { size ->
                sessionSize = size
                sessionCount = 0
                sessionCorrect = 0
                cardIndex = 0
                sessionTarget = minOf(size, deck.size).coerceAtLeast(1)
                sessionFinished = false
            },
            onAutoSpeakChange = { autoSpeak = it },
            onDismiss = { showSettings = false }
        )
    }
    if (showSchedule) {
        ReviewScheduleDialog(
            dueCount = dueWords.size,
            favoriteCount = allWords.count { reviewStates[it.id]?.isFavorite == true },
            scheduledWords = allWords.mapNotNull { reviewStates[it.id]?.dueAtMillis?.takeIf { dueAt -> dueAt > now } },
            onDismiss = { showSchedule = false }
        )
    }
}

@Composable
private fun GuessPrompt(
    word: Word,
    hintVisible: Boolean,
    answer: String,
    checked: Boolean,
    onAnswerChange: (String) -> Unit,
    onHint: () -> Unit,
    onSpeak: () -> Unit
) {
    Text(word.vietnamese, color = PracticeInk, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    if (word.englishDefinition.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Text("Định nghĩa Tiếng Anh", color = PracticeMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(word.englishDefinition, color = PracticeMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(8.dp))
    Text("Đoán từ trong câu ví dụ:", color = PracticeMuted, fontSize = 12.sp)
    Spacer(Modifier.height(7.dp))
    Text(maskWordInExample(word.example, word.english), color = PracticeInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(11.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = !checked,
            label = { Text("Từ tiếng Anh") }
        )
        Spacer(Modifier.width(7.dp))
        Button(onClick = onHint, enabled = !hintVisible && !checked, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF0E8), contentColor = PracticeCoral), shape = RoundedCornerShape(7.dp)) {
            Text("Gợi ý", fontSize = 11.sp)
        }
    }
    if (hintVisible) {
        Spacer(Modifier.height(5.dp))
        Text("Gợi ý: bắt đầu bằng “${word.english.firstOrNull() ?: '?'}” · ${word.english.length} ký tự", color = PracticeCoral, fontSize = 11.sp)
    }
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onSpeak,
            modifier = Modifier.size(42.dp).background(PracticeMint, CircleShape)
                .semantics { contentDescription = "Nghe phát âm" }
        ) { Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = PracticeGreen, modifier = Modifier.size(23.dp)) }
        Text("  Nghe phát âm", color = PracticeGreen, fontSize = 11.sp)
    }
}

@Composable
private fun ListeningPrompt(
    word: Word,
    answer: String,
    checked: Boolean,
    hintVisible: Boolean,
    onAnswerChange: (String) -> Unit,
    onSpeak: () -> Unit,
    onHint: () -> Unit
) {
    Text("Nghe và viết lại từ", color = PracticeInk, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text("Nghe âm thanh, nhập từ bạn nghe được.", color = PracticeMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(12.dp))
    IconButton(
        onClick = onSpeak,
        modifier = Modifier.size(56.dp).background(PracticeMint, CircleShape)
            .semantics { contentDescription = "Phát lại từ tiếng Anh" }
    ) { Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = PracticeGreen, modifier = Modifier.size(30.dp)) }
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = answer,
        onValueChange = onAnswerChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !checked,
        label = { Text("Từ bạn nghe được") }
    )
    Spacer(Modifier.height(6.dp))
    TextButton(onClick = onHint, enabled = !hintVisible && !checked) { Text("Gợi ý chữ đầu", color = PracticeCoral) }
    if (hintVisible) {
        Text("${word.english.firstOrNull() ?: '?'} · ${word.english.length} ký tự", color = PracticeCoral, fontSize = 12.sp)
    }
}

@Composable
private fun FlashcardPrompt(word: Word, revealed: Boolean, onReveal: () -> Unit, onSpeak: () -> Unit) {
    Text(if (revealed) word.vietnamese else word.english, color = PracticeInk, fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    if (revealed) {
        Spacer(Modifier.height(8.dp))
        Text(word.english, color = PracticeGreen, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(word.pronunciation, color = PracticeMuted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text("“${word.example}”", color = PracticeMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(11.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onReveal, colors = ButtonDefaults.buttonColors(containerColor = PracticeGreen)) {
            Text(if (revealed) "Ẩn mặt sau" else "Lật thẻ")
        }
        Button(onClick = onSpeak, colors = ButtonDefaults.buttonColors(containerColor = PracticeMint, contentColor = PracticeGreen)) {
            Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Text("Nghe")
        }
    }
}

@Composable
private fun MultipleChoicePrompt(
    word: Word,
    mode: PracticeMode,
    options: List<PracticeChoice>,
    selectedChoice: Int,
    checked: Boolean,
    wasCorrect: Boolean,
    onSelect: (Int) -> Unit,
    onSpeak: () -> Unit
) {
    val reverse = mode == PracticeMode.REVERSE
    Text(if (reverse) word.english else word.vietnamese, color = PracticeInk, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    if (reverse) {
        Spacer(Modifier.height(5.dp))
        Text(word.pronunciation, color = PracticeMuted, fontSize = 12.sp)
    }
    Spacer(Modifier.height(7.dp))
    Text(if (reverse) "Chọn nghĩa Tiếng Việt:" else "Chọn đáp án đúng:", color = PracticeMuted, fontSize = 11.sp)
    Spacer(Modifier.height(5.dp))
    options.forEachIndexed { index, option ->
        val correctOption = option.title == if (reverse) word.vietnamese else word.english
        val background = when {
            checked && correctOption -> PracticeMint
            checked && index == selectedChoice && !wasCorrect -> Color(0xFFFFEEE8)
            index == selectedChoice -> Color(0xFFE6EFF4)
            else -> Color.White
        }
        val outline = when {
            checked && correctOption -> PracticeGreen
            checked && index == selectedChoice && !wasCorrect -> PracticeCoral
            index == selectedChoice -> PracticeGreen
            else -> Color(0xFFE5EAE4)
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(enabled = !checked) { onSelect(index) },
            color = background,
            shape = RoundedCornerShape(7.dp),
            border = BorderStroke(1.dp, outline)
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(('A' + index).toString(), color = PracticeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(option.title, color = PracticeInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (option.detail.isNotBlank()) Text(option.detail, color = PracticeMuted, fontSize = 10.sp)
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    Button(onClick = onSpeak, colors = ButtonDefaults.buttonColors(containerColor = PracticeMint, contentColor = PracticeGreen)) {
        Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text("Nghe từ")
    }
}

@Composable
private fun PracticeActions(
    mode: PracticeMode,
    checked: Boolean,
    answerRevealed: Boolean,
    typedAnswer: String,
    selectedChoice: Int,
    onDontKnow: () -> Unit,
    onCheck: () -> Unit
) {
    if (mode == PracticeMode.FLASHCARD && answerRevealed && !checked) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Button(onClick = onDontKnow, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEEE8), contentColor = PracticeCoral), shape = RoundedCornerShape(7.dp)) { Text("◉ Chưa nhớ") }
            Button(onClick = onCheck, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PracticeGreen), shape = RoundedCornerShape(7.dp)) { Text("✓ Đã nhớ") }
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Button(
            onClick = if (checked) onCheck else onDontKnow,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEEE8), contentColor = PracticeCoral),
            shape = RoundedCornerShape(7.dp)
        ) { Text(if (checked) "Bỏ qua" else "◉ Không biết") }
        val canCheck = when (mode) {
            PracticeMode.GUESS, PracticeMode.REPEAT, PracticeMode.LISTENING -> typedAnswer.isNotBlank() || checked
            PracticeMode.QUIZ, PracticeMode.REVERSE, PracticeMode.DAILY -> selectedChoice >= 0 || checked
            PracticeMode.FLASHCARD -> answerRevealed
        }
        Button(
            onClick = onCheck,
            enabled = canCheck,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = PracticeGreen, disabledContainerColor = Color(0xFFD8E0DA)),
            shape = RoundedCornerShape(7.dp)
        ) { Text(if (checked) "Câu tiếp theo →" else "✓ Kiểm tra") }
    }
}

@Composable
private fun PracticeModeChip(mode: PracticeMode, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) PracticeGreen else Color.White,
        shape = RoundedCornerShape(7.dp),
        border = if (selected) null else BorderStroke(1.dp, Color(0xFFE5EAE4))
    ) {
        Text("${mode.icon} ${mode.title}", color = if (selected) Color.White else PracticeInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp))
    }
}

@Composable
private fun PracticeSettingsDialog(
    sessionSize: Int,
    autoSpeak: Boolean,
    onSessionSizeChange: (Int) -> Unit,
    onAutoSpeakChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cài đặt luyện tập", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Số thẻ mỗi phiên", color = PracticeMuted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(5, 10, 20).forEach { size ->
                        SessionSizeChip(
                            size = size,
                            selected = sessionSize == size,
                            onClick = { onSessionSizeChange(size) }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tự đọc phát âm", color = PracticeInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Đọc từ mới khi chuyển thẻ", color = PracticeMuted, fontSize = 11.sp)
                    }
                    Switch(checked = autoSpeak, onCheckedChange = onAutoSpeakChange)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}

@Composable
private fun SessionSizeChip(size: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) PracticeGreen else PracticePaper,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text("$size thẻ", color = if (selected) Color.White else PracticeInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp))
    }
}

@Composable
private fun ReviewScheduleDialog(
    dueCount: Int,
    favoriteCount: Int,
    scheduledWords: List<Long>,
    onDismiss: () -> Unit
) {
    val earliestReview = scheduledWords.minOrNull()?.let {
        SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(Date(it))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lịch ôn từ vựng", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Hôm nay có $dueCount thẻ cần ôn.", color = PracticeInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("$favoriteCount từ đã lưu yêu thích.", color = PracticeMuted, fontSize = 12.sp)
                Text("${scheduledWords.size} từ đã có lịch ôn tiếp.", color = PracticeMuted, fontSize = 12.sp)
                Spacer(Modifier.height(11.dp))
                Text("Lịch lặp lại", color = PracticeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Chưa nhớ: sau 10 phút · Nhớ đúng: sau 1, 3 rồi 7 ngày.", color = PracticeMuted, fontSize = 12.sp, lineHeight = 18.sp)
                if (earliestReview != null) {
                    Spacer(Modifier.height(9.dp))
                    Text("Lần ôn tiếp theo: $earliestReview", color = PracticeInk, fontSize = 12.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}

@Composable
private fun PracticeRoundSummary(
    mode: PracticeMode,
    correctAnswers: Int,
    totalQuestions: Int,
    onContinue: () -> Unit,
    onExit: () -> Unit
) {
    Card(shape = RoundedCornerShape(9.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = PracticeMint, shape = CircleShape, modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("✦", color = PracticeGreen, fontSize = 27.sp) }
            }
            Spacer(Modifier.height(12.dp))
            Text("Hoàn thành phiên ${mode.title.lowercase()}!", color = PracticeInk, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Bạn nhớ đúng $correctAnswers / $totalQuestions thẻ.", color = PracticeMuted, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PracticeGreen), shape = RoundedCornerShape(7.dp)) {
                Text("Bắt đầu phiên mới")
            }
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PracticePaper, contentColor = PracticeInk), shape = RoundedCornerShape(7.dp)) {
                Text("Quay lại học")
            }
        }
    }
}

@Composable
private fun EmptyPracticeState(mode: PracticeMode, dueCount: Int, onExit: () -> Unit) {
    Card(shape = RoundedCornerShape(9.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (mode == PracticeMode.REPEAT) "✓" else "✦", color = PracticeGreen, fontSize = 34.sp)
            Spacer(Modifier.height(9.dp))
            Text(if (mode == PracticeMode.REPEAT) "Không có thẻ đến hạn" else "Chưa có từ vựng", color = PracticeInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(if (mode == PracticeMode.REPEAT) "Bạn đã ôn xong ${dueCount} từ cần ôn. Hãy quay lại sau." else "Mở bài học để thêm từ vào lộ trình.", color = PracticeMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = PracticeGreen)) { Text("Quay lại học") }
        }
    }
}

internal fun createPracticeChoices(word: Word, allWords: List<Word>, reverse: Boolean): List<PracticeChoice> {
    val correct = if (reverse) PracticeChoice(word.vietnamese) else PracticeChoice(word.english, word.pronunciation)
    val candidates = allWords.asSequence()
        .filter { it.id != word.id }
        .map { if (reverse) PracticeChoice(it.vietnamese) else PracticeChoice(it.english, it.pronunciation) }
        .distinctBy { it.title.lowercase() }
        .take(3)
        .toList()
    return (candidates + correct).shuffled()
}

internal fun isPracticeAnswerCorrect(answer: String, expected: String): Boolean =
    normalizePracticeAnswer(answer) == normalizePracticeAnswer(expected)

private fun normalizePracticeAnswer(value: String): String = value
    .trim()
    .lowercase()
    .replace(Regex("[.!?,;:'\"()…]"), "")
    .replace(Regex("\\s+"), " ")

internal fun maskWordInExample(example: String, answer: String): String {
    val masked = Regex(Regex.escape(answer), RegexOption.IGNORE_CASE)
        .replace(example) { "*".repeat(answer.length.coerceAtMost(12)) }
    return if (masked == example) "Gợi nghĩa: $answer" else masked
}