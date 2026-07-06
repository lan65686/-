package com.example.ui.screens

import android.net.Uri
import android.content.Intent
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.ImageAnalysis
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.example.data.model.ScanRecord
import com.example.data.model.User
import com.example.data.sync.SyncStatus
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorScreen(
    user: User,
    supervisorCheckedIn: Boolean,
    isOnline: Boolean,
    supervisorLat: Double,
    supervisorLon: Double,
    showScanPopup: Boolean,
    scannedEmployee: User?,
    scanResultType: String,
    scanMessage: String?,
    allScanRecords: List<ScanRecord>,
    allUsers: List<User>,
    onLogoutClick: () -> Unit,
    onCheckInClick: (isFixedQR: Boolean, lat: Double, lon: Double) -> Unit,
    onToggleOnline: () -> Unit,
    onForceSync: () -> Unit,
    onSimulateScan: (String) -> Unit, // Username
    onConfirmScan: (String) -> Unit, // CHECK_IN or CHECK_OUT
    onRealScan: (String) -> Unit = {},
    onDismissPopup: () -> Unit,
    onAvatarUpdate: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    syncState: SyncStatus = SyncStatus.IDLE,
    pendingCount: Int = 0
) {
    val context = LocalContext.current
    var selectedGpsMode by remember { mutableStateOf("INSIDE") } // "INSIDE" or "OUTSIDE"
    var showProfileDialog by remember { mutableStateOf(false) }

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

    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(supervisorCheckedIn) {
        if (supervisorCheckedIn && !hasCameraPermission) {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "بيانكي - مشرف الأمن",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    // Online/Offline status switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isOnline) "متصل" else "منقطع",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = isOnline,
                            onCheckedChange = { onToggleOnline() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFEF4444)
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BiankyDeepBlue
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            if (!supervisorCheckedIn) {
                // 1. MUST CHECK-IN SCREEN
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BiankyLightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "التحقق الجغرافي",
                            tint = BiankyDeepBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "تسجيل حضور المشرف أولاً",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiankyDeepBlue,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "قبل البدء بمسح أكواد الحراس، يجب إثبات تواجدك الفعلي داخل النطاق الجغرافي لموقع العمل.",
                        fontSize = 13.sp,
                        color = BiankyGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // GPS Simulator Controller
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "محاكي الموقع الجغرافي (GPS Simulation):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiankyDeepBlue,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { selectedGpsMode = "INSIDE" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedGpsMode == "INSIDE") BiankyRoyalBlue else Color(0xFFE2E8F0),
                                        contentColor = if (selectedGpsMode == "INSIDE") Color.White else BiankyTextDark
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("داخل موقع العمل (ناجح)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { selectedGpsMode = "OUTSIDE" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedGpsMode == "OUTSIDE") BiankyError else Color(0xFFE2E8F0),
                                        contentColor = if (selectedGpsMode == "OUTSIDE") Color.White else BiankyTextDark
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("خارج النطاق (فشل)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val lat = if (selectedGpsMode == "INSIDE") 30.0444 else 30.0911
                            val lon = if (selectedGpsMode == "INSIDE") 31.2357 else 31.2899
                            Text(
                                text = "موقعك الحالي: $lat , $lon",
                                fontSize = 12.sp,
                                color = BiankyGray
                            )
                            Text(
                                text = "موقع العمل المطلوب: 30.0444 , 31.2357 (نطاق 200م)",
                                fontSize = 11.sp,
                                color = BiankyGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Option A: Start Shift via GPS
                    Button(
                        onClick = {
                            val lat = if (selectedGpsMode == "INSIDE") 30.0444 else 30.0911
                            val lon = if (selectedGpsMode == "INSIDE") 31.2357 else 31.2899
                            onCheckInClick(false, lat, lon)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BiankyDeepBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_shift_gps")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "بدء")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تسجيل حضور وبدء الشفت بالـ GPS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option B: fixed site QR
                    OutlinedButton(
                        onClick = {
                            // Automatically simulation inside GPS
                            onCheckInClick(true, 30.0444, 31.2357)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BiankyDeepBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_shift_fixed_qr")
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = "QR")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مسح كود موقع العمل الثابت لتسجيل الحضور", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Logout Button
                    OutlinedButton(
                        onClick = onLogoutClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BiankyError),
                        border = BorderStroke(1.dp, BiankyError),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ExitToApp,
                            contentDescription = "تسجيل الخروج",
                            tint = BiankyError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تسجيل الخروج", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BiankyError)
                    }
                }
            } else {
                // 2. ACTIVE SUPERVISOR SCANNING CONSOLE
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header card showing current check-in location info
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BiankyLightBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProfileDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BiankyDeepBlue
                                    )
                                    Text(
                                        text = "مشرف الأمن • @${user.username}",
                                        fontSize = 11.sp,
                                        color = BiankyGray
                                    )
                                }
                                Box(contentAlignment = Alignment.TopEnd) {
                                    IconButton(
                                        onClick = onForceSync,
                                        enabled = syncState != SyncStatus.SYNCING
                                    ) {
                                        if (syncState == SyncStatus.SYNCING) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = BiankyRoyalBlue
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = "Sync",
                                                tint = if (pendingCount > 0) BiankyWarning else BiankyRoyalBlue
                                            )
                                        }
                                    }
                                    if (pendingCount > 0 && syncState != SyncStatus.SYNCING) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = 2.dp, y = (-2).dp)
                                                .size(16.dp)
                                                .background(BiankyWarning, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = pendingCount.toString(),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
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
                    }

                    // Simulated / Real Scanner Preview Area
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "شاشة الكاميرا والمسح الضوئي / Scanner Preview",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiankyDeepBlue,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // CameraX Viewport
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                                    .border(2.dp, BiankyRoyalBlue, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasCameraPermission) {
                                    // Real Camera preview compilation with live QR scanning
                                    CameraPreviewContainer(onQrCodeScanned = { scannedText ->
                                        onRealScan(scannedText)
                                    })

                                    // Scanning overlay frame box
                                    Box(
                                        modifier = Modifier
                                            .size(140.dp)
                                            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                                    )

                                    // Real camera indicator text
                                    Text(
                                        text = "الكاميرا مدمجة ونشطة",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(12.dp)
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "الكاميرا مغلقة",
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "الكاميرا تتطلب إذن التشغيل للتصوير والمسح",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BiankyRoyalBlue),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("تفعيل الكاميرا", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }



                    // Error messages (e.g. out of geofence or expired)
                    if (scanMessage != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "خطأ",
                                        tint = BiankyError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = scanMessage,
                                        fontSize = 11.sp,
                                        color = BiankyError,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Scans logs done on this device (offline storage list)
                    item {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "حركات الحضور والمسح الحالية (${allScanRecords.size}):",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BiankyDeepBlue
                                )
                                 Column(horizontalAlignment = Alignment.End) {
                                     Text(
                                         text = when (syncState) {
                                             SyncStatus.SYNCING -> "⏳ جاري مزامنة $pendingCount حركات..."
                                             SyncStatus.SUCCESS -> "✅ تم الرفع والمزامنة بالكامل"
                                             SyncStatus.ERROR -> "❌ فشلت المزامنة مؤقتاً"
                                             SyncStatus.IDLE -> if (isOnline) {
                                                 if (pendingCount > 0) "⚠️ يوجد $pendingCount حركات معلقة" else "☁️ متصل ومزامن بالكامل"
                                             } else {
                                                 "💾 مخزن محلياً (Offline) - $pendingCount معلقة"
                                             }
                                         },
                                         fontSize = 11.sp,
                                         color = when (syncState) {
                                             SyncStatus.SYNCING -> BiankyRoyalBlue
                                             SyncStatus.SUCCESS -> BiankySuccess
                                             SyncStatus.ERROR -> BiankyError
                                             SyncStatus.IDLE -> if (isOnline && pendingCount == 0) BiankySuccess else BiankyWarning
                                         },
                                         fontWeight = FontWeight.Bold
                                         )
                                     }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Export Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        if (allScanRecords.isNotEmpty()) {
                                            val reportBuilder = StringBuilder()
                                            reportBuilder.append("تقرير حضور وانصراف المشرف بيانكي\n")
                                            reportBuilder.append("الاسم\tالعملية\tالتوقيت\n")
                                            allScanRecords.forEach {
                                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it.timestamp))
                                                val type = if (it.type == "CHECK_IN") "حضور" else "انصراف"
                                                reportBuilder.append("${it.employeeName}\t$type\t$date\n")
                                            }

                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "تقرير حضور بيانكي - مشرف")
                                                putExtra(Intent.EXTRA_TEXT, reportBuilder.toString())
                                            }
                                            context.startActivity(Intent.createChooser(intent, "تصدير التقرير عبر:"))
                                        }
                                    },
                                    enabled = allScanRecords.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BiankyRoyalBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "تصدير البيانات",
                                        tint = if (allScanRecords.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تصدير التقرير الحالي",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (allScanRecords.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا توجد حركات مسجلة حالياً.",
                                    fontSize = 12.sp,
                                    color = BiankyGray
                                )
                            }
                        }
                    } else {
                        items(allScanRecords) { record ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (record.type == "CHECK_IN") BiankySuccess else BiankyRoyalBlue)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(record.employeeName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BiankyTextDark)
                                            val timeFormat = SimpleDateFormat("hh:mm:ss a dd/MM", Locale.getDefault())
                                            Text(
                                                timeFormat.format(Date(record.timestamp)),
                                                fontSize = 11.sp,
                                                color = BiankyGray
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (record.type == "CHECK_IN") Color(0xFFECFDF5) else Color(0xFFEFF6FF)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (record.type == "CHECK_IN") "حضور" else "انصراف",
                                                color = if (record.type == "CHECK_IN") BiankySuccess else BiankyRoyalBlue,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = if (record.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                            contentDescription = "مزامنة",
                                            tint = if (record.isSynced) BiankySuccess else BiankyWarning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. CONFIRM SCAN POPUP OVERLAY
            AnimatedVisibility(
                visible = showScanPopup,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (scannedEmployee != null) {
                    AlertDialog(
                        onDismissRequest = onDismissPopup,
                        confirmButton = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onConfirmScan("CHECK_IN") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BiankySuccess),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تسجيل حضور", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Button(
                                    onClick = { onConfirmScan("CHECK_OUT") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BiankyDeepBlue),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تسجيل انصراف", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = onDismissPopup,
                                border = BorderStroke(1.dp, BiankyGray),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BiankyGray),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("إلغاء", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(BiankyRoyalBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "مسح الكود",
                                    tint = BiankyRoyalBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        },
                        title = {
                            Text(
                                text = "تأكيد تسجيل حركة الموظف",
                                color = BiankyDeepBlue,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Profile Avatar
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(scannedEmployee.avatarColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (scannedEmployee.avatarUri != null && File(scannedEmployee.avatarUri).exists()) {
                                        AsyncImage(
                                            model = File(scannedEmployee.avatarUri),
                                            contentDescription = "الصورة الشخصية",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = scannedEmployee.name.take(1),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = scannedEmployee.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BiankyTextDark
                                )

                                Text(
                                    text = "اسم المستخدم: @${scannedEmployee.username}",
                                    fontSize = 11.sp,
                                    color = BiankyGray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                Text(
                                    text = "شفت: ${scannedEmployee.shift}",
                                    fontSize = 12.sp,
                                    color = BiankyGray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val timeFormat = SimpleDateFormat("hh:mm:ss a - dd/MM/yyyy", Locale.getDefault())
                                Text(
                                    text = "وقت وتاريخ المسح: ${timeFormat.format(Date())}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BiankyTextDark,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (!isOnline) {
                                    Text(
                                        text = "⚠️ سيتم الحفظ محلياً (Offline) والمزامنة فور الاتصال",
                                        fontSize = 11.sp,
                                        color = BiankyWarning,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color.White
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
                    text = "الملف الشخصي للمشرف",
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
                        text = "الدور: مشرف الأمن",
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

// Camera preview viewport integration using CameraX helper with live QR scanning using ZXing
@Composable
fun CameraPreviewContainer(onQrCodeScanned: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastScannedText by remember { mutableStateOf("") }
    var lastScannedTime by remember { mutableStateOf(0L) }

    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                
                imageAnalysis.setAnalyzer(
                    androidx.core.content.ContextCompat.getMainExecutor(previewView.context)
                ) { image ->
                    val buffer = image.planes[0].buffer
                    val data = ByteArray(buffer.remaining())
                    buffer.get(data)
                    
                    val source = PlanarYUVLuminanceSource(
                        data,
                        image.width,
                        image.height,
                        0,
                        0,
                        image.width,
                        image.height,
                        false
                    )
                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                    try {
                        val result = MultiFormatReader().decode(binaryBitmap)
                        val text = result.text
                        val now = System.currentTimeMillis()
                        // 2 seconds de-bounce for the same scanned QR
                        if (text != lastScannedText || now - lastScannedTime > 2000) {
                            lastScannedText = text
                            lastScannedTime = now
                            onQrCodeScanned(text)
                        }
                    } catch (e: Exception) {
                        // Ignore if QR not detected in frame
                    } finally {
                        image.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(previewView.context))
        },
        modifier = Modifier.fillMaxSize()
    )
}
