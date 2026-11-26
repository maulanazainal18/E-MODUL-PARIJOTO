package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class UserRole(val label: String) {
    ADMIN("Admin"),
    GURU("Guru"),
    SISWA("Siswa");

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SISWA
    }
}

enum class AccountStatus(val label: String) {
    ACTIVE("Aktif"),
    SUSPENDED("Nonaktif");

    companion object {
        fun fromRaw(raw: String?): AccountStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ACTIVE
    }
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val uid: String = "",
    val nama: String = "",
    val email: String = "",
    val role: String = UserRole.SISWA.name,
    val accountStatus: String = AccountStatus.ACTIVE.name,
    val classId: String? = null,
    val className: String? = null,
    val academicYear: String? = null,
    val photoUrl: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun roleEnum(): UserRole = UserRole.fromRaw(role)
    fun statusEnum(): AccountStatus = AccountStatus.fromRaw(accountStatus)
}

@Keep
@IgnoreExtraProperties
data class SystemSetting(
    @DocumentId val id: String = "",
    val academicYear: String = "2026/2027",
    val semester: String = "Ganjil",
    val schoolName: String = "SMA/MA Kelas X - Lereng Muria",
    val maintenanceMode: Boolean = false,
    val allowRegistration: Boolean = true,
    val updatedBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ClassRoom(
    @DocumentId val id: String = "",
    val teacherUid: String = "",
    val teacherName: String = "",
    val name: String = "",
    val gradeLevel: String = "X SMA/MA",
    val phase: String = "E",
    val classCode: String = "",
    val academicYear: String = "2026/2027",
    val subjectId: String = "subj_bio_x",
    val description: String? = null,
    val archived: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ClassCodeLookup(
    @DocumentId val id: String = "",
    val classId: String = "",
    val className: String = "",
    val teacherUid: String = "",
    val teacherName: String = "",
    val gradeLevel: String = "",
    val academicYear: String = "",
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Enrollment(
    @DocumentId val id: String = "",
    val classId: String = "",
    val className: String = "",
    val classCode: String = "",
    val teacherUid: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val status: String = "ACTIVE",
    val progressPercent: Int = 0,
    val completedLessons: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Subject(
    @DocumentId val id: String = "",
    val title: String = "Biologi: Keanekaragaman Hayati",
    val phase: String = "E",
    val grade: String = "SMA/MA Kelas X",
    val academicYear: String = "2026/2027",
    val description: String? = null,
    val ownerUid: String = "",
    val published: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LearningModule(
    @DocumentId val id: String = "",
    val subjectId: String = "subj_bio_x",
    val moduleNumber: Int = 1,
    val title: String = "",
    val subtitle: String = "",
    val description: String? = null,
    val meetingLabel: String = "Pertemuan 1",
    val durationMinutes: Int = 90,
    val ownerUid: String = "",
    val published: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Lesson(
    @DocumentId val id: String = "",
    val moduleId: String = "",
    val orderIndex: Int = 1,
    val title: String = "",
    val learningObjective: String = "",
    val explanation: String = "",
    val teenExplanation: String? = null,
    val dailyExample: String? = null,
    val funFact: String? = null,
    val sparkQuestion: String? = null,
    val summary: String = "",
    val illustrationType: String? = null,
    val citation: String? = null,
    val ownerUid: String = "",
    val published: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PblGroup(
    @DocumentId val id: String = "",
    val classId: String = "",
    val teacherUid: String = "",
    val name: String = "",
    val topic: String? = null,
    val memberUids: List<String> = emptyList(),
    val memberNames: List<String> = emptyList(),
    val memberRoles: Map<String, String> = emptyMap(),
    val leaderUid: String = "",
    val pblPhase: Int = 1,
    val openToJoin: Boolean = true,
    val rootProblem: String? = null,
    val hypothesis: String? = null,
    val investigationNotes: String? = null,
    val conservationStrategy: String? = null,
    val campaignCaption: String? = null,
    val discussionSummary: String? = null,
    val teacherComment: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupDiscussionMessage(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val classId: String = "",
    val teacherUid: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderRole: String = "SISWA",
    val pblPhase: Int = 1,
    val message: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PeerReview(
    @DocumentId val id: String = "",
    val submissionId: String = "",
    val assignmentId: String = "",
    val classId: String = "",
    val teacherUid: String = "",
    val reviewerUid: String = "",
    val reviewerName: String = "",
    val targetStudentUid: String = "",
    val targetStudentName: String = "",
    val clarityScore: Int = 3,
    val evidenceScore: Int = 3,
    val solutionScore: Int = 3,
    val comment: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Assignment(
    @DocumentId val id: String = "",
    val classId: String = "",
    val teacherUid: String = "",
    val moduleId: String = "",
    val pblPhase: Int = 1,
    val title: String = "",
    val instructions: String = "",
    val type: String = "E_LKM_1",
    val meetingNumber: Int = 1,
    val dueDateLabel: String? = null,
    val rubricId: String? = null,
    val published: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Submission(
    @DocumentId val id: String = "",
    val assignmentId: String = "",
    val classId: String = "",
    val teacherUid: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val groupId: String? = null,
    val pblPhase: Int = 1,
    val problemAnalysis: String? = null,
    val hypothesis: String? = null,
    val scientificEvidence: String? = null,
    val dataAnalysisTable: String? = null,
    val conservationSolution: String? = null,
    val discussionSummary: String? = null,
    val cerClaim: String? = null,
    val cerEvidence: String? = null,
    val cerReasoning: String? = null,
    val campaignCaption: String? = null,
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentSizeBytes: Long? = null,
    val attachmentUrl: String? = null,
    val status: String = "DRAFT",
    val peerFeedback: String? = null,
    val teacherFeedback: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Quiz(
    @DocumentId val id: String = "",
    val classId: String = "DEFAULT",
    val moduleId: String = "",
    val ownerUid: String = "",
    val title: String = "",
    val category: String = "LATIHAN_MODUL_1",
    val durationMinutes: Int = 30,
    val questionCount: Int = 5,
    val maxAttempts: Int = 3,
    val shuffleQuestions: Boolean = true,
    val shuffleOptions: Boolean = true,
    val openForStudents: Boolean = true,
    val scheduleLabel: String = "Tersedia Sekarang",
    val scheduleStartMillis: Long = 0L,
    val scheduleEndMillis: Long = 0L,
    val resultsPublished: Boolean = false,
    val closed: Boolean = false,
    val showDiscussion: Boolean = false,
    val published: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizAnswerKey(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val keys: Map<String, String> = emptyMap(),
    val explanations: Map<String, String> = emptyMap(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Question(
    @DocumentId val id: String = "",
    val quizId: String = "",
    val orderIndex: Int = 1,
    val type: String = "MULTIPLE_CHOICE",
    val prompt: String = "",
    val stimulusText: String? = null,
    val optionA: String? = null,
    val optionB: String? = null,
    val optionC: String? = null,
    val optionD: String? = null,
    val optionE: String? = null,
    val hotsCategory: String? = null,
    val maxPoints: Int = 10,
    val ownerUid: String = "",
    val published: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val quizId: String = "",
    val classId: String = "",
    val teacherUid: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val attemptNumber: Int = 1,
    val answers: Map<String, String> = emptyMap(),
    val essayAnswers: Map<String, String> = emptyMap(),
    val essayItemScores: Map<String, Int> = emptyMap(),
    val questionOrder: List<String> = emptyList(),
    val remainingSeconds: Int = 0,
    val autoScore: Double = 0.0,
    val essayScore: Double = 0.0,
    val totalScore: Double = 0.0,
    val correctCount: Int = 0,
    val totalMcqCount: Int = 0,
    val resultsPublished: Boolean = false,
    val teacherFeedback: String? = null,
    val status: String = "IN_PROGRESS",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GradeRecord(
    @DocumentId val id: String = "",
    val classId: String = "",
    val teacherUid: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val assessmentType: String = "PENGETAHUAN",
    val sourceId: String = "",
    val sourceTitle: String = "",
    val knowledgeScore: Double? = null,
    val essayScore: Double? = null,
    val skillInvestigationScore: Int? = null,
    val skillSolutionScore: Int? = null,
    val skillSynthesisScore: Int? = null,
    val attitudeCollaboration: Int? = null,
    val attitudePietyEcology: Int? = null,
    val attitudeResponsibility: Int? = null,
    val attitudeCare: Int? = null,
    val finalScore: Double = 0.0,
    val feedback: String? = null,
    val published: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Rubric(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val title: String = "",
    val category: String = "URAIAN",
    val maxTotalScore: Int = 12,
    val criteria: List<String> = emptyList(),
    val published: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Reflection(
    @DocumentId val id: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val teacherUid: String = "",
    val classId: String = "",
    val moduleId: String = "",
    val checklistYesCount: Int = 0,
    val totalChecklist: Int = 4,
    val answers: List<Boolean> = emptyList(),
    val notes: String? = null,
    val pblReflection: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val classId: String = "",
    val teacherUid: String = "",
    val teacherName: String = "",
    val title: String = "",
    val message: String = "",
    val pinned: Boolean = false,
    val published: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentLearningProgress(
    @DocumentId val id: String = "",
    val studentUid: String = "",
    val completedLessons: List<String> = emptyList(),
    val bookmarkedLessons: List<String> = emptyList(),
    val lessonReadProgress: Map<String, Int> = emptyMap(),
    val lastLessonByModule: Map<String, String> = emptyMap(),
    val readerFontScale: Double = 1.0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
