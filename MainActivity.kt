package com.example.yourhour

import android.app.AppOpsManager
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.CalendarMonth
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.compose.foundation.clickable
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import androidx.compose.foundation.isSystemInDarkTheme
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.example.yourhour.ui.theme.YourHourTheme
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class AppUsageInfo(
    val appName: String,
    val packageName: String,
    val usageTime: Long
)

sealed class Screen {
    object Home : Screen()
    object Stats : Screen()
    object Goals : Screen()
    object Focus : Screen()
    object Life : Screen()
    object Habits : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YourHourTheme {
                YourHourApp()
            }
        }
    }
}

@Composable
fun YourHourApp() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(checkPermission(context)) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var appUsageList by remember { mutableStateOf<List<AppUsageInfo>>(emptyList()) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            appUsageList = getUsageStats(context)
        }
    }
    // Vacation Notice Service start করো
    LaunchedEffect(Unit) {
        val serviceIntent = Intent(context, VacationNoticeService::class.java)
        context.startForegroundService(serviceIntent)
    }

    if (!hasPermission) {
        PermissionScreen(
            onGrantPermission = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                hasPermission = checkPermission(context)
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen is Screen.Home,
                    onClick = { currentScreen = Screen.Home },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Stats,
                    onClick = { currentScreen = Screen.Stats },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                    label = { Text("Stats") }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Goals,
                    onClick = { currentScreen = Screen.Goals },
                    icon = { Icon(Icons.Default.Flag, contentDescription = "Goals") },
                    label = { Text("Goals") }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Habits,
                    onClick = { currentScreen = Screen.Habits },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Habits") },
                    label = { Text("Habits") }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Focus,
                    onClick = { currentScreen = Screen.Focus },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Focus") },
                    label = { Text("Focus") }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Life,
                    onClick = { currentScreen = Screen.Life },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Life") },
                    label = { Text("Life") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                is Screen.Home -> HomeScreen(appUsageList)
                is Screen.Stats -> StatsScreen(appUsageList, context)
                is Screen.Goals -> GoalsScreen()
                is Screen.Habits -> HabitsScreen()
                is Screen.Focus -> FocusScreen()
                is Screen.Life -> LifeScreen()
            }
        }
    }
}

@Composable
fun PermissionScreen(onGrantPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📱", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Welcome to YourHour", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "YourHour needs permission to track your screen time and help you live more intentionally.",
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGrantPermission,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Grant Permission", fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(appUsageList: List<AppUsageInfo>) {
    val totalTime = appUsageList.sumOf { it.usageTime }
    val top3 = appUsageList.take(3)
    val context = LocalContext.current

    val weeklyData = remember { getWeeklyUsage(context) }
    val avgHours = if (weeklyData.isNotEmpty())
        weeklyData.map { it.second }.average().toFloat() else 0f
    val todayHours = TimeUnit.MILLISECONDS.toHours(totalTime).toFloat()
    val isAboveAverage = todayHours > avgHours

    val darkBg = androidx.compose.ui.graphics.Color(0xFF0F0F1A)
    val cardBg = androidx.compose.ui.graphics.Color(0xFF1A1A2E)
    val accentPurple = androidx.compose.ui.graphics.Color(0xFF7C5CBF)
    val accentRed = androidx.compose.ui.graphics.Color(0xFFE53935)

    var selectedDayIndex by remember { mutableStateOf(weeklyData.size - 1) }
    var selectedDayHours by remember { mutableStateOf(todayHours) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "YourHour",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = accentPurple
                    )
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBg
                )
            )
        },
        containerColor = darkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "TODAY'S SCREEN TIME",
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            val hours = TimeUnit.MILLISECONDS.toHours(totalTime)
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTime) % 60
                            Text(
                                "${hours}h",
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            Text(
                                " ${minutes}m",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Text(
                            "\"You will never have this day again.\"",
                            fontSize = 12.sp,
                            color = androidx.compose.ui.graphics.Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (avgHours > 0) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isAboveAverage)
                                            accentRed.copy(alpha = 0.2f)
                                        else
                                            accentPurple.copy(alpha = 0.2f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    if (isAboveAverage)
                                        "⚠️ Above daily average · ${avgHours.toInt()}h avg"
                                    else
                                        "✅ Below daily average · ${avgHours.toInt()}h avg",
                                    fontSize = 11.sp,
                                    color = if (isAboveAverage) accentRed else accentPurple
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Selected day info
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = accentPurple.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    weeklyData.getOrNull(selectedDayIndex)?.first ?: "Today",
                                    fontSize = 13.sp,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${selectedDayHours.toInt()}h " +
                                            "${((selectedDayHours % 1) * 60).toInt()}m",
                                    fontSize = 13.sp,
                                    color = accentPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Weekly bars
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val days = listOf("S", "M", "T", "W", "T", "F", "S")
                            val maxHours = weeklyData.maxOfOrNull { it.second } ?: 1f
                            weeklyData.takeLast(7).forEachIndexed { index, pair ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.clickable {
                                        selectedDayIndex = index
                                        selectedDayHours = pair.second
                                    }
                                ) {
                                    if (selectedDayIndex == index) {
                                        Text(
                                            "${pair.second.toInt()}h",
                                            fontSize = 9.sp,
                                            color = accentPurple
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val barHeight = ((pair.second / maxHours) * 48).dp
                                    val isSelected = selectedDayIndex == index
                                    val isToday = index == weeklyData.size - 1
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(barHeight.coerceAtLeast(4.dp))
                                            .background(
                                                when {
                                                    isSelected -> accentPurple
                                                    isToday -> accentRed
                                                    else -> accentPurple.copy(alpha = 0.4f)
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        days[index],
                                        fontSize = 10.sp,
                                        color = when {
                                            isSelected -> accentPurple
                                            isToday -> androidx.compose.ui.graphics.Color.White
                                            else -> androidx.compose.ui.graphics.Color.Gray
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold
                                        else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🏆 Top Apps Today",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Text("See all →", fontSize = 13.sp, color = accentPurple)
                }
            }

            items(top3) { app ->
                DarkAppCard(
                    app = app,
                    totalTime = totalTime,
                    cardBg = cardBg,
                    accentPurple = accentPurple
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📱 All Apps",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Text("View all →", fontSize = 13.sp, color = accentPurple)
                }
            }

            items(appUsageList.drop(3)) { app ->
                DarkAppCard(
                    app = app,
                    totalTime = totalTime,
                    cardBg = cardBg,
                    accentPurple = accentPurple
                )
            }
        }
    }
}

@Composable
fun DarkAppCard(
    app: AppUsageInfo,
    totalTime: Long,
    cardBg: androidx.compose.ui.graphics.Color,
    accentPurple: androidx.compose.ui.graphics.Color
) {
    val percentage = if (totalTime > 0) (app.usageTime.toFloat() / totalTime) else 0f
    val context = LocalContext.current

    // Color based on usage
    val barColor = when {
        percentage > 0.4f -> androidx.compose.ui.graphics.Color(0xFFE53935)
        percentage > 0.2f -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        else -> accentPurple
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(barColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))

            // App icon
            val icon = remember(app.packageName) {
                try {
                    context.packageManager.getApplicationIcon(app.packageName)
                } catch (e: Exception) { null }
            }
            if (icon != null) {
                Image(
                    bitmap = icon.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            accentPurple.copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        app.appName.take(1),
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        app.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatTime(app.usageTime),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = barColor,
                    trackColor = androidx.compose.ui.graphics.Color(0xFF2A2A4A)
                )
            }
        }
    }
}

@Composable
fun AppUsageCard(app: AppUsageInfo, totalTime: Long) {
    val percentage = if (totalTime > 0) (app.usageTime.toFloat() / totalTime) else 0f
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = remember(app.packageName) {
                try {
                    context.packageManager.getApplicationIcon(app.packageName)
                } catch (e: Exception) { null }
            }
            if (icon != null) {
                Image(
                    bitmap = icon.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.appName.take(1), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        app.appName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatTime(app.usageTime),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(appUsageList: List<AppUsageInfo>, context: Context) {

    val weeklyData = getWeeklyUsage(context)
    val monthlyData = getMonthlyUsage(context)
    val goalManager = remember { GoalManager(context) }
    var goalHours by remember { mutableStateOf(4L) }

    LaunchedEffect(Unit) {
        goalManager.dailyGoalFlow.collect { goalHours = it }
    }

    val totalTime = appUsageList.sumOf { it.usageTime }
    val mostUsedApp = appUsageList.firstOrNull()
    val yearlyMillis = getYearlyUsage(context)
    val yearlyHours = TimeUnit.MILLISECONDS.toHours(yearlyMillis)
    val yearlyDays = yearlyHours / 24
    val remainingHours = yearlyHours % 24

    val darkBg = androidx.compose.ui.graphics.Color(0xFF0F0F1A)
    val cardBg = androidx.compose.ui.graphics.Color(0xFF1A1A2E)
    val accentPurple = androidx.compose.ui.graphics.Color(0xFF7C5CBF)
    val accentRed = androidx.compose.ui.graphics.Color(0xFFE53935)
    val accentGreen = androidx.compose.ui.graphics.Color(0xFF4CAF50)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Statistics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                },
                actions = {
                    // Calendar icon
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .background(
                                accentPurple.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Calendar",
                            tint = accentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBg
                )
            )
        },
        containerColor = darkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Today Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "TODAY'S SUMMARY",
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Total time
                            Column {
                                Text(
                                    "Total Time",
                                    fontSize = 11.sp,
                                    color = androidx.compose.ui.graphics.Color.Gray
                                )
                                Text(
                                    formatTime(totalTime),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                            }
                            // Most used
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Most Used",
                                    fontSize = 11.sp,
                                    color = androidx.compose.ui.graphics.Color.Gray
                                )
                                Text(
                                    mostUsedApp?.appName?.take(10) ?: "N/A",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentPurple
                                )
                            }
                            // Apps used
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Apps Used",
                                    fontSize = 11.sp,
                                    color = androidx.compose.ui.graphics.Color.Gray
                                )
                                Text(
                                    "${appUsageList.size}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                            }
                        }

                        // Goal exceeded warning
                        if (TimeUnit.MILLISECONDS.toHours(totalTime) > goalHours) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        accentRed.copy(alpha = 0.15f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "⚠️ Daily goal exceeded! Goal: ${goalHours}h",
                                    fontSize = 12.sp,
                                    color = accentRed
                                )
                            }
                        }
                    }
                }
            }

            // Weekly Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🗓️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "This Week",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        DarkBarChart(
                            data = weeklyData,
                            accentPurple = accentPurple,
                            accentRed = accentRed
                        )
                    }
                }
            }

            // Monthly Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🗓️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "This Month",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        DarkBarChart(
                            data = monthlyData,
                            accentPurple = accentPurple,
                            accentRed = accentRed
                        )
                    }
                }
            }

            // Yearly Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = accentPurple.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📅", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "This Year",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "You spent ",
                            fontSize = 15.sp,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                        Row {
                            Text(
                                "$yearlyDays days & $remainingHours hours",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentPurple
                            )
                            Text(
                                " on your phone!",
                                fontSize = 15.sp,
                                color = androidx.compose.ui.graphics.Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Total: $yearlyHours hours",
                            fontSize = 13.sp,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                    }
                }
            }

            // Today's Breakdown
            item {
                val breakdown = getDayBreakdown(context)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Today's Breakdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DarkBreakdownItem(
                                "🌅", "Morning",
                                breakdown[0], accentPurple
                            )
                            DarkBreakdownItem(
                                "☀️", "Afternoon",
                                breakdown[1], accentRed
                            )
                            DarkBreakdownItem(
                                "🌙", "Evening",
                                breakdown[2], accentGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DarkBreakdownItem(
    emoji: String,
    label: String,
    millis: Long,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = androidx.compose.ui.graphics.Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            formatTime(millis),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = color
        )
    }
}

@Composable
fun DarkBarChart(
    data: List<Pair<String, Float>>,
    accentPurple: androidx.compose.ui.graphics.Color,
    accentRed: androidx.compose.ui.graphics.Color
) {
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                val entries = data.mapIndexed { index, pair ->
                    BarEntry(index.toFloat(), pair.second)
                }
                val dataSet = BarDataSet(entries, "Hours").apply {
                    color = android.graphics.Color.parseColor("#7C5CBF")
                    valueTextColor = android.graphics.Color.WHITE
                    valueTextSize = 9f
                    highLightColor = android.graphics.Color.parseColor("#E53935")
                }
                this.data = BarData(dataSet)
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(data.map { it.first })
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    textColor = android.graphics.Color.GRAY
                    setDrawGridLines(false)
                    textSize = 9f
                }
                axisLeft.apply {
                    textColor = android.graphics.Color.GRAY
                    setDrawGridLines(true)
                    gridColor = android.graphics.Color.parseColor("#2A2A4A")
                    textSize = 9f
                }
                axisRight.isEnabled = false
                legend.isEnabled = false
                description.isEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setDrawBorders(false)
                animateY(800)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}


@Composable
fun BreakdownItem(label: String, millis: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(formatTime(millis), fontWeight = FontWeight.Bold, fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun WeeklyBarChart(weeklyData: List<Pair<String, Float>>) {
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                val entries = weeklyData.mapIndexed { index, pair ->
                    BarEntry(index.toFloat(), pair.second)
                }
                val dataSet = BarDataSet(entries, "Hours").apply {
                    color = android.graphics.Color.parseColor("#6650A4")
                    valueTextColor = android.graphics.Color.WHITE
                    valueTextSize = 10f
                }
                data = BarData(dataSet)
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(weeklyData.map { it.first })
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    textColor = android.graphics.Color.WHITE
                    setDrawGridLines(false)
                }
                axisLeft.apply {
                    textColor = android.graphics.Color.WHITE
                    setDrawGridLines(false)
                }
                axisRight.isEnabled = false
                legend.isEnabled = false
                description.isEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                animateY(1000)
            }
        },
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen() {
    val context = LocalContext.current
    val goalManager = remember { GoalManager(context) }
    val limitManager = remember { AppLimitManager(context) }
    val appUsageList = remember { getUsageStats(context) }
    var goalHours by remember { mutableStateOf(4L) }

    LaunchedEffect(Unit) {
        goalManager.dailyGoalFlow.collect {
            goalHours = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goals & Limits", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Daily Goal Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Daily Screen Time Goal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "${goalHours}h",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Slider(
                            value = goalHours.toFloat(),
                            onValueChange = { goalHours = it.toLong() },
                            valueRange = 1f..12f,
                            steps = 10
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                kotlinx.coroutines.CoroutineScope(
                                    kotlinx.coroutines.Dispatchers.IO
                                ).launch {
                                    goalManager.saveDailyGoal(goalHours)
                                }
                            }
                        ) {
                            Text("Save Goal")
                        }
                    }
                }
            }

            // App Limits section
            item {
                Text(
                    "App Limits",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Set daily limits for your apps",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(appUsageList) { app ->
                AppLimitCard(
                    app = app,
                    limitManager = limitManager,
                    context = context
                )
            }
        }
    }
}

@Composable
fun AppLimitCard(
    app: AppUsageInfo,
    limitManager: AppLimitManager,
    context: Context
) {
    var limitMinutes by remember { mutableStateOf(0L) }
    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(app.packageName) {
        limitManager.getLimitFlow(app.packageName).collect {
            limitMinutes = it
        }
    }

    // Check and send alerts
    LaunchedEffect(limitMinutes) {
        if (limitMinutes > 0) {
            val usedMinutes = TimeUnit.MILLISECONDS.toMinutes(app.usageTime)
            val percent = (usedMinutes * 100 / limitMinutes).toInt()
            when {
                percent >= 100 -> NotificationHelper.sendLimitAlert(
                    context, app.appName, 100
                )
                percent >= 80 -> NotificationHelper.sendLimitAlert(
                    context, app.appName, 80
                )
                percent >= 50 -> NotificationHelper.sendLimitAlert(
                    context, app.appName, 50
                )
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text(
                        "Used: ${formatTime(app.usageTime)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (limitMinutes > 0) {
                        Text(
                            "Limit: ${limitMinutes}m",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row {
                    TextButton(onClick = { showDialog = true }) {
                        Text(if (limitMinutes > 0) "Edit" else "Set Limit")
                    }
                    if (limitMinutes > 0) {
                        TextButton(
                            onClick = {
                                kotlinx.coroutines.CoroutineScope(
                                    kotlinx.coroutines.Dispatchers.IO
                                ).launch {
                                    limitManager.clearLimit(app.packageName)
                                }
                            }
                        ) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Progress bar for limit
            if (limitMinutes > 0) {
                val usedMinutes = TimeUnit.MILLISECONDS.toMinutes(app.usageTime)
                val progress = (usedMinutes.toFloat() / limitMinutes).coerceIn(0f, 1f)
                val color = when {
                    progress >= 1f -> MaterialTheme.colorScheme.error
                    progress >= 0.8f -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    progress >= 0.5f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = color
                )
                Text(
                    "${(progress * 100).toInt()}% used",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Set limit dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Set limit for ${app.appName}") },
            text = {
                Column {
                    Text("Enter daily limit in minutes:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Minutes") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minutes = inputText.toLongOrNull() ?: 0L
                        if (minutes > 0) {
                            kotlinx.coroutines.CoroutineScope(
                                kotlinx.coroutines.Dispatchers.IO
                            ).launch {
                                limitManager.saveLimit(app.packageName, minutes)
                                NotificationHelper.createNotificationChannel(context)
                            }
                        }
                        showDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen() {
    val context = LocalContext.current
    val habitManager = remember { HabitManager(context) }
    val totalHabits = 10

    val morningKeys = listOf("wake_up", "fajr_morning", "morning_walk", "read_quran", "plan_day")
    val habitKeys = listOf("prayer", "walk", "read", "code", "journal")
    val allKeys = morningKeys + habitKeys

    val morningItems = listOf(
        Triple("🌅", "Wake up early", "wake_up"),
        Triple("🕌", "Fajr prayer", "fajr_morning"),
        Triple("🚶", "Morning walk", "morning_walk"),
        Triple("📖", "Read Quran", "read_quran"),
        Triple("📋", "Plan the day", "plan_day")
    )

    val habits = listOf(
        Triple("🕌", "Pray", "prayer"),
        Triple("🚶", "Walk", "walk"),
        Triple("📖", "Read", "read"),
        Triple("💻", "Code", "code"),
        Triple("📝", "Journal", "journal")
    )

    val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    // Track each habit's done state
    val doneStates = allKeys.associateWith { key ->
        produceState(initialValue = false, key) {
            habitManager.getHabitFlow(key).collect { value = it }
        }
    }

    val completedCount = doneStates.values.count { it.value }

    val goldColor = androidx.compose.ui.graphics.Color(0xFFD4AF37)
    val darkBg = androidx.compose.ui.graphics.Color(0xFF1A1A2E)
    val cardBg = androidx.compose.ui.graphics.Color(0xFF16213E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "MORNING ROUTINE",
                            fontSize = 10.sp,
                            color = goldColor,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Habits & Prayer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBg,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        },
        containerColor = darkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(60.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { completedCount / totalHabits.toFloat() },
                                modifier = Modifier.size(60.dp),
                                strokeWidth = 4.dp,
                                color = goldColor,
                                trackColor = androidx.compose.ui.graphics.Color(0xFF2A2A4A)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$completedCount/$totalHabits",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = goldColor
                                )
                                Text(
                                    "done",
                                    fontSize = 8.sp,
                                    color = androidx.compose.ui.graphics.Color.Gray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Today's Progress",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { completedCount / totalHabits.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = goldColor,
                                trackColor = androidx.compose.ui.graphics.Color(0xFF2A2A4A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val dayName = java.time.LocalDate.now()
                                .dayOfWeek.name.lowercase()
                                .replaceFirstChar { it.uppercase() }
                            Text(
                                "${(completedCount * 100 / totalHabits)}% completed · $dayName",
                                fontSize = 12.sp,
                                color = androidx.compose.ui.graphics.Color.Gray
                            )
                        }
                    }
                }
            }

            // Prayer Tracker
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🕌 Prayer Tracker",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        Text(
                            "Did you pray today?",
                            fontSize = 12.sp,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            prayers.forEach { prayer ->
                                PrayerItemGold(
                                    prayer = prayer,
                                    habitManager = habitManager,
                                    goldColor = goldColor
                                )
                            }
                        }
                    }
                }
            }

            // Morning Routine
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "☀️ Morning Routine",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }

            items(morningItems) { (emoji, name, key) ->
                LuxuryHabitCard(
                    emoji = emoji,
                    habitName = name,
                    habitKey = key,
                    habitManager = habitManager,
                    goldColor = goldColor,
                    cardBg = cardBg
                )
            }

            // Daily Habits
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "💪 Daily Habits",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }

            items(habits) { (emoji, name, key) ->
                LuxuryHabitCard(
                    emoji = emoji,
                    habitName = name,
                    habitKey = key,
                    habitManager = habitManager,
                    goldColor = goldColor,
                    cardBg = cardBg
                )
            }
        }
    }
}

@Composable
fun LuxuryHabitCard(
    emoji: String,
    habitName: String,
    habitKey: String,
    habitManager: HabitManager,
    goldColor: androidx.compose.ui.graphics.Color,
    cardBg: androidx.compose.ui.graphics.Color
) {
    val scope = rememberCoroutineScope()
    var isDone by remember { mutableStateOf(false) }
    var streak by remember { mutableStateOf(0) }

    LaunchedEffect(habitKey) {
        habitManager.getHabitFlow(habitKey).collect { isDone = it }
    }
    LaunchedEffect(habitKey) {
        habitManager.getStreakFlow(habitKey).collect { streak = it }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone)
                androidx.compose.ui.graphics.Color(0xFF1E2D1E)
            else cardBg
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isDone) goldColor.copy(alpha = 0.2f)
                            else androidx.compose.ui.graphics.Color(0xFF2A2A4A),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isDone) "✅" else emoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        habitName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDone)
                            androidx.compose.ui.graphics.Color.Gray
                        else
                            androidx.compose.ui.graphics.Color.White,
                        style = androidx.compose.ui.text.TextStyle(
                            textDecoration = if (isDone)
                                androidx.compose.ui.text.style.TextDecoration.LineThrough
                            else
                                androidx.compose.ui.text.style.TextDecoration.None,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                androidx.compose.ui.graphics.Color(0xFF2A2A1A),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "🔥 $streak day streak",
                            fontSize = 11.sp,
                            color = goldColor
                        )
                    }
                }
            }

            // Right side — checkbox style button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isDone) goldColor.copy(alpha = 0.3f)
                        else androidx.compose.ui.graphics.Color(0xFF2A2A4A),
                        RoundedCornerShape(8.dp)
                    )
                    .then(
                        // শুধু done না হলেই click করা যাবে
                        if (!isDone) Modifier.clickable {
                            scope.launch {
                                habitManager.markHabitDone(habitKey)
                                habitManager.incrementStreak(habitKey, streak)
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isDone) "✓" else "",
                    fontSize = 18.sp,
                    color = goldColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PrayerItemGold(
    prayer: String,
    habitManager: HabitManager,
    goldColor: androidx.compose.ui.graphics.Color
) {
    val scope = rememberCoroutineScope()
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect(prayer) {
        habitManager.getHabitFlow("prayer_$prayer").collect { isDone = it }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = {
                scope.launch {
                    habitManager.toggleHabit("prayer_$prayer", !isDone)
                }
            },
            modifier = Modifier
                .size(50.dp)
                .background(
                    if (isDone) goldColor.copy(alpha = 0.3f)
                    else androidx.compose.ui.graphics.Color(0xFF2A2A4A),
                    CircleShape
                )
        ) {
            Text(if (isDone) "✅" else "🕌", fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            prayer,
            fontSize = 10.sp,
            color = if (isDone) goldColor
            else androidx.compose.ui.graphics.Color.Gray
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen() {
    val context = LocalContext.current

    // Timer states
    var isWorkSession by remember { mutableStateOf(true) }
    var sessionCount by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }

    val workTime = 25 * 60
    val shortBreak = 5 * 60
    val longBreak = 15 * 60

    var totalTime by remember { mutableStateOf(workTime) }
    var currentTime by remember { mutableStateOf(workTime) }

    // Timer countdown
    LaunchedEffect(isRunning) {
        while (isRunning && currentTime > 0) {
            kotlinx.coroutines.delay(1000)
            currentTime--
        }
        if (currentTime == 0 && isRunning) {
            isRunning = false
            if (isWorkSession) {
                sessionCount++
                NotificationHelper.sendLimitAlert(
                    context,
                    if (sessionCount % 4 == 0) "Time for a long break! 🎉"
                    else "Work session done! Take a break 😊",
                    100
                )
                // Switch to break
                isWorkSession = false
                val breakTime = if (sessionCount % 4 == 0) longBreak else shortBreak
                totalTime = breakTime
                currentTime = breakTime
            } else {
                NotificationHelper.sendLimitAlert(
                    context, "Break over! Time to focus 💪", 80
                )
                // Switch to work
                isWorkSession = true
                totalTime = workTime
                currentTime = workTime
            }
        }
    }

    val progress = currentTime.toFloat() / totalTime.toFloat()
    val minutes = currentTime / 60
    val seconds = currentTime % 60

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Session type indicator
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWorkSession)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (isWorkSession) "🍅 Work Session" else "☕ Break Time",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Session #${sessionCount + 1}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Circular timer
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(250.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(250.dp),
                        strokeWidth = 12.dp,
                        color = if (isWorkSession)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            String.format("%02d:%02d", minutes, seconds),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isWorkSession) "Focus" else "Rest",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Control buttons
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start/Pause
                    Button(
                        onClick = { isRunning = !isRunning },
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text(
                            if (isRunning) "⏸ Pause" else "▶ Start",
                            fontSize = 16.sp
                        )
                    }

                    // Reset
                    OutlinedButton(
                        onClick = {
                            isRunning = false
                            currentTime = totalTime
                        },
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("↺ Reset", fontSize = 16.sp)
                    }
                }
            }

            // Stop Focus Mode button
            item {
                OutlinedButton(
                    onClick = {
                        isRunning = false
                        isWorkSession = true
                        sessionCount = 0
                        totalTime = workTime
                        currentTime = workTime
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("⛔ Stop Focus Mode", fontSize = 16.sp)
                }
            }

            // Session info cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🍅", fontSize = 24.sp)
                            Text(
                                "$sessionCount",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Sessions", fontSize = 12.sp)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⏰", fontSize = 24.sp)
                            Text(
                                "${sessionCount * 25}m",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Focused", fontSize = 12.sp)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎯", fontSize = 24.sp)
                            Text(
                                "${4 - (sessionCount % 4)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("To break", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Pomodoro tips
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🧠 Pomodoro Technique",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Work for 25 minutes", fontSize = 14.sp)
                        Text("• Take a 5 minute break", fontSize = 14.sp)
                        Text("• After 4 sessions → 15 min long break", fontSize = 14.sp)
                        Text("• Repeat and stay focused! 💪", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeScreen() {
    val context = LocalContext.current
    val todoManager = remember { TodoManager(context) }
    val scope = rememberCoroutineScope()

    var todos by remember { mutableStateOf<List<TodoItem>>(emptyList()) }
    var showAddTodo by remember { mutableStateOf(false) }
    var todoInput by remember { mutableStateOf("") }

    // Time picker states
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(0) }
    var selectedAmPm by remember { mutableStateOf("AM") }

    LaunchedEffect(Unit) {
        todoManager.todosFlow.collect { todos = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Life", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ─── QURAN SECTION ───
            item {
                val quranManager = remember { HabitManager(context) }
                var quranPages by remember { mutableStateOf(0) }
                var alreadyReadToday by remember { mutableStateOf(false) }
                var quranStreak by remember { mutableStateOf(0) }

                LaunchedEffect(Unit) {
                    quranManager.getHabitFlow("quran_read_today").collect {
                        alreadyReadToday = it
                    }
                }
                LaunchedEffect(Unit) {
                    quranManager.getStreakFlow("quran_read_today").collect {
                        quranStreak = it
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "📿 Quran Tracker",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Track your daily Quran reading",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "$quranPages pages",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alreadyReadToday)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "🔥 $quranStreak day streak",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (alreadyReadToday) {
                                    Text(
                                        "✅ Counted for today!",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { if (quranPages > 0) quranPages-- },
                                        modifier = Modifier.size(36.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("-", fontSize = 16.sp)
                                    }
                                    Button(
                                        onClick = { quranPages++ },
                                        modifier = Modifier.size(36.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("+", fontSize = 16.sp)
                                    }
                                }
                                // ছোট Done button
                                Button(
                                    onClick = {
                                        if (!alreadyReadToday && quranPages > 0) {
                                            scope.launch {
                                                quranManager.markHabitDone("quran_read_today")
                                                quranManager.incrementStreak(
                                                    "quran_read_today",
                                                    quranStreak
                                                )
                                            }
                                        }
                                    },
                                    enabled = !alreadyReadToday && quranPages > 0,
                                    contentPadding = PaddingValues(
                                        horizontal = 10.dp,
                                        vertical = 4.dp
                                    ),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        if (alreadyReadToday) "Done ✅" else "Mark Read",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── TODO SECTION ───
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "✅ Todo & Reminders",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Button(
                        onClick = { showAddTodo = true },
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        )
                    ) {
                        Text("+ Add Task")
                    }
                }
            }

            if (todos.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📋", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No tasks yet!",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(todos) { todo ->
                TodoCard(
                    todo = todo,
                    onDone = { scope.launch { todoManager.markDone(todo.id) } },
                    onDelete = { scope.launch { todoManager.deleteTodo(todo.id) } }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // ─── ADD TODO DIALOG ───
    if (showAddTodo) {
        AlertDialog(
            onDismissRequest = { showAddTodo = false },
            title = { Text("Add New Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Task title
                    OutlinedTextField(
                        value = todoInput,
                        onValueChange = { todoInput = it },
                        label = { Text("Task title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        "Set Reminder Time:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    // Hour / Minute / AM-PM picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hour", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    selectedHour = if (selectedHour <= 1) 12
                                    else selectedHour - 1
                                }) {
                                    Text("▼", fontSize = 14.sp)
                                }
                                Text(
                                    String.format("%02d", selectedHour),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = {
                                    selectedHour = if (selectedHour >= 12) 1
                                    else selectedHour + 1
                                }) {
                                    Text("▲", fontSize = 14.sp)
                                }
                            }
                        }

                        Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        // Minute
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Min", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    selectedMinute = if (selectedMinute <= 0) 55
                                    else selectedMinute - 5
                                }) {
                                    Text("▼", fontSize = 14.sp)
                                }
                                Text(
                                    String.format("%02d", selectedMinute),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = {
                                    selectedMinute = if (selectedMinute >= 55) 0
                                    else selectedMinute + 5
                                }) {
                                    Text("▲", fontSize = 14.sp)
                                }
                            }
                        }

                        // AM/PM
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("AM/PM", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    selectedAmPm = if (selectedAmPm == "AM") "PM" else "AM"
                                },
                                contentPadding = PaddingValues(
                                    horizontal = 8.dp, vertical = 4.dp
                                )
                            ) {
                                Text(selectedAmPm, fontSize = 16.sp)
                            }
                        }
                    }

                    // Preview
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "⏰ Reminder at: " +
                                        "${String.format("%02d", selectedHour)}:" +
                                        "${String.format("%02d", selectedMinute)} $selectedAmPm",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (todoInput.isNotBlank()) {
                            val timeStr = "${String.format("%02d", selectedHour)}:" +
                                    "${String.format("%02d", selectedMinute)} $selectedAmPm"
                            scope.launch {
                                todoManager.addTodo(todoInput, timeStr)
                            }
                            // Schedule notification
                            scheduleReminder(context, todoInput, selectedHour,
                                selectedMinute, selectedAmPm)
                            todoInput = ""
                            showAddTodo = false
                        }
                    }
                ) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTodo = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun scheduleReminder(
    context: Context,
    title: String,
    hour: Int,
    minute: Int,
    amPm: String
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE)
            as android.app.AlarmManager
    val intent = android.content.Intent(context, ReminderReceiver::class.java).apply {
        putExtra("title", title)
    }
    val pendingIntent = android.app.PendingIntent.getBroadcast(
        context,
        System.currentTimeMillis().toInt(),
        intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                android.app.PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR, hour)
        set(java.util.Calendar.MINUTE, minute)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.AM_PM,
            if (amPm == "AM") java.util.Calendar.AM else java.util.Calendar.PM)
        // যদি time আগে হয়ে গেছে তাহলে পরের দিন
        if (timeInMillis < System.currentTimeMillis()) {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
    }

    try {
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun TodoCard(
    todo: TodoItem,
    onDone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.isDone)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    todo.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    style = androidx.compose.ui.text.TextStyle(
                        textDecoration = if (todo.isDone)
                            androidx.compose.ui.text.style.TextDecoration.LineThrough
                        else
                            androidx.compose.ui.text.style.TextDecoration.None,
                        fontSize = 15.sp
                    ),
                    color = if (todo.isDone)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (todo.reminderTime.isNotBlank()) {
                    Text(
                        "⏰ ${todo.reminderTime}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row {
                if (!todo.isDone) {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Done",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun checkPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun getUsageStats(context: Context): List<AppUsageInfo> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val pm = context.packageManager

    // আজকের শুরু — রাত 12:00 AM
    val startCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // আজকের শেষ — রাত 11:59 PM
    val endCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    val startTime = startCalendar.timeInMillis
    val endTime = endCalendar.timeInMillis

    val stats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY, startTime, endTime
    )

    return stats
        .filter { it.totalTimeInForeground > 0 }
        .map { stat ->
            val appName = try {
                pm.getApplicationLabel(
                    pm.getApplicationInfo(stat.packageName, PackageManager.GET_META_DATA)
                ).toString()
            } catch (e: Exception) {
                stat.packageName
            }
            AppUsageInfo(appName, stat.packageName, stat.totalTimeInForeground)
        }
        .sortedByDescending { it.usageTime }
        .take(20)
}

fun getWeeklyUsage(context: Context): List<Pair<String, Float>> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val result = mutableListOf<Pair<String, Float>>()

    for (i in 6 downTo 0) {
        val startCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -i)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -i)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startCal.timeInMillis,
            endCal.timeInMillis
        )

        val totalHours = stats.sumOf { it.totalTimeInForeground }.toFloat() / (1000 * 60 * 60)

        val dayIndex = (startCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        result.add(Pair(days[dayIndex], totalHours))
    }
    return result
}

fun getMonthlyUsage(context: Context): List<Pair<String, Float>> {

    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val result = mutableListOf<Pair<String, Float>>()

    for (i in 29 downTo 0) {

        val calendar = Calendar.getInstance()

        calendar.add(Calendar.DAY_OF_YEAR, -i)

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)

        val startTime = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)

        val endTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val totalHours =
            stats.sumOf { it.totalTimeInForeground }.toFloat() /
                    (1000 * 60 * 60)

        result.add(
            Pair(
                "${30 - i}",
                totalHours
            )
        )
    }

    return result
}
fun getYearlyUsage(context: Context): Long {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance()
    val endTime = calendar.timeInMillis
    calendar.set(Calendar.DAY_OF_YEAR, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    val startTime = calendar.timeInMillis

    val stats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_YEARLY, startTime, endTime
    )
    return stats.sumOf { it.totalTimeInForeground }
}

fun getDayBreakdown(context: Context): List<Long> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val today = Calendar.getInstance()

    fun getUsageForPeriod(startHour: Int, endHour: Int): Long {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, start.timeInMillis, end.timeInMillis
        )
        return stats.sumOf { it.totalTimeInForeground }
    }

    return listOf(
        getUsageForPeriod(5, 12),   // Morning: 5am - 12pm
        getUsageForPeriod(12, 17),  // Afternoon: 12pm - 5pm
        getUsageForPeriod(17, 23)   // Evening: 5pm - 11pm
    )
}