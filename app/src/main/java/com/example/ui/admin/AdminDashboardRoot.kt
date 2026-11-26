package com.example.ui.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Settings
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
import com.example.ui.reports.AdminReportsTab
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminDashboardRoot(
    user: UserAccount,
    systemSetting: SystemSetting,
    onUpdateDisplayName: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val vm: AdminViewModel = viewModel(
        key = "admin_${user.uid}",
        factory = AdminViewModel.factory(context, user.uid)
    )

    val allUsers by vm.allUsers.collectAsStateWithLifecycle()
    val allClasses by vm.allClasses.collectAsStateWithLifecycle()
    val allEnrollments by vm.allEnrollments.collectAsStateWithLifecycle()
    val allSubmissions by vm.allSubmissions.collectAsStateWithLifecycle()
    val allAttempts by vm.allAttempts.collectAsStateWithLifecycle()
    val allGrades by vm.allGrades.collectAsStateWithLifecycle()
    val allReflections by vm.allReflections.collectAsStateWithLifecycle()
    val allAssignments by vm.allAssignments.collectAsStateWithLifecycle()
    val allQuizzes by vm.allQuizzes.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val modules by vm.modules.collectAsStateWithLifecycle()
    val lessons by vm.lessons.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message.orEmpty())
            vm.clearMessage()
        }
    }

    var selectedTab by remember { mutableStateOf("overview") }
    var showGlossary by remember { mutableStateOf(false) }

    val destinations = remember {
        listOf(
            NavDestinationItem("overview", "Statistik", Icons.Default.Analytics, "nav_admin_overview"),
            NavDestinationItem("reports", "Laporan", Icons.Default.Assessment, "nav_admin_reports"),
            NavDestinationItem("users", "Pengguna", Icons.Default.ManageAccounts, "nav_admin_users"),
            NavDestinationItem("curriculum", "Kurikulum", Icons.AutoMirrored.Filled.MenuBook, "nav_admin_curriculum"),
            NavDestinationItem("settings", "Sistem", Icons.Default.Settings, "nav_admin_settings")
        )
    }

    AdaptiveRoleScaffold(
        title = "PARIJOTO LEARNING",
        subtitle = "Administrator • TA ${systemSetting.academicYear} (${systemSetting.semester})",
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
            "overview" -> AdminOverviewTab(
                users = allUsers,
                classes = allClasses,
                enrollments = allEnrollments,
                submissions = allSubmissions,
                attempts = allAttempts,
                grades = allGrades,
                reflections = allReflections,
                onExportCsv = vm::buildAdminGradesCsv
            )

            "reports" -> AdminReportsTab(
                classes = allClasses,
                enrollments = allEnrollments,
                modules = modules,
                lessons = lessons,
                assignments = allAssignments,
                submissions = allSubmissions,
                quizzes = allQuizzes,
                grades = allGrades
            )

            "users" -> AdminUsersTab(
                users = allUsers,
                busy = busy,
                onUpdateUser = vm::updateUserAccount
            )

            "curriculum" -> AdminClassesAndCurriculumTab(
                currentAdmin = user,
                users = allUsers,
                classes = allClasses,
                enrollments = allEnrollments,
                subjects = subjects,
                modules = modules,
                lessons = lessons,
                systemSetting = systemSetting,
                busy = busy,
                onCreateClass = vm::createClassAsAdmin,
                onToggleArchiveClass = vm::toggleArchiveClass,
                onSaveSubject = vm::saveSubject
            )

            "settings" -> AdminSystemSettingsTab(
                user = user,
                setting = systemSetting,
                busy = busy,
                onUpdateName = onUpdateDisplayName,
                onSaveSetting = vm::saveSystemSetting,
                onOpenGlossary = { showGlossary = true },
                onSignOut = onSignOut
            )
        }
    }

    if (showGlossary) {
        GlossaryAndConceptMapSheet(onDismiss = { showGlossary = false })
    }
}
