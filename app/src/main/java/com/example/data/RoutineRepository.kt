package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class DatabaseResponse(
    val teachers: List<String>,
    val masterGrid: List<List<String>>
)

data class RoutineEntry(
    val day: Int,
    val className: String,
    val period: Int,
    val subject: String,
    val teacher: String
)

interface RoutineApi {
    @GET("macros/s/AKfycbzGUBlzXFJ_OS8cDTKOvEl4-7B1TMFYvF-n_RySMP61SnUOSTcmnWll5L6-fvNrabdmKw/exec?action=getFullDatabase")
    suspend fun getRoutine(): DatabaseResponse
}

object RetrofitClient {
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: RoutineApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://script.google.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RoutineApi::class.java)
    }
}

class RoutineRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("routine_cache", Context.MODE_PRIVATE)

    // Shared parsing function for day-range logic
    fun parseDayRanges(rangeStr: String): List<Int> {
        val days = mutableListOf<Int>()
        val cleanStr = rangeStr.replace("(", "").replace(")", "").replace(" ", "")
        if (cleanStr.isEmpty()) return emptyList()

        val parts = cleanStr.split(",")
        for (part in parts) {
            if (part.contains("-")) {
                val bounds = part.split("-")
                if (bounds.size == 2) {
                    val start = bounds[0].toIntOrNull() ?: continue
                    val end = bounds[1].toIntOrNull() ?: continue
                    for (i in start..end) {
                        days.add(i)
                    }
                }
            } else {
                val day = part.toIntOrNull()
                if (day != null) {
                    days.add(day)
                }
            }
        }
        return days.distinct().sorted()
    }

    val allClasses: List<String> = listOf(
        "VI-A", "VI-B", "VII-A", "VII-B", 
        "VIII-A", "VIII-B", "IX-A", "IX-B", 
        "X-A", "X-B"
    )

    // Store ordered teachers derived directly from network response
    var allTeachers: List<String> = emptyList()
        private set

    fun parseCell(cell: String, period: Int, teacher: String): List<RoutineEntry> {
        val results = mutableListOf<RoutineEntry>()
        val parts = cell.split(",\n", " / ")
        var lastSubject = ""
        for (p in parts) {
            var str = p.trim()
            if (str.isEmpty() || str == "-") continue
            
            var daysRange = "2-6"
            if (str.endsWith(")")) {
                val startIdx = str.lastIndexOf("(")
                if (startIdx != -1) {
                    daysRange = str.substring(startIdx + 1, str.length - 1)
                    str = str.substring(0, startIdx).trim()
                }
            }
            
            // Extract class name
            val validClasses = listOf("VI-A", "VI-B", "VII-A", "VII-B", "VIII-A", "VIII-B", "IX-A", "IX-B", "X-A", "X-B", "VI", "VII", "VIII", "IX", "X")
            var className = ""
            for (c in validClasses) {
                if (str.endsWith(" $c") || str == c) {
                    className = c
                    str = str.substring(0, str.length - c.length).trim()
                    break
                }
            }
            if (className.isEmpty()) continue
            
            var subject = str
            if (subject.isNotEmpty()) {
                lastSubject = subject
            } else {
                subject = lastSubject
            }
            
            val days = parseDayRanges(daysRange)
            for (day in days) {
                results.add(RoutineEntry(day, className, period, subject, teacher))
            }
        }
        return results
    }

    private fun processResponse(response: DatabaseResponse): List<RoutineEntry> {
        val parsedEntries = mutableListOf<RoutineEntry>()
        for (tIndex in response.teachers.indices) {
            val teacher = response.teachers[tIndex]
            val gridRow = response.masterGrid.getOrNull(tIndex) ?: continue
            
            for (colIndex in gridRow.indices) {
                if (colIndex == 4) continue // Break
                val period = if (colIndex < 4) colIndex + 1 else colIndex
                val cellStr = gridRow[colIndex]
                
                parsedEntries.addAll(parseCell(cellStr, period, teacher))
            }
        }
        return parsedEntries
    }

    fun getRoutine(): Flow<List<RoutineEntry>> = flow {
        var hasCache = false
        val cacheJson = prefs.getString("cached_response", null)
        if (cacheJson != null) {
            try {
                val adapter = RetrofitClient.moshi.adapter(DatabaseResponse::class.java)
                val response = adapter.fromJson(cacheJson)
                if (response != null) {
                    allTeachers = response.teachers
                    val parsedEntries = processResponse(response)
                    emit(parsedEntries)
                    hasCache = true
                }
            } catch (e: Exception) {
                Log.e("RoutineRepository", "Failed to parse cache", e)
            }
        }

        try {
            val response = RetrofitClient.api.getRoutine()
            allTeachers = response.teachers

            try {
                val adapter = RetrofitClient.moshi.adapter(DatabaseResponse::class.java)
                val json = adapter.toJson(response)
                prefs.edit().putString("cached_response", json).apply()
            } catch (e: Exception) {
                Log.e("RoutineRepository", "Failed to save cache", e)
            }

            val parsedEntries = processResponse(response)
            emit(parsedEntries)
        } catch (e: Exception) {
            if (!hasCache) {
                throw e
            }
        }
    }.flowOn(Dispatchers.IO)
}
