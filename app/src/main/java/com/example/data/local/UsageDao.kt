package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<UsageSnapshotEntity>>

    @Query("SELECT * FROM usage_snapshots WHERE dateString = :date ORDER BY hour ASC")
    fun getSnapshotsForDate(date: String): Flow<List<UsageSnapshotEntity>>

    @Query("SELECT * FROM usage_snapshots WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getSnapshotsSince(sinceTimestamp: Long): Flow<List<UsageSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: UsageSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(snapshots: List<UsageSnapshotEntity>)

    @Query("DELETE FROM usage_snapshots WHERE timestamp < :olderThanTimestamp")
    suspend fun deleteOldSnapshots(olderThanTimestamp: Long)
}
