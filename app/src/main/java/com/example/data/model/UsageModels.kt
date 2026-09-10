package com.example.data.model

import android.graphics.drawable.Drawable

enum class UsagePeriod {
    TODAY,
    WEEK,
    MONTH,
    YEAR,
    LIFETIME
}

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val wifiBytes: Long = 0L,
    val mobileBytes: Long = 0L,
    val foregroundBytes: Long = 0L,
    val backgroundBytes: Long = 0L,
    val isBlockedWifi: Boolean = false,
    val isBlockedMobile: Boolean = false,
    val isWhitelisted: Boolean = false,
    val dailyLimitMb: Int = 0
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes
}

data class DayUsagePoint(
    val label: String,
    val wifiBytes: Long,
    val mobileBytes: Long
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes
}

data class PeriodUsageSummary(
    val period: UsagePeriod,
    val totalWifiBytes: Long = 0L,
    val totalMobileBytes: Long = 0L,
    val previousPeriodTotalBytes: Long = 0L,
    val changePercent: Double = 0.0,
    val dailyAverageBytes: Long = 0L,
    val breakdownPoints: List<DayUsagePoint> = emptyList()
) {
    val totalBytes: Long get() = totalWifiBytes + totalMobileBytes
}

data class RealtimeSpeed(
    val downloadBps: Long = 0L,
    val uploadBps: Long = 0L,
    val connectionType: ConnectionType = ConnectionType.WIFI,
    val networkLabel: String = "Wi-Fi",
    val isMetered: Boolean = false,
    val isRoaming: Boolean = false
)

enum class ConnectionType {
    WIFI,
    MOBILE,
    OFFLINE
}
