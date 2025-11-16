package com.example.habittrack

import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar dark mode guardado ANTES de inflar la vista
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val darkModeOn = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkModeOn) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calender)

        // Datos que nos manda el Dashboard
        val habitTitle = intent.getStringExtra("habit_title") ?: "Hábito"
        val habitStreak = intent.getStringExtra("habit_streak") ?: ""

        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val txtActivity = findViewById<TextView>(R.id.txtactivity)
        val streakEmojiCount = findViewById<TextView>(R.id.streakEmojiCount)
        val streakMessage = findViewById<TextView>(R.id.streakMessage)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val checkInButton = findViewById<Button>(R.id.checkInButton)

        txtActivity.text = habitTitle

        // Ej: "Racha: 3 días" → "🔥 Racha: 3 días"
        if (habitStreak.isNotBlank()) {
            streakEmojiCount.text = "🔥 $habitStreak"
        } else {
            streakEmojiCount.text = "🔥 Sin racha todavía"
        }

        buttonBack.setOnClickListener { finish() }

        // Botón de check-in (solo muestra un mensaje por ahora)
        checkInButton.setOnClickListener {
            val selectedDate = calendarView.date
            val format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("es"))
            val dateText = format.format(Date(selectedDate))

            Toast.makeText(
                this,
                "Check-in registrado para $dateText",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}