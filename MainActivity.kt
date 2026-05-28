package com.example.yourhour

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YourHour", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Today's Screen Time", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            formatTime(totalTime),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "You will never have this day again.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text("Top Apps Today", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            items(top3) { app ->
                AppUsageCard(app = app, totalTime = totalTime)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("All Apps", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            items(appUsageList.drop(3)) { app ->
                AppUsageCard(app = app, totalTime = totalTime)
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
    val totalTime = appUsageList.sumOf { it.usageTime }
    val mostUsedApp = appUsageList.firstOrNull()
    val yearlyMillis = getYearlyUsage(context)
    val yearlyHours = TimeUnit.MILLISECONDS.toHours(yearlyMillis)
    val yearlyDays = yearlyHours / 24
    val remainingHours = yearlyHours % 24

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Today summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Today's Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Time", fontSize = 12.sp)
                                Text(formatTime(totalTime), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Most Used", fontSize = 12.sp)
                                Text(
                                    mostUsedApp?.appName?.take(12) ?: "N/A",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Apps Used", fontSize = 12.sp)
                                Text("${appUsageList.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }

            // Weekly chart
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("This Week", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        WeeklyBarChart(weeklyData)
                    }
                }
            }

            // Yearly summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📅 This Year", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "You spent $yearlyDays days & $remainingHours hours on your phone this year!",
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Total: ${yearlyHours} hours",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Morning/Afternoon/Evening breakdown
            item {
                val breakdown = getDayBreakdown(context)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Today's Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            BreakdownItem("🌅 Morning", breakdown[0])
                            BreakdownItem("☀️ Afternoon", breakdown[1])
                            BreakdownItem("🌙 Evening", breakdown[2])
                        }
                    }
                }
            }
        }
    }
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goals", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎯", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Goals Coming Soon!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Habits, Streaks & Prayer Tracker", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen() {
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🍅", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Focus Coming Soon!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Pomodoro Timer & App Limits", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeScreen() {
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("📝", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Life Coming Soon!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Journal, Todo & Quran", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val calendar = Calendar.getInstance()
    val endTime = calendar.timeInMillis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val startTime = calendar.timeInMillis

    val stats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY, startTime, endTime
    )

    val pm = context.packageManager
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
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -i)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val endTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )
        val totalHours = stats.sumOf { it.totalTimeInForeground }.toFloat() /
                (1000 * 60 * 60)

        val dayIndex = (Calendar.getInstance().also {
            it.add(Calendar.DAY_OF_YEAR, -i)
        }.get(Calendar.DAY_OF_WEEK) + 5) % 7

        result.add(Pair(days[dayIndex], totalHours))
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