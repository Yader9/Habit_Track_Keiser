package com.example.habittrack

import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    // Firebase
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null

    // Datos del hábito actual
    private var habitIndex: Int = -1
    private var currentStreakDays: Int = 0
    private var habitTitle: String = "Hábito"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tema (dark / light)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val darkModeOn = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkModeOn) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calender)

        // Firebase
        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid

        // Datos que vienen del Dashboard
        habitTitle = intent.getStringExtra("habit_title") ?: "Hábito"
        habitIndex = intent.getIntExtra("habit_index", -1)
        currentStreakDays = intent.getIntExtra("habit_streak_days", 0)

        // Vistas
        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val txtActivity = findViewById<TextView>(R.id.txtactivity)
        val streakEmojiCount = findViewById<TextView>(R.id.streakEmojiCount)
        val streakMessage = findViewById<TextView>(R.id.streakMessage)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val checkInButton = findViewById<Button>(R.id.checkInButton)

        txtActivity.text = habitTitle

        // Mostrar racha inicial
        if (currentStreakDays > 0) {
            streakEmojiCount.text = "🔥 Racha: $currentStreakDays días"
            streakMessage.text = "¡Sigue así! Llevas $currentStreakDays días seguidos."
        } else {
            streakEmojiCount.text = "🔥 Sin racha todavía"
            streakMessage.text =
                "Toca un día y presiona \"Registrar check-in\" para empezar tu racha."
        }

        // 🔙 Botón atrás: simplemente cerrar CalendarActivity
        // Esto devuelve a la Activity anterior en la pila (DashboardActivity)
        buttonBack.setOnClickListener {
            finish()
        }

        // ✅ Botón de check-in: registra la racha en Firestore
        checkInButton.setOnClickListener {
            val selectedDate = calendarView.date
            val format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("es"))
            val dateText = format.format(Date(selectedDate))

            // Feedback inmediato
            Toast.makeText(
                this,
                "Check-in registrado para $dateText",
                Toast.LENGTH_SHORT
            ).show()

            // Actualizar racha en Firestore
            updateStreakInFirestore(
                onUpdated = { newStreak ->
                    currentStreakDays = newStreak
                    streakEmojiCount.text = "🔥 Racha: $currentStreakDays días"
                    streakMessage.text =
                        "¡Excelente! Llevas $currentStreakDays días seguidos."
                },
                onError = {
                    Toast.makeText(
                        this,
                        "No se pudo guardar la racha. Intenta de nuevo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    /**
     * Lee users/{uid}, incrementa streakDays del hábito [habitIndex],
     * actualiza también el mapa streaks[title] y guarda en Firestore.
     */
    private fun updateStreakInFirestore(
        onUpdated: (Int) -> Unit,
        onError: () -> Unit
    ) {
        val uid = userId ?: run {
            onError()
            return
        }
        if (habitIndex < 0) {
            onError()
            return
        }

        val userDocRef = db.collection("users").document(uid)

        userDocRef.get()
            .addOnSuccessListener { snapshot ->
                // Leer lista de hábitos de forma segura
                val rawHabits = snapshot.get("habits") as? List<*> ?: emptyList<Any>()
                if (habitIndex !in rawHabits.indices) {
                    onError()
                    return@addOnSuccessListener
                }

                // Convertir a lista mutable de mapas mutables
                val mutableHabits = rawHabits.mapNotNull { it as? Map<*, *> }
                    .map { it.toMutableMap() as MutableMap<String, Any?> }
                    .toMutableList()

                val habitMap = mutableHabits[habitIndex]

                val oldStreak = (habitMap["streakDays"] as? Number)?.toInt() ?: 0
                val newStreak = oldStreak + 1
                habitMap["streakDays"] = newStreak

                // Actualizar también el mapa "streaks"
                val rawStreaks = snapshot.get("streaks") as? Map<*, *>
                val streaksMap = rawStreaks
                    ?.mapNotNull { (k, v) ->
                        (k as? String)?.let { key -> key to v }
                    }
                    ?.toMap()
                    ?.toMutableMap() ?: mutableMapOf<String, Any?>()

                val titleKey = habitMap["title"] as? String ?: habitTitle
                streaksMap[titleKey] = newStreak

                val dataToUpdate = hashMapOf<String, Any>(
                    "habits" to mutableHabits,
                    "streaks" to streaksMap
                )

                userDocRef
                    .set(dataToUpdate, SetOptions.merge())
                    .addOnSuccessListener { onUpdated(newStreak) }
                    .addOnFailureListener { onError() }
            }
            .addOnFailureListener {
                onError()
            }
    }
}