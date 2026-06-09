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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clipToBounds
import com.example.data.RoutineEntry
import com.example.ui.RoutineUiState
import com.example.ui.RoutineViewModel
import com.example.ui.SearchMode
import com.example.ui.theme.MyApplicationTheme
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {
    private val viewModel: RoutineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RoutineApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineApp(viewModel: RoutineViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val selectedQuery by viewModel.selectedQuery.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderComponent(modifier = Modifier.padding(bottom = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = searchMode == SearchMode.BY_CLASS,
                onClick = { viewModel.setMode(SearchMode.BY_CLASS) },
                label = { Text("By Class") },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = searchMode == SearchMode.BY_TEACHER,
                onClick = { viewModel.setMode(SearchMode.BY_TEACHER) },
                label = { Text("By Teacher") }
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
                if (currentQuery != null) {
                    val relevantEntries = state.entries.filter { 
                        if (searchMode == SearchMode.BY_CLASS) {
                            it.className == currentQuery || currentQuery.startsWith(it.className + "-")
                        }
                        else it.teacher == currentQuery
                    }

                    val titleText = if (searchMode == SearchMode.BY_CLASS) {
                        "Class $currentQuery's Schedule"
                    } else {
                        "$currentQuery's Class Routine"
                    }
                    
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (relevantEntries.isEmpty()) {
                        Text(
                            text = "No schedule found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 48.dp)
                        )
                    } else {
                        RoutineTableWrapper(
                            entries = relevantEntries,
                            mode = searchMode,
                            query = currentQuery,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = "Please select ${if (searchMode == SearchMode.BY_CLASS) "a class" else "a teacher"}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 48.dp)
                    )
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    scale = (scale * zoom).coerceIn(0.25f, 4f)
                    
                    // Keep the focus on the pinched point
                    val fraction = (scale / oldScale) - 1
                    val zoomOffset = (centroid - offset) * fraction
                    
                    offset = offset + pan - zoomOffset
                }
            }
    ) {
        val availableWidth = maxWidth
        val desiredWidthDp = 900.dp

        if (!initialSetupDone && availableWidth.value > 0) {
            val ratio = if (availableWidth < desiredWidthDp) {
                (availableWidth.value / desiredWidthDp.value)
            } else {
                1f
            }
            scale = ratio
            // Center initially
            val scaledWidth = desiredWidthDp.value * scale
            if (scaledWidth < availableWidth.value) {
                offset = Offset((availableWidth.value - scaledWidth) / 2f, 0f)
            }
            initialSetupDone = true
        }

        Box(
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                )
                .wrapContentSize(unbounded = true, align = Alignment.TopStart)
        ) {
            RoutineTable(
                entries = entries,
                mode = mode,
                query = query,
                modifier = Modifier.requiredWidth(desiredWidthDp),
                viewportOffset = offset,
                viewportScale = scale
            )
        }
    }
}

@Composable
fun RoutineTable(
    entries: List<RoutineEntry>,
    mode: SearchMode,
    query: String,
    modifier: Modifier = Modifier,
    viewportOffset: Offset = Offset.Zero,
    viewportScale: Float = 1f
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

    fun isDayActive(dayValue: Int): Boolean {
        // Calendar.SUNDAY is 1, Calendar.MONDAY is 2. 
        // Our data uses 2 for Sunday, 3 for Monday
        return (currentTime.get(Calendar.DAY_OF_WEEK) + 1) == dayValue
    }

    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val days = listOf(2 to "Sun", 3 to "Mon", 4 to "Tue", 5 to "Wed", 6 to "Thu")
    
    data class PeriodHeader(val num: Int, val name: String, val time: String)
    val periods = listOf(
        PeriodHeader(1, "1st Period", "10:26-11:10"),
        PeriodHeader(2, "2nd Period", "11:11-11:50"),
        PeriodHeader(3, "3rd Period", "11:51-12:30"),
        PeriodHeader(4, "4th Period", "12:31-01:10"),
        PeriodHeader(-1, "Tiffin Break", "01:11-02:00"),
        PeriodHeader(5, "5th Period", "02:01-02:40"),
        PeriodHeader(6, "6th Period", "02:41-03:20"),
        PeriodHeader(7, "7th Period", "03:21-04:00")
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .zIndex(2f)
                .graphicsLayer {
                    translationY = if (viewportOffset.y < 0) -viewportOffset.y / viewportScale else 0f
                }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .zIndex(3f)
                    .graphicsLayer {
                        translationX = if (viewportOffset.x < 0) -viewportOffset.x / viewportScale else 0f
                    }
            ) {
                HeaderCell("Day", 80.dp)
            }
            periods.forEach { p ->
                if (p.num == -1) {
                    HeaderCell("Break", 50.dp, isTiffin = true, isActive = isPeriodActive(p.time)) 
                } else {
                    HeaderCell("${p.name}\n${p.time}", 110.dp, isActive = isPeriodActive(p.time))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // Days Column
            Column(
                modifier = Modifier
                    .zIndex(2f)
                    .graphicsLayer {
                        translationX = if (viewportOffset.x < 0) -viewportOffset.x / viewportScale else 0f
                    }
            ) {
                days.forEach { (dayValue, dayStr) ->
                    Cell(dayStr, 80.dp, isDark = true, height = 80.dp, isActive = isDayActive(dayValue))
                }
            }

            periods.forEach { p ->
                if (p.num == -1) {
                    TiffinBreakCell(50.dp, 80.dp * 5, isActive = isPeriodActive(p.time))
                } else {
                    Column {
                        val activePeriod = isPeriodActive(p.time)
                        days.forEach { (dayValue, _) ->
                            val text = getCellText(entries, dayValue, p.num, mode, query)
                            val activeCell = activePeriod && isDayActive(dayValue)
                            Cell(text, 110.dp, isDark = false, height = 80.dp, isActive = activeCell)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp, isTiffin: Boolean = false, isActive: Boolean = false) {
    val baseColor = if (isTiffin) Color(0xFFFFCCAA) else MaterialTheme.colorScheme.onSurfaceVariant
    val highlightBg = MaterialTheme.colorScheme.primaryContainer
    
    val backgroundColor = when {
        isActive && !isTiffin -> highlightBg
        isActive && isTiffin -> Color(0xFFFFB74D)
        isTiffin -> Color(0xFFFFE0B2) // Added a solid background for tiffin
        else -> MaterialTheme.colorScheme.surfaceVariant // Added a solid background
    }
    
    Box(
        modifier = Modifier
            .width(width)
            .height(56.dp)
            .background(backgroundColor)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = if (isActive && !isTiffin) MaterialTheme.colorScheme.onPrimaryContainer else if (isTiffin) Color(0xFFD84315) else baseColor,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun Cell(text: String, width: androidx.compose.ui.unit.Dp, isDark: Boolean, height: androidx.compose.ui.unit.Dp = 80.dp, isActive: Boolean = false) {
    val highlightBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val baseBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    val backgroundColor = if (isActive) highlightBg else baseBg

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(backgroundColor)
            .border(if (isActive) 1.5.dp else 0.5.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isDark || isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive && !isDark) MaterialTheme.colorScheme.onSurface else Color.Unspecified,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun TiffinBreakCell(width: androidx.compose.ui.unit.Dp, totalHeight: androidx.compose.ui.unit.Dp, isActive: Boolean = false) {
    Box(
        modifier = Modifier
            .width(width)
            .height(totalHeight)
            .background(if (isActive) Color(0xFFFFCC80) else Color(0xFFFFF3E0))
            .border(if (isActive) 1.5.dp else 0.5.dp, if (isActive) Color(0xFFE65100) else MaterialTheme.colorScheme.outlineVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TIFFIN BREAK",
            color = Color(0xFFE65100),
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.sp,
            modifier = Modifier.rotate(-90f),
            maxLines = 1,
            softWrap = false
        )
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
fun HeaderComponent(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Abutorab M.L. High School",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
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
