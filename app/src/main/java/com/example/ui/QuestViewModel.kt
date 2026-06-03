package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.api.GeminiRetrofitClient
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class QuestViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = QuestRepository(db.questDao())

    // Expose DB flows converted to StateFlows using standard subscribers
    val userStatsState: StateFlow<UserStats?> = repository.userStatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val todayQuestState: StateFlow<DailyQuest?> = repository.getTodayQuestFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val workoutLogsState: StateFlow<List<WorkoutLog>> = repository.workoutLogsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val inventoryItemsState: StateFlow<List<InventoryItem>> = repository.inventoryItemsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI state
    val totalCaloriesToday: StateFlow<Int> = repository.workoutLogsFlow
        .map { logs ->
            val todayDate = repository.getTodayDateString()
            logs.filter { log ->
                val sdf = android.icu.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                sdf.format(java.util.Date(log.dateLong)) == todayDate
            }.sumOf { it.caloriesBurned }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Dynamic Level Up Indicator Trigger
    private val _showLevelUpDialog = MutableStateFlow<Int?>(null) // contains new level
    val showLevelUpDialog: StateFlow<Int?> = _showLevelUpDialog

    // Active dungeon simulation
    private val _dungeonActive = MutableStateFlow(false)
    val dungeonActive: StateFlow<Boolean> = _dungeonActive

    private val _dungeonName = MutableStateFlow("")
    val dungeonName: StateFlow<String> = _dungeonName

    private val _dungeonSecondsLeft = MutableStateFlow(0)
    val dungeonSecondsLeft: StateFlow<Int> = _dungeonSecondsLeft

    private val _dungeonCaloriesBurned = MutableStateFlow(0)
    val dungeonCaloriesBurned: StateFlow<Int> = _dungeonCaloriesBurned

    private val _dungeonTargetCalories = MutableStateFlow(100)
    val dungeonTargetCalories: StateFlow<Int> = _dungeonTargetCalories

    // Penalty mode simulation
    private val _penaltyActive = MutableStateFlow(false)
    val penaltyActive: StateFlow<Boolean> = _penaltyActive

    private val _penaltySecondsLeft = MutableStateFlow(180) // 3 mins penalty survival
    val penaltySecondsLeft: StateFlow<Int> = _penaltySecondsLeft

    // System Messaging and AI Console
    private val _systemResponse = MutableStateFlow(
        "[SYSTEM WELCOME: COLD STORAGE BOOTED]\n\n" +
                "\"Welcome, Player. Complete your daily tasks to level up and avoid penalty triggers. The Administrator is monitoring your fat loss rate.\""
    )
    val systemResponse: StateFlow<String> = _systemResponse

    private val _isSystemLoading = MutableStateFlow(false)
    val isSystemLoading: StateFlow<Boolean> = _isSystemLoading

    private var dungeonJob: Job? = null
    private var penaltyJob: Job? = null
    private var initialLevel: Int = 1

    init {
        viewModelScope.launch {
            repository.initializeDatabase()
            // Track player's level to prompt Level Up dialogs dynamically
            repository.userStatsFlow.firstOrNull()?.let {
                initialLevel = it.level
            }

            // Monitor level and trigger dynamic level ups dialog
            repository.userStatsFlow.collect { stats ->
                if (stats != null && stats.level > initialLevel) {
                    _showLevelUpDialog.value = stats.level
                    initialLevel = stats.level
                }
            }
        }
    }

    fun dismissLevelUpDialog() {
        _showLevelUpDialog.value = null
    }

    fun updateWeightMetrics(weight: Double, targetWeight: Double, height: Double) {
        viewModelScope.launch {
            val original = db.questDao().getUserStats() ?: UserStats()
            val updated = original.copy(weightKg = weight, targetWeightKg = targetWeight, heightCm = height)
            db.questDao().insertUserStats(updated)
            _systemResponse.value = "[SYSTEM CALIBRATION: METRICS LOGGED]\n\n" +
                    "\"Physical parameters updated successfully: Current $weight kg | Target $targetWeight kg. Track performance closely.\""
        }
    }

    // Allocate Status attribute point
    fun upgradeStat(statName: String) {
        viewModelScope.launch {
            repository.allocateStatPoint(statName)
        }
    }

    // Manual update workouts
    fun logRepetition(exerciseType: String, reps: Int) {
        viewModelScope.launch {
            // Est. fat-burning calorie multiplier (e.g. 0.15 kcal per rep for squats/pushups)
            val kcal = (reps * 0.15).toInt().coerceAtLeast(1)
            repository.logWorkoutProgress(exerciseType, reps, kcal)
        }
    }

    fun logRunningMeters(meters: Int) {
        viewModelScope.launch {
            // Est. running calorie expenditure: ~0.06 kcal per meter for average pace
            val kcal = (meters * 0.06).toInt().coerceAtLeast(1)
            repository.logWorkoutProgress("Running", meters, kcal)
        }
    }

    // Purchase shop items
    fun buyItem(itemId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.purchaseStoreItem(itemId)
            onComplete(success)
            if (success) {
                _systemResponse.value = "[STORE EVENT: TRANSACTION APPROVED]\n\n" +
                        "\"Item has been added to your inventory. Expend energy, Player, and recover when necessary.\""
            } else {
                _systemResponse.value = "[STORE EXCEPTION: INSUFFICIENT GOLD]\n\n" +
                        "\"You do not possess enough System Gold to acquire this item. Complete more daily quests first.\""
            }
        }
    }

    // Use recovery items
    fun consumeItem(itemId: String) {
        viewModelScope.launch {
            val success = repository.useInventoryItem(itemId)
            if (success) {
                _systemResponse.value = "[INVENTORY USE: STAT RECOVERY]\n\n" +
                        "\"Fatigue metrics have been successfully recalibrated. Prepare for your next daily milestone.\""
            }
        }
    }

    // Trigger daily rest recovery
    fun triggerRest() {
        viewModelScope.launch {
            repository.completeRest()
            _systemResponse.value = "[SYSTEM NOTICE: COMPOSURE GAINED]\n\n" +
                    "\"You rest temporarily. Fatigue is reduced slightly. Consecutive stagnation can initiate penalty vectors.\""
        }
    }

    // Reconfigure Daily goals depending on user desire
    fun setOrientation(isLossType: Boolean) {
        viewModelScope.launch {
            repository.reconfigureQuestOriented(isLossType)
            val format = if (isLossType) "FAT BURN ENHANCED" else "MUSCLE STRENGTH ENHANCED"
            _systemResponse.value = "[SYSTEM RECONFIGURATION: $format]\n\n" +
                    "\"Your daily exercise quotas have been modified. Fat-burn indexes prioritised. Surpass your peak limits.\""
        }
    }

    // Activate the Instance Dungeons (Workout Game loops)
    fun attemptEnterDungeon(keyId: String): Boolean {
        // Find if they have the keys
        val items = inventoryItemsState.value
        val keyItem = items.find { it.itemId == keyId }
        val stats = userStatsState.value ?: return false

        if (keyItem == null || keyItem.quantity <= 0) {
            _systemResponse.value = "[ACCESS DENIED: KEY ABSENT]\n\n" +
                    "\"You must purchase a gate key from the System Shop before challenging Instance Dungeons.\""
            return false
        }

        if (stats.fatigue >= 85) {
            _systemResponse.value = "[EXHAUSTION CRITICAL: CANNOT ENTER]\n\n" +
                    "\"Your fatigue level (${stats.fatigue}%) is too high. Entering dungeons in this state will trigger metabolic failure. Consume E-Rank Potions first.\""
            return false
        }

        // Start Dungeon
        _dungeonActive.value = true
        _dungeonCaloriesBurned.value = 0

        if (keyId == "key_dungeon_d") {
            _dungeonName.value = "D-Rank Instance Gates: Nest of Centipedes"
            _dungeonSecondsLeft.value = 180 // 3 minutes
            _dungeonTargetCalories.value = 45 // 45 kcal burn targets
        } else {
            _dungeonName.value = "S-Rank Gate: Land of Frost Elves"
            _dungeonSecondsLeft.value = 600 // 10 minutes
            _dungeonTargetCalories.value = 150
        }

        // Consume index key
        viewModelScope.launch {
            db.questDao().insertInventoryItem(keyItem.copy(quantity = keyItem.quantity - 1))
        }

        _systemResponse.value = "[WARNING: GATE UNLOCKED]\n\n" +
                "\"You have entered the Instance Dungeon: ${_dungeonName.value}. Survive of your own accord. Move your body to increase calorie burn levels!\""

        startDungeonTimer()
        return true
    }

    private fun startDungeonTimer() {
        dungeonJob?.cancel()
        dungeonJob = viewModelScope.launch {
            while (_dungeonSecondsLeft.value > 0 && _dungeonActive.value) {
                delay(1000)
                _dungeonSecondsLeft.value = _dungeonSecondsLeft.value - 1
                
                // Simulate calorie burning increments as user workouts (e.g. they log movement progress inside UI or simple pacing)
                // We'll also allow clicking a button for "Step Workout" which adds to calorie logs actively!
            }
            if (_dungeonActive.value) {
                // If time ran out and calories target wasn't met: failed!
                if (_dungeonCaloriesBurned.value < _dungeonTargetCalories.value) {
                    _dungeonActive.value = false
                    _systemResponse.value = "[DUNGEON FAILURE: INSTANCE DESTROYED]\n\n" +
                            "\"You failed to breach the core before exhaustion claimed the dimensional gate. Fatigue has increased immensely.\""
                    val currentStats = db.questDao().getUserStats()
                    if (currentStats != null) {
                        db.questDao().insertUserStats(currentStats.copy(fatigue = (currentStats.fatigue + 40).coerceAtMost(100)))
                    }
                } else {
                    claimDungeonVictory()
                }
            }
        }
    }

    // Add movement progress inside dimensional gates
    fun executeDungeonWorkoutStep(amount: Int) {
        if (!_dungeonActive.value) return
        _dungeonCaloriesBurned.value = _dungeonCaloriesBurned.value + amount
        
        if (_dungeonCaloriesBurned.value >= _dungeonTargetCalories.value) {
            claimDungeonVictory()
        }
    }

    private fun claimDungeonVictory() {
        _dungeonActive.value = false
        dungeonJob?.cancel()

        val isHighRank = _dungeonName.value.contains("S-Rank")
        val mins = if (isHighRank) 10 else 3
        val cals = _dungeonCaloriesBurned.value

        viewModelScope.launch {
            repository.claimDungeonClear(
                dungeonName = _dungeonName.value,
                durationMin = mins,
                calories = cals
            )
            _systemResponse.value = "[CONGRATULATIONS: DUNGEON CLEARED!]\n\n" +
                    "\"You defeated the boss of the gate. Massive EXP and Gold coordinates have been synced to your user interface! Level points have expanded.\""
        }
    }

    fun exitDungeonPrematurely() {
        _dungeonActive.value = false
        dungeonJob?.cancel()
        _systemResponse.value = "[DUNGEON ESCAPE: TELEPORTED OUT]\n\n" +
                "\"You returned through the gateway gate safely, but forfeit all exp rewards. High penalty tax has been levied.\""
    }

    // Penalty desert simulation
    fun triggerPenaltyZone() {
        _penaltyActive.value = true
        _penaltySecondsLeft.value = 180 // 3 minutes survival
        _systemResponse.value = "[WARNING: PENALTY QUEST COMMENCED]\n\n" +
                "\"Penalty: Surviving the Desert of Giants. Run/Step rapidly! Keep moving to maintain thermal cooling modules. Fall of your own peril.\""

        penaltyJob?.cancel()
        penaltyJob = viewModelScope.launch {
            while (_penaltySecondsLeft.value > 0 && _penaltyActive.value) {
                delay(1000)
                _penaltySecondsLeft.value = _penaltySecondsLeft.value - 1
            }
            if (_penaltyActive.value) {
                _penaltyActive.value = false
                // Survived! Award minor rewards
                repository.addExperienceAndGold(expToGain = 20, goldToGain = 50, customFatigue = 60)
                _systemResponse.value = "[SUCCESS: PENALTY QUEST COMPLETED]\n\n" +
                        "\"You successfully survived the Desert of Giants under hostile climate. Teleporting back to home. Gained 20 EXP.\""
            }
        }
    }

    fun completePenaltyStep(stepAmount: Int) {
        if (!_penaltyActive.value) return
        // Keep them active. Reduces penalty seconds faster as they step!
        _penaltySecondsLeft.value = (_penaltySecondsLeft.value - stepAmount).coerceAtLeast(0)
        if (_penaltySecondsLeft.value <= 0) {
            _penaltyActive.value = false
            penaltyJob?.cancel()
            viewModelScope.launch {
                repository.addExperienceAndGold(expToGain = 25, goldToGain = 60, customFatigue = 50)
                _systemResponse.value = "[PENALTY TERMINATED: SURVIVAL ASSURED]\n\n" +
                        "\"Physical tracking coordinates recovered. You survived today's failure penalties and forged deep thermal endurance!\""
            }
        }
    }

    // Call server-side Gemini Model to ask Solo Leveling Admin System Trainer for advice!
    fun askSystemAdmin(userPrompt: String) {
        if (userPrompt.trim().isEmpty()) return
        _isSystemLoading.value = true
        _systemResponse.value = "[TRANSMITTING ENCRYPTED LINK TO CENTRAL COMMAND...]"

        val stats = userStatsState.value
        val todayQuest = todayQuestState.value

        val statsInfo = stats?.let {
            "Level: ${it.level}, Stats -> STR: ${it.strength} AGI: ${it.agility} VIT: ${it.vitality} INT: ${it.intelligence}, Fatigue: ${it.fatigue}%, Weight: ${it.weightKg}kg/Target: ${it.targetWeightKg}kg"
        } ?: "No data"

        val questInfo = todayQuest?.let {
            "Daily Quest Date: ${it.dateString}, Pushups: ${it.pushupsProgress}/${it.pushupsGoal}, Situps: ${it.situpsProgress}/${it.situpsGoal}, Squats: ${it.squatsProgress}/${it.squatsGoal}, Running: ${it.runningProgressMeters}/${it.runningGoalMeters} meters"
        } ?: "No active logs available"

        val finalStatsPayload = "$statsInfo | Today Quests Status: $questInfo"

        viewModelScope.launch {
            try {
                val advice = GeminiRetrofitClient.getSystemAdvice(finalStatsPayload, userPrompt)
                _systemResponse.value = advice
            } catch (e: Exception) {
                _systemResponse.value = "[LINK BREAK: RETRANSMISSION FAILURE]\n\n\"Secure channel broke down: ${e.localizedMessage}. Verify system configurations and enter keys inside secrets.\""
            } finally {
                _isSystemLoading.value = false
            }
        }
    }
}
