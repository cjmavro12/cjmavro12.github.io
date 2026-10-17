package com.example.weight_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** View only: forwards user actions and observes state. */
class DataGridActivity : ComponentActivity() {
    companion object { const val USER_ID_EXTRA = "USER_ID" }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_grid)
        val user = intent.getIntExtra("USER_ID", -1)
        if (user <= 0) { finish(); return }
        val app = applicationContext
        val model = ViewModelProvider(this, WeightViewModel.Factory(user,
            { WeightRepository(AppDatabaseHelper(app)) },
            { _ -> GoalNotifier(app).notify(user) }))[WeightViewModel::class.java]
        val date = findViewById<EditText>(R.id.editTextNewDate)
        val weight = findViewById<EditText>(R.id.editTextNewWeight)
        val goal = findViewById<EditText>(R.id.editTextGoalWeight)
        val add = findViewById<Button>(R.id.buttonAddEntry)
        val save = findViewById<Button>(R.id.buttonSaveGoal)
        val adapter = WeightAdapter(onDelete = { model.delete(it) }, onUpdate = { e, d, w -> model.update(e, d, w) })
        findViewById<RecyclerView>(R.id.recyclerViewGrid).apply {
            layoutManager = LinearLayoutManager(this@DataGridActivity)
            this.adapter = adapter
        }
        add.setOnClickListener { model.add(date.text.toString(), weight.text.toString()) }
        save.setOnClickListener { model.saveGoal(goal.text.toString()) }
        findViewById<Button>(R.id.buttonSmsSettings).setOnClickListener {
            startActivity(Intent(this, SmsPermissionActivity::class.java).putExtra("USER_ID", user))
        }
        var displayedGoal = false
        model.state.observe(this) { state ->
            adapter.submitList(state.entries)
            add.isEnabled = !state.busy
            save.isEnabled = !state.busy
            if (state.loaded && !displayedGoal) {
                if (savedInstanceState == null) state.goal?.let { goal.setText(it.toString()) }
                displayedGoal = true
            }
        }
        model.event.observe(this) { event ->
            if (event != null) {
                model.consumeEvent()
                if (event.clearEntry) { date.text.clear(); weight.text.clear() }
                Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
