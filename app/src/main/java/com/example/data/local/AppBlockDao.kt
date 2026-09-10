package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppBlockDao {
    @Query("SELECT * FROM app_block_rules ORDER BY appName ASC")
    fun getAllRules(): Flow<List<AppBlockRuleEntity>>

    @Query("SELECT * FROM app_block_rules WHERE packageName = :packageName")
    suspend fun getRule(packageName: String): AppBlockRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: AppBlockRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRules(rules: List<AppBlockRuleEntity>)

    @Query("UPDATE app_block_rules SET isBlockedWifi = :blocked WHERE packageName = :packageName")
    suspend fun setWifiBlocked(packageName: String, blocked: Boolean)

    @Query("UPDATE app_block_rules SET isBlockedMobile = :blocked WHERE packageName = :packageName")
    suspend fun setMobileBlocked(packageName: String, blocked: Boolean)

    @Query("UPDATE app_block_rules SET isWhitelisted = :whitelisted WHERE packageName = :packageName")
    suspend fun setWhitelisted(packageName: String, whitelisted: Boolean)

    @Query("DELETE FROM app_block_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)
}
