package com.project.auto_aid.utils

import java.text.NumberFormat
import java.util.Locale

fun formatUgx(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "UG"))
    return formatter.format(amount).replace("UGX", "Shs")
}