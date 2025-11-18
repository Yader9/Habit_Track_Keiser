package com.example.habittrack

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private var auth : FirebaseAuth = FirebaseAuth.getInstance() // Inicializa Firebase Authentication

    //Campos de texto para capturar email y contraseña
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText

    private lateinit var editTextNameSignup: TextInputEditText

    private lateinit var emailEditTextSignup: TextInputEditText

    private lateinit var passwordEditTextSignup: TextInputEditText

    private lateinit var confirmPasswordSignupEditText: TextInputEditText


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

        emailEditText = findViewById<TextInputEditText>(R.id.editTextEmail)
        passwordEditText = findViewById<TextInputEditText>(R.id.editTextPassword)
        editTextNameSignup = findViewById<TextInputEditText>(R.id.editTextNameSignup)
        emailEditTextSignup = findViewById<TextInputEditText>(R.id.editTextEmailSignup)
        passwordEditTextSignup = findViewById<TextInputEditText>(R.id.editTextPasswordSignup)
        confirmPasswordSignupEditText = findViewById<TextInputEditText>(R.id.editTextConfirmPasswordSignup)


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
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            logIn(email, password)

        }

        // Boton Sign Up || Llama a singUp y registra
        val buttonSignup = findViewById<MaterialButton>(R.id.buttonSignup)
        buttonSignup.setOnClickListener {
            val email = emailEditTextSignup.text.toString().trim()
            val password = passwordEditTextSignup.text.toString().trim()
            val confirmPassword = confirmPasswordSignupEditText.text.toString().trim()
            signUp(email, password, confirmPassword)
        }
    }

        // Validacion de datos y confirmacion de contraseña || Boolean: indica si los datos cumplen las reglas

        private fun validate(email : String, password: String): Boolean {

            if (email.isEmpty()){
                Toast.makeText(this, "El correo no puede estar vacio", Toast.LENGTH_SHORT).show()
                return false //Si un campo no cumple las condiciones
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                Toast.makeText(this, "Formato de correo incorrecto", Toast.LENGTH_SHORT).show()
                return false
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "La contraseña no puede estar vacia", Toast.LENGTH_SHORT).show()
                return false
            }

            if (password.length < 6){
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return false
            }

            if (password.length > 20){
                Toast.makeText(this, "La contraseña no puede superar los 20 caracteres", Toast.LENGTH_SHORT).show()
                return false
            }

            return true //Si pasa las validacion
        }

        /*signUp= Llaman a validate y si cumple con los requisitos en signUp se registra el usuario
          y cambia de tab a login para que el usuario inicie sesion

          logIn= Llama a validacion y si cumple con los requisitos te redirige al siguiente dashboard
         */
        private fun signUp(email: String, password: String, confirmPassword: String){
            val name = editTextNameSignup.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "El nombre es obligatorio para registrarse", Toast.LENGTH_SHORT).show()
                return
            }

            if (!validate(email, password)){
                Toast.makeText(this, "Datos inválidos", Toast.LENGTH_SHORT).show()
                return
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                        // Redirigir a Log in
                        val tabLayout = findViewById<TabLayout>(R.id.tabLayoutAuth)
                        tabLayout.getTabAt(0)?.select()
                        //Limpieza de datos
                        editTextNameSignup.setText("")
                        emailEditTextSignup.setText("")
                        passwordEditTextSignup.setText("")
                        confirmPasswordSignupEditText.setText("")


                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        private fun logIn(email: String, password: String){

            if (!validate(email, password)){
                Toast.makeText(this, "Datos inválidos", Toast.LENGTH_SHORT).show()
                return
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }

        }

    override fun onResume() {
        super.onResume()
        emailEditText.setText("")
        passwordEditText.setText("")
    }

    }

