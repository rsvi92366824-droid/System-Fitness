package com.example.data

import android.icu.text.SimpleDateFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date
import java.util.Locale

class QuestRepository(private val questDao: QuestDao) {

    // Obtain current date string in standard YYYY-MM-DD
    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // Get User Stats Flow
    val userStatsFlow: Flow<UserStats?> = questDao.getUserStatsFlow()

    // Get Daily Quest for today
    fun getTodayQuestFlow(): Flow<DailyQuest?> {
        val today = getTodayDateString()
        return questDao.getDailyQuestFlow(today)
    }

    val workoutLogsFlow: Flow<List<WorkoutLog>> = questDao.getAllWorkoutLogsFlow()

    val inventoryItemsFlow: Flow<List<InventoryItem>> = questDao.getAllInventoryItemsFlow()

    // Initialize and seed default stats and inventory items if vacant
    suspend fun initializeDatabase() {
        // 1. Seed User Stats if null
        val existingStats = questDao.getUserStats()
        if (existingStats == null) {
            questDao.insertUserStats(UserStats())
        }

        // 2. Seed Shop Inventory Items if empty
        val existingItems = questDao.getAllInventoryItemsFlow().firstOrNull() ?: emptyList()
        if (existingItems.isEmpty()) {
            val defaultItems = listOf(
                InventoryItem(
                    itemId = "potion_low",
                    name = "E-Rank Elixir",
                    description = "A standard recovery potion brewed by the System.",
                    quantity = 1,
                    itemType = "POTION",
                    effectDescription = "Reduces Fatigue by 25",
                    costInGold = 40
                ),
                InventoryItem(
                    itemId = "potion_high",
                    name = "Full Elixir of Life",
                    description = "A rare divine potion that completely purges physical toll.",
                    quantity = 0,
                    itemType = "POTION",
                    effectDescription = "Resets Fatigue to 0",
                    costInGold = 150
                ),
                InventoryItem(
                    itemId = "key_dungeon_d",
                    name = "D-Rank Gate Key",
                    description = "Unlocks a basic Instance Dungeon. Recommended for casual fat burn.",
                    quantity = 1,
                    itemType = "KEY",
                    effectDescription = "Enter a 3-Minute Cardio Dungeon Battle",
                    costInGold = 100
                ),
                InventoryItem(
                    itemId = "key_dungeon_s",
                    name = "Shadow monarch Gate Key",
                    description = "An ominous obsidian key emitting cold shadow energy.",
                    quantity = 0,
                    itemType = "KEY",
                    effectDescription = "Enter a 10-Minute Extreme HIIT Dungeon",
                    costInGold = 400
                ),
                InventoryItem(
                    itemId = "equipment_vest",
                    name = "System Heavy Vest",
                    description = "An equippable heavy tactical vest that hardens muscle tissues.",
                    quantity = 0,
                    itemType = "EQUIPMENT",
                    effectDescription = "Passive: Double EXP on Squats and Pushups",
                    costInGold = 250
                )
            )
            questDao.insertAllInventoryItems(defaultItems)
        }

        // 3. Ensure today's Daily Quest is created
        ensureTodayQuestCreated()
    }

    private suspend fun ensureTodayQuestCreated() {
        val today = getTodayDateString()
        if (questDao.getDailyQuest(today) == null) {
            questDao.insertDailyQuest(
                DailyQuest(
                    dateString = today,
                    pushupsGoal = 100,
                    situpsGoal = 100,
                    squatsGoal = 100,
                    runningGoalMeters = 10000 // 10 km
                )
            )
        }
    }

    // Allocate Stat point
    suspend fun allocateStatPoint(statName: String) {
        val stats = questDao.getUserStats() ?: return
        if (stats.statusPoints <= 0) return

        val updatedStats = when (statName) {
            "STRENGTH" -> stats.copy(
                strength = stats.strength + 1,
                statusPoints = stats.statusPoints - 1,
                hpMax = stats.hpMax + 10 // Strength adds max HP
            )
            "AGILITY" -> stats.copy(
                agility = stats.agility + 1,
                statusPoints = stats.statusPoints - 1
            )
            "SENSE" -> stats.copy(
                sense = stats.sense + 1,
                statusPoints = stats.statusPoints - 1
            )
            "VITALITY" -> stats.copy(
                vitality = stats.vitality + 1,
                statusPoints = stats.statusPoints - 1,
                hpMax = stats.hpMax + 20 // Vitality adds more max HP
            )
            "INTELLIGENCE" -> stats.copy(
                intelligence = stats.intelligence + 1,
                statusPoints = stats.statusPoints - 1,
                mpMax = stats.mpMax + 10 // Intelligence adds MP
            )
            else -> stats
        }
        questDao.insertUserStats(updatedStats)
    }

    // Log progress on daily quests or standard workout
    suspend fun logWorkoutProgress(type: String, amount: Int, caloriesBurned: Int) {
        val today = getTodayDateString()
        ensureTodayQuestCreated()
        val quest = questDao.getDailyQuest(today) ?: return
        val stats = questDao.getUserStats() ?: return

        // 1. Update progress metrics
        val updatedQuest = when (type.lowercase()) {
            "push-ups", "pushups" -> quest.copy(
                pushupsProgress = (quest.pushupsProgress + amount).coerceAtMost(quest.pushupsGoal)
            )
            "sit-ups", "situps" -> quest.copy(
                situpsProgress = (quest.situpsProgress + amount).coerceAtMost(quest.situpsGoal)
            )
            "squats" -> quest.copy(
                squatsProgress = (quest.squatsProgress + amount).coerceAtMost(quest.squatsGoal)
            )
            "running" -> quest.copy(
                runningProgressMeters = (quest.runningProgressMeters + amount).coerceAtMost(quest.runningGoalMeters)
            )
            else -> quest
        }

        // 2. Check if daily quest completed in this log
        val wasCompleted = quest.isCompleted
        val isNowCompleted = (updatedQuest.pushupsProgress >= updatedQuest.pushupsGoal &&
                updatedQuest.situpsProgress >= updatedQuest.situpsGoal &&
                updatedQuest.squatsProgress >= updatedQuest.squatsGoal &&
                updatedQuest.runningProgressMeters >= updatedQuest.runningGoalMeters)

        val questWithStatus = updatedQuest.copy(isCompleted = isNowCompleted)
        questDao.insertDailyQuest(questWithStatus)

        // 3. Earn rewards
        val expGain = amount * (if (stats.strength > 15) 2 else 1)
        val goldGain = amount * 2

        val log = WorkoutLog(
            type = type,
            amount = amount,
            caloriesBurned = caloriesBurned,
            expEarned = expGain,
            goldEarned = goldGain,
            summary = "Completed $amount units of $type ($caloriesBurned kcal)"
        )
        questDao.insertWorkoutLog(log)

        // 4. Increase fatigue and process experience increments in stats
        var updatedFatigue = (stats.fatigue + (amount / 10).coerceAtLeast(1))
            .coerceIn(0, 100)

        // Give them a special reward on First Complete of the Day
        var bonusExp = 0
        var bonusGold = 0
        var bonusPoints = 0
        if (isNowCompleted && !wasCompleted) {
            bonusExp = 50
            bonusGold = 100
            bonusPoints = 5
        }

        addExperienceAndGold(
            expToGain = expGain + bonusExp,
            goldToGain = goldGain + bonusGold,
            customFatigue = updatedFatigue,
            bonusStatusPoints = bonusPoints
        )
    }

    // Log general dungeon achievements or customized HIIT cardio
    suspend fun claimDungeonClear(dungeonName: String, durationMin: Int, calories: Int) {
        val stats = questDao.getUserStats() ?: return
        val expGain = durationMin * 15
        val goldGain = durationMin * 25

        val log = WorkoutLog(
            type = "$dungeonName Clear",
            amount = durationMin,
            caloriesBurned = calories,
            expEarned = expGain,
            goldEarned = goldGain,
            summary = "Successfully Cleared $dungeonName in $durationMin minutes! Gained $expGain EXP and $goldGain gold!"
        )
        questDao.insertWorkoutLog(log)

        // Dungeons drain energy heavily
        val updatedFatigue = (stats.fatigue + durationMin * 3).coerceIn(0, 100)
        addExperienceAndGold(expGain, goldGain, updatedFatigue)
    }

    suspend fun addExperienceAndGold(
        expToGain: Int,
        goldToGain: Int,
        customFatigue: Int? = null,
        bonusStatusPoints: Int = 0
    ) {
        val stats = questDao.getUserStats() ?: return
        var currentExp = stats.exp + expToGain
        var currentLevel = stats.level
        var currentPoints = stats.statusPoints + bonusStatusPoints
        var hpMax = stats.hpMax
        var mpMax = stats.mpMax

        // Handle Level up mechanics (100% boundary check)
        while (currentExp >= 100) {
            currentExp -= 100
            currentLevel += 1
            currentPoints += 5 // 5 free points on Level Up
            hpMax += 50
            mpMax += 15
        }

        val updatedFatigue = customFatigue ?: stats.fatigue

        // Update rank based on level milestones
        val rankName = when {
            currentLevel >= 80 -> "S-Rank Status"
            currentLevel >= 60 -> "A-Rank Status"
            currentLevel >= 40 -> "B-Rank Status"
            currentLevel >= 25 -> "C-Rank Status"
            currentLevel >= 12 -> "D-Rank Status"
            else -> "E-Rank Status"
        }

        // Update titles based on strength milestones
        val title = when {
            stats.strength >= 50 -> "Shadow Monarch"
            stats.strength >= 35 -> "Fat Slayer"
            stats.strength >= 20 -> "Undefeated Warrior"
            else -> "The Awakened"
        }

        val finalStats = stats.copy(
            level = currentLevel,
            exp = currentExp,
            statusPoints = currentPoints,
            gold = stats.gold + goldToGain,
            fatigue = updatedFatigue,
            hpMax = hpMax,
            hpCurrent = hpMax,
            mpMax = mpMax,
            mpCurrent = mpMax,
            className = rankName,
            titleName = title,
            lastUpdated = System.currentTimeMillis()
        )
        questDao.insertUserStats(finalStats)
    }

    // Purchase items from Store
    suspend fun purchaseStoreItem(id: String): Boolean {
        val stats = questDao.getUserStats() ?: return false
        val item = questDao.getInventoryItem(id) ?: return false

        if (stats.gold < item.costInGold) return false

        // Charge gold
        val updatedStats = stats.copy(gold = stats.gold - item.costInGold)
        questDao.insertUserStats(updatedStats)

        // Increment Inventory quantity
        val updatedItem = item.copy(quantity = item.quantity + 1)
        questDao.insertInventoryItem(updatedItem)

        // Log transaction
        questDao.insertWorkoutLog(
            WorkoutLog(
                type = "Store Purchase",
                amount = 1,
                expEarned = 0,
                goldEarned = 0,
                summary = "Purchased ${item.name} for ${item.costInGold} gold"
            )
        )
        return true
    }

    // Use Elixirs / Potions to reduce Fatigue
    suspend fun useInventoryItem(id: String): Boolean {
        val item = questDao.getInventoryItem(id) ?: return false
        val stats = questDao.getUserStats() ?: return false

        if (item.quantity <= 0) return false

        var finalFatigue = stats.fatigue

        when (id) {
            "potion_low" -> {
                finalFatigue = (stats.fatigue - 25).coerceAtLeast(0)
            }
            "potion_high" -> {
                finalFatigue = 0
            }
            else -> {
                // If it is a key or equipment, let's treat usage as triggers inside screens
                return false
            }
        }

        // Consume index and update database
        questDao.insertInventoryItem(item.copy(quantity = item.quantity - 1))
        questDao.insertUserStats(stats.copy(fatigue = finalFatigue))

        questDao.insertWorkoutLog(
            WorkoutLog(
                type = "Item Consumed",
                amount = 1,
                expEarned = 0,
                goldEarned = 0,
                summary = "Used ${item.name} and recovered physical endurance qualities!"
            )
        )
        return true
    }

    // Rest to recover partial fatigue (daily fatigue recovery/manual resting trigger)
    suspend fun completeRest() {
        val stats = questDao.getUserStats() ?: return
        val finalFatigue = (stats.fatigue - 15).coerceAtLeast(0)
        questDao.insertUserStats(stats.copy(fatigue = finalFatigue))
    }

    // Setup custom client-side quest limits for Weight Loss orientation
    suspend fun reconfigureQuestOriented(isWeightLoss: Boolean) {
        val today = getTodayDateString()
        val quest = questDao.getDailyQuest(today) ?: return
        if (isWeightLoss) {
            // weight loss focuses on running and squats (fat burning highlights)
            questDao.insertDailyQuest(
                quest.copy(
                    pushupsGoal = 60,
                    situpsGoal = 80,
                    squatsGoal = 120,
                    runningGoalMeters = 8000
                )
            )
        } else {
            // muscle focus highlights pushups, squats and sit-ups high goals
            questDao.insertDailyQuest(
                quest.copy(
                    pushupsGoal = 120,
                    situpsGoal = 100,
                    squatsGoal = 100,
                    runningGoalMeters = 5000
                )
            )
        }
    }
}
