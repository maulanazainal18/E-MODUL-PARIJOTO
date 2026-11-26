package com.example.ui.student

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.ai.AiEvidenceAnalysis
import com.example.data.model.Assignment
import com.example.data.model.Enrollment
import com.example.data.model.PblGroup
import com.example.data.model.PeerReview
import com.example.data.model.Submission
import com.example.data.model.UserAccount
import com.example.data.repository.PblAndAssessmentRepository
import com.example.ui.components.StatusPill
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AssignmentEditorScreen(
    user: UserAccount,
    enrollment: Enrollment?,
    group: PblGroup?,
    assignment: Assignment,
    existingSubmission: Submission?,
    aiEvidence: AiEvidenceAnalysis?,
    aiLoading: Boolean,
    busy: Boolean,
    onBack: () -> Unit,
    onSaveSubmission: (Submission, Boolean) -> Unit,
    onAnalyzeEvidenceAi: (String) -> Unit
) {
    val context = LocalContext.current
    val isLocked = existingSubmission?.status == "GRADED"
    val needsRevision = existingSubmission?.status == "REVISION_REQUIRED"

    var problemAnalysis by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.problemAnalysis.orEmpty()) }
    var hypothesis by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.hypothesis.orEmpty()) }
    var evidence by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.scientificEvidence.orEmpty()) }
    var dataTable by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.dataAnalysisTable.orEmpty()) }
    var solution by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.conservationSolution.orEmpty()) }
    var discussionSummary by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.discussionSummary.orEmpty()) }
    var cerClaim by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.cerClaim.orEmpty()) }
    var cerEvidence by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.cerEvidence.orEmpty()) }
    var cerReasoning by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.cerReasoning.orEmpty()) }
    var campaignCaption by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.campaignCaption.orEmpty()) }

    var attachmentName by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.attachmentName) }
    var attachmentMime by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.attachmentMimeType) }
    var attachmentSize by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.attachmentSizeBytes) }
    var attachmentUri by remember(assignment.id, existingSubmission?.id) { mutableStateOf(existingSubmission?.attachmentUrl) }
    var attachmentError by remember { mutableStateOf<String?>(null) }
    var aiQuery by remember { mutableStateOf("") }

    fun handlePickedUri(uri: Uri?, tryPersist: Boolean) {
        if (uri == null) return
        if (tryPersist) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
                // Non-persistable URI provider; metadata and current session preview still work safely
            }
        }
        val meta = queryAttachmentMetadata(context, uri)
        if (meta.mimeType == null || PblAndAssessmentRepository.ALLOWED_MIME_PREFIXES.none { meta.mimeType.startsWith(it, ignoreCase = true) }) {
            attachmentError = "Tipe berkas tidak diizinkan (${meta.mimeType ?: "tidak diketahui"}). Pilih gambar atau PDF."
            return
        }
        if (meta.sizeBytes > PblAndAssessmentRepository.MAX_ATTACHMENT_BYTES) {
            attachmentError = "Ukuran berkas ${meta.sizeBytes / 1024} KB melebihi batas maksimal 5 MB."
            return
        }
        attachmentError = null
        attachmentName = meta.fileName
        attachmentMime = meta.mimeType
        attachmentSize = meta.sizeBytes
        attachmentUri = uri.toString()
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        handlePickedUri(uri, tryPersist = false)
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        handlePickedUri(uri, tryPersist = true)
    }

    fun buildSubmission(): Submission = Submission(
        id = existingSubmission?.id ?: "${assignment.id}_${user.uid}",
        assignmentId = assignment.id,
        classId = enrollment?.classId ?: "DEFAULT",
        teacherUid = enrollment?.teacherUid ?: "SYSTEM_DEFAULT",
        studentUid = user.uid,
        studentName = user.nama,
        groupId = group?.id,
        pblPhase = assignment.pblPhase,
        problemAnalysis = problemAnalysis,
        hypothesis = hypothesis,
        scientificEvidence = evidence,
        dataAnalysisTable = dataTable,
        conservationSolution = solution,
        discussionSummary = discussionSummary,
        cerClaim = cerClaim,
        cerEvidence = cerEvidence,
        cerReasoning = cerReasoning,
        campaignCaption = campaignCaption,
        attachmentName = attachmentName,
        attachmentMimeType = attachmentMime,
        attachmentSizeBytes = attachmentSize,
        attachmentUrl = attachmentUri,
        status = existingSubmission?.status ?: "DRAFT",
        teacherFeedback = existingSubmission?.teacherFeedback
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("assignment_editor_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("assignment_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(assignment.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Fase ${assignment.pblPhase} • ${assignment.dueDateLabel ?: "PBL"}", style = MaterialTheme.typography.bodySmall)
                }
                StatusPill(
                    text = submissionStatusLabel(existingSubmission?.status),
                    containerColor = when (existingSubmission?.status) {
                        "REVISION_REQUIRED" -> MaterialTheme.colorScheme.errorContainer
                        "GRADED", "SUBMITTED" -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = when (existingSubmission?.status) {
                        "REVISION_REQUIRED" -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
        }

        val existingFb = existingSubmission?.teacherFeedback
        if (!existingFb.isNullOrBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (needsRevision) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier.testTag("teacher_feedback_banner")
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Feedback,
                            contentDescription = "Umpan Balik Guru",
                            tint = if (needsRevision) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (needsRevision) "Perlu Perbaikan (Koreksi Guru)" else "Umpan Balik Penilaian Guru",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(existingFb, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Text(assignment.instructions, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp))
            }
        }

        if (enrollment == null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("lkm_unenrolled_notice")
                ) {
                    Text(
                        "Anda belum tergabung ke kelas mana pun. Anda tetap dapat menyimpan draf E-LKM, dan silakan gabung kelas di Beranda agar tugas dapat dinilai oleh guru.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (isLocked) {
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        "Tugas ini sudah dinilai oleh guru sehingga tidak dapat diubah lagi.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (assignment.type == "E_LKM_1") {
            item {
                OutlinedTextField(
                    value = problemAnalysis,
                    onValueChange = { problemAnalysis = it },
                    enabled = !isLocked,
                    label = { Text("Fase 1: Analisis Masalah Eksploitasi & Paradoks Konservasi") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("elkm1_problem_input")
                )
            }
            item {
                OutlinedTextField(
                    value = hypothesis,
                    onValueChange = { hypothesis = it },
                    enabled = !isLocked,
                    label = { Text("Fase 2: Rumusan Masalah & Hipotesis Kelompok") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("elkm1_hypothesis_input")
                )
            }
        }

        if (assignment.type == "E_LKM_2") {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Lembar Data Acuan E-Modul (Analisis Mikroklimat & Fitokimia)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("• 2019: Kanopi 479 ha | Kelembapan 84% | Suhu 21,5°C | Epifit 42 rumpun/ha", style = MaterialTheme.typography.bodySmall)
                        Text("• 2021: Kanopi 355 ha | Kelembapan 69% | Suhu 24,8°C | Epifit 27 rumpun/ha", style = MaterialTheme.typography.bodySmall)
                        Text("• 2023: Kanopi 240 ha | Kelembapan 52% | Suhu 28,4°C | Epifit 11 rumpun/ha", style = MaterialTheme.typography.bodySmall)
                        Text("• Flavonoid (Vifta dkk., 2021): Lereng Muria Kudus 81,60 mg QE/g vs Kab. Semarang 76,48 mg QE/g.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Analisis", tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(6.dp))
                            Text("Bantuan AI Analisis Bukti Etnosains (Terstruktur)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = aiQuery,
                                onValueChange = { aiQuery = it },
                                placeholder = { Text("Misal: Cek bukti β-sitosterol & fotoinhibisi") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onAnalyzeEvidenceAi(aiQuery) },
                                enabled = !aiLoading && aiQuery.isNotBlank(),
                                modifier = Modifier.height(52.dp)
                            ) {
                                if (aiLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text("Analisis")
                            }
                        }
                        if (aiEvidence != null) {
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Tingkat Bukti: ${aiEvidence.tingkatBukti}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(aiEvidence.ringkasanIlmiah, style = MaterialTheme.typography.bodySmall)
                                    Text("Senyawa/Mekanisme: ${aiEvidence.senyawaDanMekanisme}", style = MaterialTheme.typography.bodySmall)
                                    Text("Konservasi: ${aiEvidence.dampakKonservasi}", style = MaterialTheme.typography.bodySmall)
                                    Text("Catatan Etis Medis: ${aiEvidence.catatanEtisMedis}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = evidence,
                    onValueChange = { evidence = it },
                    enabled = !isLocked,
                    label = { Text("Pengumpulan Bukti Ilmiah (Praklinis / Komputasi / Klinis)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("elkm2_evidence_input")
                )
            }
            item {
                OutlinedTextField(
                    value = dataTable,
                    onValueChange = { dataTable = it },
                    enabled = !isLocked,
                    label = { Text("Lembar Analisis Data (Tren Mikroklimat 2019–2023 & Flavonoid IG)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("elkm2_data_input")
                )
            }
            item {
                OutlinedTextField(
                    value = solution,
                    onValueChange = { solution = it },
                    enabled = !isLocked,
                    label = { Text("Formulasi Solusi Konservasi (In-Situ 30% + Ornitokori, Ex-Situ, IG)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("elkm2_solution_input")
                )
            }
        }

        if (assignment.type == "PROYEK_CER") {
            item {
                OutlinedTextField(
                    value = cerClaim,
                    onValueChange = { cerClaim = it },
                    enabled = !isLocked,
                    label = { Text("Claim (Klaim Ilmiah Utama)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("cer_claim_input")
                )
            }
            item {
                OutlinedTextField(
                    value = cerEvidence,
                    onValueChange = { cerEvidence = it },
                    enabled = !isLocked,
                    label = { Text("Evidence (Bukti Data Biokimia & Ekologi)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("cer_evidence_input")
                )
            }
            item {
                OutlinedTextField(
                    value = cerReasoning,
                    onValueChange = { cerReasoning = it },
                    enabled = !isLocked,
                    label = { Text("Reasoning (Penalaran Saintifik Biologi)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("cer_reasoning_input")
                )
            }
            item {
                OutlinedTextField(
                    value = campaignCaption,
                    onValueChange = { campaignCaption = it },
                    enabled = !isLocked,
                    label = { Text("Caption Kampanye Digital Persuasif") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!isLocked && group != null && !group.discussionSummary.isNullOrBlank() && discussionSummary.isBlank()) {
                    TextButton(onClick = { discussionSummary = group.discussionSummary }) {
                        Text("Salin Ringkasan Diskusi Kelompok (${group.name})")
                    }
                }
                OutlinedTextField(
                    value = discussionSummary,
                    onValueChange = { discussionSummary = it },
                    enabled = !isLocked,
                    label = { Text("Pengumpulan Hasil Diskusi Kelompok") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("submission_discussion_summary_input")
                )
            }
        }

        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Upload Dokumen & Gambar Hasil Karya (Opsional, Maks. 5 MB)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            enabled = !isLocked,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("pick_image_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Pilih Gambar")
                            Spacer(Modifier.width(6.dp))
                            Text("Pilih Gambar")
                        }
                        OutlinedButton(
                            onClick = { docPicker.launch(arrayOf("application/pdf", "image/*")) },
                            enabled = !isLocked,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("pick_pdf_button")
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Pilih PDF")
                            Spacer(Modifier.width(6.dp))
                            Text("Pilih Dokumen")
                        }
                    }
                    if (!attachmentName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Berkas terpilih: $attachmentName (${(attachmentSize ?: 0L) / 1024} KB)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            if (!isLocked) {
                                IconButton(onClick = {
                                    attachmentName = null; attachmentMime = null; attachmentSize = null; attachmentUri = null
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus Lampiran")
                                }
                            }
                        }
                        if (attachmentMime?.startsWith("image/") == true && !attachmentUri.isNullOrBlank()) {
                            AsyncImage(
                                model = attachmentUri,
                                contentDescription = "Pratinjau Gambar Karya",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                    if (attachmentError != null) {
                        Text(attachmentError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (!isLocked) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onSaveSubmission(buildSubmission(), false) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f).height(50.dp).testTag("save_submission_draft_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Draf")
                        Spacer(Modifier.width(6.dp))
                        Text("Simpan Draf")
                    }
                    Button(
                        onClick = { onSaveSubmission(buildSubmission(), true) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f).height(50.dp).testTag("submit_assignment_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kumpulkan")
                        Spacer(Modifier.width(6.dp))
                        Text(if (needsRevision) "Kirim Revisi" else "Kumpulkan")
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectPresentationAndPeerReviewScreen(
    user: UserAccount,
    assignments: List<Assignment>,
    presentations: List<Submission>,
    peerReviews: List<PeerReview>,
    busy: Boolean,
    onBack: () -> Unit,
    onSubmitPeerReview: (Submission, Int, Int, Int, String) -> Unit
) {
    var reviewingTarget by remember { mutableStateOf<Submission?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("presentations_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("presentations_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Presentasi Hasil Proyek & Penilaian Antarteman", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Fase 5 PBL • Tinjau karya kelompok kelas dan beri umpan balik saintifik", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (presentations.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Slideshow, contentDescription = "Presentasi", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Belum ada karya E-LKM atau Kampanye CER yang dikumpulkan di kelas Anda. Gabung ke kelas dan kumpulkan proyek untuk tampil di etalase presentasi.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            items(presentations, key = { it.id }) { sub ->
                val asgTitle = assignments.firstOrNull { it.id == sub.assignmentId }?.title ?: sub.assignmentId
                val subReviews = peerReviews.filter { it.submissionId == sub.id }
                val myReview = subReviews.firstOrNull { it.reviewerUid == user.uid }
                val isOwn = sub.studentUid == user.uid

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.testTag("presentation_card_${sub.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sub.studentName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(asgTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            StatusPill(
                                text = "${subReviews.size} Ulasan",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        sub.cerClaim?.takeIf { it.isNotBlank() }?.let { Text("Claim: $it", style = MaterialTheme.typography.bodySmall) }
                        sub.cerEvidence?.takeIf { it.isNotBlank() }?.let { Text("Evidence: $it", style = MaterialTheme.typography.bodySmall) }
                        sub.cerReasoning?.takeIf { it.isNotBlank() }?.let { Text("Reasoning: $it", style = MaterialTheme.typography.bodySmall) }
                        sub.conservationSolution?.takeIf { it.isNotBlank() }?.let { Text("Solusi Konservasi: $it", style = MaterialTheme.typography.bodySmall) }
                        sub.discussionSummary?.takeIf { it.isNotBlank() }?.let { Text("Hasil Diskusi: $it", style = MaterialTheme.typography.bodySmall) }
                        sub.campaignCaption?.takeIf { it.isNotBlank() }?.let { Text("Caption: $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
                        sub.attachmentName?.let { Text("Lampiran Karya: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary) }

                        if (subReviews.isNotEmpty()) {
                            val avg = subReviews.map { (it.clarityScore + it.evidenceScore + it.solutionScore) / 3.0 }.average()
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Rata-rata Penilaian Teman: ${String.format(Locale.US, "%.1f", avg)} / 4.0",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    subReviews.take(3).forEach { r ->
                                        Text("• ${r.reviewerName}: ${r.comment}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        if (!isOwn) {
                            val isEditingThis = reviewingTarget?.id == sub.id
                            OutlinedButton(
                                onClick = { reviewingTarget = if (isEditingThis) null else sub },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_peer_review_${sub.id}")
                            ) {
                                Icon(Icons.Default.RateReview, contentDescription = "Nilai Antarteman")
                                Spacer(Modifier.width(6.dp))
                                Text(if (myReview == null) "Beri Penilaian Antarteman" else "Perbarui Penilaian Saya")
                            }

                            if (isEditingThis) {
                                var clarity by remember(sub.id) { mutableIntStateOf(myReview?.clarityScore ?: 3) }
                                var evidenceScore by remember(sub.id) { mutableIntStateOf(myReview?.evidenceScore ?: 3) }
                                var solutionScore by remember(sub.id) { mutableIntStateOf(myReview?.solutionScore ?: 3) }
                                var comment by remember(sub.id) { mutableStateOf(myReview?.comment.orEmpty()) }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                                    Text("Kejelasan Rumusan / Klaim (1–4): $clarity", style = MaterialTheme.typography.labelMedium)
                                    Slider(value = clarity.toFloat(), onValueChange = { clarity = it.roundToInt() }, valueRange = 1f..4f, steps = 2)
                                    Text("Kekuatan Bukti Ilmiah (1–4): $evidenceScore", style = MaterialTheme.typography.labelMedium)
                                    Slider(value = evidenceScore.toFloat(), onValueChange = { evidenceScore = it.roundToInt() }, valueRange = 1f..4f, steps = 2)
                                    Text("Kelayakan Solusi Konservasi (1–4): $solutionScore", style = MaterialTheme.typography.labelMedium)
                                    Slider(value = solutionScore.toFloat(), onValueChange = { solutionScore = it.roundToInt() }, valueRange = 1f..4f, steps = 2)
                                    OutlinedTextField(
                                        value = comment,
                                        onValueChange = { comment = it },
                                        label = { Text("Komentar Konstruktif untuk ${sub.studentName}") },
                                        minLines = 2,
                                        modifier = Modifier.fillMaxWidth().testTag("peer_review_comment_input")
                                    )
                                    Button(
                                        onClick = {
                                            onSubmitPeerReview(sub, clarity, evidenceScore, solutionScore, comment)
                                            reviewingTarget = null
                                        },
                                        enabled = !busy && comment.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_peer_review_button")
                                    ) {
                                        Text("Kirim Penilaian Antarteman")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun submissionStatusLabel(status: String?): String = when (status) {
    "GRADED" -> "Dinilai"
    "SUBMITTED" -> "Terkumpul"
    "REVISION_REQUIRED" -> "Perlu Revisi"
    "DRAFT" -> "Draf"
    else -> "Belum Diisi"
}

private data class PickedAttachmentMeta(
    val fileName: String,
    val mimeType: String?,
    val sizeBytes: Long
)

private fun queryAttachmentMetadata(context: Context, uri: Uri): PickedAttachmentMeta {
    val mime = context.contentResolver.getType(uri)
    var name = "lampiran"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
        }
    }
    if (size <= 0L) {
        size = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buf = ByteArray(8192)
                var total = 0L
                val cap = PblAndAssessmentRepository.MAX_ATTACHMENT_BYTES + 1
                while (total < cap) {
                    val read = stream.read(buf)
                    if (read <= 0) break
                    total += read
                }
                total
            } ?: 0L
        }.getOrDefault(0L)
    }
    return PickedAttachmentMeta(name, mime, size)
}
