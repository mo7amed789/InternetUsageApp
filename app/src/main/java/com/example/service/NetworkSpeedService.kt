package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.ConnectionType
import com.example.data.model.RealtimeSpeed
import com.example.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NetworkSpeedService : Service() {

    companion object {
        const val CHANNEL_ID = "datapulse_speed_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.datapulse.STOP_SPEED_SERVICE"
        const val ACTION_TOGGLE_SAVER = "com.example.datapulse.TOGGLE_SAVER"

        private val _currentSpeed = MutableStateFlow(RealtimeSpeed())
        val currentSpeed: StateFlow<RealtimeSpeed> = _currentSpeed.asStateFlow()

        var isRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var speedJob: Job? = null
    private var isScreenOn = true

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTimestamp = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    // Battery optimization: Stop polling when screen is locked/off
                    speedJob?.cancel()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    startSpeedPolling()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)

        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastTimestamp = System.currentTimeMillis()

        startForegroundServiceWithNotification()
        startSpeedPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val initialNotification = buildNotification("↓ 0 KB/s  ↑ 0 KB/s", "DataPulse Monitor Active")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }
    }

    private fun startSpeedPolling() {
        speedJob?.cancel()
        speedJob = serviceScope.launch {
            while (isActive && isScreenOn) {
                val now = System.currentTimeMillis()
                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()

                val timeDiffSec = ((now - lastTimestamp) / 1000.0).coerceAtLeast(0.5)

                val rxSpeed = if (lastRxBytes > 0 && currentRx >= lastRxBytes) {
                    ((currentRx - lastRxBytes) / timeDiffSec).toLong()
                } else 0L

                val txSpeed = if (lastTxBytes > 0 && currentTx >= lastTxBytes) {
                    ((currentTx - lastTxBytes) / timeDiffSec).toLong()
                } else 0L

                lastRxBytes = currentRx
                lastTxBytes = currentTx
                lastTimestamp = now

                val connType = NetworkUtils.getConnectionType(this@NetworkSpeedService)
                val netLabel = NetworkUtils.getNetworkLabel(this@NetworkSpeedService)
                val isRoaming = NetworkUtils.isRoaming(this@NetworkSpeedService)
                val isMetered = NetworkUtils.isMetered(this@NetworkSpeedService)

                val speedObj = RealtimeSpeed(
                    downloadBps = rxSpeed,
                    uploadBps = txSpeed,
                    connectionType = connType,
                    networkLabel = netLabel,
                    isMetered = isMetered,
                    isRoaming = isRoaming
                )
                _currentSpeed.value = speedObj

                // Update status bar notification
                val speedText = "↓ ${NetworkUtils.formatSpeed(rxSpeed)}  ↑ ${NetworkUtils.formatSpeed(txSpeed)}"
                val subText = "$netLabel ${if (isRoaming) "• Roaming" else ""}"
                updateNotification(speedText, subText)

                // Poll every 1.5 seconds when active (battery efficient)
                delay(1500)
            }
        }
    }

    private fun buildNotification(speedText: String, subText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, NetworkSpeedService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(speedText)
            .setContentText(subText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification(speedText: String, subText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(speedText, subText))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Internet Speed Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time upload and download speeds in status bar"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        speedJob?.cancel()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
