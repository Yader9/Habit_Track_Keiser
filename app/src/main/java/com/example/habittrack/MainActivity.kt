package com.example.habittrack

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1) Aplicar el tema guardado ANTES de crear la UI
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val darkModeOn = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkModeOn) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ---------- Switch de modo oscuro ----------
        val switchDarkMode = findViewById<SwitchCompat>(R.id.switchDarkMode)
        switchDarkMode.isChecked = darkModeOn

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // Guardar preferencia
            prefs.edit().putBoolean("dark_mode", isChecked).apply()

            // Aplicar el nuevo modo (recrea la actividad UNA sola vez)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // ---------- Tabs LOGIN / SIGN UP ----------
        val tabLayout = findViewById<TabLayout>(R.id.tabLayoutAuth)
        val loginFormLayout = findViewById<View>(R.id.loginFormLayout)
        val signupFormLayout = findViewById<View>(R.id.signupFormLayout)

        // Estado inicial
        loginFormLayout.visibility = View.VISIBLE
        signupFormLayout.visibility = View.GONE

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // LOGIN
                        loginFormLayout.visibility = View.VISIBLE
                        signupFormLayout.visibility = View.GONE
                    }
                    1 -> { // SIGN UP
                        loginFormLayout.visibility = View.GONE
                        signupFormLayout.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // ---------- Botón Log In -> Dashboard ----------
        val buttonLogin = findViewById<MaterialButton>(R.id.buttonLogin)
        buttonLogin.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
    }
}

