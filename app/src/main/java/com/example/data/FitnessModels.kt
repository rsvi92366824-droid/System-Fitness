package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1, // Single player app, id always 1
    val name: String = "Hunter",
    val level: Int = 1,
    val exp: Int = 0, // 0 to 100%
    val hpMax: Int = 100,
    val hpCurrent: Int = 100,
    val mpMax: Int = 10,
    val mpCurrent: Int = 10,
    val fatigue: Int = 0, // 0 to 100 limit. Quests increase it, potions/rest reduce it.
    
    // Core Attributes
    val strength: Int = 10,
    val agility: Int = 10,
    val sense: Int = 10,
    val vitality: Int = 10,
    val intelligence: Int = 10,
    val statusPoints: Int = 5, // Points gained on Level Up to spend
    
    val gold: Int = 100, // Currency to buy Dungeon keys / items from store
    val weightKg: Double = 85.0,
    val targetWeightKg: Double = 70.0,
    val heightCm: Double = 175.0,
    val className: String = "E-Rank Player",
    val titleName: String = "The Awakened",
    val dailyStreak: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_quests")
data class DailyQuest(
    @PrimaryKey val dateString: String, // format "YYYY-MM-DD" e.g. "2026-06-03"
    val pushupsGoal: Int = 100,
    val pushupsProgress: Int = 0,
    val situpsGoal: Int = 100,
    val situpsProgress: Int = 0,
    val squatsGoal: Int = 100,
    val squatsProgress: Int = 0,
    val runningGoalMeters: Int = 10000,
    val runningProgressMeters: Int = 0,
    val isCompleted: Boolean = false,
    val penaltyTriggered: Boolean = false,
    val rewardsClaimed: Boolean = false
)

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateLong: Long = System.currentTimeMillis(),
    val type: String, // "Push-ups", "Sit-ups", "Squats", "Running", "General Cardio", "Plank", "Shadow Dungeon"
    val amount: Int, // Count, or seconds for duration-based
    val caloriesBurned: Int = 0,
    val expEarned: Int = 0,
    val goldEarned: Int = 0,
    val summary: String = ""
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey val itemId: String, // e.g. "potion_low", "dungeon_key_d", "heavy_vest"
    val name: String,
    val description: String,
    val quantity: Int = 0,
    val itemType: String, // "POTION", "KEY", "EQUIPMENT"
    val effectDescription: String, // e.g. "Reduces Fatigue by 40", "Unlocks Instance Dungeon"
    val costInGold: Int = 100
)
