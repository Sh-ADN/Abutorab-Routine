package com.example

import org.junit.Assert.*
import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class ExampleUnitTest {
  @Test
  fun testApiFetch() {
    val client = OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build()
    val request = Request.Builder()
        .url("https://script.google.com/macros/s/AKfycbzGUBlzXFJ_OS8cDTKOvEl4-7B1TMFYvF-n_RySMP61SnUOSTcmnWll5L6-fvNrabdmKw/exec?action=getFullDatabase")
        .build()

    val responseBody = client.newCall(request).execute().use { response ->
        response.body?.string()
    }
    java.io.File("response.txt").writeText(responseBody ?: "null")
  }
}
