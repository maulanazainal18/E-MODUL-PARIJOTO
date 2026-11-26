package com.example.ui.foundation

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserRole
import com.example.ui.components.ParijotoBotanicalBanner
import com.example.ui.components.ParijotoConfirmDialog
import com.example.ui.components.ParijotoElevatedCard
import com.example.ui.components.ParijotoPasswordField
import com.example.ui.components.ParijotoPrimaryButton
import com.example.ui.components.ParijotoSecondaryButton
import com.example.ui.components.ParijotoTextField
import com.example.ui.components.StatusPill

@Composable
fun FoundationLoginScreen(
    onNavigateToRole: (UserRole, String) -> Unit,
    onBackToOnboarding: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.SISWA) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showPhaseInfoDialog by remember { mutableStateOf(false) }

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ParijotoBotanicalBanner(compact = true)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Masuk ke PARIJOTO LEARNING",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Phase 1: Fondasi UI & Navigasi (Data Dummy)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onBackToOnboarding) {
                        Text("Intro")
                    }
                }

                ParijotoElevatedCard {
                    Text(
                        "Pilih Peran Preview",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedRole == UserRole.SISWA,
                            onClick = { selectedRole = UserRole.SISWA },
                            label = { Text("Siswa") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Siswa") },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("login_role_siswa")
                        )
                        FilterChip(
                            selected = selectedRole == UserRole.GURU,
                            onClick = { selectedRole = UserRole.GURU },
                            label = { Text("Guru") },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = "Guru") },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("login_role_guru")
                        )
                        FilterChip(
                            selected = selectedRole == UserRole.ADMIN,
                            onClick = { selectedRole = UserRole.ADMIN },
                            label = { Text("Admin") },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("login_role_admin")
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    ParijotoTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
                        label = "Email Sekolah / Google",
                        placeholder = "nama@sekolah.sch.id",
                        leadingIcon = Icons.Default.Email,
                        errorText = emailError,
                        testTag = "login_email_input"
                    )
                    Spacer(Modifier.height(10.dp))
                    ParijotoPasswordField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        label = "Kata Sandi",
                        leadingIcon = Icons.Default.Lock,
                        errorText = passwordError,
                        testTag = "login_password_input"
                    )
                    Spacer(Modifier.height(14.dp))
                    ParijotoPrimaryButton(
                        text = "Masuk sebagai ${selectedRole.label}",
                        icon = Icons.AutoMirrored.Filled.Login,
                        onClick = {
                            val cleanEmail = email.trim()
                            var valid = true
                            if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
                                emailError = "Masukkan format email yang valid"
                                valid = false
                            }
                            if (password.length < 4) {
                                passwordError = "Minimal 4 karakter untuk uji form Phase 1"
                                valid = false
                            }
                            if (valid) {
                                val displayName = cleanEmail.substringBefore("@")
                                    .replaceFirstChar { it.uppercase() }
                                onNavigateToRole(selectedRole, displayName)
                            }
                        },
                        testTag = "login_submit_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ParijotoElevatedCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = "Akses Cepat Uji Navigasi Peran",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        TextButton(onClick = { showPhaseInfoDialog = true }) {
                            Text("Info Phase 1")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ParijotoSecondaryButton(
                            text = "Siswa",
                            onClick = { onNavigateToRole(UserRole.SISWA, "Nadia (Siswa X-1)") },
                            testTag = "quick_siswa_button",
                            modifier = Modifier.weight(1f)
                        )
                        ParijotoSecondaryButton(
                            text = "Guru",
                            onClick = { onNavigateToRole(UserRole.GURU, "Ibu Sari, S.Pd.") },
                            testTag = "quick_guru_button",
                            modifier = Modifier.weight(1f)
                        )
                        ParijotoSecondaryButton(
                            text = "Admin",
                            onClick = { onNavigateToRole(UserRole.ADMIN, "Admin Muria") },
                            testTag = "quick_admin_button",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showPhaseInfoDialog) {
        ParijotoConfirmDialog(
            title = "Mode Fondasi Phase 1",
            message = "Tahap ini memvalidasi struktur modular, tema PARIJOTO LEARNING, komponen M3, responsivitas layar, dan navigasi antar-halaman menggunakan data dummy tanpa koneksi database.",
            confirmText = "Mengerti",
            dismissText = "Tutup",
            onConfirm = { showPhaseInfoDialog = false },
            onDismiss = { showPhaseInfoDialog = false }
        )
    }
}
