package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DataPlanDao {
    @Query("SELECT * FROM data_plan WHERE id = 1")
    fun getDataPlan(): Flow<DataPlanEntity?>

    @Query("SELECT * FROM data_plan WHERE id = 1")
    suspend fun getDataPlanSync(): DataPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDataPlan(plan: DataPlanEntity)
}
