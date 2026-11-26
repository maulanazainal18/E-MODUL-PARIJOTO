package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.curriculum.QuizEngineUtils
import com.example.data.curriculum.ReportComputationResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReportExportManager {

    const val MIME_PDF = "application/pdf"
    const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    fun buildReportFileName(prefix: String, ext: String): String {
        val cleanPrefix = prefix.lowercase()
            .replace("[^a-z0-9_]+".toRegex(), "_")
            .trim('_')
            .ifBlank { "laporan_parijoto" }
        return "${cleanPrefix}_2026.$ext"
    }

    fun writeBytesToUri(context: Context, uri: Uri, bytes: ByteArray): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: error("Tidak dapat membuka berkas tujuan.")
    }

    fun shareBytesViaFileProvider(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        chooserTitle: String
    ): Result<Unit> = runCatching {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
    }

    fun buildPdfReportBytes(
        title: String,
        subtitle: String,
        result: ReportComputationResult,
        isStudentMode: Boolean
    ): ByteArray {
        val nativeAttempt = runCatching {
            val pdf = PdfDocument()
            try {
                val pageWidth = 595
                val pageHeight = 842
                val margin = 32f

                val headerBgPaint = Paint().apply { color = Color.rgb(18, 92, 66) }
                val headerTextPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val subHeaderPaint = Paint().apply {
                    color = Color.rgb(220, 245, 232)
                    textSize = 9.5f
                    isAntiAlias = true
                }
                val sectionPaint = Paint().apply {
                    color = Color.rgb(18, 92, 66)
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val boldBodyPaint = Paint().apply {
                    color = Color.rgb(24, 34, 30)
                    textSize = 8.8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val bodyPaint = Paint().apply {
                    color = Color.rgb(42, 52, 48)
                    textSize = 8.5f
                    isAntiAlias = true
                }
                val linePaint = Paint().apply {
                    color = Color.rgb(200, 214, 206)
                    strokeWidth = 1f
                }

                var pageNum = 1
                var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                var canvas = page.canvas
                var y = 0f

                fun drawBannerHeader() {
                    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 74f, headerBgPaint)
                    canvas.drawText(title.take(72), margin, 30f, headerTextPaint)
                    canvas.drawText(subtitle.take(90), margin, 48f, subHeaderPaint)
                    val filterText = buildFilterSummaryText(result)
                    canvas.drawText(filterText.take(95), margin, 63f, subHeaderPaint)
                    y = 94f
                }

                fun ensureSpace(neededHeight: Float) {
                    if (y + neededHeight > pageHeight - 36f) {
                        pdf.finishPage(page)
                        pageNum += 1
                        page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                        canvas = page.canvas
                        drawBannerHeader()
                    }
                }

                drawBannerHeader()

                canvas.drawText("1. Ringkasan Eksekutif Penilaian", margin, y, sectionPaint)
                y += 16f
                val avgSummary = "Rata-rata Akhir: ${QuizEngineUtils.roundScore(result.overallAvgFinalScore)} | " +
                    "Kuis: ${fmtOpt(result.overallAvgQuiz)} | Post-Test: ${fmtOpt(result.overallAvgPostTest)} | " +
                    "Keterampilan: ${fmtOpt(result.overallAvgSkill)} | Proyek: ${fmtOpt(result.overallAvgProject)} | Sikap: ${fmtOpt(result.overallAvgAttitude)}"
                canvas.drawText(avgSummary, margin, y, bodyPaint)
                y += 13f
                val progressSummary = "Penyelesaian Materi: ${QuizEngineUtils.roundScore(result.overallMaterialCompletionPercent)}% | " +
                    "Pengumpulan Tugas: ${QuizEngineUtils.roundScore(result.overallAssignmentCompletionPercent)}% (${result.totalSubmittedTasks}/${result.totalExpectedTasks})"
                canvas.drawText(progressSummary, margin, y, bodyPaint)
                y += 18f

                if (!isStudentMode && result.classReports.isNotEmpty()) {
                    ensureSpace(34f)
                    canvas.drawText("2. Rekapitulasi Nilai per Kelas", margin, y, sectionPaint)
                    y += 14f
                    canvas.drawText("Kelas | Siswa | Kuis | Post | Ket | Proy | Sikap | Akhir | Materi | Tugas", margin, y, boldBodyPaint)
                    y += 6f
                    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                    y += 12f
                    result.classReports.forEach { c ->
                        ensureSpace(16f)
                        val row = "${c.className.take(18)} | ${c.activeStudentCount} | ${fmtOpt(c.avgQuizScore)} | " +
                            "${fmtOpt(c.avgPostTestScore)} | ${fmtOpt(c.avgSkillScore)} | ${fmtOpt(c.avgProjectScore)} | " +
                            "${fmtOpt(c.avgAttitudeScore)} | ${QuizEngineUtils.roundScore(c.avgFinalScore)} | " +
                            "${QuizEngineUtils.roundScore(c.avgMaterialCompletionPercent)}% | ${QuizEngineUtils.roundScore(c.avgAssignmentCompletionPercent)}%"
                        canvas.drawText(row, margin, y, bodyPaint)
                        y += 14f
                    }
                    y += 8f
                }

                ensureSpace(34f)
                canvas.drawText("3. Rekap Nilai & Progres Siswa", margin, y, sectionPaint)
                y += 14f
                canvas.drawText("Siswa (Kelas) | Kuis | Post | Ket | Proy | Sikap | Akhir | Materi | Tugas | Predikat", margin, y, boldBodyPaint)
                y += 6f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 12f

                if (result.studentReports.isEmpty()) {
                    canvas.drawText("Belum ada data siswa pada filter yang dipilih.", margin, y, bodyPaint)
                } else {
                    result.studentReports.forEach { s ->
                        ensureSpace(32f)
                        val line = "${s.studentName.take(18)} (${s.className.take(10)}) | ${fmtOpt(s.quizScore)} | ${fmtOpt(s.postTestScore)} | " +
                            "${fmtOpt(s.skillScore)} | ${fmtOpt(s.projectScore)} | ${fmtOpt(s.attitudeScore)} | " +
                            "${QuizEngineUtils.roundScore(s.finalReportScore)} | ${QuizEngineUtils.roundScore(s.materialCompletionPercent)}% | " +
                            "${s.submittedAssignmentsCount}/${s.totalAssignmentsCount} | ${s.predicate}"
                        canvas.drawText(line.take(102), margin, y, boldBodyPaint)
                        y += 12f
                        if (s.traces.isNotEmpty()) {
                            s.traces.take(8).forEach { t ->
                                ensureSpace(14f)
                                val traceText = "  - [${t.componentType.label} ${t.weightPercent}%] ${t.sourceTitle.take(32)}: ${t.score} (${t.formulaDetail})"
                                canvas.drawText(traceText.take(104), margin, y, bodyPaint)
                                y += 11f
                            }
                        }
                        y += 4f
                    }
                }

                pdf.finishPage(page)
                val out = ByteArrayOutputStream()
                pdf.writeTo(out)
                out.toByteArray()
            } finally {
                runCatching { pdf.close() }
            }
        }
        return nativeAttempt.getOrElse {
            buildFallbackPurePdfBytes(title, subtitle, result, isStudentMode)
        }
    }

    private fun buildFallbackPurePdfBytes(
        title: String,
        subtitle: String,
        result: ReportComputationResult,
        isStudentMode: Boolean
    ): ByteArray {
        val lines = mutableListOf<String>()
        lines += title
        lines += subtitle
        lines += buildFilterSummaryText(result)
        lines += ""
        lines += "1. Ringkasan Eksekutif Penilaian"
        lines += "Rata-rata Akhir: ${QuizEngineUtils.roundScore(result.overallAvgFinalScore)} | " +
            "Kuis: ${fmtOpt(result.overallAvgQuiz)} | Post-Test: ${fmtOpt(result.overallAvgPostTest)} | " +
            "Keterampilan: ${fmtOpt(result.overallAvgSkill)} | Proyek: ${fmtOpt(result.overallAvgProject)} | Sikap: ${fmtOpt(result.overallAvgAttitude)}"
        lines += "Penyelesaian Materi: ${QuizEngineUtils.roundScore(result.overallMaterialCompletionPercent)}% | " +
            "Pengumpulan Tugas: ${QuizEngineUtils.roundScore(result.overallAssignmentCompletionPercent)}% (${result.totalSubmittedTasks}/${result.totalExpectedTasks})"
        if (!isStudentMode && result.classReports.isNotEmpty()) {
            lines += ""
            lines += "2. Rekapitulasi Nilai per Kelas"
            result.classReports.forEach { c ->
                lines += "${c.className} | Siswa: ${c.activeStudentCount} | Akhir: ${QuizEngineUtils.roundScore(c.avgFinalScore)} | Materi: ${QuizEngineUtils.roundScore(c.avgMaterialCompletionPercent)}%"
            }
        }
        lines += ""
        lines += "3. Rekap Nilai & Progres Siswa"
        result.studentReports.forEach { s ->
            lines += "${s.studentName} (${s.className}) | Akhir: ${QuizEngineUtils.roundScore(s.finalReportScore)} | Materi: ${QuizEngineUtils.roundScore(s.materialCompletionPercent)}% | Tugas: ${s.submittedAssignmentsCount}/${s.totalAssignmentsCount} | ${s.predicate}"
            s.traces.forEach { t ->
                lines += "  - [${t.componentType.label} ${t.weightPercent}%] ${t.sourceTitle}: ${t.score} (${t.formulaDetail})"
            }
        }

        val linesPerPage = 46
        val pages = lines.chunked(linesPerPage).ifEmpty { listOf(listOf(title)) }
        val objects = mutableListOf<String>()
        // 1: Catalog, 2: Pages, 3: Font, then per page (Page + Content)
        val pageObjIds = mutableListOf<Int>()
        val contentPairs = mutableListOf<Pair<Int, String>>()
        var nextId = 4
        pages.forEach { pageLines ->
            val pageId = nextId++
            val contentId = nextId++
            pageObjIds += pageId
            val streamText = buildString {
                append("BT\n/F1 10 Tf\n36 800 Td\n15 TL\n")
                pageLines.forEachIndexed { idx, raw ->
                    val safe = raw
                        .replace("\u2022", "-")
                        .replace("[^\\x20-\\x7E\\xA0-\\xFF]".toRegex(), " ")
                        .replace("\\", "\\\\")
                        .replace("(", "\\(")
                        .replace(")", "\\)")
                        .take(95)
                    if (idx == 0) append("($safe) Tj\n") else append("T* ($safe) Tj\n")
                }
                append("ET")
            }
            contentPairs += pageId to "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents $contentId 0 R >>"
            contentPairs += contentId to "<< /Length ${streamText.toByteArray(StandardCharsets.ISO_8859_1).size} >>\nstream\n$streamText\nendstream"
        }

        objects += "<< /Type /Catalog /Pages 2 0 R >>"
        objects += "<< /Type /Pages /Kids [${pageObjIds.joinToString(" ") { "$it 0 R" }}] /Count ${pageObjIds.size} >>"
        objects += "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        contentPairs.sortedBy { it.first }.forEach { (_, body) -> objects += body }

        val pdfOut = StringBuilder("%PDF-1.4\n")
        val offsets = mutableListOf(0)
        objects.forEachIndexed { idx, objBody ->
            offsets += pdfOut.length
            pdfOut.append("${idx + 1} 0 obj\n$objBody\nendobj\n")
        }
        val xrefOffset = pdfOut.length
        pdfOut.append("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n")
        for (i in 1 until offsets.size) {
            pdfOut.append(String.format(java.util.Locale.US, "%010d 00000 n \n", offsets[i]))
        }
        pdfOut.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF")
        return pdfOut.toString().toByteArray(StandardCharsets.ISO_8859_1)
    }

    fun buildExcelWorkbookBytes(
        title: String,
        subtitle: String,
        result: ReportComputationResult,
        isStudentMode: Boolean
    ): ByteArray {
        val studentRows = mutableListOf<List<ExcelCell>>()
        studentRows += listOf(ExcelCell.str(title), ExcelCell.str(subtitle), ExcelCell.str(buildFilterSummaryText(result)))
        studentRows += listOf(
            ExcelCell.str("Nama Siswa"),
            ExcelCell.str("Kelas"),
            ExcelCell.str("Nilai Kuis"),
            ExcelCell.str("Nilai Post-Test"),
            ExcelCell.str("Nilai Keterampilan"),
            ExcelCell.str("Nilai Proyek"),
            ExcelCell.str("Nilai Sikap"),
            ExcelCell.str("Nilai Akhir Rapor"),
            ExcelCell.str("Bobot Komponen Aktif (%)"),
            ExcelCell.str("Predikat"),
            ExcelCell.str("Topik Selesai"),
            ExcelCell.str("Total Topik"),
            ExcelCell.str("Penyelesaian Materi (%)"),
            ExcelCell.str("Tugas Terkumpul"),
            ExcelCell.str("Total Tugas"),
            ExcelCell.str("Pengumpulan Tugas (%)")
        )
        result.studentReports.forEach { s ->
            studentRows += listOf(
                ExcelCell.str(s.studentName),
                ExcelCell.str(s.className),
                ExcelCell.numOrEmpty(s.quizScore),
                ExcelCell.numOrEmpty(s.postTestScore),
                ExcelCell.numOrEmpty(s.skillScore),
                ExcelCell.numOrEmpty(s.projectScore),
                ExcelCell.numOrEmpty(s.attitudeScore),
                ExcelCell.num(s.finalReportScore),
                ExcelCell.num(s.activeWeightPercent.toDouble()),
                ExcelCell.str(s.predicate),
                ExcelCell.num(s.completedLessonsCount.toDouble()),
                ExcelCell.num(s.totalLessonsCount.toDouble()),
                ExcelCell.num(s.materialCompletionPercent),
                ExcelCell.num(s.submittedAssignmentsCount.toDouble()),
                ExcelCell.num(s.totalAssignmentsCount.toDouble()),
                ExcelCell.num(s.assignmentCompletionPercent)
            )
        }

        val classRows = mutableListOf<List<ExcelCell>>()
        classRows += listOf(
            ExcelCell.str("Nama Kelas"),
            ExcelCell.str("Kode Kelas"),
            ExcelCell.str("Guru Pengampu"),
            ExcelCell.str("Jumlah Siswa"),
            ExcelCell.str("Rata-rata Kuis"),
            ExcelCell.str("Rata-rata Post-Test"),
            ExcelCell.str("Rata-rata Keterampilan"),
            ExcelCell.str("Rata-rata Proyek"),
            ExcelCell.str("Rata-rata Sikap"),
            ExcelCell.str("Rata-rata Nilai Akhir"),
            ExcelCell.str("Rata-rata Materi (%)"),
            ExcelCell.str("Rata-rata Pengumpulan Tugas (%)")
        )
        if (!isStudentMode) {
            result.classReports.forEach { c ->
                classRows += listOf(
                    ExcelCell.str(c.className),
                    ExcelCell.str(c.classCode),
                    ExcelCell.str(c.teacherName),
                    ExcelCell.num(c.activeStudentCount.toDouble()),
                    ExcelCell.numOrEmpty(c.avgQuizScore),
                    ExcelCell.numOrEmpty(c.avgPostTestScore),
                    ExcelCell.numOrEmpty(c.avgSkillScore),
                    ExcelCell.numOrEmpty(c.avgProjectScore),
                    ExcelCell.numOrEmpty(c.avgAttitudeScore),
                    ExcelCell.num(c.avgFinalScore),
                    ExcelCell.num(c.avgMaterialCompletionPercent),
                    ExcelCell.num(c.avgAssignmentCompletionPercent)
                )
            }
        }

        val traceRows = mutableListOf<List<ExcelCell>>()
        traceRows += listOf(
            ExcelCell.str("Nama Siswa"),
            ExcelCell.str("Kelas"),
            ExcelCell.str("Komponen"),
            ExcelCell.str("Sumber Penilaian"),
            ExcelCell.str("Pertemuan"),
            ExcelCell.str("Bobot (%)"),
            ExcelCell.str("Skor"),
            ExcelCell.str("Rincian Perhitungan"),
            ExcelCell.str("Dipublikasikan"),
            ExcelCell.str("Dipakai di Rata-rata")
        )
        result.studentReports.forEach { s ->
            s.traces.forEach { t ->
                traceRows += listOf(
                    ExcelCell.str(s.studentName),
                    ExcelCell.str(s.className),
                    ExcelCell.str(t.componentType.label),
                    ExcelCell.str(t.sourceTitle),
                    ExcelCell.str(t.meetingNumber?.let { "Pertemuan $it" } ?: "Lintas Pertemuan"),
                    ExcelCell.num(t.weightPercent.toDouble()),
                    ExcelCell.num(t.score),
                    ExcelCell.str(t.formulaDetail),
                    ExcelCell.str(if (t.published) "Ya" else "Draf"),
                    ExcelCell.str(if (t.includedInAverage) "Ya" else "Riwayat Percobaan")
                )
            }
        }

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", contentTypesXml())
            writeZipEntry(zip, "_rels/.rels", rootRelsXml())
            writeZipEntry(zip, "xl/workbook.xml", workbookXml())
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            writeZipEntry(zip, "xl/styles.xml", stylesXml())
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildSheetXml(studentRows))
            writeZipEntry(zip, "xl/worksheets/sheet2.xml", buildSheetXml(classRows))
            writeZipEntry(zip, "xl/worksheets/sheet3.xml", buildSheetXml(traceRows))
        }
        return baos.toByteArray()
    }

    private fun buildFilterSummaryText(result: ReportComputationResult): String {
        val f = result.filter
        val cls = f.classId?.let { id -> result.classReports.firstOrNull { it.classId == id }?.className ?: id } ?: "Semua Kelas"
        val mtg = f.meetingNumber?.let { "Pertemuan $it" } ?: "Semua Pertemuan"
        val mod = when (f.moduleId) {
            "modul_1_etnosains" -> "Modul 1"
            "modul_2_konservasi" -> "Modul 2"
            null -> "Semua Modul"
            else -> f.moduleId
        }
        return "Filter: $cls • $mtg • $mod • Siswa: ${result.studentReports.size}"
    }

    private fun fmtOpt(v: Double?): String = v?.let { QuizEngineUtils.roundScore(it).toString() } ?: "-"

    private sealed interface ExcelCell {
        data class Str(val value: String) : ExcelCell
        data class Num(val value: Double) : ExcelCell
        data object Empty : ExcelCell

        companion object {
            fun str(v: String): ExcelCell = Str(v)
            fun num(v: Double): ExcelCell = Num(v)
            fun numOrEmpty(v: Double?): ExcelCell = if (v == null) Empty else Num(v)
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildSheetXml(rows: List<List<ExcelCell>>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { rIdx, row ->
            val rowNum = rIdx + 1
            append("""<row r="$rowNum">""")
            row.forEachIndexed { cIdx, cell ->
                val ref = "${colLetter(cIdx)}$rowNum"
                when (cell) {
                    is ExcelCell.Str -> append("""<c r="$ref" t="inlineStr"><is><t>${escapeXml(cell.value)}</t></is></c>""")
                    is ExcelCell.Num -> append("""<c r="$ref"><v>${cell.value}</v></c>""")
                    ExcelCell.Empty -> Unit
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun colLetter(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A' + (n % 26)))
            n = (n / 26) - 1
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Rekap_Siswa" sheetId="1" r:id="rId1"/>
    <sheet name="Rekap_Kelas" sheetId="2" r:id="rId2"/>
    <sheet name="Telusur_Komponen" sheetId="3" r:id="rId3"/>
  </sheets>
</workbook>"""

    private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>"""
}
