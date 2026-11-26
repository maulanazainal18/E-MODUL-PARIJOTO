package com.example.ui.teacher

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.SystemSetting
import com.example.data.model.UserAccount
import com.example.ui.components.AdaptiveRoleScaffold
import com.example.ui.components.GlossaryAndConceptMapSheet
import com.example.ui.components.NavDestinationItem
import com.example.ui.viewmodel.TeacherViewModel

@Composable
fun TeacherDashboardRoot(
    user: UserAccount,
    systemSetting: SystemSetting,
    onUpdateDisplayName: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val vm: TeacherViewModel = viewModel(
        key = "teacher_${user.uid}",
        factory = TeacherViewModel.factory(context, user.uid)
    )

    val classes by vm.classes.collectAsStateWithLifecycle()
    val enrollments by vm.enrollments.collectAsStateWithLifecycle()
    val modules by vm.modules.collectAsStateWithLifecycle()
    val lessons by vm.lessons.collectAsStateWithLifecycle()
    val groups by vm.groups.collectAsStateWithLifecycle()
    val assignments by vm.assignments.collectAsStateWithLifecycle()
    val submissions by vm.submissions.collectAsStateWithLifecycle()
    val quizzes by vm.quizzes.collectAsStateWithLifecycle()
    val questions by vm.questions.collectAsStateWithLifecycle()
    val attempts by vm.attempts.collectAsStateWithLifecycle()
    val grades by vm.grades.collectAsStateWithLifecycle()
    val rubrics by vm.rubrics.collectAsStateWithLifecycle()
    val reflections by vm.reflections.collectAsStateWithLifecycle()
    val groupDiscussions by vm.groupDiscussions.collectAsStateWithLifecycle()
    val answerKeys by vm.answerKeys.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message.orEmpty())
            vm.clearMessage()
        }
    }

    var selectedTab by remember { mutableStateOf("home") }
    var showGlossary by remember { mutableStateOf(false) }

    val destinations = remember {
        listOf(
            NavDestinationItem("home", "Beranda", Icons.Default.Dashboard, "nav_teacher_home"),
            NavDestinationItem("classes", "Kelas", Icons.Default.Class, "nav_teacher_classes"),
            NavDestinationItem("curriculum", "Materi", Icons.AutoMirrored.Filled.MenuBook, "nav_teacher_curriculum"),
            NavDestinationItem("grading", "Nilai", Icons.Default.Grade, "nav_teacher_grading"),
            NavDestinationItem("profile", "Profil", Icons.Default.Person, "nav_teacher_profile")
        )
    }

    AdaptiveRoleScaffold(
        title = "PARIJOTO LEARNING",
        subtitle = "Guru • TA ${systemSetting.academicYear} (${systemSetting.semester})",
        destinations = destinations,
        selectedId = selectedTab,
        onSelect = { selectedTab = it },
        snackbarHostState = snackbarHostState,
        topActions = {
            IconButton(onClick = { showGlossary = true }) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Glosarium & Peta Konsep")
            }
        }
    ) { _ ->
        when (selectedTab) {
            "home" -> TeacherHomeTab(
                user = user,
                classes = classes,
                enrollments = enrollments,
                submissions = submissions,
                attempts = attempts,
                grades = grades,
                busy = busy,
                onPostAnnouncement = { classId, title, msg, pinned ->
                    vm.publishAnnouncement(classId, user.nama, title, msg, pinned)
                },
                onExportCsv = vm::buildGradesCsv
            )

            "classes" -> TeacherClassesTab(
                user = user,
                systemSetting = systemSetting,
                classes = classes,
                enrollments = enrollments,
                busy = busy,
                onCreateClass = vm::createClass,
                onToggleArchive = vm::toggleArchiveClass,
                onToggleStudentStatus = vm::toggleEnrollmentStatus,
                onRemoveStudent = vm::removeStudent
            )

            "curriculum" -> TeacherCurriculumTab(
                classes = classes,
                enrollments = enrollments,
                modules = modules,
                lessons = lessons,
                groups = groups,
                groupDiscussions = groupDiscussions,
                assignments = assignments,
                quizzes = quizzes,
                questions = questions,
                answerKeys = answerKeys,
                busy = busy,
                onSaveModule = vm::saveModule,
                onSaveLesson = vm::saveLesson,
                onDeleteLesson = vm::deleteLesson,
                onCreateGroup = vm::createGroup,
                onObserveGroupDiscussion = vm::setActiveDiscussionGroup,
                onAssignGroupMembers = { grp, leaderUid, memberUids, memberNames, memberRoles ->
                    vm.assignMembersToGroup(grp.id, leaderUid, memberUids, memberNames, memberRoles)
                },
                onSaveGroupComment = vm::saveGroupTeacherComment,
                onSendGroupDiscussion = { grp, phase, text ->
                    vm.sendGroupDiscussionMessage(grp, user.nama, phase, text)
                },
                onCreateAssignment = vm::createAssignment,
                onCreateQuestion = vm::createQuestion,
                onToggleQuizEvaluation = { quiz, closed, showDiscussion ->
                    vm.toggleQuizEvaluation(quiz, closed, showDiscussion)
                },
                onEnsureAnswerKeys = { vm.ensureAllAnswerKeysLoaded(quizzes) },
                onSaveQuizConfig = vm::saveQuizConfig,
                onSaveBankQuestion = vm::saveBankQuestion,
                onDeleteBankQuestion = vm::deleteBankQuestion
            )

            "grading" -> TeacherGradingTab(
                enrollments = enrollments,
                assignments = assignments,
                submissions = submissions,
                quizzes = quizzes,
                questions = questions,
                answerKeys = answerKeys,
                attempts = attempts,
                grades = grades,
                rubrics = rubrics,
                reflections = reflections,
                busy = busy,
                classes = classes,
                modules = modules,
                lessons = lessons,
                onEnsureAnswerKeys = { vm.ensureAllAnswerKeysLoaded(quizzes) },
                onGradeQuizAttempt = vm::gradeQuizAttempt,
                onGradeSubmission = vm::gradeSubmissionSkill,
                onReturnForRevision = vm::returnSubmissionForRevision,
                onGradeAttitude = vm::gradeStudentAttitude,
                onExportCsv = { vm.buildGradesCsv(null) }
            )

            "profile" -> TeacherProfileTab(
                user = user,
                classCount = classes.size,
                studentCount = enrollments.count { it.status == "ACTIVE" },
                busy = busy,
                onUpdateName = onUpdateDisplayName,
                onOpenGlossary = { showGlossary = true },
                onSignOut = onSignOut
            )
        }
    }

    if (showGlossary) {
        GlossaryAndConceptMapSheet(onDismiss = { showGlossary = false })
    }
}
