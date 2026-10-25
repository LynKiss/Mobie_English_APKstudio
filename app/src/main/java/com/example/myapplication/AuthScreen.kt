package com.example.myapplication

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AuthInk = Color(0xFF202B35)
private val AuthMuted = Color(0xFF78838A)
private val AuthGreen = Color(0xFF277A60)
private val AuthPaper = Color(0xFFF7F8F4)
private val AuthMint = Color(0xFFE2F1E9)

@Composable
fun AuthScreen(database: UserDatabase, onAuthenticated: (UserProfile) -> Unit) {
    var isRegistering by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().background(AuthPaper).verticalScroll(rememberScrollState()).padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(38.dp))
        Surface(color = AuthMint, shape = CircleShape, modifier = Modifier.size(68.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("A", color = AuthGreen, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(18.dp))
        Text("English, mỗi ngày.", color = AuthInk, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("Một hành trình nhỏ, một phiên bản tốt hơn.", color = AuthMuted, fontSize = 13.sp)
        Spacer(Modifier.height(25.dp))

        Surface(color = Color.White, shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(if (isRegistering) "Tạo tài khoản" else "Chào mừng trở lại", color = AuthInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(if (isRegistering) "Lưu hành trình học tập của riêng bạn." else "Đăng nhập để tiếp tục học tiếng Anh.", color = AuthMuted, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))

                if (isRegistering) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Họ và tên") }
                    )
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Email") }
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text("Mật khẩu") }
                )
                if (isRegistering) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text("Nhập lại mật khẩu") }
                    )
                }
                if (errorMessage != null) {
                    Spacer(Modifier.height(9.dp))
                    Text(errorMessage.orEmpty(), color = Color(0xFFB64D3B), fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val validation = if (isRegistering) {
                            validateRegistration(fullName, email, password, confirmPassword)
                        } else {
                            validateLogin(email, password)
                        }
                        if (validation != null) {
                            errorMessage = validation
                        } else {
                            val submittedName = fullName
                            val submittedEmail = email
                            val submittedPassword = password.toCharArray()
                            val registering = isRegistering
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                var storageError: String? = null
                                val result = try {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            if (registering) {
                                                database.register(submittedName, submittedEmail, submittedPassword)
                                            } else {
                                                database.authenticate(submittedEmail, submittedPassword)
                                            }
                                        } finally {
                                            submittedPassword.fill('\u0000')
                                        }
                                    }
                                } catch (exception: Exception) {
                                    storageError = exception.localizedMessage ?: "Lỗi không xác định."
                                    null
                                }
                                isLoading = false
                                if (storageError != null) {
                                    errorMessage = "Không thể xử lý tài khoản: $storageError"
                                } else if (result == null) {
                                    errorMessage = if (registering) "Email này đã được đăng ký." else "Email hoặc mật khẩu chưa chính xác."
                                } else {
                                    onAuthenticated(result)
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(49.dp),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AuthGreen)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(if (isRegistering) "Tạo tài khoản" else "Đăng nhập", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(13.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isRegistering) "Đã có tài khoản?" else "Chưa có tài khoản?", color = AuthMuted, fontSize = 12.sp)
                    Button(
                        onClick = { isRegistering = !isRegistering; errorMessage = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = AuthGreen)
                    ) { Text(if (isRegistering) "Đăng nhập" else "Đăng ký", fontWeight = FontWeight.Bold) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Tài khoản được lưu trên thiết bị này. Hãy dùng mật khẩu riêng.", color = AuthMuted, fontSize = 11.sp)
        Spacer(Modifier.height(28.dp))
    }
}

internal fun validateLogin(email: String, password: String): String? = when {
    !isValidEmail(email) -> "Vui lòng nhập email hợp lệ."
    password.isBlank() -> "Vui lòng nhập mật khẩu."
    else -> null
}

internal fun validateRegistration(fullName: String, email: String, password: String, confirmation: String): String? = when {
    fullName.trim().length < 2 -> "Họ tên cần có ít nhất 2 ký tự."
    !isValidEmail(email) -> "Vui lòng nhập email hợp lệ."
    password.length < 8 -> "Mật khẩu cần có ít nhất 8 ký tự."
    password != confirmation -> "Mật khẩu nhập lại chưa khớp."
    else -> null
}

private fun isValidEmail(email: String): Boolean =
    email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
