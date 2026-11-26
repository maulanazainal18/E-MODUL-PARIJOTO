package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiEvidenceAnalysis
import com.example.data.ai.ParijotoAiRepository
import com.example.data.curriculum.ParijotoAssessmentsAndGlossary
import com.example.data.curriculum.ParijotoCurriculumPart1
import com.example.data.curriculum.ParijotoCurriculumPart2
import com.example.data.firebase.toReadableFirestoreMessage
import com.example.data.model.Announcement
import com.example.data.model.Assignment
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GradeRecord
import com.example.data.model.GroupDiscussionMessage
import com.example.data.model.LearningModule
import com.example.data.model.Lesson
import com.example.data.model.PblGroup
import com.example.data.model.PeerReview
import com.example.data.model.Question
import com.example.data.model.Quiz
import com.example.data.model.QuizAnswerKey
import com.example.data.model.QuizAttempt
import com.example.data.model.Reflection
import com.example.data.model.StudentLearningProgress
import com.example.data.model.Submission
import com.example.data.repository.CurriculumAndClassRepository
import com.example.data.repository.PblAndAssessmentRepository
import com.example.data.repository.QuizAndEvaluationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)

class StudentViewModel(
    context: Context,
    private val studentUid: String
) : ViewModel() {

    private val curriculumRepo = CurriculumAndClassRepository(context)
    private val pblRepo = PblAndAssessmentRepository(context)
    private val quizRepo = QuizAndEvaluationRepository(context)
    private val aiRepo = ParijotoAiRepository()

    private fun <T> Flow<List<T>>.safeState(initial: List<T>): StateFlow<List<T>> =
        this.catch { e ->
            if (e is CancellationException) throw e
            _message.value = e.toReadableFirestoreMessage()
            emit(initial)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initial)

    val modules: StateFlow<List<LearningModule>> = curriculumRepo.observePublishedModules()
        .safeState(listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2))

    val lessons: StateFlow<List<Lesson>> = curriculumRepo.observePublishedLessons()
        .safeState(ParijotoCurriculumPart1.modul1Lessons + ParijotoCurriculumPart2.modul2Lessons)

    val learningProgress: StateFlow<StudentLearningProgress> = curriculumRepo
        .observeStudentLearningProgress(studentUid)
        .catch { e ->
            if (e is CancellationException) throw e
            _message.value = e.toReadableFirestoreMessage()
            emit(StudentLearningProgress(id = studentUid, studentUid = studentUid))
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            StudentLearningProgress(id = studentUid, studentUid = studentUid)
        )

    val enrollments: StateFlow<List<Enrollment>> = curriculumRepo.observeStudentEnrollments(studentUid)
        .safeState(emptyList())

    private val _activeClassId = MutableStateFlow("")
    private val _discussionGroupTarget = MutableStateFlow<Pair<String, String>?>(null)

    val myGroups: StateFlow<List<PblGroup>> = pblRepo.observeStudentGroups(studentUid)
        .safeState(emptyList())

    val openGroups: StateFlow<List<PblGroup>> = _activeClassId
        .flatMapLatest { classId ->
            if (classId.isBlank() || classId == "DEFAULT") flowOf(emptyList())
            else pblRepo.observeGroupsForClass(classId)
        }
        .safeState(emptyList())

    val assignments: StateFlow<List<Assignment>> = pblRepo.observePublishedAssignments()
        .safeState(ParijotoAssessmentsAndGlossary.defaultAssignments)

    val submissions: StateFlow<List<Submission>> = pblRepo.observeStudentSubmissions(studentUid)
        .safeState(emptyList())

    val quizzes: StateFlow<List<Quiz>> = quizRepo.observePublishedQuizzes()
        .safeState(
            listOf(
                ParijotoAssessmentsAndGlossary.quizModul1,
                ParijotoAssessmentsAndGlossary.quizModul2,
                ParijotoAssessmentsAndGlossary.quizPostTest
            )
        )

    val questions: StateFlow<List<Question>> = quizRepo.observePublishedQuestions()
        .safeState(
            ParijotoAssessmentsAndGlossary.modul1Questions +
                ParijotoAssessmentsAndGlossary.modul2Questions +
                ParijotoAssessmentsAndGlossary.postTestQuestions
        )

    val attempts: StateFlow<List<QuizAttempt>> = quizRepo.observeStudentAttempts(studentUid)
        .safeState(emptyList())

    val publishedGrades: StateFlow<List<GradeRecord>> = quizRepo.observeStudentPublishedGrades(studentUid)
        .safeState(emptyList())

    val reflections: StateFlow<List<Reflection>> = pblRepo.observeStudentReflections(studentUid)
        .safeState(emptyList())

    val announcements: StateFlow<List<Announcement>> = pblRepo.observePublishedAnnouncements()
        .safeState(emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _answerKeysCache = MutableStateFlow<Map<String, QuizAnswerKey>>(emptyMap())
    val answerKeysCache: StateFlow<Map<String, QuizAnswerKey>> = _answerKeysCache.asStateFlow()

    private val _aiTutorStream = MutableStateFlow("")
    val aiTutorStream: StateFlow<String> = _aiTutorStream.asStateFlow()

    private val _aiTutorLoading = MutableStateFlow(false)
    val aiTutorLoading: StateFlow<Boolean> = _aiTutorLoading.asStateFlow()

    private val _aiEvidence = MutableStateFlow<AiEvidenceAnalysis?>(null)
    val aiEvidence: StateFlow<AiEvidenceAnalysis?> = _aiEvidence.asStateFlow()

    private val _enrolledClassesMap = MutableStateFlow<Map<String, ClassRoom>>(emptyMap())
    val enrolledClassesMap: StateFlow<Map<String, ClassRoom>> = _enrolledClassesMap.asStateFlow()

    val classPresentations: StateFlow<List<Submission>> = _activeClassId
        .flatMapLatest { classId ->
            if (classId.isBlank() || classId == "DEFAULT") flowOf(emptyList())
            else pblRepo.observeClassPresentations(classId)
        }
        .safeState(emptyList())

    val peerReviews: StateFlow<List<PeerReview>> = _activeClassId
        .flatMapLatest { classId ->
            if (classId.isBlank() || classId == "DEFAULT") flowOf(emptyList())
            else pblRepo.observePeerReviewsForClass(classId)
        }
        .safeState(emptyList())

    val groupDiscussions: StateFlow<List<GroupDiscussionMessage>> = _discussionGroupTarget
        .flatMapLatest { target ->
            if (target == null) flowOf(emptyList())
            else pblRepo.observeGroupDiscussions(target.first, target.second)
        }
        .safeState(emptyList())

    private var aiJob: Job? = null

    init {
        viewModelScope.launch {
            enrollments.collectLatest { list ->
                if (list.isEmpty()) {
                    _enrolledClassesMap.value = emptyMap()
                    _activeClassId.value = ""
                } else {
                    _enrolledClassesMap.value = curriculumRepo.fetchClassRoomsByIds(list.map { it.classId })
                    if (_activeClassId.value.isBlank() || list.none { it.classId == _activeClassId.value }) {
                        _activeClassId.value = list.first().classId
                    }
                }
            }
        }
    }

    fun setActiveClassId(classId: String) {
        if (classId.isNotBlank()) _activeClassId.value = classId
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

    fun joinClass(code: String, studentName: String) {
        viewModelScope.launch {
            _busy.value = true
            curriculumRepo.joinClassByCode(code, studentUid, studentName)
                .onSuccess { _message.value = "Berhasil bergabung ke kelas ${it.className}!" }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun markLessonCompleted(enrollment: Enrollment?, lessonId: String, moduleId: String, totalLessons: Int) {
        viewModelScope.launch {
            val progressResult = curriculumRepo.recordLessonReadProgress(
                studentUid = studentUid,
                moduleId = moduleId,
                lessonId = lessonId,
                scrollPercent = 100,
                markComplete = true,
                current = learningProgress.value
            )
            if (enrollment != null) {
                curriculumRepo.markLessonCompleted(enrollment, lessonId, totalLessons)
                    .onSuccess { _message.value = "Progres materi ditandai selesai & tersinkron ke kelas!" }
                    .onFailure { _message.value = it.toReadableFirestoreMessage() }
            } else {
                progressResult
                    .onSuccess { _message.value = "Progres materi ditandai selesai! Gabung kelas agar guru dapat memantau progresmu." }
                    .onFailure { _message.value = it.toReadableFirestoreMessage() }
            }
        }
    }

    fun toggleBookmark(lessonId: String) {
        viewModelScope.launch {
            curriculumRepo.toggleLessonBookmark(studentUid, lessonId, learningProgress.value)
                .onSuccess { saved ->
                    _message.value = if (saved) "Subbab ditambahkan ke Bookmark." else "Bookmark dihapus."
                }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
        }
    }

    fun setReaderFontScale(fontScale: Double) {
        viewModelScope.launch {
            curriculumRepo.updateReaderFontScale(studentUid, fontScale, learningProgress.value)
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
        }
    }

    fun recordReadingProgress(moduleId: String, lessonId: String, scrollPercent: Int) {
        viewModelScope.launch {
            curriculumRepo.recordLessonReadProgress(
                studentUid = studentUid,
                moduleId = moduleId,
                lessonId = lessonId,
                scrollPercent = scrollPercent,
                markComplete = false,
                current = learningProgress.value
            )
        }
    }

    fun createGroup(enrollment: Enrollment?, name: String, topic: String, studentName: String) {
        if (enrollment == null) {
            _message.value = "Gabung ke kelas terlebih dahulu sebelum membuat kelompok PBL."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            pblRepo.createPblGroup(
                classId = enrollment.classId,
                teacherUid = enrollment.teacherUid,
                name = name,
                topic = topic,
                creatorUid = studentUid,
                creatorName = studentName,
                createdByTeacher = false
            ).onSuccess {
                _message.value = "Kelompok PBL '$name' berhasil dibuat!"
            }.onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
            _busy.value = false
        }
    }

    fun joinGroup(group: PblGroup, studentName: String) {
        viewModelScope.launch {
            _busy.value = true
            pblRepo.joinPblGroup(group, studentUid, studentName)
                .onSuccess { _message.value = "Berhasil bergabung ke ${group.name}." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun updateGroupWorkspace(
        groupId: String,
        topic: String?,
        phase: Int,
        rootProblem: String?,
        hypothesis: String?,
        investigationNotes: String?,
        conservationStrategy: String?,
        campaignCaption: String?,
        discussionSummary: String? = null,
        memberRoles: Map<String, String>? = null
    ) {
        viewModelScope.launch {
            _busy.value = true
            pblRepo.updateGroupPblWorkspace(
                groupId = groupId,
                topic = topic,
                phase = phase,
                rootProblem = rootProblem,
                hypothesis = hypothesis,
                investigationNotes = investigationNotes,
                conservationStrategy = conservationStrategy,
                campaignCaption = campaignCaption,
                discussionSummary = discussionSummary,
                memberRoles = memberRoles
            ).onSuccess {
                _message.value = "Catatan PBL kelompok tersimpan ke cloud!"
            }.onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
            _busy.value = false
        }
    }

    fun updateGroupLeaderAndRoles(
        groupId: String,
        leaderUid: String,
        memberRoles: Map<String, String>,
        openToJoin: Boolean
    ) {
        viewModelScope.launch {
            _busy.value = true
            pblRepo.updateGroupLeaderAndRoles(groupId, leaderUid, memberRoles, openToJoin)
                .onSuccess { _message.value = "Ketua & pembagian peran kelompok diperbarui!" }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun sendGroupDiscussionMessage(
        group: PblGroup,
        senderName: String,
        pblPhase: Int,
        text: String
    ) {
        viewModelScope.launch {
            pblRepo.sendGroupDiscussionMessage(
                groupId = group.id,
                classId = group.classId,
                teacherUid = group.teacherUid,
                senderUid = studentUid,
                senderName = senderName,
                senderRole = "SISWA",
                pblPhase = pblPhase,
                message = text
            ).onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
        }
    }

    fun submitPeerReview(
        submission: Submission,
        reviewerName: String,
        clarityScore: Int,
        evidenceScore: Int,
        solutionScore: Int,
        comment: String
    ) {
        viewModelScope.launch {
            _busy.value = true
            pblRepo.submitPeerReview(
                PeerReview(
                    submissionId = submission.id,
                    assignmentId = submission.assignmentId,
                    classId = submission.classId,
                    teacherUid = submission.teacherUid,
                    reviewerUid = studentUid,
                    reviewerName = reviewerName,
                    targetStudentUid = submission.studentUid,
                    targetStudentName = submission.studentName,
                    clarityScore = clarityScore,
                    evidenceScore = evidenceScore,
                    solutionScore = solutionScore,
                    comment = comment
                )
            ).onSuccess {
                _message.value = "Penilaian antarteman untuk ${submission.studentName} tersimpan!"
            }.onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
            _busy.value = false
        }
    }

    fun saveSubmission(submission: Submission, submitFinal: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            pblRepo.saveSubmission(submission, submitFinal)
                .onSuccess {
                    _message.value = if (submitFinal) {
                        "Tugas berhasil dikumpulkan untuk dinilai guru!"
                    } else {
                        "Draf tugas tersimpan."
                    }
                }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun autosaveQuizAttempt(
        quiz: Quiz,
        attemptNumber: Int,
        questionOrder: List<String>,
        totalMcqCount: Int,
        remainingSeconds: Int,
        enrollment: Enrollment?,
        studentName: String,
        mcqAnswers: Map<String, String>,
        essayAnswers: Map<String, String>,
        showConfirmation: Boolean = false
    ) {
        viewModelScope.launch {
            val classId = enrollment?.classId ?: "DEFAULT"
            val teacherUid = enrollment?.teacherUid ?: "SYSTEM_DEFAULT"
            quizRepo.autosaveQuizAttempt(
                quiz = quiz,
                attemptNumber = attemptNumber,
                questionOrder = questionOrder,
                totalMcqCount = totalMcqCount,
                remainingSeconds = remainingSeconds,
                classId = classId,
                teacherUid = teacherUid,
                studentUid = studentUid,
                studentName = studentName,
                mcqAnswers = mcqAnswers,
                essayAnswers = essayAnswers
            ).onSuccess {
                if (showConfirmation) {
                    _message.value = "Jawaban sementara berhasil disimpan."
                }
            }.onFailure {
                if (showConfirmation) {
                    _message.value = it.toReadableFirestoreMessage()
                }
            }
        }
    }

    fun submitQuizAttempt(
        quiz: Quiz,
        quizQuestions: List<Question>,
        attemptNumber: Int,
        questionOrder: List<String>,
        remainingSeconds: Int,
        enrollment: Enrollment?,
        studentName: String,
        mcqAnswers: Map<String, String>,
        essayAnswers: Map<String, String>
    ) {
        viewModelScope.launch {
            _busy.value = true
            val classId = enrollment?.classId ?: "DEFAULT"
            val teacherUid = enrollment?.teacherUid ?: "SYSTEM_DEFAULT"
            quizRepo.submitQuizAttempt(
                quiz = quiz,
                questions = quizQuestions,
                attemptNumber = attemptNumber,
                questionOrder = questionOrder,
                remainingSeconds = remainingSeconds,
                classId = classId,
                teacherUid = teacherUid,
                studentUid = studentUid,
                studentName = studentName,
                mcqAnswers = mcqAnswers,
                essayAnswers = essayAnswers
            ).onSuccess { attempt ->
                if (quiz.closed && quiz.showDiscussion && quiz.resultsPublished && attempt.resultsPublished) {
                    loadAnswerKeyIfUnlocked(quiz)
                    _message.value = "Evaluasi terkirim! Skor: ${attempt.autoScore.toInt()} (${attempt.correctCount}/${attempt.totalMcqCount})."
                } else {
                    _message.value = "Jawaban terkirim! Nilai dan pembahasan menunggu verifikasi & publikasi guru."
                }
            }.onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
            _busy.value = false
        }
    }

    fun loadAnswerKeyIfUnlocked(quiz: Quiz) {
        if (!(quiz.closed && quiz.showDiscussion && quiz.resultsPublished)) return
        viewModelScope.launch {
            quizRepo.getAnswerKeyIfAllowed(quiz).onSuccess { key ->
                if (key != null) {
                    _answerKeysCache.value = _answerKeysCache.value + (quiz.id to key)
                }
            }
        }
    }

    fun submitReflection(
        moduleId: String,
        enrollment: Enrollment?,
        studentName: String,
        answers: List<Boolean>,
        notes: String?,
        pblReflection: String?
    ) {
        viewModelScope.launch {
            _busy.value = true
            pblRepo.submitReflection(
                Reflection(
                    studentUid = studentUid,
                    studentName = studentName,
                    teacherUid = enrollment?.teacherUid ?: "SYSTEM_DEFAULT",
                    classId = enrollment?.classId ?: "DEFAULT",
                    moduleId = moduleId,
                    checklistYesCount = answers.count { it },
                    totalChecklist = answers.size.coerceAtLeast(1),
                    answers = answers,
                    notes = notes,
                    pblReflection = pblReflection
                )
            ).onSuccess {
                _message.value = "Refleksi & checklist pemahaman diri tersimpan!"
            }.onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
            _busy.value = false
        }
    }

    fun askAiTutor(question: String, lessonContext: String) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _aiTutorLoading.value = true
            _aiTutorStream.value = ""
            try {
                aiRepo.streamBiologyTutor(question, lessonContext).collect { partial ->
                    _aiTutorStream.value = partial
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "Asisten AI belum dapat merespons: ${e.localizedMessage ?: "periksa koneksi internet"}"
            } finally {
                _aiTutorLoading.value = false
            }
        }
    }

    fun analyzeEvidenceStructured(query: String) {
        viewModelScope.launch {
            _aiTutorLoading.value = true
            aiRepo.analyzeEvidenceStructured(query)
                .onSuccess { _aiEvidence.value = it }
                .onFailure { _message.value = "Gagal menganalisis bukti ilmiah: ${it.localizedMessage ?: "coba lagi"}" }
            _aiTutorLoading.value = false
        }
    }

    companion object {
        fun factory(context: Context, studentUid: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudentViewModel(context.applicationContext, studentUid) as T
                }
            }
    }
}
