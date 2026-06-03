package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyQuest
import com.example.data.InventoryItem
import com.example.data.UserStats
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestDashboard(
    viewModel: QuestViewModel,
    modifier: Modifier = Modifier
) {
    val userStats by viewModel.userStatsState.collectAsStateWithLifecycle()
    val todayQuest by viewModel.todayQuestState.collectAsStateWithLifecycle()
    val workoutLogs by viewModel.workoutLogsState.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItemsState.collectAsStateWithLifecycle()
    val totalCaloriesToday by viewModel.totalCaloriesToday.collectAsStateWithLifecycle()

    val dungeonActive by viewModel.dungeonActive.collectAsStateWithLifecycle()
    val penaltyActive by viewModel.penaltyActive.collectAsStateWithLifecycle()
    val showLevelUp by viewModel.showLevelUpDialog.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SystemDarkBlack
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Ambient holographic gradient glows on the corners
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Top-centered cyan halo (radial_gradient(circle_at_50%_-20%,rgba(6,182,212,0.15),transparent_60%))
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(SystemHoloCyan.copy(alpha = 0.15f), Color.Transparent),
                                radius = size.width * 0.9f,
                                center = androidx.compose.ui.geometry.Offset(size.width / 2f, -size.height * 0.1f)
                            ),
                            radius = size.width * 0.9f,
                            center = androidx.compose.ui.geometry.Offset(size.width / 2f, -size.height * 0.1f)
                        )
                        // Soft purple subtle counter-weight at bottom right
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(SystemRunicPurple.copy(alpha = 0.05f), Color.Transparent),
                                radius = size.width * 0.7f,
                                center = androidx.compose.ui.geometry.Offset(size.width, size.height)
                            ),
                            radius = size.width * 0.7f,
                            center = androidx.compose.ui.geometry.Offset(size.width, size.height)
                        )
                    }
            )

            // Primary Content layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Main Header bar with system state titles
                SystemHeaderSection(userStats = userStats, onRestClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.triggerRest()
                })

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Indicators panel (HP, MP , Fatigue, Calories)
                StatsStatusBarsPanel(
                    userStats = userStats,
                    totalCalories = totalCaloriesToday,
                    onRestFullClick = {
                        // Use low potion if available
                        viewModel.consumeItem("potion_low")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Main navigation layout: Status | Daily Quests | Inventory | AI Console
                var activeTab by remember { mutableStateOf(0) }
                val tabs = listOf("STATUS", "DAILY QUEST", "INVENTORY", "ADMIN TERMINAL")

                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = SystemHoloCyan,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = activeTab == index,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeTab = index
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (activeTab == index) SystemHoloCyan else SystemTextMuted
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Body content representing screens
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (activeTab) {
                        0 -> StatusStatsScreen(
                            userStats = userStats,
                            viewModel = viewModel,
                            haptic = haptic
                        )
                        1 -> DailyQuestScreen(
                            quest = todayQuest,
                            viewModel = viewModel,
                            haptic = haptic
                        )
                        2 -> ShopAndInventoryScreen(
                            inventoryItems = inventoryItems,
                            userStats = userStats,
                            viewModel = viewModel,
                            haptic = haptic
                        )
                        3 -> AdminTerminalScreen(
                            viewModel = viewModel,
                            haptic = haptic
                        )
                    }
                }
            }

            // DUNGEON RAID OVERLAY
            if (dungeonActive) {
                DungeonActiveOverlay(
                    dungeonName = viewModel.dungeonName.collectAsStateWithLifecycle().value,
                    timeLeft = viewModel.dungeonSecondsLeft.collectAsStateWithLifecycle().value,
                    caloriesEarned = viewModel.dungeonCaloriesBurned.collectAsStateWithLifecycle().value,
                    targetCalories = viewModel.dungeonTargetCalories.collectAsStateWithLifecycle().value,
                    onExecuteStep = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.executeDungeonWorkoutStep(1) // add step
                    },
                    onFlee = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.exitDungeonPrematurely()
                    }
                )
            }

            // PENALTY SURVIVAL OVERLAY
            if (penaltyActive) {
                PenaltyActiveOverlay(
                    timeLeft = viewModel.penaltySecondsLeft.collectAsStateWithLifecycle().value,
                    onExecuteStep = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.completePenaltyStep(1)
                    }
                )
            }

            // LEVEL UP DIALOG POPUP
            if (showLevelUp != null) {
                LevelUpDialog(
                    newLevel = showLevelUp ?: 1,
                    onDismiss = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.dismissLevelUpDialog()
                    }
                )
            }
        }
    }
}

// ---------------- UI SECTIONS ---------------- //

@Composable
fun SystemHeaderSection(
    userStats: UserStats?,
    onRestClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = (userStats?.titleName ?: "THE AWAKENED").uppercase(),
                color = SystemGoldAccent,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = userStats?.name ?: "HUNTER",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Level Indicator Pill
                Box(
                    modifier = Modifier
                        .background(SystemNoxSteel, RoundedCornerShape(4.dp))
                        .border(1.dp, SystemHoloCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LV. ${userStats?.level ?: 1}",
                        color = SystemHoloCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Quick rest button
        IconButton(
            onClick = onRestClick,
            modifier = Modifier
                .border(1.dp, SystemHoloCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .background(SystemOffBlack)
                .size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Rest Daily",
                tint = SystemHoloCyan
            )
        }
    }
}

@Composable
fun StatsStatusBarsPanel(
    userStats: UserStats?,
    totalCalories: Int,
    onRestFullClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SystemOffBlack, RoundedCornerShape(12.dp))
            .border(1.dp, SystemHoloCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // HP Indicators
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "HP",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = SystemWarningRed,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${userStats?.hpCurrent ?: 100} / ${userStats?.hpMax ?: 100}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = SystemWarningRed
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { 
                        val curr = userStats?.hpCurrent ?: 100
                        val m = userStats?.hpMax ?: 100
                        (curr.toFloat() / m.toFloat()).coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = SystemWarningRed,
                    trackColor = SystemNoxSteel
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Fatigue Indicator
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "FATIGUE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if ((userStats?.fatigue ?: 0) >= 75) SystemWarningRed else SystemHoloCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${userStats?.fatigue ?: 0}%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if ((userStats?.fatigue ?: 0) >= 75) SystemWarningRed else SystemHoloCyan
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { ((userStats?.fatigue ?: 0).toFloat() / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if ((userStats?.fatigue ?: 0) >= 75) SystemWarningRed else SystemHoloCyan,
                    trackColor = SystemNoxSteel
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Level up EXP Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "EXP LEVEL",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = SystemRunicPurple,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${userStats?.exp ?: 0} %",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = SystemRunicPurple
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { ((userStats?.exp ?: 0).toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = SystemRunicPurple,
                trackColor = SystemNoxSteel
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // System information counters: Rank details and logged Calories today
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rank Class",
                    tint = SystemGoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RANK CLASS: ${userStats?.className ?: "E-Rank Player"}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = SystemGoldAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Calories Burned",
                    tint = SystemHoloCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "BURNED TODAY: $totalCalories KCAL",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = SystemHoloCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------------- TAB 1: STATUS STATS PANEL ---------------- //

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusStatsScreen(
    userStats: UserStats?,
    viewModel: QuestViewModel,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    var editWeightOpen by remember { mutableStateOf(false) }
    val workoutLogs by viewModel.workoutLogsState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core status points and gold metric cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Gold card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(SystemOffBlack, RoundedCornerShape(12.dp))
                        .border(1.dp, SystemGoldAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SYSTEM GOLD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = SystemTextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Gold Coins",
                            tint = SystemGoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${userStats?.gold ?: 0}G",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = SystemGoldAccent
                        )
                    }
                }

                // Available points card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(SystemOffBlack, RoundedCornerShape(12.dp))
                        .border(1.dp, SystemHoloCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "STATUS POINTS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = SystemTextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${userStats?.statusPoints ?: 0} PTS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = SystemHoloCyan
                    )
                }
            }
        }

        // Stats upgrade list panel
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SystemOffBlack, RoundedCornerShape(12.dp))
                    .border(1.dp, SystemHoloCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "[ALLOCATE STATUS ATTRIBUTES]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = SystemHoloCyan,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val attributes = listOf(
                    Triple("STRENGTH", userStats?.strength ?: 10, "Empowers structural physical lifting, increases overall HP max levels."),
                    Triple("AGILITY", userStats?.agility ?: 10, "Increases speed, stamina, metabolic calorie multiplier rates."),
                    Triple("VITALITY", userStats?.vitality ?: 10, "Improves posture durability and boosts natural stamina capacity."),
                    Triple("INTELLIGENCE", userStats?.intelligence ?: 10, "Sharpens mental grit details, maximizes System MP parameters."),
                    Triple("SENSE", userStats?.sense ?: 10, "Improves overall athletic coordination and fat depletion awareness.")
                )

                attributes.forEach { (name, value, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = desc,
                                fontSize = 10.sp,
                                color = SystemTextMuted,
                                lineHeight = 12.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$value",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SystemHoloCyan
                            )

                            IconButton(
                                onClick = {
                                    if ((userStats?.statusPoints ?: 0) > 0) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.upgradeStat(name)
                                    }
                                },
                                enabled = (userStats?.statusPoints ?: 0) > 0,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if ((userStats?.statusPoints ?: 0) > 0) SystemHoloCyan else SystemNoxSteel,
                                        RoundedCornerShape(6.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Upgrade Attribute",
                                    tint = if ((userStats?.statusPoints ?: 0) > 0) SystemDarkBlack else SystemTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = SystemNoxSteel.copy(alpha = 0.5f))
                }
            }
        }

        // Physical parameter settings: Fat loss / Weight tracker! Satisfies "mote log apna fat lose kar sake"
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SystemOffBlack, RoundedCornerShape(12.dp))
                    .border(1.dp, SystemRunicPurple.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[FAT LOSS METRICS TRACKER]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = SystemRunicPurple,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Button(
                        onClick = { editWeightOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SystemRunicPurple),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("UPDATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current Weight", fontSize = 10.sp, color = SystemTextMuted)
                        Text(
                            text = "${userStats?.weightKg ?: 85.0} kg",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = SystemTextLight
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Target Weight", fontSize = 10.sp, color = SystemTextMuted)
                        Text(
                            text = "${userStats?.targetWeightKg ?: 70.0} kg",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = SystemHoloCyan
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Est. Fat Remaining", fontSize = 10.sp, color = SystemTextMuted)
                        val curr = userStats?.weightKg ?: 85.0
                        val target = userStats?.targetWeightKg ?: 70.0
                        val diff = (curr - target).coerceAtLeast(0.0)
                        Text(
                            text = String.format("%.1f kg", diff),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (diff > 5) SystemWarningRed else SystemHoloCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress towards target weight
                val currentWeight = userStats?.weightKg ?: 85.0
                val targetWeight = userStats?.targetWeightKg ?: 70.0
                val heightCm = userStats?.heightCm ?: 175.0
                val heightM = heightCm / 100.0
                val bmi = currentWeight / (heightM * heightM)

                val weightDiffPct = if (currentWeight > targetWeight) {
                    val initialStartDiff = 20.0 // assume max 20kg loss range
                    val completedLoss = (initialStartDiff - (currentWeight - targetWeight)).coerceAtLeast(0.0)
                    (completedLoss / initialStartDiff).coerceIn(0.0, 1.0).toFloat()
                } else {
                    1f
                }

                LinearProgressIndicator(
                    progress = { weightDiffPct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = SystemRunicPurple,
                    trackColor = SystemNoxSteel
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = String.format("Current Body BMI: %.1f (${if (bmi > 25) "Fat / Overweight Range" else "Normal Fit Range"})", bmi),
                    fontSize = 11.sp,
                    color = if (bmi > 25) SystemWarningRed else SystemHoloCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // Workouts history logs
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SystemOffBlack, RoundedCornerShape(12.dp))
                    .border(1.dp, SystemNoxSteel, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "[SOLO LEVELING WAR HISTORY]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = SystemTextLight,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (workoutLogs.isEmpty()) {
                    Text(
                        text = "System vacant. Your journey hasn't recorded calories depleting actions yet.",
                        fontSize = 11.sp,
                        color = SystemTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    for (log in workoutLogs.take(5)) {
                        val dateString = android.icu.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(log.dateLong))
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.type,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SystemHoloCyan
                                )
                                Text(
                                    text = dateString,
                                    fontSize = 10.sp,
                                    color = SystemTextMuted
                                )
                            }
                            Text(
                                text = "${log.summary}. Exp gained: +${log.expEarned} XP, Gold: +${log.goldEarned}G",
                                fontSize = 11.sp,
                                color = SystemTextLight
                            )
                        }
                        HorizontalDivider(color = SystemNoxSteel.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }

    // Weight update popup
    if (editWeightOpen) {
        Dialog(onDismissRequest = { editWeightOpen = false }) {
            var currentEntered by remember { mutableStateOf(userStats?.weightKg?.toString() ?: "85.0") }
            var targetEntered by remember { mutableStateOf(userStats?.targetWeightKg?.toString() ?: "70.0") }
            var heightEntered by remember { mutableStateOf(userStats?.heightCm?.toString() ?: "175.0") }

            Column(
                modifier = Modifier
                    .background(SystemDarkBlack, RoundedCornerShape(16.dp))
                    .border(2.dp, SystemHoloCyan, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "[RECALIBRATE BODY QUANTITIES]",
                    color = SystemHoloCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = currentEntered,
                    onValueChange = { currentEntered = it },
                    label = { Text("Current Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("85.0") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemHoloCyan,
                        focusedLabelColor = SystemHoloCyan,
                        cursorColor = SystemHoloCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetEntered,
                    onValueChange = { targetEntered = it },
                    label = { Text("Target Goal Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("70.0") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemHoloCyan,
                        focusedLabelColor = SystemHoloCyan,
                        cursorColor = SystemHoloCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = heightEntered,
                    onValueChange = { heightEntered = it },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("175.0") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemHoloCyan,
                        focusedLabelColor = SystemHoloCyan,
                        cursorColor = SystemHoloCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { editWeightOpen = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemTextLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL")
                    }

                    Button(
                        onClick = {
                            val c = currentEntered.toDoubleOrNull() ?: 85.0
                            val t = targetEntered.toDoubleOrNull() ?: 70.0
                            val h = heightEntered.toDoubleOrNull() ?: 175.0
                            viewModel.updateWeightMetrics(c, t, h)
                            editWeightOpen = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SystemHoloCyan, contentColor = SystemDarkBlack),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SAVE")
                    }
                }
            }
        }
    }
}

// ---------------- TAB 2: DAILY QUEST SCREEN ---------------- //

@Composable
fun DailyQuestScreen(
    quest: DailyQuest?,
    viewModel: QuestViewModel,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    var pushupsInput by remember { mutableStateOf("") }
    var situpsInput by remember { mutableStateOf("") }
    var squatsInput by remember { mutableStateOf("") }
    var runningInput by remember { mutableStateOf("") }

    var configGoalsOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily Quest Master card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SystemOffBlack, RoundedCornerShape(12.dp))
                    .border(2.dp, SystemHoloCyan, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[DAILY QUEST: PREPARING TO BECOME STRONG]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = SystemHoloCyan,
                        fontWeight = FontWeight.ExtraBold
                    )

                    IconButton(
                        onClick = { configGoalsOpen = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Configure Goals", tint = SystemHoloCyan)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "If you refuse to complete these tasks by midnight, you will be penalized in the desolate Penalty Zone of Giant Centipedes.",
                    fontSize = 11.sp,
                    color = SystemTextMuted,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Checklist targets
                val exercises = listOf(
                    Quadruple("Push-ups", quest?.pushupsProgress ?: 0, quest?.pushupsGoal ?: 100, "reps"),
                    Quadruple("Sit-ups", quest?.situpsProgress ?: 0, quest?.situpsGoal ?: 100, "reps"),
                    Quadruple("Squats", quest?.squatsProgress ?: 0, quest?.squatsGoal ?: 100, "reps"),
                    Quadruple("Running", quest?.runningProgressMeters ?: 0, quest?.runningGoalMeters ?: 10000, "meters")
                )

                exercises.forEach { (name, progress, goal, unit) ->
                    val isDone = progress >= goal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = "Status Icon",
                            tint = if (isDone) SystemHoloCyan else SystemTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = if (isDone) SystemHoloCyan else Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = if (name == "Running") {
                                String.format("%.2f / %.1f km", progress / 1000.0, goal / 1000.0)
                            } else {
                                "$progress / $goal $unit"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (isDone) SystemHoloCyan else SystemTextMuted
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (progress.toFloat() / goal.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (isDone) SystemHoloCyan else SystemElectricBlue,
                        trackColor = SystemNoxSteel
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Check quest success seal
                if (quest?.isCompleted == true) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SystemHoloCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, SystemHoloCyan, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SURVIVAL ASSURED FOR TODAY\n[ALL OBJECTIVES OBTAINED!]",
                            color = SystemHoloCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Action inputs to complete workouts
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SystemOffBlack, RoundedCornerShape(12.dp))
                    .border(1.dp, SystemNoxSteel, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "[RECORD QUEST MILESTONES]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Push-ups Logger
                WorkoutLoggerRow(
                    label = "Push-ups",
                    inputValue = pushupsInput,
                    onValueChange = { pushupsInput = it },
                    onRepLogged = {
                        val amount = pushupsInput.toIntOrNull() ?: 10
                        viewModel.logRepetition("Push-ups", amount)
                        pushupsInput = ""
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    quickActionReps = 10
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sit-ups Logger
                WorkoutLoggerRow(
                    label = "Sit-ups",
                    inputValue = situpsInput,
                    onValueChange = { situpsInput = it },
                    onRepLogged = {
                        val amount = situpsInput.toIntOrNull() ?: 10
                        viewModel.logRepetition("Sit-ups", amount)
                        situpsInput = ""
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    quickActionReps = 10
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Squats Logger
                WorkoutLoggerRow(
                    label = "Squats",
                    inputValue = squatsInput,
                    onValueChange = { squatsInput = it },
                    onRepLogged = {
                        val amount = squatsInput.toIntOrNull() ?: 10
                        viewModel.logRepetition("Squats", amount)
                        squatsInput = ""
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    quickActionReps = 15
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Running Logger
                RunningLoggerRow(
                    label = "Running / Walking",
                    inputValue = runningInput,
                    onValueChange = { runningInput = it },
                    onRepLogged = {
                        val meters = runningInput.toIntOrNull() ?: 1000
                        viewModel.logRunningMeters(meters)
                        runningInput = ""
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
        }

        // Direct Penalty gate triggering button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SystemWarningRed.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .border(1.dp, SystemWarningRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LAZINESS PENALTY ZONE GATEWAY",
                        color = SystemWarningRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Haven't finished the quest and want to challenge your limits or burn quick calories? Voluntarily enter the Desolate Desert of Giants and survive 3-Minutes of intense cardio stepping!",
                        color = SystemTextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.triggerPenaltyZone()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SystemWarningRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ENTER DESERT PENALTY ZONE", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Goal Configuration Drawer Dialog
    if (configGoalsOpen) {
        Dialog(onDismissRequest = { configGoalsOpen = false }) {
            Column(
                modifier = Modifier
                    .background(SystemDarkBlack, RoundedCornerShape(16.dp))
                    .border(2.dp, SystemRunicPurple, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "[SYSTEM OBJECTIVES RECONFIGURATION]",
                    color = SystemRunicPurple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Select your current workout configuration to lose fat or build muscular capability based on requirements:",
                    color = SystemTextLight,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Option 1: Weight Loss Focus
                Button(
                    onClick = {
                        viewModel.setOrientation(true)
                        configGoalsOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemRunicPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("FAT LOSS MODE (HEAVY RUNNING & SQUATS)", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Strength building focus
                Button(
                    onClick = {
                        viewModel.setOrientation(false)
                        configGoalsOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("STRENGTH MODE (HEAVY PUSHUPS/SITUPS)", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { configGoalsOpen = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemTextLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CANCEL")
                }
            }
        }
    }
}

@Composable
fun WorkoutLoggerRow(
    label: String,
    inputValue: String,
    onValueChange: (String) -> Unit,
    onRepLogged: () -> Unit,
    quickActionReps: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.3f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SystemTextLight)
            // Button to quick log 
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        onValueChange(quickActionReps.toString())
                        onRepLogged()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemNoxSteel),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("+$quickActionReps reps", fontSize = 10.sp, color = SystemHoloCyan)
                }
            }
        }

        OutlinedTextField(
            value = inputValue,
            onValueChange = onValueChange,
            placeholder = { Text("QTY", fontSize = 11.sp, color = SystemTextMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onRepLogged() }),
            modifier = Modifier
                .width(75.dp)
                .height(48.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SystemHoloCyan,
                unfocusedBorderColor = SystemNoxSteel,
                cursorColor = SystemHoloCyan
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onRepLogged,
            modifier = Modifier
                .size(40.dp)
                .background(SystemHoloCyan, RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.Add, contentDescription = "Log Reps", tint = SystemDarkBlack)
        }
    }
}

@Composable
fun RunningLoggerRow(
    label: String,
    inputValue: String,
    onValueChange: (String) -> Unit,
    onRepLogged: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.3f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SystemTextLight)
            // Quick preset selectors
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        onValueChange("1000") // 1km
                        onRepLogged()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemNoxSteel),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("+1 km", fontSize = 10.sp, color = SystemHoloCyan)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        onValueChange("2500") // 2.5km
                        onRepLogged()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemNoxSteel),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("+2.5 km", fontSize = 10.sp, color = SystemHoloCyan)
                }
            }
        }

        OutlinedTextField(
            value = inputValue,
            onValueChange = onValueChange,
            placeholder = { Text("Mtrs", fontSize = 11.sp, color = SystemTextMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onRepLogged() }),
            modifier = Modifier
                .width(75.dp)
                .height(48.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SystemHoloCyan,
                unfocusedBorderColor = SystemNoxSteel,
                cursorColor = SystemHoloCyan
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onRepLogged,
            modifier = Modifier
                .size(40.dp)
                .background(SystemHoloCyan, RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.Add, contentDescription = "Log distance", tint = SystemDarkBlack)
        }
    }
}

// ---------------- TAB 3: SHOP & INVENTORY SCREEN ---------------- //

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopAndInventoryScreen(
    inventoryItems: List<InventoryItem>,
    userStats: UserStats?,
    viewModel: QuestViewModel,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Gold balance panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SystemOffBlack),
                border = BorderStroke(1.dp, SystemGoldAccent.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("AVAILABLE SYSTEM COINS", fontSize = 10.sp, color = SystemTextMuted, fontFamily = FontFamily.Monospace)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "Gold Coins", tint = SystemGoldAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${userStats?.gold ?: 0} GOLD",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = SystemGoldAccent
                            )
                        }
                    }
                }
            }
        }

        // Shop items column
        item {
            Text(
                text = "[THE SYSTEM BLACK MARKET STORE]",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = SystemGoldAccent,
                fontWeight = FontWeight.Bold
            )
        }

        items(inventoryItems) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SystemOffBlack, RoundedCornerShape(12.dp))
                    .border(1.dp, SystemNoxSteel, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(SystemNoxSteel, RoundedCornerShape(8.dp))
                        .border(1.dp, if (item.itemType == "KEY") SystemWarningRed else SystemHoloCyan, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.itemType) {
                            "POTION" -> Icons.Default.Favorite
                            "KEY" -> Icons.Default.PlayArrow
                            else -> Icons.Default.ShoppingCart
                        },
                        contentDescription = "Item Icon",
                        tint = if (item.itemType == "KEY") SystemWarningRed else SystemHoloCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(SystemNoxSteel, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "OWNED: ${item.quantity}",
                                fontSize = 9.sp,
                                color = SystemHoloCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = SystemTextMuted,
                        lineHeight = 13.sp
                    )
                    Text(
                        text = "Effect: ${item.effectDescription}",
                        fontSize = 11.sp,
                        color = SystemHoloCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Buy / Action operations
                Column(horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = {
                            viewModel.buyItem(item.itemId) { success ->
                                if (success) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SystemGoldAccent),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "BUY (${item.costInGold}G)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SystemDarkBlack
                        )
                    }

                    if (item.quantity > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (item.itemType == "KEY") {
                                    // Enter dungeon
                                    viewModel.attemptEnterDungeon(item.itemId)
                                } else {
                                    viewModel.consumeItem(item.itemId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (item.itemType == "KEY") SystemWarningRed else SystemHoloCyan
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (item.itemType == "KEY") "ENTER GATE" else "USE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemDarkBlack
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB 4: ADMIN TERMINAL SCREEN ---------------- //

@Composable
fun AdminTerminalScreen(
    viewModel: QuestViewModel,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val systemResponse by viewModel.systemResponse.collectAsStateWithLifecycle()
    val isSystemLoading by viewModel.isSystemLoading.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    var userMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "[SYSTEM INTELLIGENT ADMINISTRATOR TERMINAL]",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = SystemHoloCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Text display window
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, SystemHoloCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SystemDarkBlack)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (isSystemLoading) {
                        CircularProgressIndicator(
                            color = SystemHoloCyan,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = systemResponse,
                        color = if (systemResponse.contains("FAIL") || systemResponse.contains("WARNING")) SystemWarningRed else SystemHoloCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Query fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                placeholder = { Text("Query Administrator (Hinglish/English)...", fontSize = 11.sp, color = SystemTextMuted) },
                maxLines = 2,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = SystemTextLight,
                    focusedBorderColor = SystemHoloCyan,
                    unfocusedBorderColor = SystemNoxSteel,
                    cursorColor = SystemHoloCyan
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (userMessage.trim().isNotEmpty()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        keyboardController?.hide()
                        viewModel.askSystemAdmin(userMessage)
                        userMessage = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(SystemHoloCyan, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Link",
                    tint = SystemDarkBlack
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ---------------- OVERLAY 1: DUNGEON RAID EVENT ---------------- //

@Composable
fun DungeonActiveOverlay(
    dungeonName: String,
    timeLeft: Int,
    caloriesEarned: Int,
    targetCalories: Int,
    onExecuteStep: () -> Unit,
    onFlee: () -> Unit
) {
    val progress = (caloriesEarned.toFloat() / targetCalories.toFloat()).coerceAtLeast(0f)
    val min = timeLeft / 60
    val sec = timeLeft % 60
    val formattedTime = String.format("%02d:%02d", min, sec)

    // Pulse animation for dungeon alerts
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowColor by infiniteTransition.animateColor(
        initialValue = SystemWarningRed.copy(alpha = 0.4f),
        targetValue = SystemWarningRed.copy(alpha = 0.7f),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alert"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SystemDarkBlack.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .border(2.dp, glowColor, RoundedCornerShape(16.dp))
                    .background(SystemOffBlack)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "[GATE RAID IN PROGRESS]",
                        color = SystemWarningRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = dungeonName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "TIME REMAINING: $formattedTime",
                        color = SystemGoldAccent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // BOSS HP BAR (represented by required calorie count!)
                    Text(
                        text = "BOSS HEALTH (KCAL DEFICIT TARGET): $caloriesEarned / $targetCalories KCAL",
                        color = SystemWarningRed,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = SystemWarningRed,
                        trackColor = SystemNoxSteel
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "ACTION: Do quick high knee running, shadow punches or cardio workout steps to damage the boss!",
                        color = SystemTextLight,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Interactive attack button!
                    Button(
                        onClick = onExecuteStep,
                        colors = ButtonDefaults.buttonColors(containerColor = SystemWarningRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Sword Swipes", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXECUTE SHADOW CARDIO STEP (+3 kcal)",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onFlee,
                        border = BorderStroke(1.dp, SystemTextMuted),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemTextMuted)
                    ) {
                        Text("FLEE GATE (Forfeits EXP, incurs tax penalty)", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ---------------- OVERLAY 2: PENALTY QUEST OVERLAY ---------------- //

@Composable
fun PenaltyActiveOverlay(
    timeLeft: Int,
    onExecuteStep: () -> Unit
) {
    val min = timeLeft / 60
    val sec = timeLeft % 60
    val formattedTime = String.format("%02d:%02d", min, sec)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1E0E0B).copy(alpha = 0.98f) // Hostile reddish sandy shade
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .border(2.dp, SystemWarningRed, RoundedCornerShape(16.dp))
                    .background(SystemDarkBlack)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Penalty Warning",
                    tint = SystemWarningRed,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "[PENALTY QUEST: SURVIVE DESERT OF GIANTS]",
                    color = SystemWarningRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "A colossal desert sand centipede will pursue you. Complete HIIT high-intensity cardio strides rapidly to outrun the threat!",
                    color = SystemTextLight,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "TIME TO SURVIVE: $formattedTime",
                    color = SystemGoldAccent,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onExecuteStep,
                    colors = ButtonDefaults.buttonColors(containerColor = SystemGoldAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Text(
                        text = "OUTRUN PURSUING MONSTERS (-2s)",
                        fontWeight = FontWeight.ExtraBold,
                        color = SystemDarkBlack,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ---------------- DIALOG 3: LEVEL UP DIALOG ---------------- //

@Composable
fun LevelUpDialog(
    newLevel: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(SystemDarkBlack, RoundedCornerShape(16.dp))
                .border(2.dp, SystemHoloCyan, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LEVEL UP!",
                color = SystemHoloCyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You have surpassed your structural threshold! Power levels increased.",
                color = SystemTextLight,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(SystemOffBlack, RoundedCornerShape(50.dp))
                    .border(2.dp, SystemHoloCyan, RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LV. $newLevel",
                    fontSize = 24.sp,
                    color = SystemHoloCyan,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "[REWARDS CONFERRED]\n+5 Status Points Added\nFull HP and MP Restoration\nStatus Rating Recalibrated!",
                color = SystemGoldAccent,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SystemHoloCyan, contentColor = SystemDarkBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CLAIM STATUS INCREASE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Helper data classes
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
