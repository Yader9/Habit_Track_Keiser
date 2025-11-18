package com.example.habittrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
        // Aplicar dark mode guardado ANTES de inflar la vista
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

        // Datos que nos manda el Dashboard
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

        // 🔙 Botón atrás de la UI
        buttonBack.setOnClickListener {
            navigateBackToDashboard()
        }

        // 🔙 Gesto / botón físico de atrás del sistema
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateBackToDashboard()
                }
            }
        )

        // Botón de check-in
        checkInButton.setOnClickListener {
            val selectedDate = calendarView.date
            val format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("es"))
            val dateText = format.format(Date(selectedDate))

            // 1) Feedback inmediato al usuario
            Toast.makeText(
                this,
                "Check-in registrado para $dateText",
                Toast.LENGTH_SHORT
            ).show()

            // 2) Actualizar racha en Firestore
            updateStreakInFirestore(
                onUpdated = { newStreak ->
                    // Actualizar UI con la nueva racha
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
     * Navegar de vuelta al Dashboard, tanto si está en la pila como si hay que recrearlo.
     */
    private fun navigateBackToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            // Si Dashboard ya está en la pila, lo trae al frente
            // y limpia cualquier Activity por encima (como este CalendarActivity).
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        // Opcional, para asegurar que Calendar no quede en la pila
        finish()
    }

    /**
     * Lee users/{uid}, incrementa streakDays del hábito [habitIndex],
     * actualiza también el mapa streaks[title] y guarda en Firestore.
     */
    private fun updateStreakInFirestore(
        onUpdated: (Int) -> Unit,
        onError: () -> Unit
    ) {
        val uid = userId
        if (uid == null || habitIndex < 0) {
            onError()
            return
        }

        val userDocRef = db.collection("users").document(uid)

        userDocRef.get()
            .addOnSuccessListener { snapshot ->
                val habitsList =
                    snapshot.get("habits") as? List<Map<String, Any>> ?: emptyList()

                if (habitIndex !in habitsList.indices) {
                    onError()
                    return@addOnSuccessListener
                }

                // Convertir a lista mutable de mapas mutables
                val mutableHabits = habitsList.map { it.toMutableMap() }.toMutableList()
                val habitMap = mutableHabits[habitIndex]

                val oldStreak = (habitMap["streakDays"] as? Long ?: 0L).toInt()
                val newStreak = oldStreak + 1
                habitMap["streakDays"] = newStreak

                // Actualizar también el mapa streaks (clave = título del hábito)
                val streaksMap = (snapshot.get("streaks") as? Map<String, Any>)
                    ?.toMutableMap() ?: mutableMapOf()

                val titleKey = habitMap["title"] as? String ?: habitTitle
                streaksMap[titleKey] = newStreak

                val dataToUpdate = hashMapOf<String, Any>(
                    "habits" to mutableHabits,
                    "streaks" to streaksMap
                )

                userDocRef
                    .set(dataToUpdate, SetOptions.merge())
                    .addOnSuccessListener {
                        onUpdated(newStreak)
                    }
                    .addOnFailureListener {
                        onError()
                    }
            }
            .addOnFailureListener {
                onError()
            }
    }
}
