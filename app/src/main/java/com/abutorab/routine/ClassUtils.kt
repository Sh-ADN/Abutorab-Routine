package com.abutorab.routine

fun isClassMatch(c1: String, c2: String): Boolean {
    if (c1 == c2) return true
    val base1 = c1.split("-")[0].trim()
    val base2 = c2.split("-")[0].trim()
    if (base1 != base2) return false
    return c1.contains(c2) || c2.contains(c1)
}
