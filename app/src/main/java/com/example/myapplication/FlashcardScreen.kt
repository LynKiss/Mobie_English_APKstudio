package com.example.myapplication

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.VolumeUp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CardInk = Color(0xFF202B35)
private val CardMuted = Color(0xFF78838A)
private val CardGreen = Color(0xFF277A60)
private val CardMint = Color(0xFFE2F1E9)
private val CardPaper = Color(0xFFF7F8F4)
private val CardCoral = Color(0xFFDE7358)

@Composable
fun FlashcardScreen(database: UserDatabase, userId: Long, modifier: Modifier = Modifier) {
    var reviewStates by remember(userId) { mutableStateOf(database.getWordReviewStates(userId)) }
    var selectedFilter by rememberSaveable(userId) { mutableIntStateOf(0) }
    var selectedView by rememberSaveable(userId) { mutableIntStateOf(0) }
    var cardIndex by rememberSaveable(userId) { mutableIntStateOf(0) }
    var showAnswer by rememberSaveable(userId) { mutableStateOf(false) }
    var searchText by rememberSaveable(userId) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val speaker = rememberSpeechController()
    val allWords = remember { lessons.flatMap { it.vocabulary } }
    val now = System.currentTimeMillis()
    val filteredWords = allWords.filter { word ->
        val state = reviewStates[word.id] ?: WordReviewState()
        val matchesFilter = when (selectedFilter) {
            1 -> state.dueAtMillis <= now
            2 -> state.isFavorite
            else -> true
        }
        val query = searchText.trim()
        matchesFilter && (query.isBlank() || word.english.contains(query, true) || word.vietnamese.contains(query, true))
    }
    val currentWord = filteredWords.getOrNull(cardIndex.coerceAtMost((filteredWords.size - 1).coerceAtLeast(0)))
    val currentState = currentWord?.let { reviewStates[it.id] ?: WordReviewState() } ?: WordReviewState()
    val dueCount = allWords.count { (reviewStates[it.id]?.dueAtMillis ?: 0L) <= now }
    val favoriteCount = allWords.count { reviewStates[it.id]?.isFavorite == true }

    Column(modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(20.dp))
        Text(if (selectedView == 0) "Từ vựng của tôi" else "Sổ tay từ vựng", color = CardInk, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            if (selectedView == 0) "Tra cứu, lưu từ hay và xem ví dụ trong lộ trình của bạn."
            else "Lật thẻ, nghe phát âm và ôn từ đúng thời điểm.",
            color = CardMuted,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlashFilter("Sổ từ", selectedView == 0) { selectedView = 0 }
            FlashFilter("Ôn flashcard", selectedView == 1) { selectedView = 1; showAnswer = false }
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlashFilter("Tất cả · ${allWords.size}", selectedFilter == 0) { selectedFilter = 0; cardIndex = 0; showAnswer = false }
            FlashFilter("Đến hạn · $dueCount", selectedFilter == 1) { selectedFilter = 1; cardIndex = 0; showAnswer = false }
            FlashFilter("Đã lưu · $favoriteCount", selectedFilter == 2) { selectedFilter = 2; cardIndex = 0; showAnswer = false }
        }
        Spacer(Modifier.height(11.dp))
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it; cardIndex = 0; showAnswer = false },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Tìm từ tiếng Anh hoặc nghĩa") }
        )
        Spacer(Modifier.height(12.dp))

        if (selectedView == 0) {
            if (filteredWords.isEmpty()) {
                Surface(color = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (selectedFilter == 2) {
                            Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = CardGreen, modifier = Modifier.size(28.dp))
                        } else {
                            Text("⌕", color = CardGreen, fontSize = 28.sp)
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            when {
                                searchText.isNotBlank() -> "Không tìm thấy từ phù hợp."
                                selectedFilter == 2 -> "Sổ tay chưa có từ đã lưu."
                                else -> "Không có từ nào đến hạn ôn."
                            },
                            color = CardInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Thử tìm từ khác hoặc đổi bộ lọc.", color = CardMuted, fontSize = 12.sp)
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${filteredWords.size} từ", color = CardMuted, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text("Chạm biểu tượng trái tim để lưu", color = CardMuted, fontSize = 10.sp)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(filteredWords, key = { it.id }) { word ->
                        val wordState = reviewStates[word.id] ?: WordReviewState()
                        NotebookWordRow(
                            word = word,
                            state = wordState,
                            onSpeak = { speaker.speak(word.english) },
                            onToggleFavorite = {
                                val favorite = !wordState.isFavorite
                                reviewStates = reviewStates + (word.id to wordState.copy(isFavorite = favorite))
                                scope.launch(Dispatchers.IO) { database.setWordFavorite(userId, word.id, favorite) }
                            }
                        )
                    }
                }
            }
        } else if (currentWord == null) {
            Surface(color = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedFilter == 2) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = CardGreen, modifier = Modifier.size(30.dp))
                    } else {
                        Text("✓", color = CardGreen, fontSize = 30.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            searchText.isNotBlank() -> "Không tìm thấy từ phù hợp."
                            selectedFilter == 2 -> "Chưa có từ yêu thích."
                            else -> "Bạn đã ôn xong các từ đến hạn."
                        },
                        color = CardInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Chọn bộ lọc khác để tiếp tục học.", color = CardMuted, fontSize = 12.sp)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("THẺ ${cardIndex + 1} / ${filteredWords.size}", color = CardMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text("Đã ôn ${currentState.reviewCount} lần", color = CardMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { (cardIndex + 1f) / filteredWords.size.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = CardGreen,
                trackColor = CardMint
            )
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE5EAE4))
            ) {
                Column(
                    Modifier.fillMaxSize().padding(19.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(color = CardMint, shape = CircleShape, modifier = Modifier.size(76.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(currentWord.imageCue, fontSize = 35.sp) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(if (showAnswer) "NGHĨA TIẾNG VIỆT" else "TỪ TIẾNG ANH", color = CardGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(9.dp))
                    Text(
                        if (showAnswer) currentWord.vietnamese else currentWord.english,
                        color = CardInk,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (showAnswer) {
                        Spacer(Modifier.height(9.dp))
                        Text(currentWord.english, color = CardGreen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        Text(currentWord.pronunciation, color = CardMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(13.dp))
                        Surface(color = CardPaper, shape = RoundedCornerShape(6.dp)) {
                            Text("“${currentWord.example}”", color = CardMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                        }
                    } else {
                        Spacer(Modifier.height(13.dp))
                        Text("Hãy thử nhớ nghĩa trước khi lật thẻ", color = CardMuted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(13.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { speaker.speak(currentWord.english) },
                            modifier = Modifier.size(44.dp)
                                .semantics { contentDescription = "Nghe ${currentWord.english}" }
                        ) { Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = CardGreen, modifier = Modifier.size(24.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Text("Nghe phát âm", color = CardGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                val nextFavorite = !currentState.isFavorite
                                reviewStates = reviewStates + (currentWord.id to currentState.copy(isFavorite = nextFavorite))
                                scope.launch(Dispatchers.IO) { database.setWordFavorite(userId, currentWord.id, nextFavorite) }
                            },
                            modifier = Modifier.size(44.dp)
                                .semantics { contentDescription = if (currentState.isFavorite) "Bỏ yêu thích" else "Lưu yêu thích" }
                        ) {
                            Icon(
                                imageVector = if (currentState.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = if (currentState.isFavorite) CardCoral else CardMuted,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(11.dp))
            if (!showAnswer) {
                Button(
                    onClick = { showAnswer = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardGreen)
                ) { Text("Lật thẻ xem nghĩa", fontWeight = FontWeight.Bold) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(
                        onClick = {
                            val word = currentWord
                            scope.launch {
                                val updated = withContext(Dispatchers.IO) { database.recordWordReview(userId, word.id, remembered = false) }
                                reviewStates = reviewStates + (word.id to updated)
                                cardIndex = nextCardIndex(cardIndex, filteredWords.size, selectedFilter)
                                showAnswer = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEEE8), contentColor = CardCoral)
                    ) { Text("↻  Chưa nhớ", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = {
                            val word = currentWord
                            scope.launch {
                                val updated = withContext(Dispatchers.IO) { database.recordWordReview(userId, word.id, remembered = true) }
                                reviewStates = reviewStates + (word.id to updated)
                                cardIndex = nextCardIndex(cardIndex, filteredWords.size, selectedFilter)
                                showAnswer = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardGreen)
                    ) { Text("✓  Đã nhớ", fontWeight = FontWeight.Bold) }
                }
            }
        }
        Spacer(Modifier.height(13.dp))
    }
}

@Composable
private fun NotebookWordRow(
    word: Word,
    state: WordReviewState,
    onSpeak: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE8ECE7))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = CardMint, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(word.imageCue, fontSize = 21.sp) }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(word.english, color = CardInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(word.pronunciation, color = CardMuted, fontSize = 11.sp)
                }
                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier.size(40.dp)
                        .semantics { contentDescription = "Nghe ${word.english}" }
                ) { Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = CardGreen, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(40.dp)
                        .semantics { contentDescription = if (state.isFavorite) "Bỏ yêu thích" else "Lưu yêu thích" }
                ) {
                    Icon(
                        imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (state.isFavorite) CardCoral else CardMuted,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(word.vietnamese, color = CardGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (word.englishDefinition.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(word.englishDefinition, color = CardMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.height(5.dp))
            Surface(color = CardPaper, shape = RoundedCornerShape(5.dp), modifier = Modifier.fillMaxWidth()) {
                Text("“${word.example}”", color = CardMuted, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp))
            }
            if (state.reviewCount > 0 || state.dueAtMillis > 0L) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (state.dueAtMillis <= System.currentTimeMillis()) "Đã ôn ${state.reviewCount} lần · đến hạn ôn"
                    else "Đã ôn ${state.reviewCount} lần · có lịch ôn tiếp",
                    color = CardMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

internal fun nextCardIndex(currentIndex: Int, deckSize: Int, filter: Int): Int = when {
    deckSize <= 1 || filter == 1 -> 0
    else -> (currentIndex + 1) % deckSize
}

@Composable
private fun FlashFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) CardGreen else Color.White,
        shape = RoundedCornerShape(6.dp),
        border = if (selected) null else BorderStroke(1.dp, Color(0xFFE5EAE4))
    ) {
        Text(label, color = if (selected) Color.White else CardInk, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp))
    }
}
