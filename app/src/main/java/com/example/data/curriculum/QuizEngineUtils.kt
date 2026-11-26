package com.example.data.curriculum

import com.example.data.model.Question
import com.example.data.model.Quiz
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

data class ShuffledOption(
    val originalKey: String,
    val displayLetter: String,
    val text: String
)

data class McqGradingSummary(
    val correctCount: Int,
    val totalMcqCount: Int,
    val earnedPoints: Int,
    val maxPoints: Int,
    val percentageScore: Double
)

data class EssayGradingSummary(
    val earnedRubricScore: Int,
    val maxRubricScore: Int,
    val percentageScore: Double
)

data class QuizAccessState(
    val canStartNew: Boolean,
    val reason: String
)

object QuizEngineUtils {

    fun quizCategoryLabel(category: String): String = when (category) {
        "LATIHAN_MODUL_1" -> "Kuis Modul 1"
        "LATIHAN_MODUL_2" -> "Kuis Modul 2"
        "POST_TEST" -> "Evaluasi Akhir"
        else -> category
    }

    fun selectQuizQuestions(
        quiz: Quiz,
        allPublishedQuestions: List<Question>,
        studentUid: String,
        attemptNumber: Int,
        existingOrder: List<String> = emptyList()
    ): List<Question> {
        val eligible = allPublishedQuestions
            .filter { it.quizId == quiz.id && it.published }
            .sortedBy { it.orderIndex }

        if (eligible.isEmpty()) return emptyList()

        if (existingOrder.isNotEmpty()) {
            val byId = eligible.associateBy { it.id }
            val ordered = existingOrder.mapNotNull { byId[it] }
            if (ordered.isNotEmpty()) {
                return ordered
            }
        }

        val limit = quiz.questionCount.coerceIn(1, eligible.size)
        val pool = if (quiz.shuffleQuestions) {
            val seed = "${quiz.id}_${studentUid}_att$attemptNumber".hashCode()
            eligible.shuffled(Random(seed))
        } else {
            eligible
        }
        return pool.take(limit)
    }

    fun getShuffledOptionsForQuestion(
        quiz: Quiz,
        question: Question,
        studentUid: String,
        attemptNumber: Int
    ): List<ShuffledOption> {
        val base = listOfNotNull(
            question.optionA?.takeIf { it.isNotBlank() }?.let { "A" to it },
            question.optionB?.takeIf { it.isNotBlank() }?.let { "B" to it },
            question.optionC?.takeIf { it.isNotBlank() }?.let { "C" to it },
            question.optionD?.takeIf { it.isNotBlank() }?.let { "D" to it },
            question.optionE?.takeIf { it.isNotBlank() }?.let { "E" to it }
        )
        if (base.isEmpty()) return emptyList()
        val ordered = if (quiz.shuffleOptions && base.size > 1) {
            val seed = "${quiz.id}_${question.id}_${studentUid}_att$attemptNumber".hashCode()
            base.shuffled(Random(seed))
        } else {
            base
        }
        return ordered.mapIndexed { index, (originalKey, text) ->
            val displayLetter = ('A' + index).toString()
            ShuffledOption(
                originalKey = originalKey,
                displayLetter = displayLetter,
                text = text
            )
        }
    }

    fun computeMcqAutoScore(
        mcqQuestions: List<Question>,
        mcqAnswers: Map<String, String>,
        answerKeys: Map<String, String>
    ): McqGradingSummary {
        if (mcqQuestions.isEmpty()) {
            return McqGradingSummary(
                correctCount = 0,
                totalMcqCount = 0,
                earnedPoints = 0,
                maxPoints = 0,
                percentageScore = 0.0
            )
        }
        var correctCount = 0
        var earnedPoints = 0
        var maxPoints = 0
        mcqQuestions.forEach { q ->
            val weight = q.maxPoints.coerceAtLeast(1)
            maxPoints += weight
            val expected = answerKeys[q.id]?.trim()?.uppercase()
            val chosen = mcqAnswers[q.id]?.trim()?.uppercase()
            if (!expected.isNullOrBlank() && expected == chosen) {
                correctCount++
                earnedPoints += weight
            }
        }
        val pct = if (maxPoints > 0) (earnedPoints * 100.0) / maxPoints else 0.0
        return McqGradingSummary(
            correctCount = correctCount,
            totalMcqCount = mcqQuestions.size,
            earnedPoints = earnedPoints,
            maxPoints = maxPoints,
            percentageScore = pct.coerceIn(0.0, 100.0)
        )
    }

    fun computeEssayManualScore(
        essayQuestions: List<Question>,
        essayItemScores: Map<String, Int>,
        maxRubricPerQuestion: Int = 4
    ): EssayGradingSummary {
        if (essayQuestions.isEmpty()) {
            return EssayGradingSummary(
                earnedRubricScore = 0,
                maxRubricScore = 0,
                percentageScore = 0.0
            )
        }
        val maxPerItem = maxRubricPerQuestion.coerceAtLeast(1)
        val totalMax = essayQuestions.size * maxPerItem
        val earned = essayQuestions.sumOf { q ->
            (essayItemScores[q.id] ?: 0).coerceIn(0, maxPerItem)
        }
        val pct = if (totalMax > 0) (earned * 100.0) / totalMax else 0.0
        return EssayGradingSummary(
            earnedRubricScore = earned,
            maxRubricScore = totalMax,
            percentageScore = pct.coerceIn(0.0, 100.0)
        )
    }

    fun combineQuizFinalScore(
        hasMcq: Boolean,
        hasEssay: Boolean,
        mcqPct: Double,
        essayPct: Double
    ): Double = when {
        hasMcq && hasEssay -> ((mcqPct * 0.6) + (essayPct * 0.4)).coerceIn(0.0, 100.0)
        hasMcq -> mcqPct.coerceIn(0.0, 100.0)
        hasEssay -> essayPct.coerceIn(0.0, 100.0)
        else -> 0.0
    }

    fun isQuizAccessibleNow(
        quiz: Quiz,
        nowMillis: Long = System.currentTimeMillis()
    ): QuizAccessState {
        if (!quiz.published) {
            return QuizAccessState(false, "Evaluasi belum dipublikasikan oleh guru.")
        }
        if (quiz.closed) {
            return QuizAccessState(false, "Evaluasi telah ditutup oleh guru.")
        }
        if (!quiz.openForStudents) {
            return QuizAccessState(false, "Evaluasi belum dibuka oleh guru sesuai jadwal.")
        }
        if (quiz.scheduleStartMillis > 0L && nowMillis < quiz.scheduleStartMillis) {
            return QuizAccessState(false, "Sesi belum dimulai. Jadwal: ${quiz.scheduleLabel}")
        }
        if (quiz.scheduleEndMillis > 0L && nowMillis > quiz.scheduleEndMillis) {
            return QuizAccessState(false, "Waktu jadwal pengerjaan telah berakhir.")
        }
        return QuizAccessState(true, "Terbuka (${quiz.scheduleLabel})")
    }

    fun formatCountdownSeconds(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val minutes = safe / 60
        val seconds = safe % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun roundScore(value: Double): Int = value.roundToInt().coerceIn(0, 100)
}
