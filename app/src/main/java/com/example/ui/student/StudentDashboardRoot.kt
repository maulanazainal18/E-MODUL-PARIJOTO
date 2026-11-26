package com.example.ui.student

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
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
import com.example.ui.viewmodel.StudentViewModel

@Composable
fun StudentDashboardRoot(
    user: UserAccount,
    systemSetting: SystemSetting,
    downloadedModules: Set<String>,
    onToggleOfflineModule: (String, Boolean) -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val vm: StudentViewModel = viewModel(
        key = "student_${user.uid}",
        factory = StudentViewModel.factory(context, user.uid)
    )

    val modules by vm.modules.collectAsStateWithLifecycle()
    val lessons by vm.lessons.collectAsStateWithLifecycle()
    val learningProgress by vm.learningProgress.collectAsStateWithLifecycle()
    val enrollments by vm.enrollments.collectAsStateWithLifecycle()
    val myGroups by vm.myGroups.collectAsStateWithLifecycle()
    val openGroups by vm.openGroups.collectAsStateWithLifecycle()
    val assignments by vm.assignments.collectAsStateWithLifecycle()
    val submissions by vm.submissions.collectAsStateWithLifecycle()
    val quizzes by vm.quizzes.collectAsStateWithLifecycle()
    val questions by vm.questions.collectAsStateWithLifecycle()
    val attempts by vm.attempts.collectAsStateWithLifecycle()
    val publishedGrades by vm.publishedGrades.collectAsStateWithLifecycle()
    val reflections by vm.reflections.collectAsStateWithLifecycle()
    val announcements by vm.announcements.collectAsStateWithLifecycle()
    val answerKeysCache by vm.answerKeysCache.collectAsStateWithLifecycle()
    val aiStream by vm.aiTutorStream.collectAsStateWithLifecycle()
    val aiLoading by vm.aiTutorLoading.collectAsStateWithLifecycle()
    val aiEvidence by vm.aiEvidence.collectAsStateWithLifecycle()
    val enrolledClassesMap by vm.enrolledClassesMap.collectAsStateWithLifecycle()
    val classPresentations by vm.classPresentations.collectAsStateWithLifecycle()
    val peerReviews by vm.peerReviews.collectAsStateWithLifecycle()
    val groupDiscussions by vm.groupDiscussions.collectAsStateWithLifecycle()
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
    var selectedClassId by remember { mutableStateOf<String?>(null) }

    val activeEnrollment = enrollments.firstOrNull { it.classId == selectedClassId } ?: enrollments.firstOrNull()

    LaunchedEffect(activeEnrollment?.classId) {
        activeEnrollment?.classId?.let(vm::setActiveClassId)
    }

    val destinations = remember {
        listOf(
            NavDestinationItem("home", "Beranda", Icons.Default.Home, "nav_student_home"),
            NavDestinationItem("modules", "Modul", Icons.AutoMirrored.Filled.MenuBook, "nav_student_modules"),
            NavDestinationItem("pbl", "PBL & LKM", Icons.Default.Groups, "nav_student_pbl"),
            NavDestinationItem("eval", "Evaluasi", Icons.Default.Quiz, "nav_student_eval"),
            NavDestinationItem("profile", "Profil", Icons.Default.Person, "nav_student_profile")
        )
    }

    AdaptiveRoleScaffold(
        title = "PARIJOTO LEARNING",
        subtitle = "Siswa • ${activeEnrollment?.className ?: "Biologi Kelas X Fase E"}",
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
            "home" -> StudentHomeTab(
                user = user,
                systemSetting = systemSetting,
                enrollments = enrollments,
                activeEnrollment = activeEnrollment,
                enrolledClassesMap = enrolledClassesMap,
                modules = modules,
                totalLessons = lessons.size.coerceAtLeast(1),
                assignments = assignments,
                submissions = submissions,
                attempts = attempts,
                publishedGrades = publishedGrades,
                announcements = announcements,
                busy = busy,
                onSelectEnrollment = { selectedClassId = it.classId },
                onJoinClass = { code -> vm.joinClass(code, user.nama) },
                onOpenModules = { selectedTab = "modules" },
                onOpenPbl = { selectedTab = "pbl" },
                onOpenEvaluation = { selectedTab = "eval" },
                onOpenGlossary = { showGlossary = true }
            )

            "modules" -> StudentModulesTab(
                studentUid = user.uid,
                modules = modules,
                lessons = lessons,
                enrollment = activeEnrollment,
                learningProgress = learningProgress,
                downloadedModules = downloadedModules,
                quizzes = quizzes,
                questions = questions,
                attempts = attempts,
                answerKeysCache = answerKeysCache,
                reflections = reflections,
                busy = busy,
                aiStreamText = aiStream,
                aiLoading = aiLoading,
                onToggleOffline = onToggleOfflineModule,
                onMarkLessonComplete = { lessonId, moduleId ->
                    vm.markLessonCompleted(activeEnrollment, lessonId, moduleId, lessons.size.coerceAtLeast(1))
                },
                onToggleBookmark = vm::toggleBookmark,
                onFontScaleChange = vm::setReaderFontScale,
                onRecordReadProgress = vm::recordReadingProgress,
                onLoadAnswerKey = vm::loadAnswerKeyIfUnlocked,
                onAutosaveQuiz = { quiz, attNum, order, mcqCount, remSec, mcqs, essays, showToast ->
                    vm.autosaveQuizAttempt(quiz, attNum, order, mcqCount, remSec, activeEnrollment, user.nama, mcqs, essays, showToast)
                },
                onSubmitQuiz = { quiz, qs, attNum, order, remSec, mcqs, essays ->
                    vm.submitQuizAttempt(quiz, qs, attNum, order, remSec, activeEnrollment, user.nama, mcqs, essays)
                },
                onSubmitReflection = { modId, answers, notes, pblRef ->
                    vm.submitReflection(modId, activeEnrollment, user.nama, answers, notes, pblRef)
                },
                onAskAiTutor = vm::askAiTutor,
                onOpenGlossary = { showGlossary = true }
            )

            "pbl" -> StudentPblTab(
                user = user,
                enrollment = activeEnrollment,
                myGroups = myGroups,
                openGroups = openGroups,
                groupDiscussions = groupDiscussions,
                assignments = assignments,
                submissions = submissions,
                classPresentations = classPresentations,
                peerReviews = peerReviews,
                reflections = reflections,
                aiEvidence = aiEvidence,
                aiLoading = aiLoading,
                busy = busy,
                onCreateGroup = { name, topic -> vm.createGroup(activeEnrollment, name, topic, user.nama) },
                onJoinGroup = { grp -> vm.joinGroup(grp, user.nama) },
                onObserveGroupDiscussion = vm::setActiveDiscussionGroup,
                onUpdateGroupWorkspace = vm::updateGroupWorkspace,
                onUpdateGroupLeaderAndRoles = vm::updateGroupLeaderAndRoles,
                onSendGroupDiscussion = vm::sendGroupDiscussionMessage,
                onSaveSubmission = vm::saveSubmission,
                onSubmitPeerReview = { sub, clarity, evidence, solution, comment ->
                    vm.submitPeerReview(sub, user.nama, clarity, evidence, solution, comment)
                },
                onSubmitPblReflection = { ans, notes, pblRef ->
                    vm.submitReflection("pbl_phase_5", activeEnrollment, user.nama, ans, notes, pblRef)
                },
                onAnalyzeEvidenceAi = vm::analyzeEvidenceStructured
            )

            "eval" -> StudentEvaluationTab(
                studentUid = user.uid,
                quizzes = quizzes,
                questions = questions,
                attempts = attempts,
                answerKeysCache = answerKeysCache,
                publishedGrades = publishedGrades,
                reflections = reflections,
                busy = busy,
                enrollments = enrollments,
                enrolledClassesMap = enrolledClassesMap,
                modules = modules,
                lessons = lessons,
                learningProgress = learningProgress,
                assignments = assignments,
                submissions = submissions,
                onLoadAnswerKey = vm::loadAnswerKeyIfUnlocked,
                onAutosaveQuiz = { quiz, attNum, order, mcqCount, remSec, mcqs, essays, showToast ->
                    vm.autosaveQuizAttempt(quiz, attNum, order, mcqCount, remSec, activeEnrollment, user.nama, mcqs, essays, showToast)
                },
                onSubmitQuiz = { quiz, qs, attNum, order, remSec, mcqs, essays ->
                    vm.submitQuizAttempt(quiz, qs, attNum, order, remSec, activeEnrollment, user.nama, mcqs, essays)
                },
                onSubmitReflection = { modId, answers, notes, pblRef ->
                    vm.submitReflection(modId, activeEnrollment, user.nama, answers, notes, pblRef)
                }
            )

            "profile" -> StudentProfileTab(
                user = user,
                enrollments = enrollments,
                enrolledClassesMap = enrolledClassesMap,
                downloadedModulesCount = downloadedModules.size,
                publishedGrades = publishedGrades,
                busy = busy,
                onUpdateName = onUpdateDisplayName,
                onJoinClass = { code -> vm.joinClass(code, user.nama) },
                onOpenGlossary = { showGlossary = true },
                onSignOut = onSignOut
            )
        }
    }

    if (showGlossary) {
        GlossaryAndConceptMapSheet(onDismiss = { showGlossary = false })
    }
}
