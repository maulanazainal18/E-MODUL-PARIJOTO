package com.example.ui.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.dummy.Phase1DummyData
import com.example.ui.components.AdaptiveRoleScaffold
import com.example.ui.components.GlossaryAndConceptMapSheet
import com.example.ui.components.MetricStatCard
import com.example.ui.components.NavDestinationItem
import com.example.ui.components.ParijotoBotanicalBanner
import com.example.ui.components.ParijotoConfirmDialog
import com.example.ui.components.ParijotoElevatedCard
import com.example.ui.components.ParijotoPrimaryButton
import com.example.ui.components.ParijotoSecondaryButton
import com.example.ui.components.ParijotoTextField
import com.example.ui.components.StatusPill
import kotlinx.coroutines.launch

@Composable
fun AdminTempDashboardScreen(
    displayName: String,
    onLogoutToLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf("overview") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showGlossary by remember { mutableStateOf(false) }
    var schoolNameDraft by remember { mutableStateOf("SMA/MA Kelas X - Lereng Muria") }

    val destinations = remember {
        listOf(
            NavDestinationItem("overview", "Statistik", Icons.Default.Analytics, "nav_p1_admin_overview"),
            NavDestinationItem("manage", "Kelola", Icons.Default.Settings, "nav_p1_admin_manage"),
            NavDestinationItem("account", "Akun", Icons.Default.Person, "nav_p1_admin_account")
        )
    }

    AdaptiveRoleScaffold(
        title = "PARIJOTO LEARNING",
        subtitle = "Admin (Dashboard Sementara Phase 1) • $displayName",
        destinations = destinations,
        selectedId = selectedTab,
        onSelect = { selectedTab = it },
        snackbarHostState = snackbarHostState,
        topActions = {
            IconButton(onClick = { showGlossary = true }) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Glosarium")
            }
        }
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                "overview" -> {
                    item { ParijotoBotanicalBanner(compact = true) }
                    item {
                        ParijotoElevatedCard(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ) {
                            StatusPill(
                                text = "Data Dummy • Phase 1 Preview",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Dashboard Administrator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Statistik simulasi untuk verifikasi tata letak responsif & navigasi.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricStatCard(Phase1DummyData.adminStats[0].label, Phase1DummyData.adminStats[0].value, Icons.Default.School, modifier = Modifier.weight(1f))
                            MetricStatCard(Phase1DummyData.adminStats[1].label, Phase1DummyData.adminStats[1].value, Icons.Default.Group, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricStatCard(Phase1DummyData.adminStats[2].label, Phase1DummyData.adminStats[2].value, Icons.Default.Class, modifier = Modifier.weight(1f))
                            MetricStatCard(Phase1DummyData.adminStats[3].label, Phase1DummyData.adminStats[3].value, Icons.Default.Dashboard, accentColor = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                        }
                    }
                    items(Phase1DummyData.adminRecentActivities) { act ->
                        ParijotoElevatedCard {
                            Text(act, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                "manage" -> {
                    item {
                        ParijotoElevatedCard {
                            Text("Preview Form Pengaturan Tahun Ajaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            ParijotoTextField(
                                value = schoolNameDraft,
                                onValueChange = { schoolNameDraft = it },
                                label = "Nama Satuan Pendidikan",
                                testTag = "admin_p1_school_input"
                            )
                            Spacer(Modifier.height(10.dp))
                            ParijotoPrimaryButton(
                                text = "Simulasikan Simpan",
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Preview form tersimpan sementara (belum ada database).")
                                    }
                                },
                                testTag = "admin_p1_save_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                else -> {
                    item {
                        ParijotoElevatedCard {
                            Text("Akun Admin Sementara", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Pengguna: $displayName • Peran: ADMIN", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            ParijotoSecondaryButton(
                                text = "Keluar & Pilih Peran Lain",
                                icon = Icons.AutoMirrored.Filled.Logout,
                                onClick = { showLogoutDialog = true },
                                testTag = "admin_p1_logout_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        ParijotoConfirmDialog(
            title = "Kembali ke Halaman Login?",
            message = "Anda akan keluar dari dashboard sementara Admin untuk menguji halaman atau peran lain.",
            confirmText = "Keluar",
            onConfirm = {
                showLogoutDialog = false
                onLogoutToLogin()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showGlossary) {
        GlossaryAndConceptMapSheet(onDismiss = { showGlossary = false })
    }
}

@Composable
fun GuruTempDashboardScreen(
    displayName: String,
    onLogoutToLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf("home") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showGlossary by remember { mutableStateOf(false) }
    var classNameDraft by remember { mutableStateOf("") }

    val destinations = remember {
        listOf(
            NavDestinationItem("home", "Beranda", Icons.Default.Dashboard, "nav_p1_guru_home"),
            NavDestinationItem("classes", "Kelas", Icons.Default.Class, "nav_p1_guru_classes"),
            NavDestinationItem("profile", "Profil", Icons.Default.Person, "nav_p1_guru_profile")
        )
    }

    AdaptiveRoleScaffold(
        title = "PARIJOTO LEARNING",
        subtitle = "Guru (Dashboard Sementara Phase 1) • $displayName",
        destinations = destinations,
        selectedId = selectedTab,
        onSelect = { selectedTab = it },
        snackbarHostState = snackbarHostState,
        topActions = {
            IconButton(onClick = { showGlossary = true }) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Glosarium")
            }
        }
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                "home" -> {
                    item { ParijotoBotanicalBanner(compact = true) }
                    item {
                        ParijotoElevatedCard(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ) {
                            StatusPill(
                                text = "Data Dummy • Phase 1 Preview",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Selamat Datang, $displayName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Kelola kelas Biologi Kelas X Fase E, materi Etnosains Parijoto, dan penilaian PBL.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item {
                        Text("Antrean Tugas & Evaluasi (Dummy)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    items(Phase1DummyData.guruPendingQueue, key = { it.id }) { task ->
                        ParijotoElevatedCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(task.pblPhase, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusPill(task.status, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }

                "classes" -> {
                    item {
                        ParijotoElevatedCard {
                            Text("Preview Form Buat Kelas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            ParijotoTextField(
                                value = classNameDraft,
                                onValueChange = { classNameDraft = it },
                                label = "Nama Kelas Baru",
                                placeholder = "Contoh: X IPA 3",
                                testTag = "guru_p1_class_input"
                            )
                            Spacer(Modifier.height(10.dp))
                            ParijotoPrimaryButton(
                                text = "Simulasikan Buat Kelas",
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Kelas preview '$classNameDraft' siap (mode dummy Phase 1).")
                                    }
                                    classNameDraft = ""
                                },
                                enabled = classNameDraft.isNotBlank(),
                                testTag = "guru_p1_create_class_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    items(Phase1DummyData.guruClasses, key = { it.id }) { cls ->
                        ParijotoElevatedCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cls.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${cls.studentCount} siswa • ${cls.activeModule}", style = MaterialTheme.typography.bodySmall)
                                }
                                StatusPill("Kode: ${cls.code}", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }

                else -> {
                    item {
                        ParijotoElevatedCard {
                            Text("Profil Guru (Phase 1)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Nama: $displayName • Mapel: Biologi Fase E", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            ParijotoSecondaryButton(
                                text = "Keluar ke Login",
                                icon = Icons.AutoMirrored.Filled.Logout,
                                onClick = { showLogoutDialog = true },
                                testTag = "guru_p1_logout_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        ParijotoConfirmDialog(
            title = "Keluar dari Dashboard Guru?",
            message = "Anda akan diarahkan kembali ke halaman Login untuk mencoba peran lain.",
            confirmText = "Keluar",
            onConfirm = {
                showLogoutDialog = false
                onLogoutToLogin()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showGlossary) {
        GlossaryAndConceptMapSheet(onDismiss = { showGlossary = false })
    }
}

@Composable
fun SiswaTempDashboardScreen(
    displayName: String,
    onLogoutToLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf("home") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showGlossary by remember { mutableStateOf(false) }
    var joinCodeDraft by remember { mutableStateOf("") }

    val destinations = remember {
        listOf(
            NavDestinationItem("home", "Beranda", Icons.Default.Home, "nav_p1_siswa_home"),
            NavDestinationItem("modules", "Modul", Icons.AutoMirrored.Filled.MenuBook, "nav_p1_siswa_modules"),
            NavDestinationItem("tasks", "PBL", Icons.AutoMirrored.Filled.Assignment, "nav_p1_siswa_tasks"),
            NavDestinationItem("profile", "Profil", Icons.Default.Person, "nav_p1_siswa_profile")
        )
    }

    AdaptiveRoleScaffold(
        title = "PARIJOTO LEARNING",
        subtitle = "Siswa (Dashboard Sementara Phase 1) • $displayName",
        destinations = destinations,
        selectedId = selectedTab,
        onSelect = { selectedTab = it },
        snackbarHostState = snackbarHostState,
        topActions = {
            IconButton(onClick = { showGlossary = true }) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Glosarium")
            }
        }
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                "home" -> {
                    item { ParijotoBotanicalBanner(compact = true) }
                    item {
                        ParijotoElevatedCard(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ) {
                            StatusPill(
                                text = "Data Dummy • Phase 1 Preview",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Halo, $displayName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Kelas X IPA 1 • Eksplorasi Keanekaragaman Hayati: Etnosains Parijoto.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item {
                        ParijotoElevatedCard {
                            Text("Preview Form Kode Kelas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            ParijotoTextField(
                                value = joinCodeDraft,
                                onValueChange = { joinCodeDraft = it.uppercase() },
                                label = "Kode Kelas",
                                placeholder = "PRJ26A",
                                testTag = "siswa_p1_join_code_input"
                            )
                            Spacer(Modifier.height(10.dp))
                            ParijotoPrimaryButton(
                                text = "Simulasikan Gabung Kelas",
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Simulasi gabung kelas '$joinCodeDraft' berhasil (Phase 1).")
                                    }
                                    joinCodeDraft = ""
                                },
                                enabled = joinCodeDraft.isNotBlank(),
                                testTag = "siswa_p1_join_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                "modules" -> {
                    items(Phase1DummyData.siswaModules, key = { it.id }) { mod ->
                        ParijotoElevatedCard {
                            StatusPill(mod.meeting, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(6.dp))
                            Text(mod.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${mod.topicsCount} topik pembelajaran • Progres ${mod.progressPercent}%", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { mod.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            )
                        }
                    }
                }

                "tasks" -> {
                    items(Phase1DummyData.siswaTasks, key = { it.id }) { task ->
                        ParijotoElevatedCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(task.pblPhase, style = MaterialTheme.typography.bodySmall)
                                }
                                StatusPill(task.status, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }

                else -> {
                    item {
                        ParijotoElevatedCard {
                            Text("Profil Siswa (Phase 1)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Nama: $displayName • Kelas X Fase E", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            ParijotoSecondaryButton(
                                text = "Keluar ke Login",
                                icon = Icons.AutoMirrored.Filled.Logout,
                                onClick = { showLogoutDialog = true },
                                testTag = "siswa_p1_logout_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        ParijotoConfirmDialog(
            title = "Keluar dari Dashboard Siswa?",
            message = "Kembali ke halaman Login untuk menguji alur navigasi atau peran lainnya.",
            confirmText = "Keluar",
            onConfirm = {
                showLogoutDialog = false
                onLogoutToLogin()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showGlossary) {
        GlossaryAndConceptMapSheet(onDismiss = { showGlossary = false })
    }
}
