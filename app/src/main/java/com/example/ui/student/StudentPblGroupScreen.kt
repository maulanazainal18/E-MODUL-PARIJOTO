package com.example.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GroupDiscussionMessage
import com.example.data.model.PblGroup
import com.example.data.model.UserAccount
import com.example.ui.components.StatusPill

private val defaultMemberRoles = listOf(
    "Ketua Kelompok",
    "Pencari Literatur",
    "Analis Data",
    "Perancang Solusi & CER",
    "Presenter"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupWorkspaceScreen(
    user: UserAccount,
    group: PblGroup,
    discussions: List<GroupDiscussionMessage>,
    busy: Boolean,
    onBack: () -> Unit,
    onObserveDiscussions: (String?, String?) -> Unit,
    onSaveWorkspace: (String, String?, Int, String?, String?, String?, String?, String?, String?, Map<String, String>?) -> Unit,
    onUpdateLeaderAndRoles: (String, String, Map<String, String>, Boolean) -> Unit,
    onSendDiscussionMessage: (PblGroup, String, Int, String) -> Unit
) {
    DisposableEffect(group.id, group.classId) {
        onObserveDiscussions(group.id, group.classId)
        onDispose { onObserveDiscussions(null, null) }
    }

    var activeSection by remember(group.id) { mutableIntStateOf(0) }
    var phase by remember(group.id) { mutableIntStateOf(group.pblPhase) }
    var topic by remember(group.id) { mutableStateOf(group.topic.orEmpty()) }
    var rootProblem by remember(group.id) { mutableStateOf(group.rootProblem.orEmpty()) }
    var hypothesis by remember(group.id) { mutableStateOf(group.hypothesis.orEmpty()) }
    var investigation by remember(group.id) { mutableStateOf(group.investigationNotes.orEmpty()) }
    var conservation by remember(group.id) { mutableStateOf(group.conservationStrategy.orEmpty()) }
    var campaign by remember(group.id) { mutableStateOf(group.campaignCaption.orEmpty()) }
    var discussionSummary by remember(group.id) { mutableStateOf(group.discussionSummary.orEmpty()) }

    val roleMap = remember(group.id, group.memberUids, group.memberRoles) {
        mutableStateMapOf<String, String>().apply {
            group.memberUids.forEach { uid ->
                put(uid, group.memberRoles[uid] ?: if (uid == group.leaderUid) "Ketua Kelompok" else "Anggota")
            }
        }
    }
    var selectedLeaderUid by remember(group.id, group.leaderUid) { mutableStateOf(group.leaderUid) }
    var openToJoin by remember(group.id, group.openToJoin) { mutableStateOf(group.openToJoin) }
    var chatInput by remember { mutableStateOf("") }
    var chatPhase by remember(group.pblPhase) { mutableIntStateOf(group.pblPhase) }

    val isLeader = group.leaderUid == user.uid

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("group_workspace_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("group_workspace_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Ketua: ${resolveMemberName(group, group.leaderUid)} • ${group.memberUids.size} anggota",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                StatusPill(
                    text = "Fase $phase/5",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (!group.teacherComment.isNullOrBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Catatan Bimbingan Guru", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(group.teacherComment, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = activeSection == 0,
                        onClick = { activeSection = 0 },
                        label = { Text("Kerja 5 Fase", maxLines = 1) },
                        modifier = Modifier.height(48.dp).testTag("group_tab_workspace")
                    )
                }
                item {
                    FilterChip(
                        selected = activeSection == 1,
                        onClick = { activeSection = 1 },
                        label = { Text("Anggota (${group.memberUids.size})", maxLines = 1) },
                        modifier = Modifier.height(48.dp).testTag("group_tab_members")
                    )
                }
                item {
                    FilterChip(
                        selected = activeSection == 2,
                        onClick = { activeSection = 2 },
                        label = { Text("Diskusi (${discussions.size})", maxLines = 1) },
                        modifier = Modifier.height(48.dp).testTag("group_tab_discussion")
                    )
                }
            }
        }

        when (activeSection) {
            0 -> {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items((1..5).toList(), key = { it }) { p ->
                            FilterChip(
                                selected = phase == p,
                                onClick = { phase = p },
                                label = { Text("F$p") },
                                modifier = Modifier.height(48.dp).testTag("group_phase_chip_$p")
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Topik Investigasi Kelompok") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = rootProblem,
                        onValueChange = { rootProblem = it },
                        label = { Text("Fase 1: Orientasi & Rumusan Masalah") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = hypothesis,
                        onValueChange = { hypothesis = it },
                        label = { Text("Fase 2: Hipotesis Awal & Rencana Kerja") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = investigation,
                        onValueChange = { investigation = it },
                        label = { Text("Fase 3: Penyelidikan Ilmiah & Validasi Etnosains") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = conservation,
                        onValueChange = { conservation = it },
                        label = { Text("Fase 4: Formulasi Solusi Konservasi (In-Situ & Ex-Situ)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = discussionSummary,
                        onValueChange = { discussionSummary = it },
                        label = { Text("Kesimpulan Hasil Diskusi Kelompok") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("group_discussion_summary_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = campaign,
                        onValueChange = { campaign = it },
                        label = { Text("Fase 5: Narasi Kampanye Digital CER Kelompok") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        onClick = {
                            onSaveWorkspace(
                                group.id,
                                topic,
                                phase,
                                rootProblem,
                                hypothesis,
                                investigation,
                                conservation,
                                campaign,
                                discussionSummary,
                                roleMap.toMap()
                            )
                        },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_group_workspace_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan")
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan Catatan & Hasil Diskusi Kelompok")
                    }
                }
            }

            1 -> {
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Pembagian Anggota & Ketua Kelompok", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (isLeader) "Sebagai Ketua Kelompok, Anda dapat menentukan ketua baru, mengatur peran tiap anggota, dan membuka/menutup keanggotaan."
                                else "Anda dapat memperbarui peran Anda, sedangkan perubahan Ketua Kelompok hanya dapat dilakukan oleh Ketua atau Guru.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (isLeader) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Izinkan Teman Kelas Bergabung", style = MaterialTheme.typography.bodySmall)
                                    Switch(checked = openToJoin, onCheckedChange = { openToJoin = it })
                                }
                            }
                        }
                    }
                }

                items(group.memberUids.indices.toList(), key = { idx -> group.memberUids[idx] }) { idx ->
                    val memberUid = group.memberUids[idx]
                    val memberName = group.memberNames.getOrNull(idx) ?: "Anggota ${idx + 1}"
                    val currentRole = roleMap[memberUid] ?: "Anggota"
                    val isSelectedLeader = selectedLeaderUid == memberUid

                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        if (isSelectedLeader) Icons.Default.Star else Icons.Default.Person,
                                        contentDescription = "Anggota",
                                        tint = if (isSelectedLeader) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(memberName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                if (isSelectedLeader) {
                                    StatusPill("Ketua", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                                } else if (isLeader) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedLeaderUid = memberUid
                                            roleMap[memberUid] = "Ketua Kelompok"
                                        },
                                        modifier = Modifier.height(48.dp).testTag("set_leader_$memberUid")
                                    ) {
                                        Text("Jadikan Ketua")
                                    }
                                }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                defaultMemberRoles.forEach { r ->
                                    FilterChip(
                                        selected = currentRole == r,
                                        onClick = { roleMap[memberUid] = r },
                                        label = { Text(r, maxLines = 1) },
                                        modifier = Modifier.height(48.dp)
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = currentRole,
                                onValueChange = { roleMap[memberUid] = it.take(60) },
                                label = { Text("Peran di Kelompok") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (isLeader) {
                                onUpdateLeaderAndRoles(group.id, selectedLeaderUid, roleMap.toMap(), openToJoin)
                            } else {
                                onSaveWorkspace(
                                    group.id, topic, phase, rootProblem, hypothesis,
                                    investigation, conservation, campaign, discussionSummary, roleMap.toMap()
                                )
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_member_roles_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan Peran")
                        Spacer(Modifier.width(6.dp))
                        Text("Simpan Pembagian Anggota & Ketua")
                    }
                }
            }

            else -> {
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Forum, contentDescription = "Ruang Diskusi", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Ruang diskusi real-time kelompok & bimbingan guru. Catat temuan etnosains, data mikroklimat, dan pembagian tugas.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (discussions.isEmpty()) {
                    item {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                            Text(
                                "Belum ada pesan diskusi. Mulai diskusi dengan membagikan rumusan masalah atau temuan awalmu.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(discussions, key = { it.id }) { msg ->
                        val isMe = msg.senderUid == user.uid
                        val isTeacher = msg.senderRole == "GURU"
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isTeacher -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                                    isMe -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${msg.senderName}${if (isTeacher) " (Guru)" else ""}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusPill(
                                        text = "Fase ${msg.pblPhase}",
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Text(msg.message, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items((1..5).toList(), key = { it }) { p ->
                                    FilterChip(
                                        selected = chatPhase == p,
                                        onClick = { chatPhase = p },
                                        label = { Text("F$p") },
                                        modifier = Modifier.height(48.dp)
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = chatInput,
                                    onValueChange = { chatInput = it },
                                    label = { Text("Tulis pesan diskusi...") },
                                    minLines = 2,
                                    modifier = Modifier.weight(1f).testTag("group_discussion_input")
                                )
                                IconButton(
                                    onClick = {
                                        if (chatInput.isNotBlank()) {
                                            onSendDiscussionMessage(group, user.nama, chatPhase, chatInput)
                                            chatInput = ""
                                        }
                                    },
                                    enabled = !busy && chatInput.isNotBlank(),
                                    modifier = Modifier.testTag("send_group_discussion_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim Pesan", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveMemberName(group: PblGroup, uid: String): String {
    val idx = group.memberUids.indexOf(uid)
    return if (idx >= 0) group.memberNames.getOrNull(idx) ?: "Anggota" else "Belum ditentukan"
}
