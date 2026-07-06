package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BiankyDeepBlue
import com.example.ui.theme.BiankyGray
import com.example.ui.theme.BiankyLightBlue
import com.example.ui.theme.BiankyRoyalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: (String, String, String, String, String) -> Unit,
    loginError: String?,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    
    // Role state: "ADMIN", "SUPERVISOR", "EMPLOYEE"
    var selectedRole by remember { mutableStateOf("EMPLOYEE") }
    // Shift state: "صباحي", "مسائي"
    var selectedShift by remember { mutableStateOf("صباحي") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // App Logo Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(BiankyLightBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "شعار بيانكي",
                    tint = BiankyDeepBlue,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // App Brand Name & Subtitle
            Text(
                text = "بيانكي لخدمات الأمن",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = BiankyDeepBlue,
                textAlign = TextAlign.Center
            )
            Text(
                text = "نظام الحضور والانصراف الذكي",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = BiankyGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Switcher Tab between Login and Register
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Register Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isRegisterMode) BiankyDeepBlue else Color.Transparent)
                        .clickable { 
                            isRegisterMode = true 
                            localError = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "إنشاء حساب جديد",
                        fontWeight = FontWeight.Bold,
                        color = if (isRegisterMode) Color.White else BiankyDeepBlue,
                        fontSize = 14.sp
                    )
                }

                // Login Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isRegisterMode) BiankyDeepBlue else Color.Transparent)
                        .clickable { 
                            isRegisterMode = false 
                            localError = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "تسجيل الدخول",
                        fontWeight = FontWeight.Bold,
                        color = if (!isRegisterMode) Color.White else BiankyDeepBlue,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full Name Input (Visible ONLY in Register Mode)
            if (isRegisterMode) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { 
                        fullName = it
                        localError = null
                    },
                    label = { Text("الاسم الكامل للموظف / Full Name") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = "Name", tint = BiankyDeepBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BiankyDeepBlue,
                        unfocusedTextColor = BiankyDeepBlue,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BiankyDeepBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = BiankyDeepBlue,
                        unfocusedLabelColor = BiankyGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fullname_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Username Input
            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it
                    localError = null
                },
                label = { Text("اسم المستخدم / Username") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username", tint = BiankyDeepBlue) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = BiankyDeepBlue,
                    unfocusedTextColor = BiankyDeepBlue,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = BiankyDeepBlue,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedLabelColor = BiankyDeepBlue,
                    unfocusedLabelColor = BiankyGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("username_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    localError = null
                },
                label = { Text("كلمة المرور / Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = BiankyDeepBlue) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "تبديل الرؤية"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = BiankyDeepBlue,
                    unfocusedTextColor = BiankyDeepBlue,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = BiankyDeepBlue,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedLabelColor = BiankyDeepBlue,
                    unfocusedLabelColor = BiankyGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            // Role and Shift selections (Visible ONLY in Register Mode)
            if (isRegisterMode) {
                Spacer(modifier = Modifier.height(10.dp))

                // Role Selection
                Text(
                    text = "حدد نوع الحساب / الدور الوظيفي:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiankyDeepBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Right
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Supervisor Card Choice
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedRole = "SUPERVISOR" }
                            .border(
                                width = if (selectedRole == "SUPERVISOR") 2.dp else 1.dp,
                                color = if (selectedRole == "SUPERVISOR") BiankyDeepBlue else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedRole == "SUPERVISOR") BiankyLightBlue else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupervisorAccount,
                                contentDescription = "مشرف",
                                tint = if (selectedRole == "SUPERVISOR") BiankyDeepBlue else BiankyGray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "مشرف",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedRole == "SUPERVISOR") BiankyDeepBlue else BiankyGray
                            )
                        }
                    }

                    // Employee Card Choice
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedRole = "EMPLOYEE" }
                            .border(
                                width = if (selectedRole == "EMPLOYEE") 2.dp else 1.dp,
                                color = if (selectedRole == "EMPLOYEE") BiankyDeepBlue else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedRole == "EMPLOYEE") BiankyLightBlue else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "موظف أمن",
                                tint = if (selectedRole == "EMPLOYEE") BiankyDeepBlue else BiankyGray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "موظف أمن",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedRole == "EMPLOYEE") BiankyDeepBlue else BiankyGray
                            )
                        }
                    }
                }

                // Shift Selection (Visible ONLY if Role is "EMPLOYEE" under Register Mode)
                if (selectedRole == "EMPLOYEE") {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "حدد شفت العمل (موظف الأمن):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiankyDeepBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.Right
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Morning Shift Card Choice
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedShift = "صباحي" }
                                .border(
                                    width = if (selectedShift == "صباحي") 2.dp else 1.dp,
                                    color = if (selectedShift == "صباحي") BiankyDeepBlue else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedShift == "صباحي") BiankyLightBlue else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = "شفت صباحي",
                                    tint = if (selectedShift == "صباحي") BiankyDeepBlue else BiankyGray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شفت صباحي",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedShift == "صباحي") BiankyDeepBlue else BiankyGray
                                )
                            }
                        }

                        // Evening Shift Card Choice
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedShift = "مسائي" }
                                .border(
                                    width = if (selectedShift == "مسائي") 2.dp else 1.dp,
                                    color = if (selectedShift == "مسائي") BiankyDeepBlue else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedShift == "مسائي") BiankyLightBlue else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = "شفت مسائي",
                                    tint = if (selectedShift == "مسائي") BiankyDeepBlue else BiankyGray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شفت مسائي",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedShift == "مسائي") BiankyDeepBlue else BiankyGray
                                )
                            }
                        }
                    }
                }
            }

            val displayError = localError ?: loginError
            if (displayError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = displayError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            Button(
                onClick = { 
                    if (isRegisterMode) {
                        val words = fullName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                        if (words.size < 4) {
                            localError = "الرجاء إدخال الاسم رباعي (4 أسماء على الأقل)"
                            return@Button
                        }
                        if (password.length < 8) {
                            localError = "يجب أن تكون كلمة المرور 8 أحرف أو أكثر"
                            return@Button
                        }
                        onRegisterClick(username, password, fullName, selectedRole, selectedShift)
                    } else {
                        onLoginClick(username, password)
                    }
                },
                enabled = username.isNotEmpty() && password.isNotEmpty() && (!isRegisterMode || fullName.isNotEmpty()),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BiankyDeepBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("login_button")
            ) {
                Text(
                    text = if (isRegisterMode) "إنشاء حساب جديد والدخول" else "تسجيل الدخول",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
