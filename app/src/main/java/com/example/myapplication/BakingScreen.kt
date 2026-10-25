package com.example.myapplication

import android.content.Context
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF202B35)
private val Muted = Color(0xFF78838A)
private val Paper = Color(0xFFF7F8F4)
private val Green = Color(0xFF277A60)
private val Mint = Color(0xFFE2F1E9)
private val Coral = Color(0xFFDE7358)
private val Yellow = Color(0xFFF2C66D)
private val Blue = Color(0xFFE6EFF4)

@Composable
fun BakingScreen() {
    val context = LocalContext.current
    val database = remember(context) { UserDatabase(context) }
    val sessionPreferences = remember(context) { context.getSharedPreferences("english_learning_session", Context.MODE_PRIVATE) }
    val initialUserId = remember(sessionPreferences) { sessionPreferences.getLong("user_id", -1L) }
    var userProfile by remember(database) { mutableStateOf(if (initialUserId >= 0L) database.getUser(initialUserId) else null) }
    var learningProgress by remember(userProfile?.id) {
        mutableStateOf(userProfile?.let { database.getProgress(it.id) } ?: LearningProgress(0, 0))
    }
    var dailyChallengeResult by remember(userProfile?.id) {
        mutableStateOf(userProfile?.let { database.getDailyChallengeResult(it.id) })
    }
    var studySummary by remember(userProfile?.id) {
        mutableStateOf(userProfile?.let { database.getStudySummary(it.id) })
    }
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedLesson by rememberSaveable { mutableIntStateOf(-1) }
    var quizMode by rememberSaveable { mutableStateOf(false) }
    var practiceStartMode by rememberSaveable { mutableStateOf("GUESS") }
    val completedLessons = java.lang.Long.bitCount(learningProgress.completedLessonMask)

    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
        primary = Green,
        secondary = Coral,
        background = Paper,
        surface = Color.White,
        onBackground = Ink,
        onSurface = Ink
    )) {
        if (userProfile == null) {
            AuthScreen(database) { signedInUser ->
                sessionPreferences.edit().putLong("user_id", signedInUser.id).apply()
                userProfile = signedInUser
                learningProgress = database.getProgress(signedInUser.id)
                dailyChallengeResult = database.getDailyChallengeResult(signedInUser.id)
            }
        } else {
            val signedInUser = userProfile!!
            fun refreshStudySummary() {
                scope.launch(Dispatchers.IO) {
                    val updatedSummary = database.getStudySummary(signedInUser.id)
                    withContext(kotlinx.coroutines.Dispatchers.Main) { studySummary = updatedSummary }
                }
            }
        Scaffold(
            containerColor = Paper,
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    listOf("⌂" to "Học", "▤" to "Từ vựng", "◉" to "Tiến độ", "○" to "Cá nhân").forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                selectedLesson = -1
                                quizMode = false
                            },
                            icon = { Text(item.first, fontSize = 21.sp) },
                            label = { Text(item.second, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            when {
                quizMode -> PracticeScreen(
                    database = database,
                    userId = signedInUser.id,
                    startMode = practiceStartMode,
                    onCorrectAnswer = {
                        val updatedProgress = learningProgress.copy(correctAnswers = learningProgress.correctAnswers + 1)
                        learningProgress = updatedProgress
                        scope.launch(Dispatchers.IO) { database.saveProgress(signedInUser.id, updatedProgress) }
                    },
                    onActivityChanged = ::refreshStudySummary,
                    onDailyChallengeCompleted = { correct, total ->
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        dailyChallengeResult = DailyChallengeResult(today, correct, total)
                    },
                    onExit = { quizMode = false },
                    modifier = Modifier.padding(innerPadding)
                )
                selectedLesson >= 0 -> LessonScreen(
                    lesson = lessons[selectedLesson],
                    isComplete = learningProgress.completedLessonMask and (1L shl selectedLesson) != 0L,
                    onBack = { selectedLesson = -1 },
                    onQuiz = {
                        quizMode = true
                        practiceStartMode = "GUESS"
                    },
                    onComplete = {
                        val updatedProgress = learningProgress.copy(
                            completedLessonMask = learningProgress.completedLessonMask or (1L shl selectedLesson)
                        )
                        learningProgress = updatedProgress
                        scope.launch(Dispatchers.IO) {
                            database.saveProgress(signedInUser.id, updatedProgress)
                            database.recordLessonCompleted(signedInUser.id)
                            val updatedSummary = database.getStudySummary(signedInUser.id)
                            withContext(kotlinx.coroutines.Dispatchers.Main) { studySummary = updatedSummary }
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                else -> when (selectedTab) {
                    1 -> FlashcardScreen(database, signedInUser.id, Modifier.padding(innerPadding))
                    2 -> ProgressScreen(
                        learningProgress.completedLessonMask,
                        learningProgress.correctAnswers,
                        studySummary = studySummary ?: StudySummary(0, DailyActivity(""), emptyList()),
                        dailyGoalMinutes = signedInUser.dailyGoalMinutes,
                        onOpenLesson = { selectedLesson = it },
                        modifier = Modifier.padding(innerPadding)
                    )
                    3 -> ProfileScreen(
                        profile = signedInUser,
                        totalLessons = lessons.size,
                        completedLessons = completedLessons,
                        correctAnswers = learningProgress.correctAnswers,
                        onSave = { updatedProfile ->
                            val saved = database.updateProfile(updatedProfile)
                            if (saved) userProfile = updatedProfile
                            saved
                        },
                        onLogout = {
                            sessionPreferences.edit().remove("user_id").apply()
                            userProfile = null
                            learningProgress = LearningProgress(0, 0)
                            selectedTab = 0
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                    else -> HomeScreen(
                        learnerName = signedInUser.fullName,
                        dailyGoalMinutes = signedInUser.dailyGoalMinutes,
                        completedLessonMask = learningProgress.completedLessonMask,
                        onOpenLesson = { selectedLesson = it },
                        onOpenFlashcards = { selectedTab = 1 },
                        onStartQuiz = {
                            quizMode = true
                            practiceStartMode = "GUESS"
                        },
                        dailyChallengeResult = dailyChallengeResult,
                        onStartDailyChallenge = {
                            quizMode = true
                            practiceStartMode = "DAILY"
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun HomeScreen(
    learnerName: String,
    dailyGoalMinutes: Int,
    completedLessonMask: Long,
    onOpenLesson: (Int) -> Unit,
    onOpenFlashcards: () -> Unit,
    onStartQuiz: () -> Unit,
    dailyChallengeResult: DailyChallengeResult?,
    onStartDailyChallenge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedLessons = java.lang.Long.bitCount(completedLessonMask)
    val nextLessonIndex = lessons.indices.firstOrNull { completedLessonMask and (1L shl it) == 0L } ?: 0
    var expandedStage by rememberSaveable(completedLessonMask) {
        mutableIntStateOf(lessons[nextLessonIndex].stage)
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(SimpleDateFormat("EEEE, dd MMM", Locale.ENGLISH).format(Date()).uppercase(Locale.ENGLISH), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(5.dp))
                Text("Chào ${learnerName.substringBefore(' ')}!", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            Surface(shape = CircleShape, color = Yellow.copy(alpha = .28f), modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("✦", fontSize = 21.sp, color = Coral) }
            }
        }
        Spacer(Modifier.height(22.dp))
        Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF205A4B))) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(alpha = .15f), shape = RoundedCornerShape(5.dp)) {
                        Text(" ✦ HÀNH TRÌNH HÔM NAY ", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 7.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("◷ $dailyGoalMinutes PHÚT / NGÀY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(17.dp))
                Text("Mỗi ngày một chút,\ntiếng Anh của bạn sẽ\nthật khác.", color = Color.White, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(17.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { (completedLessons / lessons.size.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = Yellow,
                        trackColor = Color.White.copy(alpha = .22f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("$completedLessons / ${lessons.size} bài", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        SectionHeading("Học tiếp", "Xem tất cả")
        Spacer(Modifier.height(11.dp))
        LessonCard(lessons[nextLessonIndex], if (completedLessons == lessons.size) "ÔN TẬP BÀI HỌC" else "BÀI TIẾP THEO", onClick = { onOpenLesson(nextLessonIndex) })
        Spacer(Modifier.height(23.dp))
        SectionHeading("Lộ trình học", "${courseStages.size} chặng")
        Spacer(Modifier.height(10.dp))
        courseStages.forEach { stage ->
            val stageLessons = lessons.withIndex().filter { it.value.stage == stage.number }
            val stageCompleted = stageLessons.count { (_, lesson) ->
                val index = lessons.indexOf(lesson)
                completedLessonMask and (1L shl index) != 0L
            }
            val isExpanded = expandedStage == stage.number
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedStage = if (isExpanded) -1 else stage.number }
                    .padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("CHẶNG ${stage.number}  ·  ${stage.title.uppercase()}", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("$stageCompleted/${stageLessons.size} bài hoàn thành · ${stage.description}", color = Muted, fontSize = 11.sp, maxLines = 2)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (isExpanded) "⌃" else "⌄", color = Green, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            if (isExpanded) {
                stageLessons.forEach { (index, lesson) ->
                    RoadmapLessonRow(
                        lesson = lesson,
                        number = index + 1,
                        isComplete = completedLessonMask and (1L shl index) != 0L,
                        isNext = index == nextLessonIndex && completedLessons < lessons.size,
                        onClick = { onOpenLesson(index) }
                    )
                    Spacer(Modifier.height(7.dp))
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        SectionHeading("Ôn tập thông minh", "${lessons.sumOf { it.vocabulary.size }} thẻ")
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFlashcards),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE6EFF4)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("▤", color = Green, fontSize = 22.sp)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("Flashcard từ vựng", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                    Text("Lật thẻ · nghe phát âm · ôn theo lịch", fontSize = 12.sp, color = Muted)
                }
                Text("→", color = Green, fontSize = 21.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onStartDailyChallenge),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF4EE)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✦", color = Green, fontSize = 23.sp)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("Thử thách mỗi ngày", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                    Text(
                        if (dailyChallengeResult != null) "Hôm nay: ${dailyChallengeResult.correctAnswers}/${dailyChallengeResult.totalQuestions} đúng · làm lại để luyện thêm"
                        else "5 câu mới mỗi ngày · hoàn thành để ghi điểm",
                        fontSize = 11.sp,
                        color = Muted
                    )
                }
                Text("→", color = Green, fontSize = 21.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onStartQuiz),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0E8)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✎", color = Coral, fontSize = 22.sp)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("Kiểm tra nhanh", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                    Text("5 câu hỏi · xem giải thích sau mỗi câu", fontSize = 12.sp, color = Muted)
                }
                Text("→", color = Coral, fontSize = 21.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun SectionHeading(title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.weight(1f))
        Text(trailing, fontSize = 12.sp, color = Green, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LessonCard(lesson: Lesson, label: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = lesson.color, shape = RoundedCornerShape(7.dp), modifier = Modifier.size(55.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(lesson.icon, fontSize = 25.sp, color = Green) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Green, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(lesson.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text("${lesson.duration}  ·  ${lesson.vocabulary.size} từ mới", fontSize = 11.sp, color = Muted)
            }
            Text("→", fontSize = 21.sp, color = Green)
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = lesson.color, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(43.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(lesson.icon, fontSize = 21.sp, color = Green) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(lesson.category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.sp)
                Text(lesson.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            }
            Text(lesson.duration, fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
private fun RoadmapLessonRow(lesson: Lesson, number: Int, isComplete: Boolean, isNext: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isNext) Color(0xFFEAF4EE) else Color.White)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = lesson.color, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(39.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(lesson.icon, fontSize = 20.sp, color = Green) }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("BÀI %02d  ·  %s".format(number, lesson.category), color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
                Text(lesson.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (isComplete) "✓" else if (isNext) "BẮT ĐẦU" else "○", color = if (isComplete) Green else if (isNext) Green else Muted, fontSize = if (isNext) 9.sp else 15.sp, fontWeight = FontWeight.Bold)
                Text(lesson.duration, color = Muted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun LessonScreen(lesson: Lesson, isComplete: Boolean, onBack: () -> Unit, onQuiz: () -> Unit, onComplete: () -> Unit, modifier: Modifier = Modifier) {
    val speaker = rememberSpeechController()
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(14.dp))
        Text("‹   Tất cả bài học", modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp), color = Green, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Surface(color = lesson.color, shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text(lesson.category, color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(9.dp))
                Text(lesson.title, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                Spacer(Modifier.height(7.dp))
                Text(lesson.subtitle, color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(15.dp))
                Text("◷  ${lesson.duration}     ·     ${lesson.vocabulary.size} từ vựng", color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(23.dp))
        SectionHeading("Từ vựng trọng tâm", "${lesson.vocabulary.size} từ")
        Spacer(Modifier.height(6.dp))
        Text("Chạm vào một từ để ghi nhớ cách dùng.", fontSize = 12.sp, color = Muted)
        Spacer(Modifier.height(11.dp))
        lesson.vocabulary.forEachIndexed { index, word ->
            WordCard(word, index + 1, onSpeak = { speaker.speak(word.english) })
            Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.height(11.dp))
        Button(onClick = onQuiz, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(7.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
            Text("Bắt đầu luyện tập  →", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(9.dp))
        Button(
            onClick = { if (!isComplete) onComplete() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(7.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isComplete) Mint else Color.White, contentColor = if (isComplete) Green else Ink)
        ) { Text(if (isComplete) "✓  Đã hoàn thành bài học" else "Đánh dấu đã hoàn thành", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun WordCard(word: Word, index: Int, onSpeak: () -> Unit) {
    var expanded by rememberSaveable(word.english) { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Mint, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(word.imageCue, fontSize = 21.sp) }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable { expanded = !expanded }) {
                    Text("%02d  ·  %s".format(index, word.english), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(word.pronunciation, color = Muted, fontSize = 11.sp)
                }
                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier.size(42.dp).semantics { contentDescription = "Nghe ${word.english}" }
                ) { Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Green, modifier = Modifier.size(23.dp)) }
            }
            Text(word.vietnamese, modifier = Modifier.padding(start = 52.dp, top = 6.dp), color = Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Surface(color = Paper, shape = RoundedCornerShape(5.dp), modifier = Modifier.fillMaxWidth().padding(start = 36.dp)) {
                    Text("“${word.example}”", modifier = Modifier.padding(10.dp), color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProgressScreen(
    completedLessonMask: Long,
    correctAnswers: Int,
    studySummary: StudySummary,
    dailyGoalMinutes: Int,
    onOpenLesson: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedLessons = java.lang.Long.bitCount(completedLessonMask)
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(21.dp))
        Text("Tiến độ của bạn", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("Chậm mà chắc, mỗi ngày một bước.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("BÀI HOÀN THÀNH", "$completedLessons", Mint, Modifier.weight(1f))
            StatCard("CÂU TRẢ LỜI ĐÚNG", "$correctAnswers", Blue, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("CHUỖI NGÀY HỌC", "${studySummary.currentStreak} ngày", Color(0xFFFFF0E8), Modifier.weight(1f))
            StatCard("THẺ ĐÃ ÔN", "${studySummary.lastSevenDays.sumOf { it.reviewedCards }}", Mint, Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        SectionHeading("Mục tiêu hôm nay", "${dailyGoalMinutes} phút")
        Spacer(Modifier.height(10.dp))
        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                val goalSeconds = dailyGoalMinutes.coerceAtLeast(1) * 60L
                val todaySeconds = studySummary.today.studySeconds
                val goalReached = todaySeconds >= goalSeconds
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (goalReached) "Đã đạt mục tiêu học hôm nay" else "Bạn đã học hôm nay", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${todaySeconds / 60} / $dailyGoalMinutes phút", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (todaySeconds.toFloat() / goalSeconds).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                    color = if (goalReached) Color(0xFF277A60) else Yellow,
                    trackColor = Mint
                )
                Spacer(Modifier.height(7.dp))
                Text("${studySummary.today.reviewedCards} thẻ đã ôn · ${studySummary.today.completedLessons} bài hoàn thành", color = Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionHeading("Hoạt động 7 ngày", "${studySummary.lastSevenDays.sumOf { it.studySeconds } / 60} phút")
        Spacer(Modifier.height(10.dp))
        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp)) {
                val maxSeconds = studySummary.lastSevenDays.maxOfOrNull { it.studySeconds }?.coerceAtLeast(60L) ?: 60L
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    studySummary.lastSevenDays.forEach { day ->
                        val label = runCatching {
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(day.dateKey)
                            if (date == null) "-" else SimpleDateFormat("EE", Locale("vi", "VN")).format(date)
                        }.getOrDefault("-").take(2)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text(if (day.studySeconds > 0) "${day.studySeconds / 60}" else "", color = Muted, fontSize = 9.sp)
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = if (day.studySeconds > 0) Green else Paper,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.width(25.dp).height((48f * (day.studySeconds.toFloat() / maxSeconds)).coerceAtLeast(4f).dp)
                            ) { }
                            Spacer(Modifier.height(5.dp))
                            Text(label, color = Muted, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        SectionHeading("Mục tiêu lộ trình", "${completedLessons.coerceAtMost(lessons.size)}/${lessons.size}")
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Hoàn thành lộ trình", fontSize = 14.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("${completedLessons.coerceAtMost(lessons.size)} / ${lessons.size}", fontSize = 12.sp, color = Green, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { (completedLessons / lessons.size.toFloat()).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(7.dp), color = Green, trackColor = Mint)
                Spacer(Modifier.height(8.dp))
                Text("Mỗi bài học đưa bạn đến gần mục tiêu hơn.", fontSize = 11.sp, color = Muted)
            }
        }
        Spacer(Modifier.height(22.dp))
        SectionHeading("Lộ trình học", "${lessons.size} bài")
        Spacer(Modifier.height(12.dp))
        lessons.forEachIndexed { index, lesson ->
            val completed = completedLessonMask and (1L shl index) != 0L
            LessonRow(lesson, onClick = { onOpenLesson(index) })
            Text(
                if (completed) "✓  Đã hoàn thành" else "○  Chưa hoàn thành",
                color = if (completed) Green else Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 58.dp, top = 4.dp, bottom = 8.dp)
            )
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun StatCard(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = tint, shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp, maxLines = 2)
            Spacer(Modifier.height(7.dp))
            Text(value, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
    }
}

