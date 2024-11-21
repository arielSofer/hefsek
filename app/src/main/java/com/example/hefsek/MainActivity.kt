package com.example.hefsek

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hefsek.databinding.ActivityMainBinding
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.Calendar
import android.icu.util.HebrewCalendar
import android.icu.util.ULocale

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val today = LocalDate.now()
    private val hebrewCalendar = HebrewCalendar(ULocale("he"))
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupCalendarView()
        setupMonthNavigation()
        observeViewModel()
    }
    
    private fun setupMonthNavigation() {
        binding.previousMonthButton.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.minusMonths(1))
            }
        }
        
        binding.nextMonthButton.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.plusMonths(1))
            }
        }
    }
    
    private fun setupCalendarView() {
        val currentMonth = YearMonth.now()
        val firstMonth = currentMonth.minusMonths(10)
        val lastMonth = currentMonth.plusMonths(10)
        val firstDayOfWeek = WeekFields.of(Locale("he")).firstDayOfWeek
        
        binding.calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                container.textView.text = "ו ה ד ג ב א ש"
                container.textView.layoutDirection = View.LAYOUT_DIRECTION_RTL
            }
        }
        
        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView = view.findViewById<TextView>(R.id.calendarDayText)
            lateinit var day: CalendarDay
            
            init {
                view.setOnClickListener {
                    if (day.owner == DayOwner.THIS_MONTH) {
                        showConfirmationDialog(day.date)
                    }
                }
            }
        }
        
        binding.calendarView.dayBinder = object : DayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                val textView = container.textView
                
                when (day.owner) {
                    DayOwner.THIS_MONTH -> {
                        // המרה לתאריך עברי
                        hebrewCalendar.clear()
                        hebrewCalendar.set(day.date.year, day.date.monthValue - 1, day.date.dayOfMonth)
                        val dayNumber = hebrewCalendar.get(HebrewCalendar.DAY_OF_MONTH)
                        
                        // המרה למספר עברי
                        textView.text = convertToHebrewNumber(dayNumber)
                        textView.layoutDirection = View.LAYOUT_DIRECTION_RTL
                        textView.visibility = View.VISIBLE
                        
                        val epochDay = day.date.toEpochDay()
                        
                        // ימי מחזור קיימים - עדיפות ראשונה
                        if (viewModel.isPeriodDay(epochDay)) {
                            textView.setTextColor(Color.WHITE)
                            textView.background = createPeriodBackground(PeriodType.PERIOD, this@MainActivity)
                        }
                        // יום הפסק טהרה - עדיפות שנייה
                        if (viewModel.isHefsekTaharaDay(epochDay)) {
                            textView.setTextColor(Color.WHITE)
                            textView.background = createPeriodBackground(PeriodType.HEFSEK_TAHARA, this@MainActivity)
                        }
                        // שבעה נקיים - עדיפות שלישית
                        if (viewModel.isSevenCleanDay(epochDay)) {
                            val checks = viewModel.getCleanDayChecks(epochDay)
                            if (checks != null && checks.first && checks.second) {
                                textView.setTextColor(Color.WHITE)
                                textView.background = createPeriodBackground(PeriodType.SEVEN_CLEAN, this@MainActivity)
                            } else {
                                textView.setTextColor(Color.BLACK)
                                textView.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.unclean_day_background)
                            }
                            textView.setOnLongClickListener {
                                showCleanDayChecksDialog(day.date)
                                true
                            }
                        }
                        // ימים פוטנציאליים - עדיפות אחרונה
                        if (viewModel.isAnyPotentialDay(epochDay)) {
                            textView.setTextColor(Color.BLACK)
                            if (viewModel.isPotentialDayCheckedAndClean(epochDay)) {
                                textView.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.checked_clean_background)
                            } else {
                                textView.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.expected_period_background)
                            }
                        }
                        // היום הנוכחי
                        if (day.date == today) {
                            textView.setTextColor(Color.BLACK)
                            textView.background = null
                            textView.setTypeface(null, Typeface.BOLD)
                        }
                        // ימים רגילים
                        else {
                            textView.setTextColor(Color.BLACK)
                            textView.background = null
                            textView.setTypeface(null, Typeface.NORMAL)
                        }
                    }
                    DayOwner.NEXT_MONTH, DayOwner.PREVIOUS_MONTH -> {
                        textView.visibility = View.INVISIBLE  // במקום TRANSPARENT
                        textView.background = null
                        textView.isClickable = false
                        container.view.isClickable = false  // ביטול אפשרות הלחיצה על כל התא
                    }
                }
            }
        }
        
        binding.calendarView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        binding.calendarView.setup(firstMonth, lastMonth, firstDayOfWeek)
        binding.calendarView.scrollToMonth(currentMonth)
        
        binding.calendarView.monthScrollListener = { month ->
            hebrewCalendar.clear()
            hebrewCalendar.set(month.yearMonth.year, month.yearMonth.monthValue - 1, 1)
            val hebrewMonth = when (hebrewCalendar.get(HebrewCalendar.MONTH)) {
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
            val hebrewYear = hebrewCalendar.get(HebrewCalendar.YEAR)
            binding.monthYearText.text = "$hebrewMonth $hebrewYear"
        }
    }
    
    class MonthViewContainer(view: View) : ViewContainer(view) {
        val textView = view.findViewById<TextView>(R.id.headerTextView)
    }
    
    private fun observeViewModel() {
        viewModel.periodDates.observe(this) { periodData ->
            binding.calendarView.notifyCalendarChanged()
        }
    }
    
    private fun showConfirmationDialog(date: LocalDate) {
        val epochDay = date.toEpochDay()
        
        // בדיקה אם זה יום פוטנציאלי למחזור
        if (viewModel.isMonthlyPeriodDay(epochDay) || 
            viewModel.isAveragePeriodDay(epochDay) || 
            viewModel.isIntervalPeriodDay(epochDay)) {
                
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.potential_period_day))
                .setMessage(getString(R.string.potential_period_message))
                .setPositiveButton(getString(R.string.mark_period)) { _, _ ->
                    showTimePicker(date)
                }
                .setNeutralButton(getString(R.string.perform_check)) { _, _ ->
                    showPotentialDayCheckDialog(date)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
            showTimePicker(date)
        }
    }
    
    private fun showPotentialDayCheckDialog(date: LocalDate) {
        AlertDialog.Builder(this)
            .setTitle("תוצאת בדיקה")
            .setMessage("האם הבדיקה נקייה?")
            .setPositiveButton("נקי") { _, _ ->
                viewModel.markPotentialDayAsChecked(date.toEpochDay(), true)
            }
            .setNegativeButton("לא נקי") { _, _ ->
                viewModel.markPotentialDayAsChecked(date.toEpochDay(), false)
            }
            .show()
    }
    
    private fun showTimePicker(date: LocalDate) {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                showDateTimeConfirmationDialog(date, LocalTime.of(hourOfDay, minute))
            },
            LocalTime.now().hour,
            LocalTime.now().minute,
            true
        ).show()
    }
    
    private fun showDateTimeConfirmationDialog(date: LocalDate, time: LocalTime) {
        val dateTime = LocalDateTime.of(date, time)
        val jewishDateString = viewModel.getJewishDateString(date)
        val formattedDateTime = "${jewishDateString} ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_period_start_time))
            .setMessage(getString(R.string.confirm_period_start, formattedDateTime))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.onDateTimeSelected(dateTime)
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
    
    private fun showCleanDayChecksDialog(date: LocalDate) {
        val epochDay = date.toEpochDay()
        val checks = viewModel.getCleanDayChecks(epochDay)
        val jewishDateString = viewModel.getJewishDateString(date)
        
        val items = arrayOf(
            "בדיקת בוקר",
            "בדיקת ערב"
        )
        val checkedItems = booleanArrayOf(
            checks?.first ?: false,
            checks?.second ?: false
        )
        
        AlertDialog.Builder(this)
            .setTitle("בדיקות ליום $jewishDateString")
            .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                viewModel.toggleCleanDayCheck(epochDay, which == 0)
            }
            .setPositiveButton("אישור", null)
            .show()
    }
    
    private fun convertToHebrewNumber(number: Int): String {
        val hebrewNumerals = mapOf(
            1 to "א", 2 to "ב", 3 to "ג", 4 to "ד", 5 to "ה",
            6 to "ו", 7 to "ז", 8 to "ח", 9 to "ט", 10 to "י",
            20 to "כ", 30 to "ל", 40 to "מ", 50 to "נ",
            60 to "ס", 70 to "ע", 80 to "פ", 90 to "צ",
            100 to "ק", 200 to "ר", 300 to "ש", 400 to "ת"
        )
        
        if (number <= 0) return ""
        
        val result = StringBuilder()
        var remaining = number
        
        // טיפול במספרים 15 ו-16 במיוחד
        if (remaining == 15) return "טו"
        if (remaining == 16) return "טז"
        
        // מאות
        while (remaining >= 100) {
            val hundreds = (remaining / 100) * 100
            result.append(hebrewNumerals[hundreds])
            remaining %= 100
        }
        
        // עשרות
        if (remaining >= 10) {
            val tens = (remaining / 10) * 10
            result.append(hebrewNumerals[tens])
            remaining %= 10
        }
        
        // יחידות
        if (remaining > 0) {
            result.append(hebrewNumerals[remaining])
        }
        
        return result.toString()
    }
    

}