package com.example.data.repository

import android.content.Context
import com.example.data.curriculum.ParijotoAssessmentsAndGlossary
import com.example.data.curriculum.QuizEngineUtils
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.GradeRecord
import com.example.data.model.Question
import com.example.data.model.Quiz
import com.example.data.model.QuizAnswerKey
import com.example.data.model.QuizAttempt
import com.example.data.model.Rubric
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class QuizAndEvaluationRepository(context: Context) {

    private val db: FirebaseFirestore = FirestoreClientProvider.get(context)

    private val defaultQuizzes = listOf(
        ParijotoAssessmentsAndGlossary.quizModul1,
        ParijotoAssessmentsAndGlossary.quizModul2,
        ParijotoAssessmentsAndGlossary.quizPostTest
    )

    private val defaultQuestions = ParijotoAssessmentsAndGlossary.modul1Questions +
        ParijotoAssessmentsAndGlossary.modul2Questions +
        ParijotoAssessmentsAndGlossary.postTestQuestions

    fun observePublishedQuizzes(): Flow<List<Quiz>> =
        db.collection("quizzes")
            .whereEqualTo("published", true)
            .observeListFlow<Quiz>("quizzes")
            .map { remote ->
                val merged = defaultQuizzes.associateBy { it.id }.toMutableMap()
                remote.forEach { merged[it.id] = it }
                merged.values.toList()
            }

    fun observeTeacherQuizzes(teacherUid: String): Flow<List<Quiz>> =
        combine(
            db.collection("quizzes").whereEqualTo("published", true).observeListFlow<Quiz>("quizzes"),
            db.collection("quizzes").whereEqualTo("ownerUid", teacherUid).observeListFlow<Quiz>("quizzes")
        ) { pubList, ownList ->
            val merged = defaultQuizzes.associateBy { it.id }.toMutableMap()
            pubList.forEach { merged[it.id] = it }
            ownList.forEach { merged[it.id] = it }
            merged.values.toList()
        }

    fun observePublishedQuestions(): Flow<List<Question>> =
        db.collection("questions")
            .whereEqualTo("published", true)
            .observeListFlow<Question>("questions")
            .map { remote ->
                val merged = defaultQuestions.associateBy { it.id }.toMutableMap()
                remote.forEach { merged[it.id] = it }
                merged.values.sortedWith(compareBy({ it.quizId }, { it.orderIndex }))
            }

    fun observeTeacherQuestions(teacherUid: String): Flow<List<Question>> =
        combine(
            db.collection("questions").whereEqualTo("published", true).observeListFlow<Question>("questions"),
            db.collection("questions").whereEqualTo("ownerUid", teacherUid).observeListFlow<Question>("questions")
        ) { pubList, ownList ->
            val merged = defaultQuestions.associateBy { it.id }.toMutableMap()
            pubList.forEach { merged[it.id] = it }
            ownList.forEach { merged[it.id] = it }
            merged.values.sortedWith(compareBy({ it.quizId }, { it.orderIndex }))
        }

    suspend fun saveQuizConfig(quiz: Quiz, ownerUid: String): Result<Unit> =
        runFirestoreWrite(OperationType.WRITE, "quizzes/${quiz.id}") {
            val ref = db.collection("quizzes").document(quiz.id)
            val snap = ref.get().await()
            val payload = mutableMapOf<String, Any>(
                "classId" to quiz.classId.ifBlank { "DEFAULT" },
                "moduleId" to quiz.moduleId,
                "ownerUid" to ownerUid,
                "title" to quiz.title.trim().take(200),
                "category" to quiz.category,
                "durationMinutes" to quiz.durationMinutes.coerceIn(5, 180),
                "questionCount" to quiz.questionCount.coerceIn(1, 100),
                "maxAttempts" to quiz.maxAttempts.coerceIn(1, 10),
                "shuffleQuestions" to quiz.shuffleQuestions,
                "shuffleOptions" to quiz.shuffleOptions,
                "openForStudents" to quiz.openForStudents,
                "scheduleLabel" to quiz.scheduleLabel.trim().ifBlank { "Fleksibel" }.take(120),
                "scheduleStartMillis" to quiz.scheduleStartMillis.coerceAtLeast(0L),
                "scheduleEndMillis" to quiz.scheduleEndMillis.coerceAtLeast(0L),
                "resultsPublished" to quiz.resultsPublished,
                "closed" to quiz.closed,
                "showDiscussion" to quiz.showDiscussion,
                "published" to quiz.published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!snap.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                ref.set(payload).await()
            } else {
                ref.set(payload, SetOptions.merge()).await()
            }
            ensureMasterKeyExists(quiz.id, ownerUid)
        }

    suspend fun toggleQuizEvaluationState(
        quiz: Quiz,
        ownerUid: String,
        closed: Boolean,
        showDiscussion: Boolean,
        resultsPublished: Boolean = quiz.resultsPublished
    ): Result<Unit> = saveQuizConfig(
        quiz = quiz.copy(
            closed = closed,
            showDiscussion = showDiscussion,
            resultsPublished = resultsPublished
        ),
        ownerUid = ownerUid
    )

    suspend fun saveBankQuestion(
        quiz: Quiz,
        question: Question,
        correctOption: String?,
        explanation: String?,
        ownerUid: String
    ): Result<Unit> {
        val isNew = question.id.isBlank()
        val qId = question.id.ifBlank { "q_${UUID.randomUUID().toString().take(8)}" }
        return runFirestoreWrite(if (isNew) OperationType.CREATE else OperationType.UPDATE, "questions/$qId") {
            val quizRef = db.collection("quizzes").document(quiz.id)
            if (!quizRef.get().await().exists()) {
                saveQuizConfig(quiz, ownerUid).getOrThrow()
            }

            val qRef = db.collection("questions").document(qId)
            val existingSnap = qRef.get().await()
            val qPayload = mutableMapOf<String, Any>(
                "quizId" to quiz.id,
                "orderIndex" to question.orderIndex.coerceIn(1, 100),
                "type" to question.type,
                "prompt" to question.prompt.trim().take(3000),
                "maxPoints" to question.maxPoints.coerceIn(1, 100),
                "ownerUid" to ownerUid,
                "published" to question.published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existingSnap.exists()) {
                qPayload["createdAt"] = FieldValue.serverTimestamp()
            }
            question.stimulusText?.trim()?.takeIf { it.isNotBlank() }?.let { qPayload["stimulusText"] = it.take(4000) }
            question.optionA?.trim()?.takeIf { it.isNotBlank() }?.let { qPayload["optionA"] = it.take(1000) }
            question.optionB?.trim()?.takeIf { it.isNotBlank() }?.let { qPayload["optionB"] = it.take(1000) }
            question.optionC?.trim()?.takeIf { it.isNotBlank() }?.let { qPayload["optionC"] = it.take(1000) }
            question.optionD?.trim()?.takeIf { it.isNotBlank() }?.let { qPayload["optionD"] = it.take(1000) }
            question.optionE?.trim()?.takeIf { it.isNotBlank() }?.let { qPayload["optionE"] = it.take(1000) }
            question.hotsCategory?.trim()?.takeIf { it.isNotBlank() }?.let { qPayload["hotsCategory"] = it.take(60) }

            qRef.set(qPayload, SetOptions.merge()).await()

            val keyRef = quizRef.collection("answer_keys").document("master")
            val keySnap = keyRef.get().await()
            val mergedKeys = (ParijotoAssessmentsAndGlossary.defaultAnswerKeys[quiz.id] ?: emptyMap()).toMutableMap()
            val mergedExpl = (ParijotoAssessmentsAndGlossary.defaultExplanations[quiz.id] ?: emptyMap()).toMutableMap()
            if (keySnap.exists()) {
                val existing = keySnap.toObject(QuizAnswerKey::class.java)
                if (existing != null) {
                    mergedKeys.putAll(existing.keys)
                    mergedExpl.putAll(existing.explanations)
                }
            }
            if (!correctOption.isNullOrBlank()) mergedKeys[qId] = correctOption.trim().uppercase()
            if (!explanation.isNullOrBlank()) mergedExpl[qId] = explanation.trim().take(2000)
            val keyPayload = mutableMapOf<String, Any>(
                "ownerUid" to (keySnap.getString("ownerUid") ?: ownerUid),
                "keys" to mergedKeys,
                "explanations" to mergedExpl,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!keySnap.exists()) {
                keyPayload["createdAt"] = FieldValue.serverTimestamp()
            }
            keyRef.set(keyPayload, SetOptions.merge()).await()
        }
    }

    suspend fun deleteBankQuestion(questionId: String): Result<Unit> =
        runFirestoreWrite(OperationType.DELETE, "questions/$questionId") {
            db.collection("questions").document(questionId).delete().await()
        }

    private suspend fun ensureMasterKeyExists(quizId: String, ownerUid: String) {
        val keyRef = db.collection("quizzes").document(quizId).collection("answer_keys").document("master")
        if (!keyRef.get().await().exists()) {
            val defaultKeys = ParijotoAssessmentsAndGlossary.defaultAnswerKeys[quizId] ?: emptyMap()
            val defaultExpl = ParijotoAssessmentsAndGlossary.defaultExplanations[quizId] ?: emptyMap()
            if (defaultKeys.isNotEmpty() || defaultExpl.isNotEmpty()) {
                keyRef.set(
                    mapOf(
                        "ownerUid" to ownerUid,
                        "keys" to defaultKeys,
                        "explanations" to defaultExpl,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    suspend fun getAnswerKeyIfAllowed(quiz: Quiz): Result<QuizAnswerKey?> {
        if (!(quiz.closed && quiz.showDiscussion && quiz.resultsPublished)) {
            return Result.success(null)
        }
        return try {
            val snap = db.collection("quizzes")
                .document(quiz.id)
                .collection("answer_keys")
                .document("master")
                .get()
                .await()
            val remote = if (snap.exists()) {
                snap.toObject(QuizAnswerKey::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            if (remote != null) {
                Result.success(remote)
            } else {
                val keys = ParijotoAssessmentsAndGlossary.defaultAnswerKeys[quiz.id] ?: emptyMap()
                val expl = ParijotoAssessmentsAndGlossary.defaultExplanations[quiz.id] ?: emptyMap()
                Result.success(QuizAnswerKey(id = "master", keys = keys, explanations = expl))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "quizzes/${quiz.id}/answer_keys/master")
            Result.failure(e)
        }
    }

    suspend fun getAnswerKeyForTeacher(quiz: Quiz): Result<QuizAnswerKey> {
        return try {
            val snap = db.collection("quizzes")
                .document(quiz.id)
                .collection("answer_keys")
                .document("master")
                .get()
                .await()
            val remote = if (snap.exists()) {
                snap.toObject(QuizAnswerKey::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            val mergedKeys = (ParijotoAssessmentsAndGlossary.defaultAnswerKeys[quiz.id] ?: emptyMap()) +
                (remote?.keys ?: emptyMap())
            val mergedExpl = (ParijotoAssessmentsAndGlossary.defaultExplanations[quiz.id] ?: emptyMap()) +
                (remote?.explanations ?: emptyMap())
            Result.success(QuizAnswerKey(id = "master", keys = mergedKeys, explanations = mergedExpl))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "quizzes/${quiz.id}/answer_keys/master")
            Result.failure(e)
        }
    }

    suspend fun autosaveQuizAttempt(
        quiz: Quiz,
        attemptNumber: Int,
        questionOrder: List<String>,
        totalMcqCount: Int,
        remainingSeconds: Int,
        classId: String,
        teacherUid: String,
        studentUid: String,
        studentName: String,
        mcqAnswers: Map<String, String>,
        essayAnswers: Map<String, String>
    ): Result<Unit> {
        val attemptId = "${quiz.id}_${studentUid}_att${attemptNumber.coerceIn(1, 10)}"
        return runFirestoreWrite(OperationType.WRITE, "quiz_attempts/$attemptId") {
            val ref = db.collection("quiz_attempts").document(attemptId)
            val snap = ref.get().await()
            val existingStatus = snap.getString("status")
            if (snap.exists() && existingStatus != "IN_PROGRESS") {
                return@runFirestoreWrite
            }
            if (!snap.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "quizId" to quiz.id,
                    "classId" to classId.ifBlank { "DEFAULT" },
                    "teacherUid" to teacherUid.ifBlank { "SYSTEM_DEFAULT" },
                    "studentUid" to studentUid,
                    "studentName" to studentName.trim().take(100),
                    "attemptNumber" to attemptNumber.coerceIn(1, 10),
                    "answers" to mcqAnswers,
                    "essayAnswers" to essayAnswers,
                    "questionOrder" to questionOrder.take(100),
                    "remainingSeconds" to remainingSeconds.coerceAtLeast(0),
                    "autoScore" to 0.0,
                    "essayScore" to 0.0,
                    "totalScore" to 0.0,
                    "correctCount" to 0,
                    "totalMcqCount" to totalMcqCount.coerceAtLeast(0),
                    "resultsPublished" to false,
                    "status" to "IN_PROGRESS",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            } else {
                val updates = mapOf<String, Any>(
                    "answers" to mcqAnswers,
                    "essayAnswers" to essayAnswers,
                    "questionOrder" to questionOrder.take(100),
                    "remainingSeconds" to remainingSeconds.coerceAtLeast(0),
                    "autoScore" to 0.0,
                    "totalScore" to 0.0,
                    "correctCount" to 0,
                    "totalMcqCount" to totalMcqCount.coerceAtLeast(0),
                    "resultsPublished" to false,
                    "status" to "IN_PROGRESS",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.update(updates).await()
            }
        }
    }

    suspend fun submitQuizAttempt(
        quiz: Quiz,
        questions: List<Question>,
        attemptNumber: Int,
        questionOrder: List<String>,
        remainingSeconds: Int,
        classId: String,
        teacherUid: String,
        studentUid: String,
        studentName: String,
        mcqAnswers: Map<String, String>,
        essayAnswers: Map<String, String>
    ): Result<QuizAttempt> {
        val safeAttemptNum = attemptNumber.coerceIn(1, 10)
        val attemptId = "${quiz.id}_${studentUid}_att$safeAttemptNum"
        val mcqQuestions = questions.filter { it.type == "MULTIPLE_CHOICE" }
        val hasEssay = questions.any { it.type == "ESSAY" }

        // Only reveal immediate auto-score if teacher explicitly enabled resultsPublished & closed & showDiscussion
        val canRevealAutoScoreNow = quiz.resultsPublished && quiz.closed && quiz.showDiscussion && !hasEssay
        var autoScore = 0.0
        var correctCount = 0
        if (canRevealAutoScoreNow) {
            val keyResult = getAnswerKeyIfAllowed(quiz).getOrNull()
            if (keyResult != null) {
                val summary = QuizEngineUtils.computeMcqAutoScore(mcqQuestions, mcqAnswers, keyResult.keys)
                autoScore = summary.percentageScore
                correctCount = summary.correctCount
            }
        }

        return runFirestoreWrite(OperationType.WRITE, "quiz_attempts/$attemptId") {
            val ref = db.collection("quiz_attempts").document(attemptId)
            val snap = ref.get().await()
            val existingStatus = snap.getString("status")
            if (snap.exists() && existingStatus != "IN_PROGRESS") {
                throw IllegalStateException("Percobaan ke-$safeAttemptNum sudah dikumpulkan dan tidak dapat diubah.")
            }
            if (!snap.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "quizId" to quiz.id,
                    "classId" to classId.ifBlank { "DEFAULT" },
                    "teacherUid" to teacherUid.ifBlank { "SYSTEM_DEFAULT" },
                    "studentUid" to studentUid,
                    "studentName" to studentName.trim().take(100),
                    "attemptNumber" to safeAttemptNum,
                    "answers" to mcqAnswers,
                    "essayAnswers" to essayAnswers,
                    "questionOrder" to questionOrder.take(100),
                    "remainingSeconds" to remainingSeconds.coerceAtLeast(0),
                    "autoScore" to autoScore,
                    "essayScore" to 0.0,
                    "totalScore" to autoScore,
                    "correctCount" to correctCount,
                    "totalMcqCount" to mcqQuestions.size,
                    "resultsPublished" to canRevealAutoScoreNow,
                    "status" to "SUBMITTED",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            } else {
                val updates = mapOf<String, Any>(
                    "answers" to mcqAnswers,
                    "essayAnswers" to essayAnswers,
                    "questionOrder" to questionOrder.take(100),
                    "remainingSeconds" to remainingSeconds.coerceAtLeast(0),
                    "autoScore" to autoScore,
                    "totalScore" to autoScore,
                    "correctCount" to correctCount,
                    "totalMcqCount" to mcqQuestions.size,
                    "resultsPublished" to canRevealAutoScoreNow,
                    "status" to "SUBMITTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.update(updates).await()
            }
            QuizAttempt(
                id = attemptId,
                quizId = quiz.id,
                classId = classId,
                teacherUid = teacherUid,
                studentUid = studentUid,
                studentName = studentName,
                attemptNumber = safeAttemptNum,
                answers = mcqAnswers,
                essayAnswers = essayAnswers,
                questionOrder = questionOrder,
                remainingSeconds = remainingSeconds,
                autoScore = autoScore,
                totalScore = autoScore,
                correctCount = correctCount,
                totalMcqCount = mcqQuestions.size,
                resultsPublished = canRevealAutoScoreNow,
                status = "SUBMITTED"
            )
        }
    }

    fun observeStudentAttempts(studentUid: String): Flow<List<QuizAttempt>> =
        db.collection("quiz_attempts")
            .whereEqualTo("studentUid", studentUid)
            .observeListFlow<QuizAttempt>("quiz_attempts")
            .map { list -> list.sortedByDescending { it.attemptNumber } }

    fun observeTeacherAttempts(teacherUid: String): Flow<List<QuizAttempt>> =
        db.collection("quiz_attempts")
            .whereEqualTo("teacherUid", teacherUid)
            .observeListFlow<QuizAttempt>("quiz_attempts")

    fun observeStudentPublishedGrades(studentUid: String): Flow<List<GradeRecord>> =
        db.collection("grades")
            .whereEqualTo("studentUid", studentUid)
            .whereEqualTo("published", true)
            .observeListFlow<GradeRecord>("grades")

    fun observeTeacherGrades(teacherUid: String): Flow<List<GradeRecord>> =
        db.collection("grades")
            .whereEqualTo("teacherUid", teacherUid)
            .observeListFlow<GradeRecord>("grades")

    fun observeRubrics(): Flow<List<Rubric>> =
        db.collection("rubrics")
            .whereEqualTo("published", true)
            .observeListFlow<Rubric>("rubrics")
            .map { remote ->
                val merged = ParijotoAssessmentsAndGlossary.defaultRubrics.associateBy { it.id }.toMutableMap()
                remote.forEach { merged[it.id] = it }
                merged.values.toList()
            }

    suspend fun saveAndPublishGrade(
        grade: GradeRecord,
        attemptToUpdate: QuizAttempt? = null,
        submissionIdToUpdate: String? = null
    ): Result<Unit> {
        val docId = grade.id.ifBlank { "grd_${grade.sourceId}_${grade.studentUid}" }
        return runFirestoreWrite(OperationType.WRITE, "grades/$docId") {
            val ref = db.collection("grades").document(docId)
            val existing = ref.get().await()
            val payload = mutableMapOf<String, Any>(
                "classId" to grade.classId.ifBlank { "DEFAULT" },
                "teacherUid" to grade.teacherUid,
                "studentUid" to grade.studentUid,
                "studentName" to grade.studentName.trim().take(100),
                "assessmentType" to grade.assessmentType,
                "sourceId" to grade.sourceId,
                "sourceTitle" to grade.sourceTitle.trim().take(200),
                "finalScore" to grade.finalScore.coerceIn(0.0, 100.0),
                "published" to grade.published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            grade.knowledgeScore?.let { payload["knowledgeScore"] = it.coerceIn(0.0, 100.0) }
            grade.essayScore?.let { payload["essayScore"] = it.coerceIn(0.0, 100.0) }
            grade.skillInvestigationScore?.let { payload["skillInvestigationScore"] = it.coerceIn(0, 4) }
            grade.skillSolutionScore?.let { payload["skillSolutionScore"] = it.coerceIn(0, 4) }
            grade.skillSynthesisScore?.let { payload["skillSynthesisScore"] = it.coerceIn(0, 4) }
            grade.attitudeCollaboration?.let { payload["attitudeCollaboration"] = it.coerceIn(0, 4) }
            grade.attitudePietyEcology?.let { payload["attitudePietyEcology"] = it.coerceIn(0, 4) }
            grade.attitudeResponsibility?.let { payload["attitudeResponsibility"] = it.coerceIn(0, 4) }
            grade.attitudeCare?.let { payload["attitudeCare"] = it.coerceIn(0, 4) }
            grade.feedback?.takeIf { it.isNotBlank() }?.let { payload["feedback"] = it.take(2000) }

            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                ref.set(payload).await()
            } else {
                ref.set(payload, SetOptions.merge()).await()
            }

            if (attemptToUpdate != null) {
                val attemptPayload = mutableMapOf<String, Any>(
                    "essayItemScores" to attemptToUpdate.essayItemScores,
                    "resultsPublished" to grade.published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (grade.published) {
                    attemptPayload["autoScore"] = (grade.knowledgeScore ?: attemptToUpdate.autoScore).coerceIn(0.0, 100.0)
                    attemptPayload["essayScore"] = (grade.essayScore ?: attemptToUpdate.essayScore).coerceIn(0.0, 100.0)
                    attemptPayload["totalScore"] = grade.finalScore.coerceIn(0.0, 100.0)
                    attemptPayload["correctCount"] = attemptToUpdate.correctCount
                    attemptPayload["status"] = "GRADED"
                    grade.feedback?.takeIf { it.isNotBlank() }?.let { attemptPayload["teacherFeedback"] = it.take(2000) }
                } else {
                    // Protect unpublished scores on student-readable QuizAttempt document
                    attemptPayload["autoScore"] = 0.0
                    attemptPayload["essayScore"] = 0.0
                    attemptPayload["totalScore"] = 0.0
                    attemptPayload["correctCount"] = 0
                }
                db.collection("quiz_attempts").document(attemptToUpdate.id).update(attemptPayload).await()
            }
            if (!submissionIdToUpdate.isNullOrBlank()) {
                val subUpdates = mutableMapOf<String, Any>(
                    "status" to "GRADED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                grade.feedback?.takeIf { it.isNotBlank() }?.let { subUpdates["teacherFeedback"] = it.take(2000) }
                db.collection("submissions").document(submissionIdToUpdate).update(subUpdates).await()
            }
        }
    }
}
