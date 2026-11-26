package com.example.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Lesson
import com.example.ui.components.LessonIllustrationCard
import com.example.ui.components.StatusPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonReaderScreen(
    lesson: Lesson,
    moduleLessons: List<Lesson>,
    completedLessons: Set<String>,
    bookmarkedLessons: Set<String>,
    fontScale: Float,
    savedScrollPercent: Int,
    aiStreamText: String,
    aiLoading: Boolean,
    onBack: () -> Unit,
    onSelectLesson: (Lesson) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onFontScaleChange: (Double) -> Unit,
    onRecordReadProgress: (String, String, Int) -> Unit,
    onMarkComplete: () -> Unit,
    onAskAiTutor: (String, String) -> Unit
) {
    var aiPrompt by remember { mutableStateOf("") }
    var showAiPanel by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showFontSheet by remember { mutableStateOf(false) }

    val orderedLessons = remember(moduleLessons) { moduleLessons.sortedBy { it.orderIndex } }
    val currentIndex = orderedLessons.indexOfFirst { it.id == lesson.id }
    val prevLesson = if (currentIndex > 0) orderedLessons[currentIndex - 1] else null
    val nextLesson = if (currentIndex in 0 until orderedLessons.lastIndex) orderedLessons[currentIndex + 1] else null

    val listState = rememberLazyListState()
    var maxScrollPct by remember(lesson.id) { mutableIntStateOf(savedScrollPercent.coerceIn(10, 100)) }

    val liveScrollPct by remember(lesson.id) {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount.coerceAtLeast(1)
            val lastVisible = (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            ((lastVisible * 100) / totalItems).coerceIn(10, 100)
        }
    }

    LaunchedEffect(liveScrollPct) {
        if (liveScrollPct > maxScrollPct) {
            maxScrollPct = liveScrollPct
        }
    }

    LaunchedEffect(lesson.id) {
        onRecordReadProgress(lesson.moduleId, lesson.id, maxScrollPct)
    }

    val isCompleted = completedLessons.contains(lesson.id)
    val isBookmarked = bookmarkedLessons.contains(lesson.id)

    BackHandler {
        onRecordReadProgress(lesson.moduleId, lesson.id, maxScrollPct)
        onBack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Reader Header & Reading Progress
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onRecordReadProgress(lesson.moduleId, lesson.id, maxScrollPct)
                            onBack()
                        },
                        modifier = Modifier.size(48.dp).testTag("lesson_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke Modul")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Subbab ${if (currentIndex >= 0) currentIndex + 1 else 1} dari ${orderedLessons.size.coerceAtLeast(1)} • Baca $maxScrollPct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showTocSheet = true },
                        modifier = Modifier.size(48.dp).testTag("lesson_toc_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Daftar Isi Modul")
                    }
                    IconButton(
                        onClick = { showFontSheet = true },
                        modifier = Modifier.size(48.dp).testTag("lesson_font_settings_button")
                    ) {
                        Icon(Icons.Default.FormatSize, contentDescription = "Pengaturan Ukuran Teks")
                    }
                    IconButton(
                        onClick = { onToggleBookmark(lesson.id) },
                        modifier = Modifier.size(48.dp).testTag("lesson_bookmark_button")
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Hapus Bookmark" else "Simpan Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { (maxScrollPct / 100f).coerceIn(0.05f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(4.dp)
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = lesson.citation ?: "Ahzami, S.Pd., Gr. (2026), Magister Pendidikan Biologi UNY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!lesson.illustrationType.isNullOrBlank()) {
                item {
                    LessonIllustrationCard(illustrationType = lesson.illustrationType)
                }
            }

            item {
                ScaledLessonCard(
                    heading = "Tujuan Pembelajaran",
                    body = lesson.learningObjective,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    fontScale = fontScale
                )
            }

            item {
                ScaledLessonCard(
                    heading = "Penjelasan Ilmiah Lengkap",
                    body = lesson.explanation,
                    containerColor = MaterialTheme.colorScheme.surface,
                    fontScale = fontScale
                )
            }

            lesson.teenExplanation?.takeIf { it.isNotBlank() }?.let { teen ->
                item {
                    ScaledLessonCard(
                        heading = "Bahasa Konsep SMA",
                        body = teen,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        fontScale = fontScale
                    )
                }
            }

            lesson.dailyExample?.takeIf { it.isNotBlank() }?.let { example ->
                item {
                    ScaledLessonCard(
                        heading = "Studi Kasus & Contoh Kontekstual",
                        body = example,
                        containerColor = MaterialTheme.colorScheme.surface,
                        fontScale = fontScale
                    )
                }
            }

            lesson.funFact?.takeIf { it.isNotBlank() }?.let { fact ->
                item {
                    ScaledLessonCard(
                        heading = "Fakta Menarik Botani",
                        body = fact,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                        fontScale = fontScale
                    )
                }
            }

            lesson.sparkQuestion?.takeIf { it.isNotBlank() }?.let { spark ->
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = "Pemantik", tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(Modifier.width(6.dp))
                                Text("Pertanyaan Pemantik Diskusi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = spark,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * fontScale
                                )
                            )
                        }
                    }
                }
            }

            item {
                ScaledLessonCard(
                    heading = "Rangkuman Subbab",
                    body = lesson.summary,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    fontScale = fontScale
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onMarkComplete,
                        enabled = !isCompleted,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("mark_lesson_complete_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Selesai")
                        Spacer(Modifier.width(6.dp))
                        Text(if (isCompleted) "Sudah Selesai" else "Tandai Selesai")
                    }
                    OutlinedButton(
                        onClick = { showAiPanel = !showAiPanel },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("toggle_ai_tutor_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Asisten AI")
                        Spacer(Modifier.width(6.dp))
                        Text("Tanya AI")
                    }
                }
            }

            if (showAiPanel) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, contentDescription = "Tutor Biologi", tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Asisten Riset Biologi Parijoto", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            StatusPill(
                                text = "Membedakan bukti praklinis, komputasi, & klinis",
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = aiPrompt,
                                    onValueChange = { aiPrompt = it },
                                    placeholder = { Text("Tanya konsep dari topik ini...") },
                                    modifier = Modifier.weight(1f).testTag("ai_tutor_input")
                                )
                                IconButton(
                                    onClick = {
                                        if (aiPrompt.isNotBlank()) {
                                            onAskAiTutor(aiPrompt, "${lesson.title}\n${lesson.explanation}")
                                        }
                                    },
                                    enabled = !aiLoading && aiPrompt.isNotBlank(),
                                    modifier = Modifier.size(48.dp).testTag("ai_tutor_send_button")
                                ) {
                                    if (aiLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim pertanyaan")
                                    }
                                }
                            }
                            if (aiStreamText.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = aiStreamText,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = MaterialTheme.typography.bodySmall.fontSize * fontScale,
                                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * fontScale
                                        ),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Navigasi Antarsubbab (Previous / Next)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Navigasi Antarsubbab",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (prevLesson != null) {
                                        onRecordReadProgress(lesson.moduleId, lesson.id, maxScrollPct)
                                        onSelectLesson(prevLesson)
                                    }
                                },
                                enabled = prevLesson != null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("lesson_prev_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Subbab Sebelumnya")
                                Spacer(Modifier.width(4.dp))
                                Text("Sebelumnya", maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    if (nextLesson != null) {
                                        onRecordReadProgress(lesson.moduleId, lesson.id, maxScrollPct)
                                        onSelectLesson(nextLesson)
                                    }
                                },
                                enabled = nextLesson != null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("lesson_next_button")
                            ) {
                                Text("Berikutnya", maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Subbab Berikutnya")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTocSheet) {
        ModalBottomSheet(onDismissRequest = { showTocSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "Daftar Isi Subbab (${orderedLessons.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(orderedLessons, key = { it.id }) { item ->
                        val isCurrent = item.id == lesson.id
                        val done = completedLessons.contains(item.id)
                        val bookmarked = bookmarkedLessons.contains(item.id)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRecordReadProgress(lesson.moduleId, lesson.id, maxScrollPct)
                                    showTocSheet = false
                                    onSelectLesson(item)
                                }
                                .testTag("toc_item_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (done) "Selesai" else "Belum selesai",
                                    tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (bookmarked) {
                                    Icon(
                                        Icons.Default.Bookmark,
                                        contentDescription = "Tersimpan Bookmark",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFontSheet) {
        val presets = listOf(
            "Kecil" to 0.85,
            "Normal" to 1.0,
            "Besar" to 1.15,
            "Ekstra" to 1.35
        )
        ModalBottomSheet(onDismissRequest = { showFontSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Pengaturan Ukuran Teks Pembaca",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Preferensi ukuran huruf akan tersimpan otomatis ke akunmu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (label, scale) ->
                        val selected = kotlin.math.abs(fontScale - scale) < 0.06
                        FilterChip(
                            selected = selected,
                            onClick = { onFontScaleChange(scale) },
                            label = { Text(label) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("font_scale_${label.lowercase()}")
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Contoh tampilan teks: Medinilla speciosa (Parijoto) tumbuh sebagai epifit pada hutan pegunungan lembap Gunung Muria.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * fontScale
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScaledLessonCard(
    heading: String,
    body: String,
    containerColor: Color,
    fontScale: Float
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                heading,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * fontScale
                )
            )
        }
    }
}
