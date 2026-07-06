package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.EmployeeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SupervisorScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val mainViewModel: MainViewModel = viewModel()
                
                val currentUser by mainViewModel.currentUser.collectAsState()
                val profileSyncStatus by mainViewModel.profileSyncStatus.collectAsState()
                
                LaunchedEffect(profileSyncStatus) {
                    profileSyncStatus?.let { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                        mainViewModel.clearProfileSyncStatus()
                    }
                }
                val loginError by mainViewModel.loginError.collectAsState()
                val isOnline by mainViewModel.isOnline.collectAsState()
                val supervisorCheckedIn by mainViewModel.supervisorCheckedIn.collectAsState()
                val supervisorLat by mainViewModel.supervisorLat.collectAsState()
                val supervisorLon by mainViewModel.supervisorLon.collectAsState()
                val qrToken by mainViewModel.qrToken.collectAsState()
                val secondsRemaining by mainViewModel.secondsRemaining.collectAsState()
                val showEmployeeQr by mainViewModel.showEmployeeQr.collectAsState()
                
                val showScanPopup by mainViewModel.showScanPopup.collectAsState()
                val scannedEmployee by mainViewModel.scannedEmployee.collectAsState()
                val scanResultType by mainViewModel.scanResultType.collectAsState()
                val scanMessage by mainViewModel.scanMessage.collectAsState()
                
                val allUsers by mainViewModel.allUsers.collectAsState()
                val allScanRecords by mainViewModel.scanRecords.collectAsState()
                val syncState by mainViewModel.syncState.collectAsState()
                val pendingCount by mainViewModel.pendingCount.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val user = currentUser
                    if (user == null) {
                        LoginScreen(
                            onLoginClick = { username, password ->
                                mainViewModel.login(username, password)
                            },
                            onRegisterClick = { username, password, name, role, shift ->
                                mainViewModel.register(username, password, name, role, shift)
                            },
                            loginError = loginError
                        )
                    } else {
                        when (user.role) {
                            "EMPLOYEE" -> {
                                EmployeeScreen(
                                    user = user,
                                    qrToken = qrToken,
                                    secondsRemaining = secondsRemaining,
                                    showEmployeeQr = showEmployeeQr,
                                    onGenerateClick = { mainViewModel.generateEmployeeQr() },
                                    onLogoutClick = { mainViewModel.logout() },
                                    onAvatarUpdate = { username, path -> mainViewModel.updateUserAvatar(username, path) }
                                )
                            }
                            "SUPERVISOR" -> {
                                SupervisorScreen(
                                    user = user,
                                    supervisorCheckedIn = supervisorCheckedIn,
                                    isOnline = isOnline,
                                    supervisorLat = supervisorLat,
                                    supervisorLon = supervisorLon,
                                    showScanPopup = showScanPopup,
                                    scannedEmployee = scannedEmployee,
                                    scanResultType = scanResultType,
                                    scanMessage = scanMessage,
                                    allScanRecords = allScanRecords,
                                    allUsers = allUsers,
                                    onLogoutClick = { mainViewModel.logout() },
                                    onCheckInClick = { isFixed, lat, lon ->
                                        mainViewModel.supervisorCheckIn(isFixed, lat, lon)
                                        Toast.makeText(
                                            this@MainActivity,
                                            if (isFixed) "تم تسجيل حضور المشرف بنجاح (الكود الثابت)" else "تم تسجيل حضور المشرف بنجاح (الموقع الجغرافي)",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    onToggleOnline = { mainViewModel.toggleOnlineState() },
                                    onForceSync = { 
                                        mainViewModel.triggerSync()
                                        Toast.makeText(this@MainActivity, "تم بدء مزامنة البيانات السحابية", Toast.LENGTH_SHORT).show()
                                    },
                                    onSimulateScan = { targetUser ->
                                        // Helper to generate a valid/live dynamic QR code for the simulated guard
                                        val targetEmployee = allUsers.firstOrNull { it.username == targetUser }
                                        if (targetEmployee != null) {
                                            val timestamp = System.currentTimeMillis()
                                            val simulatedQR = "BIANKY_QR_v1|${targetEmployee.username}|${targetEmployee.name}|${targetEmployee.shift}|$timestamp"
                                            mainViewModel.processScannedCode(simulatedQR)
                                        } else {
                                            Toast.makeText(this@MainActivity, "خطأ: الموظف غير موجود", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onConfirmScan = { scanType ->
                                        mainViewModel.confirmScanRecord(scanType)
                                        Toast.makeText(this@MainActivity, "تم تسجيل الحركة وحفظها بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    onDismissPopup = { mainViewModel.dismissPopup() },
                                    onAvatarUpdate = { username, path -> mainViewModel.updateUserAvatar(username, path) },
                                    syncState = syncState,
                                    pendingCount = pendingCount
                                )
                            }
                            "ADMIN" -> {
                                AdminScreen(
                                    currentUser = user,
                                    allUsers = allUsers,
                                    allScanRecords = allScanRecords,
                                    onAddUser = { newUser ->
                                        mainViewModel.registerUser(newUser)
                                        Toast.makeText(this@MainActivity, "تم تسجيل الحساب الجديد بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    onDeleteUser = { usernameToDelete ->
                                        mainViewModel.deleteUser(usernameToDelete)
                                        Toast.makeText(this@MainActivity, "تم حذف الموظف بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    onClearLogs = {
                                        mainViewModel.clearScanRecords()
                                        Toast.makeText(this@MainActivity, "تم تصفير سجلات الحضور والانصراف", Toast.LENGTH_SHORT).show()
                                    },
                                    onLogoutClick = { mainViewModel.logout() },
                                    onAvatarUpdate = { username, path -> mainViewModel.updateUserAvatar(username, path) },
                                    isOnline = isOnline,
                                    syncState = syncState,
                                    pendingCount = pendingCount,
                                    onToggleOnline = { mainViewModel.toggleOnlineState() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
