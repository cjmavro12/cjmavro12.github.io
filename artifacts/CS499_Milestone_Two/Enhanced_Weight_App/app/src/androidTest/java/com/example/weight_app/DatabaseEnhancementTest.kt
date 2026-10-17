package com.example.weight_app

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/** Each test uses its own database, never the user's weight_tracker.db. */
class DatabaseEnhancementTest {
    private lateinit var context: Context
    private lateinit var name: String
    @Before fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        name = "milestone_test_${UUID.randomUUID()}.db"
    }
    @After fun cleanup() { context.deleteDatabase(name) }

    @Test fun migrationPreservesAccountWeightsAndGoal() {
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            old.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL)")
            old.execSQL("CREATE TABLE weights (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, date TEXT NOT NULL, weight REAL NOT NULL, FOREIGN KEY(user_id) REFERENCES users(id))")
            old.execSQL("CREATE TABLE goals (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER UNIQUE NOT NULL, goal_weight REAL NOT NULL, FOREIGN KEY(user_id) REFERENCES users(id))")
            old.execSQL("INSERT INTO users VALUES (7, 'legacy', 'test only password')")
            old.execSQL("INSERT INTO weights VALUES (9, 7, '2026-09-20', 190)")
            old.execSQL("INSERT INTO goals VALUES (3, 7, 170)")
            old.version = 1
        }
        AppDatabaseHelper(context, name).use { db ->
            assertEquals(7, db.validateLogin("legacy", "test only password"))
            assertNull(db.validateLogin("legacy", "wrong"))
            assertEquals(170.0, db.getGoal(7)!!, 0.0)
            db.readAllWeights(7).use { c ->
                assertEquals(1, c.count)
                assertTrue(c.moveToFirst())
                assertEquals(9, c.getInt(c.getColumnIndexOrThrow("id")))
                assertEquals(190.0, c.getDouble(c.getColumnIndexOrThrow("weight")), 0.0)
            }
            db.readableDatabase.rawQuery("SELECT password FROM users WHERE id=7", null).use { c ->
                assertTrue(c.moveToFirst())
                assertTrue(c.getString(0).startsWith("pbkdf2-sha256\$"))
            }
            assertEquals(2, db.readableDatabase.version)
        }
        AppDatabaseHelper(context, name).use { db ->
            assertEquals(7, db.validateLogin("legacy", "test only password"))
            assertEquals(170.0, db.getGoal(7)!!, 0.0)
        }
    }

    @Test fun repositoryCrudAndUserIsolation() {
        WeightRepository(AppDatabaseHelper(context, name)).use { repo ->
            assertTrue(repo.register("alice", "password a"))
            assertFalse(repo.register("alice", "password a"))
            assertTrue(repo.register("bob", "password b"))
            val alice = repo.login("alice", "password a")!!
            val bob = repo.login("bob", "password b")!!
            repo.add(alice, "2026-09-20", 190.0)
            repo.saveGoal(alice, 170.0)
            val entry = repo.entries(alice).single()
            assertTrue(repo.entries(bob).isEmpty())
            assertNull(repo.goal(bob))
            try { repo.delete(bob, entry.id); fail("Cross-user delete allowed") }
            catch (_: IllegalStateException) { }
            try { repo.update(bob, entry.id, entry.date, 180.0); fail("Cross-user update allowed") }
            catch (_: IllegalStateException) { }
            try { repo.add(alice, "2026-02-30", 190.0); fail("Invalid date allowed") }
            catch (_: IllegalArgumentException) { }
            repo.update(alice, entry.id, "2026-09-21", 188.0)
            assertEquals(188.0, repo.entries(alice).single().weight, 0.0)
            repo.delete(alice, entry.id)
            assertTrue(repo.entries(alice).isEmpty())
        }
    }
}
