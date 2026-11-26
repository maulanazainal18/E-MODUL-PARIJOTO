package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.curriculum.ParijotoAssessmentsAndGlossary
import com.example.data.curriculum.ParijotoCurriculumPart1
import com.example.data.curriculum.ParijotoCurriculumPart2
import com.example.data.firebase.toReadableFirestoreMessage
import com.example.data.curriculum.QuizEngineUtils
import com.example.data.model.Announcement
import com.example.data.model.Assignment
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GradeRecord
import com.example.data.model.GroupDiscussionMessage
import com.example.data.model.LearningModule
import com.example.data.model.Lesson
import com.example.data.model.PblGroup
import com.example.data.model.Question
import com.example.data.model.Quiz
import com.example.data.model.QuizAnswerKey
import com.example.data.model.QuizAttempt
import com.example.data.model.Reflection
import com.example.data.model.Rubric
import com.example.data.model.Submission
import com.example.data.repository.CurriculumAndClassRepository
import com.example.data.repository.PblAndAssessmentRepository
import com.example.data.repository.QuizAndEvaluationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherViewModel(
    context: Context,
    private val teacherUid: String
) : ViewModel() {

    private val curriculumRepo = CurriculumAndClassRepository(context)
    private val pblRepo = PblAndAssessmentRepository(context)
    private val quizRepo = QuizAndEvaluationRepository(context)

    private fun <T> Flow<List<T>>.safeState(initial: List<T>): StateFlow<List<T>> =
        this.catch { e ->
            if (e is CancellationException) throw e
            _message.value = e.toReadableFirestoreMessage()
            emit(initial)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initial)

    val classes: StateFlow<List<ClassRoom>> = curriculumRepo.observeTeacherClasses(teacherUid)
        .safeState(emptyList())

    val enrollments: StateFlow<List<Enrollment>> = curriculumRepo.observeTeacherEnrollments(teacherUid)
        .safeState(emptyList())

    val modules: StateFlow<List<LearningModule>> = curriculumRepo.observePublishedModules()
        .safeState(listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2))

    val lessons: StateFlow<List<Lesson>> = curriculumRepo.observeTeacherLessons(teacherUid)
        .safeState(ParijotoCurriculumPart1.modul1Lessons + ParijotoCurriculumPart2.modul2Lessons)

    val groups: StateFlow<List<PblGroup>> = pblRepo.observeTeacherGroups(teacherUid)
        .safeState(emptyList())

    val assignments: StateFlow<List<Assignment>> = pblRepo.observePublishedAssignments()
        .safeState(ParijotoAssessmentsAndGlossary.defaultAssignments)

    val submissions: StateFlow<List<Submission>> = pblRepo.observeTeacherSubmissions(teacherUid)
        .safeState(emptyList())

    val quizzes: StateFlow<List<Quiz>> = quizRepo.observeTeacherQuizzes(teacherUid)
        .safeState(
            listOf(
                ParijotoAssessmentsAndGlossary.quizModul1,
                ParijotoAssessmentsAndGlossary.quizModul2,
                ParijotoAssessmentsAndGlossary.quizPostTest
            )
        )

    val questions: StateFlow<List<Question>> = quizRepo.observeTeacherQuestions(teacherUid)
        .safeState(
            ParijotoAssessmentsAndGlossary.modul1Questions +
                ParijotoAssessmentsAndGlossary.modul2Questions +
                ParijotoAssessmentsAndGlossary.postTestQuestions
        )

    val attempts: StateFlow<List<QuizAttempt>> = quizRepo.observeTeacherAttempts(teacherUid)
        .safeState(emptyList())

    val grades: StateFlow<List<GradeRecord>> = quizRepo.observeTeacherGrades(teacherUid)
        .safeState(emptyList())

    val rubrics: StateFlow<List<Rubric>> = quizRepo.observeRubrics()
        .safeState(ParijotoAssessmentsAndGlossary.defaultRubrics)

    val reflections: StateFlow<List<Reflection>> = pblRepo.observeTeacherReflections(teacherUid)
        .safeState(emptyList())

    val announcements: StateFlow<List<Announcement>> = pblRepo.observePublishedAnnouncements()
        .safeState(emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _answerKeys = MutableStateFlow<Map<String, QuizAnswerKey>>(emptyMap())
    val answerKeys: StateFlow<Map<String, QuizAnswerKey>> = _answerKeys.asStateFlow()

    private val _discussionGroupTarget = MutableStateFlow<Pair<String, String>?>(null)

    val groupDiscussions: StateFlow<List<GroupDiscussionMessage>> = _discussionGroupTarget
        .flatMapLatest { target ->
            if (target == null) flowOf(emptyList())
            else pblRepo.observeGroupDiscussions(target.first, target.second)
        }
        .safeState(emptyList())

    init {
        viewModelScope.launch {
            curriculumRepo.seedDefaultCurriculumIfNeeded(teacherUid)
        }
    }

    fun setActiveDiscussionGroup(groupId: String?, classId: String?) {
        _discussionGroupTarget.value = if (!groupId.isNullOrBlank() && !classId.isNullOrBlank()) {
            groupId to classId
        } else {
            null
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun createClass(teacherName: String, name: String, gradeLevel: String, academicYear: String, description: String?) {
        viewModelScope.launch {
            _busy.value = true
            curriculumRepo.createClassRoom(teacherUid, teacherName, name, gradeLevel, academicYear, description)
                .onSuccess { _message.value = "Kelas '${it.name}' dibuat! Kode kelas: ${it.classCode}" }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun toggleArchiveClass(classRoom: ClassRoom) {
        viewModelScope.launch {
            curriculumRepo.toggleArchiveClass(classRoom)
                .onSuccess {
                    _message.value = if (classRoom.archived) "Kelas diaktifkan kembali." else "Kelas diarsipkan."
                }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
        }
    }

    fun toggleEnrollmentStatus(enrollment: Enrollment) {
        val nextStatus = if (enrollment.status == "ACTIVE") "REMOVED" else "ACTIVE"
        viewModelScope.launch {
            curriculumRepo.updateEnrollmentStatus(enrollment.id, nextStatus)
                .onSuccess {
                    _message.value = if (nextStatus == "REMOVED") {
                        "Keanggotaan ${enrollment.studentName} dinonaktifkan."
                    } else {
                        "Keanggotaan ${enrollment.studentName} diaktifkan kembali."
                    }
                }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
        }
    }

    fun removeStudent(enrollmentId: String) {
        viewModelScope.launch {
            curriculumRepo.removeEnrollment(enrollmentId)
                .onSuccess { _message.value = "Siswa dihapus permanen dari kelas." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
        }
    }

    fun saveModule(module: LearningModule) {
        viewModelScope.launch {
            _busy.value = true
            curriculumRepo.saveModule(module, teacherUid)
                .onSuccess { _message.value = "Modul pembelajaran diperbarui." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun saveLesson(lesson: Lesson) {
        viewModelScope.launch {
            _busy.value = true
            curriculumRepo.saveLesson(lesson, teacherUid)
                .onSuccess { _message.value = "Materi '${lesson.title}' tersimpan." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun deleteLesson(lessonId: String) {
        viewModelScope.launch {
            curriculumRepo.deleteLesson(lessonId)
                .onSuccess { _message.value = "Materi dihapus." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
        }
    }

    fun createGroup(classId: String, name: String, topic: String) {
        viewModelScope.launch {
            _busy.value = true
            pblRepo.createPblGroup(
                classId = classId,
                teacherUid = teacherUid,
                name = name,
                topic = topic,
                creatorUid = teacherUid,
                creatorName = "Guru",
                createdByTeacher = true
            ).onSuccess {
                _message.value = "Kelompok PBL '$name' siap diikuti siswa."
            }.onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
            _busy.value = false
        }
    }

    fun assignMembersToGroup(
        groupId: String,
        leaderUid: String,
        memberUids: List<String>,
        memberNames: List<String>,
        memberRoles: Map<String, String>
    ) {
        viewModelScope.launch {
            _busy.value = true
            pblRepo.assignMembersToGroup(groupId, leaderUid, memberUids, memberNames, memberRoles)
                .onSuccess { _message.value = "Anggota, ketua, & peran kelompok PBL diperbarui." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun saveGroupTeacherComment(groupId: String, teacherComment: String) {
        viewModelScope.launch {
            _busy.value = true
            pblRepo.saveGroupTeacherComment(groupId, teacherComment)
                .onSuccess { _message.value = "Komentar bimbingan guru tersimpan untuk kelompok." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun sendGroupDiscussionMessage(
        group: PblGroup,
        teacherName: String,
        pblPhase: Int,
        text: String
    ) {
        viewModelScope.launch {
            pblRepo.sendGroupDiscussionMessage(
                groupId = group.id,
                classId = group.classId,
                teacherUid = teacherUid,
                senderUid = teacherUid,
                senderName = teacherName,
                senderRole = "GURU",
                pblPhase = pblPhase,
                message = text
            ).onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
        }
    }

    fun createAssignment(classId: String, moduleId: String, pblPhase: Int, title: String, instructions: String, type: String, meetingNumber: Int) {
        viewModelScope.launch {
            _busy.value = true
            pblRepo.createAssignment(
                Assignment(
                    classId = classId,
                    teacherUid = teacherUid,
                    moduleId = moduleId,
                    pblPhase = pblPhase,
                    title = title,
                    instructions = instructions,
                    type = type,
                    meetingNumber = meetingNumber,
                    dueDateLabel = "Pertemuan $meetingNumber",
                    rubricId = "rubric_keterampilan_pbl",
                    published = true
                ),
                teacherUid
            ).onSuccess {
                _message.value = "Tugas / E-LKM '$title' dipublikasikan!"
            }.onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
            _busy.value = false
        }
    }

    fun saveQuizConfig(updatedQuiz: Quiz) {
        viewModelScope.launch {
            _busy.value = true
            quizRepo.saveQuizConfig(updatedQuiz, teacherUid)
                .onSuccess {
                    _message.value = "Pengaturan '${updatedQuiz.title}' disimpan!"
                    if (updatedQuiz.resultsPublished) {
                        publishPendingMcqAttemptsInternal(updatedQuiz)
                    }
                }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun saveBankQuestion(
        quiz: Quiz,
        question: Question,
        correctOption: String?,
        explanation: String?
    ) {
        viewModelScope.launch {
            _busy.value = true
            quizRepo.saveBankQuestion(
                quiz = quiz,
                question = question.copy(ownerUid = teacherUid),
                correctOption = correctOption,
                explanation = explanation,
                ownerUid = teacherUid
            ).onSuccess {
                ensureAnswerKeyLoaded(quiz)
                _message.value = if (question.id.isBlank()) {
                    "Soal baru ditambahkan ke Bank Soal ${quiz.title}."
                } else {
                    "Butir soal di Bank Soal diperbarui."
                }
            }.onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
            _busy.value = false
        }
    }

    fun deleteBankQuestion(questionId: String) {
        viewModelScope.launch {
            _busy.value = true
            quizRepo.deleteBankQuestion(questionId)
                .onSuccess { _message.value = "Soal dihapus dari Bank Soal." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun createQuestion(
        quiz: Quiz,
        orderIndex: Int,
        type: String,
        prompt: String,
        stimulus: String?,
        optA: String?,
        optB: String?,
        optC: String?,
        optD: String?,
        optE: String?,
        correctOption: String?,
        explanation: String?
    ) {
        saveBankQuestion(
            quiz = quiz,
            question = Question(
                quizId = quiz.id,
                orderIndex = orderIndex,
                type = type,
                prompt = prompt,
                stimulusText = stimulus,
                optionA = optA,
                optionB = optB,
                optionC = optC,
                optionD = optD,
                optionE = optE,
                hotsCategory = if (stimulus.isNullOrBlank()) "HOTS Guru" else "Soal Kasus",
                maxPoints = if (type == "ESSAY") 50 else 20,
                ownerUid = teacherUid,
                published = true
            ),
            correctOption = correctOption,
            explanation = explanation
        )
    }

    fun toggleQuizEvaluation(
        quiz: Quiz,
        closed: Boolean,
        showDiscussion: Boolean,
        resultsPublished: Boolean = quiz.resultsPublished
    ) {
        saveQuizConfig(
            quiz.copy(
                closed = closed,
                showDiscussion = showDiscussion,
                resultsPublished = resultsPublished
            )
        )
    }

    fun ensureAnswerKeyLoaded(quiz: Quiz) {
        viewModelScope.launch {
            quizRepo.getAnswerKeyForTeacher(quiz).onSuccess { resolved ->
                _answerKeys.value = _answerKeys.value + (quiz.id to resolved)
            }
        }
    }

    fun ensureAllAnswerKeysLoaded(targetQuizzes: List<Quiz> = quizzes.value) {
        targetQuizzes.forEach { ensureAnswerKeyLoaded(it) }
    }

    private suspend fun publishPendingMcqAttemptsInternal(quiz: Quiz) {
        val allQuizQs = questions.value.filter { it.quizId == quiz.id }
        val hasEssay = allQuizQs.any { it.type == "ESSAY" }
        if (hasEssay) return
        val key = _answerKeys.value[quiz.id] ?: quizRepo.getAnswerKeyForTeacher(quiz).getOrNull() ?: return
        _answerKeys.value = _answerKeys.value + (quiz.id to key)

        val pending = attempts.value.filter {
            it.quizId == quiz.id && it.status == "SUBMITTED" && !it.resultsPublished
        }
        pending.forEach { attempt ->
            val attemptQs = if (attempt.questionOrder.isNotEmpty()) {
                val byId = allQuizQs.associateBy { it.id }
                attempt.questionOrder.mapNotNull { byId[it] }
            } else {
                allQuizQs
            }
            val mcqQs = attemptQs.filter { it.type == "MULTIPLE_CHOICE" }
            val summary = QuizEngineUtils.computeMcqAutoScore(mcqQs, attempt.answers, key.keys)
            quizRepo.saveAndPublishGrade(
                grade = GradeRecord(
                    id = "grd_quiz_${attempt.quizId}_${attempt.studentUid}_att${attempt.attemptNumber}",
                    classId = attempt.classId,
                    teacherUid = teacherUid,
                    studentUid = attempt.studentUid,
                    studentName = attempt.studentName,
                    assessmentType = "PENGETAHUAN",
                    sourceId = attempt.quizId,
                    sourceTitle = "${quiz.title} (Percobaan #${attempt.attemptNumber})",
                    knowledgeScore = summary.percentageScore,
                    essayScore = null,
                    finalScore = summary.percentageScore,
                    feedback = "Penilaian otomatis pilihan ganda (${summary.correctCount}/${summary.totalMcqCount} benar).",
                    published = true
                ),
                attemptToUpdate = attempt.copy(
                    correctCount = summary.correctCount,
                    totalMcqCount = summary.totalMcqCount
                )
            )
        }
    }

    fun gradeQuizAttempt(
        attempt: QuizAttempt,
        quiz: Quiz?,
        essayItemScores: Map<String, Int>,
        feedback: String,
        publish: Boolean
    ) {
        viewModelScope.launch {
            _busy.value = true
            val quizId = quiz?.id ?: attempt.quizId
            val quizTitle = quiz?.title ?: attempt.quizId
            val allQuizQs = questions.value.filter { it.quizId == quizId }
            val attemptQs = if (attempt.questionOrder.isNotEmpty()) {
                val byId = allQuizQs.associateBy { it.id }
                val mapped = attempt.questionOrder.mapNotNull { byId[it] }
                if (mapped.isNotEmpty()) mapped else allQuizQs
            } else {
                allQuizQs
            }

            // 1. Mekanisme Penilaian Otomatis Pilihan Ganda
            val mcqQs = attemptQs.filter { it.type == "MULTIPLE_CHOICE" }
            val resolvedKey = _answerKeys.value[quizId]
                ?: quiz?.let { quizRepo.getAnswerKeyForTeacher(it).getOrNull() }
                ?: QuizAnswerKey(
                    id = "master",
                    keys = ParijotoAssessmentsAndGlossary.defaultAnswerKeys[quizId] ?: emptyMap(),
                    explanations = ParijotoAssessmentsAndGlossary.defaultExplanations[quizId] ?: emptyMap()
                )
            _answerKeys.value = _answerKeys.value + (quizId to resolvedKey)
            val mcqSummary = QuizEngineUtils.computeMcqAutoScore(mcqQs, attempt.answers, resolvedKey.keys)

            // 2. Mekanisme Penilaian Manual Soal Uraian (Berbasis Rubrik per Butir)
            val essayQs = attemptQs.filter { it.type == "ESSAY" }
            val essaySummary = QuizEngineUtils.computeEssayManualScore(essayQs, essayItemScores)
            val finalScore = QuizEngineUtils.combineQuizFinalScore(
                hasMcq = mcqQs.isNotEmpty(),
                hasEssay = essayQs.isNotEmpty(),
                mcqPct = mcqSummary.percentageScore,
                essayPct = essaySummary.percentageScore
            )

            quizRepo.saveAndPublishGrade(
                grade = GradeRecord(
                    id = "grd_quiz_${attempt.quizId}_${attempt.studentUid}_att${attempt.attemptNumber}",
                    classId = attempt.classId,
                    teacherUid = teacherUid,
                    studentUid = attempt.studentUid,
                    studentName = attempt.studentName,
                    assessmentType = "PENGETAHUAN",
                    sourceId = attempt.quizId,
                    sourceTitle = "$quizTitle (Percobaan #${attempt.attemptNumber})",
                    knowledgeScore = if (mcqQs.isNotEmpty()) mcqSummary.percentageScore else null,
                    essayScore = if (essayQs.isNotEmpty()) essaySummary.percentageScore else null,
                    finalScore = finalScore,
                    feedback = feedback,
                    published = publish
                ),
                attemptToUpdate = attempt.copy(
                    correctCount = mcqSummary.correctCount,
                    totalMcqCount = mcqSummary.totalMcqCount,
                    essayItemScores = essayItemScores
                )
            ).onSuccess {
                _message.value = if (publish) {
                    "Nilai ${attempt.studentName} (Akhir: ${QuizEngineUtils.roundScore(finalScore)}) dipublikasikan!"
                } else {
                    "Draf nilai ${attempt.studentName} disimpan (tersembunyi dari siswa hingga dipublikasikan)."
                }
            }.onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
            _busy.value = false
        }
    }

    fun gradeSubmissionSkill(
        submission: Submission,
        assignmentTitle: String,
        investigationScore: Int,
        solutionScore: Int,
        synthesisScore: Int,
        feedback: String,
        publish: Boolean
    ) {
        viewModelScope.launch {
            _busy.value = true
            val total = investigationScore.coerceIn(1, 4) + solutionScore.coerceIn(1, 4) + synthesisScore.coerceIn(1, 4)
            val finalScore = (total * 100.0) / 12.0
            quizRepo.saveAndPublishGrade(
                grade = GradeRecord(
                    id = "grd_skl_${submission.assignmentId}_${submission.studentUid}",
                    classId = submission.classId,
                    teacherUid = teacherUid,
                    studentUid = submission.studentUid,
                    studentName = submission.studentName,
                    assessmentType = "KETERAMPILAN",
                    sourceId = submission.assignmentId,
                    sourceTitle = assignmentTitle,
                    skillInvestigationScore = investigationScore,
                    skillSolutionScore = solutionScore,
                    skillSynthesisScore = synthesisScore,
                    finalScore = finalScore,
                    feedback = feedback,
                    published = publish
                ),
                submissionIdToUpdate = submission.id
            ).onSuccess {
                _message.value = "Nilai keterampilan ${submission.studentName} tersimpan (${finalScore.toInt()})!"
            }.onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
            _busy.value = false
        }
    }

    fun returnSubmissionForRevision(submission: Submission, teacherFeedback: String) {
        if (teacherFeedback.isBlank()) {
            _message.value = "Tuliskan catatan koreksi/perbaikan sebelum mengembalikan tugas."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            pblRepo.returnSubmissionForRevision(submission.id, teacherFeedback)
                .onSuccess {
                    _message.value = "Tugas ${submission.studentName} dikembalikan untuk diperbaiki."
                }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun gradeStudentAttitude(
        enrollment: Enrollment,
        collaboration: Int,
        pietyEcology: Int,
        responsibility: Int,
        care: Int,
        feedback: String
    ) {
        viewModelScope.launch {
            _busy.value = true
            val total = collaboration + pietyEcology + responsibility + care
            val finalScore = (total * 100.0) / 16.0
            quizRepo.saveAndPublishGrade(
                grade = GradeRecord(
                    id = "grd_skp_${enrollment.classId}_${enrollment.studentUid}",
                    classId = enrollment.classId,
                    teacherUid = teacherUid,
                    studentUid = enrollment.studentUid,
                    studentName = enrollment.studentName,
                    assessmentType = "SIKAP",
                    sourceId = enrollment.classId,
                    sourceTitle = "Observasi Sikap PBL (${enrollment.className})",
                    attitudeCollaboration = collaboration,
                    attitudePietyEcology = pietyEcology,
                    attitudeResponsibility = responsibility,
                    attitudeCare = care,
                    finalScore = finalScore,
                    feedback = feedback,
                    published = true
                )
            ).onSuccess {
                _message.value = "Observasi sikap ${enrollment.studentName} dipublikasikan!"
            }.onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
            _busy.value = false
        }
    }

    fun publishAnnouncement(classId: String, teacherName: String, title: String, message: String, pinned: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            pblRepo.createAnnouncement(classId, teacherUid, teacherName, title, message, pinned)
                .onSuccess { _message.value = "Pengumuman dikirim ke siswa!" }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun buildGradesCsv(classIdFilter: String? = null): String {
        val header = "Nama Siswa,ID Kelas,Jenis Penilaian,Sumber,Skor PG,Skor Uraian,Nilai Akhir,Status Publikasi,Umpan Balik\n"
        val rows = grades.value
            .filter { classIdFilter.isNullOrBlank() || it.classId == classIdFilter }
            .joinToString("\n") { g ->
                val escName = g.studentName.replace("\"", "\"\"")
                val escSrc = g.sourceTitle.replace("\"", "\"\"")
                val escFb = g.feedback.orEmpty().replace("\"", "\"\"").replace("\n", " ")
                val scoreText = String.format(Locale.US, "%.1f", g.finalScore)
                "\"$escName\",\"${g.classId}\",\"${g.assessmentType}\",\"$escSrc\",${g.knowledgeScore ?: ""},${g.essayScore ?: ""},$scoreText,${if (g.published) "Published" else "Draft"},\"$escFb\""
            }
        return header + rows
    }

    companion object {
        fun factory(context: Context, teacherUid: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TeacherViewModel(context.applicationContext, teacherUid) as T
                }
            }
    }
}
