package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.hoverable
import androidx.compose.ui.draw.clipToBounds
import com.example.data.RoutineEntry
import com.example.ui.RoutineUiState
import com.example.ui.RoutineViewModel
import com.example.ui.SearchMode
import com.example.ui.theme.MyApplicationTheme
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.rotate

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Nightlight

data class PeriodHeader(val num: Int, val name: String, val time: String)

val daysConfig = listOf(2 to "Sunday", 3 to "Monday", 4 to "Tuesday", 5 to "Wednesday", 6 to "Thursday")
val periodsConfig = listOf(
    PeriodHeader(1, "1st Period", "10:26-11:10"),
    PeriodHeader(2, "2nd Period", "11:11-11:50"),
    PeriodHeader(3, "3rd Period", "11:51-12:30"),
    PeriodHeader(4, "4th Period", "12:31-01:10"),
    PeriodHeader(-1, "Tiffin Break", "01:11-02:00"),
    PeriodHeader(5, "5th Period", "02:01-02:40"),
    PeriodHeader(6, "6th Period", "02:41-03:20"),
    PeriodHeader(7, "7th Period", "03:21-04:00")
)

fun parseToMinutesHelper(timeStr: String): Int {
    val parts = timeStr.split(":")
    if (parts.size != 2) return 0
    var h = parts[0].toInt()
    val m = parts[1].toInt()
    if (h in 1..8) h += 12 // Map 01:xx PM to 13:xx
    return h * 60 + m
}

fun getCurrentPeriodNum(): Int {
    val currentTime = Calendar.getInstance()
    val currentMin = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE)
    
    for (p in periodsConfig) {
        if (p.num == -1) continue
        val range = p.time.split("-")
        if (range.size == 2) {
            val startMin = parseToMinutesHelper(range[0])
            val endMin = parseToMinutesHelper(range[1])
            if (currentMin in startMin..endMin) {
                return p.num
            }
        }
    }
    return 1
}

class MainActivity : ComponentActivity() {
    private val viewModel: RoutineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkTheme by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Boolean?>(null) }
            val isDark = darkTheme ?: androidx.compose.foundation.isSystemInDarkTheme()
            MyApplicationTheme(darkTheme = isDark) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RoutineApp(
                        viewModel = viewModel,
                        isDarkTheme = isDark,
                        onThemeToggle = { darkTheme = !isDark },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RoutineApp(
    viewModel: RoutineViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val selectedQuery by viewModel.selectedQuery.collectAsState()

    var previousMode by rememberSaveable { mutableStateOf<String?>(null) }
    var previousQuery by rememberSaveable { mutableStateOf<String?>(null) }

    androidx.activity.compose.BackHandler(enabled = previousMode != null) {
        val modeStr = previousMode
        if (modeStr != null) {
            viewModel.setMode(SearchMode.valueOf(modeStr as String))
            val q = previousQuery as? String
            if (q != null) {
                viewModel.setQuery(q)
            }
            previousMode = null
            previousQuery = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderComponent(
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = searchMode == SearchMode.BY_TEACHER,
                onClick = { viewModel.setMode(SearchMode.BY_TEACHER) },
                label = { Text("By Teacher") },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = searchMode == SearchMode.BY_CLASS,
                onClick = { viewModel.setMode(SearchMode.BY_CLASS) },
                label = { Text("By Class") },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = searchMode == SearchMode.BY_PERIOD,
                onClick = { viewModel.setMode(SearchMode.BY_PERIOD) },
                label = { Text("By Period") }
            )
        }

        when (val state = uiState) {
            is RoutineUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.padding(top = 48.dp))
                Text("Loading Schedule...", modifier = Modifier.padding(top = 16.dp))
            }
            is RoutineUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 48.dp)
                )
                Button(onClick = { viewModel.loadData() }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Retry")
                }
            }
            is RoutineUiState.Success -> {
                val saveableStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
                saveableStateHolder.SaveableStateProvider(searchMode) {
                    if (searchMode == SearchMode.BY_PERIOD) {
                        var selectedDay by rememberSaveable { 
                            val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 1
                            mutableStateOf(if (currentDay in 2..6) currentDay else 2)
                        }
                        var selectedPeriod by rememberSaveable {
                            mutableStateOf(getCurrentPeriodNum().takeIf { it != -1 } ?: 1)
                        }
                        val periodListState = androidx.compose.foundation.lazy.rememberLazyListState()
                        
                        val validPeriods = periodsConfig.filter { it.num != -1 }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                    ) {
                        var dayExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = dayExpanded,
                            onExpandedChange = { dayExpanded = !dayExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = daysConfig.find { it.first == selectedDay }?.second ?: "Day",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = dayExpanded,
                                onDismissRequest = { dayExpanded = false }
                            ) {
                                daysConfig.forEach { (dVal, dName) ->
                                    DropdownMenuItem(
                                        text = { Text(dName) },
                                        onClick = { selectedDay = dVal; dayExpanded = false }
                                    )
                                }
                            }
                        }
                        
                        var periodExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = periodExpanded,
                            onExpandedChange = { periodExpanded = !periodExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = validPeriods.find { it.num == selectedPeriod }?.name ?: "Period",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = periodExpanded,
                                onDismissRequest = { periodExpanded = false }
                            ) {
                                validPeriods.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name) },
                                        onClick = { selectedPeriod = p.num; periodExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                    val periodEntries = state.entries.filter { it.day == selectedDay && it.period == selectedPeriod }.sortedBy { it.className }
                    val allTeachers = viewModel.teachers
                    val busyTeachers = periodEntries.map { it.teacher }.toSet()
                    val freeTeachers = allTeachers.filter { it !in busyTeachers }.sorted()
                    
                    if (periodEntries.isEmpty() && freeTeachers.isEmpty()) {
                        Text(
                            text = "No classes scheduled.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 48.dp)
                        )
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            state = periodListState,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
                        ) {
                            items(periodEntries.size) { index ->
                                val entry = periodEntries[index]
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = entry.className,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = entry.subject,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                        Text(
                                            text = entry.teacher,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable {
                                                previousMode = searchMode.name
                                                previousQuery = selectedQuery
                                                viewModel.setMode(SearchMode.BY_TEACHER)
                                                viewModel.setQuery(entry.teacher)
                                            }.padding(4.dp)
                                        )
                                    }
                                }
                            }
                            if (freeTeachers.isNotEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                                        ) {
                                            Text(
                                                text = "Rest:",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            androidx.compose.foundation.layout.FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                freeTeachers.forEach { teacher ->
                                                    androidx.compose.material3.Surface(
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                                        shadowElevation = 2.dp,
                                                        modifier = Modifier.clickable {
                                                            previousMode = searchMode.name
                                                            previousQuery = selectedQuery
                                                            viewModel.setMode(SearchMode.BY_TEACHER)
                                                            viewModel.setQuery(teacher)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = teacher,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val options = if (searchMode == SearchMode.BY_CLASS) viewModel.classes else viewModel.teachers
                    
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedQuery ?: "Select...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        viewModel.setQuery(option)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    val currentQuery = selectedQuery
                    val isQuerySelected = currentQuery != null
                    androidx.compose.animation.AnimatedContent(
                        targetState = isQuerySelected,
                        transitionSpec = {
                            (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500, delayMillis = 90)) +
                             androidx.compose.animation.scaleIn(initialScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(500)))
                            .togetherWith(
                                 androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(90)) +
                                 androidx.compose.animation.scaleOut(targetScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(90))
                            )
                        },
                        label = "TableFormationAnimation",
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) { hasQuery ->
                        if (hasQuery && currentQuery != null) {
                            val relevantEntries = state.entries.filter { 
                                if (searchMode == SearchMode.BY_CLASS) {
                                    it.className == currentQuery || currentQuery.startsWith(it.className + "-")
                                }
                                else it.teacher == currentQuery
                            }

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val titleText = if (searchMode == SearchMode.BY_CLASS) {
                                    "Class $currentQuery's Schedule"
                                } else {
                                    "$currentQuery's Class Routine"
                                }
                                
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )

                                if (relevantEntries.isEmpty()) {
                                    Text(
                                        text = "No schedule found",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 48.dp),
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    RoutineTableWrapper(
                                        entries = relevantEntries,
                                        mode = searchMode,
                                        query = currentQuery,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                                Text(
                                    text = "Please select ${if (searchMode == SearchMode.BY_CLASS) "a class" else "a teacher"}.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 48.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
fun RoutineTableWrapper(
    entries: List<RoutineEntry>,
    mode: SearchMode,
    query: String,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var initialSetupDone by remember { mutableStateOf(false) }
    var tableSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) {
        val availableWidthScope = maxWidth
        val availableHeightScope = maxHeight
        val availableWidthPx = constraints.maxWidth.toFloat()
        val availableHeightPx = constraints.maxHeight.toFloat()
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(tableSize, availableWidthPx, availableHeightPx) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        scale = (scale * zoom).coerceIn(0.25f, 4f)
                        
                        // Keep the focus on the pinched point
                        val fraction = (scale / oldScale) - 1
                        val zoomOffset = (centroid - offset) * fraction
                        
                        val proposedX = offset.x + pan.x - zoomOffset.x
                        val proposedY = offset.y + pan.y - zoomOffset.y
                        
                        val contentWidth = tableSize.width * scale
                        val contentHeight = tableSize.height * scale
                        
                        val minX = minOf(0f, availableWidthPx - contentWidth)
                        val maxX = 0f
                        val minY = minOf(0f, availableHeightPx - contentHeight)
                        val maxY = 0f

                        offset = Offset(
                            proposedX.coerceIn(minX, maxX),
                            proposedY.coerceIn(minY, maxY)
                        )
                    }
                }
        ) {
            val desiredWidthDp = 900.dp

            if (!initialSetupDone && availableWidthScope.value > 0) {
                val ratio = if (availableWidthScope < desiredWidthDp) {
                    (availableWidthScope.value / desiredWidthDp.value)
                } else {
                    1f
                }
                scale = ratio
                // Start at top left initially
                offset = Offset.Zero
                initialSetupDone = true
            }

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    }
                    .wrapContentSize(unbounded = true, align = Alignment.TopStart)
            ) {
                RoutineTable(
                    entries = entries,
                    mode = mode,
                    query = query,
                    modifier = Modifier
                        .requiredWidth(desiredWidthDp)
                        .onSizeChanged { tableSize = it }
                )
            }
        }
    }
}

@Composable
fun RoutineTable(
    entries: List<RoutineEntry>,
    mode: SearchMode,
    query: String,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while(isActive) {
            delay(10000L) // 10 seconds
            currentTime = Calendar.getInstance()
        }
    }

    fun parseToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        var h = parts[0].toInt()
        val m = parts[1].toInt()
        if (h in 1..8) h += 12 // Map 01:xx PM to 13:xx
        return h * 60 + m
    }

    fun isPeriodActive(timeStr: String): Boolean {
        val range = timeStr.split("-")
        if (range.size != 2) return false
        val startMin = parseToMinutes(range[0])
        val endMin = parseToMinutes(range[1])
        val currentMin = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE)
        return currentMin in startMin..endMin
    }

    fun getRemainingMins(timeStr: String): Int {
        val range = timeStr.split("-")
        if (range.size != 2) return 0
        val endMin = parseToMinutes(range[1])
        val currentMin = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE)
        return maxOf(0, endMin - currentMin)
    }

    fun isDayActive(dayValue: Int): Boolean {
        // Calendar.SUNDAY is 1, Calendar.MONDAY is 2. 
        // Our data uses 2 for Sunday, 3 for Monday
        return (currentTime.get(Calendar.DAY_OF_WEEK) + 1) == dayValue
    }

    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF34495E))
        ) {
            HeaderCell("Day", 100.dp)
            periodsConfig.forEach { p ->
                if (p.num == -1) {
                    HeaderCell("Break", 50.dp, isTiffin = true, isActive = isPeriodActive(p.time)) 
                } else {
                    HeaderCell(p.name, 110.dp, isActive = isPeriodActive(p.time), time = p.time)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // Days Column
            Column {
                daysConfig.forEach { (dayValue, dayStr) ->
                    Cell(dayStr, 100.dp, isDark = true, height = 80.dp, isActive = isDayActive(dayValue))
                }
            }

            periodsConfig.forEach { p ->
                if (p.num == -1) {
                    val activePeriod = isPeriodActive(p.time)
                    val remMins = if (activePeriod) getRemainingMins(p.time) else null
                    TiffinBreakCell(50.dp, 80.dp * 5, isActive = activePeriod, remainingMins = remMins)
                } else {
                    Column {
                        val activePeriod = isPeriodActive(p.time)
                        daysConfig.forEach { (dayValue, _) ->
                            val text = getCellText(entries, dayValue, p.num, mode, query)
                            val activeCell = activePeriod && isDayActive(dayValue)
                            val remMins = if (activeCell) getRemainingMins(p.time) else null
                            Cell(text, 110.dp, isDark = false, height = 80.dp, isActive = activeCell, remainingMins = remMins)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp, isTiffin: Boolean = false, isActive: Boolean = false, time: String? = null) {
    val baseBackgroundColor = when {
        isActive && !isTiffin -> Color(0xFF3498DB) // Blue highlight for active period
        isTiffin -> Color(0xFFE74C3C) // Red for break
        else -> Color(0xFF34495E) // Dark blue default
    }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val targetBackgroundColor = if (isHovered) {
        if (isTiffin) Color(0xFFC0392B)
        else if (isActive && !isTiffin) Color(0xFF2980B9)
        else Color(0xFF2C3E50)
    } else {
        baseBackgroundColor
    }
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 150),
        label = "headerBgColor"
    )

    val textColor = Color.White
    
    Box(
        modifier = Modifier
            .width(width)
            .height(56.dp)
            .background(animatedBackgroundColor)
            .border(if (isActive && !isTiffin) 1.5.dp else 0.5.dp, if (isActive && !isTiffin) Color(0xFF2980B9) else Color(0xFFE0E0E0))
            .hoverable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                softWrap = false
            )
            if (time != null) {
                Text(
                    text = time,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun Cell(text: String, width: androidx.compose.ui.unit.Dp, isDark: Boolean, height: androidx.compose.ui.unit.Dp = 80.dp, isActive: Boolean = false, remainingMins: Int? = null) {
    val baseBackgroundColor = when {
        isDark && isActive -> Color(0xFFC6E0F5) // Highlight day
        isActive -> Color(0xFFEAF2F8) // Highlight cell
        isDark -> Color(0xFFDFE4E8) // Day column background from image
        else -> Color(0xFFFFFFFF) // Standard content background
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val targetBackgroundColor = if (isHovered) {
        if (isDark && isActive) Color(0xFFB5D5EF)
        else if (isActive) Color(0xFFDCEAF5)
        else if (isDark) Color(0xFFD2D8DF)
        else Color(0xFFF5F7F9)
    } else {
        baseBackgroundColor
    }
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 150),
        label = "cellBgColor"
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(animatedBackgroundColor)
            .border(if (isActive) 1.5.dp else 0.5.dp, if (isActive) Color(0xFF3498DB) else Color(0xFFE0E0E0))
            .hoverable(interactionSource = interactionSource)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = text,
                transitionSpec = {
                    if (targetState.isNotBlank() && initialState.isBlank() || initialState == "-") {
                        (androidx.compose.animation.fadeIn(animationSpec = tween(400, delayMillis = 100)) + 
                         androidx.compose.animation.scaleIn(initialScale = 0.5f, animationSpec = tween(400, delayMillis = 100)))
                        .togetherWith(
                            androidx.compose.animation.fadeOut(animationSpec = tween(200)) + 
                            androidx.compose.animation.scaleOut(targetScale = 0.5f, animationSpec = tween(200))
                        )
                    } else if (targetState == "-" || targetState.isBlank()) {
                        androidx.compose.animation.fadeIn(animationSpec = tween(200))
                        .togetherWith(
                            androidx.compose.animation.fadeOut(animationSpec = tween(400)) + 
                            androidx.compose.animation.scaleOut(targetScale = 1.5f, animationSpec = tween(400))
                        )
                    } else {
                         (androidx.compose.animation.slideInVertically(animationSpec = tween(400)) { height -> height } + androidx.compose.animation.fadeIn(animationSpec = tween(400)))
                        .togetherWith(
                             androidx.compose.animation.slideOutVertically(animationSpec = tween(400)) { height -> -height } + androidx.compose.animation.fadeOut(animationSpec = tween(400))
                        )
                    }
                },
                label = "textAnimation"
            ) { currentText ->
                if (currentText.isNotBlank() && currentText != "-") {
                    Text(
                        text = currentText,
                        textAlign = TextAlign.Center,
                        style = if (isDark) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isDark || isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) Color(0xFF2980B9) else Color(0xFF333333),
                        lineHeight = 16.sp,
                        maxLines = if (isDark) 1 else Int.MAX_VALUE,
                        softWrap = !isDark
                    )
                } else {
                    Text(
                        text = "-",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
            }
            if (remainingMins != null && !isDark) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$remainingMins min left",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE74C3C),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TiffinBreakCell(width: androidx.compose.ui.unit.Dp, totalHeight: androidx.compose.ui.unit.Dp, isActive: Boolean = false, remainingMins: Int? = null) {
    val baseBackgroundColor = if (isActive) Color(0xFFFFD1AA) else Color(0xFFFFF0E6)

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val targetBackgroundColor = if (isHovered) {
        if (isActive) Color(0xFFFFC08B)
        else Color(0xFFFFE0CA)
    } else {
        baseBackgroundColor
    }
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 150),
        label = "tiffinBgColor"
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(totalHeight)
            .background(animatedBackgroundColor)
            .border(if (isActive) 1.5.dp else 0.5.dp, if (isActive) Color(0xFFE65100) else Color(0xFFE0E0E0))
            .hoverable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isActive && remainingMins != null) {
                Text(
                    text = "$remainingMins m",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD84315),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                text = "T I F F I N   B R E A K",
                color = Color(0xFFC06030),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .requiredWidth(totalHeight - if (isActive && remainingMins != null) 40.dp else 0.dp)
                    .rotate(-90f),
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun getCellText(
    entries: List<RoutineEntry>,
    day: Int,
    period: Int,
    mode: SearchMode,
    query: String
): String {
    val filtered = entries.filter { it.day == day && it.period == period }
    if (filtered.isEmpty()) return ""
    
    if (mode == SearchMode.BY_CLASS) {
        val relevant = filtered.filter { it.className == query || query.startsWith(it.className + "-") }
        if (relevant.isEmpty()) return ""

        val groupedBySubject = relevant.groupBy { it.subject }
        return groupedBySubject.entries.joinToString("\n") { (subject, refs) ->
            val teachers = refs.map { it.teacher }.distinct().joinToString("/")
            "$subject ($teachers)"
        }
    } else {
        val relevant = filtered.filter { it.teacher == query }
        if (relevant.isEmpty()) return ""

        return relevant.joinToString("\n") { "${it.className}\n${it.subject}" }
    }
}

@Composable
fun HeaderComponent(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val rotation by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isDarkTheme) 360f else 0f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "ThemeRotation"
            )

            androidx.compose.material3.IconButton(
                onClick = onThemeToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier.rotate(rotation),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.Crossfade(
                        targetState = isDarkTheme,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 500),
                        label = "ThemeCrossfade"
                    ) { darkTheme ->
                        if (darkTheme) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.WbSunny,
                                contentDescription = "Switch to Light Theme",
                                tint = Color(0xFFFFC107) // Amber/Gold for Sun
                            )
                        } else {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Nightlight,
                                contentDescription = "Switch to Dark Theme",
                                tint = Color(0xFF2C3E50) // Dark Blue for Moon
                            )
                        }
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.school_logo),
                        contentDescription = "School Logo",
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 12.dp)
                    )
                    val textColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF34495E)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Abutorab M.L. High School",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = textColor
                        )
                        Text(
                            text = "Mirsarai, Chattogram",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = textColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Welcome to the Official Routine Portal. Stay updated with your daily class schedules seamlessly.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
        }
    }
}
