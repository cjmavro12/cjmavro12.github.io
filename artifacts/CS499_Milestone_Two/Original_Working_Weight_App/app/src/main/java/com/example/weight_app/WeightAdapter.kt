package com.example.weight_app

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class WeightAdapter(
    private val items: MutableList<WeightEntry>,
    private val onDelete: (WeightEntry) -> Unit,
    private val onUpdate: (WeightEntry, String, Double) -> Unit
) : RecyclerView.Adapter<WeightAdapter.WeightViewHolder>() {

    class WeightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.textDate)
        val weightText: TextView = itemView.findViewById(R.id.textWeight)
        val deleteBtn: Button = itemView.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeightViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data_row, parent, false)
        return WeightViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeightViewHolder, position: Int) {
        val item = items[position]

        holder.dateText.text = item.date
        holder.weightText.text = item.weight.toString()


        holder.deleteBtn.setOnClickListener {
            onDelete(item)
        }


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_edit_weight, null)

            val dateInput = dialogView.findViewById<EditText>(R.id.editDialogDate)
            val weightInput = dialogView.findViewById<EditText>(R.id.editDialogWeight)

            dateInput.setText(item.date)
            weightInput.setText(item.weight.toString())

            AlertDialog.Builder(context)
                .setTitle("Update Entry")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val newDate = dateInput.text.toString().trim()
                    val newWeightStr = weightInput.text.toString().trim()
                    val newWeight = newWeightStr.toDoubleOrNull()

                    if (newDate.isNotEmpty() && newWeight != null) {
                        onUpdate(item, newDate, newWeight)
                    } else {
                        Toast.makeText(
                            context,
                            "Enter a valid date and weight.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<WeightEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
