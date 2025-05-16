package com.example.makani

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class welcome : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.welcome)
        val start : Button = findViewById(R.id.startesbt)
        start.setOnClickListener {
            val intent = Intent(this, HomeActivity ::class.java)
            startActivity(intent)
        }

    }
}