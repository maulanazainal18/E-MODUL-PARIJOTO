package com.example.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.auth.AuthRepository
import com.example.data.model.AccountStatus
import com.example.data.model.UiState
import com.example.data.model.UserRole
import com.example.ui.admin.AdminDashboardRoot
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.MaintenanceModeScreen
import com.example.ui.auth.RegistrationClosedScreen
import com.example.ui.auth.RoleOnboardingScreen
import com.example.ui.auth.SuspendedAccountScreen
import com.example.ui.foundation.AdminTempDashboardScreen
import com.example.ui.foundation.FoundationLoginScreen
import com.example.ui.foundation.GuruTempDashboardScreen
import com.example.ui.foundation.OnboardingScreen
import com.example.ui.foundation.SiswaTempDashboardScreen
import com.example.ui.foundation.SplashScreen
import com.example.ui.student.StudentDashboardRoot
import com.example.ui.teacher.TeacherDashboardRoot
import com.example.ui.viewmodel.AppSessionViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

object ParijotoRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val AUTHENTICATED = "authenticated_session"

    // Retained Phase 1 preview routes for design/layout inspection in tests
    const val ADMIN_DASHBOARD = "admin_dashboard/{name}"
    const val GURU_DASHBOARD = "guru_dashboard/{name}"
    const val SISWA_DASHBOARD = "siswa_dashboard/{name}"

    fun adminDashboard(name: String): String = "admin_dashboard/${Uri.encode(name)}"
    fun guruDashboard(name: String): String = "guru_dashboard/${Uri.encode(name)}"
    fun siswaDashboard(name: String): String = "siswa_dashboard/${Uri.encode(name)}"
}

/**
 * Phase 2 Production Navigation Graph:
 * Integrates Phase 1 Splash + Onboarding + Navigation Compose with real Firebase Auth,
 * Credential Manager Google Sign-In, Cloud Firestore user role validation, and App Check.
 */
@Composable
fun ParijotoPhase2NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val authFlow = remember { AuthRepository.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentUser?.uid, currentRoute) {
        if (currentUser != null && (currentRoute == ParijotoRoutes.LOGIN || currentRoute == ParijotoRoutes.ONBOARDING)) {
            navController.navigate(ParijotoRoutes.AUTHENTICATED) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else if (currentUser == null && currentRoute == ParijotoRoutes.AUTHENTICATED) {
            navController.navigate(ParijotoRoutes.LOGIN) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = ParijotoRoutes.SPLASH
    ) {
        composable(ParijotoRoutes.SPLASH) {
            SplashScreen(
                onFinished = {
                    val target = if (currentUser != null) {
                        ParijotoRoutes.AUTHENTICATED
                    } else {
                        ParijotoRoutes.ONBOARDING
                    }
                    navController.navigate(target) {
                        popUpTo(ParijotoRoutes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(ParijotoRoutes.ONBOARDING) {
            OnboardingScreen(
                onFinishOnboarding = {
                    val target = if (currentUser != null) {
                        ParijotoRoutes.AUTHENTICATED
                    } else {
                        ParijotoRoutes.LOGIN
                    }
                    navController.navigate(target) {
                        popUpTo(ParijotoRoutes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(ParijotoRoutes.LOGIN) {
            LoginScreen(
                credentialManager = credentialManager,
                onOpenOnboarding = {
                    navController.navigate(ParijotoRoutes.ONBOARDING) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(ParijotoRoutes.AUTHENTICATED) {
            val firebaseUser = currentUser
            if (firebaseUser == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                key(firebaseUser.uid) {
                    AuthenticatedSessionShell(
                        firebaseUser = firebaseUser,
                        credentialManager = credentialManager
                    )
                }
            }
        }
    }
}

@Composable
fun AuthenticatedSessionShell(
    firebaseUser: FirebaseUser,
    credentialManager: CredentialManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionVm: AppSessionViewModel = viewModel(
        key = "session_${firebaseUser.uid}",
        factory = AppSessionViewModel.factory(context, firebaseUser)
    )

    val userState by sessionVm.userState.collectAsStateWithLifecycle()
    val systemSetting by sessionVm.systemSetting.collectAsStateWithLifecycle()
    val downloadedModules by sessionVm.downloadedModules.collectAsStateWithLifecycle()
    val busy by sessionVm.busy.collectAsStateWithLifecycle()

    val performSignOut: () -> Unit = {
        scope.launch {
            AuthRepository.signOut(credentialManager)
        }
    }

    when (val state = userState) {
        UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Memuat profil & memvalidasi peran...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = performSignOut) {
                        Text("Keluar & Muat Ulang")
                    }
                }
            }
        }

        is UiState.Success -> {
            val account = state.data
            if (account == null) {
                if (!systemSetting.allowRegistration && !sessionVm.isBootstrapAdminEmail) {
                    RegistrationClosedScreen(
                        email = firebaseUser.email.orEmpty(),
                        schoolName = systemSetting.schoolName,
                        onSignOut = performSignOut
                    )
                } else {
                    RoleOnboardingScreen(
                        defaultName = firebaseUser.displayName.orEmpty(),
                        email = firebaseUser.email.orEmpty(),
                        isBootstrapAdmin = sessionVm.isBootstrapAdminEmail,
                        busy = busy,
                        onSubmitProfile = sessionVm::provisionProfile,
                        onSignOut = performSignOut
                    )
                }
            } else if (account.statusEnum() == AccountStatus.SUSPENDED) {
                SuspendedAccountScreen(
                    userName = account.nama,
                    onSignOut = performSignOut
                )
            } else if (systemSetting.maintenanceMode && account.roleEnum() != UserRole.ADMIN) {
                MaintenanceModeScreen(
                    userName = account.nama,
                    schoolName = systemSetting.schoolName,
                    onSignOut = performSignOut
                )
            } else {
                when (account.roleEnum()) {
                    UserRole.ADMIN -> AdminDashboardRoot(
                        user = account,
                        systemSetting = systemSetting,
                        onUpdateDisplayName = sessionVm::updateDisplayName,
                        onSignOut = performSignOut
                    )

                    UserRole.GURU -> TeacherDashboardRoot(
                        user = account,
                        systemSetting = systemSetting,
                        onUpdateDisplayName = sessionVm::updateDisplayName,
                        onSignOut = performSignOut
                    )

                    UserRole.SISWA -> StudentDashboardRoot(
                        user = account,
                        systemSetting = systemSetting,
                        downloadedModules = downloadedModules,
                        onToggleOfflineModule = sessionVm::toggleOfflineModule,
                        onUpdateDisplayName = sessionVm::updateDisplayName,
                        onSignOut = performSignOut
                    )
                }
            }
        }
    }
}

/**
 * Retained Phase 1 preview navigation graph for isolated UI layout testing.
 */
@Composable
fun ParijotoNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = ParijotoRoutes.SPLASH
    ) {
        composable(ParijotoRoutes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(ParijotoRoutes.ONBOARDING) {
                        popUpTo(ParijotoRoutes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(ParijotoRoutes.ONBOARDING) {
            OnboardingScreen(
                onFinishOnboarding = {
                    navController.navigate(ParijotoRoutes.LOGIN) {
                        popUpTo(ParijotoRoutes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(ParijotoRoutes.LOGIN) {
            FoundationLoginScreen(
                onNavigateToRole = { role, displayName ->
                    val targetRoute = when (role) {
                        UserRole.ADMIN -> ParijotoRoutes.adminDashboard(displayName)
                        UserRole.GURU -> ParijotoRoutes.guruDashboard(displayName)
                        UserRole.SISWA -> ParijotoRoutes.siswaDashboard(displayName)
                    }
                    navController.navigate(targetRoute) {
                        popUpTo(ParijotoRoutes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBackToOnboarding = {
                    navController.navigate(ParijotoRoutes.ONBOARDING) { launchSingleTop = true }
                }
            )
        }
        composable(
            route = ParijotoRoutes.ADMIN_DASHBOARD,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            AdminTempDashboardScreen(
                displayName = entry.arguments?.getString("name") ?: "Admin",
                onLogoutToLogin = {
                    navController.navigate(ParijotoRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = ParijotoRoutes.GURU_DASHBOARD,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            GuruTempDashboardScreen(
                displayName = entry.arguments?.getString("name") ?: "Guru",
                onLogoutToLogin = {
                    navController.navigate(ParijotoRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = ParijotoRoutes.SISWA_DASHBOARD,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            SiswaTempDashboardScreen(
                displayName = entry.arguments?.getString("name") ?: "Siswa",
                onLogoutToLogin = {
                    navController.navigate(ParijotoRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
