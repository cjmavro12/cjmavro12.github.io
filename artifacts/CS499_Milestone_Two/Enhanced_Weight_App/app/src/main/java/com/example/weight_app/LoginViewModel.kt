package com.example.weight_app

import android.database.sqlite.SQLiteException
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(private val openRepository: () -> WeightRepository) : ViewModel() {
    val busy = MutableLiveData(false)
    val message = MutableLiveData<String?>()
    val loggedInUser = MutableLiveData<Int?>()
    fun submit(username: String, password: String, register: Boolean) {
        if (busy.value == true) return
        busy.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    openRepository().use { repo ->
                        if (register) {
                            if (repo.register(username.trim(), password)) "Account created. Now log in." to null
                            else "That username is already taken." to null
                        } else {
                            val id = repo.login(username.trim(), password)
                            (if (id == null) "Invalid login." else "") to id
                        }
                    }
                }
                message.value = result.first.takeIf { it.isNotEmpty() }
                loggedInUser.value = result.second
            } catch (e: IllegalArgumentException) { message.value = e.message }
            catch (_: SQLiteException) { message.value = "Unable to open the database. Please try again." }
            finally { busy.value = false }
        }
    }
    class Factory(private val openRepository: () -> WeightRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LoginViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(openRepository) as T
        }
    }
}
