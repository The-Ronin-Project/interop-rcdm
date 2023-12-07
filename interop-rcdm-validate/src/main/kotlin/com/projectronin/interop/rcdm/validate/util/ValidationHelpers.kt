package com.projectronin.interop.rcdm.validate.util

/**
 * Determines if the String is null or a day format ("YYYY-MM-dd" or "YYYY-MM-ddTHH:MM:SS").
 */
fun String?.isNullOrDayFormat(): Boolean {
    this?.length?.let { if (it < 10) return false }
    return true
}
