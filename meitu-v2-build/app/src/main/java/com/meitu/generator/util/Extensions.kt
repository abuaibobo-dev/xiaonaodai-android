package com.meitu.generator.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toDateString(): String {
    if (this == 0L) return "-"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toShortDateString(): String {
    if (this == 0L) return "-"
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toDurationString(): String {
    if (this <= 0L) return "0s"
    val minutes = this / 60
    val seconds = this % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
