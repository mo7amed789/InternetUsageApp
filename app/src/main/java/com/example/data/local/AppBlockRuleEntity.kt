package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_block_rules")
data class AppBlockRuleEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isBlockedWifi: Boolean = false,
    val isBlockedMobile: Boolean = false,
    val isWhitelisted: Boolean = false,
    val dailyLimitMb: Int = 0 // 0 means unmetered
)
