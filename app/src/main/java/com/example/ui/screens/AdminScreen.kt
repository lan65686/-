package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanRecord
import com.example.data.model.User
import com.example.data.sync.SyncStatus
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.draw.scale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    currentUser: User,
    allUsers: List<User>,
    allScanRecords: List<ScanRecord>,
    onAddUser: (User) -> Unit,
    onDeleteUser: (String) -> Unit,
    onClearLogs: () -> Unit,
    onLogoutClick: () -> Unit,
    onAvatarUpdate: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    syncState: SyncStatus = SyncStatus.IDLE,
    pendingCount: Int = 0,
    onToggleOnline: () -> Unit = {}
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val profileFile = File(context.filesDir, "profile_${currentUser.username}.jpg")
                    val outputStream = FileOutputStream(profileFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    onAvatarUpdate(currentUser.username, profileFile.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    var activeTab by remember { mutableStateOf("LIVE_FEED") } // "LIVE_FEED", "MANAGE_USERS", "STATS"
    var showProfileDialog by remember { mutableStateOf(false) }

    // Add user dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEmployeeForLogs by remember { mutableStateOf<User?>(null) }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("EMPLOYEE") } // "EMPLOYEE", "SUPERVISOR", "ADMIN"
    var newShift by remember { mutableStateOf("صباحي") } // "صباحي", "مسائي", "كل الشفتات"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "لوحة التحكم الإدارية",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    // Empty actions - logout button moved below profile card
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BiankyDeepBlue
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "LIVE_FEED",
                    onClick = { activeTab = "LIVE_FEED" },
                    icon = { Icon(Icons.Default.Feed, contentDescription = "المراقبة") },
                    label = { Text("مراقبة حية", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BiankyDeepBlue,
                        selectedTextColor = BiankyDeepBlue,
                        indicatorColor = BiankyLightBlue
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "MANAGE_USERS",
                    onClick = { activeTab = "MANAGE_USERS" },
                    icon = { Icon(Icons.Default.People, contentDescription = "الحسابات") },
                    label = { Text("الموظفين", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BiankyDeepBlue,
                        selectedTextColor = BiankyDeepBlue,
                        indicatorColor = BiankyLightBlue
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "STATS",
                    onClick = { activeTab = "STATS" },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "الإحصائيات") },
                    label = { Text("التقارير", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BiankyDeepBlue,
                        selectedTextColor = BiankyDeepBlue,
                        indicatorColor = BiankyLightBlue
                    )
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "LIVE_FEED" -> {
                    // LIVE MONITORING FEED
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Admin Profile Card
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
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(currentUser.avatarColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentUser.avatarUri != null && File(currentUser.avatarUri).exists()) {
                                        AsyncImage(
                                            model = File(currentUser.avatarUri),
                                            contentDescription = "الصورة الشخصية",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = currentUser.name.take(1),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    
                                    // Small Edit overlay indicator
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل الصورة",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .padding(bottom = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = currentUser.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BiankyDeepBlue
                                    )
                                    Text(
                                        text = "حساب الإداري: @${currentUser.username}",
                                        fontSize = 12.sp,
                                        color = BiankyGray
                                    )
                                }

                                // Role Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BiankyDeepBlue)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                  ) {
                                    Text(
                                        text = "المدير العام",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Admin Logout Button
                        OutlinedButton(
                            onClick = onLogoutClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BiankyError),
                            border = BorderStroke(1.dp, BiankyError),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ExitToApp,
                                contentDescription = "تسجيل الخروج",
                                tint = BiankyError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل الخروج", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BiankyError)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "متابعة الحضور والانصراف (مباشر)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiankyDeepBlue
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Export Button
                                TextButton(
                                    onClick = {
                                        if (allScanRecords.isNotEmpty()) {
                                            val reportBuilder = StringBuilder()
                                            reportBuilder.append("تقرير حضور وانصراف بيانكي\n")
                                            reportBuilder.append("الاسم\tالعملية\tالتوقيت\n")
                                            allScanRecords.forEach {
                                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it.timestamp))
                                                val type = if (it.type == "CHECK_IN") "حضور" else "انصراف"
                                                reportBuilder.append("${it.employeeName}\t$type\t$date\n")
                                            }

                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "تقرير حضور بيانكي")
                                                putExtra(Intent.EXTRA_TEXT, reportBuilder.toString())
                                            }
                                            context.startActivity(Intent.createChooser(intent, "تصدير التقرير عبر:"))
                                        }
                                    },
                                    enabled = allScanRecords.isNotEmpty(),
                                    colors = ButtonDefaults.textButtonColors(contentColor = BiankyRoyalBlue)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تصدير التقرير", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Clear logs Button
                                TextButton(
                                    onClick = onClearLogs,
                                    colors = ButtonDefaults.textButtonColors(contentColor = BiankyError)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مسح السجل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Offline sync status banner for admin
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOnline) BiankyLightBlue else Color(0xFFFFF7ED)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                        contentDescription = "Connection Status",
                                        tint = if (isOnline) BiankyRoyalBlue else BiankyWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isOnline) "حالة الاتصال: سحابي" else "حالة الاتصال: أوفلاين (محلي)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BiankyDeepBlue
                                        )
                                        Text(
                                            text = when (syncState) {
                                                SyncStatus.SYNCING -> "جاري رفع $pendingCount حركات معلقة إلى Firebase..."
                                                SyncStatus.SUCCESS -> "كافة البيانات مزامنة بالكامل سحابياً"
                                                SyncStatus.ERROR -> "خطأ مؤقت أثناء الاتصال بـ Firebase"
                                                SyncStatus.IDLE -> if (pendingCount > 0) "يوجد $pendingCount حركات حضور مخزنة محلياً بانتظار الاتصال" else "النظام مستقر ومزامن بالكامل"
                                            },
                                            fontSize = 11.sp,
                                            color = if (isOnline && pendingCount == 0) BiankySuccess else BiankyWarning
                                        )
                                    }
                                }

                                // Quick offline simulation toggle for admin
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "محاكاة أوفلاين",
                                        fontSize = 10.sp,
                                        color = BiankyGray,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Switch(
                                        checked = !isOnline,
                                        onCheckedChange = { onToggleOnline() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = BiankyWarning,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = BiankyDeepBlue
                                        ),
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }
                            }
                        }

                        if (allScanRecords.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = "لا توجد بيانات",
                                        tint = BiankyGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "لا توجد حركات حضور مسجلة اليوم.",
                                        fontSize = 13.sp,
                                        color = BiankyGray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(allScanRecords) { record ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(if (record.type == "CHECK_IN") Color(0xFFD1FAE5) else Color(0xFFDBEAFE)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (record.type == "CHECK_IN") Icons.Default.Login else Icons.Default.Logout,
                                                        contentDescription = "عملية",
                                                        tint = if (record.type == "CHECK_IN") BiankySuccess else BiankyRoyalBlue,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(record.employeeName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BiankyTextDark)
                                                    val df = SimpleDateFormat("hh:mm a - dd/MM/yyyy", Locale.getDefault())
                                                    Text(
                                                        df.format(Date(record.timestamp)),
                                                        fontSize = 11.sp,
                                                        color = BiankyGray
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (record.type == "CHECK_IN") Color(0xFFECFDF5) else Color(0xFFEFF6FF))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (record.type == "CHECK_IN") "حضور" else "انصراف",
                                                        color = if (record.type == "CHECK_IN") BiankySuccess else BiankyRoyalBlue,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (record.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                                        contentDescription = "سحابة",
                                                        tint = if (record.isSynced) BiankySuccess else BiankyWarning,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (record.isSynced) "مرفوع" else "محلي",
                                                        fontSize = 9.sp,
                                                        color = if (record.isSynced) BiankySuccess else BiankyWarning
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "MANAGE_USERS" -> {
                    // MANAGE ACCOUNTS & STAFF
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "دليل الموظفين والمشرفين",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiankyDeepBlue
                            )

                            Button(
                                onClick = {
                                    newUsername = ""
                                    newPassword = ""
                                    newName = ""
                                    newRole = "EMPLOYEE"
                                    newShift = "صباحي"
                                    showAddDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BiankyDeepBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "إضافة")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة موظف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allUsers) { u ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                        .clickable { selectedEmployeeForLogs = u }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Colored Avatar
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(u.avatarColor)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = u.name.take(1),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(u.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BiankyTextDark)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    val roleAr = when (u.role) {
                                                        "EMPLOYEE" -> "حارس أمن"
                                                        "SUPERVISOR" -> "مشرف أمن"
                                                        else -> "مدير النظام"
                                                    }
                                                    Text(
                                                        text = "$roleAr | شفت: ${u.shift}",
                                                        fontSize = 11.sp,
                                                        color = BiankyGray
                                                    )
                                                }
                                            }
                                        }

                                        // Actions (Delete except default admin)
                                        if (u.username != "admin") {
                                            IconButton(onClick = { onDeleteUser(u.username) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف",
                                                    tint = BiankyError
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "STATS" -> {
                    // STATISTICS & ANALYTICS REPORT
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "تقرير الكفاءة وتوزيع الشفتات",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiankyDeepBlue,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 3 Quick Metrics Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BiankyLightBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("الموظفين المسجلين", fontSize = 11.sp, color = BiankyGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${allUsers.filter { it.role == "EMPLOYEE" }.size}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BiankyDeepBlue
                                    )
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("عمليات الحضور اليوم", fontSize = 11.sp, color = BiankyGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${allScanRecords.filter { it.type == "CHECK_IN" }.size}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BiankySuccess
                                    )
                                }
                            }
                        }

                        // Detailed Stats Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("إحصائيات تفصيلية:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BiankyDeepBlue)
                                Spacer(modifier = Modifier.height(12.dp))

                                val morningCount = allUsers.filter { it.shift == "صباحي" }.size
                                val eveningCount = allUsers.filter { it.shift == "مسائي" }.size

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("عدد أفراد الشفت الصباحي:", fontSize = 12.sp, color = BiankyTextDark)
                                    Text("$morningCount أفراد", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BiankyDeepBlue)
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("عدد أفراد الشفت المسائي:", fontSize = 12.sp, color = BiankyTextDark)
                                    Text("$eveningCount أفراد", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BiankyDeepBlue)
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

                                val pendingSyncCount = allScanRecords.filter { !it.isSynced }.size
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("عمليات معلقة بالهاتف (Offline):", fontSize = 12.sp, color = BiankyTextDark)
                                    Text("$pendingSyncCount حركة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (pendingSyncCount > 0) BiankyWarning else BiankySuccess)
                                }
                            }
                        }

                        // App Rules summary
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BiankyLightBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = "حماية", tint = BiankyDeepBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "حماية الحضور: النظام يفحص الأكواد بالملي ثانية للتأكد من عدم تلاعب الموظف بالوقت بالهاتف، ويعتمد كلياً على التوقيت الزمني الصادر عن السيرفر والتحقق الجغرافي النشط.",
                                    fontSize = 12.sp,
                                    color = BiankyDeepBlue,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }
            }

            // ADD USER DIALOG MODAL
            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newUsername.isNotEmpty() && newName.isNotEmpty() && newPassword.isNotEmpty()) {
                                    val words = newName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                                    if (words.size < 4) {
                                        android.widget.Toast.makeText(context, "الرجاء إدخال الاسم رباعي (4 أسماء على الأقل)", android.widget.Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    if (newPassword.length < 8) {
                                        android.widget.Toast.makeText(context, "يجب أن تكون كلمة المرور 8 أحرف أو أكثر", android.widget.Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    val randomColor = listOf(0xFF3B82F6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFF8B5CF6).random().toInt()
                                    onAddUser(
                                        User(
                                            username = newUsername,
                                            password = newPassword,
                                            name = newName,
                                            role = newRole,
                                            shift = if (newRole == "EMPLOYEE") newShift else "كل الشفتات",
                                            avatarColor = randomColor
                                        )
                                    )
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BiankyDeepBlue)
                        ) {
                            Text("إضافة موظف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("إلغاء", fontSize = 12.sp, color = BiankyGray)
                        }
                    },
                    title = {
                        Text(
                            text = "تسجيل موظف جديد بالنظام",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiankyDeepBlue,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("الاسم الكامل / Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = newUsername,
                                onValueChange = { newUsername = it },
                                label = { Text("اسم المستخدم / Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("كلمة المرور / Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Role Selection Radio Buttons
                            Text("الصلاحية والدور:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BiankyDeepBlue)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = newRole == "EMPLOYEE", onClick = { newRole = "EMPLOYEE" })
                                    Text("حارس", fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = newRole == "SUPERVISOR", onClick = { newRole = "SUPERVISOR" })
                                    Text("مشرف", fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = newRole == "ADMIN", onClick = { newRole = "ADMIN" })
                                    Text("مدير", fontSize = 11.sp)
                                }
                            }

                            // Shift selection if Employee
                            if (newRole == "EMPLOYEE") {
                                Text("شفت العمل المخصص:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BiankyDeepBlue)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = newShift == "صباحي", onClick = { newShift = "صباحي" })
                                        Text("صباحي", fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = newShift == "مسائي", onClick = { newShift = "مسائي" })
                                        Text("مسائي", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            if (selectedEmployeeForLogs != null) {
                val emp = selectedEmployeeForLogs!!
                val empRecords = allScanRecords.filter { it.employeeId == emp.username }
                
                AlertDialog(
                    onDismissRequest = { selectedEmployeeForLogs = null },
                    confirmButton = {
                        TextButton(onClick = { selectedEmployeeForLogs = null }) {
                            Text("إغلاق", color = BiankyDeepBlue, fontWeight = FontWeight.Bold)
                        }
                    },
                    title = {
                        Text(
                            text = "سجل حضور وانصراف: ${emp.name}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiankyDeepBlue,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            Text(
                                text = "عدد السجلات الكلي: ${empRecords.size}",
                                fontSize = 12.sp,
                                color = BiankyGray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            if (empRecords.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "لا توجد عمليات حضور أو انصراف مسجلة لهذا الحارس.",
                                        fontSize = 13.sp,
                                        color = BiankyGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(empRecords) { rec ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    val timeSdf = SimpleDateFormat("hh:mm:ss a - yyyy/MM/dd", Locale.getDefault())
                                                    Text(
                                                        text = if (rec.type == "CHECK_IN") "📥 حضور" else "📤 انصراف",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (rec.type == "CHECK_IN") BiankySuccess else BiankyRoyalBlue
                                                    )
                                                    Text(
                                                        text = timeSdf.format(Date(rec.timestamp)),
                                                        fontSize = 11.sp,
                                                        color = BiankyTextDark,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    )
                                                }
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (rec.isSynced) Color(0xFFECFDF5) else Color(0xFFFEF3C7))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (rec.isSynced) "مرفوع" else "محلي",
                                                        fontSize = 9.sp,
                                                        color = if (rec.isSynced) BiankySuccess else BiankyWarning,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
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
                    text = "الملف الشخصي للمدير",
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
                            .background(Color(currentUser.avatarColor))
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentUser.avatarUri != null && File(currentUser.avatarUri).exists()) {
                            AsyncImage(
                                model = File(currentUser.avatarUri),
                                contentDescription = "الصورة الشخصية",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = currentUser.name.take(1),
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentUser.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiankyDeepBlue,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "اسم المستخدم: @${currentUser.username}",
                        fontSize = 14.sp,
                        color = BiankyGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الدور: المدير العام",
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
