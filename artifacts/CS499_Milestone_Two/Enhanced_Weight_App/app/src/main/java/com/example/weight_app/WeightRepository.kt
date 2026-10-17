package com.example.weight_app

/** All SQLite access is behind this boundary, on a background dispatcher. */
class WeightRepository(private val db: AppDatabaseHelper) : AutoCloseable {
    fun login(username: String, password: String): Int? {
        // Existing version 1 accounts may exceed the new registration limits.
        require(username.isNotBlank() && password.isNotEmpty()) { "Enter your username and password." }
        return db.validateLogin(username, password)
    }
    fun register(username: String, password: String): Boolean {
        InputValidator.credentials(username, password)
        return db.createUserIfNotExists(username, password)
    }
    fun entries(user: Int): List<WeightEntry> {
        require(user > 0)
        return db.readAllWeights(user).use { c ->
            buildList {
                while (c.moveToNext()) add(WeightEntry(c.getInt(c.getColumnIndexOrThrow("id")),
                    c.getString(c.getColumnIndexOrThrow("date")), c.getDouble(c.getColumnIndexOrThrow("weight"))))
            }
        }
    }
    fun goal(user: Int): Double? { require(user > 0); return db.getGoal(user) }
    fun add(user: Int, date: String, weight: Double) {
        require(user > 0); InputValidator.entry(date, weight)
        check(db.insertWeight(user, date, weight) > 0) { "Entry could not be saved." }
    }
    fun update(user: Int, entry: Int, date: String, weight: Double) {
        require(user > 0); InputValidator.entry(date, weight)
        check(db.updateWeight(user, entry, date, weight) == 1) { "Entry not found for this user." }
    }
    fun delete(user: Int, entry: Int) {
        require(user > 0)
        check(db.deleteWeight(user, entry) == 1) { "Entry not found for this user." }
    }
    fun saveGoal(user: Int, weight: Double) {
        require(user > 0); require(InputValidator.weight(weight)) { "Enter a goal between 1 and 2000." }
        check(db.upsertGoal(user, weight)) { "Goal could not be saved." }
    }
    override fun close() = db.close()
}
