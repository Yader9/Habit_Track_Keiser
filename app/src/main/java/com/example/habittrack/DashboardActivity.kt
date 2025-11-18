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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class DashboardActivity : AppCompatActivity() {

    // Modelo simple en memoria
    data class Habit(
        var title: String = "",
        var archived: Boolean = false,
        var streakDays: Int = 0
    )

    // Firestore y usuario actual
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null

    // Lista de hábitos en memoria
    private val habits = mutableListOf<Habit>()

    // Progreso global
    private var totalHabitos = 0
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

        // Firestore + UID
        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid

        // --------- Referencias de vistas ---------
        val buttonBack = findViewById<View>(R.id.buttonBack)

        val tabLayoutDashboard = findViewById<TabLayout>(R.id.tabLayoutDashboard)
        val layoutActiveHabits = findViewById<LinearLayout>(R.id.layoutActiveHabits)
        val layoutArchivedHabits = findViewById<LinearLayout>(R.id.layoutArchivedHabits)
        val textNoArchived = findViewById<TextView>(R.id.textNoArchived)
        val progressContainer = findViewById<View>(R.id.progressContainer)

        val progressBarHabits = findViewById<ProgressBar>(R.id.progressBarHabits)
        val textProgress = findViewById<TextView>(R.id.textProgress)

        val buttonAddHabit = findViewById<MaterialButton>(R.id.buttonAddHabit)

        // --------- Botón de regreso ---------
        buttonBack.setOnClickListener { finish() }

        // --------- Tabs Activos / Archivados ---------
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

        // --------- Cargar hábitos desde Firestore y construir UI ---------
        initUserDataAndLoadHabits(
            layoutActiveHabits,
            layoutArchivedHabits,
            textNoArchived,
            progressBarHabits,
            textProgress
        )

        // --------- Botón "Agregar nuevo hábito" ---------
        buttonAddHabit.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_habit, null)
            val editTextHabitName =
                dialogView.findViewById<TextInputEditText>(R.id.editTextHabitName)

            MaterialAlertDialogBuilder(this)
                .setTitle("Nuevo hábito")
                .setView(dialogView)
                .setPositiveButton("Guardar") { dialog, _ ->
                    val titulo = editTextHabitName.text?.toString()?.trim()

                    if (!titulo.isNullOrEmpty()) {
                        // 1) Agregar a la lista en memoria
                        val newHabit = Habit(title = titulo, archived = false, streakDays = 0)
                        habits.add(newHabit)

                        // 2) Actualizar contadores
                        totalHabitos = habits.size
                        habitosCompletados = habits.count { it.archived }

                        // 3) Crear su card y añadirla al layout de activos
                        val index = habits.size - 1
                        val newCard = createHabitCard(
                            layoutActiveHabits,
                            layoutArchivedHabits,
                            index,
                            textNoArchived,
                            textProgress,
                            progressBarHabits
                        )
                        layoutActiveHabits.addView(newCard)

                        // 4) Actualizar progreso visual
                        progressBarHabits.max = totalHabitos
                        actualizarProgreso(textProgress, progressBarHabits)

                        // 5) Guardar lista completa en Firestore
                        saveHabitsToFirestore()
                    }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    // =================== Carga inicial ===================

    private fun initUserDataAndLoadHabits(
        layoutActiveHabits: LinearLayout,
        layoutArchivedHabits: LinearLayout,
        textNoArchived: TextView,
        progressBarHabits: ProgressBar,
        textProgress: TextView
    ) {
        val uid = userId ?: return
        val userDocRef = db.collection("users").document(uid)

        userDocRef.get()
            .addOnSuccessListener { snapshot ->
                habits.clear()

                if (snapshot.exists()) {
                    val habitsList =
                        snapshot.get("habits") as? List<Map<String, Any>> ?: emptyList()

                    for (map in habitsList) {
                        val title = map["title"] as? String ?: continue
                        val archived = map["archived"] as? Boolean ?: false
                        val streakDays = (map["streakDays"] as? Long ?: 0L).toInt()

                        habits.add(Habit(title, archived, streakDays))
                    }
                }

                // Si no había hábitos en Firestore, creamos los 3 por defecto
                if (habits.isEmpty()) {
                    habits.add(Habit("Tomar 8 vasos de agua", false, 0))
                    habits.add(Habit("Leer 15 minutos", false, 0))
                    habits.add(Habit("Caminar 5,000 pasos", false, 0))
                }

                // Construir UI y guardar estado coherente en Firestore
                buildHabitsUI(
                    layoutActiveHabits,
                    layoutArchivedHabits,
                    textNoArchived,
                    progressBarHabits,
                    textProgress
                )
                saveHabitsToFirestore()
            }
            .addOnFailureListener {
                // En caso de error, mostramos solo los 3 por defecto en local
                if (habits.isEmpty()) {
                    habits.add(Habit("Tomar 8 vasos de agua", false, 0))
                    habits.add(Habit("Leer 15 minutos", false, 0))
                    habits.add(Habit("Caminar 5,000 pasos", false, 0))
                }
                buildHabitsUI(
                    layoutActiveHabits,
                    layoutArchivedHabits,
                    textNoArchived,
                    progressBarHabits,
                    textProgress
                )
            }
    }

    private fun buildHabitsUI(
        layoutActiveHabits: LinearLayout,
        layoutArchivedHabits: LinearLayout,
        textNoArchived: TextView,
        progressBarHabits: ProgressBar,
        textProgress: TextView
    ) {
        // Limpiar layouts (esto también elimina las cards hardcodeadas del XML)
        layoutActiveHabits.removeAllViews()
        layoutArchivedHabits.removeAllViews()
        textNoArchived.visibility = View.VISIBLE

        habits.forEachIndexed { index, habit ->
            val card = createHabitCard(
                layoutActiveHabits,
                layoutArchivedHabits,
                index,
                textNoArchived,
                textProgress,
                progressBarHabits
            )

            if (habit.archived) {
                layoutArchivedHabits.addView(card)
                textNoArchived.visibility = View.GONE
            } else {
                layoutActiveHabits.addView(card)
            }
        }

        totalHabitos = habits.size
        habitosCompletados = habits.count { it.archived }

        progressBarHabits.max = totalHabitos
        actualizarProgreso(textProgress, progressBarHabits)
    }

    // =================== Creación y comportamiento de cada card ===================

    private fun createHabitCard(
        parentActive: LinearLayout,
        parentArchived: LinearLayout,
        habitIndex: Int,
        textNoArchived: TextView,
        textProgress: TextView,
        progressBarHabits: ProgressBar
    ): MaterialCardView {
        val habit = habits[habitIndex]

        val card = layoutInflater.inflate(
            R.layout.item_habit_card,
            parentActive,
            false
        ) as MaterialCardView

        val titleView = card.findViewById<TextView>(R.id.textHabitTitleDynamic)
        val subtitleView = card.findViewById<TextView>(R.id.textHabitSubtitleDynamic)
        val checkBox = card.findViewById<CheckBox>(R.id.checkHabitDynamic)

        titleView.text = habit.title
        subtitleView.text = "Racha: ${habit.streakDays} días"
        checkBox.isChecked = false

        // Abrir CalendarActivity al tocar la racha
        subtitleView.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            intent.putExtra("habit_title", habit.title)
            intent.putExtra("habit_index", habitIndex)
            intent.putExtra("habit_streak_days", habit.streakDays)
            startActivity(intent)
        }


        // Al tocar el checkbox archivamos el hábito
        checkBox.setOnClickListener {
            val h = habits[habitIndex]

            // Si ya está archivado, no hacemos nada (y limpiamos el check visual)
            if (h.archived) {
                checkBox.isChecked = false
                return@setOnClickListener
            }

            // Ocultar el texto de "sin archivados" la primera vez
            if (textNoArchived.visibility == View.VISIBLE) {
                textNoArchived.visibility = View.GONE
            }

            // Actualizar estado en memoria
            h.archived = true

            // Mover la card de activos a archivados
            val parent = card.parent as? ViewGroup
            parent?.removeView(card)
            parentArchived.addView(card)

            // Mantener el checkbox desmarcado en la lista de archivados
            checkBox.isChecked = false

            // Recalcular progreso y guardar en Firestore
            totalHabitos = habits.size
            habitosCompletados = habits.count { it.archived }
            progressBarHabits.max = totalHabitos
            actualizarProgreso(textProgress, progressBarHabits)
            saveHabitsToFirestore()
        }

        return card
    }

    // =================== Persistencia en Firestore ===================

    private fun actualizarProgreso(textView: TextView, progressBar: ProgressBar) {
        progressBar.progress = habitosCompletados
        textView.text = "$habitosCompletados de $totalHabitos hábitos completados"
    }

    private fun saveHabitsToFirestore() {
        val uid = userId ?: return
        val userDocRef = db.collection("users").document(uid)

        val habitsData = habits.map { habit ->
            hashMapOf(
                "title" to habit.title,
                "archived" to habit.archived,
                "streakDays" to habit.streakDays
            )
        }

        val data = hashMapOf(
            "habits" to habitsData,
            "totalHabits" to totalHabitos,
            "completedHabits" to habitosCompletados
        )

        userDocRef.set(data, SetOptions.merge())
    }
    override fun onResume() {
        super.onResume()

        val layoutActiveHabits = findViewById<LinearLayout>(R.id.layoutActiveHabits)
        val layoutArchivedHabits = findViewById<LinearLayout>(R.id.layoutArchivedHabits)
        val textNoArchived = findViewById<TextView>(R.id.textNoArchived)
        val progressBarHabits = findViewById<ProgressBar>(R.id.progressBarHabits)
        val textProgress = findViewById<TextView>(R.id.textProgress)

        // Vuelve a leer users/{uid} y reconstruye las cards,
        // incluyendo las rachas que actualizamos desde CalendarActivity
        initUserDataAndLoadHabits(
            layoutActiveHabits,
            layoutArchivedHabits,
            textNoArchived,
            progressBarHabits,
            textProgress
        )
    }

}
