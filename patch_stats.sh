cat << 'INNER_EOF' > app/src/main/java/com/abutorab/routine/TeacherStatistics.kt
package com.abutorab.routine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abutorab.routine.data.RoutineEntry
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically

private data class DayStats(val day: Int, val classCount: Int, val gaps: Int)

@Composable
fun TeacherStatistics(entries: List<RoutineEntry>) {
    var expanded by remember { mutableStateOf(false) }

    val totalClasses = entries.size
    var totalMinutes = 0
    entries.forEach { entry ->
        totalMinutes += if (entry.period == 1) 45 else 40
    }
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val totalTeachingHoursStr = "${hours}h ${minutes}m"
    val uniqueClasses = entries.map { it.className.split("-")[0].trim() }.distinct().size
    val uniqueSubjects = entries.map { "${it.subject}-${it.className.trim()}" }.distinct().size

    val daysCount = daysConfig.size
    val averageClasses = if (daysCount > 0) String.format(Locale.US, "%.1f", totalClasses.toFloat() / daysCount) else "0"

    val dayStatsList = daysConfig.map { (dayValue, _) ->
        val dayEntries = entries.filter { it.day == dayValue }
        val classCount = dayEntries.size
        val periods = dayEntries.map { it.period }.sorted()
        val gaps = if (periods.size > 1) {
            val span = periods.last() - periods.first() + 1
            span - periods.size
        } else {
            0
        }
        DayStats(dayValue, classCount, gaps)
    }

    val busiestDayEntry = dayStatsList.maxWithOrNull(compareBy({ it.classCount }, { -it.gaps }))
    val leastBusyDayEntry = dayStatsList.minWithOrNull(compareBy({ it.classCount }, { -it.gaps }))

    val busiestWeekdayStr = busiestDayEntry?.let { stats -> "${daysConfig.find { it.first == stats.day }?.second} (${stats.classCount} classes)" } ?: "N/A"
    val leastBusyWeekdayStr = leastBusyDayEntry?.let { stats -> "${daysConfig.find { it.first == stats.day }?.second} (${stats.classCount} classes)" } ?: "N/A"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Teacher Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    StatRow("📚 Total Classes:", totalClasses.toString())
                    StatRow("⏱️ Total Teaching Hours:", totalTeachingHoursStr)
                    StatRow("📖 Unique Subjects:", uniqueSubjects.toString())
                    StatRow("🏫 Unique Classes:", uniqueClasses.toString())
                    StatRow("📊 Average Classes per Day:", averageClasses)
                    StatRow("🏃 Busiest Weekday:", busiestWeekdayStr)
                    StatRow("🚶 Least Busy Weekday:", leastBusyWeekdayStr)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
INNER_EOF
