package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_plan")
data class DataPlanEntity(
    @PrimaryKey
    val id: Int = 1,
    val monthlyLimitMb: Long = 20480, // Default 20 GB
    val warningThresholdPercent: Int = 80,
    val billingCycleDay: Int = 1,
    val unlimitedWifi: Boolean = true,
    val carrierName: String = "Mobile Data",
    val isRoamingAlertEnabled: Boolean = true,
    val isAutoSaverOnLimit: Boolean = true,
    val isVpnDataSaverActive: Boolean = false,
    val isLiveNotificationEnabled: Boolean = true,
    val isFloatingBubbleEnabled: Boolean = false,
    val isArabicLanguage: Boolean = true
)
