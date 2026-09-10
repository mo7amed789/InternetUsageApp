package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppBlockRuleEntity
import com.example.data.local.AppDatabase
import com.example.data.local.DataPlanEntity
import com.example.data.model.AppUsageInfo
import com.example.data.model.PeriodUsageSummary
import com.example.data.model.RealtimeSpeed
import com.example.data.model.UsagePeriod
import com.example.data.repository.DataUsageRepository
import com.example.service.FloatingBubbleService
import com.example.service.LocalDataSaverVpnService
import com.example.service.NetworkSpeedService
import com.example.util.Localization
import com.example.util.NetworkUtils
import com.example.util.SpeedTestEngine
import com.example.util.SpeedTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = DataUsageRepository(
        context = application,
        usageDao = db.usageDao(),
        appBlockDao = db.appBlockDao(),
        dataPlanDao = db.dataPlanDao()
    )
    private val speedTestEngine = SpeedTestEngine()

    // Real-time speed from Foreground Service
    val currentSpeed: StateFlow<RealtimeSpeed> = NetworkSpeedService.currentSpeed

    // Plan & Preferences from Room
    val dataPlan: StateFlow<DataPlanEntity> = repository.dataPlanFlow
        .map { it ?: DataPlanEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DataPlanEntity()
        )

    // Current period selection for historical graphs
    private val _selectedPeriod = MutableStateFlow(UsagePeriod.TODAY)
    val selectedPeriod: StateFlow<UsagePeriod> = _selectedPeriod.asStateFlow()

    // Period summary
    private val _periodSummary = MutableStateFlow(PeriodUsageSummary(UsagePeriod.TODAY))
    val periodSummary: StateFlow<PeriodUsageSummary> = _periodSummary.asStateFlow()

    // Apps usage list
    private val _appsUsage = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val appsUsage: StateFlow<List<AppUsageInfo>> = _appsUsage.asStateFlow()

    // Apps loading state
    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    // Speed test state
    private val _speedTestState = MutableStateFlow(SpeedTestResult())
    val speedTestState: StateFlow<SpeedTestResult> = _speedTestState.asStateFlow()

    // Status / Message feedback
    private val _userFeedback = MutableStateFlow<String?>(null)
    val userFeedback: StateFlow<String?> = _userFeedback.asStateFlow()

    val isArabic: Boolean
        get() = dataPlan.value.isArabicLanguage

    val lang: Localization.Lang
        get() = if (dataPlan.value.isArabicLanguage) Localization.Lang.AR else Localization.Lang.EN

    init {
        viewModelScope.launch {
            repository.getOrCreateDataPlan()
            loadPeriodSummary(_selectedPeriod.value)
            loadAppsUsage(_selectedPeriod.value)
            // Periodic snapshot
            repository.recordSnapshot()
        }
    }

    fun setPeriod(period: UsagePeriod) {
        _selectedPeriod.value = period
        viewModelScope.launch {
            loadPeriodSummary(period)
            loadAppsUsage(period)
        }
    }

    fun loadPeriodSummary(period: UsagePeriod) {
        viewModelScope.launch {
            val summary = repository.getUsageForPeriod(period)
            _periodSummary.value = summary
        }
    }

    fun loadAppsUsage(period: UsagePeriod = _selectedPeriod.value) {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val list = repository.getPerAppUsage(period)
            _appsUsage.value = list
            _isLoadingApps.value = false
        }
    }

    fun toggleLiveNotification(enabled: Boolean) {
        viewModelScope.launch {
            val updated = dataPlan.value.copy(isLiveNotificationEnabled = enabled)
            repository.updateDataPlan(updated)

            val context = getApplication<Application>()
            val intent = Intent(context, NetworkSpeedService::class.java)
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.stopService(intent)
            }
        }
    }

    fun toggleFloatingBubble(enabled: Boolean) {
        viewModelScope.launch {
            val updated = dataPlan.value.copy(isFloatingBubbleEnabled = enabled)
            repository.updateDataPlan(updated)

            val context = getApplication<Application>()
            val intent = Intent(context, FloatingBubbleService::class.java)
            if (enabled) {
                if (NetworkUtils.canDrawOverlays(context)) {
                    context.startService(intent)
                }
            } else {
                context.stopService(intent)
            }
        }
    }

    fun toggleDataSaver(active: Boolean) {
        viewModelScope.launch {
            val updated = dataPlan.value.copy(isVpnDataSaverActive = active)
            repository.updateDataPlan(updated)

            val context = getApplication<Application>()
            val intent = Intent(context, LocalDataSaverVpnService::class.java).apply {
                action = if (active) LocalDataSaverVpnService.ACTION_START else LocalDataSaverVpnService.ACTION_STOP
            }
            if (active) {
                context.startService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun toggleAppBlockMobile(packageName: String, appName: String, currentBlocked: Boolean) {
        viewModelScope.launch {
            val newBlocked = !currentBlocked
            repository.upsertRule(
                AppBlockRuleEntity(
                    packageName = packageName,
                    appName = appName,
                    isBlockedMobile = newBlocked
                )
            )
            // Refresh list
            loadAppsUsage(_selectedPeriod.value)
        }
    }

    fun toggleAppBlockWifi(packageName: String, appName: String, currentBlocked: Boolean) {
        viewModelScope.launch {
            val newBlocked = !currentBlocked
            repository.upsertRule(
                AppBlockRuleEntity(
                    packageName = packageName,
                    appName = appName,
                    isBlockedWifi = newBlocked
                )
            )
            loadAppsUsage(_selectedPeriod.value)
        }
    }

    fun toggleAppWhitelist(packageName: String, appName: String, currentWhitelisted: Boolean) {
        viewModelScope.launch {
            val newWhitelisted = !currentWhitelisted
            repository.upsertRule(
                AppBlockRuleEntity(
                    packageName = packageName,
                    appName = appName,
                    isWhitelisted = newWhitelisted
                )
            )
            loadAppsUsage(_selectedPeriod.value)
        }
    }

    fun updatePlanSettings(
        monthlyLimitGb: Long,
        renewalDay: Int,
        warningPercent: Int,
        unlimitedWifi: Boolean,
        autoSaverOnMobile: Boolean
    ) {
        viewModelScope.launch {
            val current = dataPlan.value
            val updated = current.copy(
                monthlyLimitMb = monthlyLimitGb * 1024L,
                billingCycleDay = renewalDay.coerceIn(1, 31),
                warningThresholdPercent = warningPercent.coerceIn(50, 99),
                unlimitedWifi = unlimitedWifi,
                isAutoSaverOnLimit = autoSaverOnMobile
            )
            repository.updateDataPlan(updated)
            _userFeedback.value = "Settings saved successfully"
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val current = dataPlan.value
            val updated = current.copy(isArabicLanguage = !current.isArabicLanguage)
            repository.updateDataPlan(updated)
        }
    }

    fun runSpeedTest() {
        if (_speedTestState.value.isRunning) return
        viewModelScope.launch {
            speedTestEngine.runTest { progress ->
                _speedTestState.value = progress
            }
        }
    }

    fun exportUsageReport(onCompleted: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                val file = File(exportDir, "DataPulse_Report_${System.currentTimeMillis()}.csv")
                val writer = FileWriter(file)

                writer.append("Package Name,App Name,WiFi Bytes,Mobile Bytes,Total Bytes,Foreground Bytes,Background Bytes\n")
                for (app in _appsUsage.value) {
                    writer.append("\"${app.packageName}\",")
                    writer.append("\"${app.appName}\",")
                    writer.append("${app.wifiBytes},")
                    writer.append("${app.mobileBytes},")
                    writer.append("${app.totalBytes},")
                    writer.append("${app.foregroundBytes},")
                    writer.append("${app.backgroundBytes}\n")
                }
                writer.flush()
                writer.close()
                withContext(Dispatchers.Main) {
                    onCompleted(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onCompleted(null)
                }
            }
        }
    }

    fun clearFeedback() {
        _userFeedback.value = null
    }
}
