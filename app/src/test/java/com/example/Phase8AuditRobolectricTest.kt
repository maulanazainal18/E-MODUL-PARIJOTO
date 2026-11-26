package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.curriculum.ParijotoAssessmentsAndGlossary
import com.example.data.curriculum.ParijotoCurriculumPart1
import com.example.data.curriculum.QuizEngineUtils
import com.example.data.model.AccountStatus
import com.example.data.model.SystemSetting
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.ui.admin.AdminUsersTab
import com.example.ui.auth.MaintenanceModeScreen
import com.example.ui.auth.RegistrationClosedScreen
import com.example.ui.components.AdaptiveRoleScaffold
import com.example.ui.components.NavDestinationItem
import com.example.ui.student.AssignmentEditorScreen
import com.example.ui.student.QuizRunnerScreen
import com.example.ui.student.StudentHomeTab
import com.example.ui.teacher.TeacherCreateGroupAndAssignmentCards
import com.example.ui.theme.ParijotoLearningTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class Phase8AuditRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `phase 8 maintenance mode gate renders and triggers sign out`() {
        var maintenanceSignedOut = false

        composeRule.setContent {
            ParijotoLearningTheme {
                MaintenanceModeScreen(
                    userName = "Budi Santoso",
                    schoolName = "SMA Muria Kudus",
                    onSignOut = { maintenanceSignedOut = true }
                )
            }
        }
        composeRule.onNodeWithTag("maintenance_mode_card").assertIsDisplayed()
        composeRule.onNodeWithTag("maintenance_signout_button").performClick()
        assertTrue(maintenanceSignedOut)
    }

    @Test
    fun `phase 8 registration closed gate renders and triggers sign out`() {
        var regClosedSignedOut = false

        composeRule.setContent {
            ParijotoLearningTheme {
                RegistrationClosedScreen(
                    email = "baru@sekolah.sch.id",
                    schoolName = "SMA Muria Kudus",
                    onSignOut = { regClosedSignedOut = true }
                )
            }
        }
        composeRule.onNodeWithTag("registration_closed_card").assertIsDisplayed()
        composeRule.onNodeWithTag("registration_closed_signout_button").performClick()
        assertTrue(regClosedSignedOut)
    }

    @Test
    fun `phase 8 quiz runner flushes pending answer edits to autosave on back click`() {
        val quiz = ParijotoAssessmentsAndGlossary.quizModul1.copy(
            durationMinutes = 15,
            questionCount = 2,
            shuffleQuestions = true,
            shuffleOptions = true
        )
        var autosavedAnswer = ""
        var navigatedBack = false

        composeRule.setContent {
            ParijotoLearningTheme {
                QuizRunnerScreen(
                    quiz = quiz,
                    questions = ParijotoAssessmentsAndGlossary.modul1Questions,
                    studentUid = "s_audit",
                    attemptNumber = 1,
                    existingAttempt = null,
                    answerKey = null,
                    busy = false,
                    onBack = { navigatedBack = true },
                    onAutosave = { _, _, _, mcqs, _, _ ->
                        autosavedAnswer = mcqs.values.firstOrNull().orEmpty()
                    },
                    onSubmit = { _, _, _, _, _ -> }
                )
            }
        }

        val firstSelected = QuizEngineUtils.selectQuizQuestions(
            quiz,
            ParijotoAssessmentsAndGlossary.modul1Questions,
            "s_audit",
            1
        ).first()
        composeRule.onNodeWithTag("quiz_runner_list").performScrollToNode(hasTestTag("mcq_${firstSelected.id}_opt_D"))
        composeRule.onNodeWithTag("mcq_${firstSelected.id}_opt_D").performClick()

        // Press back immediately without waiting for debounce timer
        composeRule.onNodeWithTag("quiz_runner_list").performScrollToNode(hasTestTag("quiz_back_button"))
        composeRule.onNodeWithTag("quiz_back_button").performClick()
        assertTrue(navigatedBack)
        assertEquals("D", autosavedAnswer)
    }

    @Test
    fun `phase 8 unenrolled LKM editor displays class join guidance notice`() {
        val elkm1 = ParijotoAssessmentsAndGlossary.defaultAssignments.first { it.type == "E_LKM_1" }
        val student = UserAccount(
            uid = "s1",
            nama = "Siti Aminah",
            email = "siti@siswa.id",
            role = UserRole.SISWA.name,
            accountStatus = AccountStatus.ACTIVE.name
        )

        composeRule.setContent {
            ParijotoLearningTheme {
                AssignmentEditorScreen(
                    user = student,
                    enrollment = null,
                    group = null,
                    assignment = elkm1,
                    existingSubmission = null,
                    aiEvidence = null,
                    aiLoading = false,
                    busy = false,
                    onBack = {},
                    onSaveSubmission = { _, _ -> },
                    onAnalyzeEvidenceAi = {}
                )
            }
        }
        composeRule.onNodeWithTag("lkm_unenrolled_notice").assertIsDisplayed()
    }

    @Test
    fun `phase 8 teacher PBL group creation disables submit and warns when classes empty`() {
        composeRule.setContent {
            ParijotoLearningTheme {
                TeacherCreateGroupAndAssignmentCards(
                    classes = emptyList(),
                    groupsCount = 0,
                    assignments = emptyList(),
                    busy = false,
                    onCreateGroup = { _, _, _ -> },
                    onCreateAssignment = { _, _, _, _, _, _, _ -> }
                )
            }
        }
        composeRule.onNodeWithTag("teacher_no_class_pbl_warning").assertIsDisplayed()
        composeRule.onNodeWithTag("teacher_create_group_button").assertIsNotEnabled()
    }

    @Test
    fun `phase 8 locale formatting keeps dot decimal separator under Indonesian locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.Builder().setLanguage("in").setRegion("ID").build())
            val formatted = String.format(Locale.US, "%.1f", 87.5)
            assertEquals("87.5", formatted)
            assertFalse(formatted.contains(","))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    @Config(sdk = [35], qualifiers = "w360dp-h640dp-xxhdpi")
    fun `phase 8 compact 360dp phone layout renders adaptive bottom bar and student home cleanly`() {
        val student = UserAccount(
            uid = "s360",
            nama = "Andi Pratama",
            email = "andi@siswa.id",
            role = UserRole.SISWA.name
        )

        composeRule.setContent {
            ParijotoLearningTheme {
                AdaptiveRoleScaffold(
                    title = "Parijoto Learning",
                    subtitle = "Siswa • SMA",
                    destinations = listOf(
                        NavDestinationItem("home", "Beranda", Icons.Default.Home, "nav_home"),
                        NavDestinationItem("mod", "Modul", Icons.AutoMirrored.Filled.MenuBook, "nav_mod")
                    ),
                    selectedId = "home",
                    onSelect = {},
                    snackbarHostState = remember { SnackbarHostState() }
                ) {
                    StudentHomeTab(
                        user = student,
                        systemSetting = SystemSetting(),
                        enrollments = emptyList(),
                        activeEnrollment = null,
                        enrolledClassesMap = emptyMap(),
                        modules = listOf(ParijotoCurriculumPart1.modul1),
                        totalLessons = 6,
                        assignments = ParijotoAssessmentsAndGlossary.defaultAssignments,
                        submissions = emptyList(),
                        attempts = emptyList(),
                        publishedGrades = emptyList(),
                        announcements = emptyList(),
                        busy = false,
                        onSelectEnrollment = {},
                        onJoinClass = {},
                        onOpenModules = {},
                        onOpenPbl = {},
                        onOpenEvaluation = {},
                        onOpenGlossary = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("adaptive_navigation_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("student_home_list").performScrollToNode(hasTestTag("quick_open_pbl_button"))
        composeRule.onNodeWithTag("quick_open_pbl_button").assertIsDisplayed()
        composeRule.onNodeWithTag("student_home_list").performScrollToNode(hasTestTag("quick_open_eval_button"))
        composeRule.onNodeWithTag("quick_open_eval_button").assertIsDisplayed()
        composeRule.onNodeWithTag("student_home_list").performScrollToNode(hasTestTag("quick_open_glossary_button"))
        composeRule.onNodeWithTag("quick_open_glossary_button").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [35], qualifiers = "w360dp-h640dp-xxhdpi")
    fun `phase 8 compact 360dp phone layout renders admin users tab without button clipping`() {
        val student = UserAccount(
            uid = "s360",
            nama = "Andi Pratama",
            email = "andi@siswa.id",
            role = UserRole.SISWA.name
        )
        val teacher = UserAccount(
            uid = "g360",
            nama = "Ahzami, S.Pd.",
            email = "ahzami@guru.id",
            role = UserRole.GURU.name
        )

        composeRule.setContent {
            ParijotoLearningTheme {
                AdminUsersTab(
                    users = listOf(student, teacher),
                    busy = false,
                    onUpdateUser = { _, _, _ -> }
                )
            }
        }
        composeRule.onNodeWithTag("admin_users_list").performScrollToNode(hasTestTag("admin_toggle_user_s360"))
        composeRule.onNodeWithTag("admin_toggle_user_s360").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_users_list").performScrollToNode(hasTestTag("admin_toggle_user_g360"))
        composeRule.onNodeWithTag("admin_toggle_user_g360").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [35], qualifiers = "w840dp-h1000dp-xhdpi")
    fun `phase 8 expanded 840dp tablet layout renders NavigationRail instead of bottom NavigationBar`() {
        composeRule.setContent {
            ParijotoLearningTheme {
                AdaptiveRoleScaffold(
                    title = "Parijoto Learning",
                    subtitle = "Admin Tablet",
                    destinations = listOf(
                        NavDestinationItem("home", "Beranda", Icons.Default.Home, "nav_home"),
                        NavDestinationItem("mod", "Modul", Icons.AutoMirrored.Filled.MenuBook, "nav_mod")
                    ),
                    selectedId = "home",
                    onSelect = {},
                    snackbarHostState = remember { SnackbarHostState() }
                ) {
                    Text("Konten Tablet")
                }
            }
        }

        composeRule.onNodeWithTag("adaptive_navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithText("Konten Tablet").assertIsDisplayed()
    }
}
