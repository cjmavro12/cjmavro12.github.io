package com.example.weight_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val app = applicationContext
        val model = ViewModelProvider(this, LoginViewModel.Factory {
            WeightRepository(AppDatabaseHelper(app))
        })[LoginViewModel::class.java]
        val username = findViewById<EditText>(R.id.editTextUsername)
        val password = findViewById<EditText>(R.id.editTextPassword)
        password.isSaveEnabled = false
        val login = findViewById<Button>(R.id.buttonSubmitLogin)
        val create = findViewById<Button>(R.id.buttonCreateAccount)
        login.setOnClickListener { model.submit(username.text.toString(), password.text.toString(), false) }
        create.setOnClickListener { model.submit(username.text.toString(), password.text.toString(), true) }
        model.busy.observe(this) { login.isEnabled = !it; create.isEnabled = !it }
        model.message.observe(this) { text ->
            if (text != null) { model.message.value = null; Toast.makeText(this, text, Toast.LENGTH_LONG).show() }
        }
        model.loggedInUser.observe(this) { id ->
            if (id != null) {
                model.loggedInUser.value = null
                password.text.clear()
                startActivity(Intent(this, DataGridActivity::class.java).putExtra("USER_ID", id))
                finish()
            }
        }
    }
}
