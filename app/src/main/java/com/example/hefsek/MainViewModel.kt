package com.example.hefsek

import android.icu.util.Calendar
import android.icu.util.HebrewCalendar
import android.icu.util.ULocale
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime


class MainViewModel : ViewModel() {
    private val _periodDates = MutableLiveData<PeriodData>()
    val periodDates: LiveData<PeriodData> = _periodDates
    
    data class PeriodEntry(
        val epochDay: Long,
        val hebrewCalendar: HebrewCalendar,
        val hourOfDay: Int,
        val minute: Int
    )
    
    data class CleanDayCheck(
        val epochDay: Long,
        var morningCheck: Boolean = false,
        var eveningCheck: Boolean = false
    )
    
    private val periodHistory = mutableListOf<PeriodEntry>()
    private val cleanDaysChecks = mutableMapOf<Long, CleanDayCheck>()
    private val potentialDaysChecks = mutableMapOf<Long, Boolean>()
    
    fun onDateTimeSelected(dateTime: LocalDateTime) {
        val hebrewCalendar = HebrewCalendar(ULocale("he"))
        hebrewCalendar.set(dateTime.year, dateTime.monthValue - 1, dateTime.dayOfMonth)
        
        val entry = PeriodEntry(
            epochDay = dateTime.toLocalDate().toEpochDay(),
            hebrewCalendar = hebrewCalendar,
            hourOfDay = dateTime.hour,
            minute = dateTime.minute
        )
        addPeriodStart(entry)
    }
    
    fun isPeriodDay(date: Long): Boolean {
        return _periodDates.value?.periodDays?.contains(date) ?: false
    }
    
    fun isHefsekTaharaDay(date: Long): Boolean {
        return _periodDates.value?.hefsekTaharaDays?.contains(date) ?: false
    }
    
    fun isSevenCleanDay(date: Long): Boolean {
        return _periodDates.value?.sevenCleanDays?.contains(date) ?: false
    }
    
    fun toggleCleanDayCheck(date: Long, isMorning: Boolean) {
        val check = cleanDaysChecks.getOrPut(date) { 
            CleanDayCheck(epochDay = date)
        }
        
        if (isMorning) {
            check.morningCheck = !check.morningCheck
        } else {
            check.eveningCheck = !check.eveningCheck
        }
        updatePeriodData()
    }
    
    fun getCleanDayChecks(date: Long): Pair<Boolean, Boolean>? {
        return cleanDaysChecks[date]?.let { 
            Pair(it.morningCheck, it.eveningCheck) 
        }
    }
    
    private fun addPeriodStart(entry: PeriodEntry) {
        viewModelScope.launch {
            // הוספת הרשומה החדשה
            periodHistory.add(entry)
            
            // מיון הרשומות לפי תאריך
            periodHistory.sortBy { it.epochDay }
            
            // עדכון הנתונים
            updatePeriodData()
        }
    }
    
    private fun updatePeriodData() {
        // חישוב ימי הדם והבדיקות עבור כל רשומה בהיסטוריה
        val allPeriodDays = mutableListOf<Long>()
        val allHefsekDays = mutableListOf<Long>()
        val allCleanDays = mutableListOf<Long>()
        
        periodHistory.forEach { entry ->
            // 5 ימי דם
            val periodDays = (0L..4L).map { offset ->
                entry.epochDay + offset
            }
            allPeriodDays.addAll(periodDays)
            
            // יום הפסק טהרה
            allHefsekDays.add(entry.epochDay + 5)
            
            // 7 ימי נקיים
            val cleanDays = (6L..12L).map { offset ->
                entry.epochDay + offset
            }
            allCleanDays.addAll(cleanDays)
        }
        
        // חישוב תחזית המחזור הבא
        val nextPeriodPrediction = if (periodHistory.size >= 2) {
            val lastTwoEntries = periodHistory.takeLast(2)
            val interval = lastTwoEntries[1].epochDay - lastTwoEntries[0].epochDay
            lastTwoEntries[1].epochDay + interval
        } else {
            periodHistory.lastOrNull()?.let { it.epochDay + 29 }
        }
        
        // חישוב וסת ההפלגה - רק אם יש בדיוק 2 מחזורים
        val intervalPeriodDay = if (periodHistory.size == 2) {
            val firstEntry = periodHistory[0]
            val secondEntry = periodHistory[1]
            val interval = secondEntry.epochDay - firstEntry.epochDay
            // מוסיפים את אותו מרווח מהמחזור האחרון
            secondEntry.epochDay + interval
        } else null
        
        // חישוב עונה בינונית - 30 יום מהמחזור האחרון
        val averagePeriodDay = periodHistory.lastOrNull()?.let { lastEntry ->
            lastEntry.epochDay + 30
        }
        
        // חישוב יום החודש - אותו יום בחודש העברי הבא
        val monthlyPeriodDay = periodHistory.lastOrNull()?.let { lastEntry ->
            val nextMonthDate = lastEntry.hebrewCalendar.clone() as HebrewCalendar
            
            // מעבר לחודש הבא
            if (nextMonthDate.get(HebrewCalendar.MONTH) == nextMonthDate.getActualMaximum(HebrewCalendar.MONTH)) {
                nextMonthDate.add(HebrewCalendar.YEAR, 1)
                nextMonthDate.set(HebrewCalendar.MONTH, 1)
            } else {
                nextMonthDate.add(HebrewCalendar.MONTH, 1)
            }
            
            // שמירה על אותו יום בחודש
            val dayOfMonth = lastEntry.hebrewCalendar.get(HebrewCalendar.DAY_OF_MONTH)
            val maxDaysInNextMonth = nextMonthDate.getActualMaximum(HebrewCalendar.DAY_OF_MONTH)
            nextMonthDate.set(HebrewCalendar.DAY_OF_MONTH, minOf(dayOfMonth, maxDaysInNextMonth))
            
            convertHebrewToEpochDay(nextMonthDate)
        }
        
        _periodDates.value = PeriodData(
            periodDays = allPeriodDays,
            hefsekTaharaDays = allHefsekDays,
            sevenCleanDays = allCleanDays,
            nextPeriodPrediction = null,  // לא משתמשים בזה יותר
            cleanDaysChecks = cleanDaysChecks.toMap(),
            monthlyPeriodDay = monthlyPeriodDay,    // יום החודש העברי הבא
            averagePeriodDay = averagePeriodDay,    // 30 יום מהמחזור האחרון
            intervalPeriodDay = intervalPeriodDay    // וסת ההפלגה - רק אם יש בדיוק 2 מחזורים
        )
    }
    
    private fun calculateDaysBetween(cal1: HebrewCalendar, cal2: HebrewCalendar): Long {
        return (cal2.timeInMillis - cal1.timeInMillis) / (1000 * 60 * 60 * 24)
    }
    
    fun getJewishDateString(date: LocalDate): String {
        val hebrewCalendar = HebrewCalendar(ULocale("he"))
        hebrewCalendar.set(date.year, date.monthValue - 1, date.dayOfMonth)
        return "${hebrewCalendar.get(HebrewCalendar.DAY_OF_MONTH)} ${getHebrewMonthString(hebrewCalendar)} ${hebrewCalendar.get(HebrewCalendar.YEAR)}"
    }
    
    private fun getHebrewMonthString(hebrewCalendar: HebrewCalendar): String {
        return when (hebrewCalendar.get(HebrewCalendar.MONTH)) {
            HebrewCalendar.TISHRI -> "תשרי"
            HebrewCalendar.HESHVAN -> "חשון"
            HebrewCalendar.KISLEV -> "כסלו"
            HebrewCalendar.TEVET -> "טבת"
            HebrewCalendar.SHEVAT -> "שבט"
            HebrewCalendar.ADAR -> "אדר"
            HebrewCalendar.ADAR_1 -> "אדר א'"
            HebrewCalendar.NISAN -> "ניסן"
            HebrewCalendar.IYAR -> "אייר"
            HebrewCalendar.SIVAN -> "סיון"
            HebrewCalendar.TAMUZ -> "תמוז"
            HebrewCalendar.AV -> "אב"
            HebrewCalendar.ELUL -> "אלול"
            else -> ""
        }
    }
    
    private fun convertHebrewToEpochDay(hebrewCalendar: HebrewCalendar): Long {
        val calendar = Calendar.getInstance()
        calendar.set(
            hebrewCalendar.get(Calendar.YEAR),
            hebrewCalendar.get(Calendar.MONTH),
            hebrewCalendar.get(Calendar.DAY_OF_MONTH)
        )
        return LocalDate.of(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        ).toEpochDay()
    }
    
    fun isMonthlyPeriodDay(date: Long): Boolean {
        return _periodDates.value?.monthlyPeriodDay == date
    }
    
    fun isAveragePeriodDay(date: Long): Boolean {
        return _periodDates.value?.averagePeriodDay == date
    }
    
    fun isIntervalPeriodDay(date: Long): Boolean {
        return _periodDates.value?.intervalPeriodDay == date
    }
    
    // הוספת פונקציה לבדיקה אם יש כבר רשומה בחודש מסוים
    fun hasEntryInMonth(year: Int, month: Int): Boolean {
        return periodHistory.any { entry ->
            entry.hebrewCalendar.get(HebrewCalendar.YEAR) == year &&
            entry.hebrewCalendar.get(HebrewCalendar.MONTH) == month
        }
    }
    
    // הוספת פונקציה לקבלת כל הרשומות
    fun getAllEntries(): List<PeriodEntry> {
        return periodHistory.toList()
    }
    
    fun markPotentialDayAsChecked(date: Long, isClean: Boolean) {
        potentialDaysChecks[date] = isClean
        updatePeriodData()
    }
    
    fun isPotentialDayCheckedAndClean(date: Long): Boolean {
        return potentialDaysChecks[date] == true
    }
    
    fun isAnyPotentialDay(date: Long): Boolean {
        return isMonthlyPeriodDay(date) || 
               isAveragePeriodDay(date) || 
               isIntervalPeriodDay(date)
    }
}

data class PeriodData(
    val periodDays: List<Long>? = null,
    val hefsekTaharaDays: List<Long>? = null,
    val sevenCleanDays: List<Long>? = null,
    val nextPeriodPrediction: Long? = null,
    val cleanDaysChecks: Map<Long, MainViewModel.CleanDayCheck>? = null,
    val monthlyPeriodDay: Long? = null,    // יום החודש העברי
    val averagePeriodDay: Long? = null,    // עונה בינונית - 30 יום מתחילת המחזור
    val intervalPeriodDay: Long? = null    // וסת ההפלגה
) 