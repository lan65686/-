package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.User
import com.example.ui.components.QRCodeImage
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeScreen(
    user: User,
    qrToken: String,
    secondsRemaining: Int,
    showEmployeeQr: Boolean,
    onGenerateClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAvatarUpdate: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = secondsRemaining / 10f
    val animatedProgress by animateFloatAsState(targetValue = progress)
    var showProfileDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val profileFile = File(context.filesDir, "profile_${user.username}.jpg")
                    val outputStream = FileOutputStream(profileFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    onAvatarUpdate(user.username, profileFile.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "بيانكي - موظف الأمن",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    // Empty actions - logout button moved below profile
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BiankyDeepBlue
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Card Header
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BiankyLightBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProfileDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar with Image / Fallback / Edit overlay
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(user.avatarColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.avatarUri != null && File(user.avatarUri).exists()) {
                            AsyncImage(
                                model = File(user.avatarUri),
                                contentDescription = "الصورة الشخصية",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = user.name.take(1),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        // Small Edit overlay indicator
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل الصورة",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(bottom = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = user.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiankyDeepBlue
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "شفت",
                                tint = BiankyGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "شفت العمل: ${user.shift}",
                                fontSize = 12.sp,
                                color = BiankyGray
                            )
                        }
                    }

                    // Role Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BiankyDeepBlue)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "حارس أمن",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Logout Button under profile
            OutlinedButton(
                onClick = onLogoutClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BiankyError),
                border = BorderStroke(1.dp, BiankyError),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ExitToApp,
                    contentDescription = "تسجيل الخروج",
                    tint = BiankyError,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تسجيل الخروج",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiankyError
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Anti-Fraud Device Binding Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhonelinkLock,
                    contentDescription = "ربط الجهاز",
                    tint = BiankySuccess,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "الحساب مقترن بجهازك الحالي ومحمي ضد التكرار",
                    fontSize = 11.sp,
                    color = BiankyGray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (!showEmployeeQr) {
                // INITIAL ONE BUTTON STATE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onGenerateClick,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BiankyDeepBlue),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "إنشاء الكود",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "إنشاء الكود",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "اضغط على الزر لتوليد رمز الاستجابة السريعة (QR) الخاص بتسجيل حضورك أو انصرافك اليوم.",
                            fontSize = 12.sp,
                            color = BiankyGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                // QR ACTIVE STATE
                Text(
                    text = "رمز الحضور والانصراف المؤقت",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiankyDeepBlue
                )
                Text(
                    text = "Dynamic QR Code",
                    fontSize = 12.sp,
                    color = BiankyGray,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                // QR Code Container with nice Border/Shadow
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .size(260.dp)
                        .padding(8.dp)
                ) {
                    if (qrToken.isNotEmpty()) {
                        QRCodeImage(
                            text = qrToken,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BiankyDeepBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Countdown progress bar
                Column(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "يتجدد الرمز خلال:",
                            fontSize = 12.sp,
                            color = BiankyGray
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "مؤقت",
                                tint = if (secondsRemaining <= 3) BiankyError else BiankyRoyalBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$secondsRemaining ثوانٍ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (secondsRemaining <= 3) BiankyError else BiankyRoyalBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        color = if (secondsRemaining <= 3) BiankyError else BiankyRoyalBlue,
                        trackColor = Color(0xFFE2E8F0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // User Instructions footer
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "تعليمات",
                        tint = BiankyDeepBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "توجيهات: أظهر هذا الرمز لمشرف الأمن لمسحه ضوئياً عند الدخول أو الخروج من موقع العمل. يتغير الرمز تلقائياً لمنع محاولات التلاعب أو تسجيل الحضور عن بعد.",
                        fontSize = 12.sp,
                        color = BiankyTextDark,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Right
                    )
                }
            }
        }
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            confirmButton = {
                Button(
                    onClick = { showProfileDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BiankyDeepBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إغلاق", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { launcher.launch("image/*") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BiankyDeepBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تغيير الصورة", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = "الملف الشخصي",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiankyDeepBlue,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large Image
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(Color(user.avatarColor))
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.avatarUri != null && File(user.avatarUri).exists()) {
                            AsyncImage(
                                model = File(user.avatarUri),
                                contentDescription = "الصورة الشخصية",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = user.name.take(1),
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = user.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiankyDeepBlue,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "اسم المستخدم: @${user.username}",
                        fontSize = 14.sp,
                        color = BiankyGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "شفت العمل: ${user.shift}",
                        fontSize = 14.sp,
                        color = BiankyGray,
                        textAlign = TextAlign.Center
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}
