package com.example.data.repository

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.util.Log
import com.example.data.local.AppBlockDao
import com.example.data.local.AppBlockRuleEntity
import com.example.data.local.DataPlanDao
import com.example.data.local.DataPlanEntity
import com.example.data.local.UsageDao
import com.example.data.local.UsageSnapshotEntity
import com.example.data.model.AppUsageInfo
import com.example.data.model.DayUsagePoint
import com.example.data.model.PeriodUsageSummary
import com.example.data.model.UsagePeriod
import com.example.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DataUsageRepository(
    private val context: Context,
    private val usageDao: UsageDao,
    private val appBlockDao: AppBlockDao,
    private val dataPlanDao: DataPlanDao
) {
    private val networkStatsManager: NetworkStatsManager? =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager

    val dataPlanFlow: Flow<DataPlanEntity?> = dataPlanDao.getDataPlan()
    val appBlockRulesFlow: Flow<List<AppBlockRuleEntity>> = appBlockDao.getAllRules()

    suspend fun getOrCreateDataPlan(): DataPlanEntity = withContext(Dispatchers.IO) {
        var plan = dataPlanDao.getDataPlanSync()
        if (plan == null) {
            plan = DataPlanEntity()
            dataPlanDao.insertOrUpdateDataPlan(plan)
        }
        plan
    }

    suspend fun updateDataPlan(plan: DataPlanEntity) = withContext(Dispatchers.IO) {
        dataPlanDao.insertOrUpdateDataPlan(plan)
    }

    suspend fun setWifiBlocked(packageName: String, blocked: Boolean) = withContext(Dispatchers.IO) {
        appBlockDao.setWifiBlocked(packageName, blocked)
    }

    suspend fun setMobileBlocked(packageName: String, blocked: Boolean) = withContext(Dispatchers.IO) {
        appBlockDao.setMobileBlocked(packageName, blocked)
    }

    suspend fun setWhitelisted(packageName: String, whitelisted: Boolean) = withContext(Dispatchers.IO) {
        appBlockDao.setWhitelisted(packageName, whitelisted)
    }

    suspend fun upsertRule(rule: AppBlockRuleEntity) = withContext(Dispatchers.IO) {
        appBlockDao.upsertRule(rule)
    }

    /**
     * Compute device-level usage for requested period
     */
    suspend fun getUsageForPeriod(period: UsagePeriod): PeriodUsageSummary = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        when (period) {
            UsagePeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis

                val wifi = queryTotalBytes(NetworkCapabilities.TRANSPORT_WIFI, startOfDay, now)
                val mobile = queryTotalBytes(NetworkCapabilities.TRANSPORT_CELLULAR, startOfDay, now)

                // Previous day for comparison
                val yesterdayStart = startOfDay - 86400000L
                val prevWifi = queryTotalBytes(NetworkCapabilities.TRANSPORT_WIFI, yesterdayStart, startOfDay)
                val prevMobile = queryTotalBytes(NetworkCapabilities.TRANSPORT_CELLULAR, yesterdayStart, startOfDay)
                val prevTotal = prevWifi + prevMobile
                val currentTotal = wifi + mobile

                val changePercent = if (prevTotal > 0) ((currentTotal - prevTotal).toDouble() / prevTotal) * 100.0 else 0.0

                PeriodUsageSummary(
                    period = UsagePeriod.TODAY,
                    totalWifiBytes = wifi,
                    totalMobileBytes = mobile,
                    previousPeriodTotalBytes = prevTotal,
                    changePercent = changePercent,
                    dailyAverageBytes = currentTotal,
                    breakdownPoints = listOf(
                        DayUsagePoint("Today", wifi, mobile)
                    )
                )
            }

            UsagePeriod.WEEK -> {
                val points = mutableListOf<DayUsagePoint>()
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                var totalWeekWifi = 0L
                var totalWeekMobile = 0L

                for (i in 6 downTo 0) {
                    val c = Calendar.getInstance()
                    c.add(Calendar.DAY_OF_YEAR, -i)
                    c.set(Calendar.HOUR_OF_DAY, 0)
                    c.set(Calendar.MINUTE, 0)
                    c.set(Calendar.SECOND, 0)
                    c.set(Calendar.MILLISECOND, 0)
                    val dayStart = c.timeInMillis
                    val dayEnd = if (i == 0) now else dayStart + 86400000L

                    val dayWifi = queryTotalBytes(NetworkCapabilities.TRANSPORT_WIFI, dayStart, dayEnd)
                    val dayMobile = queryTotalBytes(NetworkCapabilities.TRANSPORT_CELLULAR, dayStart, dayEnd)

                    totalWeekWifi += dayWifi
                    totalWeekMobile += dayMobile
                    points.add(DayUsagePoint(dayFormat.format(Date(dayStart)), dayWifi, dayMobile))
                }

                PeriodUsageSummary(
                    period = UsagePeriod.WEEK,
                    totalWifiBytes = totalWeekWifi,
                    totalMobileBytes = totalWeekMobile,
                    dailyAverageBytes = (totalWeekWifi + totalWeekMobile) / 7,
                    breakdownPoints = points
                )
            }

            UsagePeriod.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis

                val currentWifi = queryTotalBytes(NetworkCapabilities.TRANSPORT_WIFI, startOfMonth, now)
                val currentMobile = queryTotalBytes(NetworkCapabilities.TRANSPORT_CELLULAR, startOfMonth, now)
                val currentTotal = currentWifi + currentMobile

                // Last Month
                val prevCal = Calendar.getInstance()
                prevCal.add(Calendar.MONTH, -1)
                prevCal.set(Calendar.DAY_OF_MONTH, 1)
                prevCal.set(Calendar.HOUR_OF_DAY, 0)
                val startOfLastMonth = prevCal.timeInMillis
                val endOfLastMonth = startOfMonth - 1

                val prevWifi = queryTotalBytes(NetworkCapabilities.TRANSPORT_WIFI, startOfLastMonth, endOfLastMonth)
                val prevMobile = queryTotalBytes(NetworkCapabilities.TRANSPORT_CELLULAR, startOfLastMonth, endOfLastMonth)
                val prevTotal = prevWifi + prevMobile

                val diffPercent = if (prevTotal > 0) ((currentTotal - prevTotal).toDouble() / prevTotal) * 100.0 else 0.0

                // 4-week breakdown
                val points = mutableListOf<DayUsagePoint>()
                val weekMs = 7 * 86400000L
                for (w in 0..3) {
                    val wStart = startOfMonth + (w * weekMs)
                    val wEnd = (wStart + weekMs).coerceAtMost(now)
                    if (wStart <= now) {
                        val wWifi = queryTotalBytes(NetworkCapabilities.TRANSPORT_WIFI, wStart, wEnd)
                        val wMobile = queryTotalBytes(NetworkCapabilities.TRANSPORT_CELLULAR, wStart, wEnd)
                        points.add(DayUsagePoint("W${w + 1}", wWifi, wMobile))
                    }
                }

                val daysPassed = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)

                PeriodUsageSummary(
                    period = UsagePeriod.MONTH,
                    totalWifiBytes = currentWifi,
                    totalMobileBytes = currentMobile,
                    previousPeriodTotalBytes = prevTotal,
                    changePercent = diffPercent,
                    dailyAverageBytes = currentTotal / daysPassed,
                    breakdownPoints = points
                )
            }

            UsagePeriod.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                val startOfYear = calendar.timeInMillis

                val currentWifi = queryTotalBytes(NetworkCapabilities.TRANSPORT_WIFI, startOfYear, now)
                val currentMobile = queryTotalBytes(NetworkCapabilities.TRANSPORT_CELLULAR, startOfYear, now)

                val points = mutableListOf<DayUsagePoint>()
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val currentMonth = Calendar.getInstance().get(Calendar.MONTH)

                for (m in 0..currentMonth) {
                    val mCal = Calendar.getInstance()
                    mCal.set(Calendar.MONTH, m)
                    mCal.set(Calendar.DAY_OF_MONTH, 1)
                    mCal.set(Calendar.HOUR_OF_DAY, 0)
                    val mStart = mCal.timeInMillis

                    val nextMCal = Calendar.getInstance()
                    nextMCal.set(Calendar.MONTH, m + 1)
                    nextMCal.set(Calendar.DAY_OF_MONTH, 1)
                    nextMCal.set(Calendar.HOUR_OF_DAY, 0)
                    val mEnd = nextMCal.timeInMillis.coerceAtMost(now)

                    val mWifi = queryTotalBytes(NetworkCapabilities.TRANSPORT_WIFI, mStart, mEnd)
                    val mMobile = queryTotalBytes(NetworkCapabilities.TRANSPORT_CELLULAR, mStart, mEnd)
                    points.add(DayUsagePoint(monthFormat.format(Date(mStart)), mWifi, mMobile))
                }

                PeriodUsageSummary(
                    period = UsagePeriod.YEAR,
                    totalWifiBytes = currentWifi,
                    totalMobileBytes = currentMobile,
                    dailyAverageBytes = (currentWifi + currentMobile) / (currentMonth + 1).coerceAtLeast(1),
                    breakdownPoints = points
                )
            }

            UsagePeriod.LIFETIME -> {
                // Approximate lifetime starting from 1 year ago or boot
                val lifetimeStart = now - (365L * 86400000L)
                val wifi = queryTotalBytes(NetworkCapabilities.TRANSPORT_WIFI, lifetimeStart, now)
                val mobile = queryTotalBytes(NetworkCapabilities.TRANSPORT_CELLULAR, lifetimeStart, now)

                PeriodUsageSummary(
                    period = UsagePeriod.LIFETIME,
                    totalWifiBytes = wifi,
                    totalMobileBytes = mobile,
                    breakdownPoints = emptyList()
                )
            }
        }
    }

    private fun queryTotalBytes(networkType: Int, startTime: Long, endTime: Long): Long {
        if (!NetworkUtils.hasUsageStatsPermission(context) || networkStatsManager == null) {
            // Fallback gracefully to TrafficStats aggregate if permissions not yet approved
            return if (networkType == NetworkCapabilities.TRANSPORT_WIFI) {
                val total = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()
                val mobile = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()
                (total - mobile).coerceAtLeast(0L)
            } else {
                (TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()).coerceAtLeast(0L)
            }
        }

        return try {
            val bucket = networkStatsManager.querySummaryForDevice(networkType, null, startTime, endTime)
            bucket.rxBytes + bucket.txBytes
        } catch (e: Exception) {
            Log.e("DataUsageRepo", "Error querying summary for device: ${e.message}")
            0L
        }
    }

    /**
     * Get per-app breakdown with foreground & background separation
     */
    suspend fun getPerAppUsage(period: UsagePeriod): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val startTime = when (period) {
            UsagePeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            UsagePeriod.WEEK -> now - (7L * 86400000L)
            UsagePeriod.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.timeInMillis
            }
            UsagePeriod.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.timeInMillis
            }
            UsagePeriod.LIFETIME -> now - (365L * 86400000L)
        }

        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appMap = mutableMapOf<String, AppUsageInfo>()

        // Preload saved block rules
        val existingRules = appBlockDao.getAllRules()
        val rulesMap = mutableMapOf<String, AppBlockRuleEntity>()

        // Collect stats if permission granted
        val uidStats = mutableMapOf<Int, UidDataAcc>()

        if (NetworkUtils.hasUsageStatsPermission(context) && networkStatsManager != null) {
            // Query Wi-Fi
            queryPerUid(NetworkCapabilities.TRANSPORT_WIFI, startTime, now, uidStats, isWifi = true)
            // Query Mobile
            queryPerUid(NetworkCapabilities.TRANSPORT_CELLULAR, startTime, now, uidStats, isWifi = false)
        }

        for (app in installedApps) {
            // Filter non-system apps or important system apps that have launcher intent
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val hasLauncher = pm.getLaunchIntentForPackage(app.packageName) != null
            if (isSystem && !hasLauncher) continue

            val appName = pm.getApplicationLabel(app).toString()
            val icon = try { pm.getApplicationIcon(app) } catch (e: Exception) { null }
            val acc = uidStats[app.uid] ?: UidDataAcc()

            // If permissions not yet granted, query TrafficStats per uid for baseline
            val wifi = if (acc.wifiBytes > 0) acc.wifiBytes else TrafficStats.getUidRxBytes(app.uid).coerceAtLeast(0L)
            val mobile = if (acc.mobileBytes > 0) acc.mobileBytes else TrafficStats.getUidTxBytes(app.uid).coerceAtLeast(0L)

            val rule = rulesMap[app.packageName]

            appMap[app.packageName] = AppUsageInfo(
                packageName = app.packageName,
                appName = appName,
                icon = icon,
                wifiBytes = wifi,
                mobileBytes = mobile,
                foregroundBytes = acc.fgBytes,
                backgroundBytes = acc.bgBytes,
                isBlockedWifi = rule?.isBlockedWifi ?: false,
                isBlockedMobile = rule?.isBlockedMobile ?: false,
                isWhitelisted = rule?.isWhitelisted ?: false,
                dailyLimitMb = rule?.dailyLimitMb ?: 0
            )
        }

        // Return sorted by total bytes descending
        appMap.values.sortedByDescending { it.totalBytes }
    }

    private fun queryPerUid(
        networkType: Int,
        startTime: Long,
        endTime: Long,
        targetMap: MutableMap<Int, UidDataAcc>,
        isWifi: Boolean
    ) {
        if (networkStatsManager == null) return
        try {
            val stats = networkStatsManager.querySummary(networkType, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val uid = bucket.uid
                val bytes = bucket.rxBytes + bucket.txBytes
                val acc = targetMap.getOrPut(uid) { UidDataAcc() }
                if (isWifi) {
                    acc.wifiBytes += bytes
                } else {
                    acc.mobileBytes += bytes
                }

                if (bucket.state == NetworkStats.Bucket.STATE_FOREGROUND) {
                    acc.fgBytes += bytes
                } else {
                    acc.bgBytes += bytes
                }
            }
            stats.close()
        } catch (e: Exception) {
            Log.e("DataUsageRepo", "Error in queryPerUid: ${e.message}")
        }
    }

    suspend fun recordSnapshot() = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateString = dateFormat.format(Date())
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val wifiRx = TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes()
        val wifiTx = TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes()
        val mobileRx = TrafficStats.getMobileRxBytes()
        val mobileTx = TrafficStats.getMobileTxBytes()

        val snapshot = UsageSnapshotEntity(
            dateString = dateString,
            hour = hour,
            wifiRxBytes = wifiRx.coerceAtLeast(0L),
            wifiTxBytes = wifiTx.coerceAtLeast(0L),
            mobileRxBytes = mobileRx.coerceAtLeast(0L),
            mobileTxBytes = mobileTx.coerceAtLeast(0L)
        )
        usageDao.insertSnapshot(snapshot)
    }

    private class UidDataAcc {
        var wifiBytes: Long = 0L
        var mobileBytes: Long = 0L
        var fgBytes: Long = 0L
        var bgBytes: Long = 0L
    }
}
