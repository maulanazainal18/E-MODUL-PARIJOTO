package com.example.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.auth.AuthRepository
import com.example.data.auth.SignInOutcome
import com.example.data.model.UserRole
import com.example.ui.components.ParijotoBotanicalBanner
import com.example.ui.components.StatusPill
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    credentialManager: CredentialManager,
    onOpenOnboarding: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var statusError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        AuthRepository.attemptAutoSignIn(context, credentialManager)
    }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ParijotoBotanicalBanner()
                Spacer(Modifier.height(20.dp))

                StatusPill(
                    text = "Kurikulum Merdeka • SMA/MA Kelas X Fase E",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "PARIJOTO LEARNING",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Digital Biology Learning Platform",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Eco, contentDescription = "Botani", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Keanekaragaman Hayati: Eksplorasi Etnosains Parijoto (Medinilla speciosa)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Sumber Modul", tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Berbasis E-Modul karya Ahzami, S.Pd., Gr. (2026), UNY • Model Problem Based Learning (4 JP / 2 Pertemuan)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = "Keamanan", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Autentikasi terproteksi Google Sign-In, Cloud Firestore, dan App Check untuk Admin, Guru, dan Siswa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                if (statusError != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = statusError.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            statusError = null
                            when (val outcome = AuthRepository.signInWithGoogleInteractive(context, credentialManager)) {
                                SignInOutcome.Success -> Unit
                                SignInOutcome.Cancelled -> {
                                    statusError = "Login Google dibatalkan. Ketuk tombol di bawah untuk mencoba kembali."
                                }
                                is SignInOutcome.Error -> {
                                    statusError = outcome.message
                                }
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Menghubungkan Akun Google...")
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = "Masuk")
                        Spacer(Modifier.width(10.dp))
                        Text("Masuk dengan Google", fontWeight = FontWeight.SemiBold)
                    }
                }

                if (onOpenOnboarding != null) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenOnboarding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("login_revisit_onboarding_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Lihat Pengenalan Aplikasi")
                    }
                }
            }
        }
    }
}

@Composable
fun RoleOnboardingScreen(
    defaultName: String,
    email: String,
    isBootstrapAdmin: Boolean,
    busy: Boolean,
    onSubmitProfile: (String, UserRole) -> Unit,
    onSignOut: () -> Unit
) {
    var nama by remember { mutableStateOf(defaultName) }
    var selectedRole by remember { mutableStateOf(if (isBootstrapAdmin) UserRole.ADMIN else UserRole.SISWA) }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Lengkapi Profil Akun",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Terhubung sebagai $email",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input")
                )

                if (!isBootstrapAdmin) {
                    Text(
                        "Pilih Peran Anda dalam Pembelajaran:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedRole == UserRole.SISWA,
                            onClick = { selectedRole = UserRole.SISWA },
                            label = { Text("Siswa Kelas X") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Siswa") },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("role_siswa_chip")
                        )
                        FilterChip(
                            selected = selectedRole == UserRole.GURU,
                            onClick = { selectedRole = UserRole.GURU },
                            label = { Text("Guru Biologi") },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = "Guru") },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("role_guru_chip")
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "Email terverifikasi sebagai Administrator Utama PARIJOTO LEARNING.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = { onSubmitProfile(nama, selectedRole) },
                    enabled = !busy && nama.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("complete_onboarding_button")
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Simpan & Masuk Platform")
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboarding_signout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar")
                    Spacer(Modifier.width(8.dp))
                    Text("Gunakan Akun Google Lain")
                }
            }
        }
    }
}

@Composable
fun SuspendedAccountScreen(
    userName: String,
    onSignOut: () -> Unit
) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.widthIn(max = 460.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Akun Dinonaktifkan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Halo $userName, status akun Anda saat ini sedang dinonaktifkan oleh Administrator. Hubungi guru atau admin sekolah untuk mengaktifkan kembali.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("suspended_signout_button")
                    ) {
                        Text("Keluar dari Akun")
                    }
                }
            }
        }
    }
}

@Composable
fun MaintenanceModeScreen(
    userName: String,
    schoolName: String,
    onSignOut: () -> Unit
) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .testTag("maintenance_mode_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Pemeliharaan Sistem",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        "Sistem Sedang Pemeliharaan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Halo $userName, platform PARIJOTO LEARNING ($schoolName) sedang dalam mode pemeliharaan oleh Administrator. Akses Guru dan Siswa dibuka kembali setelah pemeliharaan selesai.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("maintenance_signout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar")
                        Spacer(Modifier.width(8.dp))
                        Text("Keluar dari Akun")
                    }
                }
            }
        }
    }
}

@Composable
fun RegistrationClosedScreen(
    email: String,
    schoolName: String,
    onSignOut: () -> Unit
) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .testTag("registration_closed_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LockPerson,
                        contentDescription = "Pendaftaran Ditutup",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        "Pendaftaran Akun Baru Ditutup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Email $email belum terdaftar di $schoolName dan pendaftaran akun baru saat ini dinonaktifkan oleh Administrator. Hubungi guru atau admin sekolah untuk pembukaan akses.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("registration_closed_signout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar")
                        Spacer(Modifier.width(8.dp))
                        Text("Gunakan Akun Terdaftar Lain")
                    }
                }
            }
        }
    }
}
