package com.example.myapplication

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UserDatabaseInstrumentedTest {
    @Test
    fun accountsAuthenticateAndKeepProgressIsolated() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "user-test-${UUID.randomUUID()}.db"
        val database = UserDatabase(context, databaseName)
        try {
            val first = database.register("An Nguyen", "An@Example.com", "firstPassword".toCharArray())
            val second = database.register("Binh Tran", "binh@example.com", "secondPassword".toCharArray())

            assertNotNull(first)
            assertNotNull(second)
            assertNull(database.register("Duplicate", "an@example.com", "otherPassword".toCharArray()))
            assertEquals(first, database.authenticate("an@example.com", "firstPassword".toCharArray()))
            assertNull(database.authenticate("an@example.com", "wrongPassword".toCharArray()))

            val firstUser = requireNotNull(first)
            val secondUser = requireNotNull(second)
            database.saveProgress(firstUser.id, LearningProgress(completedLessonMask = 3L, correctAnswers = 4))
            assertEquals(LearningProgress(3L, 4), database.getProgress(firstUser.id))
            assertEquals(LearningProgress(0L, 0), database.getProgress(secondUser.id))

            database.setWordFavorite(firstUser.id, "wake up", true)
            assertTrue(database.getWordReviewStates(firstUser.id).getValue("wake up").isFavorite)
            assertFalse(database.getWordReviewStates(secondUser.id).containsKey("wake up"))

            val forgotten = database.recordWordReview(firstUser.id, "wake up", remembered = false)
            assertEquals(0, forgotten.correctStreak)
            assertEquals(1, forgotten.reviewCount)
            assertTrue(forgotten.dueAtMillis >= System.currentTimeMillis() + 9 * 60 * 1000L)

            val remembered = database.recordWordReview(firstUser.id, "wake up", remembered = true)
            assertEquals(1, remembered.correctStreak)
            assertEquals(2, remembered.reviewCount)
            assertTrue(remembered.dueAtMillis >= System.currentTimeMillis() + 23 * 60 * 60 * 1000L)

            val today = java.util.Calendar.getInstance()
            today.set(java.util.Calendar.HOUR_OF_DAY, 12)
            today.set(java.util.Calendar.MINUTE, 0)
            today.set(java.util.Calendar.SECOND, 0)
            today.set(java.util.Calendar.MILLISECOND, 0)
            val now = today.timeInMillis
            repeat(3) { daysAgo ->
                val activityDay = today.clone() as java.util.Calendar
                activityDay.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
                database.recordStudySeconds(firstUser.id, 300L, activityDay.timeInMillis)
            }
            database.recordLessonCompleted(firstUser.id, now)
            val firstSummary = database.getStudySummary(firstUser.id, now)
            assertEquals(3, firstSummary.currentStreak)
            assertEquals(7, firstSummary.lastSevenDays.size)
            assertEquals(900L, firstSummary.lastSevenDays.sumOf { it.studySeconds })
            assertEquals(1, firstSummary.today.completedLessons)
            assertEquals(0, database.getStudySummary(secondUser.id, now).currentStreak)

            database.saveDailyChallengeResult(firstUser.id, correctAnswers = 4, totalQuestions = 5, atMillis = now)
            assertEquals(DailyChallengeResult(
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(now)),
                4,
                5
            ), database.getDailyChallengeResult(firstUser.id, now))
            assertNull(database.getDailyChallengeResult(secondUser.id, now))

            val updated = firstUser.copy(
                fullName = "An Nguyen Updated",
                phone = "0900000000",
                birthDate = "01/01/2000",
                englishLevel = "Cơ bản",
                dailyGoalMinutes = 20
            )
            assertTrue(database.updateProfile(updated))
            assertEquals(updated, database.getUser(firstUser.id))

            val storedHash = database.readableDatabase.query(
                "users",
                arrayOf("password_hash"),
                "id = ?",
                arrayOf(firstUser.id.toString()),
                null,
                null,
                null
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                cursor.getString(0)
            }
            assertFalse(storedHash.contains("firstPassword"))
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
