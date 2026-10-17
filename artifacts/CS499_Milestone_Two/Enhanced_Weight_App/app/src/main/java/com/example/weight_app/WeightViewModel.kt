package com.example.weight_app

import android.database.sqlite.SQLiteException
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WeightUiState(val entries: List<WeightEntry> = emptyList(), val goal: Double? = null,
    val busy: Boolean = false, val loaded: Boolean = false)
data class WeightEvent(val message: String, val clearEntry: Boolean = false)

/** Screen state survives rotation. No Activity or View is retained. */
class WeightViewModel(private val user: Int, private val openRepository: () -> WeightRepository,
    private val notifyGoal: (Double) -> String) : ViewModel() {
    private val mutableState = MutableLiveData(WeightUiState())
    val state: LiveData<WeightUiState> = mutableState
    private val mutableEvent = MutableLiveData<WeightEvent?>()
    val event: LiveData<WeightEvent?> = mutableEvent
    fun consumeEvent() { mutableEvent.value = null }
    init { refresh() }
    private fun execute(message: String, clear: Boolean = false, weight: Double? = null,
        action: (WeightRepository) -> Unit) {
        if (mutableState.value!!.busy) return
        mutableState.value = mutableState.value!!.copy(busy = true)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    openRepository().use { repo ->
                        action(repo)
                        val goal = repo.goal(user)
                        val notification = if (weight != null && goal != null && weight <= goal) notifyGoal(goal) else ""
                        WeightUiState(repo.entries(user), goal, loaded = true) to notification
                    }
                }
                mutableState.value = result.first
                if (message.isNotEmpty()) mutableEvent.value = WeightEvent(message + result.second, clear)
            } catch (e: IllegalArgumentException) {
                mutableEvent.value = WeightEvent(e.message ?: "Check your input.")
            } catch (e: IllegalStateException) {
                mutableEvent.value = WeightEvent(e.message ?: "Unable to change this entry.")
            } catch (_: SQLiteException) {
                mutableEvent.value = WeightEvent("Database operation failed. Please try again.")
            } finally { mutableState.value = mutableState.value!!.copy(busy = false) }
        }
    }
    fun refresh() = execute("") { }
    fun add(date: String, text: String) {
        val weight = text.toDoubleOrNull()
        if (!InputValidator.weight(weight)) { mutableEvent.value = WeightEvent("Enter a weight between 1 and 2000."); return }
        execute("Weight added.", true, weight) { it.add(user, date.trim(), weight!!) }
    }
    fun update(entry: WeightEntry, date: String, weight: Double) =
        execute("Weight updated.", weight = weight) { it.update(user, entry.id, date.trim(), weight) }
    fun delete(entry: WeightEntry) = execute("Weight deleted.") { it.delete(user, entry.id) }
    fun saveGoal(text: String) {
        val goal = text.toDoubleOrNull()
        if (!InputValidator.weight(goal)) { mutableEvent.value = WeightEvent("Enter a goal between 1 and 2000."); return }
        execute("Goal saved.") { it.saveGoal(user, goal!!) }
    }
    class Factory(private val user: Int, private val openRepository: () -> WeightRepository,
        private val notifyGoal: (Double) -> String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(WeightViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return WeightViewModel(user, openRepository, notifyGoal) as T
        }
    }
}
