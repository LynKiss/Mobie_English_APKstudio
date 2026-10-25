package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class UserProfile(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String,
    val birthDate: String,
    val englishLevel: String,
    val dailyGoalMinutes: Int
)

data class LearningProgress(val completedLessonMask: Long, val correctAnswers: Int)
data class WordReviewState(
    val isFavorite: Boolean = false,
    val correctStreak: Int = 0,
    val dueAtMillis: Long = 0L,
    val reviewCount: Int = 0
)
data class DailyActivity(
    val dateKey: String,
    val studySeconds: Long = 0L,
    val reviewedCards: Int = 0,
    val correctAnswers: Int = 0,
    val completedLessons: Int = 0
) {
    val hasActivity: Boolean
        get() = studySeconds > 0 || reviewedCards > 0 || completedLessons > 0
}

data class StudySummary(
    val currentStreak: Int,
    val today: DailyActivity,
    val lastSevenDays: List<DailyActivity>
)
data class DailyChallengeResult(val dateKey: String, val correctAnswers: Int, val totalQuestions: Int)

class UserDatabase(context: Context, databaseName: String = DATABASE_NAME) :
    SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                password_hash TEXT NOT NULL,
                password_salt TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                birth_date TEXT NOT NULL DEFAULT '',
                english_level TEXT NOT NULL DEFAULT 'Mới bắt đầu',
                daily_goal_minutes INTEGER NOT NULL DEFAULT 15,
                created_at INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE learning_progress (
                user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                completed_lesson_mask INTEGER NOT NULL DEFAULT 0,
                correct_answers INTEGER NOT NULL DEFAULT 0
            )""".trimIndent()
        )
        createWordReviewsTable(db)
        createDailyActivityTable(db)
        createDailyChallengesTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createWordReviewsTable(db)
        if (oldVersion < 3) createDailyActivityTable(db)
        if (oldVersion < 4) createDailyChallengesTable(db)
    }

    fun register(fullName: String, email: String, password: CharArray): UserProfile? {
        val salt = PasswordHasher.newSalt()
        val values = ContentValues().apply {
            put("full_name", fullName.trim())
            put("email", normalizeEmail(email))
            put("password_hash", PasswordHasher.hash(password, salt))
            put("password_salt", salt)
            put("created_at", System.currentTimeMillis())
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val id = db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            if (id == -1L) return null
            db.insertOrThrow("learning_progress", null, ContentValues().apply { put("user_id", id) })
            db.setTransactionSuccessful()
            return getUser(id)
        } finally {
            db.endTransaction()
        }
    }

    fun authenticate(email: String, password: CharArray): UserProfile? {
        readableDatabase.query(
            "users",
            arrayOf("id", "full_name", "email", "password_hash", "password_salt", "phone", "birth_date", "english_level", "daily_goal_minutes"),
            "email = ?",
            arrayOf(normalizeEmail(email)),
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val salt = cursor.getString(cursor.getColumnIndexOrThrow("password_salt"))
            val expectedHash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"))
            if (!PasswordHasher.matches(password, salt, expectedHash)) return null
            return cursor.toProfile()
        }
    }

    fun getUser(userId: Long): UserProfile? = readableDatabase.query(
        "users",
        arrayOf("id", "full_name", "email", "phone", "birth_date", "english_level", "daily_goal_minutes"),
        "id = ?",
        arrayOf(userId.toString()),
        null,
        null,
        null
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toProfile() else null }

    fun updateProfile(profile: UserProfile): Boolean {
        val values = ContentValues().apply {
            put("full_name", profile.fullName.trim())
            put("phone", profile.phone.trim())
            put("birth_date", profile.birthDate.trim())
            put("english_level", profile.englishLevel)
            put("daily_goal_minutes", profile.dailyGoalMinutes)
        }
        return writableDatabase.update("users", values, "id = ?", arrayOf(profile.id.toString())) == 1
    }

    fun getProgress(userId: Long): LearningProgress = readableDatabase.query(
        "learning_progress",
            arrayOf("completed_lesson_mask", "correct_answers"),
        "user_id = ?",
        arrayOf(userId.toString()),
        null,
        null,
        null
    ).use { cursor ->
        if (cursor.moveToFirst()) {
            LearningProgress(cursor.getLong(0), cursor.getInt(1))
        } else {
            LearningProgress(0, 0)
        }
    }

    fun saveProgress(userId: Long, progress: LearningProgress) {
        val values = ContentValues().apply {
            put("user_id", userId)
            put("completed_lesson_mask", progress.completedLessonMask)
            put("correct_answers", progress.correctAnswers)
        }
        writableDatabase.insertWithOnConflict("learning_progress", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun recordLessonCompleted(userId: Long, completedAtMillis: Long = System.currentTimeMillis()) {
        updateDailyActivity(userId, completedAtMillis, completedLessons = 1)
    }

    fun getDailyChallengeResult(userId: Long, atMillis: Long = System.currentTimeMillis()): DailyChallengeResult? {
        val key = dateKey(atMillis)
        return readableDatabase.query(
            "daily_challenges",
            arrayOf("correct_answers", "total_questions"),
            "user_id = ? AND date_key = ?",
            arrayOf(userId.toString(), key),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) DailyChallengeResult(key, cursor.getInt(0), cursor.getInt(1)) else null
        }
    }

    fun saveDailyChallengeResult(userId: Long, correctAnswers: Int, totalQuestions: Int, atMillis: Long = System.currentTimeMillis()) {
        val safeTotal = totalQuestions.coerceAtLeast(1)
        val values = ContentValues().apply {
            put("user_id", userId)
            put("date_key", dateKey(atMillis))
            put("correct_answers", correctAnswers.coerceIn(0, safeTotal))
            put("total_questions", safeTotal)
            put("completed_at", atMillis)
        }
        writableDatabase.insertWithOnConflict("daily_challenges", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun recordStudySeconds(userId: Long, seconds: Long, atMillis: Long = System.currentTimeMillis()) {
        if (seconds > 0) updateDailyActivity(userId, atMillis, studySeconds = seconds)
    }

    fun getStudySummary(userId: Long, nowMillis: Long = System.currentTimeMillis()): StudySummary {
        val nowCalendar = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayKey = dateKey(nowCalendar.timeInMillis)
        val activities = readableDatabase.query(
            "daily_activity",
            arrayOf("date_key", "study_seconds", "reviewed_cards", "correct_answers", "completed_lessons"),
            "user_id = ?",
            arrayOf(userId.toString()),
            null,
            null,
            null
        ).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    val activity = DailyActivity(
                        dateKey = cursor.getString(0),
                        studySeconds = cursor.getLong(1),
                        reviewedCards = cursor.getInt(2),
                        correctAnswers = cursor.getInt(3),
                        completedLessons = cursor.getInt(4)
                    )
                    put(activity.dateKey, activity)
                }
            }
        }

        var streak = 0
        val streakCalendar = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        if (activities[dateKey(streakCalendar.timeInMillis)]?.hasActivity != true) {
            streakCalendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        while (activities[dateKey(streakCalendar.timeInMillis)]?.hasActivity == true) {
            streak++
            streakCalendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }

        val week = (6 downTo 0).map { daysAgo ->
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val key = dateKey(calendar.timeInMillis)
            activities[key] ?: DailyActivity(key)
        }
        return StudySummary(streak, activities[todayKey] ?: DailyActivity(todayKey), week)
    }

    fun getWordReviewStates(userId: Long): Map<String, WordReviewState> = readableDatabase.query(
        "word_reviews",
        arrayOf("word_id", "is_favorite", "correct_streak", "due_at", "review_count"),
        "user_id = ?",
        arrayOf(userId.toString()),
        null,
        null,
        null
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                put(
                    cursor.getString(0),
                    WordReviewState(
                        isFavorite = cursor.getInt(1) != 0,
                        correctStreak = cursor.getInt(2),
                        dueAtMillis = cursor.getLong(3),
                        reviewCount = cursor.getInt(4)
                    )
                )
            }
        }
    }

    fun setWordFavorite(userId: Long, wordId: String, isFavorite: Boolean) {
        val current = getWordReviewStates(userId)[wordId] ?: WordReviewState()
        saveWordReviewState(userId, wordId, current.copy(isFavorite = isFavorite))
    }

    fun recordWordReview(userId: Long, wordId: String, remembered: Boolean): WordReviewState {
        val current = getWordReviewStates(userId)[wordId] ?: WordReviewState()
        val streak = if (remembered) current.correctStreak + 1 else 0
        val interval = when {
            !remembered -> REVIEW_AGAIN_DELAY
            streak == 1 -> DAY_MILLIS
            streak == 2 -> THREE_DAYS_MILLIS
            else -> WEEK_MILLIS
        }
        val updated = current.copy(
            correctStreak = streak,
            dueAtMillis = System.currentTimeMillis() + interval,
            reviewCount = current.reviewCount + 1
        )
        saveWordReviewState(userId, wordId, updated)
        updateDailyActivity(
            userId = userId,
            timestampMillis = System.currentTimeMillis(),
            reviewedCards = 1,
            correctAnswers = if (remembered) 1 else 0
        )
        return updated
    }

    private fun updateDailyActivity(
        userId: Long,
        timestampMillis: Long,
        studySeconds: Long = 0L,
        reviewedCards: Int = 0,
        correctAnswers: Int = 0,
        completedLessons: Int = 0
    ) {
        val key = dateKey(timestampMillis)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL(
                """UPDATE daily_activity SET
                   study_seconds = study_seconds + ?,
                   reviewed_cards = reviewed_cards + ?,
                   correct_answers = correct_answers + ?,
                   completed_lessons = completed_lessons + ?
                   WHERE user_id = ? AND date_key = ?""".trimIndent(),
                arrayOf(studySeconds, reviewedCards, correctAnswers, completedLessons, userId, key)
            )
            val rowExists = db.rawQuery(
                "SELECT 1 FROM daily_activity WHERE user_id = ? AND date_key = ? LIMIT 1",
                arrayOf(userId.toString(), key)
            ).use { it.moveToFirst() }
            if (!rowExists) {
                db.insertOrThrow("daily_activity", null, ContentValues().apply {
                    put("user_id", userId)
                    put("date_key", key)
                    put("study_seconds", studySeconds)
                    put("reviewed_cards", reviewedCards)
                    put("correct_answers", correctAnswers)
                    put("completed_lessons", completedLessons)
                })
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun dateKey(timestampMillis: Long): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(timestampMillis))

    private fun saveWordReviewState(userId: Long, wordId: String, state: WordReviewState) {
        val values = ContentValues().apply {
            put("user_id", userId)
            put("word_id", wordId)
            put("is_favorite", if (state.isFavorite) 1 else 0)
            put("correct_streak", state.correctStreak)
            put("due_at", state.dueAtMillis)
            put("review_count", state.reviewCount)
        }
        writableDatabase.insertWithOnConflict("word_reviews", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun createWordReviewsTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS word_reviews (
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                word_id TEXT NOT NULL,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                correct_streak INTEGER NOT NULL DEFAULT 0,
                due_at INTEGER NOT NULL DEFAULT 0,
                review_count INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (user_id, word_id)
            )""".trimIndent()
        )
    }

    private fun createDailyActivityTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS daily_activity (
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                date_key TEXT NOT NULL,
                study_seconds INTEGER NOT NULL DEFAULT 0,
                reviewed_cards INTEGER NOT NULL DEFAULT 0,
                correct_answers INTEGER NOT NULL DEFAULT 0,
                completed_lessons INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (user_id, date_key)
            )""".trimIndent()
        )
    }

    private fun createDailyChallengesTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS daily_challenges (
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                date_key TEXT NOT NULL,
                correct_answers INTEGER NOT NULL DEFAULT 0,
                total_questions INTEGER NOT NULL DEFAULT 5,
                completed_at INTEGER NOT NULL,
                PRIMARY KEY (user_id, date_key)
            )""".trimIndent()
        )
    }

    private fun android.database.Cursor.toProfile() = UserProfile(
        id = getLong(getColumnIndexOrThrow("id")),
        fullName = getString(getColumnIndexOrThrow("full_name")),
        email = getString(getColumnIndexOrThrow("email")),
        phone = getString(getColumnIndexOrThrow("phone")),
        birthDate = getString(getColumnIndexOrThrow("birth_date")),
        englishLevel = getString(getColumnIndexOrThrow("english_level")),
        dailyGoalMinutes = getInt(getColumnIndexOrThrow("daily_goal_minutes"))
    )

    private fun normalizeEmail(email: String) = email.trim().lowercase()

    companion object {
        private const val DATABASE_NAME = "english_learning.db"
        private const val DATABASE_VERSION = 4
        private const val REVIEW_AGAIN_DELAY = 10 * 60 * 1000L
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private const val THREE_DAYS_MILLIS = 3 * DAY_MILLIS
        private const val WEEK_MILLIS = 7 * DAY_MILLIS
    }
}

internal object PasswordHasher {
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256
    private const val ITERATIONS = 120_000

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(password: CharArray, salt: String): String {
        val saltBytes = salt.hexToBytes()
        val spec = PBEKeySpec(password, saltBytes, ITERATIONS, HASH_BITS)
        saltBytes.fill(0)
        return try {
            val derivedKey = deriveKey(spec).encoded
            try {
                derivedKey.toHex()
            } finally {
                derivedKey.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }

    fun matches(password: CharArray, salt: String, expectedHash: String): Boolean {
        val actual = hash(password, salt).hexToBytes()
        val expected = expectedHash.hexToBytes()
        return try {
            MessageDigest.isEqual(actual, expected)
        } finally {
            actual.fill(0)
            expected.fill(0)
        }
    }

    private fun deriveKey(spec: PBEKeySpec) = try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
    } catch (_: java.security.NoSuchAlgorithmException) {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0)
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
