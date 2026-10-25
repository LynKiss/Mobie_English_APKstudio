package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ProfileInk = Color(0xFF202B35)
private val ProfileMuted = Color(0xFF78838A)
private val ProfileGreen = Color(0xFF277A60)
private val ProfileMint = Color(0xFFE2F1E9)
private val ProfilePaper = Color(0xFFF7F8F4)

@Composable
fun ProfileScreen(
    profile: UserProfile,
    totalLessons: Int,
    completedLessons: Int,
    correctAnswers: Int,
    onSave: (UserProfile) -> Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember(profile.id) { mutableStateOf(profile.fullName) }
    var phone by remember(profile.id) { mutableStateOf(profile.phone) }
    var birthDate by remember(profile.id) { mutableStateOf(profile.birthDate) }
    var englishLevel by remember(profile.id) { mutableStateOf(profile.englishLevel) }
    var dailyGoal by remember(profile.id) { mutableStateOf(profile.dailyGoalMinutes) }
    var saveMessage by remember(profile.id) { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxSize().background(ProfilePaper).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(21.dp))
        Text("Hồ sơ cá nhân", color = ProfileInk, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("Thông tin và mục tiêu học tập của bạn.", color = ProfileMuted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))

        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = ProfileMint, shape = CircleShape) {
                    Text(
                        fullName.trim().firstOrNull()?.uppercase() ?: "A",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        color = ProfileGreen,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(Modifier.padding(start = 13.dp)) {
                    Text(fullName.ifBlank { "Người học tiếng Anh" }, color = ProfileInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(profile.email, color = ProfileMuted, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(19.dp))
        Text("THÔNG TIN CÁ NHÂN", color = ProfileGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(fullName, { fullName = it; saveMessage = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Họ và tên") })
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(profile.email, {}, modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = true, label = { Text("Email đăng nhập") })
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(phone, { phone = it; saveMessage = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Số điện thoại (không bắt buộc)") })
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(birthDate, { birthDate = it; saveMessage = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Ngày sinh (dd/mm/yyyy)") })

        Spacer(Modifier.height(19.dp))
        Text("MỤC TIÊU HỌC TẬP", color = ProfileGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Spacer(Modifier.height(8.dp))
        Text("Trình độ hiện tại", color = ProfileInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Mới bắt đầu", "Cơ bản", "Trung cấp").forEach { level ->
                SelectableChip(level, selected = englishLevel == level, onClick = { englishLevel = level })
            }
        }
        Spacer(Modifier.height(15.dp))
        Text("Thời gian học mỗi ngày", color = ProfileInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 15, 20, 30).forEach { minutes ->
                SelectableChip("$minutes phút", selected = dailyGoal == minutes, onClick = { dailyGoal = minutes })
            }
        }

        Spacer(Modifier.height(19.dp))
        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = ProfileMint)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Bài học hoàn thành", color = ProfileMuted, fontSize = 11.sp)
                    Text("$completedLessons / $totalLessons", color = ProfileInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Câu trả lời đúng", color = ProfileMuted, fontSize = 11.sp)
                    Text("$correctAnswers", color = ProfileInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                if (fullName.trim().length < 2) {
                    saveMessage = "Họ tên cần có ít nhất 2 ký tự."
                } else if (birthDate.isNotBlank() && !birthDate.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
                    saveMessage = "Ngày sinh cần theo định dạng dd/mm/yyyy."
                } else {
                    val saved = onSave(profile.copy(
                        fullName = fullName.trim(),
                        phone = phone.trim(),
                        birthDate = birthDate.trim(),
                        englishLevel = englishLevel,
                        dailyGoalMinutes = dailyGoal
                    ))
                    saveMessage = if (saved) "Đã lưu hồ sơ của bạn." else "Không thể lưu hồ sơ. Vui lòng thử lại."
                }
            },
            modifier = Modifier.fillMaxWidth().height(49.dp),
            shape = RoundedCornerShape(7.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ProfileGreen)
        ) { Text("Lưu thay đổi", fontWeight = FontWeight.Bold) }
        if (saveMessage != null) {
            Text(saveMessage.orEmpty(), color = if (saveMessage == "Đã lưu hồ sơ của bạn.") ProfileGreen else Color(0xFFB64D3B), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(9.dp))
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(47.dp),
            shape = RoundedCornerShape(7.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFB64D3B))
        ) { Text("Đăng xuất", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) ProfileGreen else Color.White,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp), color = if (selected) Color.White else ProfileInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

