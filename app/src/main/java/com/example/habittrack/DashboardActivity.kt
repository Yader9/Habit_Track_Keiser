package com.example.habittrack

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout

class DashboardActivity : AppCompatActivity() {

    private var totalHabitos = 3
    private var habitosCompletados = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar el tema guardado ANTES de crear la UI
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val darkModeOn = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkModeOn) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // --------- Botón de regreso ---------
        findViewById<View>(R.id.buttonBack).setOnClickListener {
            finish()
        }

        // --------- Switch dark mode ---------
        val switchDarkModeDashboard =
            findViewById<SwitchCompat>(R.id.switchDarkModeDashboard)

        switchDarkModeDashboard.isChecked = darkModeOn

        switchDarkModeDashboard.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // --------- Tabs Activos / Archivados ---------
        val tabLayoutDashboard = findViewById<TabLayout>(R.id.tabLayoutDashboard)
        val layoutActiveHabits = findViewById<LinearLayout>(R.id.layoutActiveHabits)
        val layoutArchivedHabits = findViewById<LinearLayout>(R.id.layoutArchivedHabits)
        val textNoArchived = findViewById<TextView>(R.id.textNoArchived)
        val progressContainer = findViewById<View>(R.id.progressContainer)

        // Progreso
        val progressBarHabits = findViewById<ProgressBar>(R.id.progressBarHabits)
        val textProgress = findViewById<TextView>(R.id.textProgress)

        progressBarHabits.max = totalHabitos
        actualizarProgreso(textProgress, progressBarHabits)

        // Estado inicial
        layoutActiveHabits.visibility = View.VISIBLE
        layoutArchivedHabits.visibility = View.GONE
        progressContainer.visibility = View.VISIBLE

        tabLayoutDashboard.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Activos
                        layoutActiveHabits.visibility = View.VISIBLE
                        layoutArchivedHabits.visibility = View.GONE
                        progressContainer.visibility = View.VISIBLE
                    }
                    1 -> { // Archivados
                        layoutActiveHabits.visibility = View.GONE
                        layoutArchivedHabits.visibility = View.VISIBLE
                        progressContainer.visibility = View.GONE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // --------- Referencias a las cards y checkboxes ---------
        val cardHabit1 = findViewById<MaterialCardView>(R.id.cardHabit1)
        val cardHabit2 = findViewById<MaterialCardView>(R.id.cardHabit2)
        val cardHabit3 = findViewById<MaterialCardView>(R.id.cardHabit3)

        val checkHabit1 = findViewById<CheckBox>(R.id.checkHabit1)
        val checkHabit2 = findViewById<CheckBox>(R.id.checkHabit2)
        val checkHabit3 = findViewById<CheckBox>(R.id.checkHabit3)

        // Títulos y descripciones de racha
        val subtitle1 = findViewById<TextView>(R.id.textHabitSubtitle1)
        val subtitle2 = findViewById<TextView>(R.id.textHabitSubtitle2)
        val subtitle3 = findViewById<TextView>(R.id.textHabitSubtitle3)

        val title1 = findViewById<TextView>(R.id.textHabitTitle1)
        val title2 = findViewById<TextView>(R.id.textHabitTitle2)
        val title3 = findViewById<TextView>(R.id.textHabitTitle3)

        // Al tocar la racha, abrimos CalendarActivity con los datos del hábito
        fun setupStreakClick(subtitle: TextView, title: TextView) {
            subtitle.setOnClickListener {
                val intent = Intent(this, CalendarActivity::class.java)
                intent.putExtra("habit_title", title.text.toString())
                intent.putExtra("habit_streak", subtitle.text.toString())
                startActivity(intent)
            }
        }

        setupStreakClick(subtitle1, title1)
        setupStreakClick(subtitle2, title2)
        setupStreakClick(subtitle3, title3)

        // Layout donde se van a mover los archivados
        val layoutArchived = layoutArchivedHabits

        fun setupHabitCheck(checkBox: CheckBox, card: MaterialCardView) {
            checkBox.setOnClickListener {
                // Si ya está en Archivados, no hacemos nada
                if (card.parent == layoutArchived) {
                    return@setOnClickListener
                }

                // Ocultar el texto "sin archivados" la primera vez
                if (textNoArchived.visibility == View.VISIBLE) {
                    textNoArchived.visibility = View.GONE
                }

                // Mantener el check visual limpio en la lista de archivados
                checkBox.isChecked = false

                // Mover la card de Activos a Archivados
                val parent = card.parent as? ViewGroup
                parent?.removeView(card)
                layoutArchived.addView(card)

                // Actualizar progreso
                habitosCompletados++
                if (habitosCompletados > totalHabitos) {
                    habitosCompletados = totalHabitos
                }
                actualizarProgreso(textProgress, progressBarHabits)
            }
        }

        setupHabitCheck(checkHabit1, cardHabit1)
        setupHabitCheck(checkHabit2, cardHabit2)
        setupHabitCheck(checkHabit3, cardHabit3)
    }

    private fun actualizarProgreso(textView: TextView, progressBar: ProgressBar) {
        progressBar.progress = habitosCompletados
        textView.text = "$habitosCompletados de $totalHabitos hábitos completados"
    }
}

