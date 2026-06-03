package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStatsFlow(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStats(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStats)

    @Query("SELECT * FROM daily_quests WHERE dateString = :date LIMIT 1")
    fun getDailyQuestFlow(date: String): Flow<DailyQuest?>

    @Query("SELECT * FROM daily_quests WHERE dateString = :date LIMIT 1")
    suspend fun getDailyQuest(date: String): DailyQuest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyQuest(quest: DailyQuest)

    @Query("SELECT * FROM workout_logs ORDER BY dateLong DESC")
    fun getAllWorkoutLogsFlow(): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLog)

    @Query("SELECT * FROM inventory_items")
    fun getAllInventoryItemsFlow(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE itemId = :id LIMIT 1")
    suspend fun getInventoryItem(id: String): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllInventoryItems(items: List<InventoryItem>)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)
}
