package com.abutorab.routine

import org.junit.Assert.*
import org.junit.Test
import java.util.regex.Pattern

class ExampleUnitTest {
  @Test
  fun testApiFetch() {
    val cell = "F&B IX (2-4), \nBen-1 X-B (5-6)"
    val parts = cell.split(Regex(",\\s*\\n| / "))
    assertEquals(2, parts.size)
    assertEquals("F&B IX (2-4)", parts[0])
    assertEquals("Ben-1 X-B (5-6)", parts[1])
  }
}
