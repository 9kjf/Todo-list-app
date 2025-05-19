package com.example.makani

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val rememberCheckBox = findViewById<CheckBox>(R.id.rememberCheckBox) // ضيفي ID بالـ XML
        val loginButton = findViewById<Button>(R.id.go)

        val isRemembered = sharedPreferences.getBoolean("remember", false)
        if (isRemembered) {
            emailEditText.setText(sharedPreferences.getString("email", ""))
            passwordEditText.setText(sharedPreferences.getString("password", ""))
            rememberCheckBox.isChecked = true
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                            if (rememberCheckBox.isChecked) {
                                val editor = sharedPreferences.edit()
                                editor.putBoolean("remember", true)
                                editor.putString("email", email)
                                editor.putString("password", password)
                                editor.apply()
                            } else {
                                sharedPreferences.edit().clear().apply()
                            }

                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        else {
                            Toast.makeText(
                                this,
                                "Login failed: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
        }
    }
}
