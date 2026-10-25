package com.example.myapplication

import org.junit.Test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun practiceChoicesContainCorrectAnswerAndStayUniqueInBothDirections() {
        val allWords = lessons.flatMap { it.vocabulary }
        allWords.forEach { word ->
            val forward = createPracticeChoices(word, allWords, reverse = false)
            val reverse = createPracticeChoices(word, allWords, reverse = true)

            assertEquals(4, forward.size)
            assertEquals(4, forward.map { it.title.lowercase() }.distinct().size)
            assertTrue(forward.any { it.title == word.english && it.detail == word.pronunciation })
            assertEquals(4, reverse.size)
            assertEquals(4, reverse.map { it.title.lowercase() }.distinct().size)
            assertTrue(reverse.any { it.title == word.vietnamese })
        }
    }

    @Test
    fun typedAnswersIgnoreCasePunctuationAndExtraSpaces() {
        assertTrue(isPracticeAnswerCorrect("  nice TO meet you! ", "Nice to meet you"))
        assertTrue(isPracticeAnswerCorrect("the   menu, please", "The menu, please"))
        assertFalse(isPracticeAnswerCorrect("nice to see you", "Nice to meet you"))
    }

    @Test
    fun dailyChallengeIsStableForDateAndChangesAcrossDates() {
        val allWords = lessons.flatMap { it.vocabulary }
        val firstRun = createDailyChallengeWords(allWords, "2026-10-25")
        val sameDayRun = createDailyChallengeWords(allWords, "2026-10-25")
        val nextDayRun = createDailyChallengeWords(allWords, "2026-10-26")

        assertEquals(DAILY_CHALLENGE_SIZE, firstRun.size)
        assertEquals(firstRun.map { it.id }, sameDayRun.map { it.id })
        assertEquals(DAILY_CHALLENGE_SIZE, nextDayRun.size)
        assertTrue(firstRun.map { it.id }.toSet() != nextDayRun.map { it.id }.toSet())
    }

    @Test
    fun listeningAnswerAcceptsSpokenWordWithPunctuationDifferences() {
        assertTrue(isPracticeAnswerCorrect("boarding pass", "Boarding pass"))
        assertTrue(isPracticeAnswerCorrect("  I'M GOING TO... ", "I'm going to..."))
        assertFalse(isPracticeAnswerCorrect("boarding", "Boarding pass"))
    }

    @Test
    fun exampleMaskHidesAnswerWithoutChangingOtherText() {
        assertEquals("The wind came from the ***** today.", maskWordInExample("The wind came from the north today.", "north"))
        assertEquals("Gợi nghĩa: commute", maskWordInExample("I take the bus to work.", "commute"))
    }

    @Test
    fun registrationValidatesRequiredFieldsAndMatchingPasswords() {
        assertEquals("Họ tên cần có ít nhất 2 ký tự.", validateRegistration("A", "a@example.com", "password1", "password1"))
        assertEquals("Vui lòng nhập email hợp lệ.", validateRegistration("An", "invalid", "password1", "password1"))
        assertEquals("Mật khẩu cần có ít nhất 8 ký tự.", validateRegistration("An", "a@example.com", "short", "short"))
        assertEquals("Mật khẩu nhập lại chưa khớp.", validateRegistration("An", "a@example.com", "password1", "password2"))
        assertEquals(null, validateRegistration("An", "a@example.com", "password1", "password1"))
    }

    @Test
    fun loginRequiresValidEmailAndPassword() {
        assertEquals("Vui lòng nhập email hợp lệ.", validateLogin("invalid", "password1"))
        assertEquals("Vui lòng nhập mật khẩu.", validateLogin("a@example.com", " "))
        assertEquals(null, validateLogin("a@example.com", "password1"))
    }

    @Test
    fun passwordHashUsesUniqueSaltAndVerifiesCredentials() {
        val firstSalt = PasswordHasher.newSalt()
        val secondSalt = PasswordHasher.newSalt()
        val firstHash = PasswordHasher.hash("password1".toCharArray(), firstSalt)

        assertFalse(firstSalt == secondSalt)
        assertTrue(PasswordHasher.matches("password1".toCharArray(), firstSalt, firstHash))
        assertFalse(PasswordHasher.matches("incorrect".toCharArray(), firstSalt, firstHash))
        assertFalse(firstHash.contains("password1"))
    }

    @Test
    fun courseRoadmapHasFiveStagesAndTwentyCompleteLessons() {
        assertEquals(5, courseStages.size)
        assertEquals(20, lessons.size)
        assertEquals(80, lessons.sumOf { it.vocabulary.size })
        courseStages.forEach { stage ->
            assertTrue(lessons.any { it.stage == stage.number })
        }
        lessons.forEach { lesson ->
            assertTrue(lesson.title.isNotBlank())
            assertTrue(lesson.subtitle.isNotBlank())
            assertEquals(4, lesson.vocabulary.size)
            lesson.vocabulary.forEach { word ->
                assertTrue(word.id.isNotBlank())
                assertTrue(word.pronunciation.isNotBlank())
                assertTrue(word.vietnamese.isNotBlank())
                assertTrue(word.example.isNotBlank())
                assertTrue(word.imageCue.isNotBlank())
            }
        }
        val allWords = lessons.flatMap { it.vocabulary }
        assertEquals(allWords.size, allWords.map { it.id }.distinct().size)
        val north = allWords.first { it.id == "north" }
        assertTrue(north.englishDefinition.contains("four main directions"))
        assertTrue(north.example.contains("north"))
    }

    @Test
    fun flashcardDeckAdvancesAndResetsWhenDueDeckShrinks() {
        assertEquals(1, nextCardIndex(currentIndex = 0, deckSize = 4, filter = 0))
        assertEquals(0, nextCardIndex(currentIndex = 3, deckSize = 4, filter = 0))
        assertEquals(0, nextCardIndex(currentIndex = 2, deckSize = 3, filter = 1))
        assertEquals(0, nextCardIndex(currentIndex = 0, deckSize = 1, filter = 0))
        assertEquals(0, nextCardIndex(currentIndex = 0, deckSize = 0, filter = 1))
    }
}