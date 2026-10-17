package com.example.weight_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

class LoginActivity : ComponentActivity() {

    private lateinit var db: AppDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = AppDatabaseHelper(this)

        val username = findViewById<EditText>(R.id.editTextUsername)
        val password = findViewById<EditText>(R.id.editTextPassword)
        val loginBtn = findViewById<Button>(R.id.buttonSubmitLogin)
        val createBtn = findViewById<Button>(R.id.buttonCreateAccount)

        loginBtn.setOnClickListener {
            val u = username.text.toString().trim()
            val p = password.text.toString().trim()

            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Enter username and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = db.validateLogin(u, p)
            if (userId != null) {
                val intent = Intent(this, DataGridActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Invalid login.", Toast.LENGTH_SHORT).show()
            }
        }

        createBtn.setOnClickListener {
            val u = username.text.toString().trim()
            val p = password.text.toString().trim()

            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Enter username and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val created = db.createUserIfNotExists(u, p)
            if (created) {
                Toast.makeText(this, "Account created. Now log in.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Account already exists. Try logging in.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
