package com.example.ui
 
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ScanRecord
import com.example.data.model.User
import com.example.data.repository.AttendanceRepository
import com.example.data.sync.OfflineSyncManager
import com.example.data.sync.SyncStatus
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AttendanceRepository(database.userDao(), database.scanRecordDao())
    val syncManager = OfflineSyncManager.getInstance(application)

    private val prefs = application.getSharedPreferences("BIANKY_PREFS", android.content.Context.MODE_PRIVATE)

    // Sync Manager states exposed to UI
    val syncState = syncManager.syncState
    val pendingCount = syncManager.pendingCount
    val lastSyncTime = syncManager.lastSyncTime

    // UI States
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _profileSyncStatus = MutableStateFlow<String?>(null)
    val profileSyncStatus: StateFlow<String?> = _profileSyncStatus.asStateFlow()

    fun clearProfileSyncStatus() {
        _profileSyncStatus.value = null
    }

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _supervisorCheckedIn = MutableStateFlow(true)
    val supervisorCheckedIn: StateFlow<Boolean> = _supervisorCheckedIn.asStateFlow()

    private val _showEmployeeQr = MutableStateFlow(false)
    val showEmployeeQr: StateFlow<Boolean> = _showEmployeeQr.asStateFlow()

    // Geofencing constants
    val workSiteLatitude = 30.0444
    val workSiteLongitude = 31.2357
    val allowedRadiusMeters = 200.0 // 200m range

    // Supervisor GPS Simulation coordinates
    private val _supervisorLat = MutableStateFlow(30.0444)
    val supervisorLat: StateFlow<Double> = _supervisorLat.asStateFlow()

    private val _supervisorLon = MutableStateFlow(31.2357)
    val supervisorLon: StateFlow<Double> = _supervisorLon.asStateFlow()

    // QR dynamic countdown (10 seconds)
    private val _qrToken = MutableStateFlow("")
    val qrToken: StateFlow<String> = _qrToken.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(10)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    // Scanned employee popup
    private val _scannedEmployee = MutableStateFlow<User?>(null)
    val scannedEmployee: StateFlow<User?> = _scannedEmployee.asStateFlow()

    private val _scanResultType = MutableStateFlow<String>("") // "CHECK_IN" or "CHECK_OUT"
    val scanResultType: StateFlow<String> = _scanResultType.asStateFlow()

    private val _showScanPopup = MutableStateFlow(false)
    val showScanPopup: StateFlow<Boolean> = _showScanPopup.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    // Data lists from repository
    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanRecords: StateFlow<List<ScanRecord>> = repository.allScanRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var qrJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedDefaultUsersIfEmpty()
            syncManager.updatePendingCount()
            // Auto-login from saved session
            val savedUsername = prefs.getString("SAVED_USER", null)
            if (savedUsername != null) {
                val user = database.userDao().getUserByUsername(savedUsername)
                if (user != null) {
                    withContext(Dispatchers.Main) {
                        _currentUser.value = user
                        _showEmployeeQr.value = false
                    }
                    // Auto-sync current user on startup to restore any deleted/missing fields in Firestore
                    syncUserToFirebase(user)
                }
            }
        }
    }

    // Login existing user
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginError.value = null
            if (username.isBlank() || password.isBlank()) {
                _loginError.value = "من فضلك أدخل اسم المستخدم وكلمة المرور"
                return@launch
            }

            // Special handling for Director/Admin (العميد)
            if (username == "العميد" && password == "Zxcvbnm/1234") {
                val existingAdmin = database.userDao().getUserByUsername("العميد")
                val adminUser = if (existingAdmin == null || existingAdmin.password != "Zxcvbnm/1234" || existingAdmin.role != "ADMIN") {
                    val newAdmin = User(
                        username = "العميد",
                        password = "Zxcvbnm/1234",
                        name = "العميد",
                        role = "ADMIN",
                        shift = "كل الشفتات",
                        avatarColor = 0xFF8B5CF6.toInt()
                    )
                    repository.insertUser(newAdmin)
                    newAdmin
                } else {
                    existingAdmin
                }
                _currentUser.value = adminUser
                _loginError.value = null
                prefs.edit().putString("SAVED_USER", "العميد").apply()
                _showEmployeeQr.value = false
                stopQRGenerator()
                syncUserToFirebase(adminUser)
                return@launch
            }
            
            // Check if user already exists
            val existingUser = database.userDao().getUserByUsername(username)
            if (existingUser != null) {
                if (existingUser.password == password) {
                    _currentUser.value = existingUser
                    _loginError.value = null
                    prefs.edit().putString("SAVED_USER", username).apply()
                    _showEmployeeQr.value = false
                    stopQRGenerator()
                    // Sync logged in user to Firebase as well
                    syncUserToFirebase(existingUser)
                } else {
                    _loginError.value = "كلمة المرور خاطئة لهذا الحساب المسجل"
                }
            } else {
                _loginError.value = "اسم المستخدم هذا غير مسجل، يرجى إنشاء حساب جديد أولاً"
            }
        }
    }

    // Register new user with full details and profile creation
    fun register(username: String, password: String, name: String, role: String, shift: String) {
        viewModelScope.launch {
            _loginError.value = null
            if (username.isBlank() || password.isBlank() || name.isBlank()) {
                _loginError.value = "من فضلك املأ جميع الحقول المطلوبة"
                return@launch
            }
            
            val existingUser = database.userDao().getUserByUsername(username)
            if (existingUser != null) {
                _loginError.value = "اسم المستخدم هذا مسجل بالفعل، يرجى اختيار اسم آخر"
            } else {
                val randomColor = listOf(0xFF3B82F6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFF8B5CF6).random().toInt()
                val newUser = User(
                    username = username,
                    password = password,
                    name = name,
                    role = role,
                    shift = if (role == "EMPLOYEE") shift else "كل الشفتات",
                    avatarColor = randomColor
                )
                repository.insertUser(newUser)
                _currentUser.value = newUser
                _loginError.value = null
                prefs.edit().putString("SAVED_USER", username).apply()
                _showEmployeeQr.value = false
                stopQRGenerator()
                syncUserToFirebase(newUser)
            }
        }
    }

    // Logout
    fun logout() {
        _currentUser.value = null
        _supervisorCheckedIn.value = true
        _showEmployeeQr.value = false
        prefs.edit().remove("SAVED_USER").apply()
        stopQRGenerator()
    }

    fun generateEmployeeQr() {
        _showEmployeeQr.value = true
        startQRGenerator()
    }

    // Toggle Online/Offline simulation
    fun toggleOnlineState() {
        _isOnline.value = !_isOnline.value
        if (_isOnline.value) {
            triggerSync()
        }
    }

    // Trigger sync
    fun triggerSync() {
        syncManager.triggerBackgroundSync()
    }

    // Start Dynamic QR Generation
    private fun startQRGenerator() {
        qrJob?.cancel()
        qrJob = viewModelScope.launch {
            while (true) {
                val user = _currentUser.value ?: break
                val timestamp = System.currentTimeMillis()
                // Format: BIANKY_QR_v1|username|name|shift|timestamp
                _qrToken.value = "BIANKY_QR_v1|${user.username}|${user.name}|${user.shift}|$timestamp"
                
                // 10 second countdown
                for (i in 10 downTo 1) {
                    _secondsRemaining.value = i
                    delay(1000)
                }
            }
        }
    }

    private fun stopQRGenerator() {
        qrJob?.cancel()
        qrJob = null
    }

    // Supervisor Check-In
    fun supervisorCheckIn(isFixedQR: Boolean, lat: Double, lon: Double) {
        _supervisorLat.value = lat
        _supervisorLon.value = lon
        _supervisorCheckedIn.value = true
    }

    // Update GPS simulation for supervisor
    fun updateSupervisorGPS(lat: Double, lon: Double) {
        _supervisorLat.value = lat
        _supervisorLon.value = lon
    }

    // Register User (Admin Dashboard)
    fun registerUser(user: User) {
        viewModelScope.launch {
            repository.insertUser(user)
        }
    }

    // Update User Avatar URI (Local Room Db + Firebase Storage & Firestore)
    fun updateUserAvatar(username: String, imageUriString: String) {
        viewModelScope.launch {
            // 1. Immediately update locally in Room database for zero lag
            val user = database.userDao().getUserByUsername(username)
            if (user != null) {
                val updatedUser = user.copy(avatarUri = imageUriString)
                database.userDao().updateUser(updatedUser)
                // If this is the current logged-in user, update the live StateFlow
                if (_currentUser.value?.username == username) {
                    _currentUser.value = updatedUser
                }

                // 2. Begin Firebase Upload & Firestore Sync
                _profileSyncStatus.value = "جاري الحفظ المحلي والرفع السحابي..."
                try {
                    // Check if Firebase is initialized first
                    val isFirebaseAvailable = try {
                        FirebaseApp.getInstance()
                        true
                    } catch (e: Exception) {
                        false
                    }

                    if (!isFirebaseAvailable) {
                        _profileSyncStatus.value = "تم الحفظ محلياً بنجاح! لتفعيل الرفع السحابي، يرجى إضافة ملف google-services.json."
                        return@launch
                    }

                    // Perform real cloud upload to Firebase Storage
                    val storage = FirebaseStorage.getInstance()
                    val firestore = FirebaseFirestore.getInstance()

                    val localFile = File(imageUriString)
                    if (!localFile.exists()) {
                        _profileSyncStatus.value = "خطأ: الملف المحلي غير موجود."
                        return@launch
                    }

                    val fileUri = Uri.fromFile(localFile)
                    val storageRef = storage.reference.child("profile_pictures/$username.jpg")

                    storageRef.putFile(fileUri)
                        .addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                val cloudUrl = downloadUrl.toString()
                                
                                // Create user map for Firestore
                                val firestoreUser = mapOf(
                                    "username" to user.username,
                                    "name" to user.name,
                                    "role" to user.role,
                                    "shift" to user.shift,
                                    "profilePhotoUrl" to cloudUrl,
                                    "avatarColor" to user.avatarColor,
                                    "updatedAt" to System.currentTimeMillis()
                                )

                                // Save/Link photo URL in Firestore
                                firestore.collection("users").document(username)
                                    .set(firestoreUser)
                                    .addOnSuccessListener {
                                        _profileSyncStatus.value = "تم رفع الصورة وربطها سحابياً بنجاح على Firebase!"
                                    }
                                    .addOnFailureListener { err ->
                                        _profileSyncStatus.value = "تم الحفظ محلياً. فشل الحفظ في Firestore: ${err.localizedMessage}"
                                    }
                            }.addOnFailureListener { err ->
                                _profileSyncStatus.value = "تم الحفظ محلياً. فشل الحصول على رابط التحميل: ${err.localizedMessage}"
                            }
                        }
                        .addOnFailureListener { err ->
                            _profileSyncStatus.value = "تم الحفظ محلياً. فشل رفع الملف: ${err.localizedMessage}"
                        }

                } catch (e: Exception) {
                    _profileSyncStatus.value = "تم الحفظ محلياً بنجاح! الرفع السحابي يتطلب ملف google-services.json مفعّل."
                    e.printStackTrace()
                }
            }
        }
    }

    // Delete User (Admin Dashboard)
    fun deleteUser(username: String) {
        viewModelScope.launch {
            repository.deleteUser(username)
        }
    }

    // Clear all scan logs (Admin feature)
    fun clearScanRecords() {
        viewModelScope.launch {
            repository.clearScanRecords()
        }
    }

    // Process a scanned QR code to initiate the check-in/check-out option
    fun processScannedCode(scannedText: String): Boolean {
        val parts = scannedText.split("|")
        if (parts.size < 5 || parts[0] != "BIANKY_QR_v1") {
            _scanMessage.value = "رمز الاستجابة السريعة (QR) غير صالح أو غير معتمد من نظام بيانكي!"
            return false
        }

        val username = parts[1]
        val name = parts[2]
        val shift = parts[3]
        val timestamp = parts[4].toLongOrNull() ?: 0L

        // Anti-Fraud check: QR Code expiry (Allowing up to 10 minutes discrepancy/drift to tolerate differences in device system times)
        val currentTime = System.currentTimeMillis()
        val differenceSeconds = (currentTime - timestamp) / 1000
        if (differenceSeconds > 600 || differenceSeconds < -600) {
            _scanMessage.value = "العملية فشلت: رمز QR منتهي الصلاحية (تم توليده منذ $differenceSeconds ثوانٍ). يرجى مسح رمز جديد أو التأكد من مطابقة وقت وتاريخ الهاتف للوقت الفعلي!"
            return false
        }

        // Set state for visual popup card
        viewModelScope.launch {
            val dbUser = database.userDao().getUserByUsername(username)
            if (dbUser != null) {
                _scannedEmployee.value = dbUser
            } else {
                val mockAvatarColor = when (username) {
                    "emp" -> 0xFF3B82F6.toInt()
                    "emp2" -> 0xFF10B981.toInt()
                    else -> 0xFF8B5CF6.toInt()
                }
                _scannedEmployee.value = User(
                    username = username,
                    password = "",
                    name = name,
                    role = "EMPLOYEE",
                    shift = shift,
                    avatarColor = mockAvatarColor
                )
            }
        }
        _scanMessage.value = null
        _showScanPopup.value = true

        return true
    }

    // Confirm scan record with selected type ("CHECK_IN" or "CHECK_OUT")
    fun confirmScanRecord(scanType: String) {
        val employee = _scannedEmployee.value ?: return
        val currentTime = System.currentTimeMillis()
        val isSynced = _isOnline.value
        val scanRecord = ScanRecord(
            employeeId = employee.username,
            employeeName = employee.name,
            timestamp = currentTime,
            latitude = _supervisorLat.value,
            longitude = _supervisorLon.value,
            isSynced = isSynced,
            type = scanType
        )

        viewModelScope.launch {
            repository.insertScanRecord(scanRecord)
            syncManager.updatePendingCount()
            if (isSynced) {
                syncManager.triggerBackgroundSync()
            }
        }

        dismissPopup()
    }

    fun dismissPopup() {
        _showScanPopup.value = false
        _scannedEmployee.value = null
    }

    // Helper: Haversine formula to compute distance in meters between two points
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth's radius in meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }

    // Helper: Sync registered/logged in user to Firebase Firestore
    fun syncUserToFirebase(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check if Firebase is initialized first
                val isFirebaseAvailable = try {
                    FirebaseApp.getInstance()
                    true
                } catch (e: Exception) {
                    false
                }

                if (!isFirebaseAvailable) return@launch

                val firestore = FirebaseFirestore.getInstance()
                val firestoreUser = mapOf(
                    "username" to user.username,
                    "name" to user.name,
                    "role" to user.role,
                    "shift" to user.shift,
                    "avatarColor" to user.avatarColor,
                    "avatarUri" to (user.avatarUri ?: ""),
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("users").document(user.username)
                    .set(firestoreUser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
