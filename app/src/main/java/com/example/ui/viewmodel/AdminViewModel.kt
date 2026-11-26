package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.curriculum.ParijotoAssessmentsAndGlossary
import com.example.data.curriculum.ParijotoCurriculumPart1
import com.example.data.curriculum.ParijotoCurriculumPart2
import com.example.data.firebase.toReadableFirestoreMessage
import com.example.data.model.Assignment
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GradeRecord
import com.example.data.model.LearningModule
import com.example.data.model.Lesson
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.Reflection
import com.example.data.model.Subject
import com.example.data.model.Submission
import com.example.data.model.SystemSetting
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.data.repository.AdminAndUserRepository
import com.example.data.repository.CurriculumAndClassRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class AdminViewModel(
    context: Context,
    private val adminUid: String
) : ViewModel() {

    private val adminRepo = AdminAndUserRepository(context)
    private val curriculumRepo = CurriculumAndClassRepository(context)

    private fun <T> Flow<List<T>>.safeState(initial: List<T>): StateFlow<List<T>> =
        this.catch { e ->
            if (e is CancellationException) throw e
            _message.value = e.toReadableFirestoreMessage()
            emit(initial)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initial)

    val allUsers: StateFlow<List<UserAccount>> = adminRepo.observeAllUsersAdmin().safeState(emptyList())
    val allClasses: StateFlow<List<ClassRoom>> = adminRepo.observeAllClassesAdmin().safeState(emptyList())
    val allEnrollments: StateFlow<List<Enrollment>> = adminRepo.observeAllEnrollmentsAdmin().safeState(emptyList())
    val allSubmissions: StateFlow<List<Submission>> = adminRepo.observeAllSubmissionsAdmin().safeState(emptyList())
    val allAttempts: StateFlow<List<QuizAttempt>> = adminRepo.observeAllAttemptsAdmin().safeState(emptyList())
    val allGrades: StateFlow<List<GradeRecord>> = adminRepo.observeAllGradesAdmin().safeState(emptyList())
    val allReflections: StateFlow<List<Reflection>> = adminRepo.observeAllReflectionsAdmin().safeState(emptyList())
    val allAssignments: StateFlow<List<Assignment>> = adminRepo.observeAllAssignmentsAdmin()
        .safeState(ParijotoAssessmentsAndGlossary.defaultAssignments)
    val allQuizzes: StateFlow<List<Quiz>> = adminRepo.observeAllQuizzesAdmin()
        .safeState(
            listOf(
                ParijotoAssessmentsAndGlossary.quizModul1,
                ParijotoAssessmentsAndGlossary.quizModul2,
                ParijotoAssessmentsAndGlossary.quizPostTest
            )
        )

    val subjects: StateFlow<List<Subject>> = curriculumRepo.observePublishedSubjects()
        .safeState(listOf(ParijotoAssessmentsAndGlossary.defaultSubject))
    val modules: StateFlow<List<LearningModule>> = curriculumRepo.observePublishedModules()
        .safeState(listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2))
    val lessons: StateFlow<List<Lesson>> = curriculumRepo.observePublishedLessons()
        .safeState(ParijotoCurriculumPart1.modul1Lessons + ParijotoCurriculumPart2.modul2Lessons)

    val systemSetting: StateFlow<SystemSetting> = adminRepo.observeSystemSetting()
        .catch { e ->
            if (e is CancellationException) throw e
            emit(SystemSetting())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SystemSetting())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            curriculumRepo.seedDefaultCurriculumIfNeeded(adminUid)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun updateUserAccount(user: UserAccount, newRole: UserRole, newStatus: String) {
        viewModelScope.launch {
            _busy.value = true
            adminRepo.updateUserAccountAdmin(user, newRole, newStatus)
                .onSuccess { _message.value = "Akun ${user.nama} diperbarui menjadi ${newRole.label} ($newStatus)." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun saveSystemSetting(setting: SystemSetting) {
        viewModelScope.launch {
            _busy.value = true
            adminRepo.updateSystemSettingAdmin(setting, adminUid)
                .onSuccess { _message.value = "Pengaturan tahun ajaran & sistem tersimpan." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun saveSubject(subject: Subject) {
        viewModelScope.launch {
            _busy.value = true
            curriculumRepo.saveSubject(subject, adminUid)
                .onSuccess { _message.value = "Mata pelajaran tersimpan." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun saveModule(module: LearningModule) {
        viewModelScope.launch {
            _busy.value = true
            curriculumRepo.saveModule(module, adminUid)
                .onSuccess { _message.value = "Modul pembelajaran tersimpan." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun saveLesson(lesson: Lesson) {
        viewModelScope.launch {
            _busy.value = true
            curriculumRepo.saveLesson(lesson, adminUid)
                .onSuccess { _message.value = "Materi '${lesson.title}' diperbarui." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun createClassAsAdmin(
        teacherUid: String,
        teacherName: String,
        name: String,
        gradeLevel: String,
        academicYear: String,
        description: String?
    ) {
        viewModelScope.launch {
            _busy.value = true
            curriculumRepo.createClassRoom(
                teacherUid = teacherUid.ifBlank { adminUid },
                teacherName = teacherName.ifBlank { "Guru Pengampu" },
                name = name,
                gradeLevel = gradeLevel,
                academicYear = academicYear,
                description = description
            ).onSuccess {
                _message.value = "Kelas '${it.name}' dibuat! Kode kelas: ${it.classCode}"
            }.onFailure {
                _message.value = it.toReadableFirestoreMessage()
            }
            _busy.value = false
        }
    }

    fun toggleArchiveClass(classRoom: ClassRoom) {
        viewModelScope.launch {
            curriculumRepo.toggleArchiveClass(classRoom)
                .onSuccess { _message.value = "Status kelas '${classRoom.name}' diperbarui." }
                .onFailure { _message.value = it.toReadableFirestoreMessage() }
        }
    }

    fun buildAdminGradesCsv(): String {
        val header = "Siswa,Kelas ID,Jenis,Sumber,Nilai Akhir,Published\n"
        val body = allGrades.value.joinToString("\n") { g ->
            val scoreText = String.format(Locale.US, "%.1f", g.finalScore)
            "\"${g.studentName.replace("\"", "\"\"")}\",\"${g.classId}\",\"${g.assessmentType}\",\"${g.sourceTitle.replace("\"", "\"\"")}\",$scoreText,${g.published}"
        }
        return header + body
    }

    companion object {
        fun factory(context: Context, adminUid: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminViewModel(context.applicationContext, adminUid) as T
                }
            }
    }
}
