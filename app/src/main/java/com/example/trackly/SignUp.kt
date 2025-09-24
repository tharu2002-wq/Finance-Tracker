package com.example.trackly

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignUp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val emailInput = findViewById<EditText>(R.id.email_input)
        val nameInput = findViewById<EditText>(R.id.name_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val registerButton = findViewById<Button>(R.id.login_button)
        val signInLink = findViewById<TextView>(R.id.sign_up_link)

        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val username = nameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validate fields
            if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidPassword(password)) {
                Toast.makeText(
                    this,
                    "Password must be at least 6 characters, include 1 uppercase letter, 1 number, and 1 symbol",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences("UserCredentials", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("email", email)
                putString("username", username)
                putString("password", password)
                apply()
            }

            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignIn::class.java))
            finish()
        }

        signInLink.setOnClickListener {
            startActivity(Intent(this, SignIn::class.java))
            finish()
        }
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordRegex = Regex("^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#\$%^&+=!]).{6,}\$")
        return passwordRegex.matches(password)
    }
}
