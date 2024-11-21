package com.example.hefsek

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

enum class PeriodType {
    PERIOD,
    HEFSEK_TAHARA,
    SEVEN_CLEAN
}

fun createPeriodBackground(type: PeriodType, context: Context): Drawable {
    val backgroundResId = when (type) {
        PeriodType.PERIOD -> R.drawable.period_background
        PeriodType.HEFSEK_TAHARA -> R.drawable.hefsek_tahara_background
        PeriodType.SEVEN_CLEAN -> R.drawable.seven_clean_background
    }
    return ContextCompat.getDrawable(context, backgroundResId)!!
} 