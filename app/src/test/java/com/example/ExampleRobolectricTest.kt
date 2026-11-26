package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.credentials.CredentialManager
import androidx.test.core.app.ApplicationProvider
import com.example.data.curriculum.LearningReportEngine
import com.example.data.curriculum.ParijotoAssessmentsAndGlossary
import com.example.data.curriculum.ParijotoCurriculumPart1
import com.example.data.curriculum.ParijotoCurriculumPart2
import com.example.data.curriculum.QuizEngineUtils
import com.example.data.curriculum.ReportFilter
import com.example.data.export.ReportExportManager
import com.example.data.model.AccountStatus
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GradeRecord
import com.example.data.model.GroupDiscussionMessage
import com.example.data.model.LearningModule
import com.example.data.model.PblGroup
import com.example.data.model.PeerReview
import com.example.data.model.Question
import com.example.data.model.Quiz
import com.example.data.model.QuizAnswerKey
import com.example.data.model.QuizAttempt
import com.example.data.model.StudentLearningProgress
import com.example.data.model.Subject
import com.example.data.model.Submission
import com.example.data.model.SystemSetting
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.ui.admin.AdminClassesAndCurriculumTab
import com.example.ui.admin.AdminUsersTab
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.RoleOnboardingScreen
import com.example.ui.auth.SuspendedAccountScreen
import com.example.ui.navigation.ParijotoNavGraph
import com.example.ui.reports.AdminReportsTab
import com.example.ui.student.LessonReaderScreen
import com.example.ui.student.QuizRunnerScreen
import com.example.ui.student.StudentEvaluationTab
import com.example.ui.student.StudentHomeTab
import com.example.ui.student.StudentModulesTab
import com.example.ui.student.StudentPblTab
import com.example.ui.student.StudentProfileTab
import com.example.ui.teacher.TeacherClassesTab
import com.example.ui.teacher.TeacherCurriculumTab
import com.example.ui.teacher.TeacherGradingTab
import com.example.ui.theme.ParijotoLearningTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `app name matches Parijoto Learning`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Parijoto Learning", appName)
    }

    @Test
    fun `phase 1 navigation traverses splash onboarding login and role dashboards`() {
        composeRule.setContent {
            ParijotoLearningTheme {
                ParijotoNavGraph()
            }
        }

        composeRule.onNodeWithTag("splash_skip_button").performClick()
        composeRule.onNodeWithTag("onboarding_skip_button").assertIsDisplayed()

        composeRule.onNodeWithTag("onboarding_skip_button").performClick()
        composeRule.onNodeWithTag("quick_siswa_button").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("quick_siswa_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("nav_p1_siswa_home").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_p1_siswa_profile").performClick()
        composeRule.onNodeWithTag("siswa_p1_logout_button").performClick()
        composeRule.onNodeWithTag("dialog_confirm_button").performClick()

        composeRule.onNodeWithTag("quick_guru_button").performScrollTo().performClick()
        composeRule.onNodeWithText("Antrean Tugas & Evaluasi (Dummy)").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_p1_guru_profile").performClick()
        composeRule.onNodeWithTag("guru_p1_logout_button").performClick()
        composeRule.onNodeWithTag("dialog_confirm_button").performClick()

        composeRule.onNodeWithTag("quick_admin_button").performScrollTo().performClick()
        composeRule.onNodeWithText("Dashboard Administrator").assertIsDisplayed()
    }

    @Test
    fun `phase 2 user role and account status validation maps accurately`() {
        val admin = UserAccount(uid = "u1", role = "ADMIN", accountStatus = "ACTIVE")
        val guru = UserAccount(uid = "u2", role = "GURU", accountStatus = "ACTIVE")
        val siswa = UserAccount(uid = "u3", role = "SISWA", accountStatus = "SUSPENDED")
        val unknown = UserAccount(uid = "u4", role = "INVALID", accountStatus = "UNKNOWN")

        assertEquals(UserRole.ADMIN, admin.roleEnum())
        assertEquals(UserRole.GURU, guru.roleEnum())
        assertEquals(UserRole.SISWA, siswa.roleEnum())
        assertEquals(AccountStatus.SUSPENDED, siswa.statusEnum())
        assertEquals(UserRole.SISWA, unknown.roleEnum())
        assertEquals(AccountStatus.ACTIVE, unknown.statusEnum())
    }

    @Test
    fun `phase 2 login screen shows google sign in and onboarding trigger`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(context)
        }
        val credentialManager = CredentialManager.create(context)
        var revisitedOnboarding = false

        composeRule.setContent {
            ParijotoLearningTheme {
                LoginScreen(
                    credentialManager = credentialManager,
                    onOpenOnboarding = { revisitedOnboarding = true }
                )
            }
        }

        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("login_revisit_onboarding_button").performScrollTo().performClick()
        assertTrue(revisitedOnboarding)
    }

    @Test
    fun `phase 2 role onboarding submits selected role and supports sign out`() {
        var submittedName = ""
        var submittedRole: UserRole? = null
        var signedOut = false

        composeRule.setContent {
            ParijotoLearningTheme {
                RoleOnboardingScreen(
                    defaultName = "Sari",
                    email = "sari@sman1muria.sch.id",
                    isBootstrapAdmin = false,
                    busy = false,
                    onSubmitProfile = { name, role ->
                        submittedName = name
                        submittedRole = role
                    },
                    onSignOut = { signedOut = true }
                )
            }
        }

        composeRule.onNodeWithTag("onboarding_name_input").performTextReplacement("Ibu Sari, S.Pd.")
        composeRule.onNodeWithTag("role_guru_chip").performClick()
        composeRule.onNodeWithTag("complete_onboarding_button").performClick()

        assertEquals("Ibu Sari, S.Pd.", submittedName)
        assertEquals(UserRole.GURU, submittedRole)

        composeRule.onNodeWithTag("onboarding_signout_button").performClick()
        assertTrue(signedOut)
    }

    @Test
    fun `phase 2 suspended account blocks access and triggers sign out`() {
        var signedOut = false
        composeRule.setContent {
            ParijotoLearningTheme {
                SuspendedAccountScreen(
                    userName = "Budi",
                    onSignOut = { signedOut = true }
                )
            }
        }
        composeRule.onNodeWithText("Akun Dinonaktifkan").assertIsDisplayed()
        composeRule.onNodeWithTag("suspended_signout_button").performClick()
        assertTrue(signedOut)
    }

    @Test
    fun `phase 3 admin creates class for teacher and searches users`() {
        val admin = UserAccount(uid = "adm1", nama = "Admin Muria", email = "maulanazainal18@gmail.com", role = "ADMIN")
        val guru = UserAccount(uid = "g1", nama = "Ahzami, S.Pd., Gr.", email = "guru@muria.sch.id", role = "GURU")
        val siswaA = UserAccount(uid = "s1", nama = "Andi Pratama", email = "andi@muria.sch.id", role = "SISWA")
        val siswaB = UserAccount(uid = "s2", nama = "Siti Rahma", email = "siti@muria.sch.id", role = "SISWA")

        var createdClassName = ""
        var assignedTeacherUid = ""

        composeRule.setContent {
            ParijotoLearningTheme {
                AdminClassesAndCurriculumTab(
                    currentAdmin = admin,
                    users = listOf(admin, guru, siswaA, siswaB),
                    classes = emptyList(),
                    enrollments = emptyList(),
                    subjects = listOf(Subject(id = "subj_bio_x")),
                    modules = emptyList(),
                    lessons = emptyList(),
                    systemSetting = SystemSetting(),
                    busy = false,
                    onCreateClass = { tUid, _, clsName, _, _, _ ->
                        assignedTeacherUid = tUid
                        createdClassName = clsName
                    },
                    onToggleArchiveClass = {},
                    onSaveSubject = {}
                )
            }
        }

        composeRule.onNodeWithTag("admin_class_name_input").performTextReplacement("X IPA 3 Parijoto")
        composeRule.onNodeWithTag("admin_create_class_button").performClick()
        assertEquals("g1", assignedTeacherUid)
        assertEquals("X IPA 3 Parijoto", createdClassName)
    }

    @Test
    fun `phase 3 admin users tab filters by search query and toggles account status`() {
        val guru = UserAccount(uid = "g1", nama = "Ahzami, S.Pd., Gr.", email = "guru@muria.sch.id", role = "GURU")
        val siswaA = UserAccount(uid = "s1", nama = "Andi Pratama", email = "andi@muria.sch.id", role = "SISWA")
        val siswaB = UserAccount(uid = "s2", nama = "Siti Rahma", email = "siti@muria.sch.id", role = "SISWA")
        var updatedStatus = ""

        composeRule.setContent {
            ParijotoLearningTheme {
                AdminUsersTab(
                    users = listOf(guru, siswaA, siswaB),
                    busy = false,
                    onUpdateUser = { _, _, status -> updatedStatus = status }
                )
            }
        }

        composeRule.onNodeWithTag("admin_user_search_input").performTextReplacement("Siti")
        composeRule.onNodeWithText("Siti Rahma").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_toggle_user_s2").performClick()
        assertEquals("SUSPENDED", updatedStatus)
    }

    @Test
    fun `phase 3 teacher manages two students membership deactivation removal and meetings`() {
        val guru = UserAccount(uid = "g1", nama = "Ahzami, S.Pd., Gr.", email = "guru@muria.sch.id", role = "GURU")
        val cls = ClassRoom(
            id = "cls1",
            teacherUid = "g1",
            teacherName = guru.nama,
            name = "X IPA 1",
            classCode = "PRJ101"
        )
        val enrA = Enrollment(id = "cls1_s1", classId = "cls1", className = "X IPA 1", classCode = "PRJ101", teacherUid = "g1", studentUid = "s1", studentName = "Andi Pratama", status = "ACTIVE", progressPercent = 50)
        val enrB = Enrollment(id = "cls1_s2", classId = "cls1", className = "X IPA 1", classCode = "PRJ101", teacherUid = "g1", studentUid = "s2", studentName = "Siti Rahma", status = "ACTIVE", progressPercent = 25)

        var toggledUid = ""
        var removedId = ""

        composeRule.setContent {
            ParijotoLearningTheme {
                TeacherClassesTab(
                    user = guru,
                    systemSetting = SystemSetting(),
                    classes = listOf(cls),
                    enrollments = listOf(enrA, enrB),
                    busy = false,
                    onCreateClass = { _, _, _, _, _ -> },
                    onToggleArchive = {},
                    onToggleStudentStatus = { toggledUid = it.studentUid },
                    onRemoveStudent = { removedId = it }
                )
            }
        }

        composeRule.onNodeWithText("Anggota (2)").performClick()
        composeRule.onNodeWithText("Andi Pratama (50%)").assertIsDisplayed()
        composeRule.onNodeWithText("Siti Rahma (25%)").assertIsDisplayed()

        composeRule.onNodeWithTag("toggle_member_status_s2").performClick()
        assertEquals("s2", toggledUid)

        composeRule.onNodeWithTag("remove_member_s2").performClick()
        composeRule.onNodeWithTag("dialog_confirm_button").performClick()
        assertEquals("cls1_s2", removedId)
    }

    @Test
    fun `phase 3 teacher curriculum tab saves pertemuan learning module`() {
        val mod1 = LearningModule(
            id = "modul_1",
            moduleNumber = 1,
            title = "Konsep Keanekaragaman Hayati",
            subtitle = "Etnosains Parijoto",
            meetingLabel = "Pertemuan 1",
            durationMinutes = 90
        )
        var savedLabel = ""

        composeRule.setContent {
            ParijotoLearningTheme {
                TeacherCurriculumTab(
                    classes = emptyList(),
                    modules = listOf(mod1),
                    lessons = emptyList(),
                    groups = emptyList(),
                    assignments = emptyList(),
                    quizzes = emptyList(),
                    questions = emptyList(),
                    busy = false,
                    onSaveModule = { savedLabel = it.meetingLabel },
                    onSaveLesson = {},
                    onCreateGroup = { _, _, _ -> },
                    onCreateAssignment = { _, _, _, _, _, _, _ -> },
                    onCreateQuestion = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
                    onToggleQuizEvaluation = { _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("teacher_meeting_label_input").performTextReplacement("Pertemuan 1 (2 x 45 Menit)")
        composeRule.onNodeWithTag("teacher_save_module_button").performScrollTo().performClick()
        assertEquals("Pertemuan 1 (2 x 45 Menit)", savedLabel)
    }

    @Test
    fun `phase 3 student views joined class teacher profile and enters class code`() {
        val siswa = UserAccount(uid = "s1", nama = "Andi Pratama", email = "andi@muria.sch.id", role = "SISWA")
        val cls = ClassRoom(
            id = "cls1",
            teacherUid = "g1",
            teacherName = "Ahzami, S.Pd., Gr.",
            name = "X IPA 1 Muria",
            gradeLevel = "X SMA/MA",
            phase = "E",
            classCode = "PRJ77A",
            academicYear = "2026/2027",
            description = "Eksplorasi Keanekaragaman Hayati Muria"
        )
        val enr = Enrollment(
            id = "cls1_s1",
            classId = "cls1",
            className = "X IPA 1 Muria",
            classCode = "PRJ77A",
            teacherUid = "g1",
            studentUid = "s1",
            studentName = "Andi Pratama",
            status = "ACTIVE",
            progressPercent = 40
        )
        var joinedCode = ""

        composeRule.setContent {
            ParijotoLearningTheme {
                StudentHomeTab(
                    user = siswa,
                    systemSetting = SystemSetting(),
                    enrollments = listOf(enr),
                    activeEnrollment = enr,
                    enrolledClassesMap = mapOf("cls1" to cls),
                    modules = listOf(LearningModule(id = "m1", title = "Pertemuan 1 Parijoto", subtitle = "Etnosains")),
                    totalLessons = 5,
                    assignments = emptyList(),
                    submissions = emptyList(),
                    attempts = emptyList(),
                    publishedGrades = emptyList(),
                    announcements = emptyList(),
                    busy = false,
                    onSelectEnrollment = {},
                    onJoinClass = { joinedCode = it },
                    onOpenModules = {},
                    onOpenPbl = {},
                    onOpenEvaluation = {},
                    onOpenGlossary = {}
                )
            }
        }

        composeRule.onNodeWithText("Guru Pengampu: Ahzami, S.Pd., Gr.").assertIsDisplayed()
        composeRule.onNodeWithText("Kode: PRJ77A").assertIsDisplayed()
        composeRule.onNodeWithTag("toggle_join_another_class_button").performClick()
        composeRule.onNodeWithTag("join_class_code_input").performTextReplacement("PRJ99B")
        composeRule.onNodeWithTag("join_class_button").performClick()
        assertEquals("PRJ99B", joinedCode)
    }

    @Test
    fun `phase 3 student profile displays all joined classes and teacher info`() {
        val siswa = UserAccount(uid = "s2", nama = "Siti Rahma", email = "siti@muria.sch.id", role = "SISWA")
        val cls = ClassRoom(id = "cls1", teacherUid = "g1", teacherName = "Ahzami, S.Pd., Gr.", name = "X IPA 1 Muria", classCode = "PRJ77A")
        val enr = Enrollment(id = "cls1_s2", classId = "cls1", className = "X IPA 1 Muria", classCode = "PRJ77A", teacherUid = "g1", studentUid = "s2", studentName = "Siti Rahma")

        composeRule.setContent {
            ParijotoLearningTheme {
                StudentProfileTab(
                    user = siswa,
                    enrollments = listOf(enr),
                    enrolledClassesMap = mapOf("cls1" to cls),
                    downloadedModulesCount = 2,
                    publishedGrades = emptyList(),
                    busy = false,
                    onUpdateName = {},
                    onJoinClass = {},
                    onOpenGlossary = {},
                    onSignOut = {}
                )
            }
        }

        composeRule.onNodeWithText("Kelas yang Diikuti & Profil Guru (1)").assertIsDisplayed()
        composeRule.onNodeWithText("Guru Pengampu: Ahzami, S.Pd., Gr.").assertIsDisplayed()
    }

    @Test
    fun `phase 4 student modules tab covers modul 1 and 2 sections search bookmark and quiz`() {
        val allLessons = ParijotoCurriculumPart1.modul1Lessons + ParijotoCurriculumPart2.modul2Lessons
        val progress = StudentLearningProgress(
            id = "s1",
            studentUid = "s1",
            completedLessons = listOf("m1_l01"),
            bookmarkedLessons = listOf("m1_l12"),
            lessonReadProgress = mapOf("m1_l01" to 100, "m1_l12" to 60),
            lastLessonByModule = mapOf(ParijotoCurriculumPart1.modul1.id to "m1_l12"),
            readerFontScale = 1.0
        )
        var toggledBookmark = ""

        composeRule.setContent {
            ParijotoLearningTheme {
                StudentModulesTab(
                    modules = listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2),
                    lessons = allLessons,
                    enrollment = null,
                    learningProgress = progress,
                    downloadedModules = emptySet(),
                    quizzes = listOf(ParijotoAssessmentsAndGlossary.quizModul1, ParijotoAssessmentsAndGlossary.quizModul2),
                    questions = ParijotoAssessmentsAndGlossary.modul1Questions + ParijotoAssessmentsAndGlossary.modul2Questions,
                    attempts = emptyList(),
                    answerKeysCache = emptyMap(),
                    reflections = emptyList(),
                    busy = false,
                    aiStreamText = "",
                    aiLoading = false,
                    onToggleOffline = { _, _ -> },
                    onMarkLessonComplete = { _, _ -> },
                    onToggleBookmark = { toggledBookmark = it },
                    onFontScaleChange = {},
                    onRecordReadProgress = { _, _, _ -> },
                    onLoadAnswerKey = {},
                    onSubmitQuiz = { _, _, _, _, _, _, _ -> },
                    onSubmitReflection = { _, _, _, _ -> },
                    onAskAiTutor = { _, _ -> },
                    onOpenGlossary = {}
                )
            }
        }

        // Resume & Search & Bookmark filter in Modul 1
        composeRule.onNodeWithTag("resume_last_lesson_button").assertIsDisplayed()
        composeRule.onNodeWithTag("filter_lessons_bookmark").performClick()
        composeRule.onNodeWithText("12. Validasi Ilmiah Senyawa Bioaktif").assertIsDisplayed()
        composeRule.onNodeWithTag("bookmark_toggle_m1_l12").performClick()
        assertEquals("m1_l12", toggledBookmark)

        // Reset filter and test Search
        composeRule.onNodeWithTag("filter_lessons_all").performClick()
        composeRule.onNodeWithTag("module_search_input").performTextReplacement("Mitoni")
        composeRule.onNodeWithText("10. Tradisi Mitoni & Mapati Masyarakat Muria").assertIsDisplayed()

        // Switch to Modul 2 and verify Identitas, Studi Kasus, Kuis & Refleksi tabs
        composeRule.onNodeWithTag("select_module_2").performClick()
        composeRule.onNodeWithTag("module_section_tab_1").performClick()
        composeRule.onNodeWithText("Identitas Modul").assertIsDisplayed()
        composeRule.onNodeWithText("Peta Konsep Modul 2").assertIsDisplayed()

        composeRule.onNodeWithTag("module_section_tab_2").performClick()
        composeRule.onNodeWithText("Studi Kasus 2: Paradoks Konservasi & Kegagalan Budidaya Dataran Rendah").assertIsDisplayed()

        composeRule.onNodeWithTag("module_section_tab_3").performClick()
        composeRule.onNodeWithTag("module_start_quiz_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("module_submit_reflection_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `phase 4 lesson reader navigates subbab opens toc adjusts font size bookmarks and marks complete`() {
        val m1Lessons = ParijotoCurriculumPart1.modul1Lessons
        var selectedLessonId = m1Lessons[0].id
        var bookmarkedId = ""
        var savedScale = 1.0
        var markedComplete = false

        composeRule.setContent {
            ParijotoLearningTheme {
                val active = m1Lessons.first { it.id == selectedLessonId }
                LessonReaderScreen(
                    lesson = active,
                    moduleLessons = m1Lessons,
                    completedLessons = emptySet(),
                    bookmarkedLessons = emptySet(),
                    fontScale = savedScale.toFloat(),
                    savedScrollPercent = 20,
                    aiStreamText = "",
                    aiLoading = false,
                    onBack = {},
                    onSelectLesson = { selectedLessonId = it.id },
                    onToggleBookmark = { bookmarkedId = it },
                    onFontScaleChange = { savedScale = it },
                    onRecordReadProgress = { _, _, _ -> },
                    onMarkComplete = { markedComplete = true },
                    onAskAiTutor = { _, _ -> }
                )
            }
        }

        // Bookmark & Font Size adjustment
        composeRule.onNodeWithTag("lesson_bookmark_button").performClick()
        assertEquals("m1_l01", bookmarkedId)

        composeRule.onNodeWithTag("lesson_font_settings_button").performClick()
        composeRule.onNodeWithTag("font_scale_besar").performClick()
        assertEquals(1.15, savedScale, 0.01)

        // Navigate Next via Navigasi Antarsubbab
        composeRule.onNodeWithTag("lesson_next_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("m1_l02", selectedLessonId)

        // Mark complete
        composeRule.onNodeWithTag("mark_lesson_complete_button").performScrollTo().performClick()
        assertTrue(markedComplete)
    }

    @Test
    fun `phase 4 teacher curriculum edits existing lesson and deletes lesson with confirmation`() {
        val mod1 = ParijotoCurriculumPart1.modul1
        val les1 = ParijotoCurriculumPart1.modul1Lessons[0]
        var updatedTitle = ""
        var deletedId = ""

        composeRule.setContent {
            ParijotoLearningTheme {
                TeacherCurriculumTab(
                    classes = emptyList(),
                    modules = listOf(mod1),
                    lessons = listOf(les1),
                    groups = emptyList(),
                    assignments = emptyList(),
                    quizzes = emptyList(),
                    questions = emptyList(),
                    busy = false,
                    onSaveModule = {},
                    onSaveLesson = { updatedTitle = it.title },
                    onDeleteLesson = { deletedId = it },
                    onCreateGroup = { _, _, _ -> },
                    onCreateAssignment = { _, _, _, _, _, _, _ -> },
                    onCreateQuestion = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
                    onToggleQuizEvaluation = { _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("teacher_edit_lesson_m1_l01"))
        composeRule.onNodeWithTag("teacher_edit_lesson_m1_l01").performClick()
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("teacher_lesson_title_input"))
        composeRule.onNodeWithTag("teacher_lesson_title_input").performTextReplacement("1. Konsep Biodiversitas Muria (Revisi)")
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("teacher_save_lesson_button"))
        composeRule.onNodeWithTag("teacher_save_lesson_button").performClick()
        assertEquals("1. Konsep Biodiversitas Muria (Revisi)", updatedTitle)

        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("teacher_delete_lesson_m1_l01"))
        composeRule.onNodeWithTag("teacher_delete_lesson_m1_l01").performClick()
        composeRule.onNodeWithTag("dialog_confirm_button").performClick()
        assertEquals("m1_l01", deletedId)
    }

    @Test
    fun `phase 5 student navigates pbl stepper manages leader and discussion and saves reflection`() {
        val siswaA = UserAccount(uid = "s1", nama = "Andi Pratama", role = "SISWA")
        val enr = Enrollment(id = "cls1_s1", classId = "cls1", className = "X IPA 1", classCode = "PRJ77A", teacherUid = "g1", studentUid = "s1", studentName = "Andi Pratama")
        val grp = PblGroup(
            id = "grp1",
            classId = "cls1",
            teacherUid = "g1",
            name = "Kelompok Epifit 1",
            topic = "Konservasi Parijoto Lereng Muria",
            leaderUid = "s1",
            memberUids = listOf("s1", "s2"),
            memberNames = listOf("Andi Pratama", "Siti Rahma"),
            memberRoles = mapOf("s1" to "Ketua Kelompok", "s2" to "Analis Data"),
            pblPhase = 2,
            teacherComment = "Lengkapi bukti praklinis dan data 2023."
        )
        val elkm1 = ParijotoAssessmentsAndGlossary.defaultAssignments.first()
        val revisionSub = Submission(
            id = "${elkm1.id}_s1",
            assignmentId = elkm1.id,
            classId = "cls1",
            teacherUid = "g1",
            studentUid = "s1",
            studentName = "Andi Pratama",
            status = "REVISION_REQUIRED",
            teacherFeedback = "Perbaiki hipotesis hubungan naungan kanopi dengan Parijoto."
        )
        var updatedLeader = ""
        var sentChat = ""
        var savedPblReflection = ""

        composeRule.setContent {
            ParijotoLearningTheme {
                StudentPblTab(
                    user = siswaA,
                    enrollment = enr,
                    myGroups = listOf(grp),
                    openGroups = emptyList(),
                    groupDiscussions = listOf(
                        GroupDiscussionMessage(id = "m1", groupId = "grp1", classId = "cls1", senderUid = "g1", senderName = "Guru", senderRole = "GURU", pblPhase = 2, message = "Bagus, lanjutkan!")
                    ),
                    assignments = listOf(elkm1),
                    submissions = listOf(revisionSub),
                    classPresentations = emptyList(),
                    peerReviews = emptyList(),
                    reflections = emptyList(),
                    aiEvidence = null,
                    aiLoading = false,
                    busy = false,
                    onCreateGroup = { _, _ -> },
                    onJoinGroup = {},
                    onObserveGroupDiscussion = { _, _ -> },
                    onUpdateGroupWorkspace = { _, _, _, _, _, _, _, _, _, _ -> },
                    onUpdateGroupLeaderAndRoles = { _, leader, _, _ -> updatedLeader = leader },
                    onSendGroupDiscussion = { _, _, _, msg -> sentChat = msg },
                    onSaveSubmission = { _, _ -> },
                    onSubmitPeerReview = { _, _, _, _, _ -> },
                    onSubmitPblReflection = { _, _, ref -> savedPblReflection = ref },
                    onAnalyzeEvidenceAi = {}
                )
            }
        }

        // Stepper 5 fase & revision banner
        composeRule.onNodeWithTag("pbl_stepper_phase_4").performClick()
        composeRule.onNodeWithText("4. Mengembangkan & Menyajikan Solusi").assertIsDisplayed()
        composeRule.onNodeWithTag("student_pbl_list").performScrollToNode(hasTestTag("assignment_card_${elkm1.id}"))
        composeRule.onNodeWithText("Umpan Balik Guru: Perbaiki hipotesis hubungan naungan kanopi dengan Parijoto.").assertIsDisplayed()

        // Open group workspace -> assign leader -> discussion -> back
        composeRule.onNodeWithTag("student_pbl_list").performScrollToNode(hasTestTag("my_group_card_grp1"))
        composeRule.onNodeWithTag("my_group_card_grp1").performClick()
        composeRule.onNodeWithTag("group_tab_members").performClick()
        composeRule.onNodeWithTag("group_workspace_list").performScrollToNode(hasTestTag("set_leader_s2"))
        composeRule.onNodeWithTag("set_leader_s2").performClick()
        composeRule.onNodeWithTag("group_workspace_list").performScrollToNode(hasTestTag("save_member_roles_button"))
        composeRule.onNodeWithTag("save_member_roles_button").performClick()
        assertEquals("s2", updatedLeader)

        composeRule.onNodeWithTag("group_tab_discussion").performClick()
        composeRule.onNodeWithTag("group_workspace_list").performScrollToNode(hasTestTag("group_discussion_input"))
        composeRule.onNodeWithTag("group_discussion_input").performTextReplacement("Data flavonoid Muria 81,60 mg QE/g sudah kami catat.")
        composeRule.onNodeWithTag("send_group_discussion_button").performClick()
        assertEquals("Data flavonoid Muria 81,60 mg QE/g sudah kami catat.", sentChat)

        composeRule.onNodeWithTag("group_workspace_list").performScrollToNode(hasTestTag("group_workspace_back_button"))
        composeRule.onNodeWithTag("group_workspace_back_button").performClick()

        // Individual reflection
        composeRule.onNodeWithTag("student_pbl_list").performScrollToNode(hasTestTag("pbl_individual_reflection_input"))
        composeRule.onNodeWithTag("pbl_individual_reflection_input").performTextReplacement("Saya belajar membedakan bukti praklinis dan klinis.")
        composeRule.onNodeWithTag("student_pbl_list").performScrollToNode(hasTestTag("submit_pbl_reflection_button"))
        composeRule.onNodeWithTag("submit_pbl_reflection_button").performClick()
        assertEquals("Saya belajar membedakan bukti praklinis dan klinis.", savedPblReflection)
    }

    @Test
    fun `phase 5 student edits E-LKM 2 saves draft submits revision and evaluates peer presentation`() {
        val siswaA = UserAccount(uid = "s1", nama = "Andi Pratama", role = "SISWA")
        val enr = Enrollment(id = "cls1_s1", classId = "cls1", className = "X IPA 1", classCode = "PRJ77A", teacherUid = "g1", studentUid = "s1", studentName = "Andi Pratama")
        val elkm2 = ParijotoAssessmentsAndGlossary.defaultAssignments.first { it.type == "E_LKM_2" }
        val cerAsg = ParijotoAssessmentsAndGlossary.defaultAssignments.first { it.type == "PROYEK_CER" }
        val peerSub = Submission(
            id = "${cerAsg.id}_s2",
            assignmentId = cerAsg.id,
            classId = "cls1",
            teacherUid = "g1",
            studentUid = "s2",
            studentName = "Siti Rahma",
            cerClaim = "Panen selektif 30% menjaga regenerasi epifit Parijoto",
            status = "SUBMITTED"
        )
        var lastSaveWasFinal: Boolean? = null
        var lastEvidenceText = ""
        var peerComment = ""

        composeRule.setContent {
            ParijotoLearningTheme {
                StudentPblTab(
                    user = siswaA,
                    enrollment = enr,
                    myGroups = emptyList(),
                    openGroups = emptyList(),
                    groupDiscussions = emptyList(),
                    assignments = listOf(elkm2, cerAsg),
                    submissions = emptyList(),
                    classPresentations = listOf(peerSub),
                    peerReviews = emptyList(),
                    reflections = emptyList(),
                    aiEvidence = null,
                    aiLoading = false,
                    busy = false,
                    onCreateGroup = { _, _ -> },
                    onJoinGroup = {},
                    onObserveGroupDiscussion = { _, _ -> },
                    onUpdateGroupWorkspace = { _, _, _, _, _, _, _, _, _, _ -> },
                    onUpdateGroupLeaderAndRoles = { _, _, _, _ -> },
                    onSendGroupDiscussion = { _, _, _, _ -> },
                    onSaveSubmission = { sub, isFinal ->
                        lastSaveWasFinal = isFinal
                        lastEvidenceText = sub.scientificEvidence.orEmpty()
                    },
                    onSubmitPeerReview = { _, _, _, _, comment -> peerComment = comment },
                    onSubmitPblReflection = { _, _, _ -> },
                    onAnalyzeEvidenceAi = {}
                )
            }
        }

        // Open E-LKM 2 -> verify dataset -> save draft -> submit -> back
        composeRule.onNodeWithTag("student_pbl_list").performScrollToNode(hasTestTag("assignment_card_${elkm2.id}"))
        composeRule.onNodeWithTag("assignment_card_${elkm2.id}").performClick()
        composeRule.onNodeWithText("Lembar Data Acuan E-Modul (Analisis Mikroklimat & Fitokimia)").assertIsDisplayed()
        composeRule.onNodeWithTag("assignment_editor_list").performScrollToNode(hasTestTag("elkm2_evidence_input"))
        composeRule.onNodeWithTag("elkm2_evidence_input").performTextReplacement("Praklinis in vitro flavonoid; belum ada uji klinis manusia.")
        composeRule.onNodeWithTag("assignment_editor_list").performScrollToNode(hasTestTag("save_submission_draft_button"))
        composeRule.onNodeWithTag("save_submission_draft_button").performClick()
        assertEquals(false, lastSaveWasFinal)
        assertEquals("Praklinis in vitro flavonoid; belum ada uji klinis manusia.", lastEvidenceText)

        composeRule.onNodeWithTag("submit_assignment_button").performClick()
        assertEquals(true, lastSaveWasFinal)

        composeRule.onNodeWithTag("assignment_editor_list").performScrollToNode(hasTestTag("assignment_back_button"))
        composeRule.onNodeWithTag("assignment_back_button").performClick()

        // Open Presentation & Peer Review
        composeRule.onNodeWithTag("student_pbl_list").performScrollToNode(hasTestTag("open_presentations_button"))
        composeRule.onNodeWithTag("open_presentations_button").performClick()
        composeRule.onNodeWithTag("presentations_list").performScrollToNode(hasTestTag("open_peer_review_${peerSub.id}"))
        composeRule.onNodeWithTag("open_peer_review_${peerSub.id}").performClick()
        composeRule.onNodeWithTag("presentations_list").performScrollToNode(hasTestTag("peer_review_comment_input"))
        composeRule.onNodeWithTag("peer_review_comment_input").performTextReplacement("Argumen ekologi sangat kuat dan berbasis data.")
        composeRule.onNodeWithTag("presentations_list").performScrollToNode(hasTestTag("submit_peer_review_button"))
        composeRule.onNodeWithTag("submit_peer_review_button").performClick()
        assertEquals("Argumen ekologi sangat kuat dan berbasis data.", peerComment)
    }

    @Test
    fun `phase 5 teacher monitors group discussion and saves group comment`() {
        val cls = ClassRoom(id = "cls1", teacherUid = "g1", teacherName = "Ahzami, S.Pd., Gr.", name = "X IPA 1", classCode = "PRJ77A")
        val enr = Enrollment(id = "cls1_s1", classId = "cls1", className = "X IPA 1", classCode = "PRJ77A", teacherUid = "g1", studentUid = "s1", studentName = "Andi Pratama")
        val grp = PblGroup(id = "grp1", classId = "cls1", teacherUid = "g1", name = "Kelompok Epifit 1", leaderUid = "s1", memberUids = listOf("s1"), memberNames = listOf("Andi Pratama"), pblPhase = 3)
        val elkm2 = ParijotoAssessmentsAndGlossary.defaultAssignments.first { it.type == "E_LKM_2" }
        var savedGroupComment = ""

        composeRule.setContent {
            ParijotoLearningTheme {
                TeacherCurriculumTab(
                    classes = listOf(cls),
                    enrollments = listOf(enr),
                    modules = listOf(ParijotoCurriculumPart1.modul1),
                    lessons = emptyList(),
                    groups = listOf(grp),
                    groupDiscussions = emptyList(),
                    assignments = listOf(elkm2),
                    quizzes = emptyList(),
                    questions = emptyList(),
                    busy = false,
                    onSaveModule = {},
                    onSaveLesson = {},
                    onCreateGroup = { _, _, _ -> },
                    onSaveGroupComment = { _, comment -> savedGroupComment = comment },
                    onCreateAssignment = { _, _, _, _, _, _, _ -> },
                    onCreateQuestion = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
                    onToggleQuizEvaluation = { _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("PBL & LKM").performClick()
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("teacher_expand_group_grp1"))
        composeRule.onNodeWithTag("teacher_expand_group_grp1").performClick()
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("teacher_group_comment_input_grp1"))
        composeRule.onNodeWithTag("teacher_group_comment_input_grp1").performTextReplacement("Perjelas korelasi suhu 28,4°C terhadap penurunan epifit.")
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("teacher_save_group_comment_grp1"))
        composeRule.onNodeWithTag("teacher_save_group_comment_grp1").performClick()
        assertEquals("Perjelas korelasi suhu 28,4°C terhadap penurunan epifit.", savedGroupComment)
    }

    @Test
    fun `phase 5 teacher grades submission by rubric or returns for revision`() {
        val enr = Enrollment(id = "cls1_s1", classId = "cls1", className = "X IPA 1", classCode = "PRJ77A", teacherUid = "g1", studentUid = "s1", studentName = "Andi Pratama")
        val elkm2 = ParijotoAssessmentsAndGlossary.defaultAssignments.first { it.type == "E_LKM_2" }
        val sub = Submission(
            id = "sub1",
            assignmentId = elkm2.id,
            classId = "cls1",
            teacherUid = "g1",
            studentUid = "s1",
            studentName = "Andi Pratama",
            scientificEvidence = "Uji praklinis flavonoid",
            status = "SUBMITTED"
        )
        var revisionFeedback = ""
        var gradedFeedback = ""

        composeRule.setContent {
            ParijotoLearningTheme {
                TeacherGradingTab(
                    enrollments = listOf(enr),
                    assignments = listOf(elkm2),
                    submissions = listOf(sub),
                    quizzes = emptyList(),
                    attempts = emptyList(),
                    grades = emptyList(),
                    rubrics = ParijotoAssessmentsAndGlossary.defaultRubrics,
                    reflections = emptyList(),
                    busy = false,
                    onGradeAttempt = { _, _, _, _, _, _ -> },
                    onGradeSubmission = { _, _, _, _, _, fb, _ -> gradedFeedback = fb },
                    onReturnForRevision = { _, fb -> revisionFeedback = fb },
                    onGradeAttitude = { _, _, _, _, _, _ -> },
                    onExportCsv = { "" }
                )
            }
        }

        composeRule.onNodeWithText("LKM & CER").performClick()
        composeRule.onNodeWithText("Rubrik Penilaian Keterampilan (Proyek E-LKM & Kampanye CER)").assertIsDisplayed()
        composeRule.onNodeWithTag("teacher_grading_list").performScrollToNode(hasTestTag("submission_feedback_input_sub1"))
        composeRule.onNodeWithTag("submission_feedback_input_sub1").performTextReplacement("Tambahkan perbandingan kadar flavonoid Kudus vs Semarang.")
        composeRule.onNodeWithTag("teacher_grading_list").performScrollToNode(hasTestTag("return_revision_button_sub1"))
        composeRule.onNodeWithTag("return_revision_button_sub1").performClick()
        assertEquals("Tambahkan perbandingan kadar flavonoid Kudus vs Semarang.", revisionFeedback)

        composeRule.onNodeWithTag("publish_skill_grade_button_sub1").performClick()
        assertEquals("Tambahkan perbandingan kadar flavonoid Kudus vs Semarang.", gradedFeedback)
    }

    @Test
    fun `phase 6 quiz engine handles deterministic shuffling weighted mcq auto score and manual essay score`() {
        val quiz = ParijotoAssessmentsAndGlossary.quizPostTest.copy(
            questionCount = 8,
            shuffleQuestions = true,
            shuffleOptions = true
        )
        val allQs = ParijotoAssessmentsAndGlossary.postTestQuestions
        val selectedA1 = QuizEngineUtils.selectQuizQuestions(quiz, allQs, "siswa_1", 1)
        val selectedA1Repeat = QuizEngineUtils.selectQuizQuestions(quiz, allQs, "siswa_1", 1)
        val selectedA2 = QuizEngineUtils.selectQuizQuestions(quiz, allQs, "siswa_1", 2)

        assertEquals(8, selectedA1.size)
        assertEquals(selectedA1.map { it.id }, selectedA1Repeat.map { it.id })
        assertTrue(selectedA1.map { it.id } != selectedA2.map { it.id })

        val q1 = ParijotoAssessmentsAndGlossary.postTestQuestions.first()
        val opts = QuizEngineUtils.getShuffledOptionsForQuestion(quiz, q1, "siswa_1", 1)
        assertEquals(5, opts.size)
        assertEquals(listOf("A", "B", "C", "D", "E"), opts.map { it.displayLetter })

        val keys = ParijotoAssessmentsAndGlossary.defaultAnswerKeys[quiz.id]!!
        val mcqQs = allQs.filter { it.type == "MULTIPLE_CHOICE" }
        val essayQs = allQs.filter { it.type == "ESSAY" }

        val mcqSummary = QuizEngineUtils.computeMcqAutoScore(mcqQs, keys, keys)
        assertEquals(6, mcqSummary.correctCount)
        assertEquals(100.0, mcqSummary.percentageScore, 0.01)

        val essaySummary = QuizEngineUtils.computeEssayManualScore(
            essayQs,
            mapOf("q_pt_7" to 4, "q_pt_8" to 2)
        )
        assertEquals(6, essaySummary.earnedRubricScore)
        assertEquals(75.0, essaySummary.percentageScore, 0.01)

        val combined = QuizEngineUtils.combineQuizFinalScore(true, true, mcqSummary.percentageScore, essaySummary.percentageScore)
        assertEquals(90.0, combined, 0.01)

        assertTrue(QuizEngineUtils.isQuizAccessibleNow(quiz).canStartNew)
        assertFalse(QuizEngineUtils.isQuizAccessibleNow(quiz.copy(openForStudents = false)).canStartNew)
        assertFalse(QuizEngineUtils.isQuizAccessibleNow(quiz.copy(closed = true)).canStartNew)
    }

    @Test
    fun `phase 6 student evaluation filters 3 categories protects unpublished grades and shows history`() {
        val q1 = ParijotoAssessmentsAndGlossary.quizModul1
        val q2 = ParijotoAssessmentsAndGlossary.quizModul2.copy(openForStudents = false, scheduleLabel = "Minggu Depan")
        val pt = ParijotoAssessmentsAndGlossary.quizPostTest
        val unpublishedAttempt = QuizAttempt(
            id = "${pt.id}_s1_att1",
            quizId = pt.id,
            studentUid = "s1",
            studentName = "Andi",
            attemptNumber = 1,
            answers = mapOf("q_pt_1" to "C"),
            autoScore = 88.0,
            totalScore = 88.0,
            resultsPublished = false,
            status = "SUBMITTED"
        )

        composeRule.setContent {
            ParijotoLearningTheme {
                StudentEvaluationTab(
                    studentUid = "s1",
                    quizzes = listOf(q1, q2, pt),
                    questions = ParijotoAssessmentsAndGlossary.modul1Questions +
                        ParijotoAssessmentsAndGlossary.modul2Questions +
                        ParijotoAssessmentsAndGlossary.postTestQuestions,
                    attempts = listOf(unpublishedAttempt),
                    answerKeysCache = mapOf(pt.id to QuizAnswerKey(id = "master", keys = mapOf("q_pt_1" to "C"))),
                    publishedGrades = emptyList(),
                    reflections = emptyList(),
                    busy = false,
                    onLoadAnswerKey = {},
                    onSubmitQuiz = { _, _, _, _, _, _, _ -> },
                    onSubmitReflection = { _, _, _, _ -> }
                )
            }
        }

        // 3 categories shown and filterable
        composeRule.onNodeWithTag("quiz_card_quiz_modul_1").assertIsDisplayed()
        composeRule.onNodeWithTag("eval_filter_POST_TEST").performClick()
        composeRule.onNodeWithTag("quiz_card_quiz_post_test").assertIsDisplayed()

        // History hides unpublished score ("Nilai Dilindungi", score 88 NOT leaked)
        composeRule.onNodeWithTag("student_evaluation_list").performScrollToNode(hasTestTag("history_attempt_card_${unpublishedAttempt.id}"))
        composeRule.onNodeWithText("Nilai Dilindungi").assertIsDisplayed()
        composeRule.onNodeWithTag("history_attempt_card_${unpublishedAttempt.id}").performClick()
        composeRule.onNodeWithTag("quiz_unpublished_lock_banner").assertIsDisplayed()
    }

    @Test
    fun `phase 6 student quiz runner shows countdown timer shuffles options autosaves and submits`() {
        val quiz = ParijotoAssessmentsAndGlossary.quizModul1.copy(
            durationMinutes = 15,
            questionCount = 2,
            shuffleQuestions = true,
            shuffleOptions = true
        )
        var autosavedAnswer = ""
        var submittedAnswer = ""

        composeRule.setContent {
            ParijotoLearningTheme {
                QuizRunnerScreen(
                    quiz = quiz,
                    questions = ParijotoAssessmentsAndGlossary.modul1Questions,
                    studentUid = "s1",
                    attemptNumber = 1,
                    existingAttempt = null,
                    answerKey = null,
                    busy = false,
                    onBack = {},
                    onAutosave = { _, _, _, mcqs, _, _ ->
                        autosavedAnswer = mcqs.values.firstOrNull().orEmpty()
                    },
                    onSubmit = { _, _, _, mcqs, _ ->
                        submittedAnswer = mcqs.values.firstOrNull().orEmpty()
                    }
                )
            }
        }

        composeRule.onNodeWithTag("quiz_countdown_timer").assertIsDisplayed()
        val firstSelected = QuizEngineUtils.selectQuizQuestions(quiz, ParijotoAssessmentsAndGlossary.modul1Questions, "s1", 1).first()
        composeRule.onNodeWithTag("quiz_runner_list").performScrollToNode(hasTestTag("mcq_${firstSelected.id}_opt_E"))
        composeRule.onNodeWithTag("mcq_${firstSelected.id}_opt_E").performClick()
        composeRule.onNodeWithTag("quiz_manual_autosave_button").performClick()
        assertEquals("E", autosavedAnswer)

        composeRule.onNodeWithTag("quiz_runner_list").performScrollToNode(hasTestTag("submit_quiz_attempt_button"))
        composeRule.onNodeWithTag("submit_quiz_attempt_button").performClick()
        assertEquals("E", submittedAnswer)
    }

    @Test
    fun `phase 6 teacher configures quiz settings and adds case based hots question to bank soal`() {
        val quiz = ParijotoAssessmentsAndGlossary.quizPostTest
        var savedQuizConfig: Quiz? = null
        var savedQuestion: Question? = null
        var savedCorrectKey: String? = null

        composeRule.setContent {
            ParijotoLearningTheme {
                TeacherCurriculumTab(
                    classes = emptyList(),
                    modules = listOf(ParijotoCurriculumPart1.modul1),
                    lessons = emptyList(),
                    groups = emptyList(),
                    assignments = emptyList(),
                    quizzes = listOf(ParijotoAssessmentsAndGlossary.quizModul1, quiz),
                    questions = ParijotoAssessmentsAndGlossary.postTestQuestions,
                    busy = false,
                    onSaveModule = {},
                    onSaveLesson = {},
                    onCreateGroup = { _, _, _ -> },
                    onCreateAssignment = { _, _, _, _, _, _, _ -> },
                    onSaveQuizConfig = { savedQuizConfig = it },
                    onSaveBankQuestion = { _, q, key, _ ->
                        savedQuestion = q
                        savedCorrectKey = key
                    }
                )
            }
        }

        composeRule.onNodeWithText("Kuis & Soal").performClick()
        composeRule.onNodeWithTag("teacher_quiz_tab_POST_TEST").performClick()
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("quiz_duration_input"))
        composeRule.onNodeWithTag("quiz_duration_input").performScrollTo().performTextReplacement("60")
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("quiz_max_attempts_input"))
        composeRule.onNodeWithTag("quiz_max_attempts_input").performScrollTo().performTextReplacement("1")
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("teacher_save_quiz_config_button"))
        composeRule.onNodeWithTag("teacher_save_quiz_config_button").performScrollTo().performClick()
        assertEquals(60, savedQuizConfig?.durationMinutes)
        assertEquals(1, savedQuizConfig?.maxAttempts)

        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("bank_stimulus_input"))
        composeRule.onNodeWithTag("bank_stimulus_input").performScrollTo().performTextReplacement("Wacana kasus penurunan kelembapan mikro Muria.")
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("bank_prompt_input"))
        composeRule.onNodeWithTag("bank_prompt_input").performScrollTo().performTextReplacement("Apa mekanisme adaptasi konservasi yang tepat?")
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("bank_opt_a"))
        composeRule.onNodeWithTag("bank_opt_a").performScrollTo().performTextReplacement("Kultur jaringan ex-situ")
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("bank_opt_b"))
        composeRule.onNodeWithTag("bank_opt_b").performScrollTo().performTextReplacement("Pembakaran hutan")
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("bank_key_A"))
        composeRule.onNodeWithTag("bank_key_A").performScrollTo().performClick()
        composeRule.onNodeWithTag("teacher_curriculum_list").performScrollToNode(hasTestTag("teacher_save_bank_question_button"))
        composeRule.onNodeWithTag("teacher_save_bank_question_button").performScrollTo().performClick()
        assertEquals("Apa mekanisme adaptasi konservasi yang tepat?", savedQuestion?.prompt)
        assertEquals("A", savedCorrectKey)
    }

    @Test
    fun `phase 6 teacher grades quiz with separate auto pg and manual essay rubric and shows recap`() {
        val quiz = ParijotoAssessmentsAndGlossary.quizPostTest
        val attempt = QuizAttempt(
            id = "${quiz.id}_s1_att1",
            quizId = quiz.id,
            classId = "cls1",
            teacherUid = "g1",
            studentUid = "s1",
            studentName = "Andi Pratama",
            attemptNumber = 1,
            answers = ParijotoAssessmentsAndGlossary.defaultAnswerKeys[quiz.id] ?: emptyMap(),
            essayAnswers = mapOf(
                "q_pt_7" to "Kultur jaringan ex-situ berbasis totipotensi sel.",
                "q_pt_8" to "Fotoinhibisi dan plasmolisis akibat hilangnya kanopi."
            ),
            status = "SUBMITTED"
        )
        var lastPublishFlag: Boolean? = null
        var lastEssayScores: Map<String, Int> = emptyMap()

        composeRule.setContent {
            ParijotoLearningTheme {
                TeacherGradingTab(
                    enrollments = emptyList(),
                    assignments = emptyList(),
                    submissions = emptyList(),
                    quizzes = listOf(quiz),
                    questions = ParijotoAssessmentsAndGlossary.postTestQuestions,
                    answerKeys = mapOf(
                        quiz.id to QuizAnswerKey(
                            id = "master",
                            keys = ParijotoAssessmentsAndGlossary.defaultAnswerKeys[quiz.id] ?: emptyMap()
                        )
                    ),
                    attempts = listOf(attempt),
                    grades = listOf(
                        GradeRecord(
                            id = "grd1",
                            studentUid = "s1",
                            studentName = "Andi Pratama",
                            assessmentType = "PENGETAHUAN",
                            sourceId = quiz.id,
                            sourceTitle = quiz.title,
                            knowledgeScore = 100.0,
                            essayScore = 75.0,
                            finalScore = 90.0,
                            published = true
                        )
                    ),
                    reflections = emptyList(),
                    busy = false,
                    onGradeQuizAttempt = { _, _, essayMap, _, publish ->
                        lastEssayScores = essayMap
                        lastPublishFlag = publish
                    },
                    onGradeSubmission = { _, _, _, _, _, _, _ -> },
                    onGradeAttitude = { _, _, _, _, _, _ -> },
                    onExportCsv = { "" }
                )
            }
        }

        composeRule.onNodeWithText("Penilaian Otomatis Pilihan Ganda").assertIsDisplayed()
        composeRule.onNodeWithText("Penilaian Manual Soal Uraian (Rubrik 0–4) • Skor Uraian: 75").assertIsDisplayed()
        composeRule.onNodeWithTag("teacher_grading_list").performScrollToNode(hasTestTag("save_attempt_draft_${attempt.id}"))
        composeRule.onNodeWithTag("save_attempt_draft_${attempt.id}").performClick()
        assertEquals(false, lastPublishFlag)
        assertEquals(2, lastEssayScores.size)

        composeRule.onNodeWithTag("publish_attempt_grade_${attempt.id}").performClick()
        assertEquals(true, lastPublishFlag)

        composeRule.onNodeWithText("Sikap & Rekap").performClick()
        composeRule.onNodeWithTag("teacher_grade_recap_card").assertIsDisplayed()
    }

    @Test
    fun `phase 7 report engine calculates traceable weighted scores material pct task recap and filters`() {
        val cls1 = ClassRoom(id = "cls1", teacherUid = "g1", teacherName = "Ahzami, S.Pd., Gr.", name = "X IPA 1", classCode = "PRJ77A")
        val cls2 = ClassRoom(id = "cls2", teacherUid = "g1", teacherName = "Ahzami, S.Pd., Gr.", name = "X IPA 2", classCode = "PRJ88B")
        val allLessons = ParijotoCurriculumPart1.modul1Lessons + ParijotoCurriculumPart2.modul2Lessons
        val enr1 = Enrollment(
            id = "cls1_s1",
            classId = "cls1",
            className = "X IPA 1",
            classCode = "PRJ77A",
            teacherUid = "g1",
            studentUid = "s1",
            studentName = "Andi Pratama",
            completedLessons = allLessons.take(6).map { it.id }
        )
        val enr2 = Enrollment(
            id = "cls2_s2",
            classId = "cls2",
            className = "X IPA 2",
            classCode = "PRJ88B",
            teacherUid = "g1",
            studentUid = "s2",
            studentName = "Siti Rahma",
            completedLessons = allLessons.take(3).map { it.id }
        )

        val elkm1 = ParijotoAssessmentsAndGlossary.defaultAssignments.first { it.type == "E_LKM_1" }
        val elkm2 = ParijotoAssessmentsAndGlossary.defaultAssignments.first { it.type == "E_LKM_2" }
        val cer = ParijotoAssessmentsAndGlossary.defaultAssignments.first { it.type == "PROYEK_CER" }

        val grades = listOf(
            GradeRecord(id = "g_q1", classId = "cls1", studentUid = "s1", studentName = "Andi Pratama", assessmentType = "PENGETAHUAN", sourceId = ParijotoAssessmentsAndGlossary.quizModul1.id, sourceTitle = "Kuis Modul 1", finalScore = 80.0, published = true),
            GradeRecord(id = "g_pt", classId = "cls1", studentUid = "s1", studentName = "Andi Pratama", assessmentType = "PENGETAHUAN", sourceId = ParijotoAssessmentsAndGlossary.quizPostTest.id, sourceTitle = "Evaluasi Akhir", finalScore = 90.0, published = true),
            GradeRecord(id = "g_sk", classId = "cls1", studentUid = "s1", studentName = "Andi Pratama", assessmentType = "KETERAMPILAN", sourceId = elkm1.id, sourceTitle = elkm1.title, finalScore = 85.0, published = true),
            GradeRecord(id = "g_pr", classId = "cls1", studentUid = "s1", studentName = "Andi Pratama", assessmentType = "KETERAMPILAN", sourceId = cer.id, sourceTitle = cer.title, finalScore = 95.0, published = true),
            GradeRecord(id = "g_at", classId = "cls1", studentUid = "s1", studentName = "Andi Pratama", assessmentType = "SIKAP", sourceId = "attitude_cls1_s1", sourceTitle = "Observasi Sikap", finalScore = 100.0, published = true),
            GradeRecord(id = "g_s2", classId = "cls2", studentUid = "s2", studentName = "Siti Rahma", assessmentType = "PENGETAHUAN", sourceId = ParijotoAssessmentsAndGlossary.quizPostTest.id, sourceTitle = "Evaluasi Akhir", finalScore = 70.0, published = false)
        )

        val subs = listOf(
            Submission(id = "sub1", assignmentId = elkm1.id, classId = "cls1", studentUid = "s1", studentName = "Andi Pratama", status = "GRADED"),
            Submission(id = "sub2", assignmentId = cer.id, classId = "cls1", studentUid = "s1", studentName = "Andi Pratama", status = "SUBMITTED")
        )

        val full = LearningReportEngine.compute(
            filter = ReportFilter(),
            publishedOnly = false,
            classes = listOf(cls1, cls2),
            enrollments = listOf(enr1, enr2),
            modules = listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2),
            lessons = allLessons,
            assignments = listOf(elkm1, elkm2, cer),
            submissions = subs,
            quizzes = listOf(ParijotoAssessmentsAndGlossary.quizModul1, ParijotoAssessmentsAndGlossary.quizModul2, ParijotoAssessmentsAndGlossary.quizPostTest),
            grades = grades
        )

        assertEquals(2, full.classReports.size)
        val andi = full.studentReports.first { it.studentUid == "s1" }
        // Weighted sum: 80*0.20 + 90*0.25 + 85*0.20 + 95*0.20 + 100*0.15 = 16 + 22.5 + 17 + 19 + 15 = 89.5
        assertEquals(89.5, andi.finalReportScore, 0.01)
        assertEquals(100, andi.activeWeightPercent)
        assertEquals(5, andi.traces.size)
        assertEquals(6, andi.completedLessonsCount)
        assertEquals(2, andi.submittedAssignmentsCount)
        assertEquals(3, andi.totalAssignmentsCount)

        // Filter by class + meeting 1
        val filtered = LearningReportEngine.compute(
            filter = ReportFilter(classId = "cls1", meetingNumber = 1),
            publishedOnly = false,
            classes = listOf(cls1, cls2),
            enrollments = listOf(enr1, enr2),
            modules = listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2),
            lessons = allLessons,
            assignments = listOf(elkm1, elkm2, cer),
            submissions = subs,
            quizzes = listOf(ParijotoAssessmentsAndGlossary.quizModul1, ParijotoAssessmentsAndGlossary.quizModul2, ParijotoAssessmentsAndGlossary.quizPostTest),
            grades = grades
        )
        assertEquals(1, filtered.studentReports.size)
        assertEquals(1, filtered.classReports.size)
        assertTrue(filtered.studentReports.first().traces.none { it.sourceId == cer.id })
    }

    @Test
    fun `phase 7 student mode hides unpublished grades and renders report view`() {
        val cls = ClassRoom(id = "cls1", teacherUid = "g1", teacherName = "Ahzami, S.Pd., Gr.", name = "X IPA 1", classCode = "PRJ77A")
        val enr = Enrollment(id = "cls1_s1", classId = "cls1", className = "X IPA 1", classCode = "PRJ77A", teacherUid = "g1", studentUid = "s1", studentName = "Andi Pratama")
        val allLessons = ParijotoCurriculumPart1.modul1Lessons + ParijotoCurriculumPart2.modul2Lessons
        val published = GradeRecord(
            id = "g_pub",
            classId = "cls1",
            studentUid = "s1",
            studentName = "Andi Pratama",
            assessmentType = "PENGETAHUAN",
            sourceId = ParijotoAssessmentsAndGlossary.quizModul1.id,
            sourceTitle = "Kuis Modul 1",
            finalScore = 84.0,
            published = true
        )
        val hiddenUnpublished = GradeRecord(
            id = "g_priv",
            classId = "cls1",
            studentUid = "s1",
            studentName = "Andi Pratama",
            assessmentType = "PENGETAHUAN",
            sourceId = ParijotoAssessmentsAndGlossary.quizPostTest.id,
            sourceTitle = "Evaluasi Akhir Rahasia",
            finalScore = 45.0,
            published = false
        )

        val studentOnly = LearningReportEngine.compute(
            filter = ReportFilter(studentUid = "s1"),
            publishedOnly = true,
            classes = listOf(cls),
            enrollments = listOf(enr),
            modules = listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2),
            lessons = allLessons,
            assignments = ParijotoAssessmentsAndGlossary.defaultAssignments,
            submissions = emptyList(),
            quizzes = listOf(ParijotoAssessmentsAndGlossary.quizModul1, ParijotoAssessmentsAndGlossary.quizPostTest),
            grades = listOf(published, hiddenUnpublished),
            studentPersonalProgress = StudentLearningProgress(studentUid = "s1", completedLessons = listOf("m1_l01", "m1_l02"))
        )
        val stu = studentOnly.studentReports.first()
        assertEquals(1, stu.unpublishedSuppressedCount)
        assertEquals(84.0, stu.finalReportScore, 0.01)
        assertEquals(null, stu.postTestScore)

        composeRule.setContent {
            ParijotoLearningTheme {
                StudentEvaluationTab(
                    studentUid = "s1",
                    quizzes = listOf(ParijotoAssessmentsAndGlossary.quizModul1, ParijotoAssessmentsAndGlossary.quizPostTest),
                    questions = emptyList(),
                    attempts = emptyList(),
                    answerKeysCache = emptyMap(),
                    publishedGrades = listOf(published),
                    reflections = emptyList(),
                    busy = false,
                    enrollments = listOf(enr),
                    enrolledClassesMap = mapOf("cls1" to cls),
                    learningProgress = StudentLearningProgress(studentUid = "s1", completedLessons = listOf("m1_l01")),
                    onLoadAnswerKey = {},
                    onSubmitQuiz = { _, _, _, _, _, _, _ -> },
                    onSubmitReflection = { _, _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("student_eval_mode_report").performClick()
        composeRule.onNodeWithTag("student_evaluation_list").performScrollToNode(hasTestTag("student_report_published_notice"))
        composeRule.onNodeWithTag("student_report_published_notice").assertIsDisplayed()
        composeRule.onNodeWithTag("student_evaluation_list").performScrollToNode(hasTestTag("student_report_card_s1_cls1"))
        composeRule.onNodeWithTag("student_report_card_s1_cls1").assertIsDisplayed()
    }

    @Test
    fun `phase 7 pdf and excel export generators produce valid non-empty documents`() {
        val cls = ClassRoom(id = "cls1", teacherUid = "g1", teacherName = "Ahzami, S.Pd., Gr.", name = "X IPA 1", classCode = "PRJ77A")
        val enr = Enrollment(id = "cls1_s1", classId = "cls1", className = "X IPA 1", classCode = "PRJ77A", teacherUid = "g1", studentUid = "s1", studentName = "Andi Pratama")
        val grade = GradeRecord(
            id = "g1",
            classId = "cls1",
            studentUid = "s1",
            studentName = "Andi Pratama",
            assessmentType = "PENGETAHUAN",
            sourceId = ParijotoAssessmentsAndGlossary.quizModul1.id,
            sourceTitle = "Kuis Modul 1",
            finalScore = 92.0,
            published = true
        )
        val res = LearningReportEngine.compute(
            filter = ReportFilter(),
            publishedOnly = false,
            classes = listOf(cls),
            enrollments = listOf(enr),
            modules = listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2),
            lessons = ParijotoCurriculumPart1.modul1Lessons,
            assignments = ParijotoAssessmentsAndGlossary.defaultAssignments,
            submissions = emptyList(),
            quizzes = listOf(ParijotoAssessmentsAndGlossary.quizModul1),
            grades = listOf(grade)
        )

        val pdfBytes = ReportExportManager.buildPdfReportBytes(
            title = "Laporan Hasil Belajar",
            subtitle = "Parijoto Learning",
            result = res,
            isStudentMode = false
        )
        assertTrue(pdfBytes.size > 200)
        assertEquals("%PDF-", String(pdfBytes.copyOfRange(0, 5), Charsets.US_ASCII))

        val xlsxBytes = ReportExportManager.buildExcelWorkbookBytes(
            title = "Rekapitulasi Penilaian",
            subtitle = "Parijoto Learning",
            result = res,
            isStudentMode = false
        )
        assertTrue(xlsxBytes.size > 300)

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(xlsxBytes)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                entries[entry.name] = String(zis.readBytes(), Charsets.UTF_8)
            }
        }
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet2.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet3.xml"))
        assertTrue(entries["xl/workbook.xml"].orEmpty().contains("Rekap_Siswa"))
        assertTrue(entries["xl/worksheets/sheet1.xml"].orEmpty().contains("Andi Pratama"))
        assertTrue(entries["xl/worksheets/sheet3.xml"].orEmpty().contains("Kuis Modul 1"))
    }

    @Test
    fun `phase 7 teacher and admin report tabs display filters export actions charts and traceable components`() {
        val cls1 = ClassRoom(id = "cls1", teacherUid = "g1", teacherName = "Ahzami, S.Pd., Gr.", name = "X IPA 1", classCode = "PRJ77A")
        val enr1 = Enrollment(id = "cls1_s1", classId = "cls1", className = "X IPA 1", classCode = "PRJ77A", teacherUid = "g1", studentUid = "s1", studentName = "Andi Pratama")
        val grd = GradeRecord(
            id = "g1",
            classId = "cls1",
            studentUid = "s1",
            studentName = "Andi Pratama",
            assessmentType = "SIKAP",
            sourceId = "att_1",
            sourceTitle = "Observasi Sikap",
            finalScore = 88.0,
            published = true
        )

        composeRule.setContent {
            ParijotoLearningTheme {
                TeacherGradingTab(
                    enrollments = listOf(enr1),
                    assignments = ParijotoAssessmentsAndGlossary.defaultAssignments,
                    submissions = emptyList(),
                    quizzes = listOf(ParijotoAssessmentsAndGlossary.quizModul1),
                    attempts = emptyList(),
                    grades = listOf(grd),
                    reflections = emptyList(),
                    busy = false,
                    classes = listOf(cls1),
                    onGradeSubmission = { _, _, _, _, _, _, _ -> },
                    onGradeAttitude = { _, _, _, _, _, _ -> },
                    onExportCsv = { "" }
                )
            }
        }

        composeRule.onNodeWithTag("teacher_grading_tab_reports").performClick()
        composeRule.onNodeWithTag("teacher_grading_list").performScrollToNode(hasTestTag("report_filter_card"))
        composeRule.onNodeWithTag("report_filter_card").assertIsDisplayed()
        composeRule.onNodeWithTag("teacher_grading_list").performScrollToNode(hasTestTag("export_pdf_button"))
        composeRule.onNodeWithTag("export_pdf_button").assertIsDisplayed()
        composeRule.onNodeWithTag("export_excel_button").assertIsDisplayed()
        composeRule.onNodeWithTag("teacher_grading_list").performScrollToNode(hasTestTag("progression_line_chart_card"))
        composeRule.onNodeWithTag("progression_line_chart_card").assertIsDisplayed()
        composeRule.onNodeWithTag("teacher_grading_list").performScrollToNode(hasTestTag("toggle_trace_s1_cls1"))
        composeRule.onNodeWithTag("toggle_trace_s1_cls1").performClick()
        composeRule.onNodeWithText("[Sikap • 15%] Observasi Sikap").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `phase 7 admin reports tab aggregates multiple classes and students`() {
        val cls1 = ClassRoom(id = "cls1", teacherUid = "g1", teacherName = "Guru A", name = "X IPA 1", classCode = "AAA111")
        val cls2 = ClassRoom(id = "cls2", teacherUid = "g2", teacherName = "Guru B", name = "X IPA 2", classCode = "BBB222")
        val enr1 = Enrollment(id = "e1", classId = "cls1", className = "X IPA 1", classCode = "AAA111", teacherUid = "g1", studentUid = "s1", studentName = "Andi")
        val enr2 = Enrollment(id = "e2", classId = "cls2", className = "X IPA 2", classCode = "BBB222", teacherUid = "g2", studentUid = "s2", studentName = "Budi")

        composeRule.setContent {
            ParijotoLearningTheme {
                AdminReportsTab(
                    classes = listOf(cls1, cls2),
                    enrollments = listOf(enr1, enr2),
                    modules = listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2),
                    lessons = ParijotoCurriculumPart1.modul1Lessons,
                    assignments = ParijotoAssessmentsAndGlossary.defaultAssignments,
                    submissions = emptyList(),
                    quizzes = listOf(ParijotoAssessmentsAndGlossary.quizModul1),
                    grades = emptyList()
                )
            }
        }

        composeRule.onNodeWithTag("admin_reports_list").performScrollToNode(hasTestTag("class_report_card_cls1"))
        composeRule.onNodeWithTag("class_report_card_cls1").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_reports_list").performScrollToNode(hasTestTag("class_report_card_cls2"))
        composeRule.onNodeWithTag("class_report_card_cls2").assertIsDisplayed()
    }
}




